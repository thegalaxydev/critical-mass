package com.criticalmass.content.power;

import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public class WireItem extends Item {
    private static final String TERMINAL = "WireTerminal";

    public WireItem(Properties properties) {
        super(properties);
    }

    public static void dropWires(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        int cut = WireLinks.cutAll(level, pos);
        if (cut > 0) {
            Block.popResource(level, pos, new ItemStack(CMItems.POWER_WIRE.get(), cut));
        }
    }

    private static int storedTerminal(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : data.copyTag().getInt(TERMINAL);
    }

    private static void forget(ItemStack stack) {
        stack.remove(CMComponents.LINK_TARGET.get());
        stack.remove(DataComponents.CUSTOM_DATA);
    }

    private static void tell(Player player, String key, Object... args) {
        if (player != null) {
            player.displayClientMessage(Component.translatable(key, args), true);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        WireNode node = WireLinks.node(level, pos);
        if (node == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        int terminal = node.terminalAt(context.getClickLocation().subtract(Vec3.atLowerCornerOf(pos)));
        if (player != null && player.isShiftKeyDown()) {
            if (WireLinks.cutLast(level, pos, terminal)) {
                if (!player.isCreative()) {
                    player.getInventory().placeItemBackInInventory(new ItemStack(this));
                }
                level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.5f, 1.4f);
                tell(player, "criticalmass.wire.cut");
            } else {
                tell(player, "criticalmass.wire.none");
            }
            return InteractionResult.SUCCESS;
        }
        BlockPos from = stack.get(CMComponents.LINK_TARGET.get());
        if (from == null) {
            stack.set(CMComponents.LINK_TARGET.get(), pos.immutable());
            CompoundTag tag = new CompoundTag();
            tag.putInt(TERMINAL, terminal);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            tell(player, "criticalmass.wire.started", level.getBlockState(pos).getBlock().getName());
            return InteractionResult.SUCCESS;
        }
        String problem = WireLinks.connect(level, from, storedTerminal(stack), pos, terminal);
        forget(stack);
        if (!problem.isEmpty()) {
            tell(player, problem);
            return InteractionResult.FAIL;
        }
        if (player == null || !player.isCreative()) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 0.8f, 1.2f);
        tell(player, "criticalmass.wire.joined");
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.has(CMComponents.LINK_TARGET.get())) {
            if (!level.isClientSide) {
                forget(stack);
                tell(player, "criticalmass.wire.dropped");
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
        tooltip.add(Component.translatable("criticalmass.wire.tooltip").withStyle(ChatFormatting.GRAY));
        BlockPos from = stack.get(CMComponents.LINK_TARGET.get());
        if (from != null) {
            tooltip.add(Component.translatable("criticalmass.wire.tooltip_from", from.getX(), from.getY(), from.getZ()).withStyle(ChatFormatting.AQUA));
        }
    }
}
