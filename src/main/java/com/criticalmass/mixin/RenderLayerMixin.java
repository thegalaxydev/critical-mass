package com.criticalmass.mixin;

import com.criticalmass.client.entity.Mutants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RenderLayer.class)
public abstract class RenderLayerMixin {
    @ModifyVariable(method = "renderColoredCutoutModel", at = @At("HEAD"), argsOnly = true)
    private static ResourceLocation criticalmass$irradiatedLayer(ResourceLocation texture, EntityModel<?> model, ResourceLocation same, PoseStack pose, MultiBufferSource buffers, int light,
            LivingEntity entity, int colour) {
        return Mutants.skin(entity, texture);
    }
}
