package com.criticalmass.content.fuel;

import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ServicePortBlock extends DirectionalKineticBlock implements IBE<ServicePortBlockEntity> {
    public ServicePortBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof FuelAssemblyItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        return onBlockEntityUseItemOn(level, pos, be -> {
            String reason = be.refuseReason(stack);
            if (reason != null) {
                player.displayClientMessage(Component.translatable(reason), true);
                return ItemInteractionResult.FAIL;
            }
            ItemStack rest = be.insert(stack, false);
            if (rest.getCount() == stack.getCount()) {
                return ItemInteractionResult.FAIL;
            }
            player.setItemInHand(hand, rest);
            return ItemInteractionResult.SUCCESS;
        });
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return onBlockEntityUse(level, pos, be -> {
            ItemStack out = be.takeOutput(false);
            if (out.isEmpty()) {
                player.displayClientMessage(Component.translatable(be.open() ? be.status() : "criticalmass.service.closed"), true);
                return InteractionResult.PASS;
            }
            if (!player.getInventory().add(out)) {
                player.drop(out, false);
            }
            return InteractionResult.SUCCESS;
        });
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<ServicePortBlockEntity> getBlockEntityClass() {
        return ServicePortBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ServicePortBlockEntity> getBlockEntityType() {
        return CMBlockEntities.SERVICE_PORT.get();
    }
}
