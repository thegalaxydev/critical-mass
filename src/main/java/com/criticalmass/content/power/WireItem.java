package com.criticalmass.content.power;

import com.criticalmass.registry.CMSounds;
import com.criticalmass.registry.CMComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
    private final WireType type;

    public WireItem(Properties properties, WireType type) {
        super(properties);
        this.type = type;
    }

    public WireType type() {
        return type;
    }

    public static void dropWires(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        for (WireType cut : WireLinks.cutAll(level, pos)) {
            Block.popResource(level, pos, new ItemStack(cut.item()));
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

    private static void touch(Level level, Player player, BlockPos pos, int terminal, double kilovolts) {
        if (player != null) {
            Hazard.shock(player, PowerNetwork.terminal(level, pos, terminal), kilovolts);
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
        double kilovolts = PowerNetwork.kilovolts(level, pos, terminal);
        if (player != null && player.isShiftKeyDown()) {
            WireType cut = WireLinks.cutLast(level, pos, terminal);
            if (cut != null) {
                if (!player.isCreative()) {
                    player.getInventory().placeItemBackInInventory(new ItemStack(cut.item()));
                }
                level.playSound(null, pos, CMSounds.WIRE_CUT.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
                tell(player, "criticalmass.wire.cut");
                touch(level, player, pos, terminal, kilovolts);
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
            touch(level, player, pos, terminal, kilovolts);
            return InteractionResult.SUCCESS;
        }
        String problem = WireLinks.connect(level, from, storedTerminal(stack), pos, terminal, type);
        forget(stack);
        if (!problem.isEmpty()) {
            tell(player, problem);
            return InteractionResult.FAIL;
        }
        if (player == null || !player.isCreative()) {
            stack.shrink(1);
        }
        level.playSound(null, pos, CMSounds.WIRE_STRING.get(), SoundSource.BLOCKS, 0.8f, 0.95f + level.random.nextFloat() * 0.1f);
        tell(player, "criticalmass.wire.joined");
        touch(level, player, pos, terminal, kilovolts);
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
        tooltip.add(Component.translatable("criticalmass.wire.rating", ratingNumber(type.kilovolts), ratingNumber(type.kiloamps)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("criticalmass.wire.tooltip").withStyle(ChatFormatting.GRAY));
        BlockPos from = stack.get(CMComponents.LINK_TARGET.get());
        if (from != null) {
            tooltip.add(Component.translatable("criticalmass.wire.tooltip_from", from.getX(), from.getY(), from.getZ()).withStyle(ChatFormatting.AQUA));
        }
    }

    private static String ratingNumber(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }
}
