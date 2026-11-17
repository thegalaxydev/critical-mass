package com.criticalmass.client;

import com.criticalmass.content.radiation.WaterCannonBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

public class WaterCannonRenderer extends SafeBlockEntityRenderer<WaterCannonBlockEntity> {
    public WaterCannonRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(WaterCannonBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        ms.pushPose();
        ms.translate(0.5, 0, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(be.angle(partialTicks)));
        ms.translate(-0.5, 0, -0.5);
        CachedBuffers.partial(CMPartialModels.WATER_CANNON_HEAD, be.getBlockState()).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        ms.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(WaterCannonBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1);
    }
}
