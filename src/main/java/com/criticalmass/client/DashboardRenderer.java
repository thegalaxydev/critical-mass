package com.criticalmass.client;

import com.criticalmass.content.control.DashPart;
import com.criticalmass.content.control.DashboardBlock;
import com.criticalmass.content.control.DashboardBlockEntity;
import com.criticalmass.content.control.PartPanelBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

public class DashboardRenderer extends SafeBlockEntityRenderer<DashboardBlockEntity> {
    private static final float PX = 1 / 16f;
    private static final float TILT = -22.5f;
    private static final float BOARD_Y = 14 * PX;
    private static final int FLASH_TICKS = 6;

    public DashboardRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static boolean lit(DashPart part, long gameTime) {
        return part.type == DashPart.Type.ALARM && part.alarming() && (!part.sounding() || gameTime / FLASH_TICKS % 2 == 0);
    }

    @Override
    protected void renderSafe(DashboardBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        long time = be.getLevel() == null ? 0 : be.getLevel().getGameTime();
        ms.pushPose();
        ms.translate(0.5, 0, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(180 - state.getValue(HorizontalDirectionalBlock.FACING).toYRot()));
        ms.translate(-0.5, 0, -0.5);
        if (be.panel()) {
            AttachFace face = state.getValue(PartPanelBlock.FACE);
            if (face == AttachFace.WALL) {
                ms.translate(0, 0, 1);
                ms.mulPose(Axis.XP.rotationDegrees(-90));
            } else if (face == AttachFace.CEILING) {
                ms.translate(0, 1, 1);
                ms.mulPose(Axis.XP.rotationDegrees(180));
            }
            for (int slot = 0; slot < be.slots(); slot++) {
                DashPart part = be.part(slot);
                if (part == null) {
                    continue;
                }
                ms.pushPose();
                ms.translate(PartPanelBlock.cellX(slot, be.centered()), 0, PartPanelBlock.cellZ(slot, be.centered()));
                DashPartDraw.draw(part, be.animation(slot, partialTicks), lit(part, time), ms, buffer, light, state);
                ms.popPose();
            }
            ms.popPose();
            return;
        }
        ms.translate(0, BOARD_Y, 1);
        ms.mulPose(Axis.XP.rotationDegrees(TILT));
        ms.translate(0, -BOARD_Y, -1);
        for (int slot = 0; slot < be.slots(); slot++) {
            DashPart part = be.part(slot);
            if (part == null) {
                continue;
            }
            ms.pushPose();
            ms.translate(slot * 8 * PX, BOARD_Y, (float) DashboardBlock.CELL_FRONT);
            DashPartDraw.draw(part, be.animation(slot, partialTicks), lit(part, time), ms, buffer, light, state);
            ms.popPose();
        }
        ms.popPose();
    }
}
