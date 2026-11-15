package com.criticalmass.client;

import com.criticalmass.content.kinetics.ControlRodDriveBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class ControlRodDriveRenderer extends SafeBlockEntityRenderer<ControlRodDriveBlockEntity> {
    private static final float COG_LIFT = 7 / 16f;
    private static final float ROD_LIFT = 2 / 16f;
    private static final float TURNS_PER_BLOCK = 1f;

    public ControlRodDriveRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(ControlRodDriveBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        VertexConsumer solid = buffer.getBuffer(RenderType.solid());
        Direction.Axis shaft = KineticBlockEntityRenderer.getRotationAxisOf(be);
        for (Direction.AxisDirection end : Direction.AxisDirection.values()) {
            SuperByteBuffer stub = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, Direction.fromAxisAndDirection(shaft, end));
            KineticBlockEntityRenderer.standardKineticRotationTransform(stub, be, light).renderInto(ms, solid);
        }
        be.shownInsertion += (float) ((be.insertion() - be.shownInsertion) * 0.2);
        float length = (1 - be.shownInsertion) * Math.max(1, be.travel());
        CachedBuffers.partial(AllPartialModels.SHAFTLESS_COGWHEEL, state)
                .translate(0, COG_LIFT, 0)
                .center()
                .rotateYDegrees(length * TURNS_PER_BLOCK * 360f)
                .uncenter()
                .light(light)
                .renderInto(ms, solid);
        float top = 1 + ROD_LIFT + length;
        int segments = (int) Math.ceil(length);
        for (int i = 0; i < segments; i++) {
            CachedBuffers.partial(CMPartialModels.CONTROL_ROD_SHAFT, state).translate(0, top - 1 - i, 0).light(light).renderInto(ms, solid);
        }
        CachedBuffers.partial(CMPartialModels.CONTROL_ROD_CAP, state).translate(0, top, 0).light(light).renderInto(ms, solid);
    }

    @Override
    public AABB getRenderBoundingBox(ControlRodDriveBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(0.25, 0, 0.25).expandTowards(0, Math.max(1, be.travel()) + 1.5, 0);
    }
}
