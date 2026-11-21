package com.criticalmass.content.thermal;

import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ResinCartridgeItem extends Item {
    public ResinCartridgeItem(Properties properties) {
        super(properties.fireResistant());
    }

    public static boolean isCartridge(ItemStack stack) {
        return stack.is(CMItems.RESIN_CARTRIDGE.get()) || stack.is(CMItems.USED_RESIN_CARTRIDGE.get());
    }

    public static double load(ItemStack stack) {
        return stack.is(CMItems.USED_RESIN_CARTRIDGE.get()) ? Mth.clamp(stack.getOrDefault(CMComponents.RESIN_LOAD.get(), 1.0), 0, 1) : 0;
    }

    public static ItemStack loaded(double load) {
        if (load <= 0) {
            return new ItemStack(CMItems.RESIN_CARTRIDGE.get());
        }
        ItemStack stack = new ItemStack(CMItems.USED_RESIN_CARTRIDGE.get());
        stack.set(CMComponents.RESIN_LOAD.get(), Math.min(1, load));
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
        double load = load(stack);
        tooltip.add(Component.translatable(load >= 1 ? "criticalmass.resin.spent" : "criticalmass.resin.left", Math.round((1 - load) * 100)).withStyle(load >= 1 ? ChatFormatting.RED : ChatFormatting.GOLD));
        tooltip.add(Component.translatable("criticalmass.resin.store").withStyle(ChatFormatting.DARK_AQUA));
    }
}
