package com.criticalmass.client;

import com.criticalmass.content.thermal.CoolingTowerBlock;
import com.criticalmass.content.thermal.CoolingTowerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

public class PackingRenderer extends SafeBlockEntityRenderer<CoolingTowerBlockEntity> {
    private static final float WALL = 2 / 16f + 1 / 128f;
    private static final float CAP = 2 / 16f;

    public PackingRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(CoolingTowerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState shaft = AllBlocks.SHAFT.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y);
        SuperByteBuffer turning = CachedBuffers.block(shaft);
        KineticBlockEntityRenderer.standardKineticRotationTransform(turning, be, light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
        int height = be.columnHeight();
        float level = be.columnLevel(partialTicks);
        FluidStack fluid = be.columnFluid();
        if (height <= 0 || level <= 0.005f || fluid.isEmpty()) {
            return;
        }
        CoolingTowerBlock.Shape shape = be.getBlockState().getValue(CoolingTowerBlock.SHAPE);
        float floor = shape.openBottom() ? 0 : CAP;
        float ceiling = height - CAP;
        float surface = floor + (ceiling - floor) * Math.min(1, level);
        NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid, WALL, floor, WALL, 1 - WALL, surface, 1 - WALL, buffer, ms, light, false, false);
    }

    @Override
    public AABB getRenderBoundingBox(CoolingTowerBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0, Math.max(0, be.columnHeight() - 1), 0);
    }
}
