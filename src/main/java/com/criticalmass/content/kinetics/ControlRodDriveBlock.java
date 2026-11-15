package com.criticalmass.content.kinetics;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import com.criticalmass.content.CMShapes;
import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ControlRodDriveBlock extends HorizontalKineticBlock implements IBE<ControlRodDriveBlockEntity>, ICogWheel {
    private static final VoxelShape NORTH = Shapes.or(box(0, 0, 0, 16, 2, 16), box(3, 2, 3, 13, 6, 13), box(0, 6, 0, 16, 10, 16), box(3, 10, 3, 13, 14, 13), box(2, 14, 2, 14, 15, 14), box(6, 15, 6, 10, 16, 10));

    public ControlRodDriveBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
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
