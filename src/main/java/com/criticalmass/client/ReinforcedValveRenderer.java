package com.criticalmass.client;

import com.criticalmass.content.thermal.ReinforcedValveBlock;
import com.criticalmass.content.thermal.ReinforcedValveBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class ReinforcedValveRenderer extends KineticBlockEntityRenderer<ReinforcedValveBlockEntity> {
    public ReinforcedValveRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    public static int pointerOffset(BlockState state, Direction.Axis shaftAxis) {
        Direction.Axis pipeAxis = ReinforcedValveBlock.getPipeAxis(state);
        return pipeAxis.isHorizontal() && shaftAxis == Direction.Axis.X || pipeAxis.isVertical() ? 90 : 0;
    }

    public static float pitch(Direction facing) {
        return facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90;
    }

    @Override
    protected void renderSafe(ReinforcedValveBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) {
            return;
        }
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(ReinforcedValveBlock.FACING);
        SuperByteBuffer pointer = CachedBuffers.partial(CMPartialModels.REINFORCED_VALVE_POINTER, state);
        float rotation = Mth.lerp(be.pointer.getValue(partialTicks), 0, -90);
        pointer.center()
                .rotateYDegrees(AngleHelper.horizontalAngle(facing))
                .rotateXDegrees(pitch(facing))
                .rotateYDegrees(pointerOffset(state, getRotationAxisOf(be)) + rotation)
                .uncenter()
                .light(light)
                .renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    @Override
    protected BlockState getRenderedBlockState(ReinforcedValveBlockEntity be) {
        return shaft(getRotationAxisOf(be));
    }
}
