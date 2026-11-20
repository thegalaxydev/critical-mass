package com.criticalmass.registry;

import com.criticalmass.CriticalMass;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CMCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CriticalMass.MODID);

    public static final Supplier<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.criticalmass.main"))
            .icon(() -> new ItemStack(CMBlocks.REACTOR_VESSEL_CASING.get()))
            .displayItems((params, output) -> TabSection.ordered(CMItems.TAB_ORDER.stream().map(item -> item.get()).toList()).forEach(output::accept))
            .build());

    private CMCreativeTabs() {
    }
}
