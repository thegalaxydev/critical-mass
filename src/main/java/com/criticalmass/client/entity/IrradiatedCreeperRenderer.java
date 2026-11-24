package com.criticalmass.client.entity;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.radiation.IrradiatedCreeper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class IrradiatedCreeperRenderer extends MobRenderer<IrradiatedCreeper, IrradiatedCreeperModel> {
    private static final ResourceLocation SKIN = CriticalMass.asResource("textures/entity/irradiated_creeper.png");
    private static final ResourceLocation GLOW = CriticalMass.asResource("textures/entity/irradiated_creeper_glow.png");
    private static final float SIZE = 1.15F;
    private static final ResourceLocation POWER = ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");

    public IrradiatedCreeperRenderer(EntityRendererProvider.Context context) {
        super(context, new IrradiatedCreeperModel(context.bakeLayer(IrradiatedCreeperModel.LAYER)), 0.6F);
        addLayer(new Glow(this));
        addLayer(new Power(this, context.getModelSet()));
    }

    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(IrradiatedCreeperModel.LAYER, () -> IrradiatedCreeperParts.create(CubeDeformation.NONE));
        event.registerLayerDefinition(IrradiatedCreeperModel.ARMOR, () -> IrradiatedCreeperParts.create(new CubeDeformation(1.5F)));
    }

    @Override
    protected void scale(IrradiatedCreeper entity, PoseStack pose, float partialTick) {
        float swell = entity.getSwelling(partialTick);
        float shake = 1F + Mth.sin(swell * 100F) * swell * 0.01F;
        float grown = Mth.clamp(swell, 0F, 1F);
        grown *= grown;
        grown *= grown;
        float wide = (1F + grown * 0.4F) * shake * SIZE;
        pose.scale(wide, (1F + grown * 0.1F) / shake * SIZE, wide);
    }

    @Override
    protected float getWhiteOverlayProgress(IrradiatedCreeper entity, float partialTick) {
        float swell = entity.getSwelling(partialTick);
        return (int) (swell * 10F) % 2 == 0 ? 0F : Mth.clamp(swell, 0.5F, 1F);
    }

    @Override
    public ResourceLocation getTextureLocation(IrradiatedCreeper entity) {
        return SKIN;
    }

    static class Glow extends RenderLayer<IrradiatedCreeper, IrradiatedCreeperModel> {
        Glow(RenderLayerParent<IrradiatedCreeper, IrradiatedCreeperModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, IrradiatedCreeper entity, float limbSwing, float limbSwingAmount, float partialTick, float age, float headYaw,
                float headPitch) {
            if (entity.isInvisible()) {
                return;
            }
            float swell = Mth.clamp(entity.getSwelling(partialTick), 0F, 1F);
            float level = Mth.clamp(0.5F + 0.16F * Mth.sin((age + entity.getId() * 7.3F) * 0.12F) + 0.6F * swell, 0F, 1F);
            int shade = (int) (level * 255F);
            VertexConsumer out = buffers.getBuffer(RenderType.eyes(GLOW));
            getParentModel().renderToBuffer(pose, out, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(255, shade, shade, shade));
        }
    }

    static class Power extends EnergySwirlLayer<IrradiatedCreeper, IrradiatedCreeperModel> {
        private final IrradiatedCreeperModel model;

        Power(RenderLayerParent<IrradiatedCreeper, IrradiatedCreeperModel> parent, EntityModelSet models) {
            super(parent);
            model = new IrradiatedCreeperModel(models.bakeLayer(IrradiatedCreeperModel.ARMOR));
        }

        @Override
        protected float xOffset(float tickCount) {
            return tickCount * 0.01F;
        }

        @Override
        protected ResourceLocation getTextureLocation() {
            return POWER;
        }

        @Override
        protected EntityModel<IrradiatedCreeper> model() {
            return model;
        }
    }
}
