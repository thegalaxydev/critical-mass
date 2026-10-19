package com.criticalmass.content.kinetics;

import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.LoadedThermalBlocks;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMStress;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ControlRodDriveBlockEntity extends KineticBlockEntity {
    private double insertion = 1.0;
    private double target = 1.0;
    private boolean moving;
    private boolean failSafe;
    private int reactorTripTicks;
    private int syncCooldown;

    public ControlRodDriveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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

    public double insertion() {
        return insertion;
    }

    public double target() {
        return target;
    }

    public boolean moving() {
        return moving;
    }

    public boolean failSafe() {
        return failSafe;
    }

    public void setInsertion(double value) {
        insertion = Math.max(0, Math.min(1, value));
    }

    public void reactorTripped() {
        reactorTripTicks = 3;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (reactorTripTicks > 0) {
            reactorTripTicks--;
        }
        target = 1.0 - level.getBestNeighborSignal(worldPosition) / 15.0;
        boolean latched = getSpeed() != 0 && !isOverStressed() && reactorTripTicks == 0;
        boolean wasMoving = moving;
        failSafe = !latched && insertion < 1.0;
        if (!latched) {
            moving = false;
            if (insertion < 1.0) {
                insertion = Math.min(1.0, insertion + 1.0 / CMThermal.ROD_FAILSAFE_TICKS);
            }
        } else {
            double rate = Math.min(Math.abs(getSpeed()) * CMThermal.ROD_RATE_PER_RPM, CMThermal.ROD_MAX_RATE);
            double gap = target - insertion;
            moving = Math.abs(gap) > 1e-9;
            if (moving) {
                insertion += Math.signum(gap) * Math.min(Math.abs(gap), rate);
            }
        }
        if (wasMoving != moving && hasNetwork()) {
            getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
            getOrCreateNetwork().updateStress();
        }
        if (syncCooldown-- <= 0 || wasMoving != moving) {
            syncCooldown = 10;
            setChanged();
            sendData();
        }
    }

    @Override
    public float calculateStressApplied() {
        float impact = moving ? (float) CMStress.CONTROL_ROD_DRIVE_IMPACT : 0f;
        lastStressApplied = impact;
        return impact;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("Insertion", insertion);
        tag.putDouble("Target", target);
        tag.putBoolean("Moving", moving);
        tag.putBoolean("FailSafe", failSafe);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        insertion = tag.contains("Insertion") ? tag.getDouble("Insertion") : 1.0;
        target = tag.contains("Target") ? tag.getDouble("Target") : 1.0;
        moving = tag.getBoolean("Moving");
        failSafe = tag.getBoolean("FailSafe");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    ").append(Component.translatable("criticalmass.goggles.rod_drive")));
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.rod_position", ChatFormatting.GRAY, Math.round(insertion * 100), Math.round(target * 100)));
        String state = failSafe ? "criticalmass.rod.fail_safe" : moving ? "criticalmass.rod.moving" : "criticalmass.rod.holding";
        tooltip.add(ThermalTooltips.line(state, failSafe ? ChatFormatting.RED : moving ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        return true;
    }
}
