package com.criticalmass.client;

import com.criticalmass.content.power.MotorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class MotorRenderer extends KineticBlockEntityRenderer<MotorBlockEntity> {
    public MotorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(MotorBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state);
    }

    @Override
    protected void renderSafe(MotorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        WireDraw.draw(be, ms, buffer);
    }

    @Override
    public boolean shouldRenderOffScreen(MotorBlockEntity be) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(MotorBlockEntity be) {
        return WireDraw.bounds(be);
    }
}
