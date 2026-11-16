package com.criticalmass.content.power;

import com.criticalmass.content.thermal.ThermalTooltips;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class GeneratorBlockEntity extends KineticBlockEntity implements WireNode {
    private final WireLinks wires = new WireLinks();
    private static final int UPDATE_TICKS = 5;
    private static final int SYNC_TICKS = 10;
    private static final int RESCAN_TICKS = 40;
    private static final int LOAD_STEPS = 50;
    private LoadSetting setting;
    private double appliedPower;
    private double load;
    private double trim;
    private double phase;
    private double frequency;
    private double output;
    private boolean synced;
    private boolean syncPermit;
    private int sectionCount = 1;
    private String status = "criticalmass.generator.stopped";
    private PowerNetwork.Island island;
    private int islandVersion = -1;
    private int rescanIn;
    private int updateCooldown;
    private int syncCooldown;

    public GeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        setting = new LoadSetting(this);
        setting.withCallback(step -> master().setLoad(step / (double) LOAD_STEPS));
        behaviours.add(setting);
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
        return axis(getBlockState()).isVertical() ? new Vec3(0.5, 0.5, 0.875) : new Vec3(0.5, 0.875, 0.5);
    }

    private static Direction.Axis axis(BlockState state) {
        return state.getValue(RotatedPillarKineticBlock.AXIS);
    }

    private static boolean section(Level level, BlockPos pos, Direction.Axis axis) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof GeneratorBlock && axis(state) == axis;
    }

    public static BlockPos masterPos(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GeneratorBlock)) {
            return pos;
        }
        Direction.Axis axis = axis(state);
        Direction back = Direction.get(Direction.AxisDirection.NEGATIVE, axis);
        BlockPos at = pos;
        for (int i = 1; i < Power.MAX_SECTIONS && section(level, at.relative(back), axis); i++) {
            at = at.relative(back);
        }
        return at;
    }

    public static List<BlockPos> sections(Level level, BlockPos master) {
        List<BlockPos> out = new ArrayList<>();
        BlockState state = level.getBlockState(master);
        if (!(state.getBlock() instanceof GeneratorBlock)) {
            return out;
        }
        Direction.Axis axis = axis(state);
        Direction forward = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        BlockPos at = master;
        for (int i = 0; i < Power.MAX_SECTIONS && section(level, at, axis); i++) {
            out.add(at);
            at = at.relative(forward);
        }
        return out;
    }

    public GeneratorBlockEntity master() {
        if (level == null) {
            return this;
        }
        return level.getBlockEntity(masterPos(level, worldPosition)) instanceof GeneratorBlockEntity g ? g : this;
    }

    public boolean isMaster() {
        return level != null && worldPosition.equals(masterPos(level, worldPosition));
    }

    public int sectionCount() {
        return sectionCount;
    }

    public double ratedSu() {
        return sectionCount * Power.SECTION_RATED_SU;
    }

    public double load() {
        return load;
    }

    public void setLoad(double fraction) {
        load = Math.max(0, Math.min(1, fraction));
        setting.value = (int) Math.round(load * LOAD_STEPS);
        updateCooldown = 0;
        setChanged();
        sendData();
    }

    public double trim() {
        return trim;
    }

    public void raise() {
        if (synced) {
            setLoad(load + Power.LOAD_STEP);
        } else {
            trim = Math.min(Power.TRIM_MAX, trim + Power.TRIM_STEP);
            setChanged();
        }
    }

    public void lower() {
        if (synced) {
            setLoad(load - Power.LOAD_STEP);
        } else {
            trim = Math.max(-Power.TRIM_MAX, trim - Power.TRIM_STEP);
            setChanged();
        }
    }

    public boolean synced() {
        return synced;
    }

    public void permitSync() {
        syncPermit = true;
    }

    public boolean energised() {
        return frequency > 0;
    }

    public double frequency() {
        return frequency;
    }

    public double phase() {
        return phase;
    }

    public void setPhase(double degrees) {
        phase = Power.wrap(degrees);
    }

    public double outputSu() {
        return output;
    }

    public String status() {
        return status;
    }

    private PowerNetwork.Island island() {
        int version = PowerNetwork.version(level);
        if (island == null || version != islandVersion || rescanIn-- <= 0) {
            island = PowerNetwork.scan(level, worldPosition);
            islandVersion = version;
            rescanIn = RESCAN_TICKS;
        }
        return island;
    }

    private double headroom(List<GeneratorBlockEntity> parts) {
        if (!hasNetwork()) {
            return 0;
        }
        KineticNetwork network = getOrCreateNetwork();
        double speed = Math.abs(getTheoreticalSpeed());
        double mine = 0;
        for (GeneratorBlockEntity part : parts) {
            mine += part.lastStressApplied * speed;
        }
        return Math.max(0, network.calculateCapacity() - (network.calculateStress() - mine));
    }

    private void apply(double total, List<GeneratorBlockEntity> parts) {
        double share = parts.isEmpty() ? 0 : total / parts.size();
        boolean changed = false;
        for (GeneratorBlockEntity part : parts) {
            if (Math.abs(share - part.appliedPower) > Math.max(2.0, part.appliedPower * 0.02) || (share == 0 && part.appliedPower != 0)) {
                part.appliedPower = share;
                if (part.hasNetwork()) {
                    part.getOrCreateNetwork().updateStressFor(part, part.calculateStressApplied());
                    changed = true;
                }
                part.setChanged();
            }
        }
        if (changed && hasNetwork()) {
            getOrCreateNetwork().updateStress();
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
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || !isMaster()) {
            return;
        }
        List<GeneratorBlockEntity> parts = new ArrayList<>();
        for (BlockPos pos : sections(level, worldPosition)) {
            if (level.getBlockEntity(pos) instanceof GeneratorBlockEntity part) {
                parts.add(part);
            }
        }
        boolean resized = parts.size() != sectionCount;
        sectionCount = Math.max(1, parts.size());
        float speed = Math.abs(getSpeed());
        boolean running = speed > 0 && !isOverStressed();
        double base = Power.GRID_HZ * speed / Power.SYNC_RPM;
        boolean grid = !island().grids.isEmpty();
        boolean was = synced;
        String now;
        double wanted = 0;
        if (!running) {
            if (grid && synced) {
                PowerNetwork.tripNearest(level, worldPosition, "criticalmass.breaker.trip.reverse_power");
            }
            synced = false;
            frequency = 0;
            now = "criticalmass.generator.stopped";
        } else if (grid) {
            if (!synced && !syncPermit) {
                PowerNetwork.tripNearest(level, worldPosition, "criticalmass.breaker.trip.not_synchronised");
                frequency = base + trim;
                now = "criticalmass.generator.running";
            } else if (Math.abs(base - Power.GRID_HZ) > Power.OUT_OF_STEP_HZ) {
                PowerNetwork.tripNearest(level, worldPosition, "criticalmass.breaker.trip.out_of_step");
                synced = false;
                frequency = base + trim;
                now = "criticalmass.generator.running";
            } else {
                if (!synced) {
                    synced = true;
                    trim = 0;
                    phase = 0;
                    load = 0;
                    setting.value = 0;
                }
                frequency = Power.GRID_HZ;
                wanted = load * ratedSu();
                now = "criticalmass.generator.synchronised";
            }
        } else {
            synced = false;
            frequency = base + trim;
            phase = Power.wrap(phase + (frequency - Power.GRID_HZ) * 360.0 / 20.0);
            now = "criticalmass.generator.running";
        }
        syncPermit = false;
        if (updateCooldown-- <= 0 || was != synced) {
            updateCooldown = UPDATE_TICKS;
            apply(running ? Math.min(wanted, headroom(parts) * 0.98) : 0, parts);
        }
        int shown = (int) Math.round(load * LOAD_STEPS);
        double total = 0;
        for (GeneratorBlockEntity part : parts) {
            total += part.appliedPower;
            if (part.setting.value != shown) {
                part.setting.value = shown;
                part.setChanged();
                part.sendData();
            }
        }
        output = synced ? total : 0;
        boolean changed = !now.equals(status) || was != synced || resized;
        status = now;
        if (changed || syncCooldown-- <= 0) {
            syncCooldown = SYNC_TICKS;
            setChanged();
            sendData();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("Applied", appliedPower);
        tag.putDouble("Load", load);
        tag.putDouble("Trim", trim);
        tag.putDouble("Phase", phase);
        tag.putDouble("Frequency", frequency);
        tag.putDouble("Output", output);
        tag.putBoolean("Synced", synced);
        tag.putInt("Sections", sectionCount);
        tag.putString("Status", status);
        tag.put("Wires", wires.save());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        appliedPower = tag.getDouble("Applied");
        load = tag.getDouble("Load");
        trim = tag.getDouble("Trim");
        phase = tag.getDouble("Phase");
        frequency = tag.getDouble("Frequency");
        output = tag.getDouble("Output");
        synced = tag.getBoolean("Synced");
        sectionCount = Math.max(1, tag.getInt("Sections"));
        status = tag.contains("Status") ? tag.getString("Status") : "criticalmass.generator.stopped";
        wires.load(tag, "Wires");
        if (setting != null) {
            setting.value = (int) Math.round(load * LOAD_STEPS);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.kinetic(tooltip, super.addToGoggleTooltip(tooltip, isPlayerSneaking));
        GeneratorBlockEntity m = master();
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ThermalTooltips.status(tooltip, m.status, m.synced ? ChatFormatting.GREEN : m.energised() ? ChatFormatting.GOLD : ChatFormatting.GRAY);
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.electrical_output");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.megawatts", ChatFormatting.AQUA, ThermalTooltips.number(Power.megawatts(m.output)))
                .append(ThermalTooltips.aside("criticalmass.goggles.aside.of_megawatts", ThermalTooltips.number(Power.megawatts(m.load * m.ratedSu())))));
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.frequency");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.hertz", ChatFormatting.AQUA, String.format(java.util.Locale.ROOT, "%.2f", m.frequency)));
        return true;
    }

    static class LoadSetting extends ScrollValueBehaviour {
        LoadSetting(SmartBlockEntity be) {
            super(Component.translatable("criticalmass.generator.load"), be, new LoadBox());
            between(0, LOAD_STEPS);
            withFormatter(step -> step * 100 / LOAD_STEPS + "%");
        }

        @Override
        public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
            return new ValueSettingsBoard(label, max, 5, ImmutableList.of(Component.translatable("criticalmass.generator.load_row")),
                    new ValueSettingsFormatter(settings -> Component.literal(settings.value() * 100 / LOAD_STEPS + "%")));
        }
    }

    static class LoadBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 14.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction.getAxis().isHorizontal() && direction.getAxis() != axis(state);
        }
    }
}
