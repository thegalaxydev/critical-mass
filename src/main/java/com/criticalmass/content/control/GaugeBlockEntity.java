package com.criticalmass.content.control;

import com.criticalmass.content.thermal.ThermalTooltips;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class GaugeBlockEntity extends BoundPanelBlockEntity {
    private static final int READ_TICKS = 5;
    private ScrollValueBehaviour setpoint;
    private PlantChannel channel = PlantChannel.TEMPERATURE;
    private boolean alarmBelow;
    private boolean valid;
    private double value;
    private double min;
    private double max = 1;
    private String unit = "";
    private boolean alarm;
    private int comparator;
    private int readIn;
    public float shownFraction;

    public GaugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        setpoint = new ScrollValueBehaviour(Component.translatable("criticalmass.gauge.setpoint"), this, new SetpointBox());
        setpoint.between(0, 100);
        setpoint.value = 0;
        setpoint.withFormatter(v -> v == 0 ? "-" : v + "%");
        behaviours.add(setpoint);
    }

    public PlantChannel channel() {
        return channel;
    }

    public void setChannel(PlantChannel value) {
        channel = value;
        readIn = 0;
        changed();
    }

    public void cycleChannel(boolean back) {
        PlantChannel[] all = PlantChannel.values();
        PlantChannel start = channel;
        do {
            channel = all[Math.floorMod(channel.ordinal() + (back ? -1 : 1), all.length)];
        } while (channel != start && target != null && level != null && channel.read(level, target) == null);
        readIn = 0;
        changed();
    }

    public int setpoint() {
        return setpoint == null ? 0 : setpoint.getValue();
    }

    public void setSetpoint(int percent, boolean below) {
        setpoint.setValue(Math.max(0, Math.min(100, percent)));
        alarmBelow = below;
        readIn = 0;
        changed();
    }

    public boolean alarmBelow() {
        return alarmBelow;
    }

    public void toggleAlarmSide() {
        alarmBelow = !alarmBelow;
        readIn = 0;
        changed();
    }

    public boolean valid() {
        return valid;
    }

    public double value() {
        return value;
    }

    public String unit() {
        return unit;
    }

    public float fraction() {
        return valid && max > min ? (float) Math.max(0, Math.min(1, (value - min) / (max - min))) : 0;
    }

    public boolean alarm() {
        return alarm;
    }

    public int comparator() {
        return comparator;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || readIn-- > 0) {
            return;
        }
        readIn = READ_TICKS;
        PlantChannel.Reading r = channel.read(level, target);
        boolean nowValid = r != null;
        double nowValue = r == null ? 0 : r.value();
        double nowMin = r == null ? 0 : r.min();
        double nowMax = r == null ? 1 : r.max();
        String nowUnit = r == null ? channel.unit : r.unit();
        double span = Math.max(1e-9, nowMax - nowMin);
        boolean moved = nowValid != valid || Math.abs(nowValue - value) > span * 0.004 || nowMin != min || nowMax != max || !nowUnit.equals(unit);
        valid = nowValid;
        value = nowValue;
        min = nowMin;
        max = nowMax;
        unit = nowUnit;
        int percent = setpoint();
        float f = fraction();
        boolean nowAlarm = valid && percent > 0 && (alarmBelow ? f * 100 <= percent : f * 100 >= percent);
        int nowComparator = valid ? Math.round(15 * f) : 0;
        if (nowAlarm != alarm || nowComparator != comparator) {
            alarm = nowAlarm;
            comparator = nowComparator;
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            moved = true;
        }
        if (moved) {
            changed();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Channel", channel.ordinal());
        tag.putBoolean("AlarmBelow", alarmBelow);
        tag.putBoolean("Valid", valid);
        tag.putDouble("Value", value);
        tag.putDouble("Min", min);
        tag.putDouble("Max", max);
        tag.putString("Unit", unit);
        tag.putBoolean("Alarm", alarm);
        tag.putInt("Comparator", comparator);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        channel = PlantChannel.values()[Math.floorMod(tag.getInt("Channel"), PlantChannel.values().length)];
        alarmBelow = tag.getBoolean("AlarmBelow");
        valid = tag.getBoolean("Valid");
        value = tag.getDouble("Value");
        min = tag.getDouble("Min");
        max = tag.contains("Max") ? tag.getDouble("Max") : 1;
        unit = tag.getString("Unit");
        alarm = tag.getBoolean("Alarm");
        comparator = tag.getInt("Comparator");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        header(tooltip, "criticalmass.goggles.gauge");
        Component name = Component.translatable(channel.langKey());
        if (!valid) {
            tooltip.add(ThermalTooltips.line("criticalmass.gauge.no_reading", ChatFormatting.GRAY, name));
        } else {
            tooltip.add(ThermalTooltips.line("criticalmass.gauge.reading", alarm ? ChatFormatting.RED : ChatFormatting.AQUA, name, ThermalTooltips.number(value), unit));
            tooltip.add(ThermalTooltips.line("criticalmass.gauge.scale", ChatFormatting.DARK_GRAY, ThermalTooltips.number(min), ThermalTooltips.number(max), unit));
        }
        if (setpoint() > 0) {
            tooltip.add(ThermalTooltips.line(alarmBelow ? "criticalmass.gauge.alarm_below" : "criticalmass.gauge.alarm_above", alarm ? ChatFormatting.RED : ChatFormatting.GRAY, setpoint()));
        }
        tooltip.add(ThermalTooltips.line("criticalmass.gauge.hint", ChatFormatting.DARK_GRAY));
        return true;
    }

    static class SetpointBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 3.5, 3.5);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction == state.getValue(HorizontalDirectionalBlock.FACING);
        }
    }
}
