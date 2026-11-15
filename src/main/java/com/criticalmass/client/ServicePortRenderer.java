package com.criticalmass.client;

import com.criticalmass.content.fuel.ServicePortBlock;
import com.criticalmass.content.fuel.ServicePortBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class ServicePortRenderer extends SafeBlockEntityRenderer<ServicePortBlockEntity> {
    private static final float TRAVEL = 3 / 16f;

    public ServicePortRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(ServicePortBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(ServicePortBlock.FACING);
        int outside = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(facing));
        float slide = be.lid.getValue(partialTicks) * TRAVEL;
        VertexConsumer solid = buffer.getBuffer(RenderType.solid());
        CachedBuffers.partialFacingVertical(CMPartialModels.SERVICE_PORT_DOOR_WEST, state, facing).translate(-slide, 0, 0).light(outside).renderInto(ms, solid);
        CachedBuffers.partialFacingVertical(CMPartialModels.SERVICE_PORT_DOOR_EAST, state, facing).translate(slide, 0, 0).light(outside).renderInto(ms, solid);
    }
}
