package com.criticalmass.client;

import com.criticalmass.CriticalMass;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

public final class CMPartialModels {
    public static final PartialModel CIRCULATION_PUMP_ROTOR = block("circulation_pump_rotor");
    public static final PartialModel STEAM_TURBINE_ROTOR = block("steam_turbine_rotor");
    public static final PartialModel CONTROL_ROD_DRIVE_PINION = block("control_rod_drive_pinion");
    public static final PartialModel REINFORCED_VALVE_POINTER = block("reinforced_valve/pointer");
    public static final PartialModel CONTROL_ROD_SHAFT = block("control_rod_shaft");
    public static final PartialModel TOWER_FAN_ROTOR = block("tower_fan_rotor");
    public static final PartialModel CONTROL_ROD_CAP = block("control_rod_cap");
    public static final PartialModel CONSOLE_BUTTON = block("console/button");
    public static final PartialModel CONSOLE_BUTTON_COLLAR = block("console/button_collar");
    public static final PartialModel CONSOLE_BIG_BUTTON = block("console/big_button");
    public static final PartialModel CONSOLE_BIG_BUTTON_COLLAR = block("console/big_button_collar");
    public static final PartialModel CONSOLE_SWITCH_BASE = block("console/switch_base");
    public static final PartialModel CONSOLE_SWITCH_LEVER = block("console/switch_lever");
    public static final PartialModel CONSOLE_DIAL = block("console/dial");
    public static final PartialModel CONSOLE_DIAL_POINTER = block("console/dial_pointer");
    public static final PartialModel CONSOLE_DIAL_COLLAR = block("console/dial_collar");
    public static final PartialModel CONSOLE_LAMP = block("console/lamp");

    private CMPartialModels() {
    }

    private static PartialModel block(String path) {
        return PartialModel.of(CriticalMass.asResource("block/" + path));
    }

    public static void init() {
    }
}
