package com.criticalmass.content.control;

import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class ScreenBlockEntity extends KineticBlockEntity {
    public static final int LINES_PER_BLOCK = 3;
    public static final int CHARACTERS_PER_BLOCK = 5;
    public static final int CAPACITY = 400;
    public static final int MAX_SPAN = 16;
    public static final int BOARD_WIDTH = 9;
    public static final int BOARD_HEIGHT = 5;
    public static final int HISTORY = 120;
    public static final int SAMPLE_TICKS = 20;
    public static final int EVENT_LIMIT = 5;
    public static final List<PlantChannel> ROWS = List.of(PlantChannel.NEUTRON, PlantChannel.POWER, PlantChannel.PERIOD, PlantChannel.REACTIVITY, PlantChannel.TEMPERATURE,
            PlantChannel.COOLANT_TEMPERATURE, PlantChannel.PRESSURE, PlantChannel.FLOW, PlantChannel.RODS_A, PlantChannel.XENON, PlantChannel.BORON);
    private static final int SCAN_TICKS = 10;
    private static final int LINE_LIMIT = 160;

    public enum Trend {
        POWER("power", PlantChannel.NEUTRON, 120), PRESSURE("pressure", PlantChannel.PRESSURE, 20), COOLANT("coolant", PlantChannel.COOLANT_TEMPERATURE, 200), XENON("xenon", PlantChannel.XENON, 100);

        public final String key;
        public final PlantChannel channel;
        public final float floor;

        Trend(String key, PlantChannel channel, float floor) {
            this.key = key;
            this.channel = channel;
            this.floor = floor;
        }
    }

    public record Event(long time, String key, String about) {
        public Component text() {
            return about.isEmpty() ? Component.translatable(key) : Component.translatable(key, Component.translatable(about));
        }
    }

    private int energy;
    private boolean powered;
    private boolean controller = true;
    private int width = 1;
    private int height = 1;
    private int scanIn;
    private final List<String> lines = new ArrayList<>();
    @Nullable
    private BlockPos reactor;
    @Nullable
    private BlockPos turbine;
    private int sampleIn;
    private int samples;
    private final float[][] history = new float[Trend.values().length][HISTORY];
    private final float[] scale = new float[Trend.values().length];
    private final byte[][] plot = new byte[Trend.values().length][0];
    private String status = "";
    private final List<String> values = new ArrayList<>();
    private String turbineSpeed = "";
    private String output = "";
    private final List<Event> events = new ArrayList<>();
    private int lastAlarms;
    public ScreenBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public void charge() {
        energy = CAPACITY;
    }

    public Direction facing() {
        return getBlockState().getValue(ScreenBlock.FACING);
    }

    public boolean powered() {
        return powered;
    }

    public boolean isController() {
        return controller;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public List<String> lines() {
        return lines;
    }

    public boolean linked() {
        return reactor != null;
    }

    public boolean bigEnough() {
        return width >= BOARD_WIDTH && height >= BOARD_HEIGHT;
    }

    @Nullable
    public BlockPos reactor() {
        return reactor;
    }

    public boolean hasTurbine() {
        return turbine != null;
    }

    public String status() {
        return status;
    }

    public List<String> values() {
        return values;
    }

    public String speed() {
        return turbineSpeed;
    }

    public String output() {
        return output;
    }

    public List<Event> events() {
        return events;
    }

    public int samples() {
        return samples;
    }

    public float scale(Trend trend) {
        return scale[trend.ordinal()];
    }

    public byte[] plot(Trend trend) {
        return plot[trend.ordinal()];
    }

    public float latest(Trend trend) {
        return samples == 0 ? 0 : history[trend.ordinal()][samples - 1];
    }

    private static boolean joins(LevelAccessor level, BlockPos pos, Direction facing) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof ScreenBlock && state.getValue(ScreenBlock.FACING) == facing;
    }

    public static Direction left(Direction facing) {
        return facing.getClockWise();
    }

    public static BlockPos controllerPos(LevelAccessor level, BlockPos pos, Direction facing) {
        BlockPos p = pos;
        for (int i = 0; i < MAX_SPAN && joins(level, p.above(), facing); i++) {
            p = p.above();
        }
        for (int i = 0; i < MAX_SPAN && joins(level, p.relative(left(facing)), facing); i++) {
            p = p.relative(left(facing));
        }
        return p;
    }

    @Nullable
    public static ScreenBlockEntity controllerOf(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ScreenBlock)) {
            return null;
        }
        return level.getBlockEntity(controllerPos(level, pos, state.getValue(ScreenBlock.FACING))) instanceof ScreenBlockEntity s ? s : null;
    }

    public boolean link(BlockPos pos) {
        if (level == null) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ReactorControllerBlockEntity) {
            if (!pos.equals(reactor)) {
                clearReadings();
            }
            reactor = pos.immutable();
        } else if (be instanceof SteamTurbineBlockEntity) {
            turbine = pos.immutable();
        } else {
            return false;
        }
        sampleIn = 0;
        setChanged();
        sendData();
        return true;
    }

    public void unlink() {
        reactor = null;
        turbine = null;
        clearReadings();
        setChanged();
        sendData();
    }

    private void clearReadings() {
        samples = 0;
        status = "";
        values.clear();
        turbineSpeed = "";
        output = "";
        events.clear();
        lastAlarms = 0;
    }

    private void adopt(ScreenBlockEntity other) {
        if (reactor == null && other.reactor != null) {
            reactor = other.reactor;
            events.addAll(other.events);
        }
        if (turbine == null) {
            turbine = other.turbine;
        }
        other.reactor = null;
        other.turbine = null;
        other.clearReadings();
        sampleIn = 0;
        setChanged();
        sendData();
    }

    private void scan() {
        Direction facing = facing();
        boolean nowController = controllerPos(level, worldPosition, facing).equals(worldPosition);
        int nowWidth = 1;
        int nowHeight = 1;
        if (nowController) {
            while (nowWidth < MAX_SPAN && joins(level, worldPosition.relative(left(facing).getOpposite(), nowWidth), facing)) {
                nowWidth++;
            }
            while (nowHeight < MAX_SPAN && joins(level, worldPosition.below(nowHeight), facing)) {
                nowHeight++;
            }
        } else if (reactor != null || turbine != null) {
            ScreenBlockEntity main = controllerOf(level, worldPosition);
            if (main != null && main != this) {
                main.adopt(this);
            }
        }
        if (nowController != controller || nowWidth != width || nowHeight != height) {
            controller = nowController;
            width = nowWidth;
            height = nowHeight;
            if (!controller) {
                lines.clear();
            }
            while (lines.size() > height * LINES_PER_BLOCK) {
                lines.remove(lines.size() - 1);
            }
            setChanged();
            sendData();
        }
    }

    public void setLine(int index, String text) {
        if (index < 0 || index >= height * LINES_PER_BLOCK) {
            return;
        }
        while (lines.size() <= index) {
            lines.add("");
        }
        String clipped = text.length() > LINE_LIMIT ? text.substring(0, LINE_LIMIT) : text;
        if (!clipped.equals(lines.get(index))) {
            lines.set(index, clipped);
            setChanged();
            sendData();
        }
    }

    private static String shown(@Nullable PlantChannel.Reading r) {
        return r == null ? "--" : PlantDisplaySources.number(r.value()) + (r.unit().isEmpty() ? "" : " " + r.unit());
    }

    private void event(String key, String about) {
        events.add(0, new Event(level.getGameTime(), key, about));
        while (events.size() > EVENT_LIMIT) {
            events.remove(events.size() - 1);
        }
    }

    public void sample() {
        if (level == null || reactor == null || !level.isLoaded(reactor)) {
            return;
        }
        String nowStatus = "criticalmass.screen.no_reactor";
        int alarms = 0;
        values.clear();
        if (level.getBlockEntity(reactor) instanceof ReactorControllerBlockEntity c) {
            nowStatus = c.isTripped() && !c.tripReason().isEmpty() ? "criticalmass.reactor.trip." + c.tripReason() + ".short" : c.status() + ".short";
            alarms = PlantAlarms.evaluate(c);
            for (PlantChannel channel : ROWS) {
                values.add(shown(channel.read(level, reactor)));
            }
            if (samples == HISTORY) {
                for (float[] series : history) {
                    System.arraycopy(series, 1, series, 0, HISTORY - 1);
                }
                samples--;
            }
            for (Trend trend : Trend.values()) {
                PlantChannel.Reading r = trend.channel.read(level, reactor);
                history[trend.ordinal()][samples] = r == null ? 0 : (float) Math.max(0, r.value());
            }
            samples++;
        }
        if (!nowStatus.equals(status)) {
            if (!status.isEmpty()) {
                event(nowStatus, "");
            }
            status = nowStatus;
        }
        for (PlantAlarms.Alarm a : PlantAlarms.Alarm.values()) {
            boolean was = (lastAlarms & 1 << a.ordinal()) != 0;
            boolean is = (alarms & 1 << a.ordinal()) != 0;
            if (a != PlantAlarms.Alarm.TRIP && was != is) {
                event(is ? a.langKey() : "criticalmass.screen.event.cleared", is ? "" : a.langKey());
            }
        }
        lastAlarms = alarms;
        turbineSpeed = turbine == null ? "" : shown(PlantChannel.SPEED.read(level, turbine));
        output = turbine == null ? "" : shown(PlantChannel.OUTPUT.read(level, turbine));
        setChanged();
        sendData();
    }

    private static float ceiling(float value, float floor) {
        float top = Math.max(value * 1.1f, floor);
        double magnitude = Math.pow(10, Math.floor(Math.log10(top)));
        double lead = top / magnitude;
        return (float) ((lead <= 1 ? 1 : lead <= 2 ? 2 : lead <= 5 ? 5 : 10) * magnitude);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (scanIn-- <= 0) {
            scanIn = SCAN_TICKS;
            scan();
        }
        if (getSpeed() != 0) {
            charge();
        } else if (energy > 0) {
            energy--;
        }
        for (Direction d : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(d)) instanceof ScreenBlockEntity other && other.energy < energy - 1) {
                int moved = (energy - other.energy) / 2;
                energy -= moved;
                other.energy += moved;
                other.setChanged();
            }
        }
        boolean now = energy > 0;
        if (now != powered) {
            powered = now;
            sendData();
        }
        if (controller && powered && reactor != null && sampleIn-- <= 0) {
            sampleIn = SAMPLE_TICKS - 1;
            sample();
        }
        setChanged();
    }

    private static void putPos(CompoundTag tag, String key, @Nullable BlockPos pos) {
        if (pos != null) {
            tag.putIntArray(key, new int[]{pos.getX(), pos.getY(), pos.getZ()});
        }
    }

    @Nullable
    private static BlockPos getPos(CompoundTag tag, String key) {
        int[] at = tag.getIntArray(key);
        return at.length == 3 ? new BlockPos(at[0], at[1], at[2]) : null;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Energy", energy);
        tag.putBoolean("Powered", powered);
        tag.putBoolean("Controller", controller);
        tag.putInt("Width", width);
        tag.putInt("Height", height);
        ListTag list = new ListTag();
        for (String line : lines) {
            list.add(StringTag.valueOf(line));
        }
        tag.put("Lines", list);
        putPos(tag, "Reactor", reactor);
        putPos(tag, "Turbine", turbine);
        ListTag log = new ListTag();
        for (Event e : events) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Time", e.time());
            entry.putString("Key", e.key());
            entry.putString("About", e.about());
            log.add(entry);
        }
        tag.put("Events", log);
        tag.putString("Status", status);
        tag.putInt("Alarms", lastAlarms);
        if (clientPacket && reactor != null) {
            ListTag shown = new ListTag();
            for (String value : values) {
                shown.add(StringTag.valueOf(value));
            }
            tag.put("Values", shown);
            tag.putString("TurbineSpeed", turbineSpeed);
            tag.putString("Output", output);
            tag.putInt("Samples", samples);
            for (Trend trend : Trend.values()) {
                float[] series = history[trend.ordinal()];
                float top = 0;
                for (int i = 0; i < samples; i++) {
                    top = Math.max(top, series[i]);
                }
                float max = ceiling(top, trend.floor);
                byte[] packed = new byte[samples];
                for (int i = 0; i < samples; i++) {
                    packed[i] = (byte) Math.round(Math.min(1, series[i] / max) * 255);
                }
                tag.putFloat("Scale" + trend.ordinal(), max);
                tag.putFloat("Latest" + trend.ordinal(), samples == 0 ? 0 : series[samples - 1]);
                tag.putByteArray("Trend" + trend.ordinal(), packed);
            }
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        energy = tag.getInt("Energy");
        powered = tag.getBoolean("Powered");
        controller = !tag.contains("Controller") || tag.getBoolean("Controller");
        width = Math.max(1, tag.getInt("Width"));
        height = Math.max(1, tag.getInt("Height"));
        lines.clear();
        for (Tag t : tag.getList("Lines", Tag.TAG_STRING)) {
            lines.add(t.getAsString());
        }
        reactor = getPos(tag, "Reactor");
        turbine = getPos(tag, "Turbine");
        events.clear();
        for (Tag t : tag.getList("Events", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            events.add(new Event(entry.getLong("Time"), entry.getString("Key"), entry.getString("About")));
        }
        status = tag.getString("Status");
        lastAlarms = tag.getInt("Alarms");
        if (clientPacket) {
            values.clear();
            for (Tag t : tag.getList("Values", Tag.TAG_STRING)) {
                values.add(t.getAsString());
            }
            turbineSpeed = tag.getString("TurbineSpeed");
            output = tag.getString("Output");
            samples = tag.getInt("Samples");
            for (Trend trend : Trend.values()) {
                scale[trend.ordinal()] = tag.getFloat("Scale" + trend.ordinal());
                plot[trend.ordinal()] = tag.getByteArray("Trend" + trend.ordinal());
                if (samples > 0 && samples <= HISTORY) {
                    history[trend.ordinal()][samples - 1] = tag.getFloat("Latest" + trend.ordinal());
                }
            }
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("criticalmass.goggles.screen")));
        tooltip.add(powered ? ThermalTooltips.line("criticalmass.screen.powered", ChatFormatting.GREEN) : ThermalTooltips.line("criticalmass.screen.unpowered", ChatFormatting.GOLD));
        ScreenBlockEntity main = level == null ? null : controllerOf(level, worldPosition);
        if (main != null) {
            tooltip.add(ThermalTooltips.line("criticalmass.screen.size", ChatFormatting.GRAY, main.width, main.height, main.height * LINES_PER_BLOCK, main.width * CHARACTERS_PER_BLOCK));
            if (main.reactor == null) {
                tooltip.add(ThermalTooltips.line("criticalmass.screen.unlinked", ChatFormatting.DARK_GRAY, BOARD_WIDTH, BOARD_HEIGHT));
            } else if (main.bigEnough()) {
                tooltip.add(ThermalTooltips.line("criticalmass.screen.linked", ChatFormatting.GREEN, main.reactor.getX(), main.reactor.getY(), main.reactor.getZ()));
            } else {
                tooltip.add(ThermalTooltips.line("criticalmass.screen.too_small", ChatFormatting.GOLD, BOARD_WIDTH, BOARD_HEIGHT));
            }
        }
        return true;
    }

    public static class Target extends DisplayTarget {
        @Override
        public void acceptText(int line, List<MutableComponent> text, DisplayLinkContext context) {
            ScreenBlockEntity screen = controllerOf(context.level(), context.getTargetPos());
            if (screen == null) {
                return;
            }
            reserve(line, screen, context);
            for (int i = 0; i < text.size(); i++) {
                if (i > 0 && isReserved(line + i, screen, context)) {
                    break;
                }
                screen.setLine(line + i, text.get(i).getString());
            }
        }

        @Override
        public DisplayTargetStats provideStats(DisplayLinkContext context) {
            ScreenBlockEntity screen = controllerOf(context.level(), context.getTargetPos());
            if (screen == null) {
                return new DisplayTargetStats(LINES_PER_BLOCK, CHARACTERS_PER_BLOCK, this);
            }
            return new DisplayTargetStats(screen.height * LINES_PER_BLOCK, screen.width * CHARACTERS_PER_BLOCK, this);
        }

        @Override
        public AABB getMultiblockBounds(LevelAccessor level, BlockPos pos) {
            ScreenBlockEntity screen = controllerOf(level, pos);
            if (screen == null) {
                return super.getMultiblockBounds(level, pos);
            }
            BlockPos corner = screen.getBlockPos().relative(left(screen.facing()).getOpposite(), screen.width - 1).below(screen.height - 1);
            return new AABB(screen.getBlockPos()).minmax(new AABB(corner));
        }
    }
}
