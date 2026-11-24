package com.criticalmass.registry;

import com.criticalmass.CriticalMass;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public enum TabSection {
    REACTOR("reactor", 0xFFFFE9B0, 24, 2,
            "reactor_vessel_casing", "reactor_controller", "reactor_viewport", "fuel_channel", "control_channel", "coolant_channel", "control_rod_drive", "control_rod",
            "reactor_service_port", "fuel_channel_cap", "primary_coolant_port", "reinforced_pipe", "reinforced_valve", "circulation_pump", "heat_exchanger", "pressurizer", "borax", "boric_acid_bucket", "primary_coolant_bucket", "contaminated_coolant_bucket", "demineralizer", "resin_cartridge", "used_resin_cartridge", "steam_turbine", "turbine_blade",
            "condenser", "relief_valve", "cooling_tower_packing", "radiator_panel", "boiler_heating_plate", "shielding_concrete", "shielded_glass", "shielded_hatch"),
    EQUIPMENT("equipment", 0xFFFFDFA6, 24, 2,
            "uranium_ore", "deepslate_uranium_ore", "raw_uranium", "uranium_concentrate", "centrifuge", "enriched_uranium", "depleted_uranium", "ceramic_fuel_pellet", "fuel_cladding",
            "sealing_gasket", "empty_fuel_assembly", "incomplete_fuel_assembly", "fuel_assembly", "spent_fuel_assembly", "damaged_fuel_assembly", "spent_fuel_rack", "shielded_cask",
            "sheared_fuel", "incomplete_sheared_fuel", "open_waste_canister", "incomplete_waste_canister", "sealed_waste_canister", "steel_ingot", "steel_nugget", "steel_plate", "steel_block",
            "reactor_alloy_ingot", "reactor_alloy_sheet", "reactor_alloy_block", "welding_torch"),
    CONSOLE("console", 0xFFB6FFC8, 24, 2,
            "reactor_alloy_dashboard", "dashboard_button", "dashboard_switch", "dashboard_lever", "dashboard_gauge", "dashboard_alarm", "plant_screen", "plant_linker",
            "reactor_signal_interface", "meltdown_siren"),
    FALLOUT("fallout", 0xFFD8FF9A, 48, 3,
            "survey_meter", "dosimeter", "area_radiation_monitor", "protective_hood", "protective_suit", "protective_leggings", "protective_boots", "decontamination_compound", "water_cannon", "fallout_ash",
            "strontium_90", "corium", "ruined_channel", "core_debris", "wreckage", "irradiated_creeper_spawn_egg"),
    ELECTRICITY("electricity", 0xFFBFE6FF, 16, 2,
            "generator", "power_wire", "power_cable", "transmission_line", "insulator", "bus_bar", "circuit_breaker", "disconnect_switch", "earthing_switch", "transformer",
            "grid_connection", "electric_motor", "station_battery", "radioisotope_cell", "diesel_engine", "fuel_oil_bucket", "transfer_switch", "surge_arrester"),
    BUILDING("building", 0xFFE4EE9A, 24, 2,
            "chartrine", "cut_chartrine", "cut_chartrine_stairs", "cut_chartrine_slab", "cut_chartrine_wall",
            "polished_cut_chartrine", "polished_cut_chartrine_stairs", "polished_cut_chartrine_slab", "polished_cut_chartrine_wall",
            "cut_chartrine_bricks", "cut_chartrine_brick_stairs", "cut_chartrine_brick_slab", "cut_chartrine_brick_wall",
            "small_chartrine_bricks", "small_chartrine_brick_stairs", "small_chartrine_brick_slab", "small_chartrine_brick_wall",
            "layered_chartrine", "chartrine_pillar",
            "charred_log", "charred_wood", "stripped_charred_log", "stripped_charred_wood", "charred_planks", "charred_stairs", "charred_slab", "charred_fence", "charred_fence_gate",
            "charred_door", "charred_trapdoor", "charred_pressure_plate", "charred_button", "charred_sign", "charred_hanging_sign");

    public static final int WIDTH = 162;
    public static final int HEIGHT = 18;
    private static final Map<String, TabSection> BY_PATH = new HashMap<>();
    public final String key;
    public final int titleColour;
    public final int frames;
    public final int frameTicks;
    private final List<String> paths;

    static {
        for (TabSection section : values()) {
            for (String path : section.paths) {
                BY_PATH.put(path, section);
            }
        }
    }

    TabSection(String key, int titleColour, int frames, int frameTicks, String... paths) {
        this.key = key;
        this.titleColour = titleColour;
        this.frames = frames;
        this.frameTicks = frameTicks;
        this.paths = List.of(paths);
    }

    public String langKey() {
        return "criticalmass.section." + key;
    }

    public ResourceLocation texture() {
        return CriticalMass.asResource("textures/gui/banner/" + key + ".png");
    }

    public static TabSection of(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id.getNamespace().equals(CriticalMass.MODID) ? BY_PATH.getOrDefault(id.getPath(), EQUIPMENT) : EQUIPMENT;
    }

    public static List<ItemStack> ordered(List<? extends Item> items) {
        List<ItemStack> out = new ArrayList<>();
        for (TabSection section : values()) {
            for (String path : section.paths) {
                for (Item item : items) {
                    if (BuiltInRegistries.ITEM.getKey(item).getPath().equals(path)) {
                        out.add(new ItemStack(item));
                    }
                }
            }
            if (section == EQUIPMENT) {
                for (Item item : items) {
                    if (!BY_PATH.containsKey(BuiltInRegistries.ITEM.getKey(item).getPath())) {
                        out.add(new ItemStack(item));
                    }
                }
            }
        }
        return out;
    }
}
