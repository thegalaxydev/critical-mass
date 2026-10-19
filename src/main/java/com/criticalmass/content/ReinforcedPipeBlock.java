package com.criticalmass.content;

import com.criticalmass.content.kinetics.CirculationPumpBlock;
import com.criticalmass.content.kinetics.SteamTurbineBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class ReinforcedPipeBlock extends PipeBlock {
    public static final MapCodec<ReinforcedPipeBlock> CODEC = simpleCodec(ReinforcedPipeBlock::new);

    public ReinforcedPipeBlock(Properties properties) {
        super(0.25f, properties);
        BlockState state = stateDefinition.any();
        for (Direction d : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(d), false);
        }
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends PipeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(neighbor, direction));
    }

    public static BlockState withConnections(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(d), connectsTo(level.getBlockState(pos.relative(d)), d));
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
