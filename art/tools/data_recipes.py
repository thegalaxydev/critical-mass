import json
import os
import shutil

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
RES = os.path.join(ROOT, "src", "main", "resources")
DATA = os.path.join(RES, "data")
ASSETS = os.path.join(RES, "assets", "criticalmass")
M = "criticalmass:"


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def ing(x):
    if isinstance(x, dict):
        return x
    if x.startswith("#"):
        return {"tag": x[1:]}
    return {"item": x if ":" in x else M + x}


def fluid(fid, amount):
    return {"type": "neoforge:single", "amount": amount, "fluid": fid}


def out(x, count=1, chance=None):
    r = {"id": x if ":" in x else M + x}
    if count != 1:
        r["count"] = count
    if chance is not None:
        r["chance"] = chance
    return r


RECIPES = {}


def processing(name, kind, ingredients, results, heat=None, time=None, conditions=None):
    r = {}
    if conditions:
        r["neoforge:conditions"] = conditions
    r["type"] = "create:" + kind
    if heat:
        r["heat_requirement"] = heat
    r["ingredients"] = [ing(i) for i in ingredients]
    if time:
        r["processing_time"] = time
    r["results"] = results
    RECIPES[kind + "/" + name] = r


def shaped(name, pattern, key, result, count=1, mechanical=False):
    r = {"type": "create:mechanical_crafting" if mechanical else "minecraft:crafting_shaped", "category": "misc",
         "key": {k: ing(v) for k, v in key.items()}, "pattern": pattern, "result": out(result, count)}
    if mechanical:
        r["accept_mirrored"] = False
        r["show_notification"] = False
    RECIPES[("mechanical_crafting/" if mechanical else "crafting/") + name] = r


def shapeless(name, ingredients, result, count=1):
    RECIPES["crafting/" + name] = {"type": "minecraft:crafting_shapeless", "category": "misc",
                                   "ingredients": [ing(i) for i in ingredients], "result": out(result, count)}


def step_deploy(item, part="incomplete_fuel_assembly"):
    return {"type": "create:deploying", "ingredients": [ing(part), ing(item)], "results": [out(part)]}


def step_press(part="incomplete_fuel_assembly"):
    return {"type": "create:pressing", "ingredients": [ing(part)], "results": [out(part)]}


def step_fill(fl, part):
    return {"type": "create:filling", "ingredients": [ing(part), fl], "results": [out(part)]}


OTHER_URANIUM = ["mekanism", "immersiveengineering", "ic2"]
NO_OTHER = [{"type": "neoforge:not", "value": {"type": "neoforge:mod_loaded", "modid": m}} for m in OTHER_URANIUM]

processing("raw_uranium", "milling", ["#c:raw_materials/uranium"], [out("create:crushed_raw_uranium")], time=400)
processing("crushed_raw_uranium", "splashing", ["create:crushed_raw_uranium"], [out("uranium_concentrate"), out("uranium_concentrate", chance=0.25)], conditions=NO_OTHER)
processing("uranium_concentrate", "mixing", ["create:crushed_raw_uranium", fluid("minecraft:water", 250)], [out("uranium_concentrate")])
processing("ceramic_fuel_pellet", "compacting", ["enriched_uranium"], [out("ceramic_fuel_pellet")], heat="heated")
processing("steel_ingot", "mixing", ["#c:ingots/iron", "#minecraft:coals"], [out("steel_ingot")], heat="superheated")
processing("steel_plate", "pressing", ["#c:ingots/steel"], [out("steel_plate")])
processing("reactor_alloy_ingot", "mixing", ["#c:ingots/steel", "#c:ingots/steel", "#c:ingots/zinc", "#c:gems/quartz"], [out("reactor_alloy_ingot", 2)], heat="heated")
processing("reactor_alloy_sheet", "pressing", ["#c:ingots/reactor_alloy"], [out("reactor_alloy_sheet")])
processing("fuel_cladding", "pressing", ["#c:plates/reactor_alloy"], [out("fuel_cladding", 2)])
processing("turbine_blade", "pressing", ["#c:plates/steel"], [out("turbine_blade")])
processing("sealing_gasket", "compacting", ["minecraft:dried_kelp", "minecraft:dried_kelp", "#c:nuggets/iron"], [out("sealing_gasket", 2)])
processing("reinforced_pipe", "item_application", ["create:fluid_pipe", "reactor_alloy_ingot"], [out("reinforced_pipe")])
processing("reinforced_valve", "item_application", ["create:fluid_valve", "reactor_alloy_ingot"], [out("reinforced_valve")])
processing("shielding_concrete", "mixing", ["minecraft:gravel", "minecraft:gravel", "#c:nuggets/iron", "#c:nuggets/iron", "#c:nuggets/iron", fluid("minecraft:water", 250)], [out("shielding_concrete", 2)])
processing("shielding_concrete_from_depleted_uranium", "mixing", ["minecraft:gravel", "minecraft:gravel", "depleted_uranium", fluid("minecraft:water", 250)], [out("shielding_concrete", 4)])
processing("primary_coolant", "mixing", ["minecraft:lapis_lazuli", fluid("minecraft:water", 1000)], [{"amount": 1000, "id": M + "primary_coolant"}])
processing("decontamination_compound", "mixing", ["create:limestone", "minecraft:dried_kelp", fluid("minecraft:water", 250)], [out("decontamination_compound", 2)])

