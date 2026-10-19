package com.criticalmass.content;

import com.criticalmass.content.kinetics.CirculationPumpBlock;
import com.criticalmass.content.kinetics.SteamTurbineBlock;
import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ReinforcedPipeBlock extends PipeBlock {
    public static final MapCodec<ReinforcedPipeBlock> CODEC = simpleCodec(ReinforcedPipeBlock::new);
    public static final Map<Direction, BooleanProperty> RIM = new EnumMap<>(Direction.class);
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        for (Direction d : Direction.values()) {
            RIM.put(d, BooleanProperty.create("rim_" + d.getSerializedName()));
        }
        VoxelShape core = Block.box(4, 4, 4, 12, 12, 12);
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = core;
            for (Direction d : Direction.values()) {
                if ((mask & (1 << d.ordinal())) != 0) {
                    shape = Shapes.or(shape, arm(d));
                }
            }
            SHAPES[mask] = shape;
        }
    }

    public ReinforcedPipeBlock(Properties properties) {
        super(0.25f, properties);
        BlockState state = stateDefinition.any();
        for (Direction d : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(d), false).setValue(RIM.get(d), false);
        }
        registerDefaultState(state);
    }

    private static VoxelShape arm(Direction d) {
        return switch (d) {
            case NORTH -> Block.box(4, 4, 0, 12, 12, 4);
            case SOUTH -> Block.box(4, 4, 12, 12, 12, 16);
            case WEST -> Block.box(0, 4, 4, 4, 12, 12);
            case EAST -> Block.box(12, 4, 4, 16, 12, 12);
            case DOWN -> Block.box(4, 0, 4, 12, 4, 12);
            case UP -> Block.box(4, 12, 4, 12, 16, 12);
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction d : Direction.values()) {
            if (state.getValue(PROPERTY_BY_DIRECTION.get(d)) || state.getValue(RIM.get(d))) {
                mask |= 1 << d.ordinal();
            }
        }
        return SHAPES[mask];
    }

    @Override
    protected MapCodec<? extends PipeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
        RIM.values().forEach(builder::add);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(context.getLevel(), context.getClickedPos(), defaultBlockState(), context.getClickedFace().getAxis());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return withConnections(level, pos, state, null);
    }

    public static BlockState withConnections(BlockGetter level, BlockPos pos, BlockState state) {
        return withConnections(level, pos, state, null);
    }

    public static BlockState withConnections(BlockGetter level, BlockPos pos, BlockState state, @Nullable Direction.Axis loneAxis) {
        Direction.Axis keep = null;
        for (Direction d : Direction.values()) {
            if (state.getValue(RIM.get(d)) && !state.getValue(PROPERTY_BY_DIRECTION.get(d))) {
                keep = d.getAxis();
            }
        }
        int count = 0;
        Direction only = null;
        for (Direction d : Direction.values()) {
            BlockState n = level.getBlockState(pos.relative(d));
            boolean connected = connectsTo(n, d);
            state = state.setValue(PROPERTY_BY_DIRECTION.get(d), connected).setValue(RIM.get(d), connected && !(n.getBlock() instanceof ReinforcedPipeBlock));
            if (connected) {
                count++;
                only = d;
            }
        }
        if (count == 1) {
            state = state.setValue(RIM.get(only.getOpposite()), true);
        } else if (count == 0) {
            Direction.Axis axis = keep != null ? keep : loneAxis != null ? loneAxis : Direction.Axis.Z;
            state = state.setValue(RIM.get(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE)), true)
                    .setValue(RIM.get(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE)), true);
        }
        return state;
    }

    public static boolean connectsTo(BlockState neighbor, Direction towards) {
        Block b = neighbor.getBlock();
        if (b instanceof ReinforcedPipeBlock) {
            return true;
        }
        if (b instanceof CoolantPortBlock) {
            return neighbor.getValue(FacingBlock.FACING) == towards.getOpposite();
        }
        if (b instanceof CirculationPumpBlock) {
            return neighbor.getValue(CirculationPumpBlock.FACING).getAxis() == towards.getAxis();
        }
        if (b instanceof SteamTurbineBlock) {
            return towards.getAxis() == Direction.Axis.Y;
        }
        if (b instanceof PipeConnectable connectable) {
            return connectable.connectsOn(neighbor, towards.getOpposite());
        }
        return false;
    }
}
