package com.criticalmass.content.kinetics;

import com.criticalmass.content.CMShapes;
import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CirculationPumpBlock extends DirectionalAxisKineticBlock implements IBE<CirculationPumpBlockEntity> {
    private static final VoxelShape NORTH = Shapes.or(
            box(2, 2, 4, 14, 14, 12),
            box(5, 5, 2, 11, 11, 14),
            box(4, 4, 0, 12, 12, 2),
            box(4, 4, 14, 12, 12, 16));

    public CirculationPumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == getRotationAxis(state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CMShapes.rotated(NORTH, state.getValue(FACING));
    }

    @Override
    public Class<CirculationPumpBlockEntity> getBlockEntityClass() {
        return CirculationPumpBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CirculationPumpBlockEntity> getBlockEntityType() {
        return CMBlockEntities.CIRCULATION_PUMP.get();
    }
}
