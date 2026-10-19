package com.criticalmass.content.radiation;

import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMAttachments;
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

public class DosimeterItem extends Item {
    public DosimeterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            RadiationData d = RadiationEvents.data(player);
            if (player.isShiftKeyDown()) {
                d.baseline = d.total;
                player.setData(CMAttachments.RADIATION.get(), d);
                player.displayClientMessage(Component.translatable("criticalmass.dosimeter.reset"), true);
            } else {
                player.displayClientMessage(Component.translatable("criticalmass.dosimeter.summary", ThermalTooltips.number(d.dosimeter()), ThermalTooltips.number(d.total),
                        ThermalTooltips.number(d.body), ThermalTooltips.number(d.contamination)), false);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("criticalmass.dosimeter.hint").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("criticalmass.dosimeter.bands", ThermalTooltips.number(RadiationEvents.HUNGER_DOSE), ThermalTooltips.number(RadiationEvents.WEAKNESS_DOSE),
                ThermalTooltips.number(RadiationEvents.NAUSEA_DOSE), ThermalTooltips.number(RadiationEvents.WITHER_DOSE)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
