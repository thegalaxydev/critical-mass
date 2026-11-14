package com.criticalmass.content.control;

import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ScreenBlock extends HorizontalKineticBlock implements EntityBlock, ICogWheel {
    public static final Property<Direction> FACING = HORIZONTAL_FACING;
    private static final VoxelShape ALONG_X = Block.box(0, 0, 3, 16, 16, 13);
    private static final VoxelShape ALONG_Z = Block.box(3, 0, 0, 13, 16, 16);

    public ScreenBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? ALONG_X : ALONG_Z;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        BlockState on = context.getLevel().getBlockState(context.getClickedPos().relative(face.getOpposite()));
        Direction facing = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
        if (on.getBlock() == this && face.getAxis() != on.getValue(FACING).getAxis()) {
            facing = on.getValue(FACING);
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public float getParticleTargetRadius() {
        return 0.85f;
    }

    @Override
    public float getParticleInitialRadius() {
        return 0.75f;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ScreenBlockEntity(CMBlockEntities.PLANT_SCREEN.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == CMBlockEntities.PLANT_SCREEN.get() ? (l, p, s, be) -> ((ScreenBlockEntity) be).tick() : null;
    }
}
