package com.criticalmass.inspect;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.fml.util.ObfuscationReflectionHelper;

final class InspectScreens {
    private InspectScreens() {
    }

    static String advancementTab(Minecraft mc, String id) {
        ClientAdvancements held = mc.player.connection.getAdvancements();
        AdvancementHolder root = held.get(ResourceLocation.parse(id));
        mc.setScreen(new AdvancementsScreen(held));
        if (root != null) {
            held.setSelectedTab(root, true);
        }
        return "advancement tab " + id + (root == null ? " not found" : "");
    }

    static String creativeTab(Minecraft mc, String id) {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(ResourceLocation.parse(id));
        ObfuscationReflectionHelper.setPrivateValue(CreativeModeInventoryScreen.class, null, tab, "selectedTab");
        mc.setScreen(new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), mc.options.operatorItemsTab().get()));
        return "creative tab " + id;
    }

    static void creativeScroll(Minecraft mc, int rows) {
        if (mc.screen instanceof CreativeModeInventoryScreen creative) {
            for (int i = 0; i < rows; i++) {
                creative.mouseScrolled(0, 0, 0, -1);
            }
        }
    }

    static String creativeHover(Minecraft mc, int row) {
        if (!(mc.screen instanceof CreativeModeInventoryScreen creative)) {
            return "no creative screen to point at";
        }
        double guiX = creative.getGuiLeft() + 8 + 120;
        double guiY = creative.getGuiTop() + 17 + 9 + row * 18;
        double scale = mc.getWindow().getScreenWidth() / (double) mc.getWindow().getGuiScaledWidth();
        ObfuscationReflectionHelper.setPrivateValue(MouseHandler.class, mc.mouseHandler, guiX * scale, "xpos");
        ObfuscationReflectionHelper.setPrivateValue(MouseHandler.class, mc.mouseHandler, guiY * scale, "ypos");
        return "pointer over row " + row;
    }
}
