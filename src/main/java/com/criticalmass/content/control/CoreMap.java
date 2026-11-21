package com.criticalmass.content.control;

import com.criticalmass.content.fuel.ChannelCapBlock;
import com.criticalmass.content.fuel.ChannelCapBlockEntity;
import com.criticalmass.content.fuel.FuelState;
import com.criticalmass.content.fuel.ServicePortBlockEntity;
import com.criticalmass.content.kinetics.ControlRodDriveBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.ReactorCore;
import com.criticalmass.content.thermal.VesselStructure;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class CoreMap {
    public static final int LOW_FUEL = 15;
    private static final int MAX_TEMPERATURE = 0xffff;

    public enum Kind {
        NONE, FUEL, CONTROL, COOLANT
    }

    public enum State {
        EMPTY, ACTIVE, SPENT, DAMAGED, HOT, MELTED
    }

    public enum Cap {
        NONE, LOCKED, UNLOCKED
    }

    public enum Handling {
        FREE(null), RUNNING("criticalmass.service.running"), INVALID("criticalmass.service.invalid"), FROZEN("criticalmass.service.frozen");

        @Nullable
        private final String blocker;

        Handling(@Nullable String blocker) {
            this.blocker = blocker;
        }

        public String langKey() {
            return "criticalmass.screen.core.handling." + name().toLowerCase(java.util.Locale.ROOT);
        }

        static Handling of(@Nullable String blocker) {
            for (Handling h : values()) {
                if (h.blocker != null && h.blocker.equals(blocker)) {
                    return h;
                }
            }
            return FREE;
        }
    }

    public record Cell(Kind kind, State state, Cap cap, int percent, int temperature) {
        public static final Cell BLANK = new Cell(Kind.NONE, State.EMPTY, Cap.NONE, 0, 0);

        public int pack() {
            return kind.ordinal() | state.ordinal() << 2 | cap.ordinal() << 5 | Math.max(0, Math.min(100, percent)) << 7 | Math.max(0, Math.min(MAX_TEMPERATURE, temperature)) << 14;
        }

        static Cell unpack(int packed) {
            int kind = packed & 3;
            int state = packed >> 2 & 7;
            int cap = packed >> 5 & 3;
            return new Cell(Kind.values()[kind], State.values()[Math.min(state, State.values().length - 1)], Cap.values()[Math.min(cap, Cap.values().length - 1)], Math.min(100, packed >> 7 & 127), packed >> 14 & MAX_TEMPERATURE);
        }

        public boolean loaded() {
            return kind == Kind.FUEL && state != State.EMPTY && state != State.MELTED;
        }
    }

    public record Snapshot(int width, int depth, int[] cells, Handling handling) {
        public static final Snapshot NONE = new Snapshot(0, 0, new int[0], Handling.INVALID);

        public boolean present() {
            return width > 0 && depth > 0;
        }

        public Cell cell(int x, int z) {
            return x < 0 || z < 0 || x >= width || z >= depth ? Cell.BLANK : Cell.unpack(cells[z * width + x]);
        }

        public int count(Kind kind) {
            int n = 0;
            for (int packed : cells) {
                if (Cell.unpack(packed).kind() == kind) {
                    n++;
                }
            }
            return n;
        }

        public int loaded() {
            int n = 0;
            for (int packed : cells) {
                if (Cell.unpack(packed).loaded()) {
                    n++;
                }
            }
            return n;
        }

        public int caps(boolean unlockedOnly) {
            int n = 0;
            for (int packed : cells) {
                Cap cap = Cell.unpack(packed).cap();
                if (cap == Cap.UNLOCKED || (!unlockedOnly && cap == Cap.LOCKED)) {
                    n++;
                }
            }
            return n;
        }

        public int lowest() {
            int low = -1;
            for (int packed : cells) {
                Cell c = Cell.unpack(packed);
                if (c.loaded() && (low < 0 || c.percent() < low)) {
                    low = c.percent();
                }
            }
            return low;
        }

        public int hottest() {
            int hot = -1;
            for (int packed : cells) {
                Cell c = Cell.unpack(packed);
                if (c.kind() == Kind.FUEL) {
                    hot = Math.max(hot, c.temperature());
                }
            }
            return hot;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Width", width);
            tag.putInt("Depth", depth);
            tag.putIntArray("Cells", cells);
            tag.putInt("Handling", handling.ordinal());
            return tag;
        }
    }

    private CoreMap() {
    }

    public static Snapshot load(CompoundTag tag) {
        int width = tag.getInt("Width");
        int depth = tag.getInt("Depth");
        int[] cells = tag.getIntArray("Cells");
        int handling = tag.getInt("Handling");
        if (width <= 0 || depth <= 0 || width > VesselStructure.MAX_WIDTH || depth > VesselStructure.MAX_WIDTH || cells.length != width * depth) {
            return Snapshot.NONE;
        }
        return new Snapshot(width, depth, cells, Handling.values()[Math.max(0, Math.min(Handling.values().length - 1, handling))]);
    }

    public static boolean isSource(@Nullable BlockEntity be) {
        return be instanceof ServicePortBlockEntity || be instanceof ChannelCapBlockEntity;
    }

    @Nullable
    public static ReactorControllerBlockEntity controller(@Nullable BlockEntity be) {
        if (be instanceof ServicePortBlockEntity port) {
            return port.controller();
        }
        return be instanceof ChannelCapBlockEntity cap ? cap.controller() : null;
    }

    public static Snapshot build(@Nullable ReactorControllerBlockEntity c) {
        Level level = c == null ? null : c.getLevel();
        VesselStructure layout = c == null ? null : c.layout();
        if (level == null || layout == null || layout.unloaded()) {
            return Snapshot.NONE;
        }
        int width = layout.max.getX() - layout.min.getX() - 1;
        int depth = layout.max.getZ() - layout.min.getZ() - 1;
        if (width <= 0 || depth <= 0 || width > VesselStructure.MAX_WIDTH || depth > VesselStructure.MAX_WIDTH) {
            return Snapshot.NONE;
        }
        int[] cells = new int[width * depth];
        ReactorCore core = c.core();
        Map<Long, ControlRodDriveBlockEntity> drives = c.findDrives();
        BlockPos at = c.getBlockPos();
        for (VesselStructure.Column column : layout.columns) {
            int x = column.x() - layout.min.getX() - 1;
            int z = column.z() - layout.min.getZ() - 1;
            if (x < 0 || z < 0 || x >= width || z >= depth) {
                continue;
            }
            long key = ReactorCore.key(column.x() - at.getX(), column.z() - at.getZ());
            Cell cell = switch (column.kind()) {
                case FUEL -> fuel(level, layout, core, column, key);
                case CONTROL -> {
                    ControlRodDriveBlockEntity drive = drives.get(key);
                    double insertion = drive != null ? drive.insertion() : c.insertion();
                    yield new Cell(Kind.CONTROL, State.EMPTY, Cap.NONE, (int) Math.round(Math.max(0, Math.min(1, insertion)) * 100), 0);
                }
                case COOLANT -> new Cell(Kind.COOLANT, State.EMPTY, Cap.NONE, 0, 0);
                default -> Cell.BLANK;
            };
            cells[z * width + x] = cell.pack();
        }
        return new Snapshot(width, depth, cells, Handling.of(c.fuelMoveBlocker()));
    }

    private static Cell fuel(Level level, VesselStructure layout, ReactorCore core, VesselStructure.Column column, long key) {
        FuelState assembly = core.assembly(key);
        ReactorCore.FuelColumn f = core.column(key);
        double temperature = f == null ? CMThermal.AMBIENT : f.temperature();
        State state;
        if (core.coriumColumns().contains(key)) {
            state = State.MELTED;
        } else if (assembly == null) {
            state = State.EMPTY;
        } else if (assembly.damaged()) {
            state = State.DAMAGED;
        } else if (temperature >= CMThermal.HOT_SPOT_TEMPERATURE) {
            state = State.HOT;
        } else {
            state = assembly.spent() ? State.SPENT : State.ACTIVE;
        }
        BlockState top = level.getBlockState(new BlockPos(column.x(), layout.max.getY(), column.z()));
        Cap cap = !(top.getBlock() instanceof ChannelCapBlock) ? Cap.NONE : top.getValue(ChannelCapBlock.OPEN) ? Cap.UNLOCKED : Cap.LOCKED;
        int percent = assembly == null ? 0 : (int) Math.round(Math.max(0, Math.min(1, assembly.remaining())) * 100);
        return new Cell(Kind.FUEL, state, cap, percent, (int) Math.round(temperature));
    }
}
