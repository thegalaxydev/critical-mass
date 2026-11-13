package com.criticalmass.content.control;

import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.kinetics.ControlRodDriveBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.PressurizerBlockEntity;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.ThermalNode;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.lycoris.loconautics.content.controls.panelgauge.GaugeMetric;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class PlantGauges {
    public static final String MOD_ID = "criticalmass";
    public static final String MOD_NAME = "Create: Critical Mass";
    private static final Map<String, GaugeMetric.Group> CREATED = new HashMap<>();
    private static final List<GroupSpec> GROUPS = List.of(
            group("CM_REACTOR", "Reactor Controller", ReactorControllerBlockEntity.class, PlantChannel.TEMPERATURE, PlantChannel.COOLANT_TEMPERATURE, PlantChannel.PRESSURE,
                    PlantChannel.LEVEL, PlantChannel.FLOW, PlantChannel.POWER, PlantChannel.NEUTRON, PlantChannel.PERIOD, PlantChannel.REACTIVITY, PlantChannel.XENON, PlantChannel.BORON,
                    PlantChannel.RODS_A, PlantChannel.RODS_B, PlantChannel.RODS_C, PlantChannel.RODS_D),
            group("CM_PRESSURIZER", "Pressurizer", PressurizerBlockEntity.class, PlantChannel.PRESSURE, PlantChannel.TEMPERATURE),
            group("CM_TURBINE", "Steam Turbine", SteamTurbineBlockEntity.class, PlantChannel.SPEED, PlantChannel.POWER, PlantChannel.OUTPUT, PlantChannel.FLOW,
                    PlantChannel.TEMPERATURE, PlantChannel.PRESSURE, PlantChannel.LEVEL),
            group("CM_PUMP", "Circulation Pump", CirculationPumpBlockEntity.class, PlantChannel.FLOW, PlantChannel.SPEED),
            group("CM_ROD_DRIVE", "Control Rod Drive", ControlRodDriveBlockEntity.class, PlantChannel.RODS_A),
            group("CM_LOOP", "Coolant loop machines", ThermalNode.class, PlantChannel.TEMPERATURE, PlantChannel.PRESSURE, PlantChannel.LEVEL));

    private PlantGauges() {
    }

    public record MetricSpec(String name, PlantChannel channel, ChatFormatting color, int face) {
    }

    public record GroupSpec(String name, String blockName, Class<?> source, List<MetricSpec> metrics) {
        @SuppressWarnings("unchecked")
        public List<Class<? extends BlockEntity>> sources() {
            return List.of((Class<? extends BlockEntity>) source);
        }
    }

    private static GroupSpec group(String name, String blockName, Class<?> source, PlantChannel... channels) {
        List<MetricSpec> metrics = new ArrayList<>();
        for (PlantChannel channel : channels) {
            metrics.add(new MetricSpec(name + "_" + channel.name(), channel, color(channel), face(channel)));
        }
        return new GroupSpec(name, blockName, source, List.copyOf(metrics));
    }

    private static ChatFormatting color(PlantChannel channel) {
        return switch (channel) {
            case TEMPERATURE, COOLANT_TEMPERATURE, POWER -> ChatFormatting.GOLD;
            case LEVEL, BORON -> ChatFormatting.BLUE;
            case NEUTRON, REACTIVITY -> ChatFormatting.GREEN;
            case XENON -> ChatFormatting.LIGHT_PURPLE;
            case PERIOD, RODS_A, RODS_B, RODS_C, RODS_D -> ChatFormatting.WHITE;
            default -> ChatFormatting.AQUA;
        };
    }

    private static int face(PlantChannel channel) {
        return switch (channel) {
            case TEMPERATURE, COOLANT_TEMPERATURE -> 27;
            case PRESSURE -> 21;
            case LEVEL -> 2;
            case FLOW, OUTPUT -> 15;
            case POWER -> 22;
            case RODS_A, RODS_B, RODS_C, RODS_D -> 32;
            case SPEED -> 9;
            case NEUTRON -> 23;
            case PERIOD -> 33;
            case REACTIVITY -> 28;
            case XENON -> 17;
            case BORON -> 14;
        };
    }

    public static List<GroupSpec> groups() {
        return GROUPS;
    }

    public static void created(GroupSpec spec, GaugeMetric.Group group) {
        CREATED.put(spec.name(), group);
    }

    public static GaugeMetric.Group created(GroupSpec spec) {
        return CREATED.get(spec.name());
    }

    @Nullable
    private static PlantReadings.Entry entry(BlockEntity be) {
        Level level = be.getLevel();
        return level == null ? null : PlantReadings.of(level, be.getBlockPos());
    }

    @Nullable
    public static String text(BlockEntity be, MetricSpec metric) {
        PlantReadings.Entry entry = entry(be);
        if (entry == null) {
            return null;
        }
        PlantChannel.Reading r = entry.reading(metric.channel());
        return r == null ? null : ThermalTooltips.number(r.value()) + (r.unit().isEmpty() ? "" : " " + r.unit());
    }

    @Nullable
    public static Float fraction(BlockEntity be, MetricSpec metric) {
        PlantReadings.Entry entry = entry(be);
        if (entry == null) {
            return null;
        }
        PlantChannel.Reading r = entry.reading(metric.channel());
        return r == null ? null : (float) r.fraction();
    }
}
