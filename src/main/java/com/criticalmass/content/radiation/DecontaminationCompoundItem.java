package com.criticalmass.content.radiation;

import com.criticalmass.registry.CMAttachments;
import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMFluids;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public class DecontaminationCompoundItem extends Item {
    public static final int RUNOFF = 250;
    public static final int RUNOFF_SEARCH = 4;
    public static final int CLEAN_RADIUS = 1;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public DecontaminationCompoundItem(Properties properties) {
        super(properties);
    }

    public static FluidStack runoff() {
        return new FluidStack(CMFluids.CONTAMINATED_COOLANT.source.get(), RUNOFF);
    }

    @Nullable
    public static IFluidHandler runoffTarget(Level level, BlockPos around) {
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(around.offset(-RUNOFF_SEARCH, -RUNOFF_SEARCH, -RUNOFF_SEARCH), around.offset(RUNOFF_SEARCH, RUNOFF_SEARCH, RUNOFF_SEARCH))) {
            candidates.add(p.immutable());
        }
        candidates.sort(Comparator.comparingDouble(p -> p.distSqr(around)));
        for (BlockPos p : candidates) {
            IFluidHandler h = level.getCapability(Capabilities.FluidHandler.BLOCK, p, null);
            if (h != null && h.fill(runoff(), IFluidHandler.FluidAction.SIMULATE) == RUNOFF) {
                return h;
            }
        }
        return null;
    }

    public static boolean cleanArea(ServerLevel level, BlockPos center) {
        ContaminationData data = ContaminationData.get(level);
        double found = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-CLEAN_RADIUS, -CLEAN_RADIUS, -CLEAN_RADIUS), center.offset(CLEAN_RADIUS, CLEAN_RADIUS, CLEAN_RADIUS))) {
            found += data.get(p);
        }
        if (found <= 0) {
            return false;
        }
        IFluidHandler target = runoffTarget(level, center);
        if (target == null) {
            return false;
        }
        target.fill(runoff(), IFluidHandler.FluidAction.EXECUTE);
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-CLEAN_RADIUS, -CLEAN_RADIUS, -CLEAN_RADIUS), center.offset(CLEAN_RADIUS, CLEAN_RADIUS, CLEAN_RADIUS))) {
            data.clear(p);
        }
        return true;
    }

    public static boolean cleanPlayer(ServerLevel level, Player player) {
        RadiationData d = RadiationEvents.data(player);
        double gear = RadiationEvents.gearContamination(player);
        if (d.contamination <= 0 && gear <= 0) {
            return false;
        }
        IFluidHandler target = runoffTarget(level, player.blockPosition());
        if (target == null) {
            return false;
        }
        target.fill(runoff(), IFluidHandler.FluidAction.EXECUTE);
        d.contamination = 0;
        player.setData(CMAttachments.RADIATION.get(), d);
        for (EquipmentSlot slot : ARMOR) {
            player.getItemBySlot(slot).remove(CMComponents.CONTAMINATION.get());
        }
        return true;
    }

    private static void consume(Player player, ItemStack stack, Level level, BlockPos pos) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        ServerLevel server = (ServerLevel) level;
        if (!(ContaminationData.get(server).near(pos.getCenter(), CLEAN_RADIUS) > 0)) {
            player.displayClientMessage(Component.translatable("criticalmass.decon.clean"), true);
            return InteractionResult.FAIL;
        }
        if (!cleanArea(server, pos) && !cleanArea(server, context.getClickedPos())) {
            player.displayClientMessage(Component.translatable("criticalmass.decon.no_runoff", RUNOFF_SEARCH), true);
            return InteractionResult.FAIL;
        }
        consume(player, context.getItemInHand(), level, pos);
        player.displayClientMessage(Component.translatable("criticalmass.decon.done", RUNOFF), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        RadiationData d = RadiationEvents.data(player);
        if (d.contamination <= 0 && RadiationEvents.gearContamination(player) <= 0) {
            player.displayClientMessage(Component.translatable("criticalmass.decon.self_clean"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!cleanPlayer((ServerLevel) level, player)) {
            player.displayClientMessage(Component.translatable("criticalmass.decon.no_runoff", RUNOFF_SEARCH), true);
            return InteractionResultHolder.fail(stack);
        }
        consume(player, stack, level, player.blockPosition());
        player.displayClientMessage(Component.translatable("criticalmass.decon.done", RUNOFF), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("criticalmass.decon.hint", RUNOFF, RUNOFF_SEARCH).withStyle(ChatFormatting.GRAY));
    }
}
