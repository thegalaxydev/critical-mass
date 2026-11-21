package com.criticalmass.content.radiation;

import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMSounds;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import java.util.List;
import java.util.Locale;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class AreaMonitorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int DEFAULT_LEVEL = 100;
    public static final int ROW_MAX = 100;
    public static final int COARSE = 10;
    public static final int MAX_LEVEL = ROW_MAX * COARSE;
    public static final double CLEAR = 0.9;
    public static final int SAMPLE_TICKS = 20;
    public static final int HORN_TICKS = 40;
    public static final double DIAL_LOW = 0.01;
    public static final double DIAL_DECADES = 4;
    public static final float SWEEP = 60f;
    public static final String NORMAL = "criticalmass.monitor.normal";
    public static final String ALARM = "criticalmass.monitor.alarm";
    private static final double SEND_SHARE = 0.02;
    private static final double SEND_FLOOR = 0.005;
    private static final float NEEDLE_EASE = 0.2f;
    private static final float NEEDLE_SHAKE = 1.5f;
    private static final int CLICKS_PER_TICK = 2;
    private static final float CLICK_VOLUME = 0.3f;
    private ScrollValueBehaviour level_;
    private double rate;
    private double sent = -1;
    private boolean silenced;
    private boolean due = true;
    private int analog;
    private float needle = -SWEEP;
    private float lastNeedle = -SWEEP;

    public AreaMonitorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        level_ = new AlarmLevel(this);
        level_.withCallback(v -> due = true);
        behaviours.add(level_);
    }

    public ScrollValueBehaviour alarmLevel() {
        return level_;
    }

    public double rate() {
        return rate;
    }

    public double setpoint() {
        return level_.getValue() / 10.0;
    }

    public boolean alarm() {
        return getBlockState().getValue(AreaMonitorBlock.ALARM);
    }

    public boolean silenced() {
        return silenced;
    }

    public String status() {
        return alarm() ? ALARM : NORMAL;
    }

    public int analog() {
        return alarm() ? 15 : Mth.clamp((int) Math.floor(15 * rate / setpoint()), 0, 14);
    }

    public boolean canSilence() {
        return alarm() && !silenced;
    }

    public void silence() {
        silenced = true;
        setChanged();
    }

    public float needle(float partialTicks) {
        return Mth.lerp(partialTicks, lastNeedle, needle);
    }

    public static float dial(double rate) {
        double share = (Math.log10(Math.max(DIAL_LOW, rate)) - Math.log10(DIAL_LOW)) / DIAL_DECADES;
        return (float) (-SWEEP + 2 * SWEEP * Mth.clamp(share, 0, 1));
    }

    public Vec3 detector() {
        Vec3 at = worldPosition.getCenter();
        SubLevelAccess body = SableCompanion.INSTANCE.getContaining(this);
        return body == null ? at : body.logicalPose().transformPosition(at);
    }

    public void sample() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        due = false;
        rate = Radiation.ambientRate(server, detector());
        BlockState state = getBlockState();
        boolean was = state.getValue(AreaMonitorBlock.ALARM);
        boolean is = rate >= setpoint() || was && rate >= setpoint() * CLEAR;
        if (is != was) {
            if (!is) {
                silenced = false;
            }
            server.playSound(null, worldPosition, CMSounds.RELAY_CLICK.get(), SoundSource.BLOCKS, 0.6f, is ? 1.3f : 0.9f);
            server.setBlock(worldPosition, state.setValue(AreaMonitorBlock.ALARM, is), Block.UPDATE_ALL);
            server.updateNeighborsAt(AreaMonitorBlock.support(state, worldPosition), state.getBlock());
        }
        int now = analog();
        if (now != analog) {
            analog = now;
            server.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
        if (is != was || sent < 0 || Math.abs(rate - sent) > Math.max(SEND_FLOOR, sent * SEND_SHARE)) {
            sent = rate;
            setChanged();
            sendData();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (!level.isClientSide) {
            long now = level.getGameTime() + worldPosition.asLong();
            if (due || Math.floorMod(now, SAMPLE_TICKS) == 0) {
                sample();
            }
            if (alarm() && !silenced && Math.floorMod(now, HORN_TICKS) == 0) {
                level.playSound(null, worldPosition, CMSounds.ALARM.get(), SoundSource.BLOCKS, 0.5f, 1.3f);
            }
            return;
        }
        lastNeedle = needle;
        float target = dial(rate) + (rate > DIAL_LOW ? (level.random.nextFloat() - 0.5f) * NEEDLE_SHAKE : 0);
        needle += (target - needle) * NEEDLE_EASE;
        if (isVirtual()) {
            return;
        }
        double expected = RadiationEvents.clicks(rate) / 20.0;
        for (int i = 0; i < CLICKS_PER_TICK && expected > 0; i++, expected -= 1) {
            if (level.random.nextDouble() < expected) {
                level.playLocalSound(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, CMSounds.GEIGER.get(), SoundSource.BLOCKS,
                        CLICK_VOLUME, 0.94f + level.random.nextFloat() * 0.12f, false);
            }
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("Rate", rate);
        tag.putBoolean("Silenced", silenced);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        rate = tag.getDouble("Rate");
        silenced = tag.getBoolean("Silenced");
    }

    public static String shown(double value) {
        return value < RadiationEvents.FINE_BELOW ? String.format(Locale.ROOT, "%.2f", value) : ThermalTooltips.number(value);
    }

    private static String level(int tenths) {
        return tenths % 10 == 0 && tenths >= ROW_MAX ? String.valueOf(tenths / 10) : String.format(Locale.ROOT, "%.1f", tenths / 10.0);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ThermalTooltips.status(tooltip, status(), alarm() ? ChatFormatting.RED : ChatFormatting.GREEN);
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.dose_rate");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.dose_rate", alarm() ? ChatFormatting.RED : ChatFormatting.AQUA, shown(rate))
                .append(ThermalTooltips.aside("criticalmass.goggles.aside.alarms_at", level(level_.getValue()))));
        return true;
    }

    static class AlarmLevel extends ScrollValueBehaviour {
        AlarmLevel(SmartBlockEntity be) {
            super(Component.translatable("criticalmass.monitor.level"), be, new LevelBox());
            between(1, MAX_LEVEL);
            value = DEFAULT_LEVEL;
            withFormatter(AreaMonitorBlockEntity::level);
        }

        @Override
        public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
            return new ValueSettingsBoard(label, ROW_MAX, 10, ImmutableList.of(Component.translatable("criticalmass.monitor.level_fine"), Component.translatable("criticalmass.monitor.level_coarse")),
                    new ValueSettingsFormatter(settings -> Component.translatable("criticalmass.unit.dose_rate", level(tenths(settings)))));
        }

        private static int tenths(ValueSettings settings) {
            return Math.max(1, settings.value()) * (settings.row() == 0 ? 1 : COARSE);
        }

        @Override
        public void setValueSettings(Player player, ValueSettings settings, boolean ctrlHeld) {
            int tenths = tenths(settings);
            if (tenths == value) {
                return;
            }
            setValue(tenths);
            playFeedbackSound(this);
        }

        @Override
        public ValueSettings getValueSettings() {
            return value <= ROW_MAX ? new ValueSettings(0, value) : new ValueSettings(1, Math.round(value / (float) COARSE));
        }
    }

    static class LevelBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 5.5, 4.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction == state.getValue(AreaMonitorBlock.FACING);
        }
    }
}
