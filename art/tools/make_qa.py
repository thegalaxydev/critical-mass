import json
import os
import sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import specs

SHIP = {"circulation_pump", "steam_turbine", "reactor_vessel_casing", "control_rod_drive", "alarm_siren"}
INSTALL = {"reactor_vessel_casing", "fuel_channel", "control_channel", "coolant_channel", "shielded_glass", "reactor_controller", "reactor_signal_interface",
           "reactor_service_port", "primary_coolant_port", "circulation_pump", "heat_exchanger", "control_rod_drive", "relief_valve", "alarm_siren", "steam_turbine",
           "emergency_coolant_tank", "spent_fuel_rack", "shielded_cask", "fuel_assembly_bench", "shielding_concrete"}
qa = {}
for n in specs.BLOCK_DEFS:
    v = {"iso": True, "in_game": True}
    if n in INSTALL:
        v["installation"] = True
    if n in SHIP:
        v["moving_body"] = True
    if n in ("circulation_pump", "steam_turbine", "control_rod_drive"):
        v["rotation_with_network"] = True
        v["rotation_on_tilted_body"] = True
    if n == "circulation_pump":
        v["stops_when_overstressed"] = True
    qa[n] = v
for n in specs.ITEM_DEFS:
    v = {"iso": True, "in_game": True}
    if n.startswith("protective_"):
        v.update({"worn": True, "worn_poses_front_back_sneak_walk_ride_swim": True, "armor_stand": True, "slim_model_same_layers_in_code": True})
    qa[n] = v
for n in specs.FLUID_LANG:
    qa[n] = {"in_game_create_tank": True, "animated": True}
json.dump(qa, open(os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "art", "qa.json"), "w"), indent=1)
print(len(qa), "qa records")