RECIPES["sequenced_assembly/fuel_assembly"] = {
    "type": "create:sequenced_assembly",
    "ingredient": ing("empty_fuel_assembly"),
    "loops": 4,
    "results": [out("fuel_assembly")],
    "sequence": [step_deploy("fuel_cladding"), step_deploy("ceramic_fuel_pellet"), step_deploy("ceramic_fuel_pellet"), step_press()],
    "transitional_item": out("incomplete_fuel_assembly"),
}

RECIPES["sequenced_assembly/sealed_waste_canister"] = {
    "type": "create:sequenced_assembly",
    "ingredient": ing("open_waste_canister"),
    "loops": 1,
    "results": [out("sealed_waste_canister")],
    "sequence": [step_fill(fluid(M + "contaminated_coolant", 1000), "incomplete_waste_canister"), step_deploy("sealing_gasket", "incomplete_waste_canister"), step_press("incomplete_waste_canister")],
    "transitional_item": out("incomplete_waste_canister"),
}

RECIPES["sequenced_assembly/damaged_fuel_canister"] = {
    "type": "create:sequenced_assembly",
    "ingredient": ing("damaged_fuel_assembly"),
    "loops": 1,
    "results": [out("sealed_waste_canister")],
    "sequence": [step_deploy("open_waste_canister", "incomplete_waste_canister"), step_deploy("sealing_gasket", "incomplete_waste_canister"), step_press("incomplete_waste_canister")],
    "transitional_item": out("incomplete_waste_canister"),
}

