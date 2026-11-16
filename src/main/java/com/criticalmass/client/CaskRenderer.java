package com.criticalmass.client;

import com.criticalmass.content.fuel.ShieldedCaskBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class CaskRenderer extends SafeBlockEntityRenderer<ShieldedCaskBlockEntity> {
    private static final float LIFT = 1.75f / 16;

    public CaskRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(ShieldedCaskBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        ms.pushPose();
        ms.translate(0, be.lid.getValue(partialTicks) * LIFT, 0);
        CachedBuffers.partial(CMPartialModels.CASK_LID, be.getBlockState()).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
        ms.popPose();
    }
}
