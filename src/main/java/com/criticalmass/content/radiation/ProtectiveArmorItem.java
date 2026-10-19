package com.criticalmass.content.radiation;

import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMConfig;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class ProtectiveArmorItem extends ArmorItem {
    public ProtectiveArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    public static boolean isProtective(ItemStack stack) {
        return stack.getItem() instanceof ProtectiveArmorItem;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        double full = CMConfig.get(CMConfig.GEAR_CONTAMINATION_PROTECTION);
        tooltip.add(Component.translatable("criticalmass.gear.protection", Math.round(full * 25), Math.round(full * 100)).withStyle(ChatFormatting.GRAY));
        double c = stack.getOrDefault(CMComponents.CONTAMINATION.get(), 0.0);
        if (c > 0) {
            tooltip.add(Component.translatable("criticalmass.gear.contamination", ThermalTooltips.number(c)).withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("criticalmass.gear.not_shielding").withStyle(ChatFormatting.DARK_RED));
    }
}
