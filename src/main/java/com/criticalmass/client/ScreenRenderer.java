package com.criticalmass.client;

import com.criticalmass.content.control.ScreenBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public class ScreenRenderer extends SafeBlockEntityRenderer<ScreenBlockEntity> {
    private static final int GREEN = 0x41FF6A;
    private static final float FRONT = 13 / 16f - 0.004f;
    private static final float MARGIN = 1.5f / 16f;
    private static final float LINE = 1f / ScreenBlockEntity.LINES_PER_BLOCK;
    private static final float SCALE = LINE / 10f;

    public ScreenRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(ScreenBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (!be.isController() || !be.powered()) {
            return;
        }
        List<String> lines = be.lines();
        if (lines.isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        Direction facing = be.facing();
        int widest = Math.round((be.width() - 2 * MARGIN) / SCALE);
        float pitch = (be.height() - 2 * MARGIN) / (be.height() * ScreenBlockEntity.LINES_PER_BLOCK);
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        ms.translate(-0.5, -0.5, -0.5);
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
        ms.popPose();
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
