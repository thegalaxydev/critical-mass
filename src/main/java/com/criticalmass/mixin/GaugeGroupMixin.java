package com.criticalmass.mixin;

import com.criticalmass.content.control.PlantGauges;
import com.lycoris.loconautics.content.controls.panelgauge.GaugeMetric;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GaugeMetric.Group.class, remap = false)
public abstract class GaugeGroupMixin {
    @Invoker("<init>")
    private static GaugeMetric.Group criticalmass$create(String name, int ordinal, String modId, String modName, String blockName, Supplier<List<Class<? extends BlockEntity>>> sources) {
        throw new AssertionError();
    }

    @Inject(method = "$values", at = @At("RETURN"), cancellable = true)
    private static void criticalmass$plantGroups(CallbackInfoReturnable<GaugeMetric.Group[]> cir) {
        GaugeMetric.Group[] base = cir.getReturnValue();
        List<PlantGauges.GroupSpec> specs = PlantGauges.groups();
        GaugeMetric.Group[] all = Arrays.copyOf(base, base.length + specs.size());
        for (int i = 0; i < specs.size(); i++) {
            PlantGauges.GroupSpec spec = specs.get(i);
            GaugeMetric.Group group = criticalmass$create(spec.name(), base.length + i, PlantGauges.MOD_ID, PlantGauges.MOD_NAME, spec.blockName(), spec::sources);
            PlantGauges.created(spec, group);
            all[base.length + i] = group;
        }
        cir.setReturnValue(all);
    }
}
