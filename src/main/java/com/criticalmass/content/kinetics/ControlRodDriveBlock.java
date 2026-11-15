package com.criticalmass.content.kinetics;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
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
    private static final VoxelShape NORTH = Shapes.or(box(0, 0, 0, 16, 2, 16), box(2, 2, 2, 14, 12, 14), box(0, 12, 0, 16, 16, 16));

    public ControlRodDriveBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CMShapes.rotated(NORTH, state.getValue(HORIZONTAL_FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return onBlockEntityUse(level, pos, be -> {
            be.setBank(be.bank() + (player.isShiftKeyDown() ? -1 : 1));
            player.displayClientMessage(Component.translatable("criticalmass.rod.bank_set", String.valueOf((char) ('A' + be.bank()))), true);
            return InteractionResult.SUCCESS;
        });
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
