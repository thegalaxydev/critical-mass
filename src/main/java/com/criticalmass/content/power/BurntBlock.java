package com.criticalmass.content.power;

import com.criticalmass.registry.CMBlockEntities;
import com.criticalmass.registry.CMSounds;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BurntBlock extends Block implements IBE<BurntBlockEntity> {
    public BurntBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BurntBlockEntity wreck) || !wreck.wants(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        wreck.supply(stack);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, CMSounds.SERVICE.get(), SoundSource.BLOCKS, 1.0f, 0.9f);
        if (wreck.complete()) {
            wreck.restore();
            player.displayClientMessage(Component.translatable("criticalmass.burnt.repaired"), true);
        } else {
            player.displayClientMessage(wreck.needs(), true);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BurntBlockEntity wreck) {
            player.displayClientMessage(wreck.needs(), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Class<BurntBlockEntity> getBlockEntityClass() {
        return BurntBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BurntBlockEntity> getBlockEntityType() {
        return CMBlockEntities.BURNT_EQUIPMENT.get();
    }
}
