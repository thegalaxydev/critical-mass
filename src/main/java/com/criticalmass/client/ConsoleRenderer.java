package com.criticalmass.client;

import com.criticalmass.content.control.ConsoleBlock;
import com.criticalmass.content.control.ConsoleBlockEntity;
import com.criticalmass.content.control.ConsoleBlockEntity.Control;
import com.criticalmass.content.control.ConsoleBlockEntity.Kind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class ConsoleRenderer extends SafeBlockEntityRenderer<ConsoleBlockEntity> {
    private static final float PX = 1 / 16f;
    private static final float TILT = -22.5f;
    private static final int LABEL = 0xD8D4C4;

    public ConsoleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(ConsoleBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Direction facing = be.facing();
        BlockState state = be.getBlockState();
        int lit = be.getLevel() == null ? light : LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above(2).relative(facing));
        boolean flash = be.getLevel() != null && (be.getLevel().getGameTime() / 6) % 2 == 0;
        ms.pushPose();
        ms.translate(0.5, 0, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        ms.translate(-0.5, 0, -0.5);
        for (Control c : Control.values()) {
            double depth = c.v / ConsoleBlockEntity.PANEL_STRETCH * PX;
            ms.pushPose();
            ms.translate(3 - c.across() * PX, ConsoleBlockEntity.PANEL_BASE + depth * ConsoleBlock.SLOPE + 0.002, depth);
            ms.mulPose(Axis.XP.rotationDegrees(TILT));
            control(be, c, state, ms, buffer, lit, flash);
            label(c.label(), ms, buffer, 0, (float) -c.kind.radius - 1.6f, c.kind == Kind.LAMP ? 0.85f : 1.0f, lit);
            ms.popPose();
        }
        for (int column = 0; column < ConsoleBlock.COLUMNS; column++) {
            double depth = 31.5 / ConsoleBlockEntity.PANEL_STRETCH * PX;
            ms.pushPose();
            ms.translate(3 - (column * 16 + 8) * PX, ConsoleBlockEntity.PANEL_BASE + depth * ConsoleBlock.SLOPE + 0.002, depth);
            ms.mulPose(Axis.XP.rotationDegrees(TILT));
            label(Component.translatable("criticalmass.console.section." + column), ms, buffer, 0, 0, 1.3f, lit);
            ms.popPose();
        }
        ms.popPose();
    }

    private void part(PartialModel model, BlockState state, PoseStack ms, VertexConsumer vc, int color, int light) {
        CachedBuffers.partial(model, state).light(light).color(color).renderInto(ms, vc);
    }

    private static int shade(int rgb, float k) {
        int r = (int) (((rgb >> 16) & 255) * k);
        int g = (int) (((rgb >> 8) & 255) * k);
        int b = (int) ((rgb & 255) * k);
        return r << 16 | g << 8 | b;
    }

    private void control(ConsoleBlockEntity be, Control c, BlockState state, PoseStack ms, MultiBufferSource buffer, int light, boolean flash) {
        VertexConsumer vc = buffer.getBuffer(RenderType.solid());
        switch (c.kind) {
            case BUTTON, BIG_BUTTON -> {
                boolean big = c.kind == Kind.BIG_BUTTON;
                part(big ? CMPartialModels.CONSOLE_BIG_BUTTON_COLLAR : CMPartialModels.CONSOLE_BUTTON_COLLAR, state, ms, vc, 0xFFFFFF, light);
                ms.pushPose();
                if (be.pressed(c)) {
                    ms.translate(0, -(big ? 0.8f : 0.6f) * PX, 0);
                }
                part(big ? CMPartialModels.CONSOLE_BIG_BUTTON : CMPartialModels.CONSOLE_BUTTON, state, ms, vc, be.pressed(c) ? shade(c.color, 0.7f) : c.color, light);
                ms.popPose();
            }
            case SWITCH -> {
                part(CMPartialModels.CONSOLE_SWITCH_BASE, state, ms, vc, 0xFFFFFF, light);
                ms.pushPose();
                ms.translate(0, 0.4f * PX, 0);
                ms.mulPose(Axis.XP.rotationDegrees(be.on(c) ? 28 : -28));
                part(CMPartialModels.CONSOLE_SWITCH_LEVER, state, ms, vc, be.on(c) ? 0xF2EEDF : 0xA8A49A, light);
                ms.popPose();
            }
            case DIAL -> {
                boolean on = be.on(c);
                part(CMPartialModels.CONSOLE_DIAL_COLLAR, state, ms, vc, on ? 0x58D94A : 0x15171A, on ? LightTexture.FULL_BRIGHT : light);
                ms.pushPose();
                ms.mulPose(Axis.YP.rotationDegrees(on ? 180 : -90));
                part(CMPartialModels.CONSOLE_DIAL, state, ms, vc, c.color, light);
                part(CMPartialModels.CONSOLE_DIAL_POINTER, state, ms, vc, on ? 0xB4FF9E : 0xE8E4D6, on ? LightTexture.FULL_BRIGHT : light);
                ms.popPose();
            }
            case LAMP -> {
                boolean active = be.alarm(c);
                boolean acknowledged = be.acknowledgedAlarm(c);
                boolean on = active && (acknowledged || flash);
                part(CMPartialModels.CONSOLE_LAMP, state, ms, vc, on ? (acknowledged ? 0xF2B33A : c.color) : shade(c.color, 0.22f), on ? LightTexture.FULL_BRIGHT : light);
            }
        }
    }

    private void label(Component text, PoseStack ms, MultiBufferSource buffer, float across, float along, float height, int light) {
        Font font = Minecraft.getInstance().font;
        int width = font.width(text);
        if (width == 0) {
            return;
        }
        float scale = height / 8f * PX;
        ms.pushPose();
        ms.translate(-across * PX, 0.001, along * PX);
        ms.mulPose(Axis.YP.rotationDegrees(180));
        ms.mulPose(Axis.XP.rotationDegrees(-90));
        ms.scale(scale, -scale, scale);
        font.drawInBatch(text, -width / 2f, -4f, LABEL, false, ms.last().pose(), buffer, Font.DisplayMode.POLYGON_OFFSET, 0, light);
        ms.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(ConsoleBlockEntity be) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ConsoleBlockEntity be) {
        AABB box = new AABB(be.getBlockPos());
        for (BlockPos p : ConsoleBlock.positions(be.getBlockPos(), be.facing())) {
            box = box.minmax(new AABB(p));
        }
        return box.inflate(0.25);
    }
}
