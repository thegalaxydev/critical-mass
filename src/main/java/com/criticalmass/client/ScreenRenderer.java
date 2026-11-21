package com.criticalmass.client;

import com.criticalmass.content.control.CoreMap;
import com.criticalmass.content.control.Mimic;
import com.criticalmass.content.control.PlantChannel;
import com.criticalmass.content.control.PlantDisplaySources;
import com.criticalmass.content.control.ScreenBlockEntity;
import com.criticalmass.content.control.ScreenBlockEntity.Trend;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.criticalmass.content.control.ScreenBlock;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.world.level.block.state.BlockState;
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

public class ScreenRenderer extends KineticBlockEntityRenderer<ScreenBlockEntity> {
    private static final int GREEN = 0x41FF6A;
    private static final int DIM = 0x25A54D;
    private static final int GRID = 0x12491F;
    private static final int AMBER = 0xFFC23A;
    private static final int RED = 0xFF5548;
    private static final float FRONT = 3 / 16f - 0.004f;
    private static final float MARGIN = 1.5f / 16f;
    private static final float LINE = 1f / ScreenBlockEntity.LINES_PER_BLOCK;
    private static final float SCALE = LINE / 10f;
    private static final int LEFT = -1;
    private static final int CENTRE = 0;
    private static final int RIGHT = 1;

    public ScreenRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(ScreenBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacingVertical(AllPartialModels.SHAFTLESS_COGWHEEL, state, state.getValue(ScreenBlock.FACING));
    }

    @Override
    protected void renderSafe(ScreenBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
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
        private float u;
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
            if (be.fullBoard()) {
                float pad = 0.16f * u;
                float side = w * 0.285f;
                graph(0, pad, pad, side, h / 2 - pad / 2);
                graph(1, pad, h / 2 + pad / 2, side, h - pad);
                graph(2, w - side, pad, w - pad, h / 2 - pad / 2);
                graph(3, w - side, h / 2 + pad / 2, w - pad, h - pad);
                centre(side + pad, pad, w - side - pad, h - pad);
            } else if (be.bigEnough() && be.core() != null) {
                coreMap(be.core());
            } else if (be.bigEnough() && !be.mimic().isEmpty()) {
                mimic();
            } else if (be.bigEnough()) {
                page();
            } else {
                float size = Math.min(0.3f, Math.min(w, h) * 0.22f);
                float y = h / 2 - size * 1.3f;
                text(Component.translatable("criticalmass.screen.board.too_small", ScreenBlockEntity.MIN_WIDTH, ScreenBlockEntity.MIN_HEIGHT).getString(), w / 2, y, size, AMBER, CENTRE, w - 0.4f);
                text(Component.translatable("criticalmass.screen.board.size", be.width(), be.height()).getString(), w / 2, y + size * 1.5f, size * 0.8f, DIM, CENTRE, w - 0.4f);
            }
            for (Runnable text : texts) {
                text.run();
            }
        }

        private static String upper(String key) {
            return Component.translatable(key).getString().toUpperCase(java.util.Locale.ROOT);
        }

        private static String named(PlantChannel channel) {
            return upper("criticalmass.channel." + channel.key);
        }

