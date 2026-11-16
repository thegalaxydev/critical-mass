package com.criticalmass.content.control;

import com.criticalmass.content.kinetics.TurbineStructure;
import com.criticalmass.registry.CMBlocks;
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
import net.minecraft.world.level.block.Block;

public class DashPartItem extends Item {
    public final DashPart.Type type;

    public DashPartItem(DashPart.Type type, Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = TurbineStructure.resolve(level, context.getClickedPos());
        if (LoconauticsCompat.theirDesk(level.getBlockState(pos)) && (player == null || !player.isShiftKeyDown())) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            DashboardBlock ours = CMBlocks.REACTOR_ALLOY_DASHBOARD.get();
            if (!ours.adopt(level, pos, player) || !(level.getBlockEntity(pos) instanceof DashboardBlockEntity desk)) {
                return InteractionResult.FAIL;
            }
            DashboardBlock.fit(level, pos, desk, DashboardBlock.slotAt(level.getBlockState(pos), pos, context.getClickLocation()), type, context.getItemInHand(), player);
            return InteractionResult.SUCCESS;
        }
        Block clicked = level.getBlockState(pos).getBlock();
        if (clicked instanceof DashboardBlock || clicked instanceof PartPanelBlock) {
            return InteractionResult.PASS;
        }
        if (player == null || !player.isShiftKeyDown()) {
            return PartPanelBlock.mount(context, type);
        }
        if (!DashPart.accepts(type, level, pos)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("criticalmass.dashboard.item.refused"), true);
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            context.getItemInHand().set(CMComponents.LINK_TARGET.get(), pos.immutable());
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4f, 1.4f);
            player.displayClientMessage(Component.translatable("criticalmass.dashboard.item.bound", level.getBlockState(pos).getBlock().getName(), pos.getX(), pos.getY(), pos.getZ()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.has(CMComponents.LINK_TARGET.get())) {
            if (!level.isClientSide) {
                stack.remove(CMComponents.LINK_TARGET.get());
                player.displayClientMessage(Component.translatable("criticalmass.dashboard.item.cleared"), true);
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
        tooltip.add(Component.translatable("criticalmass.dashboard.item.tooltip." + type.key).withStyle(ChatFormatting.GRAY));
        BlockPos stored = stack.get(CMComponents.LINK_TARGET.get());
        if (stored != null) {
            tooltip.add(Component.translatable("criticalmass.dashboard.item.tooltip_bound", stored.getX(), stored.getY(), stored.getZ()).withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("criticalmass.dashboard.credit").withStyle(ChatFormatting.DARK_GRAY));
    }
}
