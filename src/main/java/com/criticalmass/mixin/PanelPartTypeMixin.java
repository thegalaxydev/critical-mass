package com.criticalmass.mixin;

import com.criticalmass.content.control.AlarmPart;
import com.lycoris.loconautics.content.controls.panel.PanelPart;
import com.lycoris.loconautics.content.controls.panel.PanelPartType;
import java.util.Arrays;
import java.util.function.Function;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PanelPartType.class, remap = false)
public abstract class PanelPartTypeMixin {
    @Invoker("<init>")
    private static PanelPartType criticalmass$create(String name, int ordinal, Function<CompoundTag, PanelPart> reader, double[] slotBox) {
        throw new AssertionError();
    }

    @Inject(method = "$values", at = @At("RETURN"), cancellable = true)
    private static void criticalmass$alarmPart(CallbackInfoReturnable<PanelPartType[]> cir) {
        PanelPartType[] base = cir.getReturnValue();
        PanelPartType[] all = Arrays.copyOf(base, base.length + 1);
        all[base.length] = criticalmass$create(AlarmPart.TYPE_NAME, base.length, AlarmPart::read, AlarmPart.SLOT_BOX);
        AlarmPart.registered(all[base.length]);
        cir.setReturnValue(all);
    }
}
