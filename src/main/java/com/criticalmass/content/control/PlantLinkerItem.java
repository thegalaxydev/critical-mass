package com.criticalmass.content.control;

import com.criticalmass.registry.CMSounds;
import com.criticalmass.content.kinetics.TurbineStructure;
import com.criticalmass.registry.CMComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
        BlockPos pos = TurbineStructure.resolve(level, context.getClickedPos());
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockEntity be = level.getBlockEntity(pos);
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (be instanceof MeltdownSirenBlockEntity siren) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            BlockPos stored = stack.get(CMComponents.LINK_TARGET.get());
            if (stored == null) {
                if (siren.reactor() == null) {
                    player.displayClientMessage(Component.translatable("criticalmass.linker.nothing_stored"), true);
                    return InteractionResult.FAIL;
                }
                siren.unlink();
                player.displayClientMessage(Component.translatable("criticalmass.siren.link.cleared"), true);
                return InteractionResult.SUCCESS;
            }
            if (!siren.link(stored)) {
                player.displayClientMessage(Component.translatable("criticalmass.siren.link.refused"), true);
                return InteractionResult.FAIL;
            }
            level.playSound(null, pos, CMSounds.LINK_BOUND.get(), SoundSource.BLOCKS, 0.6f, 1.0f);
            player.displayClientMessage(Component.translatable("criticalmass.linker.bound", level.getBlockState(stored).getBlock().getName(), stored.getX(), stored.getY(), stored.getZ()), true);
            return InteractionResult.SUCCESS;
        }
        ScreenBlockEntity screen = ScreenBlockEntity.controllerOf(level, pos);
        if (screen != null) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            BlockPos stored = stack.get(CMComponents.LINK_TARGET.get());
            if (stored == null) {
                if (!screen.linked()) {
                    player.displayClientMessage(Component.translatable("criticalmass.linker.nothing_stored"), true);
                    return InteractionResult.FAIL;
                }
                screen.unlink();
                player.displayClientMessage(Component.translatable("criticalmass.screen.link.cleared"), true);
                return InteractionResult.SUCCESS;
            }
            if (!screen.link(stored)) {
                player.displayClientMessage(Component.translatable("criticalmass.screen.link.refused"), true);
                return InteractionResult.FAIL;
            }
            level.playSound(null, pos, CMSounds.LINK_BOUND.get(), SoundSource.BLOCKS, 0.6f, 1.0f);
            if (screen.linked() && player instanceof net.minecraft.server.level.ServerPlayer linker) {
                com.criticalmass.registry.CMTriggers.grant(linker, "screen_linked");
            }
            if (screen.linked() && !screen.bigEnough()) {
                player.displayClientMessage(Component.translatable("criticalmass.screen.link.too_small", screen.width(), screen.height(), ScreenBlockEntity.MIN_WIDTH, ScreenBlockEntity.MIN_HEIGHT), true);
            } else {
                player.displayClientMessage(Component.translatable("criticalmass.linker.bound", level.getBlockState(stored).getBlock().getName(), stored.getX(), stored.getY(), stored.getZ()), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (PlantChannel.isTarget(be) || CoreMap.isSource(be)) {
            if (!level.isClientSide) {
                stack.set(CMComponents.LINK_TARGET.get(), pos.immutable());
                level.playSound(null, pos, CMSounds.RELAY_CLICK.get(), SoundSource.BLOCKS, 0.6f, 1.2f);
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
