package com.criticalmass.content.thermal;

import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PressurizerBlock extends Block implements IBE<PressurizerBlockEntity> {
    public PressurizerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof PressurizerBlockEntity p ? (int) Math.round(15 * p.pressure() / CMThermal.PRESSURIZER_MAX) : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<PressurizerBlockEntity> getBlockEntityClass() {
        return PressurizerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PressurizerBlockEntity> getBlockEntityType() {
        return CMBlockEntities.PRESSURIZER.get();
    }
}
