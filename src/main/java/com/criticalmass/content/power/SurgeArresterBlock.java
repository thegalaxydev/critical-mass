package com.criticalmass.content.power;

import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SurgeArresterBlock extends Block implements IBE<SurgeArresterBlockEntity> {
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

    public SurgeArresterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            WireItem.dropWires(level, pos);
        }
        IBE.onRemove(state, level, pos, newState);
        PowerNetwork.changed(level);
    }

    @Override
    public Class<SurgeArresterBlockEntity> getBlockEntityClass() {
        return SurgeArresterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SurgeArresterBlockEntity> getBlockEntityType() {
        return CMBlockEntities.SURGE_ARRESTER.get();
    }
}
