package com.criticalmass.content.kinetics;

import com.criticalmass.content.CMShapes;
import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
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

public class ControlRodDriveBlock extends HorizontalKineticBlock implements IBE<ControlRodDriveBlockEntity> {
    private static final VoxelShape NORTH = Shapes.or(box(0, 0, 0, 16, 3, 16), box(3, 3, 3, 13, 12, 13), box(7, 12, 7, 9, 16, 9));

    public ControlRodDriveBlock(Properties properties) {
        super(properties);
    }

    public static Direction inputSide(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getOpposite();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == inputSide(state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CMShapes.rotated(NORTH, state.getValue(HORIZONTAL_FACING));
    }

    @Override
    public Class<ControlRodDriveBlockEntity> getBlockEntityClass() {
        return ControlRodDriveBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ControlRodDriveBlockEntity> getBlockEntityType() {
        return CMBlockEntities.CONTROL_ROD_DRIVE.get();
    }
}
