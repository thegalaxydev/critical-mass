package com.criticalmass.content.fuel;

import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SealedWasteCanisterBlock extends WasteCanisterBlock implements IBE<SealedWasteCanisterBlockEntity> {
    public SealedWasteCanisterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<SealedWasteCanisterBlockEntity> getBlockEntityClass() {
        return SealedWasteCanisterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SealedWasteCanisterBlockEntity> getBlockEntityType() {
        return CMBlockEntities.SEALED_WASTE_CANISTER.get();
    }
}
