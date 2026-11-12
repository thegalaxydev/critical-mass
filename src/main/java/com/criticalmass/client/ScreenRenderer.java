package com.criticalmass.client;

import com.criticalmass.content.control.PlantChannel;
import com.criticalmass.content.control.PlantDisplaySources;
import com.criticalmass.content.control.ScreenBlockEntity;
import com.criticalmass.content.control.ScreenBlockEntity.Trend;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public class ScreenRenderer extends SafeBlockEntityRenderer<ScreenBlockEntity> {
    private static final int GREEN = 0x41FF6A;
    private static final int DIM = 0x25A54D;
    private static final int GRID = 0x12491F;
    private static final int AMBER = 0xFFC23A;
    private static final int RED = 0xFF5548;
    private static final float FRONT = 13 / 16f - 0.004f;
    private static final float MARGIN = 1.5f / 16f;
    private static final float LINE = 1f / ScreenBlockEntity.LINES_PER_BLOCK;
    private static final float SCALE = LINE / 10f;
    private static final int LEFT = -1;
    private static final int CENTRE = 0;
    private static final int RIGHT = 1;

    public ScreenRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(ScreenBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (!be.isController() || !be.powered()) {
            return;
        }
        Direction facing = be.facing();
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        ms.translate(-0.5, -0.5, -0.5);
        if (be.linked()) {
            ms.translate(1, 1, FRONT);
            ms.mulPose(Axis.YP.rotationDegrees(180));
            ms.scale(1, -1, 1);
            new Board(be, ms.last().pose(), buffer).draw();
        } else {
            lines(be, ms, buffer);
        }
        ms.popPose();
    }

    private void lines(ScreenBlockEntity be, PoseStack ms, MultiBufferSource buffer) {
        List<String> lines = be.lines();
        Font font = Minecraft.getInstance().font;
        int widest = Math.round((be.width() - 2 * MARGIN) / SCALE);
        float pitch = (be.height() - 2 * MARGIN) / (be.height() * ScreenBlockEntity.LINES_PER_BLOCK);
        for (int i = 0; i < lines.size(); i++) {
            String text = lines.get(i);
            if (text.isEmpty()) {
                continue;
            }
            ms.pushPose();
            ms.translate(1 - MARGIN, 1 - MARGIN - (i + 0.5f) * pitch, FRONT);
            ms.mulPose(Axis.YP.rotationDegrees(180));
            ms.scale(SCALE, -SCALE, SCALE);
            font.drawInBatch(font.plainSubstrByWidth(text, widest), 0, -4f, GREEN, false, ms.last().pose(), buffer, Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
            ms.popPose();
        }
    }

    private static final class Board {
        private final ScreenBlockEntity be;
        private final Matrix4f base;
        private final MultiBufferSource buffer;
        private final Font font = Minecraft.getInstance().font;
        private final List<Runnable> texts = new ArrayList<>();
        private final float w;
        private final float h;
        private final float u;
        private VertexConsumer quads;

        Board(ScreenBlockEntity be, Matrix4f base, MultiBufferSource buffer) {
            this.be = be;
            this.base = new Matrix4f(base);
            this.buffer = buffer;
            this.w = be.width();
            this.h = be.height();
            this.u = Math.min(w / ScreenBlockEntity.BOARD_WIDTH, h / ScreenBlockEntity.BOARD_HEIGHT);
        }

        void draw() {
            quads = buffer.getBuffer(RenderType.textBackground());
            if (be.bigEnough()) {
                float pad = 0.16f * u;
                float side = w * 0.285f;
                Trend[] trends = Trend.values();
                graph(trends[0], pad, pad, side, h / 2 - pad / 2);
                graph(trends[1], pad, h / 2 + pad / 2, side, h - pad);
                graph(trends[2], w - side, pad, w - pad, h / 2 - pad / 2);
                graph(trends[3], w - side, h / 2 + pad / 2, w - pad, h - pad);
                centre(side + pad, pad, w - side - pad, h - pad);
            } else {
                float size = Math.min(0.3f, Math.min(w, h) * 0.22f);
                float y = h / 2 - size * 1.3f;
                text(Component.translatable("criticalmass.screen.board.too_small", ScreenBlockEntity.BOARD_WIDTH, ScreenBlockEntity.BOARD_HEIGHT).getString(), w / 2, y, size, AMBER, CENTRE, w - 0.4f);
                text(Component.translatable("criticalmass.screen.board.size", be.width(), be.height()).getString(), w / 2, y + size * 1.5f, size * 0.8f, DIM, CENTRE, w - 0.4f);
            }
            for (Runnable text : texts) {
                text.run();
            }
        }

        private void vertex(float x, float y, int color) {
            quads.addVertex(base, x, y, 0).setColor((color >> 16) & 255, (color >> 8) & 255, color & 255, 255).setLight(LightTexture.FULL_BRIGHT);
        }

        private void poly(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, int color) {
            vertex(ax, ay, color);
            vertex(bx, by, color);
            vertex(cx, cy, color);
            vertex(dx, dy, color);
            vertex(dx, dy, color);
            vertex(cx, cy, color);
            vertex(bx, by, color);
            vertex(ax, ay, color);
        }

        private void line(float x0, float y0, float x1, float y1, float thickness, int color) {
            float dx = x1 - x0;
            float dy = y1 - y0;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length < 1e-5f) {
                return;
            }
            float nx = -dy / length * thickness / 2;
            float ny = dx / length * thickness / 2;
            poly(x0 + nx, y0 + ny, x0 - nx, y0 - ny, x1 - nx, y1 - ny, x1 + nx, y1 + ny, color);
        }

        private void box(float x0, float y0, float x1, float y1, float thickness, int color) {
            line(x0, y0, x1, y0, thickness, color);
            line(x1, y0, x1, y1, thickness, color);
            line(x1, y1, x0, y1, thickness, color);
            line(x0, y1, x0, y0, thickness, color);
        }

        private void text(String text, float x, float y, float height, int color, int align, float maxWidth) {
            if (text.isEmpty()) {
                return;
            }
            float scale = height / 8f;
            float width = font.width(text) * scale;
            if (maxWidth > 0 && width > maxWidth) {
                scale *= maxWidth / width;
                width = maxWidth;
            }
            float left = align == LEFT ? x : align == RIGHT ? x - width : x - width / 2;
            Matrix4f at = new Matrix4f(base).translate(left, y, 0.002f).scale(scale, scale, scale);
            texts.add(() -> font.drawInBatch(text, 0, 0, color, false, at, buffer, Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT));
        }

        private void graph(Trend trend, float x0, float y0, float x1, float y1) {
            float title = 0.2f * u;
            float small = 0.13f * u;
            float thin = 0.012f * u;
            text(Component.translatable("criticalmass.screen.trend." + trend.key).getString(), x0, y0, title, GREEN, LEFT, (x1 - x0) * 0.68f);
            text(be.samples() == 0 ? "--" : PlantDisplaySources.number(be.latest(trend)), x1, y0, title, GREEN, RIGHT, (x1 - x0) * 0.28f);
            float px0 = x0 + 0.44f * u;
            float px1 = x1;
            float py0 = y0 + 0.34f * u;
            float py1 = y1 - 0.24f * u;
            for (int i = 1; i < 4; i++) {
                float gy = py0 + (py1 - py0) * i / 4;
                float gx = px0 + (px1 - px0) * i / 4;
                line(px0, gy, px1, gy, thin, GRID);
                line(gx, py0, gx, py1, thin, GRID);
            }
            box(px0, py0, px1, py1, thin * 1.6f, DIM);
            float max = be.scale(trend);
            if (max > 0) {
                text(PlantDisplaySources.number(max), px0 - 0.05f * u, py0 - small * 0.2f, small, DIM, RIGHT, 0.38f * u);
                text(PlantDisplaySources.number(max / 2), px0 - 0.05f * u, (py0 + py1) / 2 - small / 2, small, DIM, RIGHT, 0.38f * u);
            }
            text("0", px0 - 0.05f * u, py1 - small * 0.9f, small, DIM, RIGHT, 0.38f * u);
            text(Component.translatable("criticalmass.screen.trend.past").getString(), px0, py1 + 0.06f * u, small, DIM, LEFT, 0);
            text(Component.translatable("criticalmass.screen.trend.now").getString(), px1, py1 + 0.06f * u, small, DIM, RIGHT, 0);
            byte[] plot = be.plot(trend);
            float step = (px1 - px0) / (ScreenBlockEntity.HISTORY - 1);
            float lastX = 0;
            float lastY = 0;
            for (int i = 0; i < plot.length; i++) {
                float x = px1 - (plot.length - 1 - i) * step;
                float y = py1 - (plot[i] & 255) / 255f * (py1 - py0);
                if (i > 0) {
                    line(lastX, lastY, x, y, 0.03f * u, GREEN);
                }
                lastX = x;
                lastY = y;
            }
        }

        private int tone(String key) {
            if (key.contains(".trip.") || key.contains("breached") || key.contains("damaged") || key.contains("no_reactor")) {
                return RED;
            }
            if (key.startsWith("criticalmass.alarm.")) {
                return AMBER;
            }
            return key.contains("running") || key.contains("cleared") ? GREEN : AMBER;
        }

        private void centre(float x0, float y0, float x1, float y1) {
            float width = x1 - x0;
            String status = be.status();
            float y = y0;
            text(status.isEmpty() ? "--" : Component.translatable(status).getString(), (x0 + x1) / 2, y, 0.38f * u, status.isEmpty() ? DIM : tone(status), CENTRE, width);
            y += 0.5f * u;
            line(x0, y, x1, y, 0.02f * u, DIM);
            y += 0.1f * u;
            float row = 0.236f * u;
            float size = 0.19f * u;
            List<PlantChannel> rows = ScreenBlockEntity.ROWS;
            List<String> values = be.values();
            for (int i = 0; i < rows.size(); i++) {
                text(Component.translatable("criticalmass.screen.row." + rows.get(i).key).getString(), x0, y, size, DIM, LEFT, width * 0.5f);
                text(i < values.size() ? values.get(i) : "--", x1, y, size, GREEN, RIGHT, width * 0.48f);
                y += row;
            }
            if (be.hasTurbine()) {
                text(Component.translatable("criticalmass.screen.row.turbine").getString(), x0, y, size, DIM, LEFT, width * 0.5f);
                text(be.speed().isEmpty() ? "--" : be.speed(), x1, y, size, GREEN, RIGHT, width * 0.48f);
                y += row;
            }
            if (be.hasGenerator()) {
                text(Component.translatable("criticalmass.screen.row.output").getString(), x0, y, size, DIM, LEFT, width * 0.5f);
                text(be.output().isEmpty() ? "--" : be.output(), x1, y, size, GREEN, RIGHT, width * 0.48f);
                y += row;
            }
            y += 0.06f * u;
            line(x0, y, x1, y, 0.02f * u, DIM);
            y += 0.08f * u;
            text(Component.translatable("criticalmass.screen.events").getString(), x0, y, 0.17f * u, DIM, LEFT, width);
            y += 0.24f * u;
            float entry = 0.16f * u;
            long now = be.getLevel() == null ? 0 : be.getLevel().getGameTime();
            List<ScreenBlockEntity.Event> events = be.events();
            if (events.isEmpty()) {
                text(Component.translatable("criticalmass.screen.events.none").getString(), x0, y, entry, DIM, LEFT, width);
            }
            for (ScreenBlockEntity.Event e : events) {
                if (y + entry > y1) {
                    break;
                }
                long seconds = Math.max(0, (now - e.time()) / 20);
                String age = seconds / 60 + ":" + (seconds % 60 < 10 ? "0" : "") + seconds % 60;
                text(age, x0, y, entry, DIM, LEFT, 0.5f * u);
                text(e.text().getString().toUpperCase(java.util.Locale.ROOT), x0 + 0.55f * u, y, entry, tone(e.about().isEmpty() ? e.key() : "cleared"), LEFT, width - 0.55f * u);
                y += 0.2f * u;
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(ScreenBlockEntity be) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ScreenBlockEntity be) {
        AABB box = new AABB(be.getBlockPos());
        if (!be.isController()) {
            return box;
        }
        return box.minmax(new AABB(be.getBlockPos().relative(ScreenBlockEntity.left(be.facing()).getOpposite(), be.width() - 1).below(be.height() - 1)));
    }
}
