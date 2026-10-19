package com.criticalmass.content.thermal;

import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.content.PipeConnectable;
import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;

public class BypassValveBlock extends HorizontalBlock implements IBE<BypassValveBlockEntity>, PipeConnectable {
    public BypassValveBlock(Properties properties) {
        super(properties, Shapes.or(Block.box(3, 3, 3, 13, 13, 13), Block.box(0, 4, 4, 16, 12, 12), Block.box(4, 4, 0, 12, 12, 3), Block.box(4, 13, 4, 12, 16, 12)), false);
    }

    public static Direction.Axis lineAxis(BlockState state) {
        return state.getValue(FACING).getClockWise().getAxis();
    }

    @Override
    public boolean connectsOn(BlockState state, Direction face) {
        return face.getAxis() == lineAxis(state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<BypassValveBlockEntity> getBlockEntityClass() {
        return BypassValveBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BypassValveBlockEntity> getBlockEntityType() {
        return CMBlockEntities.BYPASS_VALVE.get();
    }
}
