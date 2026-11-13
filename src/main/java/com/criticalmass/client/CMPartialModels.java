package com.criticalmass.client;

import com.criticalmass.CriticalMass;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.EnumMap;
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
    public static final Map<Direction, PartialModel> REINFORCED_PIPE_RIM = facings("reinforced_pipe/rim/");
    public static final Map<Direction, PartialModel> REINFORCED_PIPE_DRAIN = facings("reinforced_pipe/drain/");

    private CMPartialModels() {
    }

    private static PartialModel block(String path) {
        return PartialModel.of(CriticalMass.asResource("block/" + path));
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
