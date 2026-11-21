package com.criticalmass.content;

import com.criticalmass.content.radiation.ContaminationData;
import com.criticalmass.content.thermal.CMThermal;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public class FluidBucketItem extends BucketItem {
    public static final int AMOUNT = 1000;

    public FluidBucketItem(Fluid content, Properties properties) {
        super(content, properties);
    }

    public double spillDirt() {
        FluidStack held = new FluidStack(content, AMOUNT);
        return AMOUNT * (CMThermal.isContaminated(held) ? CMThermal.CONTAMINATED_SPILL : CMThermal.isPrimaryMedium(held) ? CMThermal.CLEAN_SPILL : 0);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        BlockPos on = hit.getBlockPos();
        BlockPos spot = on.relative(hit.getDirection());
        boolean store = level.getCapability(Capabilities.FluidHandler.BLOCK, on, hit.getDirection()) != null || level.getCapability(Capabilities.FluidHandler.BLOCK, on, null) != null;
        if (store || !level.mayInteract(player, on) || !player.mayUseItemAt(spot, hit.getDirection(), stack)) {
            return InteractionResultHolder.pass(stack);
        }
        if (level instanceof ServerLevel server) {
            double dirt = spillDirt();
            if (dirt > 0) {
                ContaminationData.get(server).add(spot, dirt);
            }
            server.playSound(null, spot, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            server.sendParticles(ParticleTypes.SPLASH, spot.getX() + 0.5, spot.getY() + 0.1, spot.getZ() + 0.5, 24, 0.25, 0.05, 0.25, 0.1);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)), level.isClientSide);
    }

    @Override
    public boolean emptyContents(@Nullable Player player, Level level, BlockPos pos, @Nullable BlockHitResult result, @Nullable ItemStack container) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.criticalmass.fluid_bucket.hint").withStyle(ChatFormatting.GRAY));
    }
}
