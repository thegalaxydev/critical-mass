package com.criticalmass.content.kinetics;

import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMConfig;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import java.util.List;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class GeneratorBlockEntity extends KineticBlockEntity {
    public static final int FE_PER_STEP = 8;
    public static final int MAX_STEPS = 256;
    private static final int BUFFER_TICKS = 40;
    private static final int MIN_BUFFER = 4000;
    private static final int UPDATE_TICKS = 10;
    private ScrollValueBehaviour load;
    private final IEnergyStorage output = new Output();
    private int stored;
    private double fraction;
    private double appliedPower;
    private double outputRate;
    private double generated;
    private double shaftWork;
    private boolean breakerOpen;
    private boolean breakerLatched;
    private int updateCooldown;
    private int syncCooldown;

    public GeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        load = new ScrollValueBehaviour(Component.translatable("criticalmass.generator.load"), this, new LoadBox());
        load.between(0, MAX_STEPS);
        load.value = 64;
        load.withFormatter(v -> v * FE_PER_STEP + " FE/t");
        behaviours.add(load);
    }

    public IEnergyStorage output() {
        return output;
    }

    public int setting() {
        return load == null ? 0 : load.getValue() * FE_PER_STEP;
    }

    public void setSetting(int fePerTick) {
        load.setValue(Math.max(0, Math.min(MAX_STEPS, fePerTick / FE_PER_STEP)));
    }

    public int stored() {
        return stored;
    }

    public int capacity() {
        return Math.max(MIN_BUFFER, setting() * BUFFER_TICKS);
    }

    public double outputRate() {
        return outputRate;
    }

    public double shaftPower() {
        return appliedPower;
    }

    public double generated() {
        return generated;
    }

    public double shaftWork() {
        return shaftWork;
    }

    public void setBreakerLatched(boolean open) {
        breakerLatched = open;
        updateCooldown = 0;
        setChanged();
    }

    public boolean breakerOpen() {
        return breakerOpen;
    }

    private static double fePerSu() {
        return CMConfig.get(CMConfig.FE_PER_SU) * CMConfig.get(CMConfig.GENERATOR_EFFICIENCY);
    }

    private double wantedPower() {
        double signal = level.getBestNeighborSignal(worldPosition) / 15.0;
        breakerOpen = signal >= 1 || breakerLatched;
        double target = breakerLatched ? 0 : setting() * (1 - signal);
        double fill = stored / (double) capacity();
        double throttle = fill >= 0.9 ? 0 : fill <= 0.5 ? 1 : (0.9 - fill) / 0.4;
        double per = fePerSu();
        return per <= 0 ? 0 : target * throttle / per;
    }

    private double headroom() {
        if (!hasNetwork()) {
            return 0;
        }
        KineticNetwork network = getOrCreateNetwork();
        double mine = lastStressApplied * Math.abs(getTheoreticalSpeed());
        return Math.max(0, network.calculateCapacity() - (network.calculateStress() - mine));
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        float speed = Math.abs(getTheoreticalSpeed());
        if (updateCooldown-- <= 0) {
            updateCooldown = UPDATE_TICKS;
            double wanted = wantedPower();
            double want = speed > 0 ? Math.min(wanted, headroom() * 0.98) : 0;
            if (Math.abs(want - appliedPower) > Math.max(2.0, appliedPower * 0.02)) {
                appliedPower = want;
                if (hasNetwork()) {
                    getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
                    getOrCreateNetwork().updateStress();
                }
            }
        }
        double produced = 0;
        if (getSpeed() != 0 && !isOverStressed() && appliedPower > 0) {
            shaftWork += appliedPower;
            fraction += appliedPower * fePerSu();
            int whole = (int) Math.floor(fraction);
            int room = capacity() - stored;
            int add = Math.min(whole, Math.max(0, room));
            fraction -= whole;
            stored += add;
            generated += add;
            produced = add;
        }
        outputRate += (produced - outputRate) * 0.1;
        if (outputRate < 1e-3) {
            outputRate = 0;
        }
        push();
        if (syncCooldown-- <= 0) {
            syncCooldown = 10;
            setChanged();
            sendData();
        }
    }

    private void push() {
        if (stored <= 0) {
            return;
        }
        Direction shaft = GeneratorBlock.inputSide(getBlockState());
        for (Direction d : Direction.values()) {
            if (d == shaft || stored <= 0) {
                continue;
            }
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.relative(d), d.getOpposite());
            if (target != null && target.canReceive()) {
                stored -= target.receiveEnergy(stored, false);
            }
        }
    }

    @Override
    public float calculateStressApplied() {
        float speed = Math.abs(getTheoreticalSpeed());
        float impact = speed > 0 ? (float) (appliedPower / speed) : 0f;
        lastStressApplied = impact;
        return impact;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Stored", stored);
        tag.putDouble("Fraction", fraction);
        tag.putDouble("Applied", appliedPower);
        tag.putDouble("Rate", outputRate);
        tag.putDouble("Generated", generated);
        tag.putDouble("ShaftWork", shaftWork);
        tag.putBoolean("BreakerOpen", breakerOpen);
        tag.putBoolean("BreakerLatched", breakerLatched);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        stored = tag.getInt("Stored");
        fraction = tag.getDouble("Fraction");
        appliedPower = tag.getDouble("Applied");
        outputRate = tag.getDouble("Rate");
        generated = tag.getDouble("Generated");
        shaftWork = tag.getDouble("ShaftWork");
        breakerOpen = tag.getBoolean("BreakerOpen");
        breakerLatched = tag.getBoolean("BreakerLatched");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    ").append(Component.translatable("criticalmass.goggles.generator")));
        if (breakerOpen) {
            tooltip.add(ThermalTooltips.line("criticalmass.generator.breaker_open", ChatFormatting.GOLD));
        }
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.generator_output", outputRate > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY, ThermalTooltips.number(outputRate), setting()));
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.shaft_load", ChatFormatting.GRAY, ThermalTooltips.number(appliedPower)));
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.generator_stored", ChatFormatting.GRAY, stored, capacity()));
        return true;
    }

    static class LoadBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 16.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction == state.getValue(GeneratorBlock.HORIZONTAL_FACING);
        }
    }

    private final class Output implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int n = Math.max(0, Math.min(toExtract, stored));
            if (!simulate && n > 0) {
                stored -= n;
                setChanged();
            }
            return n;
        }

        @Override
        public int getEnergyStored() {
            return stored;
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }
}
