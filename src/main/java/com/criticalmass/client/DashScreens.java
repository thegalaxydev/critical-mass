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

    public static void grabLever(BlockPos pos, int slot, int level, boolean inverted) {
        LeverHold.grab(pos, slot, level, inverted);
    }
}
