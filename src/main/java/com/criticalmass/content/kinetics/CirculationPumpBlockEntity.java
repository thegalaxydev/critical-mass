package com.criticalmass.content.kinetics;

import com.criticalmass.content.effects.Effects;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolantCircuit;
import com.criticalmass.content.thermal.LoadedThermalBlocks;
import com.criticalmass.content.thermal.PipeCoolant;
import com.criticalmass.content.thermal.TankCoolant;
import com.criticalmass.content.thermal.ThermalNode;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMSounds;
import com.criticalmass.registry.CMStress;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

public class CirculationPumpBlockEntity extends KineticBlockEntity {
    public static final String CAVITATING = "criticalmass.circuit.cavitating";
    private CoolantCircuit circuit;
    private int retraceIn;
    private int flow;
    private int movedTotal;
    private String status = "criticalmass.circuit.idle";
    private int syncCooldown;
    private boolean cavitating;
    private boolean syncPending;
    private final LerpedFloat wheelSpeed = LerpedFloat.linear().chase(0, 1 / 64f, LerpedFloat.Chaser.EXP);
    private float wheelAngle;
    private double wear;
    private double leakDebt;
    private double leakRate;
    private long leaked;

    public CirculationPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(new Pipework(this));
    }

    private static final class Pipework extends FluidTransportBehaviour {
        Pipework(SmartBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            return state.getBlock() instanceof CirculationPumpBlock && direction.getAxis() == state.getValue(CirculationPumpBlock.FACING).getAxis();
        }

        @Override
        public boolean canPullFluidFrom(FluidStack fluid, BlockState state, Direction direction) {
            return false;
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        LoadedThermalBlocks.add(this);
    }

    @Override
    public void invalidate() {
        LoadedThermalBlocks.remove(this);
        super.invalidate();
    }

    public int flow() {
        return flow;
    }

    public double wear() {
        return wear;
    }

    public void setWear(double value) {
        wear = Math.max(0, Math.min(1, value));
        syncPending = true;
        setChanged();
    }

    public boolean leaking() {
        return wear >= CMThermal.SEAL_LEAK_AT;
    }

    public double leakRate() {
        return leakRate;
    }

    public long leaked() {
        return leaked;
    }

    public static double leakAt(double wear) {
        return wear < CMThermal.SEAL_LEAK_AT ? 0 : CMThermal.SEAL_LEAK_MAX * (wear - CMThermal.SEAL_LEAK_AT) / (1 - CMThermal.SEAL_LEAK_AT);
    }

    public net.minecraft.world.ItemInteractionResult reseal(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.player.Player player) {
        if (!stack.is(com.criticalmass.registry.CMItems.SEALING_GASKET.get()) || wear < 0.01 || level == null) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        if (getSpeed() != 0) {
            player.hurt(level.damageSources().generic(), CMThermal.SEAL_HURT);
            player.displayClientMessage(Component.translatable("criticalmass.pump.turning"), true);
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        setWear(wear - CMThermal.SEAL_STEP);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, worldPosition, CMSounds.SERVICE.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.1f);
        player.displayClientMessage(Component.translatable("criticalmass.service.done", com.criticalmass.content.power.Wear.percent(wear)), true);
        return net.minecraft.world.ItemInteractionResult.SUCCESS;
    }

    private void seep() {
        double rate = circuit != null && circuit.ok() ? leakAt(wear) : 0;
        double now = 0;
        if (rate > 0) {
            leakDebt = Math.min(leakDebt + rate, CMThermal.SEAL_LEAK_MAX * 2);
            int want = (int) leakDebt;
            for (ThermalNode node : pipeContents()) {
                if (!(node instanceof PipeCoolant pipe) || pipe.coolant().amount() == 0) {
                    continue;
                }
                now = rate;
                int n = Math.min(want, pipe.coolant().amount());
                if (n > 0) {
                    leakDebt -= n;
                    com.criticalmass.content.thermal.CoolantBuffer.Packet lost = pipe.coolant().take(n);
                    boolean primary = CMThermal.isPrimaryMedium(lost.fluid());
                    pipe.sinks().vent(lost);
                    leaked += n;
                    if (primary && level instanceof net.minecraft.server.level.ServerLevel server) {
                        com.criticalmass.content.radiation.ContaminationData.get(server).add(worldPosition, (n - lost.activity()) * CMThermal.CLEAN_SPILL + lost.activity() * CMThermal.CONTAMINATED_SPILL);
                    }
                    if (level.getGameTime() % 10 == 0) {
                        com.criticalmass.content.thermal.ThermalSafety.steamPuff(level, worldPosition, 0, 0.2, 0);
                    }
                }
                break;
            }
        } else {
            leakDebt = 0;
        }
        if (now != leakRate) {
            leakRate = now;
            syncPending = true;
        }
    }

    public int movedTotal() {
        return movedTotal;
    }

    public String status() {
        return status;
    }

    public boolean cavitating() {
        return cavitating;
    }

    public float wheelAngle(float partialTicks) {
        return wheelAngle + wheelSpeed.getValue(partialTicks) * 3 / 10f * partialTicks;
    }

    public List<ThermalNode> pipeContents() {
        List<ThermalNode> out = new ArrayList<>();
        if (circuit == null || level == null) {
            return out;
        }
        for (CoolantCircuit.Hop h : circuit.passThrough) {
            ThermalNode p = PipeCoolant.at(level, h.pos());
            if (p == null) {
                p = TankCoolant.at(level, h.pos());
            }
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    @org.jetbrains.annotations.Nullable
    public CoolantCircuit circuit() {
        return circuit;
    }

    public void invalidateCircuit() {
        retraceIn = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            wheelSpeed.updateChaseTarget(getSpeed());
            wheelSpeed.tickChaser();
            wheelAngle = (wheelAngle + wheelSpeed.getValue() * 3 / 10f) % 360;
            if (getSpeed() != 0 && status.equals(CAVITATING)) {
                Effects.loop(level, worldPosition, CMSounds.PUMP_CAVITATION.get(), 0.8f, 0.8f + Math.min(1f, Math.abs(getSpeed()) / 128f) * 0.4f);
            }
            return;
        }
        if (circuit == null || retraceIn-- <= 0) {
            circuit = CoolantCircuit.trace(level, worldPosition, getBlockState().getValue(CirculationPumpBlock.FACING));
            retraceIn = 20;
        }
        int newFlow = 0;
        boolean starved = false;
        String newStatus;
        if (!circuit.ok()) {
            newStatus = circuit.error;
        } else if (getSpeed() == 0) {
            newStatus = isOverStressed() ? "criticalmass.circuit.overstressed" : "criticalmass.circuit.no_rotation";
        } else {
            double steam = circuit.intakeSteam(level);
            double share = steam <= CMThermal.CAVITATION_FROM ? 1 : Math.max(CMThermal.CAVITATION_FLOOR, 1 - steam);
            CoolantCircuit.Flow result = circuit.transfer(level, Math.max(1, (int) Math.round(CMThermal.pumpFlow(getSpeed()) * share)));
            newFlow = result.moved();
            movedTotal += newFlow;
            starved = share < 1;
            newStatus = result.error() != null ? result.error() : starved ? CAVITATING : "criticalmass.circuit.running";
            if (result.error() != null) {
                retraceIn = Math.min(retraceIn, 5);
            }
        }
        cavitating = starved;
        boolean wasFlowing = flow > 0;
        if (newFlow != flow || !newStatus.equals(status)) {
            flow = newFlow;
            status = newStatus;
            syncPending = true;
        }
        if (wasFlowing != (flow > 0) && hasNetwork()) {
            getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
            getOrCreateNetwork().updateStress();
        }
        if (flow > 0) {
            wear = Math.min(1, wear + CMThermal.SEAL_WEAR * (starved ? CMThermal.SEAL_CAVITATION : 1));
            if (level.getGameTime() % 200 == 0) {
                syncPending = true;
            }
        }
        seep();
        if (syncCooldown > 0) {
            syncCooldown--;
        } else if (syncPending) {
            syncPending = false;
            syncCooldown = 10;
            setChanged();
            sendData();
        }
    }

    @Override
    public float calculateStressApplied() {
        float speed = Math.abs(getSpeed());
        float share = flow > 0 && speed > 0 ? Math.min(speed, CMThermal.pumpCapRpm()) / speed : CMThermal.PUMP_IDLE_STRESS;
        float impact = (float) CMStress.CIRCULATION_PUMP_IMPACT * share;
        lastStressApplied = impact;
        return impact;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Flow", flow);
        tag.putInt("Moved", movedTotal);
        tag.putDouble("Wear", wear);
        tag.putDouble("LeakRate", leakRate);
        if (clientPacket) {
            tag.putString("Status", status);
        } else {
            tag.putDouble("LeakDebt", leakDebt);
            tag.putLong("Leaked", leaked);
        }
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        com.criticalmass.content.power.Wear.collect(components, wear);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        wear = com.criticalmass.content.power.Wear.read(input.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Wear");
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        flow = tag.getInt("Flow");
        movedTotal = tag.getInt("Moved");
        wear = tag.getDouble("Wear");
        leakRate = tag.getDouble("LeakRate");
        if (clientPacket) {
            status = tag.getString("Status");
        } else {
            leakDebt = tag.getDouble("LeakDebt");
            leaked = tag.getLong("Leaked");
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.kinetic(tooltip, super.addToGoggleTooltip(tooltip, isPlayerSneaking));
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ThermalTooltips.status(tooltip, status, flow > 0 && !status.equals(CAVITATING) ? ChatFormatting.GREEN : ChatFormatting.GOLD);
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.flow");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.flow", flow > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY, ThermalTooltips.number(flow)));
        com.criticalmass.content.power.Wear.goggles(tooltip, wear);
        if (leakRate > 0) {
            ThermalTooltips.label(tooltip, "criticalmass.goggles.label.seal_leak");
            ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.flow", ChatFormatting.RED, String.format(java.util.Locale.ROOT, "%.1f", leakRate)));
        }
        return true;
    }
}
