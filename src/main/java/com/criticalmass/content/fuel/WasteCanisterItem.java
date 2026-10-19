package com.criticalmass.content.fuel;

import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class WasteCanisterItem extends Item {
    public WasteCanisterItem(Properties properties) {
        super(properties.fireResistant());
    }

    public static ItemStack sealing(FuelState state) {
        ItemStack stack = new ItemStack(CMItems.SEALED_WASTE_CANISTER.get());
        stack.set(CMComponents.FUEL_STATE.get(), state);
        return stack;
    }

    @Override
    public int getEntityLifespan(ItemStack stack, Level level) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity.getAge() > 0 && !entity.level().isClientSide) {
            entity.setUnlimitedLifetime();
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FuelState s = stack.get(CMComponents.FUEL_STATE.get());
        if (s == null) {
            tooltip.add(Component.translatable("criticalmass.canister.solidified").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("criticalmass.canister.source", Component.translatable("item.criticalmass.damaged_fuel_assembly")).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("criticalmass.fuel.burned", ThermalTooltips.number(s.burned())).withStyle(ChatFormatting.DARK_GRAY));
            if (!s.damageCause().isEmpty()) {
                tooltip.add(Component.translatable("criticalmass.fuel.damage_cause", Component.translatable("criticalmass.fuel.cause." + s.damageCause())).withStyle(ChatFormatting.RED));
            }
            if (s.residual() > 0) {
                tooltip.add(Component.translatable("criticalmass.fuel.residual", ThermalTooltips.number(s.residual())).withStyle(ChatFormatting.GOLD));
            }
            if (flag.isAdvanced()) {
                tooltip.add(Component.translatable("criticalmass.fuel.lifecycle", s.id().toString()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        tooltip.add(Component.translatable("criticalmass.canister.store").withStyle(ChatFormatting.DARK_AQUA));
    }
}
