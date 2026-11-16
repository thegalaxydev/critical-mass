package com.criticalmass.client;

import com.criticalmass.content.power.GeneratorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

public class GeneratorRenderer extends ShaftRenderer<GeneratorBlockEntity> {
    public GeneratorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(GeneratorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        WireDraw.draw(be, ms, buffer);
    }

    @Override
    public boolean shouldRenderOffScreen(GeneratorBlockEntity be) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(GeneratorBlockEntity be) {
        return WireDraw.bounds(be);
    }
}
