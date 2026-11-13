package com.criticalmass.registry;

import com.criticalmass.CriticalMass;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CMSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, CriticalMass.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM = SOUNDS.register("alarm", () -> SoundEvent.createVariableRangeEvent(CriticalMass.asResource("alarm")));

    private CMSounds() {
    }
}
