package com.criticalmass.content.fuel;

import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.content.kinetics.deployer.DeployerRecipeSearchEvent;
import net.minecraft.world.item.ItemStack;

public final class WasteCanisterSequence {
    private static final ThreadLocal<ItemStack> DEPLOYED = ThreadLocal.withInitial(() -> ItemStack.EMPTY);

    private WasteCanisterSequence() {
    }

    public static void deploying(ItemStack held) {
        DEPLOYED.set(held == null ? ItemStack.EMPTY : held.copy());
    }

    public static void deployed() {
        DEPLOYED.remove();
    }

    public static void carryRecord(ItemStack input, ItemStack output) {
        if (output == null || output.isEmpty() || !(output.is(CMItems.INCOMPLETE_WASTE_CANISTER.get()) || output.is(CMItems.SEALED_WASTE_CANISTER.get()))) {
            return;
        }
        FuelState state = input.get(CMComponents.FUEL_STATE.get());
        if (state == null) {
            ItemStack held = DEPLOYED.get();
            if (held.is(CMItems.DAMAGED_FUEL_ASSEMBLY.get())) {
                state = held.get(CMComponents.FUEL_STATE.get());
            }
        }
        if (state != null) {
            output.set(CMComponents.FUEL_STATE.get(), state);
        }
    }

    public static boolean tooHot(ItemStack stack) {
        if (!stack.is(CMItems.DAMAGED_FUEL_ASSEMBLY.get())) {
            return false;
        }
        FuelState state = stack.get(CMComponents.FUEL_STATE.get());
        return state != null && !state.cooled();
    }

    public static void onDeployerSearch(DeployerRecipeSearchEvent event) {
        if (tooHot(event.getInventory().getItem(1))) {
            event.setCanceled(true);
        }
    }
}
