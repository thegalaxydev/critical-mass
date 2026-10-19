package com.criticalmass.client;

import com.criticalmass.content.fuel.FuelAssemblyBenchBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.depot.DepotRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class BenchRenderer extends SafeBlockEntityRenderer<FuelAssemblyBenchBlockEntity> {
    public BenchRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(FuelAssemblyBenchBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        ms.pushPose();
        ms.translate(0, 1 / 16f, 0);
        DepotRenderer.renderItemsOf(be, partialTicks, ms, buffer, light, overlay, be.depot());
        ms.popPose();
    }
}
