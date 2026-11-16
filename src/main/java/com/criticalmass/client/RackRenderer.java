package com.criticalmass.client;

import com.criticalmass.content.fuel.SpentFuelRackBlockEntity;
import com.criticalmass.content.thermal.CoolantBuffer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;

public class RackRenderer extends SafeBlockEntityRenderer<SpentFuelRackBlockEntity> {
    private static final float SIDE = 2.01f / 16;
    private static final float FLOOR = 2.02f / 16;
    private static final float DEPTH = 11.5f / 16;

    public RackRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(SpentFuelRackBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        CoolantBuffer coolant = be.coolant();
        if (coolant.amount() <= 0 || coolant.capacity() <= 0) {
            return;
        }
        float level = Mth.clamp(coolant.amount() / (float) coolant.capacity(), 0, 1);
        NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(coolant.fluid(), SIDE, FLOOR, SIDE, 1 - SIDE, FLOOR + DEPTH * level, 1 - SIDE, buffer, ms, light, false, false);
    }
}
