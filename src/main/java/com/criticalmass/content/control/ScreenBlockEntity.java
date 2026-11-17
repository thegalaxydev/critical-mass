package com.criticalmass.content.control;

import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.power.BatteryBlockEntity;
import com.criticalmass.content.power.BreakerBlockEntity;
import com.criticalmass.content.power.DieselEngineBlockEntity;
import com.criticalmass.content.power.BusBarBlockEntity;
import com.criticalmass.content.power.GeneratorBlockEntity;
import com.criticalmass.content.power.GridTieBlockEntity;
import com.criticalmass.content.power.MotorBlockEntity;
import com.criticalmass.content.power.TransformerBlockEntity;
import com.criticalmass.content.thermal.PressurizerBlockEntity;
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
    public static final int MIN_WIDTH = 3;
    public static final int MIN_HEIGHT = 2;
    public static final int MIMIC_WIDTH = 5;
    public static final int MIMIC_HEIGHT = 3;
    public static final int MAX_TRENDS = 6;
    public static final int HISTORY = 120;
    public static final int SAMPLE_TICKS = 20;
    public static final int EVENT_LIMIT = 5;
    public static final List<PlantChannel> ROWS = List.of(PlantChannel.NEUTRON, PlantChannel.POWER, PlantChannel.PERIOD, PlantChannel.REACTIVITY, PlantChannel.TEMPERATURE,
            PlantChannel.COOLANT_TEMPERATURE, PlantChannel.PRESSURE, PlantChannel.FLOW, PlantChannel.RODS_A, PlantChannel.XENON, PlantChannel.BORON);
    private static final List<PlantChannel> TREND_ORDER = List.of(PlantChannel.NEUTRON, PlantChannel.OUTPUT, PlantChannel.DEMAND, PlantChannel.CURRENT, PlantChannel.SPEED, PlantChannel.FREQUENCY, PlantChannel.VOLTAGE,
            PlantChannel.SYNC, PlantChannel.FLOW, PlantChannel.PRESSURE, PlantChannel.TEMPERATURE, PlantChannel.COOLANT_TEMPERATURE, PlantChannel.LEVEL, PlantChannel.POWER, PlantChannel.RODS_A,
            PlantChannel.RODS_B, PlantChannel.RODS_C, PlantChannel.RODS_D, PlantChannel.XENON, PlantChannel.BORON, PlantChannel.REACTIVITY, PlantChannel.PERIOD);
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
    private final float[][] history = new float[MAX_TRENDS][HISTORY];
    private final float[] low = new float[MAX_TRENDS];
    private final float[] scale = new float[MAX_TRENDS];
    private final float[] rangeLow = new float[MAX_TRENDS];
    private final float[] rangeHigh = new float[MAX_TRENDS];
    private final byte[][] plot = new byte[MAX_TRENDS][0];
    private final String[] units = new String[MAX_TRENDS];
    private List<PlantChannel> trends = List.of();
    private List<PlantChannel> rows = List.of();
    private boolean board;
    private String title = "";
    private String status = "";
    private final List<String> values = new ArrayList<>();
    private String turbineSpeed = "";
    private String output = "";
    private final List<Event> events = new ArrayList<>();
    private int lastAlarms;
    private List<Mimic.Node> mimic = List.of();
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
        return width >= MIN_WIDTH && height >= MIN_HEIGHT;
    }

    public boolean board() {
        return board;
    }

    public boolean fullBoard() {
        return board && width >= BOARD_WIDTH && height >= BOARD_HEIGHT;
    }

    public String title() {
        return title;
    }

    public List<PlantChannel> trends() {
        return trends;
    }

    public List<PlantChannel> rows() {
        return rows;
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

    public List<Mimic.Node> mimic() {
        return mimic;
    }

    public int samples() {
        return samples;
    }

    public String unit(int trend) {
        return units[trend] == null ? "" : units[trend];
    }

    public float low(int trend) {
        return low[trend];
    }

    public float scale(int trend) {
        return scale[trend];
    }

    public byte[] plot(int trend) {
        return plot[trend];
    }

    public float latest(int trend) {
        return samples == 0 ? 0 : history[trend][samples - 1];
    }

    public float latest(Trend trend) {
        return latest(trend.ordinal());
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
        boolean onReactor = reactor != null && level.getBlockEntity(reactor) instanceof ReactorControllerBlockEntity;
        if (be instanceof SteamTurbineBlockEntity && onReactor) {
            turbine = pos.immutable();
        } else if (PlantChannel.isTarget(be)) {
            if (!pos.equals(reactor)) {
                clearReadings();
                turbine = null;
            }
            reactor = pos.immutable();
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
        title = "";
        trends = List.of();
        rows = List.of();
        board = false;
        values.clear();
        turbineSpeed = "";
        output = "";
        events.clear();
        lastAlarms = 0;
        mimic = List.of();
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

    public static String statusOf(@Nullable BlockEntity be) {
        if (be instanceof ReactorControllerBlockEntity c) {
            return c.isTripped() && !c.tripReason().isEmpty() ? "criticalmass.reactor.trip." + c.tripReason() + ".short" : c.status() + ".short";
        }
        if (be instanceof GeneratorBlockEntity g) {
            return g.master().status();
        }
        if (be instanceof MotorBlockEntity m) {
            return m.status();
        }
        if (be instanceof BreakerBlockEntity b) {
            return b.closed() ? "criticalmass.breaker.closed" : b.tripReason().isEmpty() ? "criticalmass.breaker.open" : b.tripReason();
        }
        if (be instanceof BusBarBlockEntity bus) {
            return bus.live() ? "criticalmass.bus.live" : "criticalmass.bus.dead";
        }
        if (be instanceof TransformerBlockEntity t) {
            return t.heat() > 0.5 ? "criticalmass.transformer.hot" : t.highSide() > 0 || t.lowSide() > 0 ? "criticalmass.transformer.live" : "criticalmass.bus.dead";
        }
        if (be instanceof BatteryBlockEntity battery) {
            return battery.status();
        }
        if (be instanceof DieselEngineBlockEntity engine) {
            return engine.status();
        }
        if (be instanceof GridTieBlockEntity tie) {
            return switch (tie.event()) {
                case OUTAGE -> "criticalmass.grid.lost";
                case DIP -> "criticalmass.grid.dipping";
                case SWING -> "criticalmass.grid.swinging";
                default -> tie.ordering() ? "criticalmass.grid.ordering" : "criticalmass.grid.idle";
            };
        }
        if (be instanceof SteamTurbineBlockEntity t) {
            return t.status();
        }
        if (be instanceof CirculationPumpBlockEntity p) {
            return p.status();
        }
        if (be instanceof PressurizerBlockEntity p) {
            return p.status();
        }
        return "";
    }

    public void sample() {
        if (level == null || reactor == null || !level.isLoaded(reactor)) {
            return;
        }
        BlockEntity be = level.getBlockEntity(reactor);
        boolean nowBoard = be instanceof ReactorControllerBlockEntity;
        List<PlantChannel> nowTrends;
        List<PlantChannel> nowRows;
        if (nowBoard) {
            nowTrends = new ArrayList<>();
            for (Trend trend : Trend.values()) {
                nowTrends.add(trend.channel);
            }
            nowRows = ROWS;
        } else {
            nowRows = new ArrayList<>();
            for (PlantChannel channel : TREND_ORDER) {
                if (PlantChannel.isTarget(be) && channel.read(level, reactor) != null) {
                    nowRows.add(channel);
                }
            }
            nowTrends = nowRows.subList(0, Math.min(MAX_TRENDS, nowRows.size()));
        }
        if (!nowTrends.equals(trends)) {
            trends = List.copyOf(nowTrends);
            samples = 0;
        }
        rows = List.copyOf(nowRows);
        board = nowBoard;
        title = be == null ? "" : be.getBlockState().getBlock().getDescriptionId();
        String nowStatus = rows.isEmpty() ? "criticalmass.screen.no_target" : statusOf(be);
        int alarms = be instanceof ReactorControllerBlockEntity c ? PlantAlarms.evaluate(c) : 0;
        values.clear();
        for (PlantChannel channel : rows) {
            values.add(shown(channel.read(level, reactor)));
        }
        if (!trends.isEmpty()) {
            if (samples == HISTORY) {
                for (float[] series : history) {
                    System.arraycopy(series, 1, series, 0, HISTORY - 1);
                }
                samples--;
            }
            for (int i = 0; i < trends.size(); i++) {
                PlantChannel.Reading r = trends.get(i).read(level, reactor);
                rangeLow[i] = r == null ? 0 : (float) r.min();
                units[i] = r == null ? trends.get(i).unit : r.unit();
                rangeHigh[i] = r == null ? 0 : (float) r.max();
                history[i][samples] = r == null ? 0 : (float) Math.max(Math.min(0, r.min()), r.value());
            }
            samples++;
        }
        if (!nowStatus.equals(status)) {
            if (!status.isEmpty() && !nowStatus.isEmpty()) {
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
        mimic = be instanceof BusBarBlockEntity && width >= MIMIC_WIDTH && height >= MIMIC_HEIGHT ? Mimic.build(level, reactor) : List.of();
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
            tag.putString("Title", title);
            tag.putBoolean("Board", board);
            tag.putIntArray("Trends", trends.stream().mapToInt(Enum::ordinal).toArray());
            tag.putIntArray("Rows", rows.stream().mapToInt(Enum::ordinal).toArray());
            ListTag shown = new ListTag();
            for (String value : values) {
                shown.add(StringTag.valueOf(value));
            }
            tag.put("Values", shown);
            tag.putString("TurbineSpeed", turbineSpeed);
            tag.put("Mimic", Mimic.save(mimic));
            tag.putString("Output", output);
            tag.putInt("Samples", samples);
            for (int i = 0; i < trends.size(); i++) {
                float[] series = history[i];
                float top = 0;
                for (int j = 0; j < samples; j++) {
                    top = Math.max(top, series[j]);
                }
                boolean windowed = !board && rangeLow[i] != 0;
                float floor = board ? Trend.values()[i].floor : Math.max(0.1f, rangeHigh[i] * 0.1f);
                float min = windowed ? rangeLow[i] : 0;
                float max = windowed ? rangeHigh[i] : ceiling(top, floor);
                byte[] packed = new byte[samples];
                for (int j = 0; j < samples; j++) {
                    packed[j] = (byte) Math.round(Math.max(0, Math.min(1, (series[j] - min) / (max - min))) * 255);
                }
                tag.putFloat("Low" + i, min);
                tag.putString("Unit" + i, unit(i));
                tag.putFloat("Scale" + i, max);
                tag.putFloat("Latest" + i, samples == 0 ? 0 : series[samples - 1]);
                tag.putByteArray("Trend" + i, packed);
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
            mimic = Mimic.load(tag.getList("Mimic", Tag.TAG_COMPOUND));
            output = tag.getString("Output");
            samples = tag.getInt("Samples");
            title = tag.getString("Title");
            board = tag.getBoolean("Board");
            trends = channels(tag.getIntArray("Trends"), MAX_TRENDS);
            rows = channels(tag.getIntArray("Rows"), PlantChannel.values().length);
            for (int i = 0; i < trends.size(); i++) {
                low[i] = tag.getFloat("Low" + i);
                units[i] = tag.getString("Unit" + i);
                scale[i] = tag.getFloat("Scale" + i);
                plot[i] = tag.getByteArray("Trend" + i);
                if (samples > 0 && samples <= HISTORY) {
                    history[i][samples - 1] = tag.getFloat("Latest" + i);
                }
            }
        }
    }

    private static List<PlantChannel> channels(int[] ordinals, int limit) {
        List<PlantChannel> out = new ArrayList<>();
        PlantChannel[] all = PlantChannel.values();
        for (int ordinal : ordinals) {
            if (ordinal >= 0 && ordinal < all.length && out.size() < limit) {
                out.add(all[ordinal]);
            }
        }
        return out;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ScreenBlockEntity main = level == null ? null : controllerOf(level, worldPosition);
        if (!powered) {
            ThermalTooltips.status(tooltip, "criticalmass.screen.unpowered", ChatFormatting.GOLD);
        } else if (main == null || main.reactor == null) {
            ThermalTooltips.status(tooltip, "criticalmass.screen.unlinked", ChatFormatting.GRAY);
        } else if (main.bigEnough()) {
            ThermalTooltips.status(tooltip, "criticalmass.screen.linked", ChatFormatting.GREEN, level.getBlockState(main.reactor).getBlock().getName(), main.reactor.getX(), main.reactor.getY(), main.reactor.getZ());
        } else {
            ThermalTooltips.status(tooltip, "criticalmass.screen.too_small", ChatFormatting.GOLD, MIN_WIDTH, MIN_HEIGHT);
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
