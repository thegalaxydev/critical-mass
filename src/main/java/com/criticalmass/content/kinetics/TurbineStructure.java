package com.criticalmass.content.kinetics;

import com.criticalmass.content.thermal.NodeBlocks;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class TurbineStructure {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty ALONG = IntegerProperty.create("along", 0, 2);
    public static final IntegerProperty ACROSS = IntegerProperty.create("across", 0, 2);
    public static final IntegerProperty RISE = IntegerProperty.create("rise", 0, 2);
    public static final BooleanProperty DUCT = BooleanProperty.create("duct");
    private static final double[] APOTHEM = {19.3137, 15.3137, 11.3137};
    private static final double[] WINDOW = {5, 3.3431, 1.6863};
    private static final VoxelShape[] SHAPES = new VoxelShape[216];

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

    public static Direction facing(BlockState turbine) {
        return turbine.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
    }

    public static Vec3i offset(Direction facing, Part part) {
        Direction side = facing.getCounterClockWise();
        return new Vec3i(-facing.getStepX() * part.along() + side.getStepX() * part.across(), part.rise(), -facing.getStepZ() * part.along() + side.getStepZ() * part.across());
    }

    public static BlockState state(Direction facing, Part part) {
        return CMBlocks.STEAM_TURBINE_STRUCTURE.get().defaultBlockState().setValue(FACING, facing).setValue(ALONG, part.along() + 1).setValue(ACROSS, part.across() + 1).setValue(RISE, part.rise() + 1);
    }

    public static Part part(BlockState state) {
        return new Part(state.getValue(ALONG) - 1, state.getValue(ACROSS) - 1, state.getValue(RISE) - 1);
    }

    public static BlockPos master(BlockPos pos, BlockState state) {
        return pos.subtract(offset(state.getValue(FACING), part(state)));
    }

    public static boolean valid(BlockGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof SteamTurbineStructuralBlock)) {
            return false;
        }
        BlockState master = level.getBlockState(master(pos, state));
        return master.getBlock() instanceof SteamTurbineBlock && facing(master) == state.getValue(FACING);
    }

    public static BlockPos resolve(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return valid(level, pos, state) ? master(pos, state) : pos;
    }

    public static boolean isShaft(BlockState state) {
        Part p = part(state);
        return p.along() == -1 && p.across() == 0 && p.rise() == 0;
    }

    @Nullable
    public static Direction portFace(BlockState state) {
        if (!(state.getBlock() instanceof SteamTurbineStructuralBlock)) {
            return null;
        }
        Part p = part(state);
        if (p.across() != 0) {
            return null;
        }
        if (p.along() == 1 && p.rise() == 0) {
            return state.getValue(FACING).getOpposite();
        }
        return ductFace(state);
    }

    @Nullable
    public static Direction ductFace(BlockState state) {
        Part p = part(state);
        if (p.along() != 0 || p.across() != 0 || p.rise() == 0) {
            return null;
        }
        return p.rise() > 0 ? Direction.UP : Direction.DOWN;
    }

    public static boolean piped(LevelAccessor level, BlockPos port, Direction face) {
        return NodeBlocks.piped(level, port, face);
    }

    public static boolean hasDuct(BlockGetter level, BlockPos master, Direction face) {
        BlockState state = level.getBlockState(master.relative(face));
        return state.getBlock() instanceof SteamTurbineStructuralBlock && state.getValue(DUCT) && ductFace(state) == face;
    }

    public static boolean clear(BlockGetter level, BlockPos master, Direction facing) {
        for (Part part : PARTS) {
            if (!level.getBlockState(master.offset(offset(facing, part))).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    public static boolean build(Level level, BlockPos master, Direction facing) {
        for (Part part : PARTS) {
            BlockPos at = master.offset(offset(facing, part));
            BlockState found = level.getBlockState(at);
            if (found.getBlock() instanceof SteamTurbineStructuralBlock && found.getValue(FACING) == facing && part(found).equals(part)) {
                continue;
            }
            if (!found.canBeReplaced()) {
                return false;
            }
            BlockState wanted = state(facing, part);
            Direction duct = ductFace(wanted);
            if (duct != null) {
                wanted = wanted.setValue(DUCT, piped(level, at, duct));
            }
            level.setBlock(at, wanted, Block.UPDATE_ALL);
        }
        return true;
    }

    public static void clearAway(Level level, BlockPos master, Direction facing) {
        for (Part part : PARTS) {
            BlockPos at = master.offset(offset(facing, part));
            BlockState found = level.getBlockState(at);
            if (found.getBlock() instanceof SteamTurbineStructuralBlock && found.getValue(FACING) == facing && part(found).equals(part)) {
                level.removeBlock(at, false);
            }
        }
    }

    public static int signal(Level level, BlockPos master, Direction facing) {
        int best = level.getBestNeighborSignal(master);
        for (Part part : PARTS) {
            if (best >= 15) {
                break;
            }
            best = Math.max(best, level.getBestNeighborSignal(master.offset(offset(facing, part))));
        }
        return best;
    }

    public static VoxelShape shape(BlockState state) {
        Direction facing = state.getValue(FACING);
        boolean duct = state.getValue(DUCT) && ductFace(state) != null;
        int index = (duct ? 108 : 0) + facing.get2DDataValue() * 27 + state.getValue(ALONG) * 9 + state.getValue(ACROSS) * 3 + state.getValue(RISE);
        VoxelShape shape = SHAPES[index];
        if (shape == null) {
            shape = build(facing, part(state), duct);
            SHAPES[index] = shape;
        }
        return shape;
    }

    private static VoxelShape build(Direction facing, Part part, boolean duct) {
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
        VoxelShape shape = box(facing, 0, y[0], z[0], 16, y[1], z[1]);
        if (part.rise() == 0) {
            double half = WINDOW[part.along() + 1];
            shape = Shapes.join(shape, box(facing, 2, 8 - half, z[0], 14, 8 + half, z[1]), BooleanOp.ONLY_FIRST);
        }
        if (duct) {
            shape = Shapes.or(shape, part.rise() < 0 ? box(facing, 3, 0, 3, 13, y[0], 13) : box(facing, 3, y[1], 3, 13, 16, 13));
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

    private static VoxelShape box(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
        int turns = switch (facing) {
            case NORTH -> 1;
            case EAST -> 2;
            case SOUTH -> 3;
            default -> 0;
        };
        for (int i = 0; i < turns; i++) {
            double nx0 = 16 - z1;
            double nx1 = 16 - z0;
            z0 = x0;
            z1 = x1;
            x0 = nx0;
            x1 = nx1;
        }
        return Block.box(x0, y0, z0, x1, y1, z1);
    }
}
