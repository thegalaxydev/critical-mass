package com.criticalmass.client;

import com.criticalmass.content.kinetics.CirculationPumpBlock;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class PumpRenderer extends SafeBlockEntityRenderer<CirculationPumpBlockEntity> {
    public PumpRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(CirculationPumpBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(CirculationPumpBlock.FACING);
        Direction.Axis axis = facing.getAxis();
        SuperByteBuffer wheel = CachedBuffers.partialFacingVertical(CMPartialModels.CIRCULATION_PUMP_ROTOR, state, facing);
        float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), axis);
        KineticBlockEntityRenderer.kineticRotationTransform(wheel, be, axis, angle, light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }
}
