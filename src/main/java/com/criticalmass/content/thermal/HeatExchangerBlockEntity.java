package com.criticalmass.content.thermal;

import com.criticalmass.content.HorizontalBlock;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class HeatExchangerBlockEntity extends SmartBlockEntity implements ThermalNode, IHaveGoggleInformation {
    private final CoolantBuffer primary = new CoolantBuffer(CMThermal.EXCHANGER_CAPACITY, this::changed);
    private final CoolantBuffer secondary = new CoolantBuffer(CMThermal.EXCHANGER_CAPACITY, this::changed);
    private int primaryFlowTicks;
    private int secondaryFlowTicks;
    private double lastExchange;
    private double exchanged;
    private boolean syncPending;
    private final ThermalSinks sinks = new ThermalSinks();
    private int syncCooldown;
    private boolean stubsChecked;
    private double wear;
    private double leakDebt;
    private double leakRate;
    private double leaked;
    public static final int MAX_BANK = 8;

    public HeatExchangerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    private Direction.Axis primaryAxis() {
        return getBlockState().getValue(HorizontalBlock.FACING).getAxis();
    }

    @Override
    public CoolantBuffer coolant() {
        return primary;
    }

    public CoolantBuffer primary() {
        return primary;
    }

    public CoolantBuffer secondary() {
        return secondary;
    }

    public double exchanged() {
        return exchanged;
    }

    public double lastExchange() {
        return lastExchange;
    }

    public double wear() {
        return wear;
    }

    public void setWear(double value) {
        wear = Math.max(0, Math.min(1, value));
        changed();
        setChanged();
    }

    public boolean leaking() {
        return wear >= CMThermal.TUBE_LEAK_AT;
    }

    public double leakRate() {
        return leakRate;
    }

    public double leaked() {
        return leaked;
    }

    public static double leakAt(double wear) {
        return wear < CMThermal.TUBE_LEAK_AT ? 0 : CMThermal.TUBE_LEAK_MAX * (wear - CMThermal.TUBE_LEAK_AT) / (1 - CMThermal.TUBE_LEAK_AT);
    }

    public boolean cool() {
        return primaryFlowTicks <= 0 && secondaryFlowTicks <= 0 && primary.temperature() <= CMThermal.TUBE_COOL && secondary.temperature() <= CMThermal.TUBE_COOL;
    }

    public net.minecraft.world.ItemInteractionResult retube(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.player.Player player) {
        if (!stack.is(com.criticalmass.registry.CMItems.REACTOR_ALLOY_SHEET.get()) || wear < 0.01 || level == null) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        if (!cool()) {
            player.hurt(level.damageSources().hotFloor(), CMThermal.TUBE_HURT);
            player.displayClientMessage(Component.translatable("criticalmass.exchanger.hot"), true);
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        setWear(wear - CMThermal.TUBE_STEP);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, worldPosition, com.criticalmass.registry.CMSounds.SERVICE.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 0.9f);
        player.displayClientMessage(Component.translatable("criticalmass.service.done", com.criticalmass.content.power.Wear.percent(wear)), true);
        return net.minecraft.world.ItemInteractionResult.SUCCESS;
    }

    private void leak() {
        double rate = leakAt(wear);
        if (rate <= 0) {
            leakDebt = 0;
            leakRate = 0;
            return;
        }
        net.neoforged.neoforge.fluids.FluidStack water = new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1);
        boolean coolant = CMThermal.isPrimaryMedium(primary.fluid());
        if (primary.amount() == 0 || !(coolant || primary.steamMedium()) || !secondary.canMix(water)) {
            leakDebt = 0;
            leakRate = 0;
            return;
        }
        leakDebt = Math.min(leakDebt + rate, CMThermal.TUBE_LEAK_MAX * 2);
        int n = Math.min((int) leakDebt, Math.min(primary.amount(), secondary.space()));
        leakRate = secondary.space() > 0 ? rate : 0;
        if (n <= 0) {
            return;
        }
        leakDebt -= n;
        CoolantBuffer.Packet lost = primary.take(n);
        double activity = coolant ? Math.max(lost.activity(), n * CMThermal.LEAK_ACTIVITY) : lost.activity();
        secondary.give(new CoolantBuffer.Packet(water.copyWithAmount(n), lost.heat(), 0, activity));
        leaked += n;
    }

    @Override
    public ThermalSinks sinks() {
        return sinks;
    }

    @Override
    public List<CoolantBuffer> buffers() {
        return List.of(primary, secondary);
    }

    @Override
    @Nullable
    public Direction passThroughExit(Direction enteredFrom) {
        if (enteredFrom.getAxis() == Direction.Axis.Y || enteredFrom.getAxis() == primaryAxis()) {
            return enteredFrom.getOpposite();
        }
        return null;
    }

    public List<BlockPos> route(Direction enteredFrom) {
        if (passThroughExit(enteredFrom) == null) {
            return null;
        }
        BlockState state = getBlockState();
        if (level == null || !(state.getBlock() instanceof HeatExchangerBlock block) || enteredFrom.getAxis() == block.bankAxis(state)) {
            return List.of(worldPosition);
        }
        Direction out = enteredFrom.getOpposite();
        if (NodeBlocks.piped(level, worldPosition, out)) {
            return List.of(worldPosition);
        }
        Direction along = Direction.fromAxisAndDirection(block.bankAxis(state), Direction.AxisDirection.POSITIVE);
        boolean forward = true;
        boolean backward = true;
        for (int reach = 1; reach < MAX_BANK && (forward || backward); reach++) {
            for (int sign = 1; sign >= -1; sign -= 2) {
                if (!(sign > 0 ? forward : backward)) {
                    continue;
                }
                BlockPos at = worldPosition.relative(along, sign * reach);
                if (!level.isLoaded(at) || !block.sameBank(state, level.getBlockState(at))) {
                    if (sign > 0) {
                        forward = false;
                    } else {
                        backward = false;
                    }
                    continue;
                }
                if (NodeBlocks.piped(level, at, out)) {
                    List<BlockPos> path = new ArrayList<>();
                    for (int i = 0; i <= reach; i++) {
                        path.add(worldPosition.relative(along, sign * i));
                    }
                    return path;
                }
            }
        }
        return List.of(worldPosition);
    }

    @Override
    public CoolantBuffer bufferFor(Direction enteredFrom) {
        return enteredFrom.getAxis() == Direction.Axis.Y ? secondary : primary;
    }

    @Override
    public void onCoolantPassed(Direction enteredFrom, int amount) {
        if (amount <= 0) {
            return;
        }
        if (enteredFrom.getAxis() == Direction.Axis.Y) {
            secondaryFlowTicks = 2;
        } else {
            primaryFlowTicks = 2;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (!stubsChecked && level.hasChunksAt(worldPosition.offset(-1, -1, -1), worldPosition.offset(1, 1, 1))) {
            stubsChecked = true;
            if (getBlockState().getBlock() instanceof HeatExchangerBlock block) {
                BlockState joined = block.withStubs(level, worldPosition, getBlockState());
                if (joined != getBlockState()) {
                    level.setBlock(worldPosition, joined, Block.UPDATE_ALL);
                }
            }
        }
        ThermalSafety.ruptureIfNeeded(level, worldPosition, primary, sinks);
        ThermalSafety.ruptureIfNeeded(level, worldPosition, secondary, sinks);
        double q = 0;
        if (primaryFlowTicks > 0 && secondaryFlowTicks > 0 && primary.amount() > 0 && secondary.amount() > 0) {
            double cp = primary.amount() * CMThermal.HEAT_CAPACITY;
            double cs = secondary.amount() * CMThermal.HEAT_CAPACITY;
            double gap = primary.temperature() - secondary.temperature();
            double surface = CMThermal.EXCHANGER_RATE * CMThermal.EXCHANGER_CAPACITY * CMThermal.HEAT_CAPACITY / 2;
            double reach = CMThermal.EXCHANGER_REACH * cp * cs / (cp + cs);
            q = gap * Math.min(surface, reach);
            if (q > 0) {
                q = primary.removeHeat(q);
                secondary.addHeat(q);
            } else if (q < 0) {
                q = -secondary.removeHeat(-q);
                primary.addHeat(-q);
            }
        }
        boolean worn = false;
        if (q != 0) {
            wear = Math.min(1, wear + CMThermal.TUBE_WEAR * Math.min(1, Math.abs(primary.temperature() - secondary.temperature()) / CMThermal.TUBE_STRESS_GAP));
            worn = true;
        } else if (primaryFlowTicks > 0 && primary.amount() > 0 && secondary.amount() == 0 && primary.temperature() > CMThermal.BOILING) {
            wear = Math.min(1, wear + CMThermal.TUBE_DRY_WEAR);
            worn = true;
        }
        if (worn && level.getGameTime() % 200 == 0) {
            changed();
        }
        leak();
        if (primaryFlowTicks > 0) {
            primaryFlowTicks--;
        }
        if (secondaryFlowTicks > 0) {
            secondaryFlowTicks--;
        }
        lastExchange = q;
        exchanged += q;
        if (syncCooldown > 0) {
            syncCooldown--;
        } else if (syncPending) {
            syncPending = false;
            syncCooldown = 20;
            setChanged();
            sendData();
        }
    }

    private void changed() {
        syncPending = true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Primary", primary.save(registries));
        tag.put("Secondary", secondary.save(registries));
        tag.putDouble("Exchanged", exchanged);
        tag.putDouble("LastExchange", lastExchange);
        tag.put("Sinks", sinks.save());
        tag.putDouble("Wear", wear);
        tag.putDouble("LeakRate", leakRate);
        if (!clientPacket) {
            tag.putDouble("LeakDebt", leakDebt);
            tag.putDouble("Leaked", leaked);
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
        primary.load(tag.getCompound("Primary"), registries);
        secondary.load(tag.getCompound("Secondary"), registries);
        exchanged = tag.getDouble("Exchanged");
        lastExchange = tag.getDouble("LastExchange");
        sinks.load(tag.getCompound("Sinks"));
        wear = tag.getDouble("Wear");
        leakRate = tag.getDouble("LeakRate");
        if (!clientPacket) {
            leakDebt = tag.getDouble("LeakDebt");
            leaked = tag.getDouble("Leaked");
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.exchange");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.heat_rate", lastExchange != 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY, ThermalTooltips.number(lastExchange)));
        ThermalTooltips.coolant(tooltip, "criticalmass.goggles.label.primary", primary);
        ThermalTooltips.coolant(tooltip, "criticalmass.goggles.label.secondary", secondary);
        com.criticalmass.content.power.Wear.goggles(tooltip, wear);
        if (leakRate > 0) {
            ThermalTooltips.label(tooltip, "criticalmass.goggles.label.tube_leak");
            ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.flow", ChatFormatting.RED, String.format(java.util.Locale.ROOT, "%.1f", leakRate)));
        }
        return true;
    }
}