shaped("open_waste_canister", ["P P", " P "], {"P": "#c:plates/steel"}, "open_waste_canister")
shaped("empty_fuel_assembly", [" S ", "SGS", " S "], {"S": "#c:plates/reactor_alloy", "G": "sealing_gasket"}, "empty_fuel_assembly", mechanical=True)
shaped("steam_turbine", [" C ", "BBB", "BSB", "BBB", " C "], {"C": "create:brass_casing", "B": "turbine_blade", "S": "create:shaft"}, "steam_turbine", mechanical=True)
shaped("centrifuge", ["SAS", "LML", "SCS"], {"S": "#c:plates/steel", "A": "create:shaft", "L": "reactor_alloy_sheet", "M": "create:precision_mechanism", "C": "create:brass_casing"}, "centrifuge", mechanical=True)
shaped("shielded_hatch", ["PCP", "PDP", " C "], {"P": "#c:plates/steel", "C": "shielding_concrete", "D": "minecraft:iron_door"}, "shielded_hatch", mechanical=True)
shaped("shielded_cask", ["PCP", "P P", "PCP"], {"P": "#c:plates/steel", "C": "shielding_concrete"}, "shielded_cask", mechanical=True)
shaped("reactor_vessel_casing", ["SSS", "SBS", "SSS"], {"S": "#c:plates/steel", "B": "create:brass_casing"}, "reactor_vessel_casing", 4, mechanical=True)
shaped("reactor_controller", ["CEC", "EPE", "CEC"], {"C": "create:brass_casing", "E": "create:electron_tube", "P": "create:precision_mechanism"}, "reactor_controller", mechanical=True)
shaped("heat_exchanger", ["CPC", "CTC", "CPC"], {"C": "#c:plates/copper", "P": "create:fluid_pipe", "T": "create:fluid_tank"}, "heat_exchanger", mechanical=True)
shaped("control_rod", ["A", "R", "A"], {"A": "#c:ingots/reactor_alloy", "R": "#c:dusts/redstone"}, "control_rod")
shaped("control_channel", [" R ", "RPR", " R "], {"R": "control_rod", "P": "create:fluid_pipe"}, "control_channel")
shaped("fuel_channel", [" L ", "LPL", " L "], {"L": "fuel_cladding", "P": "create:fluid_pipe"}, "fuel_channel")
shaped("coolant_channel", [" C ", "CPC", " C "], {"C": "#c:plates/copper", "P": "create:fluid_pipe"}, "coolant_channel")
shaped("shielded_glass", [" G ", "GPG", " G "], {"G": "#c:glass_blocks/colorless", "P": "#c:plates/steel"}, "shielded_glass", 4)
shapeless("reactor_viewport", ["reactor_vessel_casing", "shielded_glass"], "reactor_viewport")
shaped("control_rod_drive", ["G", "P", "A"], {"G": "create:gearbox", "P": "create:precision_mechanism", "A": "create:andesite_casing"}, "control_rod_drive")
shaped("condenser", ["CTC", "CFC"], {"C": "#c:plates/copper", "T": "create:fluid_tank", "F": "create:encased_fan"}, "condenser")
shaped("pressurizer", ["SES", "STS", "SBS"], {"S": "#c:plates/steel", "E": "create:electron_tube", "T": "create:fluid_tank", "B": "minecraft:blaze_rod"}, "pressurizer")
shaped("cooling_tower_packing", ["PAP", "APA", "PAP"], {"P": "#minecraft:planks", "A": "create:andesite_alloy"}, "cooling_tower_packing", 8)
shaped("radiator_panel", ["CBC", "CBC", "CBC"], {"C": "#c:plates/copper", "B": "minecraft:iron_bars"}, "radiator_panel", 2)
shaped("boiler_heating_plate", ["CCC", "PRP"], {"C": "#c:plates/copper", "P": "create:fluid_pipe", "R": "#c:rods/blaze"}, "boiler_heating_plate")
shaped("spent_fuel_rack", ["BBB", "BVB", "BBB"], {"B": "minecraft:iron_bars", "V": "reactor_vessel_casing"}, "spent_fuel_rack")
shaped("plant_screen", ["GGG", "PEP"], {"G": "#c:glass_panes/colorless", "E": "create:electron_tube", "P": "#c:plates/steel"}, "plant_screen", 4)
shaped("plant_linker", [" E", "B "], {"E": "create:electron_tube", "B": "#c:plates/brass"}, "plant_linker")
shaped("reactor_alloy_dashboard", [" D ", "ABA"], {"D": "create:display_board", "A": "reactor_alloy_ingot", "B": "create:industrial_iron_block"}, "reactor_alloy_dashboard")
shapeless("reactor_alloy_dashboard_from_dashboard", ["#criticalmass:loconautics_dashboards", "reactor_alloy_ingot"], "reactor_alloy_dashboard")
shapeless("dashboard_button", ["minecraft:stone_button", "#c:plates/brass"], "dashboard_button")
shapeless("dashboard_switch", ["minecraft:lever", "#c:plates/brass"], "dashboard_switch")
shapeless("dashboard_lever", ["create:analog_lever", "#c:plates/brass"], "dashboard_lever")
shapeless("dashboard_gauge", ["create:electron_tube", "#c:glass_panes", "#c:plates/brass"], "dashboard_gauge")
shapeless("dashboard_alarm", ["dashboard_button", "minecraft:redstone_lamp", "minecraft:note_block"], "dashboard_alarm")
shapeless("reactor_service_port", ["reactor_vessel_casing", "create:brass_casing", "minecraft:iron_trapdoor"], "reactor_service_port")
shapeless("primary_coolant_port", ["reactor_vessel_casing", "create:fluid_pipe", "#c:plates/copper"], "primary_coolant_port")
shaped("circulation_pump", [" B ", "BPB", " B "], {"B": "#c:ingots/brass", "P": "reinforced_pipe"}, "circulation_pump")
shapeless("relief_valve", ["create:fluid_valve", "create:fluid_pipe", "#c:plates/steel"], "relief_valve")
shapeless("reactor_signal_interface", ["create:redstone_link", "create:brass_casing", "create:electron_tube"], "reactor_signal_interface")
shapeless("welding_torch", ["#c:plates/steel", "#c:plates/copper", "minecraft:flint_and_steel"], "welding_torch")

