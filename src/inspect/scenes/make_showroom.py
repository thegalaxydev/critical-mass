import json
import os

BLOCKS = [
    ("reactor_vessel_casing", ""), ("reactor_controller", "[facing=north]"), ("fuel_channel", "[axis=y]"), ("control_channel", "[axis=y]"),
    ("coolant_channel", "[axis=y]"), ("reactor_service_port", "[facing=north]"), ("primary_coolant_port", "[facing=north,mode=inlet]"),
    ("primary_coolant_port", "[facing=north,mode=outlet]"), ("heat_exchanger", "[facing=north]"), ("condenser", "[facing=north]"),
    ("cooling_tower_packing", ""), ("radiator_panel", "[facing=north]"), ("emergency_coolant_tank", ""), ("relief_valve", "[facing=north]"),
    ("reinforced_valve", "[facing=up,axis_along_first=false]"), ("boiler_heating_plate", "[facing=north]"), ("shielding_concrete", ""), ("shielded_glass", ""),
    ("shielded_hatch", "[facing=north,half=bottom,open=false]"), ("fuel_assembly_bench", "[facing=north]"), ("spent_fuel_rack", ""),
    ("shielded_cask", ""), ("open_waste_canister", ""), ("sealed_waste_canister", ""), ("instrumentation_panel", "[facing=north]"), ("reactor_signal_interface", "[facing=north]"),
    ("alarm_siren", "[facing=north,powered=false]"), ("alarm_siren", "[facing=north,powered=true]"), ("docking_service_connector", "[facing=north,connected=false]"),
    ("docking_service_connector", "[facing=north,connected=true]"), ("circulation_pump", "[facing=north,axis_along_first=true]"),
    ("steam_turbine", "[axis=x]"), ("control_rod_drive", "[facing=north]"), ("reinforced_pipe", "[east=true,west=true]"),
]
ITEMS = ["raw_uranium", "uranium_concentrate", "ceramic_fuel_pellet", "fuel_cladding", "empty_fuel_assembly", "fuel_assembly",
         "spent_fuel_assembly", "damaged_fuel_assembly", "reactor_alloy_ingot", "reactor_alloy_sheet", "steel_plate", "control_rod", "turbine_blade",
         "sealing_gasket", "survey_meter", "dosimeter", "protective_hood", "protective_suit", "protective_leggings", "protective_boots",
         "decontamination_compound", "welding_torch"]

steps = [{"cmds": ["/time set 6000", "/weather clear", "/fill -20 -61 -30 140 -61 40 minecraft:smooth_stone"], "wait": 20}]
cmds = []
for i, (b, st) in enumerate(BLOCKS):
    x = i * 3
    if b in ("instrumentation_panel", "radiator_panel"):
        cmds.append("/setblock %d -60 1 criticalmass:reactor_vessel_casing" % x)
    if b == "alarm_siren" and "powered=true" in st:
        cmds.append("/setblock %d -60 1 minecraft:redstone_block" % x)
    cmds.append("/setblock %d -60 0 criticalmass:%s%s" % (x, b, st))
steps.append({"cmds": cmds, "wait": 20})

kin = [
    "/setblock 0 -60 -10 criticalmass:circulation_pump[facing=north,axis_along_first=true]", "/setblock -1 -60 -10 create:creative_motor[facing=east]",
    "/setblock 4 -60 -10 criticalmass:circulation_pump[facing=east,axis_along_first=true]", "/setblock 4 -61 -10 create:creative_motor[facing=up]",
    "/setblock 8 -60 -10 criticalmass:circulation_pump[facing=up,axis_along_first=false]", "/setblock 8 -60 -11 create:creative_motor[facing=south]",
    "/setblock 12 -60 -10 criticalmass:steam_turbine[axis=x]", "/setblock 11 -60 -10 create:creative_motor[facing=east]",
    "/setblock 16 -60 -10 criticalmass:steam_turbine[axis=z]", "/setblock 16 -60 -11 create:creative_motor[facing=south]",
    "/setblock 20 -60 -10 criticalmass:control_rod_drive[facing=north]", "/setblock 20 -60 -9 create:creative_motor[facing=north]",
    "/setblock 24 -60 -10 criticalmass:control_rod_drive[facing=east]", "/setblock 23 -60 -10 create:creative_motor[facing=east]",
    "/setblock 28 -60 -10 criticalmass:circulation_pump[facing=north,axis_along_first=true]", "/setblock 27 -60 -10 create:creative_motor[facing=east]",
    "/setblock 28 -60 -9 criticalmass:reinforced_pipe", "/setblock 28 -60 -8 criticalmass:reinforced_pipe", "/setblock 28 -60 -7 criticalmass:primary_coolant_port[facing=north,mode=inlet]",
    "/setblock 28 -60 -11 criticalmass:reinforced_pipe", "/setblock 28 -60 -12 criticalmass:reinforced_pipe", "/setblock 29 -60 -12 criticalmass:reinforced_pipe",
    "/setblock 30 -60 -12 criticalmass:reinforced_pipe", "/setblock 30 -59 -12 criticalmass:reinforced_pipe",
]
steps.append({"cmds": kin, "wait": 20})

