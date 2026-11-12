package com.criticalmass.content;

import com.criticalmass.content.thermal.InstrumentationPanelBlockEntity;
import com.criticalmass.registry.CMBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class InstrumentationPanelBlock extends WallPanelBlock implements IBE<InstrumentationPanelBlockEntity> {
    public static final MapCodec<InstrumentationPanelBlock> CODEC = simpleCodec(InstrumentationPanelBlock::new);

    public InstrumentationPanelBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof InstrumentationPanelBlockEntity panel ? panel.comparator() : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<InstrumentationPanelBlockEntity> getBlockEntityClass() {
        return InstrumentationPanelBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends InstrumentationPanelBlockEntity> getBlockEntityType() {
        return CMBlockEntities.INSTRUMENTATION_PANEL.get();
    }
}
