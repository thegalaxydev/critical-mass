package com.criticalmass.mixin;

import com.criticalmass.client.TabBanners;
import java.util.Collection;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeScreenMixin extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Shadow
    private static CreativeModeTab selectedTab;
    @Shadow
    private float scrollOffs;

    private CreativeScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void criticalmass$sections(CreativeModeTab tab, CallbackInfo ci) {
        if (TabBanners.ours(tab)) {
            TabBanners.pad(menu.items);
            menu.scrollTo(0);
        }
    }

    @Inject(method = "refreshCurrentTabContents", at = @At("TAIL"))
    private void criticalmass$resection(Collection<ItemStack> items, CallbackInfo ci) {
        if (TabBanners.ours(selectedTab)) {
            TabBanners.pad(menu.items);
            menu.scrollTo(scrollOffs);
        }
    }

    @Inject(method = "renderLabels", at = @At("TAIL"))
    private void criticalmass$banners(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (TabBanners.ours(selectedTab)) {
            TabBanners.draw(graphics, font, scrollOffs, menu.items.size(), mouseX - leftPos, mouseY - topPos);
        }
    }
}
