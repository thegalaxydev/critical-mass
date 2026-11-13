package com.criticalmass.mixin;

import com.lycoris.loconautics.content.variantpicker.BlockVariantRegistry;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BlockVariantRegistry.class, remap = false)
public abstract class BlockVariantRegistryMixin {
    @Redirect(method = "registerLoaded", at = @At(value = "INVOKE", target = "Ljava/util/List;subList(II)Ljava/util/List;"))
    private static List<BlockVariantRegistry.VariantType> criticalmass$wholeFamily(List<BlockVariantRegistry.VariantType> types, int from, int to) {
        return from == 0 ? types : List.of();
    }
}
