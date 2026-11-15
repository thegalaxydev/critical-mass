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

    public static String count(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    public static void title(List<Component> tooltip, Component name) {
        tooltip.add(Component.literal("    ").append(Component.translatable("criticalmass.goggles.title", name)));
    }

    public static void status(List<Component> tooltip, String key, ChatFormatting color, Object... args) {
        if (!key.isEmpty()) {
            tooltip.add(line(key, color, args));
        }
    }

    public static void label(List<Component> tooltip, String key, Object... args) {
        tooltip.add(Component.literal("    ").append(Component.translatable(key, args).withStyle(ChatFormatting.GRAY)));
    }

    public static void value(List<Component> tooltip, Component value) {
        tooltip.add(Component.literal("     ").append(value));
    }

    public static MutableComponent reading(String key, ChatFormatting color, Object... args) {
        return Component.translatable(key, args).withStyle(color);
    }

    public static MutableComponent aside(String key, Object... args) {
        return Component.literal(" ").append(Component.translatable(key, args)).withStyle(ChatFormatting.DARK_GRAY);
    }

    public static void kinetic(List<Component> tooltip, boolean hadStats) {
        if (hadStats) {
            tooltip.add(Component.empty());
        }
    }

    public static void coolant(List<Component> tooltip, CoolantBuffer coolant) {
        coolant(tooltip, coolant.amount() == 0 ? "criticalmass.goggles.label.coolant" : "criticalmass.goggles.title", coolant);
    }

    public static void coolant(List<Component> tooltip, String label, CoolantBuffer coolant) {
        if (coolant.amount() == 0) {
            label(tooltip, label);
            value(tooltip, reading("criticalmass.goggles.empty", ChatFormatting.DARK_GRAY));
            return;
        }
        label(tooltip, label, coolant.fluid().getHoverName());
        boolean steam = coolant.steamMedium() && coolant.quality() > 0;
        MutableComponent state = steam
                ? reading("criticalmass.unit.bar", coolant.pressure() >= CMThermal.RELIEF_PRESSURE ? ChatFormatting.RED : ChatFormatting.AQUA, number(coolant.pressure()))
                : reading("criticalmass.unit.celsius", ChatFormatting.GOLD, number(coolant.temperature()));
        value(tooltip, reading("criticalmass.unit.millibuckets", ChatFormatting.GOLD, count(coolant.amount())).append(aside("criticalmass.goggles.aside.at")).append(Component.literal(" ")).append(state));
    }
}
