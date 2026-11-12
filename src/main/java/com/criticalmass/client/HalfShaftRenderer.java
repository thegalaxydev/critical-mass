package com.criticalmass.client;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import java.util.function.Function;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class HalfShaftRenderer<T extends KineticBlockEntity> extends KineticBlockEntityRenderer<T> {
    private final Function<BlockState, Direction> side;

    public HalfShaftRenderer(BlockEntityRendererProvider.Context context, Function<BlockState, Direction> side) {
        super(context);
        this.side = side;
    }

    @Override
    protected SuperByteBuffer getRotatedModel(T be, BlockState state) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, side.apply(state));
    }
}