        private void page() {
            u = Math.min(1f, Math.min(w / 5f, h / 2.5f));
            float pad = 0.16f * u;
            float right = w - pad;
            String status = be.status();
            text(be.title().isEmpty() ? "--" : upper(be.title()), pad, pad, 0.3f * u, GREEN, LEFT, (right - pad) * 0.58f);
            text(status.isEmpty() ? "" : upper(status), right, pad + 0.03f * u, 0.25f * u, tone(status), RIGHT, (right - pad) * 0.4f);
            float top = pad + 0.42f * u;
            line(pad, top, right, top, 0.02f * u, DIM);
            top += 0.1f * u;
            float bottom = h - pad;
            List<PlantChannel> rows = be.rows();
            boolean beside = w >= 5 && !rows.isEmpty();
            float tableWidth = Math.max(2.2f, Math.min(4f, w * 0.3f));
            float graphsRight = beside ? right - tableWidth - pad : right;
            float graphsBottom = bottom;
            boolean under = !beside && h >= 3.5f && !rows.isEmpty();
            if (under) {
                graphsBottom = bottom - Math.min(rows.size() * 0.236f * u + 0.16f * u, (bottom - top) * 0.45f);
            }
            int count = be.trends().size();
            float gap = 0.14f * u;
            float areaWidth = graphsRight - pad;
            float areaHeight = graphsBottom - top;
            int maxColumns = Math.max(1, (int) (areaWidth / 2.2f));
            int maxRows = Math.max(1, (int) (areaHeight / 1.0f));
            int shown = Math.min(count, maxColumns * maxRows);
            if (shown > 0) {
                int columns = Math.min(maxColumns, (int) Math.ceil(shown / (float) maxRows));
                int lines = (int) Math.ceil(shown / (float) columns);
                float cellWidth = (areaWidth - gap * (columns - 1)) / columns;
                float cellHeight = (areaHeight - gap * (lines - 1)) / lines;
                for (int i = 0; i < shown; i++) {
                    float x = pad + (i % columns) * (cellWidth + gap);
                    float y = top + (i / columns) * (cellHeight + gap);
                    graph(i, x, y, x + cellWidth, y + cellHeight);
                }
            }
            if (beside) {
                table(right - tableWidth, top, right, bottom);
            } else if (under) {
                table(pad, graphsBottom + 0.12f * u, right, bottom);
            }
        }

        private void table(float x0, float y0, float x1, float y1) {
            float width = x1 - x0;
            float row = 0.236f * u;
            float size = 0.19f * u;
            float y = y0;
            List<PlantChannel> rows = be.rows();
            List<String> values = be.values();
            text(Component.translatable("criticalmass.screen.readings").getString(), x0, y, 0.17f * u, DIM, LEFT, width);
            y += 0.26f * u;
            for (int i = 0; i < rows.size() && y + size <= y1; i++) {
                text(named(rows.get(i)), x0, y, size, DIM, LEFT, width * 0.56f);
                text(i < values.size() ? values.get(i) : "--", x1, y, size, GREEN, RIGHT, width * 0.42f);
                y += row;
            }
            y += 0.08f * u;
            if (y + 0.7f * u <= y1) {
                line(x0, y, x1, y, 0.02f * u, DIM);
                events(x0, y + 0.08f * u, x1, y1);
            }
        }

