package com.criticalmass.content.control;

import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;

public final class PlantAlarms {
    public enum Alarm {
        TRIP("trip"), LOW_FLOW("low_flow"), CORE_HOT("core_hot"), LOW_COOLANT("low_coolant"), CASING("casing"), PRESSURE("pressure"), EQUIPMENT("equipment"), BYPASS("bypass");

        public final String key;

        Alarm(String key) {
            this.key = key;
        }

        public String langKey() {
            return "criticalmass.alarm." + key;
        }
    }

    private PlantAlarms() {
    }

    public static boolean watchable(@org.jetbrains.annotations.Nullable net.minecraft.world.level.block.entity.BlockEntity be) {
        return be instanceof ReactorControllerBlockEntity || be instanceof com.criticalmass.content.power.BreakerBlockEntity || be instanceof com.criticalmass.content.power.GridTieBlockEntity || be instanceof com.criticalmass.content.power.TransformerBlockEntity
                || be instanceof com.criticalmass.content.power.GeneratorBlockEntity || be instanceof com.criticalmass.content.power.BatteryBlockEntity || be instanceof com.criticalmass.content.power.DieselEngineBlockEntity || be instanceof com.criticalmass.content.power.TransferSwitchBlockEntity
                || be instanceof com.criticalmass.content.power.MotorBlockEntity || be instanceof com.criticalmass.content.power.BusBarBlockEntity || be instanceof com.criticalmass.content.power.BurntBlockEntity
                || be instanceof com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
    }

    public static boolean trouble(@org.jetbrains.annotations.Nullable net.minecraft.world.level.block.entity.BlockEntity be) {
        if (be instanceof com.criticalmass.content.power.BurntBlockEntity) {
            return true;
        }
        if (be instanceof com.criticalmass.content.power.BreakerBlockEntity breaker) {
            return breaker.locked() || breaker.wear() > 0.75;
        }
        if (be instanceof com.criticalmass.content.power.GridTieBlockEntity tie) {
            return tie.event() != com.criticalmass.content.power.GridTieBlockEntity.Event.NORMAL;
        }
        if (be instanceof com.criticalmass.content.power.TransformerBlockEntity transformer) {
            return transformer.heat() > 0.5 || transformer.wear() > 0.75;
        }
        if (be instanceof com.criticalmass.content.power.GeneratorBlockEntity part) {
            String status = part.master().status();
            return status.endsWith("generator.fault") || status.endsWith("generator.overloaded");
        }
        if (be instanceof com.criticalmass.content.power.BatteryBlockEntity battery) {
            return battery.online() || battery.status().endsWith("battery.flat") || battery.status().endsWith("battery.overloaded");
        }
        if (be instanceof com.criticalmass.content.power.DieselEngineBlockEntity engine) {
            return engine.status().endsWith("engine.no_fuel");
        }
        if (be instanceof com.criticalmass.content.power.TransferSwitchBlockEntity transfer) {
            return transfer.position() != com.criticalmass.content.power.TransferSwitchBlock.Position.NORMAL;
        }
        if (be instanceof com.criticalmass.content.power.MotorBlockEntity motor) {
            return motor.status().endsWith("motor.voltage") || motor.status().endsWith("motor.frequency") || motor.wear() > 0.75;
        }
        if (be instanceof com.criticalmass.content.power.BusBarBlockEntity bus) {
            return !bus.live();
        }
        if (be instanceof com.criticalmass.content.kinetics.CirculationPumpBlockEntity pump) {
            return pump.cavitating() || pump.flow() == 0;
        }
        return false;
    }

    public static int of(@org.jetbrains.annotations.Nullable net.minecraft.world.level.block.entity.BlockEntity be) {
        if (be instanceof ReactorControllerBlockEntity c) {
            return evaluate(c);
        }
        return trouble(be) ? 1 << Alarm.EQUIPMENT.ordinal() : 0;
    }

    public static int evaluate(ReactorControllerBlockEntity c) {
        int mask = 0;
        boolean hot = c.isReacting() || c.core().residualPower() > CMThermal.RESIDUAL_COLD_LIMIT;
        if (c.isTripped()) {
            mask |= 1 << Alarm.TRIP.ordinal();
        }
        if (hot && c.effectiveFlow() == 0) {
            mask |= 1 << Alarm.LOW_FLOW.ordinal();
        }
        if (c.core().maxTemperature() >= CMThermal.HOT_SPOT_TEMPERATURE) {
            mask |= 1 << Alarm.CORE_HOT.ordinal();
        }
        if (c.coolant().amount() < c.coolant().capacity() * CMThermal.LOW_COOLANT_ALARM) {
            mask |= 1 << Alarm.LOW_COOLANT.ordinal();
        }
        if (c.isDamaged() || c.isBreached()) {
            mask |= 1 << Alarm.CASING.ordinal();
        }
        if (c.flashing()) {
            mask |= 1 << Alarm.PRESSURE.ordinal();
        }
        if (c.bypassed()) {
            mask |= 1 << Alarm.BYPASS.ordinal();
        }
        return mask;
    }
}
