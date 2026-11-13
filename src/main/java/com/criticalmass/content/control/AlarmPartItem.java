package com.criticalmass.content.control;

import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.registry.CMComponents;
import com.lycoris.loconautics.content.controls.panel.PanelBlockItem;
import com.lycoris.loconautics.content.controls.panel.PanelPart;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.level.block.Block;

public class AlarmPartItem extends PanelBlockItem {
    public AlarmPartItem(Block panel, Properties properties) {
        super(panel, properties);
    }

    @Override
    protected PanelPart partFor(ItemStack stack) {
        return new AlarmPart(stack.get(CMComponents.LINK_TARGET.get()), 0, 0);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (level.getBlockEntity(pos) instanceof ReactorControllerBlockEntity) {
            if (!level.isClientSide) {
                context.getItemInHand().set(CMComponents.LINK_TARGET.get(), pos.immutable());
                level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4f, 1.4f);
                if (player != null) {
                    player.displayClientMessage(Component.translatable("criticalmass.alarm_part.bound", pos.getX(), pos.getY(), pos.getZ()), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.has(CMComponents.LINK_TARGET.get())) {
            if (!level.isClientSide) {
                stack.remove(CMComponents.LINK_TARGET.get());
                player.displayClientMessage(Component.translatable("criticalmass.alarm_part.cleared"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(CMComponents.LINK_TARGET.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("criticalmass.alarm_part.tooltip").withStyle(ChatFormatting.GRAY));
        BlockPos stored = stack.get(CMComponents.LINK_TARGET.get());
        if (stored != null) {
            tooltip.add(Component.translatable("criticalmass.alarm_part.tooltip_bound", stored.getX(), stored.getY(), stored.getZ()).withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public String getDescriptionId() {
        return getOrCreateDescriptionId();
    }

    @Override
    public void registerBlocks(Map<Block, Item> blockToItemMap, Item item) {
    }
}
