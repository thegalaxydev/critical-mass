package com.criticalmass.client;

import com.criticalmass.CriticalMass;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.compat.jei.ConversionRecipe;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;

public final class JeiHints {
    private JeiHints() {
    }

    @SuppressWarnings("unchecked")
    public static void register() {
        if (!ModList.get().isLoaded("jei")) {
            return;
        }
        try {
            List<Object> hints = (List<Object>) Class.forName("com.simibubi.create.compat.jei.category.MysteriousItemConversionCategory").getField("RECIPES").get(null);
            hints.add(ConversionRecipe.create(new ItemStack(CMItems.CONTAMINATED_COOLANT_BUCKET.get()), new ItemStack(CMBlocks.CHARTRINE.get())));
            hints.add(ConversionRecipe.create(new ItemStack(Items.OAK_LOG), new ItemStack(CMBlocks.CHARRED_LOG.get())));
        } catch (ReflectiveOperationException | LinkageError e) {
            CriticalMass.LOGGER.warn("Create's conversion hints were not found, so JEI will not list Chartrine or the Charred Log", e);
        }
    }
}
