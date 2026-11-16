package com.criticalmass.client;

import com.criticalmass.content.control.DashLeverSet;
import com.criticalmass.content.control.DashPart;
import com.criticalmass.content.control.DashboardBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.createmod.catnip.gui.UIRenderHelper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

public final class LeverHold {
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 14;
    private static final int TEXT_COLOR = 0x4A3A28;
    private static final double REACH = 6.0;
    private static final double FULL_TRAVEL = 180.0;
    private static boolean active;
    private static BlockPos pos = BlockPos.ZERO;
    private static int slot;
    private static boolean inverted;
    private static int signal;
    private static int sent;
    private static float value;
    private static float shown;
    private static float lastShown;

    private LeverHold() {
    }

    public static boolean active() {
        return active;
    }

    public static void grab(BlockPos at, int partSlot, int level, boolean partInverted) {
        if (active) {
            return;
        }
        active = true;
        pos = at;
        slot = partSlot;
        inverted = partInverted;
        signal = sent = Mth.clamp(level, 0, DashPart.MAX_LEVEL);
        value = signal / (float) DashPart.MAX_LEVEL;
        shown = lastShown = value;
    }

    public static void tick() {
        if (!active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean held = mc.player != null && mc.level != null && mc.screen == null && mc.options.keyUse.isDown()
                && mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= REACH * REACH
                && mc.level.getBlockEntity(pos) instanceof DashboardBlockEntity desk && desk.part(slot) != null && desk.part(slot).type == DashPart.Type.LEVER;
        if (!held) {
            active = false;
            return;
        }
        lastShown = shown;
        shown = shown * 0.15f + value * 0.85f;
    }

    public static void mouse(double dx, double dy) {
        if (!active) {
            return;
        }
        double sensitivity = Minecraft.getInstance().options.sensitivity().get() * 0.6 + 0.2;
        double pitch = dy * sensitivity * sensitivity * sensitivity * 8.0 * 0.15;
        value = Mth.clamp(value - (float) (pitch / FULL_TRAVEL), 0, 1);
        signal = Math.round(value * DashPart.MAX_LEVEL);
        if (signal != sent) {
            sent = signal;
            PacketDistributor.sendToServer(new DashLeverSet(pos, slot, signal));
        }
    }

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || mc.options.hideGui) {
            return;
        }
        int x = graphics.guiWidth() / 2 - BAR_WIDTH / 2 + 16;
        int y = graphics.guiHeight() / 2 - BAR_HEIGHT / 2;
        PoseStack ms = graphics.pose();
        ms.pushPose();
        ms.translate(x + BAR_WIDTH / 2f, y + BAR_HEIGHT / 2f, 0);
        ms.mulPose(Axis.ZP.rotationDegrees(90));
        ms.translate(-x - BAR_WIDTH / 2f, -y - BAR_HEIGHT / 2f, 0);
        AllGuiTextures.BRASS_FRAME_TL.render(graphics, x, y);
        AllGuiTextures.BRASS_FRAME_TR.render(graphics, x + BAR_WIDTH - 4, y);
        AllGuiTextures.BRASS_FRAME_BL.render(graphics, x, y + BAR_HEIGHT - 4);
        AllGuiTextures.BRASS_FRAME_BR.render(graphics, x + BAR_WIDTH - 4, y + BAR_HEIGHT - 4);
        UIRenderHelper.drawStretched(graphics, x, y + 4, 3, BAR_HEIGHT - 8, 2, AllGuiTextures.BRASS_FRAME_LEFT);
        UIRenderHelper.drawStretched(graphics, x + BAR_WIDTH - 3, y + 4, 3, BAR_HEIGHT - 8, 2, AllGuiTextures.BRASS_FRAME_RIGHT);
        UIRenderHelper.drawCropped(graphics, x + 4, y, BAR_WIDTH - 8, 3, 2, AllGuiTextures.BRASS_FRAME_TOP);
        UIRenderHelper.drawCropped(graphics, x + 4, y + BAR_HEIGHT - 3, BAR_WIDTH - 8, 3, 2, AllGuiTextures.BRASS_FRAME_BOTTOM);
        int step = AllGuiTextures.VALUE_SETTINGS_BAR.getWidth() - 1;
        for (int filled = 0; filled < BAR_WIDTH - 6; filled += step) {
            UIRenderHelper.drawCropped(graphics, x + 3 + filled, y + 3, Math.min(step, BAR_WIDTH - 6 - filled), 8, 2, AllGuiTextures.VALUE_SETTINGS_BAR);
        }
        ms.popPose();
        float at = Mth.lerp(delta.getGameTimeDeltaPartialTick(false), lastShown, shown);
        float cursorY = (1 - 2 * at) * 3 * BAR_HEIGHT + 2;
        int cursorX = x + BAR_WIDTH / 2 - 7;
        ms.pushPose();
        ms.translate(0, y + BAR_HEIGHT / 2f - 9 + cursorY, 4);
        AllGuiTextures.VALUE_SETTINGS_CURSOR_LEFT.render(graphics, cursorX - 3, 0);
        UIRenderHelper.drawCropped(graphics, cursorX, 0, 14, 14, 2, AllGuiTextures.VALUE_SETTINGS_CURSOR);
        AllGuiTextures.VALUE_SETTINGS_CURSOR_RIGHT.render(graphics, cursorX + 14, 0);
        ms.translate(0, 0, 4);
        String text = String.valueOf(inverted ? DashPart.MAX_LEVEL - signal : signal);
        graphics.drawString(mc.font, text, cursorX + 7 - mc.font.width(text) / 2, 3, TEXT_COLOR, false);
        ms.popPose();
    }
}
