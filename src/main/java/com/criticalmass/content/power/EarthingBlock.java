package com.criticalmass.content.power;

import com.criticalmass.content.HorizontalBlock;
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
import net.minecraft.world.phys.BlockHitResult;

public class EarthingBlock extends HorizontalBlock implements IBE<EarthingBlockEntity> {
    public EarthingBlock(Properties properties) {
        super(properties, Block.box(3, 0, 3, 13, 11, 13), false);
        registerDefaultState(defaultBlockState().setValue(BreakerBlock.CLOSED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BreakerBlock.CLOSED);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return stack.getItem() instanceof WireItem ? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return onBlockEntityUse(level, pos, be -> {
            player.displayClientMessage(Component.translatable(be.toggle()), true);
            return InteractionResult.SUCCESS;
        });
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
    public Class<EarthingBlockEntity> getBlockEntityClass() {
        return EarthingBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EarthingBlockEntity> getBlockEntityType() {
        return CMBlockEntities.EARTHING_SWITCH.get();
    }
}
