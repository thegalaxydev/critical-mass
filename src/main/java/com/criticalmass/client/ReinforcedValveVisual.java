package com.criticalmass.client;

import com.criticalmass.content.thermal.ReinforcedValveBlock;
import com.criticalmass.content.thermal.ReinforcedValveBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.ShaftVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.function.Consumer;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public class ReinforcedValveVisual extends ShaftVisual<ReinforcedValveBlockEntity> implements SimpleDynamicVisual {
    private final TransformedInstance pointer;
    private final float yaw;
    private final float pitch;
    private final int offset;
    private boolean settled;

    public ReinforcedValveVisual(VisualizationContext context, ReinforcedValveBlockEntity be, float partialTick) {
        super(context, be, partialTick);
        Direction facing = blockState.getValue(ReinforcedValveBlock.FACING);
        yaw = AngleHelper.horizontalAngle(facing);
        pitch = ReinforcedValveRenderer.pitch(facing);
        offset = ReinforcedValveRenderer.pointerOffset(blockState, KineticBlockEntityRenderer.getRotationAxisOf(be));
        pointer = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CMPartialModels.REINFORCED_VALVE_POINTER)).createInstance();
        transformPointer(partialTick);
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        if (blockEntity.pointer.settled() && settled) {
            return;
        }
        transformPointer(ctx.partialTick());
    }

    private void transformPointer(float partialTick) {
        float value = blockEntity.pointer.getValue(partialTick);
        settled = (value == 0 || value == 1) && blockEntity.pointer.settled();
        pointer.setIdentityTransform()
                .translate(getVisualPosition())
                .center()
                .rotateYDegrees(yaw)
                .rotateXDegrees(pitch)
                .rotateYDegrees(offset + Mth.lerp(value, 0, -90))
                .uncenter()
                .setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        super.updateLight(partialTick);
        relight(pointer);
    }

    @Override
    protected void _delete() {
        super._delete();
        pointer.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        super.collectCrumblingInstances(consumer);
        consumer.accept(pointer);
    }
}
