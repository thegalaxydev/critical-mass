package com.criticalmass.content.power;

import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;

public class DieselEngineBlock extends DirectionalKineticBlock implements IBE<DieselEngineBlockEntity> {
    public DieselEngineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferred = getPreferredFacing(context);
        Direction facing = preferred != null && preferred.getAxis().isHorizontal() && (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown())
                ? preferred : context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return direction != null && direction.getOpposite() != state.getValue(FACING);
    }

    @Override
    public boolean hideStressImpact() {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return onBlockEntityUse(level, pos, be -> {
            player.displayClientMessage(Component.translatable(be.toggle() ? "criticalmass.engine.called" : "criticalmass.engine.released"), true);
            return InteractionResult.SUCCESS;
        });
    }

    @Override
    public Class<DieselEngineBlockEntity> getBlockEntityClass() {
        return DieselEngineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DieselEngineBlockEntity> getBlockEntityType() {
        return CMBlockEntities.DIESEL_ENGINE.get();
    }
}
