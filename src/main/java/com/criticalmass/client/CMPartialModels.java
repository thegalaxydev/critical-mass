package com.criticalmass.client;

import com.criticalmass.CriticalMass;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Direction;

public final class CMPartialModels {
    public static final PartialModel CIRCULATION_PUMP_ROTOR = block("circulation_pump_rotor");
    public static final PartialModel STEAM_TURBINE_ROTOR = block("steam_turbine_rotor");
    public static final PartialModel CONTROL_ROD_DRIVE_PINION = block("control_rod_drive_pinion");
    public static final PartialModel CONTROL_ROD_SHAFT = block("control_rod_shaft");
    public static final PartialModel REINFORCED_VALVE_POINTER = block("reinforced_valve/pointer");
    public static final PartialModel CENTRIFUGE_ROTOR = block("centrifuge_rotor");
    public static final PartialModel CONTROL_ROD_CAP = block("control_rod_cap");
    public static final PartialModel PANEL_BUTTON = block("panel/button");
    public static final PartialModel PANEL_BUTTON_COLLAR = block("panel/button_collar");
    public static final PartialModel PANEL_BIG_BUTTON = block("panel/big_button");
    public static final PartialModel PANEL_BIG_BUTTON_COLLAR = block("panel/big_button_collar");
    public static final PartialModel PANEL_SWITCH_BASE = block("panel/switch_base");
    public static final PartialModel PANEL_SWITCH_LEVER = block("panel/switch_lever");
    public static final PartialModel PANEL_DIAL = block("panel/dial");
    public static final PartialModel PANEL_DIAL_POINTER = block("panel/dial_pointer");
    public static final PartialModel PANEL_DIAL_COLLAR = block("panel/dial_collar");
    public static final PartialModel PANEL_LAMP = block("panel/lamp");
    public static final Map<String, PartialModel> DASHBOARD = pieces("dashboard/", "button_base", "button_cap", "switch_base", "switch_paddle", "lever_base", "lever_handle",
            "gauge_body", "gauge_needle", "alarm_base", "alarm_lamp", "alarm_lamp_lit");
    public static final Map<Direction, PartialModel> REINFORCED_PIPE_RIM = facings("reinforced_pipe/rim/");
    public static final Map<Direction, PartialModel> REINFORCED_PIPE_DRAIN = facings("reinforced_pipe/drain/");

    private CMPartialModels() {
    }

    private static PartialModel block(String path) {
        return PartialModel.of(CriticalMass.asResource("block/" + path));
    }

    private static Map<String, PartialModel> pieces(String path, String... names) {
        Map<String, PartialModel> map = new HashMap<>();
        for (String name : names) {
            map.put(name, block(path + name));
        }
        return map;
    }

    private static Map<Direction, PartialModel> facings(String path) {
        Map<Direction, PartialModel> map = new EnumMap<>(Direction.class);
        for (Direction d : Direction.values()) {
            map.put(d, block(path + d.getSerializedName()));
        }
        return map;
    }

    public static void init() {
    }
}
