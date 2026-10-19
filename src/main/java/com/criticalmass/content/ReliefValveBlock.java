package com.criticalmass.content;

import com.criticalmass.content.thermal.ReliefValveBlockEntity;
import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;

public class ReliefValveBlock extends FacingBlock implements IBE<ReliefValveBlockEntity>, PipeConnectable {
    public static final BooleanProperty VENTING = BooleanProperty.create("venting");

    public ReliefValveBlock(Properties properties) {
        super(properties, Shapes.or(Block.box(3, 3, 12, 13, 13, 16), Block.box(4, 4, 6, 12, 12, 12), Block.box(6, 6, 2, 10, 10, 6), Block.box(7, 10, 3, 9, 14, 5)));
        registerDefaultState(defaultBlockState().setValue(VENTING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VENTING);
    }

    @Override
    public boolean connectsOn(BlockState state, Direction face) {
        return face.getAxis() != state.getValue(FACING).getAxis();
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(VENTING) ? 15 : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<ReliefValveBlockEntity> getBlockEntityClass() {
        return ReliefValveBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ReliefValveBlockEntity> getBlockEntityType() {
        return CMBlockEntities.RELIEF_VALVE.get();
    }
}
