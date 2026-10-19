package com.criticalmass.content.radiation;

import com.criticalmass.registry.CMComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class SurveyMeterItem extends Item {
    public SurveyMeterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int mode = 1 - stack.getOrDefault(CMComponents.METER_MODE.get(), 0);
            stack.set(CMComponents.METER_MODE.get(), mode);
            player.displayClientMessage(Component.translatable(mode == 0 ? "criticalmass.meter.mode_rate" : "criticalmass.meter.mode_contamination"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int mode = stack.getOrDefault(CMComponents.METER_MODE.get(), 0);
        tooltip.add(Component.translatable(mode == 0 ? "criticalmass.meter.mode_rate" : "criticalmass.meter.mode_contamination").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("criticalmass.meter.hint").withStyle(ChatFormatting.GRAY));
    }
}
