package com.criticalmass.client;

import com.criticalmass.content.control.DashPart;
import com.criticalmass.content.control.DashboardBlock;
import com.criticalmass.content.control.DashboardBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class DashboardRenderer extends SafeBlockEntityRenderer<DashboardBlockEntity> {
    private static final float PX = 1 / 16f;
    private static final float TILT = -22.5f;
    private static final float BOARD_Y = 14 * PX;
    private static final float PIVOT = 4 * PX;

    public DashboardRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(DashboardBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        VertexConsumer vc = buffer.getBuffer(RenderType.cutoutMipped());
        long time = be.getLevel() == null ? 0 : be.getLevel().getGameTime();
        ms.pushPose();
        ms.translate(0.5, 0, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(180 - state.getValue(DashboardBlock.FACING).toYRot()));
        ms.translate(-0.5, 0, -0.5);
        ms.translate(0, BOARD_Y, 1);
        ms.mulPose(Axis.XP.rotationDegrees(TILT));
        ms.translate(0, -BOARD_Y, -1);
        for (int slot = 0; slot < DashboardBlockEntity.SLOTS; slot++) {
            DashPart part = be.part(slot);
            if (part == null) {
                continue;
            }
            float a = be.animation(slot, partialTicks);
            ms.pushPose();
            ms.translate(slot * 8 * PX, BOARD_Y, (float) DashboardBlock.CELL_FRONT);
            switch (part.type) {
                case BUTTON -> {
                    draw("button_base", state, ms, vc, light);
                    ms.translate(0, -a * PX, 0);
                    draw("button_cap", state, ms, vc, light);
                }
                case SWITCH -> {
                    draw("switch_base", state, ms, vc, light);
                    swing(ms, Mth.lerp(a, -25f, 25f));
                    draw("switch_paddle", state, ms, vc, light);
                }
                case LEVER -> {
                    draw("lever_base", state, ms, vc, light);
                    swing(ms, Mth.lerp(a, -40f, 40f));
                    draw("lever_handle", state, ms, vc, light);
                }
                case GAUGE -> {
                    draw("gauge_body", state, ms, vc, light);
                    ms.translate(PIVOT, 0, PIVOT);
                    ms.mulPose(Axis.YP.rotationDegrees(-45f - 270f * a));
                    ms.translate(-PIVOT, 0, -PIVOT);
                    draw("gauge_needle", state, ms, vc, light);
                }
                case ALARM -> {
                    draw("alarm_base", state, ms, vc, light);
                    boolean lit = part.alarming() && (!part.sounding() || time / 6 % 2 == 0);
                    draw(lit ? "alarm_lamp_lit" : "alarm_lamp", state, ms, vc, lit ? LightTexture.FULL_BRIGHT : light);
                }
            }
            ms.popPose();
        }
        ms.popPose();
    }

    private static void swing(PoseStack ms, float degrees) {
        ms.translate(PIVOT, PX, PIVOT);
        ms.mulPose(Axis.XP.rotationDegrees(degrees));
        ms.translate(-PIVOT, -PX, -PIVOT);
    }

    private static void draw(String piece, BlockState state, PoseStack ms, VertexConsumer vc, int light) {
        CachedBuffers.partial(CMPartialModels.DASHBOARD.get(piece), state).light(light).renderInto(ms, vc);
    }
}