dropped = []
for i, it in enumerate(ITEMS):
    x = (i % 12) * 1.0
    z = 8 + (i // 12) * 1.2
    dropped.append("/summon minecraft:item %.1f -59.6 %.1f {NoGravity:1b,PickupDelay:32767,Age:-32768,Item:{id:\"criticalmass:%s\",count:1}}" % (x + 0.5, z, it))
dropped.append("/summon minecraft:armor_stand 16 -60 9 {ShowArms:1b,NoBasePlate:1b,Rotation:[180f,0f],ArmorItems:[{id:\"criticalmass:protective_boots\",count:1},{id:\"criticalmass:protective_leggings\",count:1},{id:\"criticalmass:protective_suit\",count:1},{id:\"criticalmass:protective_hood\",count:1}],HandItems:[{id:\"criticalmass:survey_meter\",count:1},{id:\"criticalmass:dosimeter\",count:1}]}")
dropped.append("/summon minecraft:armor_stand 19 -60 9 {ShowArms:1b,NoBasePlate:1b,Rotation:[0f,0f],ArmorItems:[{id:\"criticalmass:protective_boots\",count:1},{id:\"criticalmass:protective_leggings\",count:1},{id:\"criticalmass:protective_suit\",count:1},{id:\"criticalmass:protective_hood\",count:1}]}")
steps.append({"cmds": dropped, "wait": 20})
steps.append({"cmds": ["/gamemode spectator"], "gui": False, "fov": 60, "wait": 10})

for g in range(0, len(BLOCKS), 4):
    cx = g * 3 + 4.5
    steps.append({"cmds": ["/tp @p %.1f -58.3 -4.2 0 18" % cx], "wait": 45})
    steps.append({"shot": "row_%02d" % (g // 4)})
    steps.append({"cmds": ["/tp @p %.1f -57.9 4.4 180 22" % cx], "wait": 30})
    steps.append({"shot": "row_%02d_back" % (g // 4)})

for name, (x, y, z, yaw, pitch) in {
    "kin_pumps": (4.5, -58.2, -14.6, 0, 22), "kin_turbines": (14.5, -58.4, -14.2, 0, 22), "kin_roddrives": (22.5, -58.3, -13.6, 0, 25), "kin_pipes": (29.0, -57.5, -15.0, 0, 30)}.items():
    steps.append({"cmds": ["/tp @p %.1f %.1f %.1f %d %d" % (x, y, z, yaw, pitch)], "wait": 40})
    steps.append({"shot": name + "_a", "wait": 6})
    steps.append({"shot": name + "_b"})

steps.append({"cmds": ["/tp @p 6.0 -58.4 5.6 0 32"], "wait": 30})
steps.append({"shot": "items_dropped"})
for name, (x, z, yaw) in {"outfit_front": (17.5, 6.2, 0), "outfit_back": (17.5, 12.0, 180), "outfit_left": (14.0, 9.0, -90), "outfit_right": (21.5, 9.0, 90)}.items():
    steps.append({"cmds": ["/tp @p %.1f -58.6 %.1f %d 8" % (x, z, yaw)], "wait": 30})
    steps.append({"shot": name})

worn = ["/gamemode creative", "/tp @p 40.5 -60 10.5 180 0",
        "/item replace entity @p armor.head with criticalmass:protective_hood", "/item replace entity @p armor.chest with criticalmass:protective_suit",
        "/item replace entity @p armor.legs with criticalmass:protective_leggings", "/item replace entity @p armor.feet with criticalmass:protective_boots",
        "/item replace entity @p weapon.mainhand with criticalmass:survey_meter", "/item replace entity @p weapon.offhand with criticalmass:dosimeter"]
steps.append({"cmds": worn, "wait": 20})
steps.append({"camera": "front", "wait": 30})
steps.append({"shot": "worn_front"})
steps.append({"camera": "back", "wait": 15})
steps.append({"shot": "worn_back"})
steps.append({"camera": "front", "sneak": True, "wait": 15})
steps.append({"shot": "worn_sneak"})
steps.append({"sneak": False, "forward": True, "wait": 8})
steps.append({"shot": "worn_walk"})
steps.append({"forward": False, "cmds": ["/summon minecraft:minecart 44.5 -60 10.5", "/ride @p mount @e[type=minecraft:minecart,limit=1,sort=nearest]"], "wait": 25})
steps.append({"shot": "worn_ride"})
steps.append({"cmds": ["/ride @p dismount", "/fill 48 -61 8 52 -60 12 minecraft:water", "/tp @p 50.5 -60.4 10.5 180 0"], "wait": 20})
steps.append({"sprint": True, "forward": True, "wait": 12})
steps.append({"shot": "worn_swim"})
steps.append({"sprint": False, "forward": False, "cmds": ["/tp @p 40.5 -60 10.5 180 20"], "camera": "first", "gui": True, "wait": 25})
steps.append({"shot": "fp_meter_dosimeter"})
steps.append({"cmds": ["/item replace entity @p weapon.mainhand with criticalmass:fuel_assembly", "/item replace entity @p weapon.offhand with criticalmass:spent_fuel_assembly"], "wait": 15})
steps.append({"shot": "fp_fuel"})
for h in range(3):
    give = ["/clear @p"] + ["/item replace entity @p hotbar.%d with criticalmass:%s" % (k, it) for k, it in enumerate(ITEMS[h * 9:(h + 1) * 9])]
    steps.append({"cmds": give, "wait": 15})
    steps.append({"shot": "hotbar_items_%d" % h})
bl = sorted(set(b for b, _ in BLOCKS))
for h in range(0, len(bl), 9):
    give = ["/clear @p"] + ["/item replace entity @p hotbar.%d with criticalmass:%s" % (k, it) for k, it in enumerate(bl[h:h + 9])]
    steps.append({"cmds": give, "wait": 15})
    steps.append({"shot": "hotbar_blocks_%d" % (h // 9)})

over = ["/setblock 70 -58 -20 create:water_wheel[axis=x]", "/setblock 70 -57 -21 minecraft:water", "/setblock 70 -57 -19 minecraft:water"]
for k in range(9):
    over.append("/setblock %d -58 -20 criticalmass:circulation_pump[facing=north,axis_along_first=true]" % (71 + k))
steps.append({"cmds": over, "wait": 60})
steps.append({"cmds": ["/gamemode spectator", "/tp @p 74.5 -56.8 -25.5 0 22"], "gui": False, "camera": "first", "wait": 40})
steps.append({"shot": "overstress_a", "wait": 8})
steps.append({"shot": "overstress_b"})
steps.append({"cmds": ["/fill 74 -58 -20 79 -58 -20 minecraft:air"], "wait": 60})
steps.append({"shot": "relieved_a", "wait": 8})
steps.append({"shot": "relieved_b"})

tanks = []
for i, fl in enumerate(["primary_coolant", "steam", "contaminated_coolant"]):
    x = 36 + i * 3
    tanks.append("/setblock %d -60 -10 create:fluid_tank{Size:1,Height:1,Window:1b,Controller:[I;%d,-60,-10],TankContent:{Fluid:{id:\"criticalmass:%s\",amount:6000}}}" % (x, x, fl))
steps.append({"cmds": tanks, "wait": 30})
steps.append({"cmds": ["/gamemode spectator", "/tp @p 39.5 -58.6 -13.6 0 25"], "gui": False, "camera": "first", "wait": 40})
steps.append({"shot": "fluid_tanks_a", "wait": 10})
steps.append({"shot": "fluid_tanks_b"})

inst = ["/fill 2 -60 22 6 -57 26 criticalmass:reactor_vessel_casing", "/fill 3 -60 23 5 -57 25 minecraft:air",
        "/fill 4 -60 24 4 -57 24 criticalmass:fuel_channel[axis=y]", "/fill 3 -60 24 3 -57 24 criticalmass:control_channel[axis=y]", "/fill 5 -60 24 5 -57 24 criticalmass:control_channel[axis=y]",
        "/fill 4 -60 23 4 -57 23 criticalmass:coolant_channel[axis=y]", "/fill 4 -60 25 4 -57 25 criticalmass:coolant_channel[axis=y]",
        "/fill 3 -59 22 5 -58 22 criticalmass:shielded_glass", "/setblock 4 -60 22 criticalmass:reactor_controller[facing=north]",
        "/setblock 2 -59 21 criticalmass:instrumentation_panel[facing=north]", "/setblock 6 -60 22 criticalmass:reactor_signal_interface[facing=north]",
        "/setblock 2 -60 22 criticalmass:reactor_service_port[facing=north]",
        "/setblock 6 -59 24 criticalmass:primary_coolant_port[facing=east,mode=outlet]", "/setblock 7 -59 24 criticalmass:reinforced_pipe[east=true,west=true]",
        "/setblock 8 -59 24 criticalmass:circulation_pump[facing=east,axis_along_first=true]", "/setblock 8 -60 24 create:creative_motor[facing=up]",
        "/setblock 9 -59 24 criticalmass:reinforced_pipe[east=true,west=true]", "/setblock 10 -59 24 criticalmass:heat_exchanger[facing=north]",
        "/setblock 2 -59 24 criticalmass:primary_coolant_port[facing=west,mode=inlet]", "/setblock 1 -59 24 criticalmass:reinforced_pipe[east=true,west=true]",
        "/setblock 3 -56 24 criticalmass:control_rod_drive[facing=north]", "/setblock 3 -56 25 create:creative_motor[facing=north]",
        "/setblock 5 -56 24 criticalmass:control_rod_drive[facing=north]", "/setblock 5 -56 25 create:creative_motor[facing=north]",
        "/setblock 4 -56 23 criticalmass:relief_valve[facing=north]", "/setblock 6 -57 21 criticalmass:alarm_siren[facing=north,powered=false]", "/setblock 7 -57 21 minecraft:redstone_block",
        "/setblock 13 -59 24 criticalmass:steam_turbine[axis=x]", "/setblock 12 -59 24 create:shaft[axis=x]", "/setblock 14 -59 24 create:shaft[axis=x]", "/setblock 15 -59 24 create:creative_motor[facing=west]",
        "/setblock 0 -60 27 criticalmass:emergency_coolant_tank", "/setblock 9 -60 27 criticalmass:spent_fuel_rack", "/setblock 11 -60 27 criticalmass:shielded_cask",
        "/setblock 13 -60 27 criticalmass:fuel_assembly_bench[facing=north]", "/fill -1 -60 29 15 -58 29 criticalmass:shielding_concrete"]
steps.append({"cmds": inst, "wait": 40, "gui": False, "camera": "first"})
steps.append({"cmds": ["/gamemode spectator", "/tp @p 17.5 -53.0 13.5 48 20"], "wait": 40})
steps.append({"shot": "install_a", "wait": 7})
steps.append({"shot": "install_b"})
steps.append({"cmds": ["/tp @p 4.5 -56.5 14.5 0 15"], "wait": 30})
steps.append({"shot": "install_front_a", "wait": 7})
steps.append({"shot": "install_front_b"})

ship = ["/gamemode spectator", "/fill 100 -58 0 106 -58 4 create:andesite_casing",
        "/setblock 102 -57 2 criticalmass:circulation_pump[facing=north,axis_along_first=true]", "/setblock 101 -57 2 create:creative_motor[facing=east]",
        "/setblock 104 -57 2 criticalmass:steam_turbine[axis=x]", "/setblock 103 -57 2 create:shaft[axis=x]", "/setblock 105 -57 2 create:shaft[axis=x]",
        "/setblock 102 -57 0 criticalmass:reactor_vessel_casing", "/setblock 103 -57 0 criticalmass:instrumentation_panel[facing=north]", "/setblock 103 -57 1 criticalmass:reactor_vessel_casing",
        "/setblock 100 -57 4 criticalmass:control_rod_drive[facing=north]", "/setblock 100 -57 5 create:creative_motor[facing=north]",
        "/setblock 106 -57 4 criticalmass:alarm_siren[facing=north,powered=false]"]
steps.append({"cmds": ship, "wait": 30, "gui": False, "camera": "first"})
steps.append({"cmds": ["/tp @p 103.0 -54.5 -6.0 0 25"], "wait": 40})
steps.append({"shot": "ship_before_a", "wait": 6})
steps.append({"shot": "ship_before_b"})
steps.append({"cmds": ["/sable assemble area 100 -58 0 106 -57 5"], "wait": 40})
steps.append({"cmds": ["/fill 98 -60 6 108 -58 6 minecraft:stone_bricks", "/sable paused true", "/sable teleport @n 103 -50 3", "/tp @p 113 -50 -8 40 20"], "wait": 30})
steps.append({"cmds": ["/sable paused false", "/sable physics impulse @n angular 30 0 0 global"], "wait": 4})
steps.append({"shot": "ship_air_a", "wait": 4})
steps.append({"shot": "ship_air_b", "wait": 100})
steps.append({"cmds": ["/sable info @n", "/tp @p 110.5 -55.5 -5.5 40 22"], "wait": 30})
steps.append({"shot": "ship_tilted_a", "wait": 7})
steps.append({"shot": "ship_tilted_b"})
steps.append({"cmds": ["/tp @p 99.5 -55.0 -4.5 -30 20"], "wait": 30})
steps.append({"shot": "ship_tilted_side_a", "wait": 7})
steps.append({"shot": "ship_tilted_side_b"})
steps.append({"wait": 10})

out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "showroom.json")
json.dump({"name": "showroom", "steps": steps}, open(out, "w"), indent=1)
print(out, len(steps), "steps")