for metal in ("steel", "reactor_alloy"):
    shaped(metal + "_block", ["III", "III", "III"], {"I": "#c:ingots/" + metal}, metal + "_block")
    shapeless(metal + "_ingot_from_block", [metal + "_block"], metal + "_ingot", 9)
shapeless("steel_nugget", ["#c:ingots/steel"], "steel_nugget", 9)
shaped("steel_ingot_from_nuggets", ["NNN", "NNN", "NNN"], {"N": "#c:nuggets/steel"}, "steel_ingot")


def tag(kind, name, values):
    ns, path = name.split(":")
    write(os.path.join(DATA, ns, "tags", kind, path + ".json"), {"values": values})


def ores():
    for n, base in [("uranium_ore", "stone"), ("deepslate_uranium_ore", "deepslate")]:
        write(os.path.join(ASSETS, "blockstates", n + ".json"), {"variants": {"": {"model": M + "block/" + n}}})
        write(os.path.join(ASSETS, "models", "block", n + ".json"), {"parent": "minecraft:block/cube_all", "textures": {"all": M + "block/" + n}})
        write(os.path.join(ASSETS, "models", "item", n + ".json"), {"parent": M + "block/" + n})
        write(os.path.join(DATA, "criticalmass", "loot_table", "blocks", n + ".json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": M + n, "conditions": [{"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
                {"type": "minecraft:item", "name": M + "raw_uranium", "functions": [
                    {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                    {"function": "minecraft:explosion_decay"}]}]}]}],
            "random_sequence": "criticalmass:blocks/" + n})
    write(os.path.join(ASSETS, "models", "item", "incomplete_fuel_assembly.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/empty_fuel_assembly"}})
    tag("block", "c:ores/uranium", [M + "uranium_ore", M + "deepslate_uranium_ore"])
    tag("item", "c:ores/uranium", [M + "uranium_ore", M + "deepslate_uranium_ore"])
    tag("block", "c:ores", ["#c:ores/uranium"])
    tag("item", "c:ores", ["#c:ores/uranium"])
    tag("block", "c:ores_in_ground/stone", [M + "uranium_ore"])
    tag("block", "c:ores_in_ground/deepslate", [M + "deepslate_uranium_ore"])
    tag("item", "c:ores_in_ground/stone", [M + "uranium_ore"])
    tag("item", "c:ores_in_ground/deepslate", [M + "deepslate_uranium_ore"])
    pick = os.path.join(DATA, "minecraft", "tags", "block", "mineable", "pickaxe.json")
    values = json.load(open(pick))["values"]
    for n in [M + "uranium_ore", M + "deepslate_uranium_ore", M + "steel_block", M + "reactor_alloy_block"]:
        if n not in values:
            values.append(n)
    write(pick, {"values": values})
    tag("block", "minecraft:needs_iron_tool", [M + "uranium_ore", M + "deepslate_uranium_ore", M + "steel_block", M + "reactor_alloy_block"])
    tag("item", "c:raw_materials/uranium", [M + "raw_uranium"])
    tag("item", "c:raw_materials", ["#c:raw_materials/uranium"])
    tag("item", "c:ingots/reactor_alloy", [M + "reactor_alloy_ingot"])
    tag("item", "c:ingots/steel", [M + "steel_ingot"])
    tag("item", "c:ingots", ["#c:ingots/reactor_alloy", "#c:ingots/steel"])
    tag("item", "c:nuggets/steel", [M + "steel_nugget"])
    tag("item", "c:nuggets", ["#c:nuggets/steel"])
    tag("item", "c:plates/reactor_alloy", [M + "reactor_alloy_sheet"])
    tag("item", "c:plates/steel", [M + "steel_plate"])
    tag("item", "c:plates", ["#c:plates/reactor_alloy", "#c:plates/steel"])
    storage_blocks()
    tag("item", "criticalmass:fuel_assemblies", [M + "fuel_assembly", M + "spent_fuel_assembly", M + "damaged_fuel_assembly"])
    wg = os.path.join(DATA, "criticalmass", "worldgen")
    write(os.path.join(wg, "configured_feature", "uranium_ore.json"), {"type": "minecraft:ore", "config": {"size": 7, "discard_chance_on_air_exposure": 0.3, "targets": [
        {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}, "state": {"Name": M + "uranium_ore"}},
        {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}, "state": {"Name": M + "deepslate_uranium_ore"}}]}})
    write(os.path.join(wg, "placed_feature", "uranium_ore.json"), {"feature": M + "uranium_ore", "placement": [
        {"type": "minecraft:count", "count": 4},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:trapezoid", "min_inclusive": {"above_bottom": 0}, "max_inclusive": {"absolute": 48}}},
        {"type": "minecraft:biome"}]})
    write(os.path.join(DATA, "criticalmass", "neoforge", "biome_modifier", "uranium_ore.json"), {"type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
                                                                                              "features": M + "uranium_ore", "step": "underground_ores"})


