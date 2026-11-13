package com.criticalmass.mixin;

import com.criticalmass.client.AlarmPartVisual;
import com.criticalmass.content.control.AlarmPart;
import com.lycoris.loconautics.content.controls.panel.PanelPart;
import com.lycoris.loconautics.content.controls.panel.visual.PanelPartVisual;
import com.lycoris.loconautics.content.controls.panel.visual.PanelVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PanelVisual.class, remap = false)
public abstract class PanelVisualMixin {
    @Shadow
    @Final
    private VisualizationContext context;

    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private void criticalmass$alarmVisual(PanelPart part, CallbackInfoReturnable<PanelPartVisual> cir) {
        if (part instanceof AlarmPart) {
            cir.setReturnValue(new AlarmPartVisual(context));
        }
    }
}
