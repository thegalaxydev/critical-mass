package com.criticalmass.content.kinetics;

import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolantBuffer;
import com.criticalmass.content.thermal.LoadedThermalBlocks;
import com.criticalmass.content.thermal.ThermalNode;
import com.criticalmass.content.thermal.ThermalSafety;
import com.criticalmass.content.thermal.ThermalSinks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SteamTurbineBlockEntity extends GeneratingKineticBlockEntity implements ThermalNode {
    private final CoolantBuffer coolant = new CoolantBuffer(CMThermal.TURBINE_CAPACITY, this::setChanged);
    private double heatRate;
    private double heatExtracted;
    private double appliedCapacity;
    private double admission;
    private double upstreamBypass;
    private int bypassTicks;
    private String status = "criticalmass.turbine.no_steam";
    private final ThermalSinks sinks = new ThermalSinks();
    private int updateCooldown;
    private int syncCooldown;

    public SteamTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
    public CoolantBuffer coolant() {
        return coolant;
    }

    @Override
    @Nullable
    public Direction passThroughExit(Direction enteredFrom) {
        return enteredFrom.getAxis() == Direction.Axis.Y ? enteredFrom.getOpposite() : null;
    }

    public double heatRate() {
        return heatRate;
    }

    public double heatExtracted() {
        return heatExtracted;
    }

    public double stressCapacity() {
        return appliedCapacity;
    }

    public double admission() {
        return admission;
    }

    public String status() {
        return status;
    }

    @Override
    public ThermalSinks sinks() {
        return sinks;
    }

    @Override
    public void setUpstreamBypass(double fraction) {
        upstreamBypass = fraction;
        bypassTicks = 3;
    }

    public boolean shaftConnected() {
        if (level == null) {
            return false;
        }
        Direction.Axis axis = getBlockState().getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS);
        for (Direction d : new Direction[]{Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE), Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE)}) {
            if (level.getBlockEntity(worldPosition.relative(d)) instanceof KineticBlockEntity) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (ThermalSafety.ruptureIfNeeded(level, worldPosition, coolant, sinks)) {
            status = "criticalmass.turbine.ruptured";
        }
        if (bypassTicks > 0) {
            bypassTicks--;
        } else {
            upstreamBypass = 0;
        }
        boolean connected = shaftConnected();
        double governor = level.getBestNeighborSignal(worldPosition) / 15.0;
        admission = connected ? (1 - governor) * (1 - upstreamBypass) : 0;
        double steam = coolant.steamEnergy();
        if (!connected) {
            status = "criticalmass.turbine.disconnected";
        } else if (steam <= 0) {
            status = "criticalmass.turbine.no_steam";
        } else if (upstreamBypass >= 1) {
            status = "criticalmass.turbine.bypassed";
        } else if (governor >= 1) {
            status = "criticalmass.turbine.governed";
        } else {
            status = "criticalmass.turbine.ok";
        }
        double extracted = steam > 0 ? coolant.removeHeat(steam * CMThermal.TURBINE_EXTRACTION * admission) : 0;
        heatExtracted += extracted;
        heatRate += (extracted - heatRate) * 0.1;
        if (heatRate < 1e-3) {
            heatRate = 0;
        }
        double target = heatRate * CMThermal.SU_PER_HEAT;
        if (target < CMThermal.TURBINE_MIN_SU) {
            target = 0;
        }
        if (updateCooldown > 0) {
            updateCooldown--;
        } else if (Math.abs(target - appliedCapacity) > Math.max(16.0, appliedCapacity * 0.02)) {
            appliedCapacity = target;
            updateCooldown = 10;
            updateGeneratedRotation();
            sendData();
        }
        if (syncCooldown-- <= 0) {
            syncCooldown = 20;
            if (!hasSource() && getSpeed() != getGeneratedSpeed()) {
                updateGeneratedRotation();
            }
            sendData();
        }
    }

    @Override
    public float getGeneratedSpeed() {
        return appliedCapacity > 0 ? CMThermal.TURBINE_RPM : 0;
    }

    @Override
    public float calculateAddedStressCapacity() {
        float perRpm = appliedCapacity > 0 ? (float) (appliedCapacity / CMThermal.TURBINE_RPM) : 0;
        lastCapacityProvided = perRpm;
        return perRpm;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Coolant", coolant.save(registries));
        tag.putDouble("HeatRate", heatRate);
        tag.putDouble("HeatExtracted", heatExtracted);
        tag.putDouble("Capacity", appliedCapacity);
        tag.put("Sinks", sinks.save());
        tag.putDouble("Admission", admission);
        tag.putString("Status", status);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        coolant.load(tag.getCompound("Coolant"), registries);
        heatRate = tag.getDouble("HeatRate");
        heatExtracted = tag.getDouble("HeatExtracted");
        appliedCapacity = tag.getDouble("Capacity");
        sinks.load(tag.getCompound("Sinks"));
        admission = tag.getDouble("Admission");
        status = tag.getString("Status");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    ").append(Component.translatable("criticalmass.goggles.turbine")));
        tooltip.add(ThermalTooltips.line(status, status.endsWith(".ok") ? ChatFormatting.GREEN : ChatFormatting.GOLD));
        ThermalTooltips.coolant(tooltip, coolant);
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.admission", ChatFormatting.GRAY, Math.round(admission * 100)));
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.heat_rate", ChatFormatting.GOLD, ThermalTooltips.number(heatRate)));
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.capacity", appliedCapacity > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY, ThermalTooltips.number(appliedCapacity)));
        return true;
    }
}
