package com.criticalmass.content.fuel;

import com.criticalmass.content.thermal.NodeBlocks;
import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class SpentFuelRackBlock extends Block implements IBE<SpentFuelRackBlockEntity> {
    public static final int FILL_MAX = 4;
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, FILL_MAX);

    public SpentFuelRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FILL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof FuelAssemblyItem)) {
            return onBlockEntityUseItemOn(level, pos, be -> NodeBlocks.fill(level, pos, player, hand, be.coolant()));
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        return onBlockEntityUseItemOn(level, pos, be -> {
            if (!be.insert(stack)) {
                player.displayClientMessage(Component.translatable("criticalmass.rack.full"), true);
                return ItemInteractionResult.FAIL;
            }
            stack.shrink(1);
            return ItemInteractionResult.SUCCESS;
        });
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return onBlockEntityUse(level, pos, be -> {
            ItemStack out = be.takeLast();
            if (out.isEmpty()) {
                return InteractionResult.PASS;
            }
            if (!player.getInventory().add(out)) {
                player.drop(out, false);
            }
            return InteractionResult.SUCCESS;
        });
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SpentFuelRackBlockEntity rack ? rack.count() * 15 / rack.slots().size() : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<SpentFuelRackBlockEntity> getBlockEntityClass() {
        return SpentFuelRackBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SpentFuelRackBlockEntity> getBlockEntityType() {
        return CMBlockEntities.SPENT_FUEL_RACK.get();
    }
}
