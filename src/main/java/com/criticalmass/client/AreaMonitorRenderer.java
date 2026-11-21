package com.criticalmass.client;

import com.criticalmass.content.radiation.AreaMonitorBlock;
import com.criticalmass.content.radiation.AreaMonitorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class AreaMonitorRenderer extends SafeBlockEntityRenderer<AreaMonitorBlockEntity> {
    public static final double PIVOT_X = 8 / 16.0;
    public static final double PIVOT_Y = 9.25 / 16.0;

    public AreaMonitorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(AreaMonitorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(180 - be.getBlockState().getValue(AreaMonitorBlock.FACING).toYRot()));
        ms.translate(-0.5, -0.5, -0.5);
        ms.translate(PIVOT_X, PIVOT_Y, 0);
        ms.mulPose(Axis.ZP.rotationDegrees(be.needle(partialTicks)));
        ms.translate(-PIVOT_X, -PIVOT_Y, 0);
        CachedBuffers.partial(CMPartialModels.AREA_MONITOR_NEEDLE, be.getBlockState()).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        ms.popPose();
    }
}
