package com.criticalmass.client;

import com.criticalmass.content.control.DashboardBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class DashScreens {
    private DashScreens() {
    }

    public static void open(BlockPos pos, int slot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof DashboardBlockEntity desk && desk.part(slot) != null) {
            mc.setScreen(new DashPartScreen(desk, slot));
        }
    }

    private static final int OPEN_TRIES = 40;
    private static BlockPos waiting;
    private static int waitingSlot;
    private static int tries;

    public static void openSoon(BlockPos pos, int slot) {
        waiting = pos;
        waitingSlot = slot;
        tries = OPEN_TRIES;
    }

    public static void tick() {
        if (waiting == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.screen == null && mc.level.getBlockEntity(waiting) instanceof DashboardBlockEntity desk && desk.part(waitingSlot) != null) {
            mc.setScreen(new DashPartScreen(desk, waitingSlot));
            waiting = null;
        } else if (--tries <= 0) {
            waiting = null;
        }
    }

    public static void grabLever(BlockPos pos, int slot, int level, boolean inverted) {
        LeverHold.grab(pos, slot, level, inverted);
    }
}