def storage_blocks():
    for metal in ("steel", "reactor_alloy"):
        n = metal + "_block"
        write(os.path.join(ASSETS, "blockstates", n + ".json"), {"variants": {"": {"model": M + "block/" + n}}})
        write(os.path.join(ASSETS, "models", "block", n + ".json"), {"parent": "minecraft:block/cube_all", "textures": {"all": M + "block/" + n}})
        write(os.path.join(ASSETS, "models", "item", n + ".json"), {"parent": M + "block/" + n})
        write(os.path.join(DATA, "criticalmass", "loot_table", "blocks", n + ".json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": M + n}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": "criticalmass:blocks/" + n})
        tag("block", "c:storage_blocks/" + metal, [M + n])
        tag("item", "c:storage_blocks/" + metal, [M + n])
    for kind in ("block", "item"):
        tag(kind, "c:storage_blocks", ["#c:storage_blocks/steel", "#c:storage_blocks/reactor_alloy"])
    tag("block", "minecraft:beacon_base_blocks", [M + "steel_block", M + "reactor_alloy_block"])
    tag("item", "minecraft:beacon_payment_items", [M + "steel_ingot", M + "reactor_alloy_ingot"])
    for n in ("steel_ingot", "steel_nugget", "steel_plate"):
        write(os.path.join(ASSETS, "models", "item", n + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/" + n}})


def unknown(target):
    found = []
    for folder, _, files in os.walk(target):
        for name in files:
            key = os.path.relpath(os.path.join(folder, name), target).replace(os.sep, "/")[:-len(".json")]
            if name.endswith(".json") and key not in RECIPES:
                found.append(key)
    return sorted(found)


def main():
    import sys
    target = os.path.join(DATA, "criticalmass", "recipe")
    extra = unknown(target) if os.path.isdir(target) else []
    if extra and "wipe" not in sys.argv:
        print("left alone: the recipe folder holds %d recipes this tool does not know, written by hand since. Running it would delete them." % len(extra))
        print("the first of them:", ", ".join(extra[:8]))
        print("pass wipe only if losing them is meant")
        return
    if os.path.isdir(target):
        shutil.rmtree(target)
    for path, r in RECIPES.items():
        write(os.path.join(target, path + ".json"), r)
    ores()
    print(len(RECIPES), "recipes")


if __name__ == "__main__":
    main()
