package com.criticalmass.client;

import com.criticalmass.content.control.AnnunciatorBlockEntity;
import com.criticalmass.content.control.ControlButtonBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;

public final class PanelRenderers {
    private static final float PX = 1 / 16f;
    private static final float FRONT = 13 / 16f - 0.002f;
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final int BRIGHT = LightTexture.FULL_BRIGHT;

    private PanelRenderers() {
    }

    private static final class Face {
        final PoseStack ms;
        final MultiBufferSource buffer;
        final Matrix4f pose;

        Face(BlockEntity be, PoseStack ms, MultiBufferSource buffer) {
            this.ms = ms;
            this.buffer = buffer;
            Direction facing = be.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
            ms.pushPose();
            ms.translate(0.5, 0.5, 0.5);
            ms.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
            ms.translate(-0.5, -0.5, -0.5);
            pose = ms.last().pose();
        }

        VertexConsumer quads() {
            return buffer.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        }

        void vertex(VertexConsumer vc, float sx, float sy, float depth, int color, int light) {
            vc.addVertex(pose, (16 - sx) * PX, sy * PX, FRONT - depth).setColor(color).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(ms.last(), 0, 0, -1);
        }

        void rect(float x0, float y0, float x1, float y1, float depth, int color, int light) {
            VertexConsumer vc = quads();
            vertex(vc, x0, y0, depth, color, light);
            vertex(vc, x1, y0, depth, color, light);
            vertex(vc, x1, y1, depth, color, light);
            vertex(vc, x0, y1, depth, color, light);
        }

        void needle(float cx, float cy, float fraction, float length, float half, int color, int light) {
            double angle = Math.toRadians(-135 + 270 * Math.max(0, Math.min(1, fraction)));
            float dx = (float) Math.sin(angle);
            float dy = (float) Math.cos(angle);
            float px = dy * half;
            float py = -dx * half;
            VertexConsumer vc = quads();
            vertex(vc, cx - px, cy - py, 0, color, light);
            vertex(vc, cx + dx * length - px * 0.4f, cy + dy * length - py * 0.4f, 0, color, light);
            vertex(vc, cx + dx * length + px * 0.4f, cy + dy * length + py * 0.4f, 0, color, light);
            vertex(vc, cx + px, cy + py, 0, color, light);
        }

        void text(String s, float cx, float cy, float height, float maxWidth, int color, int light) {
            if (s.isEmpty()) {
                return;
            }
            Font font = Minecraft.getInstance().font;
            int width = font.width(s);
            float scale = Math.min(height / 8f, maxWidth / Math.max(1, width)) * PX;
            ms.pushPose();
            ms.translate((16 - cx) * PX, cy * PX, FRONT - 0.001f);
            ms.mulPose(Axis.YP.rotationDegrees(180));
            ms.scale(scale, -scale, scale);
            font.drawInBatch(s, -width / 2f, -4f, color, false, ms.last().pose(), buffer, Font.DisplayMode.POLYGON_OFFSET, 0, light);
            ms.popPose();
        }

        void end() {
            ms.popPose();
        }
    }

    private static int shade(int argb, float k) {
        int r = (int) (((argb >> 16) & 255) * k);
        int g = (int) (((argb >> 8) & 255) * k);
        int b = (int) ((argb & 255) * k);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static String number(double v) {
        double a = Math.abs(v);
        return a >= 100 ? String.valueOf(Math.round(v)) : a >= 10 ? String.format(java.util.Locale.ROOT, "%.1f", v) : String.format(java.util.Locale.ROOT, "%.2f", v);
    }

    public static class Button extends SafeBlockEntityRenderer<ControlButtonBlockEntity> {
        public Button(BlockEntityRendererProvider.Context context) {
        }

        @Override
        protected void renderSafe(ControlButtonBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
            Face f = new Face(be, ms, buffer);
            ControlButtonBlockEntity.Function fn = be.function();
            boolean down = be.pressed();
            float depth = down ? 0.01f : 0.045f;
            int cap = down ? shade(fn.color, 0.65f) : fn.color;
            f.rect(6, 5.4f, 10, 9.4f, depth, cap, light);
            f.rect(6, 5.0f, 10, 5.4f, depth * 0.5f, shade(fn.color, 0.45f), light);
            String name = be.label().isEmpty() ? I18n.get(fn.langKey() + ".short") : be.label();
            f.text(name, 8, 10.7f, 1.3f, 7f, 0xE8E4D6, light);
            f.end();
        }
    }

    public static class Annunciator extends SafeBlockEntityRenderer<AnnunciatorBlockEntity> {
        public Annunciator(BlockEntityRendererProvider.Context context) {
        }

        @Override
        protected void renderSafe(AnnunciatorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
            Face f = new Face(be, ms, buffer);
            boolean flash = be.getLevel() != null && (be.getLevel().getGameTime() / 6) % 2 == 0;
            AnnunciatorBlockEntity.Alarm[] alarms = AnnunciatorBlockEntity.Alarm.values();
            for (int i = 0; i < alarms.length; i++) {
                int col = i % 3;
                int row = i / 3;
                float x0 = 2 + col * 4;
                float y0 = row == 0 ? 8 : 2;
                boolean active = be.active(alarms[i]);
                boolean acked = be.acknowledged(alarms[i]);
                boolean lit = active && (acked || flash);
                if (lit) {
                    f.rect(x0 + 0.2f, y0 + 0.2f, x0 + 3.8f, y0 + 4.8f, 0, acked ? 0xFFF2B33A : 0xFFFF4030, BRIGHT);
                }
                f.text(I18n.get(alarms[i].langKey() + ".short"), x0 + 2, y0 + 2.5f, 1.4f, 3.4f, lit ? 0x20140C : 0x7C8087, lit ? BRIGHT : light);
            }
            f.end();
        }
    }
}
