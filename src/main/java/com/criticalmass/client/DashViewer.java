package com.criticalmass.client;

import com.criticalmass.content.control.DashPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public final class DashViewer {
    private static final float PIVOT_X = 0.25f;
    private static final float PIVOT_Y = 0.0625f;
    private static final float PIVOT_Z = 0.25f;
    private static final float DEFAULT_YAW = 136.66673f;
    private static final float DEFAULT_PITCH = 43.666695f;
    private static final float DEFAULT_SCALE = 3.0759742f;
    private static final float DEFAULT_PAN_Y = 2f;
    private static final float ZOOM_STEP = 1.1f;
    private static final int ZOOM_LIMIT = 16;
    private static final float ICON_DROP = 0.09f;
    private static final float ICON_SCALE = 0.92f;
    private static final float DEPTH = 250f;
    private static float yaw = DEFAULT_YAW;
    private static float pitch = DEFAULT_PITCH;
    private static float panX;
    private static float panY = DEFAULT_PAN_Y;
    private static int zoom;
    private final BlockState state;
    private int drag;
    private int x;
    private int y;
    private int w;
    private int h;

    public DashViewer(BlockState state) {
        this.state = state;
    }

    private void draw(DashPart part, float animation, boolean lit, PoseStack pose) {
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        DashPartDraw.draw(part, animation, lit, pose, buffers, LightTexture.FULL_BRIGHT, state);
        buffers.endBatch();
    }

    public void render(GuiGraphics g, DashPart part, float animation, boolean lit, int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        float size = Math.min(w, h) * 0.5f * DEFAULT_SCALE * (float) Math.pow(ZOOM_STEP, zoom);
        g.enableScissor(x, y, x + w, y + h);
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x + w / 2f + panX, y + h / 2f + panY, DEPTH);
        pose.scale(size, -size, size);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);
        draw(part, animation, lit, pose);
        pose.popPose();
        g.disableScissor();
    }

    public void icon(GuiGraphics g, DashPart part, float animation, boolean lit, int cx, int cy, int box) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy + box * ICON_DROP, DEPTH);
        float size = box * 0.5f * DEFAULT_SCALE * ICON_SCALE;
        pose.scale(size, -size, size);
        pose.mulPose(Axis.XP.rotationDegrees(DEFAULT_PITCH));
        pose.mulPose(Axis.YP.rotationDegrees(DEFAULT_YAW));
        pose.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);
        draw(part, animation, lit, pose);
        pose.popPose();
    }

    public void reset() {
        yaw = DEFAULT_YAW;
        pitch = DEFAULT_PITCH;
        panX = 0;
        panY = DEFAULT_PAN_Y;
        zoom = 0;
    }

    public int zoom() {
        return zoom;
    }

    private boolean inside(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!inside(mx, my)) {
            return false;
        }
        drag = button == 0 ? 1 : 2;
        return true;
    }

    public boolean mouseDragged(double dx, double dy) {
        if (drag == 1) {
            yaw += (float) dx;
            pitch = Mth.clamp((float) (pitch + dy), -90f, 90f);
            return true;
        }
        if (drag == 2) {
            panX += (float) dx;
            panY += (float) dy;
            return true;
        }
        return false;
    }

    public boolean mouseReleased() {
        boolean was = drag != 0;
        drag = 0;
        return was;
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!inside(mx, my) || amount == 0) {
            return false;
        }
        zoom = Mth.clamp(zoom + (amount > 0 ? 1 : -1), -ZOOM_LIMIT, ZOOM_LIMIT);
        return true;
    }
}
