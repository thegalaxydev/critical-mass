package com.criticalmass.content.thermal;

import com.criticalmass.registry.CMParticles;
import com.criticalmass.registry.CMSounds;
import com.criticalmass.registry.CMItems;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class WeldingTorchItem extends Item {
    private static final double ARC_STANDOFF = 0.12;
    private static final double ARC_FLASH = 0.5;
    private static final Map<UUID, BlockPos> TARGETS = new ConcurrentHashMap<>();

    public WeldingTorchItem(Properties properties) {
        super(properties);
    }

    @Nullable
    private static ReactorControllerBlockEntity owner(Level level, BlockPos pos) {
        for (BlockEntity be : LoadedThermalBlocks.snapshot()) {
            if (be instanceof ReactorControllerBlockEntity c && be.getLevel() == level && c.owns(pos)) {
                return c;
            }
        }
        return null;
    }

    public static boolean needsRepair(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof VesselCasingBlock)) {
            return false;
        }
        ReactorControllerBlockEntity c = owner(level, pos);
        return c != null ? c.integrity(pos) < 1 : state.getValue(VesselCasingBlock.CRACK) > 0;
    }

    public static boolean allowed(Level level, BlockPos pos) {
        ReactorControllerBlockEntity c = owner(level, pos);
        return c == null || c.repairAllowed();
    }

    public static boolean repairAt(Level level, BlockPos pos) {
        if (!needsRepair(level, pos) || !allowed(level, pos)) {
            return false;
        }
        ReactorControllerBlockEntity c = owner(level, pos);
        if (c != null) {
            return c.repair(pos, CMThermal.REPAIR_AMOUNT);
        }
        BlockState state = level.getBlockState(pos);
        level.setBlock(pos, state.setValue(VesselCasingBlock.CRACK, Math.max(0, state.getValue(VesselCasingBlock.CRACK) - 2)), Block.UPDATE_ALL);
        return true;
    }

    public static void cut(Level level, BlockPos pos, @org.jetbrains.annotations.Nullable Player player) {
        level.destroyBlock(pos, false);
        Block.popResource(level, pos, new ItemStack(CMItems.STEEL_PLATE.get()));
        if (level.random.nextInt(2) == 0) {
            Block.popResource(level, pos, new ItemStack(CMItems.STEEL_NUGGET.get(), 1 + level.random.nextInt(4)));
        }
        if (level.random.nextInt(4) == 0) {
            Block.popResource(level, pos, new ItemStack(CMItems.REACTOR_ALLOY_SHEET.get()));
        }
        level.playSound(null, pos, CMSounds.TORCH_DONE.get(), SoundSource.BLOCKS, 0.7f, 0.85f);
        if (player instanceof net.minecraft.server.level.ServerPlayer worker) {
            com.criticalmass.registry.CMTriggers.grant(worker, "wreckage_cut");
        }
    }

    public static ItemStack findPlate(Player player) {
        if (player.getOffhandItem().is(CMItems.STEEL_PLATES)) {
            return player.getOffhandItem();
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(CMItems.STEEL_PLATES)) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player != null && level.getBlockState(pos).is(com.criticalmass.registry.CMBlocks.WRECKAGE.get())) {
            TARGETS.put(player.getUUID(), pos.immutable());
            player.startUsingItem(context.getHand());
            return InteractionResult.CONSUME;
        }
        if (player == null || !needsRepair(level, pos)) {
            return InteractionResult.PASS;
        }
        if (!allowed(level, pos)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("criticalmass.repair.too_hot"), true);
            }
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild && findPlate(player).isEmpty()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("criticalmass.welder.needs_plate"), true);
            }
            return InteractionResult.FAIL;
        }
        TARGETS.put(player.getUUID(), pos.immutable());
        player.startUsingItem(context.getHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!level.isClientSide || remaining % 2 != 0 || !(entity instanceof Player player)) {
            return;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 reach = eye.add(player.getViewVector(1.0f).scale(player.blockInteractionRange()));
        BlockHitResult hit = level.clip(new ClipContext(eye, reach, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        Vec3 at = hit.getLocation().add(eye.subtract(hit.getLocation()).normalize().scale(ARC_STANDOFF));
        for (int i = 0; i < 3; i++) {
            level.addParticle(CMParticles.ARC_SPARK.get(), at.x, at.y, at.z, (level.random.nextDouble() - 0.5) * 0.3, level.random.nextDouble() * 0.2, (level.random.nextDouble() - 0.5) * 0.3);
        }
        level.addParticle(CMParticles.EMBER.get(), at.x, at.y, at.z, (level.random.nextDouble() - 0.5) * 0.06, 0.03, (level.random.nextDouble() - 0.5) * 0.06);
        if (remaining % 4 == 0) {
            level.addParticle(CMParticles.GLOW.get(), true, at.x, at.y, at.z, ARC_FLASH, 0, 0);
        }
        if (remaining % 8 == 0) {
            level.playLocalSound(at.x, at.y, at.z, CMSounds.TORCH_ARC.get(), SoundSource.PLAYERS, 0.35f, 0.95f + level.random.nextFloat() * 0.1f, false);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return CMThermal.REPAIR_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BRUSH;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        TARGETS.remove(entity.getUUID());
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        BlockPos pos = TARGETS.remove(entity.getUUID());
        if (level.isClientSide || pos == null || !(entity instanceof Player player)) {
            return stack;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 reach = eye.add(player.getViewVector(1.0f).scale(player.blockInteractionRange()));
        BlockHitResult hit = level.clip(new ClipContext(eye, reach, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(pos)) {
            return stack;
        }
        boolean creative = player.getAbilities().instabuild;
        if (level.getBlockState(pos).is(com.criticalmass.registry.CMBlocks.WRECKAGE.get())) {
            cut(level, pos, player);
            if (!creative) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            }
            return stack;
        }
        ItemStack plate = creative ? ItemStack.EMPTY : findPlate(player);
        if (!creative && plate.isEmpty()) {
            player.displayClientMessage(Component.translatable("criticalmass.welder.needs_plate"), true);
            return stack;
        }
        if (repairAt(level, pos)) {
            level.playSound(null, pos, CMSounds.TORCH_DONE.get(), SoundSource.BLOCKS, 0.7f, 1.1f);
            if (!creative) {
                plate.shrink(1);
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            }
        }
        return stack;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(CMItems.STEEL_PLATES);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("criticalmass.welder.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("criticalmass.welder.tooltip_cost").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("criticalmass.welder.tooltip_cut").withStyle(ChatFormatting.GRAY));
    }
}
