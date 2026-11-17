package com.criticalmass.content.power;

import com.criticalmass.content.effects.Effects;
import com.criticalmass.registry.CMSounds;
import com.criticalmass.registry.CMParticles;
import net.minecraft.server.level.ServerLevel;
import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import java.util.List;
import java.util.Locale;
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

public class TransformerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, WireNode {
    public static final int HIGH = 0;
    public static final int LOW = 1;
    public static final double[][] RATIOS = {{Power.GRID_KV, Power.GENERATOR_KV}, {Power.GRID_KV, Power.MOTOR_KV}, {Power.GENERATOR_KV, Power.MOTOR_KV}};
    private static final int SYNC_TICKS = 10;
    private static final int STALE_TICKS = 20;
    private final WireLinks wires = new WireLinks();
    private ScrollValueBehaviour ratio;
    private double through;
    private long carriedAt = -1;
    private double heat;
    private double highSide;
    private double lowSide;
    private int syncIn;
    private int tap;

    public TransformerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        ratio = new Winding(this);
        ratio.withCallback(i -> {
            if (level != null && !level.isClientSide) {
                PowerNetwork.changed(level);
            }
        });
        behaviours.add(ratio);
    }

    private static String pair(int index) {
        double[] kv = RATIOS[Math.max(0, Math.min(RATIOS.length - 1, index))];
        return (int) kv[0] + " / " + (int) kv[1] + " kV";
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

    private Direction highSideOf() {
        return getBlockState().getValue(HorizontalBlock.FACING).getClockWise();
    }

    @Override
    public Vec3 terminal(int index) {
        Direction side = index == HIGH ? highSideOf() : highSideOf().getOpposite();
        return new Vec3(0.5 + side.getStepX() * 0.25, index == HIGH ? 1.0 : 0.875, 0.5 + side.getStepZ() * 0.25);
    }

    @Override
    public int terminalAt(Vec3 local) {
        Direction side = highSideOf();
        return (local.x - 0.5) * side.getStepX() + (local.z - 0.5) * side.getStepZ() >= 0 ? HIGH : LOW;
    }

    public void setRatio(int index) {
        ratio.setValue(index);
    }

    public double highKilovolts() {
        return RATIOS[ratio.getValue()][0];
    }

    public double lowKilovolts() {
        return RATIOS[ratio.getValue()][1];
    }

    public int tap() {
        return tap;
    }

    public void setTap(int steps) {
        int next = Math.max(-Power.TAP_STEPS, Math.min(Power.TAP_STEPS, steps));
        if (next == tap) {
            return;
        }
        tap = next;
        syncIn = 0;
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, CMSounds.SWITCH_THROW.get(), SoundSource.BLOCKS, 0.8f, 0.8f);
        }
        setChanged();
        sendData();
    }

    public double turns() {
        return lowKilovolts() / highKilovolts() * (1.0 + tap * Power.TAP_STEP);
    }

    private static String offset(int steps) {
        return steps == 0 ? "" : String.format(Locale.ROOT, " %+.2f%%", steps * Power.TAP_STEP * 100);
    }

    public void carry(double megawatts) {
        through = megawatts;
        carriedAt = level == null ? 0 : level.getGameTime();
    }

    public double throughMw() {
        return through;
    }

    public double heat() {
        return heat;
    }

    public double oilTemperature() {
        return 30 + heat * 90;
    }

    public double highSide() {
        return highSide;
    }

    public double lowSide() {
        return lowSide;
    }

    public int comparator() {
        return (int) Math.max(0, Math.min(15, Math.round(heat * 15)));
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            if (highSide > 0 || lowSide > 0) {
                Effects.loop(level, worldPosition, CMSounds.HUM.get(), (float) Math.min(1.0, 0.25 + 0.55 * through / Power.TRANSFORMER_MW + heat * 0.4), (float) (1.0 + heat * 0.12));
            }
            return;
        }
        if (heat > 0.35 && level.getGameTime() % 6 == 0 && level instanceof ServerLevel server) {
            server.sendParticles(CMParticles.OIL_SMOKE.get(), worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, 0, 0.0, 1.0, 0.0, 0.05 + heat * 0.05);
        }
        if (carriedAt < 0 || level.getGameTime() - carriedAt > STALE_TICKS) {
            through = 0;
        }
        int before = comparator();
        double load = through / Power.TRANSFORMER_MW;
        heat = Math.max(0, heat + (load > 1 ? (load * load - 1) / (Power.TRANSFORMER_HEAT_SECONDS * 20) : -1.0 / (Power.TRANSFORMER_COOL_SECONDS * 20)));
        if (heat >= 1) {
            Hazard.destroy(level, worldPosition, 4.0f);
            return;
        }
        if (before != comparator()) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        if (syncIn-- <= 0) {
            syncIn = SYNC_TICKS;
            PowerNetwork.Island around = PowerNetwork.scan(level, worldPosition);
            highSide = around.kilovoltsAt(level, worldPosition, HIGH);
            lowSide = around.kilovoltsAt(level, worldPosition, LOW);
            setChanged();
            sendData();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("Through", through);
        tag.putDouble("Heat", heat);
        tag.putDouble("HighSide", highSide);
        tag.putDouble("LowSide", lowSide);
        tag.putInt("Tap", tap);
        tag.put("Wires", wires.save());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        through = tag.getDouble("Through");
        heat = tag.getDouble("Heat");
        highSide = tag.getDouble("HighSide");
        lowSide = tag.getDouble("LowSide");
        tap = tag.getInt("Tap");
        wires.load(tag, "Wires");
    }

    private static Component side(double kilovolts) {
        return kilovolts > 0 ? ThermalTooltips.reading("criticalmass.unit.kilovolts", ChatFormatting.AQUA, ThermalTooltips.number(kilovolts))
                : ThermalTooltips.reading("criticalmass.breaker.dead", ChatFormatting.GRAY);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        boolean live = highSide > 0 || lowSide > 0;
        ThermalTooltips.status(tooltip, heat > 0.5 ? "criticalmass.transformer.hot" : live ? "criticalmass.transformer.live" : "criticalmass.bus.dead",
                heat > 0.5 ? ChatFormatting.RED : live ? ChatFormatting.GREEN : ChatFormatting.GRAY);
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.high_side");
        ThermalTooltips.value(tooltip, side(highSide));
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.low_side");
        ThermalTooltips.value(tooltip, tap == 0 ? side(lowSide) : side(lowSide).copy().append(ThermalTooltips.aside("criticalmass.goggles.aside.tap", String.format(Locale.ROOT, "%+d", tap))));
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.loading");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.megawatts", ChatFormatting.AQUA, ThermalTooltips.number(through))
                .append(ThermalTooltips.aside("criticalmass.goggles.aside.of_rating", ThermalTooltips.number(Power.TRANSFORMER_MW))));
        return true;
    }

    static class Winding extends ScrollValueBehaviour {
        Winding(SmartBlockEntity be) {
            super(Component.translatable("criticalmass.transformer.ratio"), be, new WindingBox());
            between(0, RATIOS.length - 1);
            withFormatter(index -> (int) RATIOS[index][0] + "/" + (int) RATIOS[index][1]);
        }

        @Override
        public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
            ImmutableList.Builder<Component> rows = ImmutableList.builder();
            for (int i = 0; i < RATIOS.length; i++) {
                rows.add(Component.translatable("criticalmass.transformer.tap_raise", pair(i)));
                rows.add(Component.translatable("criticalmass.transformer.tap_lower", pair(i)));
            }
            return new ValueSettingsBoard(label, Power.TAP_STEPS, 2, rows.build(),
                    new ValueSettingsFormatter(settings -> Component.literal(pair(settings.row() / 2) + offset((settings.row() % 2 == 0 ? 1 : -1) * settings.value()))));
        }

        @Override
        public void setValueSettings(Player player, ValueSettings settings, boolean ctrlHeld) {
            if (settings.equals(getValueSettings())) {
                return;
            }
            setValue(settings.row() / 2);
            if (blockEntity instanceof TransformerBlockEntity transformer) {
                transformer.setTap((settings.row() % 2 == 0 ? 1 : -1) * settings.value());
            }
            playFeedbackSound(this);
        }

        @Override
        public ValueSettings getValueSettings() {
            int tap = blockEntity instanceof TransformerBlockEntity transformer ? transformer.tap() : 0;
            return new ValueSettings(value * 2 + (tap < 0 ? 1 : 0), Math.abs(tap));
        }
    }

    static class WindingBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 5, 15.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction.getAxis() == state.getValue(HorizontalBlock.FACING).getAxis();
        }
    }
}
