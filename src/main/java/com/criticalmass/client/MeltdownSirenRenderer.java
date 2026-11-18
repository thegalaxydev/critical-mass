package com.criticalmass.client;

import com.criticalmass.content.control.MeltdownSirenBlock;
import com.criticalmass.content.control.MeltdownSirenBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class MeltdownSirenRenderer extends SafeBlockEntityRenderer<MeltdownSirenBlockEntity> {
    private static final double AXIS_X = 8 / 16.0;
    private static final double AXIS_Y = 10 / 16.0;

    public MeltdownSirenRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(MeltdownSirenBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(-be.getBlockState().getValue(MeltdownSirenBlock.FACING).toYRot()));
        ms.translate(-0.5, -0.5, -0.5);
        ms.translate(AXIS_X, AXIS_Y, 0);
        ms.mulPose(Axis.ZP.rotationDegrees(be.angle(partialTicks)));
        ms.translate(-AXIS_X, -AXIS_Y, 0);
        CachedBuffers.partial(CMPartialModels.MELTDOWN_SIREN_SPINDLE, be.getBlockState()).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        ms.popPose();
    }
}
