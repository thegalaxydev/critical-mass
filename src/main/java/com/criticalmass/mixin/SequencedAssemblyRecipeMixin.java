package com.criticalmass.mixin;

import com.criticalmass.content.fuel.WasteCanisterSequence;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SequencedAssemblyRecipe.class)
public abstract class SequencedAssemblyRecipeMixin {
    @Inject(method = "advance", at = @At("RETURN"))
    private void criticalmass$keepFuelRecord(ResourceLocation id, ItemStack input, RandomSource random, CallbackInfoReturnable<ItemStack> cir) {
        WasteCanisterSequence.carryRecord(input, cir.getReturnValue());
    }
}
