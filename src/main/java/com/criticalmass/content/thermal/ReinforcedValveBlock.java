package com.criticalmass.content.thermal;

import com.criticalmass.content.PipeConnectable;
import com.criticalmass.content.ReinforcedPipeBlock;
import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ReinforcedValveBlock extends DirectionalAxisKineticBlock implements IBE<ReinforcedValveBlockEntity>, PipeConnectable {
    public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");

    public ReinforcedValveBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ENABLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(ENABLED));
    }

    public static Direction.Axis getPipeAxis(BlockState state) {
        Direction facing = state.getValue(FACING);
        boolean alongFirst = !state.getValue(AXIS_ALONG_FIRST_COORDINATE);
        for (Direction.Axis axis : Direction.Axis.values()) {
            if (axis == facing.getAxis()) {
                continue;
            }
            if (!alongFirst) {
                alongFirst = true;
                continue;
            }
            return axis;
        }
        throw new IllegalStateException("Impossible axis.");
    }

    public static boolean isOpen(BlockState state) {
        return state.getValue(ENABLED);
    }

    @Override
    public boolean connectsOn(BlockState state, Direction face) {
        return face.getAxis() == getPipeAxis(state);
    }

    @Override
    protected boolean prefersConnectionTo(LevelReader reader, BlockPos pos, Direction facing, boolean shaftAxis) {
        if (!shaftAxis) {
            return ReinforcedPipeBlock.connectsTo(reader.getBlockState(pos.relative(facing)), facing);
        }
        return super.prefersConnectionTo(reader, pos, facing, shaftAxis);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return AllShapes.FLUID_VALVE.get(getPipeAxis(state));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public Class<ReinforcedValveBlockEntity> getBlockEntityClass() {
        return ReinforcedValveBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ReinforcedValveBlockEntity> getBlockEntityType() {
        return CMBlockEntities.REINFORCED_VALVE.get();
    }
}
