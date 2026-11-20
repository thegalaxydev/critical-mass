package com.criticalmass.content.power;

import com.criticalmass.registry.CMSounds;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import java.util.List;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class GridTieBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, WireNode {
    private final WireLinks wires = new WireLinks();
    private static final int SYNC_TICKS = 10;
    private ScrollValueBehaviour contract;
    private double delivered;
    private double order;
    private int orderTicks;
    private int heldTicks;
    private int idleTicks;
    private int completed;
    private int missed;
    private int syncIn;
    private Event event = Event.NORMAL;
    private int eventTicks;
    private double depth;
    private double pu = 1.0;
    private double offset;
    private double phase;

    public enum Event {
        NORMAL, DIP, SWING, OUTAGE
    }

    public GridTieBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        contract = new Contract(this);
        behaviours.add(contract);
    }

    @Override
    public WireLinks wires() {
        return wires;
    }

    @Override
    public void wiresChanged() {
        setChanged();
        sendData();
    }

    @Override
    public Vec3 terminal(int index) {
        return new Vec3(0.5, 1.0, 0.5);
    }

    public Event event() {
        return event;
    }

    public boolean live() {
        return event != Event.OUTAGE;
    }

    public double kilovolts() {
        return Power.GRID_KV * pu;
    }

    public double hertz() {
        return Power.GRID_HZ + offset;
    }

    public double phase() {
        return phase;
    }

    public void disturb(Event kind, int ticks, double amount) {
        boolean was = live();
        event = kind;
        eventTicks = kind == Event.NORMAL ? 0 : ticks;
        depth = amount;
        if (level != null && !level.isClientSide && was != live()) {
            PowerNetwork.changed(level);
            if (was) {
                PowerNetwork.tripNearest(level, worldPosition, "criticalmass.breaker.trip.grid_lost");
            }
        }
        syncIn = 0;
        setChanged();
    }

    public double contractMw() {
        return contract.getValue() * Power.ORDER_STEP_MW;
    }

    public void setContractMw(int megawatts) {
        contract.setValue(megawatts / Power.ORDER_STEP_MW);
    }

    public double deliveredMw() {
        return Power.megawatts(delivered);
    }

    public double orderMw() {
        return order;
    }

    public boolean ordering() {
        return order > 0;
    }

    public int completed() {
        return completed;
    }

    public int missed() {
        return missed;
    }

    public int heldTicks() {
        return heldTicks;
    }

    public void placeOrder(double megawatts) {
        order = megawatts;
        orderTicks = 0;
        heldTicks = 0;
        idleTicks = 0;
        syncIn = 0;
        setChanged();
    }

    public int comparator() {
        return order <= 0 ? 0 : (int) Math.max(1, Math.min(15, Math.round(15.0 * heldTicks / Power.ORDER_HOLD_TICKS)));
    }

    private PowerNetwork.Island island() {
        return PowerNetwork.scan(level, worldPosition);
    }

    private void finish(boolean met) {
        if (met) {
            completed++;
            com.criticalmass.registry.CMTriggers.near(level, worldPosition, 128, "order_met");
        } else {
            missed++;
        }
        level.playSound(null, worldPosition, met ? CMSounds.ORDER_MET.get() : CMSounds.ORDER_MISSED.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        order = 0;
        orderTicks = 0;
        heldTicks = 0;
        idleTicks = 0;
        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        syncIn = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (event != Event.NORMAL && --eventTicks <= 0) {
            disturb(Event.NORMAL, 0, 0);
        }
        pu += Math.max(-Power.DIP_RATE, Math.min(Power.DIP_RATE, (event == Event.DIP ? depth : 1.0) - pu));
        offset += Math.max(-Power.SWING_RATE, Math.min(Power.SWING_RATE, (event == Event.SWING ? depth : 0.0) - offset));
        phase = Power.wrap(phase + offset * 360.0 / 20.0);
        PowerNetwork.Island around = island();
        double total = 0;
        if (live()) {
            total = -around.demand(level);
            for (GeneratorBlockEntity g : around.machines(level)) {
                if (g.synced()) {
                    total += g.outputSu();
                }
            }
        }
        delivered += (total - delivered) * 0.2;
        if (Math.abs(delivered) < 1e-3) {
            delivered = 0;
        }
        double size = contractMw();
        if (order <= 0) {
            if (size > 0 && idleTicks++ >= Power.ORDER_GAP_TICKS) {
                int steps = Math.max(1, (int) Math.round(size / Power.ORDER_STEP_MW * (0.4 + 0.6 * level.random.nextDouble())));
                placeOrder(steps * Power.ORDER_STEP_MW);
                level.playSound(null, worldPosition, CMSounds.ORDER_PLACED.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        } else {
            orderTicks++;
            int before = comparator();
            boolean within = Math.abs(deliveredMw() - order) <= order * Power.ORDER_MARGIN;
            heldTicks = within ? heldTicks + 1 : 0;
            if (heldTicks >= Power.ORDER_HOLD_TICKS) {
                finish(true);
            } else if (orderTicks >= Power.ORDER_DEADLINE_TICKS) {
                finish(false);
            } else if (before != comparator()) {
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }
        if (syncIn-- <= 0) {
            syncIn = SYNC_TICKS;
            setChanged();
            sendData();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("Delivered", delivered);
        tag.putDouble("Order", order);
        tag.putInt("OrderTicks", orderTicks);
        tag.putInt("HeldTicks", heldTicks);
        tag.putInt("IdleTicks", idleTicks);
        tag.putInt("Completed", completed);
        tag.putInt("Missed", missed);
        tag.putInt("Event", event.ordinal());
        tag.putInt("EventTicks", eventTicks);
        tag.putDouble("Depth", depth);
        tag.putDouble("Pu", pu);
        tag.putDouble("Offset", offset);
        tag.putDouble("Phase", phase);
        tag.put("Wires", wires.save());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        delivered = tag.getDouble("Delivered");
        order = tag.getDouble("Order");
        orderTicks = tag.getInt("OrderTicks");
        heldTicks = tag.getInt("HeldTicks");
        idleTicks = tag.getInt("IdleTicks");
        completed = tag.getInt("Completed");
        missed = tag.getInt("Missed");
        event = Event.values()[Math.max(0, Math.min(Event.values().length - 1, tag.getInt("Event")))];
        eventTicks = tag.getInt("EventTicks");
        depth = tag.getDouble("Depth");
        pu = tag.contains("Pu") ? tag.getDouble("Pu") : 1.0;
        offset = tag.getDouble("Offset");
        phase = tag.getDouble("Phase");
        wires.load(tag, "Wires");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        if (event == Event.OUTAGE) {
            ThermalTooltips.status(tooltip, "criticalmass.grid.lost", ChatFormatting.RED);
        } else if (event == Event.DIP) {
            ThermalTooltips.status(tooltip, "criticalmass.grid.dipping", ChatFormatting.GOLD);
        } else if (event == Event.SWING) {
            ThermalTooltips.status(tooltip, "criticalmass.grid.swinging", ChatFormatting.GOLD);
        } else if (order > 0) {
            ThermalTooltips.status(tooltip, "criticalmass.grid.order", ChatFormatting.GOLD, ThermalTooltips.number(order), (Power.ORDER_DEADLINE_TICKS - orderTicks) / 20);
        } else {
            ThermalTooltips.status(tooltip, "criticalmass.grid.idle", ChatFormatting.GRAY);
        }
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.grid_supply");
        ThermalTooltips.value(tooltip, live() ? ThermalTooltips.reading("criticalmass.unit.kilovolts", ChatFormatting.AQUA, ThermalTooltips.number(kilovolts()))
                .append(ThermalTooltips.aside("criticalmass.goggles.aside.at_hertz", String.format(java.util.Locale.ROOT, "%.2f", hertz()))) : ThermalTooltips.reading("criticalmass.breaker.dead", ChatFormatting.GRAY));
        ThermalTooltips.label(tooltip, delivered < 0 ? "criticalmass.goggles.label.drawn" : "criticalmass.goggles.label.delivered");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.megawatts", ChatFormatting.AQUA, ThermalTooltips.number(Math.abs(deliveredMw()))));
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.orders");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.grid.tally", ChatFormatting.AQUA, completed, missed));
        return true;
    }

    static class Contract extends ScrollValueBehaviour {
        Contract(SmartBlockEntity be) {
            super(Component.translatable("criticalmass.grid.contract"), be, new ContractBox());
            between(0, Power.ORDER_MAX_STEPS);
            withFormatter(step -> step == 0 ? "-" : step * Power.ORDER_STEP_MW + " MW");
        }

        @Override
        public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
            return new ValueSettingsBoard(label, max, 10, ImmutableList.of(Component.translatable("criticalmass.grid.contract_row")),
                    new ValueSettingsFormatter(settings -> Component.literal(settings.value() * Power.ORDER_STEP_MW + " MW")));
        }
    }

    static class ContractBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 6, 15.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction.getAxis().isHorizontal();
        }
    }
}