        private void events(float x0, float top, float x1, float y1) {
            float width = x1 - x0;
            float y = top;
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

        private void ring(float cx, float cy, float r, float thickness, int color) {
            float lastX = cx + r;
            float lastY = cy;
            for (int i = 1; i <= 14; i++) {
                double angle = Math.PI * 2 * i / 14;
                float x = cx + (float) Math.cos(angle) * r;
                float y = cy + (float) Math.sin(angle) * r;
                line(lastX, lastY, x, y, thickness, color);
                lastX = x;
                lastY = y;
            }
        }

        private void lettered(String letter, float cx, float cy, float s, float thin, int color) {
            ring(cx, cy, s, thin, color);
            text(letter, cx, cy - s * 0.5f, s * 1.1f, color, CENTRE, 0);
        }

        private void symbol(Mimic.Node node, float cx, float cy, float s, float thin, float low, float high) {
            int color = node.state() == 2 && node.kind() != Mimic.Kind.TRANSFER ? AMBER : node.live() ? GREEN : DIM;
            switch (node.kind()) {
                case GRID -> {
                    line(cx - s, cy - s, cx + s, cy - s, thin * 2, color);
                    line(cx - s, cy - s, cx, cy + s, thin, color);
                    line(cx + s, cy - s, cx, cy + s, thin, color);
                }
                case BREAKER -> {
                    float a = s * 0.8f;
                    if (node.state() == 1) {
                        poly(cx - a, cy - a, cx - a, cy + a, cx + a, cy + a, cx + a, cy - a, color);
                    } else {
                        box(cx - a, cy - a, cx + a, cy + a, thin, color);
                        line(cx, cy - s, cx, cy - a, thin, color);
                        line(cx, cy + a, cx, cy + s, thin, color);
                        if (node.state() == 2) {
                            line(cx - a, cy - a, cx + a, cy + a, thin, color);
                            line(cx - a, cy + a, cx + a, cy - a, thin, color);
                        }
                    }
                }
                case DISCONNECT -> {
                    if (node.state() == 1) {
                        line(cx, cy - s, cx, cy + s, thin * 1.5f, color);
                    } else {
                        line(cx, cy + s, cx + s * 0.8f, cy - s * 0.5f, thin * 1.5f, color);
                    }
                    line(cx - s * 0.4f, cy + s, cx + s * 0.4f, cy + s, thin, color);
                    line(cx - s * 0.4f, cy - s, cx + s * 0.4f, cy - s, thin, color);
                }
                case TRANSFORMER -> {
                    ring(cx, cy - s * 0.4f, s * 0.6f, thin, color);
                    ring(cx, cy + s * 0.4f, s * 0.6f, thin, color);
                }
                case BUS -> line(Math.min(low, cx - s) - s * 0.5f, cy, Math.max(high, cx + s) + s * 0.5f, cy, thin * 3, color);
                case GENERATOR -> lettered("G", cx, cy, s, thin, color);
                case MOTOR -> lettered("M", cx, cy, s, thin, color);
                case BATTERY -> lettered("B", cx, cy, s, thin, color);
                case TRANSFER -> {
                    line(cx, cy - s, cx + s, cy, thin, color);
                    line(cx + s, cy, cx, cy + s, thin, color);
                    line(cx, cy + s, cx - s, cy, thin, color);
                    line(cx - s, cy, cx, cy - s, thin, color);
                    text(node.state() == 0 ? "N" : node.state() == 1 ? "S" : "-", cx, cy - s * 0.4f, s * 0.9f, node.state() == 0 ? color : AMBER, CENTRE, 0);
                }
                case EARTH -> {
                    line(cx, cy - s, cx, cy, thin, color);
                    line(cx - s * 0.8f, cy, cx + s * 0.8f, cy, thin, color);
                    line(cx - s * 0.5f, cy + s * 0.35f, cx + s * 0.5f, cy + s * 0.35f, thin, color);
                    line(cx - s * 0.2f, cy + s * 0.7f, cx + s * 0.2f, cy + s * 0.7f, thin, color);
                }
            }
        }

        private void mimic() {
            u = Math.min(1f, Math.min(w / 5f, h / 2.5f));
            float pad = 0.16f * u;
            float right = w - pad;
            String status = be.status();
            text(upper("criticalmass.screen.mimic"), pad, pad, 0.3f * u, GREEN, LEFT, (right - pad) * 0.58f);
            text(status.isEmpty() ? "" : upper(status), right, pad + 0.03f * u, 0.25f * u, tone(status), RIGHT, (right - pad) * 0.4f);
            float top = pad + 0.42f * u;
            line(pad, top, right, top, 0.02f * u, DIM);
            top += 0.06f * u;
            List<Mimic.Node> nodes = be.mimic();
            float columns = 1;
            int levels = 1;
            for (Mimic.Node node : nodes) {
                columns = Math.max(columns, node.x() + 1);
                levels = Math.max(levels, node.depth() + 1);
            }
            float cellWidth = (right - pad) / columns;
            float cellHeight = (h - pad - top) / levels;
            float s = Math.min(0.3f, Math.min(cellWidth * 0.3f, cellHeight * 0.28f));
            float thin = Math.max(0.012f, s * 0.12f);
            float label = Math.min(0.16f, Math.min(cellHeight * 0.26f, cellWidth * 0.2f));
            int count = nodes.size();
            float[] px = new float[count];
            float[] py = new float[count];
            float[] low = new float[count];
            float[] high = new float[count];
            boolean[] leaf = new boolean[count];
            for (int i = 0; i < count; i++) {
                px[i] = pad + (nodes.get(i).x() + 0.5f) * cellWidth;
                py[i] = top + (nodes.get(i).depth() + 0.5f) * cellHeight;
                low[i] = px[i];
                high[i] = px[i];
                leaf[i] = true;
            }
            for (int i = 0; i < count; i++) {
                int parent = nodes.get(i).parent();
                if (parent >= 0 && parent < count) {
                    low[parent] = Math.min(low[parent], px[i]);
                    high[parent] = Math.max(high[parent], px[i]);
                    leaf[parent] = false;
                }
            }
            for (int i = 0; i < count; i++) {
                Mimic.Node node = nodes.get(i);
                int parent = node.parent();
                if (parent < 0 || parent >= count) {
                    continue;
                }
                int color = node.live() ? GREEN : DIM;
                float end = node.kind() == Mimic.Kind.BUS ? py[i] : py[i] - s;
                if (nodes.get(parent).kind() == Mimic.Kind.BUS) {
                    line(px[i], py[parent], px[i], end, thin, color);
                } else {
                    float mid = py[parent] + cellHeight / 2;
                    line(px[parent], py[parent] + s, px[parent], mid, thin, color);
                    line(px[parent], mid, px[i], mid, thin, color);
                    line(px[i], mid, px[i], end, thin, color);
                }
            }
            for (int i = 0; i < count; i++) {
                Mimic.Node node = nodes.get(i);
                symbol(node, px[i], py[i], s, thin, low[i], high[i]);
                if (node.value().isEmpty()) {
                    continue;
                }
                int color = node.live() ? GREEN : DIM;
                if (node.kind() == Mimic.Kind.BUS) {
                    text(node.value(), Math.min(low[i], px[i] - s) - s * 0.5f, py[i] - label * 1.25f, label, color, LEFT, cellWidth);
                } else if (leaf[i]) {
                    text(node.value(), px[i], py[i] + s * 1.2f, label, color, CENTRE, cellWidth * 0.95f);
                } else {
                    text(node.value(), px[i] + s * 1.3f, py[i] - label / 2, label, color, LEFT, Math.max(0.1f, cellWidth * 0.5f - s * 1.3f + cellWidth * 0.4f));
                }
            }
        }

        private void coreMap(CoreMap.Snapshot map) {
            u = Math.min(1.6f, Math.min(w / 5f, h / 2.5f));
            float pad = 0.16f * u;
            float right = w - pad;
            String status = be.status();
            text(upper("criticalmass.screen.core"), pad, pad, 0.3f * u, GREEN, LEFT, (right - pad) * 0.5f);
            text(status.isEmpty() ? "" : upper(status), right, pad + 0.03f * u, 0.25f * u, tone(status), RIGHT, (right - pad) * 0.46f);
            float top = pad + 0.42f * u;
            line(pad, top, right, top, 0.02f * u, DIM);
            top += 0.1f * u;
            float bottom = h - pad;
            if (!map.present()) {
                text(upper("criticalmass.screen.core.none"), w / 2, (top + bottom) / 2 - 0.12f * u, 0.24f * u, AMBER, CENTRE, right - pad);
                return;
            }
            float gap = 0.2f * u;
            top += 0.16f * u;
            float areaWidth = right - pad;
            float areaHeight = bottom - top;
            float fit = Math.min(areaWidth / map.width(), areaHeight / map.depth());
            boolean beside = areaWidth - fit * map.width() - gap >= 1.7f * u;
            float tableHeight = 1.5f * u;
            boolean under = !beside && areaHeight - gap - tableHeight >= areaHeight * 0.4f;
            float cell = under ? Math.min(areaWidth / map.width(), (areaHeight - gap - tableHeight) / map.depth()) : fit;
            float gridWidth = cell * map.width();
            float gridHeight = cell * map.depth();
            float gridX = beside ? pad : pad + (areaWidth - gridWidth) / 2;
            float gridY = under ? top : top + (areaHeight - gridHeight) / 2;
            text(Component.translatable("criticalmass.screen.core.north").getString(), gridX + gridWidth / 2, gridY - 0.15f * u, 0.12f * u, DIM, CENTRE, 0);
            for (int z = 0; z < map.depth(); z++) {
                for (int x = 0; x < map.width(); x++) {
                    cell(map.cell(x, z), gridX + x * cell, gridY + z * cell, cell, true);
                }
            }
            if (beside) {
                float x0 = gridX + gridWidth + gap;
                summary(map, x0, top - 0.16f * u, Math.min(right, x0 + 3.2f * u), bottom);
            } else if (under) {
                summary(map, pad, gridY + gridHeight + gap, right, bottom);
            }
        }

        private int fuelTone(CoreMap.Cell c) {
            return switch (c.state()) {
                case EMPTY -> DIM;
                case ACTIVE -> c.percent() <= CoreMap.LOW_FUEL ? AMBER : GREEN;
                case SPENT -> AMBER;
                default -> RED;
            };
        }

        private void cell(CoreMap.Cell c, float x, float y, float size, boolean labelled) {
            float inset = size * 0.07f;
            float x0 = x + inset;
            float y0 = y + inset;
            float x1 = x + size - inset;
            float y1 = y + size - inset;
            float cx = x + size / 2;
            float cy = y + size / 2;
            float thin = Math.max(0.008f, size * 0.035f);
            boolean words = labelled && size >= 0.16f;
            switch (c.kind()) {
                case FUEL -> {
                    int color = fuelTone(c);
                    if (c.state() == CoreMap.State.MELTED) {
                        poly(x0, y0, x0, y1, x1, y1, x1, y0, GRID);
                        line(x0, y0, x1, y1, thin, RED);
                        line(x0, y1, x1, y0, thin, RED);
                    }
                    box(x0, y0, x1, y1, thin * (c.state() == CoreMap.State.EMPTY ? 1 : 1.6f), color);
                    if (c.state() == CoreMap.State.DAMAGED) {
                        line(x0, y0, x1, y1, thin, RED);
                    }
                    if (c.loaded()) {
                        float bar = y1 - thin - size * 0.1f;
                        float full = x1 - x0 - thin * 3;
                        float end = x0 + thin * 1.5f + full * c.percent() / 100f;
                        line(x0 + thin * 1.5f, bar, x1 - thin * 1.5f, bar, size * 0.08f, GRID);
                        line(x0 + thin * 1.5f, bar, end, bar, size * 0.08f, color);
                    }
                    if (c.cap() != CoreMap.Cap.NONE) {
                        float s = size * 0.09f;
                        float mx = x1 - thin * 1.5f - s;
                        float my = y0 + thin * 1.5f + s;
                        if (c.cap() == CoreMap.Cap.LOCKED) {
                            poly(mx - s, my - s, mx - s, my + s, mx + s, my + s, mx + s, my - s, DIM);
                        } else {
                            box(mx - s, my - s, mx + s, my + s, thin, AMBER);
                        }
                    }
                    if (words) {
                        String main = c.state() == CoreMap.State.EMPTY ? "--" : c.state() == CoreMap.State.MELTED ? "" : String.valueOf(c.percent());
                        text(main, cx, cy - size * 0.22f, size * 0.3f, color, CENTRE, size * 0.6f);
                        if (size >= 0.3f && c.state() != CoreMap.State.MELTED) {
                            text(c.temperature() + "\u00b0", cx, cy + size * 0.1f, size * 0.17f, c.state() == CoreMap.State.HOT ? RED : DIM, CENTRE, size * 0.7f);
                        }
                    }
                }
                case CONTROL -> {
                    boolean numbered = words && size >= 0.3f;
                    float radius = size * (numbered ? 0.27f : 0.34f);
                    float ry = numbered ? cy - size * 0.09f : cy;
                    ring(cx, ry, radius, thin, DIM);
                    float rod = radius * 0.7f * c.percent() / 100f;
                    if (rod > thin) {
                        poly(cx, ry - rod, cx - rod, ry, cx, ry + rod, cx + rod, ry, GREEN);
                    }
                    if (numbered) {
                        text(String.valueOf(c.percent()), cx, ry + radius + size * 0.04f, size * 0.17f, GREEN, CENTRE, size * 0.5f);
                    }
                }
                case COOLANT -> box(cx - size * 0.12f, cy - size * 0.12f, cx + size * 0.12f, cy + size * 0.12f, thin, GRID);
                default -> {
                }
            }
        }

        private float fitted(float height, float room, String... keys) {
            int widest = 1;
            for (String key : keys) {
                widest = Math.max(widest, font.width(Component.translatable(key).getString()));
            }
            return Math.min(height, room * 8f / widest);
        }

        private float fact(String key, String value, int color, float x0, float x1, float y, float size) {
            text(Component.translatable(key).getString(), x0, y, size, DIM, LEFT, 0);
            text(value, x1, y, size, color, RIGHT, (x1 - x0) * 0.36f);
            return y + 0.22f * u;
        }

        private float legend(CoreMap.Cell sample, String key, float x0, float y, float size) {
            float symbol = 0.16f * u;
            cell(sample, x0, y, symbol, false);
            text(Component.translatable(key).getString(), x0 + symbol * 1.4f, y + (symbol - size) / 2, size, DIM, LEFT, 0);
            return y + 0.18f * u;
        }

        private void summary(CoreMap.Snapshot map, float x0, float y0, float x1, float y1) {
            float y = y0;
            float width = x1 - x0;
            int columns = map.count(CoreMap.Kind.FUEL);
            int lowest = map.lowest();
            int hottest = map.hottest();
            int caps = map.caps(false);
            int unlocked = map.caps(true);
            boolean free = map.handling() == CoreMap.Handling.FREE;
            text(Component.translatable(map.handling().langKey()).getString(), x0, y, 0.2f * u, free ? GREEN : AMBER, LEFT, width);
            y += 0.28f * u;
            float size = fitted(0.19f * u, width * 0.6f, "criticalmass.screen.core.loaded", "criticalmass.screen.core.lowest", "criticalmass.screen.core.hottest", "criticalmass.screen.core.caps");
            y = fact("criticalmass.screen.core.loaded", Component.translatable("criticalmass.screen.core.of", map.loaded(), columns).getString(), map.loaded() == columns ? GREEN : AMBER, x0, x1, y, size);
            y = fact("criticalmass.screen.core.lowest", lowest < 0 ? "--" : lowest + " %", lowest >= 0 && lowest <= CoreMap.LOW_FUEL ? AMBER : GREEN, x0, x1, y, size);
            y = fact("criticalmass.screen.core.hottest", hottest < 0 ? "--" : hottest + " \u00b0C", hottest >= com.criticalmass.content.thermal.CMThermal.HOT_SPOT_TEMPERATURE ? RED : GREEN, x0, x1, y, size);
            if (caps > 0) {
                y = fact("criticalmass.screen.core.caps", Component.translatable("criticalmass.screen.core.of", unlocked, caps).getString(), unlocked > 0 ? AMBER : GREEN, x0, x1, y, size);
            }
            y += 0.06f * u;
            float pitch = 0.18f * u;
            if (y + 0.1f * u + 4 * pitch > y1 + 0.02f * u) {
                return;
            }
            line(x0, y, x1, y, 0.02f * u, DIM);
            y += 0.1f * u;
            float small = fitted(0.14f * u, width - 0.23f * u, "criticalmass.screen.core.legend.fuel", "criticalmass.screen.core.legend.low", "criticalmass.screen.core.legend.bad", "criticalmass.screen.core.legend.rod",
                    "criticalmass.screen.core.legend.locked", "criticalmass.screen.core.legend.unlocked");
            y = legend(new CoreMap.Cell(CoreMap.Kind.FUEL, CoreMap.State.ACTIVE, CoreMap.Cap.NONE, 70, 0), "criticalmass.screen.core.legend.fuel", x0, y, small);
            y = legend(new CoreMap.Cell(CoreMap.Kind.FUEL, CoreMap.State.SPENT, CoreMap.Cap.NONE, 8, 0), "criticalmass.screen.core.legend.low", x0, y, small);
            y = legend(new CoreMap.Cell(CoreMap.Kind.FUEL, CoreMap.State.DAMAGED, CoreMap.Cap.NONE, 40, 0), "criticalmass.screen.core.legend.bad", x0, y, small);
            y = legend(new CoreMap.Cell(CoreMap.Kind.CONTROL, CoreMap.State.EMPTY, CoreMap.Cap.NONE, 60, 0), "criticalmass.screen.core.legend.rod", x0, y, small);
            if (caps > 0 && y + 2 * pitch <= y1 + 0.02f * u) {
                y = legend(new CoreMap.Cell(CoreMap.Kind.FUEL, CoreMap.State.EMPTY, CoreMap.Cap.LOCKED, 0, 0), "criticalmass.screen.core.legend.locked", x0, y, small);
                legend(new CoreMap.Cell(CoreMap.Kind.FUEL, CoreMap.State.EMPTY, CoreMap.Cap.UNLOCKED, 0, 0), "criticalmass.screen.core.legend.unlocked", x0, y, small);
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

        private void graph(int index, float x0, float y0, float x1, float y1) {
            if (index >= be.trends().size()) {
                return;
            }
            PlantChannel channel = be.trends().get(index);
            String name = be.board() ? Component.translatable("criticalmass.screen.trend." + Trend.values()[index].key).getString() : named(channel) + (be.unit(index).isEmpty() ? "" : " " + be.unit(index));
            float title = 0.2f * u;
            float small = 0.13f * u;
            float thin = 0.012f * u;
            text(name, x0, y0, title, GREEN, LEFT, (x1 - x0) * 0.68f);
            text(be.samples() == 0 ? "--" : PlantDisplaySources.number(be.latest(index)), x1, y0, title, GREEN, RIGHT, (x1 - x0) * 0.28f);
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
            float min = be.low(index);
            float max = be.scale(index);
            if (max > min) {
                text(PlantDisplaySources.number(max), px0 - 0.05f * u, py0 - small * 0.2f, small, DIM, RIGHT, 0.38f * u);
                text(PlantDisplaySources.number((max + min) / 2), px0 - 0.05f * u, (py0 + py1) / 2 - small / 2, small, DIM, RIGHT, 0.38f * u);
            }
            text(PlantDisplaySources.number(min), px0 - 0.05f * u, py1 - small * 0.9f, small, DIM, RIGHT, 0.38f * u);
            text(Component.translatable("criticalmass.screen.trend.past").getString(), px0, py1 + 0.06f * u, small, DIM, LEFT, 0);
            text(Component.translatable("criticalmass.screen.trend.now").getString(), px1, py1 + 0.06f * u, small, DIM, RIGHT, 0);
            byte[] plot = be.plot(index);
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
            if (key.contains(".trip.") || key.contains("breached") || key.contains("damaged") || key.contains("no_target") || key.contains("overloaded") || key.contains("transformer.hot")) {
                return RED;
            }
            if (key.startsWith("criticalmass.alarm.")) {
                return AMBER;
            }
            return key.contains("running") || key.contains("cleared") || key.contains("synchronised") || key.contains("supplying") || key.contains("breaker.closed") || key.endsWith(".live") ? GREEN : AMBER;
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
                text(Component.translatable("criticalmass.screen.row.output").getString(), x0, y, size, DIM, LEFT, width * 0.5f);
                text(be.output().isEmpty() ? "--" : be.output(), x1, y, size, GREEN, RIGHT, width * 0.48f);
                y += row;
            }
            y += 0.06f * u;
            line(x0, y, x1, y, 0.02f * u, DIM);
            events(x0, y + 0.08f * u, x1, y1);
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
