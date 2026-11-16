package com.criticalmass.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

public class WireNodeRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    public WireNodeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(T be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        WireDraw.draw(be, ms, buffer);
    }

    @Override
    public boolean shouldRenderOffScreen(T be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(T be) {
        return WireDraw.bounds(be);
    }
}
