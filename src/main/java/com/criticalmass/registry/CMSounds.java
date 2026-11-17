package com.criticalmass.registry;

import com.criticalmass.CriticalMass;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CMSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, CriticalMass.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM = SOUNDS.register("alarm", () -> SoundEvent.createVariableRangeEvent(CriticalMass.asResource("alarm")));

    public static final DeferredHolder<SoundEvent, SoundEvent> HUM = sound("hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> GENERATOR_RUN = sound("generator_run");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOTOR_RUN = sound("motor_run");
    public static final DeferredHolder<SoundEvent, SoundEvent> STEAM_HISS = sound("steam_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> REACTOR_THRUM = sound("reactor_thrum");
    public static final DeferredHolder<SoundEvent, SoundEvent> BREAKER_CLOSE = sound("breaker_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> BREAKER_OPEN = sound("breaker_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> BREAKER_TRIP = sound("breaker_trip");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWITCH_THROW = sound("switch_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> RELAY_CLICK = sound("relay_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARC_ZAP = sound("arc_zap");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARC_BLAST = sound("arc_blast");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIRE_SIZZLE = sound("wire_sizzle");
    public static final DeferredHolder<SoundEvent, SoundEvent> STEAM_VENT = sound("steam_vent");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIPE_BURST = sound("pipe_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER = sound("geiger");
    public static final DeferredHolder<SoundEvent, SoundEvent> ORDER_PLACED = sound("order_placed");
    public static final DeferredHolder<SoundEvent, SoundEvent> ORDER_MET = sound("order_met");
    public static final DeferredHolder<SoundEvent, SoundEvent> ORDER_MISSED = sound("order_missed");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIRE_STRING = sound("wire_string");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIRE_CUT = sound("wire_cut");

    private CMSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(CriticalMass.asResource(name)));
    }
}
