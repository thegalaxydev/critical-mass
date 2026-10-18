package com.criticalmass.content.thermal;

import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ThermalTooltips {
    private ThermalTooltips() {
    }

    public static String number(double value) {
        return Math.abs(value) >= 100 ? String.format(Locale.ROOT, "%,.0f", value) : String.format(Locale.ROOT, "%.1f", value);
    }

    public static MutableComponent line(String key, ChatFormatting color, Object... args) {
        return Component.literal("     ").append(Component.translatable(key, args).withStyle(color));
    }

    public static void coolant(List<Component> tooltip, CoolantBuffer coolant) {
        if (coolant.amount() == 0) {
            tooltip.add(line("criticalmass.goggles.coolant_empty", ChatFormatting.GRAY, number(coolant.capacity())));
            return;
        }
        tooltip.add(line("criticalmass.goggles.coolant", ChatFormatting.GRAY, coolant.fluid().getHoverName(), number(coolant.amount()), number(coolant.capacity())));
        tooltip.add(line("criticalmass.goggles.temperature", ChatFormatting.GOLD, number(coolant.temperature())));
        tooltip.add(line("criticalmass.goggles.heat", ChatFormatting.GRAY, number(coolant.heat())));
    }
}
