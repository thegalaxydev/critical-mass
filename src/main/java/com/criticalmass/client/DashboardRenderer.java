package com.criticalmass.client;

import com.criticalmass.content.control.DashPart;
import com.criticalmass.content.control.DashboardBlock;
import com.criticalmass.content.control.DashboardBlockEntity;
import com.criticalmass.content.control.PlantChannel;
import com.lycoris.loconautics.client.LoconauticsPartialModels;
import com.lycoris.loconautics.client.LoconauticsSpriteShifts;
import com.lycoris.loconautics.content.controls.panel.PanelBodyRenderer;
import com.lycoris.loconautics.content.controls.panel.style.PanelStyle;
import com.lycoris.loconautics.content.controls.panel.style.PanelStyles;
import com.lycoris.loconautics.content.controls.panelbutton.ButtonPartRenderer;
import com.lycoris.loconautics.content.controls.panelgauge.GaugeNeedle;
import com.lycoris.loconautics.content.controls.panelgauge.GaugePartRenderer;
import com.lycoris.loconautics.content.controls.panellever.LeverPartRenderer;
import com.lycoris.loconautics.content.controls.panelswitch.SwitchPartRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DashboardRenderer extends SafeBlockEntityRenderer<DashboardBlockEntity> {
    private static final float PX = 1 / 16f;
    private static final float TILT = -22.5f;
    private static final float BOARD_Y = 14 * PX;
    private static final float SHEET = 64f;
    private static final int STEEL_BODY = 3;
    private static final int NEEDLE_COLOR = 4;
    private static final int PLAIN_FACE = 9;
    private static final int RED = 0;
    private static final int DARK_RED = 1;
    private static final int ORANGE = 2;
    private static final int DARK_ORANGE = 3;
    private static final int YELLOW = 4;
    private static final int LIGHT_GREEN = 6;
    private static final int GREEN = 7;
    private static final int BLACK = 30;
    private static final int LIGHT_GRAY = 33;

    public DashboardRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(DashboardBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        Level level = be.getLevel();
        long time = level == null ? 0 : level.getGameTime();
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
                    DashPart.Press press = part.press(level);
                    boolean big = press == DashPart.Press.SCRAM;
                    ButtonPartRenderer.render(a, ms, buffer, light, state, STEEL_BODY, capColor(press), big ? bigStyle() : 0, big ? 0 : PanelStyles.BUTTON.defaultOptions(), 0);
                }
                case SWITCH -> SwitchPartRenderer.render(a, ms, buffer, light, state, STEEL_BODY, BLACK, 0, PanelStyles.SWITCH.defaultOptions(), 0);
                case LEVER -> LeverPartRenderer.render(a, ms, buffer, light, state, STEEL_BODY, RED, 0, PanelStyles.LEVER.defaultOptions(), 0);
                case GAUGE -> gauge(a, face(part.channel(level)), ms, buffer, light, state);
                case ALARM -> alarm(part.alarming() && (!part.sounding() || time / 6 % 2 == 0), ms, buffer, light, state);
            }
            ms.popPose();
        }
        ms.popPose();
    }

    private static int bigStyle() {
        int index = 0;
        for (PanelStyle style : PanelStyles.BUTTON.all()) {
            if (style.kit() != null) {
                return index;
            }
            index++;
        }
        return 0;
    }

    private static int capColor(DashPart.Press press) {
        return switch (press) {
            case SCRAM -> RED;
            case REACTOR_RESET -> GREEN;
            case ROD_IN -> YELLOW;
            case ROD_OUT -> ORANGE;
            case TURBINE_TRIP -> DARK_RED;
            case TURBINE_RESET -> LIGHT_GREEN;
            case PULSE -> LIGHT_GRAY;
        };
    }

    private static int face(@Nullable PlantChannel channel) {
        if (channel == null) {
            return PLAIN_FACE;
        }
        return switch (channel) {
            case TEMPERATURE, COOLANT_TEMPERATURE -> 27;
            case PRESSURE -> 21;
            case LEVEL -> 2;
            case FLOW, OUTPUT -> 15;
            case POWER -> 22;
            case RODS_A, RODS_B, RODS_C, RODS_D -> 32;
            case SPEED -> PLAIN_FACE;
            case NEUTRON -> 23;
            case PERIOD -> 33;
            case REACTIVITY -> 28;
            case XENON -> 17;
            case BORON -> 14;
        };
    }

    private static void gauge(float shown, int face, PoseStack ms, MultiBufferSource buffer, int light, BlockState state) {
        TextureAtlasSprite sheet = LoconauticsSpriteShifts.LOCOMOTIVE_GAUGE.getTarget();
        float pixelU = (sheet.getU1() - sheet.getU0()) / SHEET;
        float pixelV = (sheet.getV1() - sheet.getV0()) / SHEET;
        GaugeNeedle needle = GaugeNeedle.forBackground(face);
        VertexConsumer cutout = buffer.getBuffer(RenderType.cutout());
        PanelBodyRenderer.renderPlate(ms, cutout, light, state);
        CachedBuffers.partial(LoconauticsPartialModels.GAUGE_BACK, state).light(light).renderInto(ms, cutout);
        CachedBuffers.partial(LoconauticsPartialModels.GAUGE_FACE, state).light(light)
                .shiftUVScrolling(LoconauticsSpriteShifts.LOCOMOTIVE_GAUGE, pixelU * 8 * (face % 6), pixelV * 8 * (face / 6))
                .renderInto(ms, cutout);
        PanelBodyRenderer.renderBody(ms, cutout, light, state, GaugePartRenderer.bodyForFront(STEEL_BODY));
        ms.pushPose();
        GaugePartRenderer.applyNeedle(ms, needle, shown, 0);
        CachedBuffers.partial(GaugePartRenderer.partialFor(needle), state).light(light)
                .shiftUVScrolling(LoconauticsSpriteShifts.LOCOMOTIVE_GAUGE, pixelU * needle.colorOffsetU(NEEDLE_COLOR), pixelV * needle.colorOffsetV(NEEDLE_COLOR))
                .renderInto(ms, cutout);
        ms.popPose();
    }

    private static void alarm(boolean lit, PoseStack ms, MultiBufferSource buffer, int light, BlockState state) {
        ButtonPartRenderer.CapStyle cap = ButtonPartRenderer.capStyle(bigStyle());
        TextureAtlasSprite sheet = cap.shift().getTarget();
        float pixelU = (sheet.getU1() - sheet.getU0()) / SHEET;
        float pixelV = (sheet.getV1() - sheet.getV0()) / SHEET;
        int color = lit ? YELLOW : DARK_ORANGE;
        VertexConsumer cutout = buffer.getBuffer(RenderType.cutout());
        PanelBodyRenderer.renderBigBody(ms, cutout, light, state, STEEL_BODY);
        CachedBuffers.partial(cap.cap(), state).light(lit ? LightTexture.FULL_BRIGHT : light)
                .shiftUVScrolling(cap.shift(), pixelU * cap.strideX() * (color % 5), pixelV * cap.strideY() * (color / 5))
                .renderInto(ms, cutout);
    }
}
