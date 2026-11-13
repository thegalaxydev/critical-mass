package com.criticalmass.client;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class DashColumn {
    public interface Slot {
        void render(GuiGraphics g, int index, int centerX, int centerY, int size);
    }

    private static final int EDGE_SIZE = 22;
    private static final int CENTER_SIZE = 24;
    private static final float SCROLL_MILLIS = 140f;
    private static final int HALF = 12;
    private static final int REACH = 11;
    private final int centerX;
    private final int topY;
    private final int midY;
    private final int bottomY;
    private final int count;
    private final Slot slot;
    private int selected;
    private float scrollFrom;
    private long scrollStart;

    public DashColumn(int centerX, int topY, int midY, int bottomY, int count, Slot slot) {
        this.centerX = centerX;
        this.topY = topY;
        this.midY = midY;
        this.bottomY = bottomY;
        this.count = count;
        this.slot = slot;
    }

    public int selected() {
        return selected;
    }

    public void select(int index) {
        selected = Math.floorMod(index, count);
    }

    private int wrapped(int offset) {
        return Math.floorMod(selected + offset, count);
    }

    private void step(int direction) {
        scrollFrom = Mth.clamp(scrollOffset() + direction, -1.5f, 1.5f);
        scrollStart = Util.getMillis();
        selected = wrapped(direction);
    }

    private float scrollOffset() {
        float t = Mth.clamp((Util.getMillis() - scrollStart) / SCROLL_MILLIS, 0f, 1f);
        float eased = 1f - (1f - t) * (1f - t) * (1f - t);
        return scrollFrom * (1f - eased);
    }

    private int slotY(float position) {
        return Math.round(position < 0 ? midY + position * (midY - topY) : midY + position * (bottomY - midY));
    }

    public void render(GuiGraphics g) {
        float scroll = scrollOffset();
        if (scroll == 0) {
            slot.render(g, wrapped(-1), centerX, topY, 14);
            slot.render(g, selected, centerX, midY, 18);
            slot.render(g, wrapped(1), centerX, bottomY, 14);
            return;
        }
        g.enableScissor(centerX - HALF, topY - REACH, centerX + HALF, bottomY + REACH);
        for (int offset = -2; offset <= 2; offset++) {
            float position = offset + scroll;
            if (Math.abs(position) <= 1.6f) {
                slot.render(g, wrapped(offset), centerX, slotY(position), Math.round(Mth.lerp(Math.min(1f, Math.abs(position)), 18f, 14f)));
            }
        }
        g.disableScissor();
    }

    public boolean hovered(double mx, double my) {
        return mx >= centerX - HALF && mx < centerX + HALF && my >= topY - REACH && my < bottomY + REACH;
    }

    private boolean hit(double mx, double my, int cy, int size) {
        double half = size / 2.0;
        return mx >= centerX - half && mx < centerX + half && my >= cy - half && my < cy + half;
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!hovered(mx, my) || amount == 0) {
            return false;
        }
        step(amount > 0 ? -1 : 1);
        return true;
    }

    public boolean mouseClicked(double mx, double my) {
        if (hit(mx, my, topY, EDGE_SIZE)) {
            step(-1);
            return true;
        }
        if (hit(mx, my, bottomY, EDGE_SIZE)) {
            step(1);
            return true;
        }
        return hit(mx, my, midY, CENTER_SIZE);
    }
}
