package com.criticalmass.client;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class AxialRotorRenderer<T extends KineticBlockEntity> extends KineticBlockEntityRenderer<T> {
    private final PartialModel rotor;

    public AxialRotorRenderer(BlockEntityRendererProvider.Context context, PartialModel rotor) {
        super(context);
        this.rotor = rotor;
    }

    @Override
    protected SuperByteBuffer getRotatedModel(T be, BlockState state) {
        Direction.Axis axis = getRotationAxisOf(be);
        return CachedBuffers.partialFacingVertical(rotor, state, Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE));
    }
}
