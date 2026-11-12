package com.criticalmass.content.thermal;

import com.criticalmass.content.CoolantPortBlock;
import com.criticalmass.content.FacingBlock;
import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.registry.CMBlocks;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class VesselStructure {
    public static final int MIN_SIZE = 5;
    public static final int MAX_WIDTH = 11;
    public static final int MAX_HEIGHT = 13;
    public static final int MAX_PROBLEMS = 64;
    public static final String UNLOADED = "criticalmass.vessel.unloaded";

    public enum Kind { NONE, FUEL, CONTROL, COOLANT }

    public record Problem(BlockPos pos, String reason) {
    }

    public record Column(int x, int z, Kind kind) {
    }

    public final BlockPos min;
    public final BlockPos max;
    public final List<Problem> problems;
    public final List<Problem> warnings;
    public final List<String> missing;
    public final List<Column> columns;
    public final java.util.Map<BlockPos, Integer> cracked;
    public final String sizeError;
    public final int interiorHeight;

    private VesselStructure(BlockPos min, BlockPos max, List<Problem> problems, List<Problem> warnings, List<String> missing, List<Column> columns, String sizeError, int interiorHeight, java.util.Map<BlockPos, Integer> cracked) {
        this.min = min;
        this.max = max;
        this.problems = problems;
        this.warnings = warnings;
        this.missing = missing;
        this.columns = columns;
        this.sizeError = sizeError;
        this.interiorHeight = interiorHeight;
        this.cracked = cracked;
    }

    public boolean valid() {
        return sizeError == null && problems.isEmpty();
    }

    public boolean unloaded() {
        return UNLOADED.equals(sizeError);
    }

    public boolean complete() {
        return valid() && missing.isEmpty();
    }

    public int width() {
        return max.getX() - min.getX() + 1;
    }

    public int height() {
        return max.getY() - min.getY() + 1;
    }

    public int length() {
        return max.getZ() - min.getZ() + 1;
    }

    public boolean onShell(BlockPos pos) {
        return contains(pos, 0) && (pos.getX() == min.getX() || pos.getX() == max.getX() || pos.getY() == min.getY() || pos.getY() == max.getY()
                || pos.getZ() == min.getZ() || pos.getZ() == max.getZ());
    }

    public boolean contains(BlockPos pos, int margin) {
        return pos.getX() >= min.getX() - margin && pos.getX() <= max.getX() + margin && pos.getY() >= min.getY() - margin
                && pos.getY() <= max.getY() + margin && pos.getZ() >= min.getZ() - margin && pos.getZ() <= max.getZ() + margin;
    }

    public long count(Kind kind) {
        return columns.stream().filter(c -> c.kind() == kind).count();
    }

    public static boolean isShell(Block b) {
        return b instanceof VesselCasingBlock || b == CMBlocks.REACTOR_CONTROLLER.get() || b == CMBlocks.PRIMARY_COOLANT_PORT.get()
                || b == CMBlocks.REACTOR_SERVICE_PORT.get() || b == CMBlocks.REACTOR_SIGNAL_INTERFACE.get();
    }

    public static Kind channelKind(Block b) {
        if (b == CMBlocks.FUEL_CHANNEL.get()) {
            return Kind.FUEL;
        }
        if (b == CMBlocks.CONTROL_CHANNEL.get()) {
            return Kind.CONTROL;
        }
        if (b == CMBlocks.COOLANT_CHANNEL.get()) {
            return Kind.COOLANT;
        }
        return Kind.NONE;
    }

    public static VesselStructure scan(Level level, BlockPos controller) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(controller);
        seen.add(controller);
        int minX = controller.getX(), minY = controller.getY(), minZ = controller.getZ();
        int maxX = minX, maxY = minY, maxZ = minZ;
        int limit = MAX_WIDTH * MAX_WIDTH * MAX_HEIGHT;
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (seen.contains(n)) {
                    continue;
                }
                if (!level.isLoaded(n)) {
                    return failed(controller, UNLOADED);
                }
                if (!isShell(level.getBlockState(n).getBlock())) {
                    continue;
                }
                if (seen.size() > limit) {
                    return failed(controller, "criticalmass.vessel.too_large_scan");
                }
                seen.add(n);
                queue.add(n);
            }
        }
        BlockPos min = new BlockPos(minX, minY, minZ);
        BlockPos max = new BlockPos(maxX, maxY, maxZ);
        if (!level.hasChunksAt(min, max)) {
            return failed(controller, UNLOADED);
        }
        int w = maxX - minX + 1;
        int h = maxY - minY + 1;
        int l = maxZ - minZ + 1;
        String sizeError = null;
        if (w < MIN_SIZE || l < MIN_SIZE || h < MIN_SIZE) {
            sizeError = "criticalmass.vessel.too_small";
        } else if (w > MAX_WIDTH || l > MAX_WIDTH || h > MAX_HEIGHT) {
            sizeError = "criticalmass.vessel.too_large";
        } else if (w % 2 == 0 || l % 2 == 0 || h % 2 == 0) {
            sizeError = "criticalmass.vessel.even";
        }
        List<Problem> problems = new ArrayList<>();
        boolean inlet = false;
        boolean outlet = false;
        boolean service = false;
        java.util.Map<BlockPos, Integer> cracked = new java.util.HashMap<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    int faces = (x == minX || x == maxX ? 1 : 0) + (y == minY || y == maxY ? 1 : 0) + (z == minZ || z == maxZ ? 1 : 0);
                    if (faces == 0) {
                        continue;
                    }
                    BlockState s = level.getBlockState(p);
                    Block b = s.getBlock();
                    Direction normal = faces == 1 ? outward(p, min, max) : null;
                    String reason = shellProblem(s, b, faces, normal, p.equals(controller));
                    if (reason != null) {
                        add(problems, p, reason);
                        continue;
                    }
                    if (b instanceof VesselCasingBlock && s.hasProperty(VesselCasingBlock.CRACK) && s.getValue(VesselCasingBlock.CRACK) > 0) {
                        cracked.put(p, s.getValue(VesselCasingBlock.CRACK));
                    }
                    if (b == CMBlocks.PRIMARY_COOLANT_PORT.get()) {
                        if (s.getValue(CoolantPortBlock.MODE) == CoolantPortBlock.Mode.INLET) {
                            inlet = true;
                        } else {
                            outlet = true;
                        }
                    } else if (b == CMBlocks.REACTOR_SERVICE_PORT.get()) {
                        service = true;
                    }
                }
            }
        }
        List<Column> columns = new ArrayList<>();
        int interiorHeight = Math.max(0, h - 2);
        for (int x = minX + 1; x < maxX; x++) {
            for (int z = minZ + 1; z < maxZ; z++) {
                Kind column = null;
                boolean columnOk = true;
                for (int y = minY + 1; y < maxY; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState s = level.getBlockState(p);
                    Block b = s.getBlock();
                    Kind k = channelKind(b);
                    if (k == Kind.NONE && !s.isAir()) {
                        add(problems, p, isShell(b) ? "criticalmass.vessel.problem.shell_inside" : "criticalmass.vessel.problem.wrong_block");
                        columnOk = false;
                        continue;
                    }
                    if (k != Kind.NONE && s.hasProperty(RotatedPillarBlock.AXIS) && s.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Y) {
                        add(problems, p, "criticalmass.vessel.problem.channel_axis");
                        columnOk = false;
                    }
                    if (column == null) {
                        column = k;
                    } else if (column != k) {
                        add(problems, p, "criticalmass.vessel.problem.mixed_column");
                        columnOk = false;
                    }
                }
                if (columnOk && column != null) {
                    columns.add(new Column(x, z, column));
                }
            }
        }
        List<String> missing = new ArrayList<>();
        if (!inlet) {
            missing.add("criticalmass.vessel.missing.inlet");
        }
        if (!outlet) {
            missing.add("criticalmass.vessel.missing.outlet");
        }
        if (!service) {
            missing.add("criticalmass.vessel.missing.service_port");
        }
        return new VesselStructure(min, max, List.copyOf(problems), List.copyOf(layoutWarnings(columns, minY + 1)), List.copyOf(missing), List.copyOf(columns), sizeError, interiorHeight, java.util.Map.copyOf(cracked));
    }

    private static VesselStructure failed(BlockPos at, String key) {
        return new VesselStructure(at, at, List.of(), List.of(), List.of(), List.of(), key, 0, java.util.Map.of());
    }

    private static List<Problem> layoutWarnings(List<Column> columns, int bottom) {
        java.util.Map<Long, Kind> kinds = new java.util.HashMap<>();
        for (Column c : columns) {
            kinds.put(BlockPos.asLong(c.x(), 0, c.z()), c.kind());
        }
        List<Problem> warnings = new ArrayList<>();
        for (Column c : columns) {
            if (c.kind() != Kind.FUEL) {
                continue;
            }
            boolean coolant = false;
            boolean control = false;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                Kind k = kinds.get(BlockPos.asLong(c.x() + d.getStepX(), 0, c.z() + d.getStepZ()));
                coolant |= k == Kind.COOLANT;
                control |= k == Kind.CONTROL;
            }
            BlockPos pos = new BlockPos(c.x(), bottom, c.z());
            if (!coolant) {
                add(warnings, pos, "criticalmass.vessel.warning.no_coolant");
            }
            if (!control) {
                add(warnings, pos, "criticalmass.vessel.warning.no_control");
            }
        }
        return warnings;
    }

    private static void add(List<Problem> problems, BlockPos pos, String reason) {
        if (problems.size() < MAX_PROBLEMS) {
            problems.add(new Problem(pos, reason));
        }
    }

    @Nullable
    private static Direction outward(BlockPos p, BlockPos min, BlockPos max) {
        if (p.getX() == min.getX()) return Direction.WEST;
        if (p.getX() == max.getX()) return Direction.EAST;
        if (p.getY() == min.getY()) return Direction.DOWN;
        if (p.getY() == max.getY()) return Direction.UP;
        if (p.getZ() == min.getZ()) return Direction.NORTH;
        if (p.getZ() == max.getZ()) return Direction.SOUTH;
        return null;
    }

    @Nullable
    private static String shellProblem(BlockState s, Block b, int faces, @Nullable Direction normal, boolean isOurController) {
        if (!isShell(b)) {
            return channelKind(b) != Kind.NONE ? "criticalmass.vessel.problem.channel_outside" : "criticalmass.vessel.problem.missing_casing";
        }
        if (b instanceof VesselCasingBlock) {
            return null;
        }
        if (faces > 1) {
            return "criticalmass.vessel.problem.edge";
        }
        if (b == CMBlocks.REACTOR_CONTROLLER.get()) {
            if (!isOurController) {
                return "criticalmass.vessel.problem.second_controller";
            }
            return s.getValue(HorizontalBlock.FACING) == normal ? null : "criticalmass.vessel.problem.facing";
        }
        if (b == CMBlocks.REACTOR_SIGNAL_INTERFACE.get()) {
            return s.getValue(HorizontalBlock.FACING) == normal ? null : "criticalmass.vessel.problem.facing";
        }
        if (b == CMBlocks.REACTOR_SERVICE_PORT.get()) {
            if (normal != Direction.UP) {
                return "criticalmass.vessel.problem.service_top";
            }
            return s.getValue(FacingBlock.FACING) == Direction.UP ? null : "criticalmass.vessel.problem.facing";
        }
        if (b == CMBlocks.PRIMARY_COOLANT_PORT.get()) {
            return s.getValue(FacingBlock.FACING) == normal ? null : "criticalmass.vessel.problem.facing";
        }
        return null;
    }
}
