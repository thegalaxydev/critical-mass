package com.criticalmass.content.fuel;

import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMItems;
import net.minecraft.world.item.ItemStack;

public final class WasteCanisterSequence {
    private WasteCanisterSequence() {
    }

    public static void carryRecord(ItemStack input, ItemStack output) {
        if (output == null || output.isEmpty() || !(output.is(CMItems.INCOMPLETE_WASTE_CANISTER.get()) || output.is(CMItems.SEALED_WASTE_CANISTER.get()))) {
            return;
        }
        FuelState state = input.get(CMComponents.FUEL_STATE.get());
        if (state != null) {
            output.set(CMComponents.FUEL_STATE.get(), state);
        }
    }

    public static boolean tooHot(ItemStack input) {
        if (!input.is(CMItems.DAMAGED_FUEL_ASSEMBLY.get())) {
            return false;
        }
        FuelState state = input.get(CMComponents.FUEL_STATE.get());
        return state != null && !state.cooled();
    }
}
