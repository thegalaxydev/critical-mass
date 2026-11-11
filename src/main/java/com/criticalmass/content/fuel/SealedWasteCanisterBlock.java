package com.criticalmass.content.fuel;

import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SealedWasteCanisterBlock extends Block implements IBE<SealedWasteCanisterBlockEntity> {
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public SealedWasteCanisterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
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
