package com.criticalmass.content.kinetics;

import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SteamTurbineBlock extends HorizontalAxisKineticBlock implements IBE<SteamTurbineBlockEntity> {
    private static final VoxelShape X = Shapes.or(box(0, 1, 1, 16, 15, 15), box(5, 0, 5, 11, 16, 11));
    private static final VoxelShape Z = Shapes.or(box(1, 1, 0, 15, 15, 16), box(5, 0, 5, 11, 16, 11));

    public SteamTurbineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HORIZONTAL_AXIS) == Direction.Axis.X ? X : Z;
    }

    @Override
    public Class<SteamTurbineBlockEntity> getBlockEntityClass() {
        return SteamTurbineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteamTurbineBlockEntity> getBlockEntityType() {
        return CMBlockEntities.STEAM_TURBINE.get();
    }
}
