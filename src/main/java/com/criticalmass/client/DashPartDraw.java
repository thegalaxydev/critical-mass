package com.criticalmass.client;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.control.DashGlassLid;
import com.criticalmass.content.control.DashPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SpriteShifter;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import com.simibubi.create.foundation.render.RenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public final class DashPartDraw {
    private static final SpriteShiftEntry GAUGE = sheet("gauge_atlas");
    private static final SpriteShiftEntry BODY = sheet("body_atlas");
    private static final SpriteShiftEntry SLIM_BODY = sheet("slim_body_atlas");
    private static final SpriteShiftEntry BIG_BODY = sheet("big_body_atlas");
    private static final SpriteShiftEntry BUTTON = sheet("button_atlas");
    private static final SpriteShiftEntry BIG_BUTTON = sheet("big_button_atlas");
    private static final SpriteShiftEntry LEVER = sheet("lever_atlas");
    private static final SpriteShiftEntry SWITCH = sheet("switch_atlas");
    private static final SpriteShiftEntry ALARM = sheet("alarm_atlas");
    private static final SpriteShiftEntry ALARM_LIT = SpriteShifter.get(CriticalMass.asResource("block/dashboard_part/alarm_atlas"), CriticalMass.asResource("block/dashboard_part/alarm_atlas_lit"));
    private static final float SHEET = 64f;
    private static final float BULB_LIT = 0.125f;
    private static final float QUIVER = 1.0f;
    private static final int PALETTE_ROW = 5;
    private static final int BODY_STRIDE = 14;
    private static final int ALLOY_ROW = 24;
    private static final float BUTTON_TRAVEL = 0.046875f;
    private static final float SWITCH_ROCK = 22.5f;
    private static final float LEVER_SWING = 55f;
    private static final int FACES_PER_ROW = 6;
    private static final int FACE_STRIDE = 8;
    private static final int AUTHORED_NEEDLE = 4;

    private enum Needle {
        LINEAR("needle_linear", 0f, 0f, 0f, 3.5f, 1, 0, 2),
        DIAL("needle_default", 4f, 2f, -160f, 0f, 4, 2, 4),
        CLASSIC("needle_classic", 2.13137f, 2.13137f, -80f, 0f, 4, 2, 5),
        VERTICAL("needle_vertical", 6f, 4f, -150f, 0f, 4, 2, 4);

        final String model;
        final float pivotX;
        final float pivotZ;
        final float sweep;
        final float slide;
        final int perBand;
        final int strideU;
        final int strideV;

        Needle(String model, float pivotX, float pivotZ, float sweep, float slide, int perBand, int strideU, int strideV) {
            this.model = model;
            this.pivotX = pivotX / 16f;
            this.pivotZ = pivotZ / 16f;
            this.sweep = sweep;
            this.slide = slide;
            this.perBand = perBand;
            this.strideU = strideU;
            this.strideV = strideV;
        }

        static Needle of(int face) {
            return switch (Math.floorMod(face, FACES_PER_ROW)) {
                case 3 -> DIAL;
                case 4 -> CLASSIC;
                case 5 -> VERTICAL;
                default -> LINEAR;
            };
        }
    }

    private DashPartDraw() {
    }

    public static void init() {
    }

    private static SpriteShiftEntry sheet(String name) {
        return SpriteShifter.get(CriticalMass.asResource("block/dashboard_part/" + name), CriticalMass.asResource("block/dashboard_part/" + name));
    }

    private static SuperByteBuffer piece(String name, BlockState state, int light) {
        return CachedBuffers.partial(CMPartialModels.DASH.get(name), state).light(light);
    }

    private static SuperByteBuffer shifted(SuperByteBuffer buffer, SpriteShiftEntry sheet, float pixelsU, float pixelsV) {
        TextureAtlasSprite target = sheet.getTarget();
        return buffer.shiftUVScrolling(sheet, (target.getU1() - target.getU0()) / SHEET * pixelsU, (target.getV1() - target.getV0()) / SHEET * pixelsV);
    }

    private static void shell(String name, SpriteShiftEntry sheet, int body, BlockState state, PoseStack ms, VertexConsumer vc, int light) {
        boolean alloy = body == DashPart.ALLOY_BODY;
        shifted(piece(name, state, light), sheet, alloy ? 0 : BODY_STRIDE * body, alloy ? ALLOY_ROW : 0).renderInto(ms, vc);
    }

    private static void tinted(String name, SpriteShiftEntry sheet, int strideU, int strideV, int color, BlockState state, PoseStack ms, VertexConsumer vc, int light) {
        shifted(piece(name, state, light), sheet, strideU * (color % PALETTE_ROW), strideV * (color / PALETTE_ROW)).renderInto(ms, vc);
    }

    public static void draw(DashPart part, float animation, boolean lit, PoseStack ms, MultiBufferSource buffers, int light, BlockState state) {
        draw(part, animation, lit, 0, 0, 0, ms, buffers, light, state);
    }

    private static void bulb(String name, float output, PoseStack ms, MultiBufferSource buffers, int light, BlockState state) {
        float glow = output <= 0.01f ? 0 : 0.2f + 0.55f * output;
        boolean on = glow > BULB_LIT;
        piece(name, state, on ? LightTexture.FULL_BRIGHT : light).renderInto(ms, buffers.getBuffer(RenderType.translucent()));
        if (on) {
            int level = Mth.clamp((int) (200 * Mth.clamp(1 - 2 * (glow - 0.75f) * (glow - 0.75f), -1, 1)), 0, 255);
            piece(name, state, LightTexture.FULL_BRIGHT).color(level, level, level, 255).renderInto(ms, buffers.getBuffer(RenderTypes.additive()));
        }
    }

    public static float quiver(float time, float phase, float reading) {
        if (reading <= 0.01f) {
            return 0;
        }
        float sway = (Mth.sin(time + phase) * 0.6f + Mth.sin(time * 2.7f + phase * 1.3f) * 0.4f) * QUIVER * Math.min(1, reading);
        float cycle = Mth.frac(time * 0.45f + phase);
        if (reading < 0.97f || cycle > 0.22f) {
            return sway;
        }
        float kick = 1 - cycle / 0.22f;
        return sway - kick * kick * QUIVER * 2.5f;
    }

    public static void draw(DashPart part, float animation, boolean lit, float glow, float lid, float quiver, PoseStack ms, MultiBufferSource buffers, int light, BlockState state) {
        body(part, animation, lit, quiver, ms, buffers, light, state);
        if (part.light() && part.type != DashPart.Type.ALARM) {
            bulb(part.type == DashPart.Type.LEVER ? "lever_bulb" : part.type == DashPart.Type.SWITCH ? "switch_bulb" : "bulb", glow, ms, buffers, light, state);
        }
        if (part.glass()) {
            ms.pushPose();
            ms.translate(DashGlassLid.PIVOT_X, DashGlassLid.PIVOT_Y, DashGlassLid.PIVOT_Z);
            ms.mulPose(Axis.XP.rotationDegrees(lid));
            ms.translate(-DashGlassLid.PIVOT_X, -DashGlassLid.PIVOT_Y, -DashGlassLid.PIVOT_Z);
            piece(part.type == DashPart.Type.SWITCH ? "glass_slim" : "glass_body", state, light).renderInto(ms, buffers.getBuffer(RenderType.translucent()));
            ms.popPose();
        }
    }

    private static void body(DashPart part, float animation, boolean lit, float quiver, PoseStack ms, MultiBufferSource buffers, int light, BlockState state) {
        VertexConsumer vc = buffers.getBuffer(RenderType.cutout());
        switch (part.type) {
            case BUTTON -> {
                boolean big = part.big();
                if (part.plate()) {
                    piece("plate", state, light).renderInto(ms, vc);
                }
                shell(big ? "big_body" : "body", big ? BIG_BODY : BODY, part.body, state, ms, vc, light);
                ms.pushPose();
                ms.translate(0, -animation * BUTTON_TRAVEL, 0);
                tinted(big ? "button_big_cap" : "button_cap", big ? BIG_BUTTON : BUTTON, big ? 12 : 5, 6, part.color, state, ms, vc, light);
                ms.popPose();
            }
            case SWITCH -> {
                if (part.plate()) {
                    piece("slim_plate", state, light).renderInto(ms, vc);
                }
                shell("slim_body", SLIM_BODY, part.body, state, ms, vc, light);
                ms.pushPose();
                ms.translate(0.25f, 0.125f, 0.250205f);
                ms.mulPose(Axis.XP.rotationDegrees(SWITCH_ROCK * animation));
                ms.translate(-0.25f, -0.125f, -0.250205f);
                tinted("switch_paddle", SWITCH, 3, 5, part.color, state, ms, vc, light);
                ms.popPose();
            }
            case LEVER -> {
                boolean ball = part.style == 1;
                if (part.plate()) {
                    piece("slim_plate", state, light).renderInto(ms, vc);
                }
                shell("slim_body", SLIM_BODY, part.body, state, ms, vc, light);
                if (ball) {
                    piece("lever_ball_base", state, light).renderInto(ms, vc);
                }
                ms.pushPose();
                ms.translate(0.25f, 0.09375f, 0.25f);
                ms.mulPose(Axis.XP.rotationDegrees(Mth.lerp(animation, -LEVER_SWING, LEVER_SWING)));
                ms.translate(-0.25f, -0.09375f, -0.25f);
                piece(ball ? "lever_ball_handle" : "lever_handle", state, light).renderInto(ms, vc);
                tinted(ball ? "lever_ball_knob" : "lever_knob", LEVER, 5, ball ? 3 : 5, part.color, state, ms, vc, light);
                ms.popPose();
            }
            case GAUGE -> {
                Needle needle = Needle.of(part.face);
                if (part.plate()) {
                    piece("plate", state, light).renderInto(ms, vc);
                }
                piece("back", state, light).renderInto(ms, vc);
                shifted(piece("face", state, light), GAUGE, FACE_STRIDE * (part.face % FACES_PER_ROW), FACE_STRIDE * (part.face / FACES_PER_ROW)).renderInto(ms, vc);
                shell("body", BODY, part.body, state, ms, vc, light);
                ms.pushPose();
                float travel = 0.15f + 0.7f * animation;
                if (needle.slide != 0) {
                    ms.translate(0, 0, travel * needle.slide / 16f);
                } else {
                    ms.translate(needle.pivotX, 0, needle.pivotZ);
                    ms.mulPose(Axis.YP.rotationDegrees((travel - 0.5f) * needle.sweep + quiver));
                    ms.translate(-needle.pivotX, 0, -needle.pivotZ);
                }
                shifted(piece(needle.model, state, light), GAUGE, (part.color % needle.perBand - AUTHORED_NEEDLE % needle.perBand) * needle.strideU,
                        (part.color / needle.perBand - AUTHORED_NEEDLE / needle.perBand) * needle.strideV).renderInto(ms, vc);
                ms.popPose();
            }
            case ALARM -> {
                shell("big_body", BIG_BODY, part.body, state, ms, vc, light);
                tinted("alarm_lamp", lit ? ALARM_LIT : ALARM, 9, 8, part.color, state, ms, vc, lit ? LightTexture.FULL_BRIGHT : light);
                piece("alarm_cage", state, light).renderInto(ms, vc);
            }
        }
    }
}
