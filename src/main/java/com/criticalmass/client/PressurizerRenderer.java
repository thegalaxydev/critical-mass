package com.criticalmass.client;

import com.criticalmass.content.control.DashPart;
import com.criticalmass.content.control.PlantChannel;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.PressurizerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public class PressurizerRenderer extends SafeBlockEntityRenderer<PressurizerBlockEntity> {
    private static final float PX = 1 / 16f;
    private final DashPart dial = new DashPart(DashPart.Type.GAUGE);

    public PressurizerRenderer(BlockEntityRendererProvider.Context context) {
        dial.face = DashPart.face(PlantChannel.PRESSURE);
        dial.options = 0;
    }

    @Override
    protected void renderSafe(PressurizerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Direction face = be.gaugeFace();
        if (face == null) {
            return;
        }
        float fraction = (float) Mth.clamp(be.pressure() / CMThermal.PRESSURIZER_MAX, 0, 1);
        int outside = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(face));
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(180 - face.toYRot()));
        ms.translate(-0.5, -0.5, -0.5);
        ms.translate(4 * PX, 8 * PX, 2 * PX);
        ms.mulPose(Axis.XP.rotationDegrees(-90));
        DashPartDraw.draw(dial, fraction, false, ms, buffer, outside, be.getBlockState());
        ms.popPose();
    }
}
