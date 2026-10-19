package com.criticalmass.content.thermal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

public class RepairKitItem extends Item {
    private static final Map<UUID, BlockPos> TARGETS = new ConcurrentHashMap<>();

    public RepairKitItem(Properties properties) {
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
            return c.repair(pos, CMThermal.REPAIR_KIT_AMOUNT);
        }
        BlockState state = level.getBlockState(pos);
        level.setBlock(pos, state.setValue(VesselCasingBlock.CRACK, Math.max(0, state.getValue(VesselCasingBlock.CRACK) - 2)), Block.UPDATE_ALL);
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null || !needsRepair(level, pos)) {
            return InteractionResult.PASS;
        }
        if (!allowed(level, pos)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("criticalmass.repair.too_hot"), true);
            }
            return InteractionResult.FAIL;
        }
        TARGETS.put(player.getUUID(), pos.immutable());
        player.startUsingItem(context.getHand());
        return InteractionResult.CONSUME;
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
        if (repairAt(level, pos)) {
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 1.2f);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return stack;
    }
}
