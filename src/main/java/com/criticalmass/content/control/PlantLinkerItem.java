package com.criticalmass.content.control;

import com.criticalmass.registry.CMComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PlantLinkerItem extends Item {
    public PlantLinkerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockEntity be = level.getBlockEntity(pos);
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (be instanceof PanelBlockEntity panel) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            BlockPos stored = stack.get(CMComponents.LINK_TARGET.get());
            if (stored == null) {
                player.displayClientMessage(Component.translatable("criticalmass.linker.nothing_stored"), true);
                return InteractionResult.FAIL;
            }
            if (!panel.bind(stored)) {
                player.displayClientMessage(Component.translatable("criticalmass.linker.refused"), true);
                return InteractionResult.FAIL;
            }
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.4f, 1.6f);
            player.displayClientMessage(Component.translatable("criticalmass.linker.bound", level.getBlockState(stored).getBlock().getName(), stored.getX(), stored.getY(), stored.getZ()), true);
            return InteractionResult.SUCCESS;
        }
        if (PlantChannel.isTarget(be)) {
            if (!level.isClientSide) {
                stack.set(CMComponents.LINK_TARGET.get(), pos.immutable());
                level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4f, 1.4f);
                player.displayClientMessage(Component.translatable("criticalmass.linker.stored", level.getBlockState(pos).getBlock().getName(), pos.getX(), pos.getY(), pos.getZ()), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.has(CMComponents.LINK_TARGET.get())) {
            if (!level.isClientSide) {
                stack.remove(CMComponents.LINK_TARGET.get());
                player.displayClientMessage(Component.translatable("criticalmass.linker.cleared"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(CMComponents.LINK_TARGET.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BlockPos stored = stack.get(CMComponents.LINK_TARGET.get());
        tooltip.add(Component.translatable("criticalmass.linker.tooltip").withStyle(ChatFormatting.GRAY));
        if (stored != null) {
            tooltip.add(Component.translatable("criticalmass.linker.tooltip_stored", stored.getX(), stored.getY(), stored.getZ()).withStyle(ChatFormatting.AQUA));
        }
    }
}
