package com.criticalmass.client;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.InstrumentationPanelBlock;
import com.criticalmass.content.thermal.InstrumentationPanelBlockEntity;
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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

public class InstrumentationPanelRenderer extends SafeBlockEntityRenderer<InstrumentationPanelBlockEntity> {
    private static final float FRONT = 13 / 16f - 0.002f;
    private static final float PX = 1 / 16f;
    private static final float WHITE_U = 0.5f / 32f;
    private static final float WHITE_V = 20.5f / 32f;
    private static final float SWEEP = (float) Math.toRadians(135);
    private static final int NEEDLE = 0xFFC0261C;
    private static final int[] LAMP_COLORS = {0, 0xFF6CE35C, 0xFFFFC23A, 0xFFFF5040};
    private static final float[][] LAMPS = {{10, 12}, {7, 9}, {4, 6}};
    private static final float SCREEN_CENTER_X = 8f;
    private static final float SCREEN_CENTER_Y = 5.5f;
    private static final float SCREEN_WIDTH = 9.5f;
    private static final float TEXT_HEIGHT = 2.6f;

    public InstrumentationPanelRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(InstrumentationPanelBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Direction facing = be.getBlockState().getValue(InstrumentationPanelBlock.FACING);
        be.shownCore += (float) ((be.core() - be.shownCore) * 0.08);
        be.shownFlow += (be.flow() - be.shownFlow) * 0.08f;
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        ms.translate(-0.5, -0.5, -0.5);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(CriticalMass.asResource("block/instrumentation_panel"));
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        float u = sprite.getU(WHITE_U);
        float v = sprite.getV(WHITE_V);
        Matrix4f pose = ms.last().pose();
        int bright = LightTexture.FULL_BRIGHT;
        needle(vc, ms, pose, u, v, 10.5f, 10f, be.linked() ? InstrumentationPanelBlockEntity.coreFraction(be.shownCore) : 0, light);
        needle(vc, ms, pose, u, v, 5.5f, 10f, be.linked() ? InstrumentationPanelBlockEntity.flowFraction(Math.round(be.shownFlow)) : 0, light);
        InstrumentationPanelBlockEntity.Lamp lamp = be.lamp();
        if (lamp != InstrumentationPanelBlockEntity.Lamp.NONE) {
            float[] x = LAMPS[lamp.ordinal() - 1];
            quad(vc, ms, pose, u, v, x[0], 3f, x[1], 4f, LAMP_COLORS[lamp.ordinal()], bright);
        }
        text(be, ms, buffer, lamp, bright);
        ms.popPose();
    }

    private static void needle(VertexConsumer vc, PoseStack ms, Matrix4f pose, float u, float v, float cx, float cy, float fraction, int light) {
        float angle = -SWEEP + 2 * SWEEP * fraction;
        float dx = (float) -Math.sin(angle);
        float dy = (float) Math.cos(angle);
        float len = 1.7f;
        float half = 0.22f;
        float px = -dy * half;
        float py = dx * half;
        float x0 = cx * PX;
        float y0 = cy * PX;
        float x1 = (cx + dx * len) * PX;
        float y1 = (cy + dy * len) * PX;
        vertex(vc, ms, pose, x0 - px * PX, y0 - py * PX, u, v, NEEDLE, light);
        vertex(vc, ms, pose, x1 - px * PX, y1 - py * PX, u, v, NEEDLE, light);
        vertex(vc, ms, pose, x1 + px * PX, y1 + py * PX, u, v, NEEDLE, light);
        vertex(vc, ms, pose, x0 + px * PX, y0 + py * PX, u, v, NEEDLE, light);
    }

    private static void quad(VertexConsumer vc, PoseStack ms, Matrix4f pose, float u, float v, float x0, float y0, float x1, float y1, int color, int light) {
        vertex(vc, ms, pose, x0 * PX, y0 * PX, u, v, color, light);
        vertex(vc, ms, pose, x1 * PX, y0 * PX, u, v, color, light);
        vertex(vc, ms, pose, x1 * PX, y1 * PX, u, v, color, light);
        vertex(vc, ms, pose, x0 * PX, y1 * PX, u, v, color, light);
    }

    private static void vertex(VertexConsumer vc, PoseStack ms, Matrix4f pose, float x, float y, float u, float v, int color, int light) {
        vc.addVertex(pose, x, y, FRONT).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(ms.last(), 0, 0, -1);
    }

    private static void text(InstrumentationPanelBlockEntity be, PoseStack ms, MultiBufferSource buffer, InstrumentationPanelBlockEntity.Lamp lamp, int light) {
        Font font = Minecraft.getInstance().font;
        String shown = be.linked() ? Math.round(be.shownCore) + "°C" : "--";
        int color = lamp == InstrumentationPanelBlockEntity.Lamp.RED ? 0xFF5040 : lamp == InstrumentationPanelBlockEntity.Lamp.AMBER ? 0xFFC23A : be.linked() ? 0x6CE35C : 0x5A6066;
        int width = font.width(shown);
        float scale = Math.min(TEXT_HEIGHT / 8f, SCREEN_WIDTH / Math.max(1, width)) * PX;
        ms.pushPose();
        ms.translate(SCREEN_CENTER_X * PX, SCREEN_CENTER_Y * PX, FRONT - 0.001f);
        ms.mulPose(Axis.YP.rotationDegrees(180));
        ms.scale(scale, -scale, scale);
        font.drawInBatch(shown, -width / 2f, -4f, color, false, ms.last().pose(), buffer, Font.DisplayMode.POLYGON_OFFSET, 0, light);
        ms.popPose();
    }
}
