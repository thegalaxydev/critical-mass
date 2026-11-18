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
    public static final PartialModel CONTROL_ROD_SHAFT = block("control_rod_shaft");
    public static final PartialModel REINFORCED_VALVE_POINTER = block("reinforced_valve/pointer");
    public static final PartialModel CENTRIFUGE_ROTOR = block("centrifuge_rotor");
    public static final PartialModel CASK_LID = block("shielded_cask_lid");
    public static final PartialModel WATER_CANNON_HEAD = block("water_cannon_head");
    public static final PartialModel CONTROL_ROD_CAP = block("control_rod_cap");
    public static final PartialModel MELTDOWN_SIREN_SPINDLE = block("meltdown_siren_spindle");
    public static final PartialModel SERVICE_PORT_DOOR_WEST = block("reactor_service_port_door_west");
    public static final PartialModel SERVICE_PORT_DOOR_EAST = block("reactor_service_port_door_east");
    public static final Map<String, PartialModel> DASH = pieces("dashboard_part/", "back", "body", "big_body", "slim_body", "plate", "slim_plate", "face", "button_cap", "button_big_cap",
            "lever_handle", "lever_knob", "lever_ball_base", "lever_ball_handle", "lever_ball_knob", "switch_paddle", "needle_default", "needle_classic", "needle_vertical",
            "needle_linear", "alarm_lamp", "alarm_cage", "bulb", "lever_bulb", "switch_bulb", "glass_body", "glass_slim");
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
