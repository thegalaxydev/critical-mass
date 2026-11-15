package com.criticalmass.client;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.function.Function;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class AxialRotorRenderer<T extends KineticBlockEntity> extends KineticBlockEntityRenderer<T> {
    private final PartialModel rotor;
    private final double reach;
    private final Function<BlockState, Direction> upEnd;

    public AxialRotorRenderer(BlockEntityRendererProvider.Context context, PartialModel rotor) {
        this(context, rotor, 0, null);
    }

    public AxialRotorRenderer(BlockEntityRendererProvider.Context context, PartialModel rotor, double reach, Function<BlockState, Direction> upEnd) {
        super(context);
        this.rotor = rotor;
        this.reach = reach;
        this.upEnd = upEnd;
    }

    @Override
    protected SuperByteBuffer getRotatedModel(T be, BlockState state) {
        Direction.Axis axis = getRotationAxisOf(be);
        return CachedBuffers.partialFacingVertical(rotor, state, upEnd != null ? upEnd.apply(state) : Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE));
    }

    @Override
    public AABB getRenderBoundingBox(T be) {
        return new AABB(be.getBlockPos()).inflate(reach);
    }
}
