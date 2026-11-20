package com.criticalmass.content.thermal;

import com.criticalmass.registry.CMBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CoreDebrisBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 9, 15);

    public CoreDebrisBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Source(CMBlockEntities.CORE_DEBRIS.get(), pos, state);
    }

    public static class Source extends BlockEntity {
        public Source(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }

        @Override
        public void onLoad() {
            super.onLoad();
            LoadedThermalBlocks.add(this);
        }

        @Override
        public void setRemoved() {
            LoadedThermalBlocks.remove(this);
            super.setRemoved();
        }
    }
}
