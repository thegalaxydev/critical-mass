package com.criticalmass.content.kinetics;

import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class TurbineStructure {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final IntegerProperty ALONG = IntegerProperty.create("along", 0, 2);
    public static final IntegerProperty ACROSS = IntegerProperty.create("across", 0, 2);
    public static final IntegerProperty RISE = IntegerProperty.create("rise", 0, 2);
    private static final double[] APOTHEM = {19.3137, 15.3137, 11.3137};
    private static final double[] WINDOW = {5, 3.3431, 1.6863};
    private static final VoxelShape[] SHAPES = new VoxelShape[54];

    public record Part(int along, int across, int rise) {
    }

    public static final List<Part> PARTS = parts();

    private TurbineStructure() {
    }

    private static List<Part> parts() {
        List<Part> out = new ArrayList<>();
        for (int a = -1; a <= 1; a++) {
            for (int c = -1; c <= 1; c++) {
                for (int r = -1; r <= 1; r++) {
                    boolean centre = a == 0 && c == 0 && r == 0;
                    boolean backCorner = a == 1 && c != 0 && r != 0;
                    if (!centre && !backCorner) {
                        out.add(new Part(a, c, r));
                    }
                }
            }
        }
        return List.copyOf(out);
    }

    public static Vec3i offset(Direction.Axis axis, Part part) {
        return axis == Direction.Axis.X ? new Vec3i(part.along(), part.rise(), part.across()) : new Vec3i(-part.across(), part.rise(), part.along());
    }

    public static BlockState state(Direction.Axis axis, Part part) {
        return CMBlocks.STEAM_TURBINE_STRUCTURE.get().defaultBlockState().setValue(AXIS, axis).setValue(ALONG, part.along() + 1).setValue(ACROSS, part.across() + 1).setValue(RISE, part.rise() + 1);
    }

    public static Part part(BlockState state) {
        return new Part(state.getValue(ALONG) - 1, state.getValue(ACROSS) - 1, state.getValue(RISE) - 1);
    }

    public static BlockPos master(BlockPos pos, BlockState state) {
        return pos.subtract(offset(state.getValue(AXIS), part(state)));
    }

    public static boolean valid(BlockGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof SteamTurbineStructuralBlock)) {
            return false;
        }
        BlockState master = level.getBlockState(master(pos, state));
        return master.getBlock() instanceof SteamTurbineBlock && master.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS) == state.getValue(AXIS);
    }

    public static BlockPos resolve(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return valid(level, pos, state) ? master(pos, state) : pos;
    }

    public static boolean isShaft(BlockState state) {
        Part p = part(state);
        return p.along() != 0 && p.across() == 0 && p.rise() == 0;
    }

    @Nullable
    public static Direction portFace(BlockState state) {
        if (!(state.getBlock() instanceof SteamTurbineStructuralBlock)) {
            return null;
        }
        Part p = part(state);
        if (p.along() != 0 || p.across() != 0 || p.rise() == 0) {
            return null;
        }
        return p.rise() > 0 ? Direction.UP : Direction.DOWN;
    }

    public static boolean clear(BlockGetter level, BlockPos master, Direction.Axis axis) {
        for (Part part : PARTS) {
            if (!level.getBlockState(master.offset(offset(axis, part))).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    public static boolean build(Level level, BlockPos master, Direction.Axis axis) {
        for (Part part : PARTS) {
            BlockPos at = master.offset(offset(axis, part));
            BlockState wanted = state(axis, part);
            BlockState found = level.getBlockState(at);
            if (found == wanted) {
                continue;
            }
            if (!found.canBeReplaced()) {
                return false;
            }
            level.setBlock(at, wanted, Block.UPDATE_ALL);
        }
        return true;
    }

    public static void clearAway(Level level, BlockPos master, Direction.Axis axis) {
        for (Part part : PARTS) {
            BlockPos at = master.offset(offset(axis, part));
            BlockState found = level.getBlockState(at);
            if (found.getBlock() instanceof SteamTurbineStructuralBlock && found.getValue(AXIS) == axis && part(found).equals(part)) {
                level.removeBlock(at, false);
            }
        }
    }

    public static int signal(Level level, BlockPos master, Direction.Axis axis) {
        int best = level.getBestNeighborSignal(master);
        for (Part part : PARTS) {
            if (best >= 15) {
                break;
            }
            best = Math.max(best, level.getBestNeighborSignal(master.offset(offset(axis, part))));
        }
        return best;
    }

    public static VoxelShape shape(BlockState state) {
        int index = (state.getValue(AXIS) == Direction.Axis.X ? 0 : 27) + state.getValue(ALONG) * 9 + state.getValue(ACROSS) * 3 + state.getValue(RISE);
        VoxelShape shape = SHAPES[index];
        if (shape == null) {
            shape = build(state.getValue(AXIS), part(state));
            SHAPES[index] = shape;
        }
        return shape;
    }

    private static VoxelShape build(Direction.Axis axis, Part part) {
        if (part.across() == 0 && part.rise() == 0) {
            return Shapes.block();
        }
        double apothem = APOTHEM[part.along() + 1];
        boolean corner = part.across() != 0 && part.rise() != 0;
        double reach = corner ? (Math.sqrt(2) * apothem - 16) / 2 : apothem - 8;
        if (reach <= 0.01) {
            return Shapes.empty();
        }
        double[] y = span(part.rise(), reach);
        double[] z = span(part.across(), reach);
        VoxelShape shape = box(axis, 0, y[0], z[0], 16, y[1], z[1]);
        if (part.rise() == 0) {
            double half = WINDOW[part.along() + 1];
            shape = Shapes.join(shape, box(axis, 2, 8 - half, z[0], 14, 8 + half, z[1]), BooleanOp.ONLY_FIRST);
        }
        return shape;
    }

    private static double[] span(int side, double reach) {
        if (side > 0) {
            return new double[]{0, reach};
        }
        if (side < 0) {
            return new double[]{16 - reach, 16};
        }
        return new double[]{0, 16};
    }

    private static VoxelShape box(Direction.Axis axis, double x0, double y0, double z0, double x1, double y1, double z1) {
        return axis == Direction.Axis.X ? Block.box(x0, y0, z0, x1, y1, z1) : Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
    }
}
