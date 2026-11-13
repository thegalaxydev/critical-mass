package com.criticalmass.mixin;

import com.criticalmass.content.control.PlantGauges;
import com.lycoris.loconautics.content.controls.panelgauge.GaugeMetric;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GaugeMetric.class, remap = false)
public abstract class GaugeMetricMixin {
    @Invoker("<init>")
    private static GaugeMetric criticalmass$create(String name, int ordinal, GaugeMetric.Group group, Function<BlockEntity, String> reader, Function<BlockEntity, Float> normalizer,
                                                   ChatFormatting color, int defaultBackground) {
        throw new AssertionError();
    }

    @Inject(method = "$values", at = @At("RETURN"), cancellable = true)
    private static void criticalmass$plantMetrics(CallbackInfoReturnable<GaugeMetric[]> cir) {
        List<GaugeMetric> all = new ArrayList<>(Arrays.asList(cir.getReturnValue()));
        for (PlantGauges.GroupSpec spec : PlantGauges.groups()) {
            GaugeMetric.Group group = PlantGauges.created(spec);
            for (PlantGauges.MetricSpec metric : spec.metrics()) {
                all.add(criticalmass$create(metric.name(), all.size(), group, be -> PlantGauges.text(be, metric), be -> PlantGauges.fraction(be, metric), metric.color(), metric.face()));
            }
        }
        cir.setReturnValue(all.toArray(new GaugeMetric[0]));
    }
}
