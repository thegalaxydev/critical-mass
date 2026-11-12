package com.criticalmass.client;

import com.criticalmass.content.kinetics.ControlRodDriveBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class ControlRodDriveRenderer extends InputSideRotorRenderer<ControlRodDriveBlockEntity> {
    public ControlRodDriveRenderer(BlockEntityRendererProvider.Context context) {
        super(context, CMPartialModels.CONTROL_ROD_DRIVE_PINION);
    }

    @Override
    protected void renderSafe(ControlRodDriveBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        be.shownInsertion += (float) ((be.insertion() - be.shownInsertion) * 0.2);
        float length = (1 - be.shownInsertion) * Math.max(1, be.travel());
        BlockState state = be.getBlockState();
        VertexConsumer vc = buffer.getBuffer(RenderType.solid());
        float top = 1 + length;
        int segments = (int) Math.ceil(length);
        for (int i = 0; i < segments; i++) {
            CachedBuffers.partial(CMPartialModels.CONTROL_ROD_SHAFT, state).translate(0, top - 1 - i, 0).light(light).renderInto(ms, vc);
        }
        CachedBuffers.partial(CMPartialModels.CONTROL_ROD_CAP, state).translate(0, top, 0).light(light).renderInto(ms, vc);
    }

    @Override
    public AABB getRenderBoundingBox(ControlRodDriveBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0, Math.max(1, be.travel()) + 1, 0);
    }
}
