package com.criticalmass.content.control;

import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;

public final class PlantAlarms {
    public enum Alarm {
        TRIP("trip"), LOW_FLOW("low_flow"), CORE_HOT("core_hot"), LOW_COOLANT("low_coolant"), CASING("casing"), PRESSURE("pressure");

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
        return mask;
    }
}
