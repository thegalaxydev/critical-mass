package com.criticalmass.client;

import com.criticalmass.content.control.PanelBlockEntity;
import com.criticalmass.content.control.PanelBlockEntity.Control;
import com.criticalmass.content.control.PanelBlockEntity.Kind;
import com.criticalmass.content.control.PanelBlockEntity.Type;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.List;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

public class PanelRenderer extends SafeBlockEntityRenderer<PanelBlockEntity> {
    private static final float PX = 1 / 16f;
    private static final float TILT = -90f;
    private static final int LABEL = 0xD8D4C4;

    public PanelRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(PanelBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Direction facing = be.facing();
        BlockState state = be.getBlockState();
        Type type = be.type();
        int lit = be.getLevel() == null ? light : LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(facing));
        boolean flash = be.getLevel() != null && (be.getLevel().getGameTime() / 6) % 2 == 0;
        ms.pushPose();
        ms.translate(0.5, 0, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        ms.translate(-0.5, 0, -0.5);
        List<Control> controls = type.controls;
        for (int i = 0; i < controls.size(); i++) {
            Control c = controls.get(i);
            place(ms, c.u(), c.v());
            ms.pushPose();
            ms.scale(c.scale(), c.scale(), c.scale());
            control(be, type, c, i, state, ms, buffer, lit, flash);
            ms.popPose();
            label(c.label(), ms, buffer, (float) c.labelU(), (float) c.labelV(), c.kind() == Kind.LAMP ? 0.85f : 1.0f, lit);
            if (c.kind() == Kind.DIAL) {
                label(Component.literal(Integer.toString(be.strength())), ms, buffer, 0, (float) -(c.radius() + 1.4), 1.3f, lit);
            }
            ms.popPose();
        }
        Component title = be.label().isEmpty() ? type.title() : Component.literal(be.label());
        place(ms, 8, PanelBlockEntity.TITLE_V);
        label(title, ms, buffer, 0, 0, 1.25f, lit);
        ms.popPose();
        ms.popPose();
    }

    private static void place(PoseStack ms, double u, double v) {
        ms.pushPose();
        ms.translate(1 - u * PX, v * PX, PanelBlockEntity.FACE_Z - 0.002);
        ms.mulPose(Axis.XP.rotationDegrees(TILT));
    }

    private void part(PartialModel model, BlockState state, PoseStack ms, VertexConsumer vc, int color, int light) {
        CachedBuffers.partial(model, state).light(light).color(color).renderInto(ms, vc);
    }

    private static int shade(int rgb, float k) {
        int r = (int) (((rgb >> 16) & 255) * k);
        int g = (int) (((rgb >> 8) & 255) * k);
        int b = (int) ((rgb & 255) * k);
        return r << 16 | g << 8 | b;
    }

    private void control(PanelBlockEntity be, Type type, Control c, int index, BlockState state, PoseStack ms, MultiBufferSource buffer, int light, boolean flash) {
        VertexConsumer vc = buffer.getBuffer(RenderType.solid());
        switch (c.kind()) {
            case BUTTON, BIG_BUTTON -> {
                boolean big = c.kind() == Kind.BIG_BUTTON;
                boolean pressed = be.pressed(index);
                part(big ? CMPartialModels.PANEL_BIG_BUTTON_COLLAR : CMPartialModels.PANEL_BUTTON_COLLAR, state, ms, vc, 0xFFFFFF, light);
                ms.pushPose();
                if (pressed) {
                    ms.translate(0, -(big ? 0.8f : 0.6f) * PX, 0);
                }
                part(big ? CMPartialModels.PANEL_BIG_BUTTON : CMPartialModels.PANEL_BUTTON, state, ms, vc, pressed ? shade(c.color(), 0.7f) : c.color(), light);
                ms.popPose();
            }
            case SWITCH -> {
                part(CMPartialModels.PANEL_SWITCH_BASE, state, ms, vc, 0xFFFFFF, light);
                ms.pushPose();
                ms.translate(0, 0.4f * PX, 0);
                ms.mulPose(Axis.XP.rotationDegrees(be.on() ? 28 : -28));
                part(CMPartialModels.PANEL_SWITCH_LEVER, state, ms, vc, be.on() ? 0xF2EEDF : 0xA8A49A, light);
                ms.popPose();
            }
            case DIAL -> {
                boolean on = be.on();
                part(CMPartialModels.PANEL_DIAL_COLLAR, state, ms, vc, on ? 0x58D94A : 0x15171A, on ? LightTexture.FULL_BRIGHT : light);
                ms.pushPose();
                ms.mulPose(Axis.YP.rotationDegrees(on ? -90 - 12f * be.strength() : -45));
                part(CMPartialModels.PANEL_DIAL, state, ms, vc, c.color(), light);
                part(CMPartialModels.PANEL_DIAL_POINTER, state, ms, vc, on ? 0xB4FF9E : 0xE8E4D6, on ? LightTexture.FULL_BRIGHT : light);
                ms.popPose();
            }
            case LAMP -> {
                boolean acknowledged = c.alarm() >= 0 && be.acknowledged(c.alarm());
                boolean on = c.alarm() < 0 ? be.on() : be.alarm(c.alarm()) && (acknowledged || flash);
                part(CMPartialModels.PANEL_LAMP, state, ms, vc, on ? (acknowledged ? 0xF2B33A : c.color()) : shade(c.color(), 0.22f), on ? LightTexture.FULL_BRIGHT : light);
            }
        }
    }

    private void label(Component text, PoseStack ms, MultiBufferSource buffer, float across, float along, float height, int light) {
        Font font = Minecraft.getInstance().font;
        int width = font.width(text);
        if (width == 0) {
            return;
        }
        float scale = Math.min(height / 8f, 14f / width) * PX;
        ms.pushPose();
        ms.translate(-across * PX, 0.001, along * PX);
        ms.mulPose(Axis.YP.rotationDegrees(180));
        ms.mulPose(Axis.XP.rotationDegrees(-90));
        ms.scale(scale, -scale, scale);
        font.drawInBatch(text, -width / 2f, -4f, LABEL, false, ms.last().pose(), buffer, Font.DisplayMode.POLYGON_OFFSET, 0, light);
        ms.popPose();
    }
}
