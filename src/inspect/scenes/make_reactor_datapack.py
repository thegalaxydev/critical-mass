import json
import os
import sys

import reactor_layout
from reactor_layout import CONTROLLER, PRIMARY, REDSTONE

STEAM = [(6, 1, 6), (6, 2, 6), (6, 5, 6), (6, 7, 6), (7, 7, 6), (9, 7, 6), (10, 7, 6), (10, 6, 6), (10, 5, 6), (10, 4, 6), (10, 3, 6),
         (10, 2, 6), (10, 1, 6), (9, 1, 6), (8, 1, 6)]
COOLING = [(8, 6, 6), (8, 6, 7), (8, 5, 7), (8, 5, 8), (9, 2, 9), (9, 1, 9), (11, 1, 9), (11, 2, 9), (11, 3, 9), (11, 5, 9), (11, 6, 9),
           (11, 7, 9), (11, 8, 9), (11, 9, 9), (11, 9, 8), (11, 9, 7), (11, 9, 6), (10, 9, 6), (9, 9, 6), (8, 9, 6), (8, 8, 6)]
VALVE = (7, 3, 4)
PLATE = (7, 3, 2)
FEEDWATER = (9, 1, 6)
MAKEUP = (5, 3, 9)
TOWER_MOTOR = (8, 1, 9)
FILL = [(8, y, 9) for y in range(2, 6)]
ECCS_PORT, ECCS_PUMP, ECCS_TANK, ECCS_LEVER = (1, 2, 2), (0, 2, 2), (-1, 2, 2), (-1, 4, 2)
PIPES = [p for p in PRIMARY if p not in (VALVE, PLATE)] + [p for p in STEAM if p != FEEDWATER] + COOLING
DRIVES = [((3, 6, 2), "south", (3, 6, 1), (2, 6, 2)), ((3, 6, 4), "north", (3, 6, 5), (2, 6, 4))]
VIEWPORTS = [(5, 2, 4), (5, 3, 4), (5, 4, 4), (3, 2, 5), (3, 4, 5)]
CORE = ["frf", "cfc", "frf"]
NS = "criticalmass_demo"
TAG = "cm_reactor_origin"
AREA = ((-9, -2), (14, 13))
TEMP = "cm_bind"
PUMP1, TURBINE = (4, 3, 6), (6, 6, 6)
SCREENS = ((-9, 2), (-1, 6), 12)
SCREEN_SHAFT = (-5, 4, 13)
SCREEN_MOTOR = (-5, 4, 14)
PRESSURIZER = (6, 5, 5)
DESK_Z = 11
DESKS = {
    -3: [("reset", "button", CONTROLLER, 1, 7, "RESET"), ("scram", "button", CONTROLLER, 0, 0, "SCRAM")],
    -4: [("rod_out", "button", CONTROLLER, 3, 2, "RODS OUT"), ("rod_in", "button", CONTROLLER, 2, 4, "RODS IN")],
    -5: [("power", "gauge", CONTROLLER, 5, 4, "HEAT OUTPUT"), ("alarm", "alarm", CONTROLLER, 0, 0, "ALARM")],
    -6: [("pressure", "gauge", CONTROLLER, 2, 4, "PRESSURE"), ("coolant", "gauge", CONTROLLER, 1, 4, "COOLANT")],
    -7: [("turbine_reset", "button", TURBINE, 1, 7, "TURBINE RESET"), ("trip", "button", TURBINE, 0, 1, "TURBINE TRIP")],
}
RUN_POWER = 0.7
RUN_COOLANT = 125


def rel(o):
    def w(x, y, z):
        return "~%d ~%d ~%d" % (x + o[0], y + o[1], z + o[2])
    return w


reactor_layout.at = rel
w = rel((0, 0, 0))


def merge(pos, key, fluid, amount, heat):
    return "data merge block %s {%s:{Fluid:{id:\"%s\",amount:%d},Heat:%.1fd}}" % (w(*pos), key, fluid, amount, heat)


def tank(pos, fluid, amount, heat=0.0):
    held = "Fluid:{id:\"%s\",amount:%d}" % (fluid, amount)
    return "data merge block %s {TankContent:{%s},\"neoforge:attachments\":{\"criticalmass:tank_coolant\":{%s,Heat:%.1fd}}}" % (w(*pos), held, held, heat)


def strip(cmds):
    return [c[1:] if c.startswith("/") else c for c in cmds]


def setblock(pos, block):
    return "setblock %s %s" % (w(*pos), block)


def bind(panel, target, extra="", key="Target"):
    cmds = ["data merge block %s {%s:[I;0,0,0]%s}" % (w(*panel), key, extra),
            "summon minecraft:marker %s {Tags:[\"%s\"]}" % (w(*target), TEMP)]
    cmds += ["execute store result block %s %s[%d] int 1 run data get entity @e[type=minecraft:marker,tag=%s,limit=1] Pos[%d]" % (w(*panel), key, i, TEMP, i) for i in range(3)]
    return cmds + ["kill @e[type=minecraft:marker,tag=%s]" % TEMP]


def fit(desk, slot, part):
    _, kind, target, mode, color, label = part
    return ["criticalmass desk %s %d %s %s %d %d %s" % (w(*desk), slot, kind, w(*target), mode, color, label)]


(x0, z0), (x1, z1) = AREA
vessel = [c for c in strip(reactor_layout.vessel((0, 0, 0), CORE)) if not c.startswith("criticalmass fuel")]

plant = [
    setblock((4, 3, 6), "criticalmass:circulation_pump[facing=east,axis_along_first=true]"),
    setblock((6, 3, 6), "criticalmass:heat_exchanger[facing=east]"),
    setblock((7, 1, 6), "criticalmass:circulation_pump[facing=west,axis_along_first=true]"),
    setblock((6, 4, 6), "criticalmass:relief_valve[facing=south]"),
    setblock((6, 6, 6), "criticalmass:steam_turbine[axis=x]"),
    setblock((7, 6, 6), "create:stressometer[facing=south,axis_along_first=true]"),
    setblock((5, 6, 6), "create:mechanical_press[facing=west]"),
    setblock((8, 7, 6), "criticalmass:condenser[facing=east]"),
    "fill %s %s criticalmass:cooling_tower_packing" % (w(*FILL[0]), w(*FILL[-1])),
    setblock((8, 6, 9), "create:encased_fan[facing=up]"),
    setblock(MAKEUP, "create:creative_fluid_tank"),
    setblock((6, 3, 9), "create:mechanical_pump[facing=east]"),
    setblock((7, 3, 9), "create:fluid_pipe"),
    setblock((6, 4, 9), "create:cogwheel[axis=x]"),
    setblock((5, 4, 9), "create:creative_motor[facing=east]"),
    setblock(FEEDWATER, "create:fluid_tank"),
    setblock(PRESSURIZER, "criticalmass:pressurizer"),
    setblock((PRESSURIZER[0], PRESSURIZER[1] - 1, PRESSURIZER[2]), "create:blaze_burner[blaze=kindled]{isCreative:1b}"),
    setblock((10, 1, 9), "criticalmass:circulation_pump[facing=east,axis_along_first=true]"),
    setblock((11, 4, 9), "criticalmass:radiator_panel[facing=south]"),
    setblock(VALVE, "criticalmass:reinforced_valve[facing=up,axis_along_first=true]"),
    setblock((6, 3, 4), "create:creative_motor[facing=east]"),
    setblock(PLATE, "criticalmass:boiler_heating_plate[facing=south]"),
    setblock((7, 4, 2), "create:fluid_tank"),
    setblock((7, 5, 2), "create:steam_whistle"),
    setblock((8, 4, 2), "create:fluid_pipe"),
    setblock((9, 4, 2), "create:mechanical_pump[facing=west]"),
    setblock((10, 4, 2), "create:creative_fluid_tank"),
    setblock((9, 5, 2), "create:cogwheel[axis=x]"),
    setblock((10, 5, 2), "create:creative_motor[facing=west]"),
]

safety = [
    setblock(ECCS_PORT, "criticalmass:primary_coolant_port[facing=west,mode=inlet]"),
    setblock(ECCS_PUMP, "create:mechanical_pump[facing=east]"),
    setblock(ECCS_TANK, "create:fluid_tank"),
    setblock((0, 3, 2), "create:cogwheel[axis=x]"),
    setblock((-1, 3, 2), "create:clutch[axis=x]"),
    setblock((-2, 3, 2), "create:creative_motor[facing=east]"),
    setblock(ECCS_LEVER, "minecraft:lever[face=floor,facing=north,powered=true]"),
    setblock((4, 6, 3), "criticalmass:docking_service_connector[facing=up]"),
]

bay = ["fill %s %s criticalmass:shielding_concrete" % (w(-2, 0, -1), w(-2, 2, 7)),
       "fill %s %s criticalmass:shielded_glass" % (w(-2, 1, 2), w(-2, 1, 4)),
       setblock((-5, 0, 0), "criticalmass:spent_fuel_rack"),
       setblock((-5, 0, 1), "criticalmass:spent_fuel_rack"),
       setblock((-5, 0, 3), "criticalmass:shielded_cask"),
       setblock((-5, 0, 5), "criticalmass:centrifuge"),
       setblock((-5, 1, 5), "create:creative_motor[facing=down]{ScrollValue:128}"),
       setblock((-6, 0, 5), "criticalmass:open_waste_canister"),
       setblock((-4, -2, 7), "criticalmass:sealed_waste_canister"),
       setblock((-4, -1, 7), "criticalmass:shielded_hatch[half=top,open=false,facing=north]"),
       setblock((-8, 0, -1), "criticalmass:steel_block"),
       setblock((-8, 0, 0), "criticalmass:reactor_alloy_block"),
       setblock((-8, 0, 1), "criticalmass:uranium_ore"),
       setblock((-8, 0, 2), "criticalmass:deepslate_uranium_ore")]

(bx0, by0), (bx1, by1), bz = SCREENS
desk = ["fill %s %s criticalmass:shielding_concrete" % (w(bx0, 0, bz + 1), w(bx1 + 1, by1 + 1, bz + 1)),
        "fill %s %s criticalmass:plant_screen[facing=north]" % (w(bx0, by0, bz), w(bx1, by1, bz)),
        setblock(SCREEN_SHAFT, "create:shaft[axis=z]"),
        setblock(SCREEN_MOTOR, "create:creative_motor[facing=north]")]
desk += ["fill %s %s criticalmass:shielding_concrete" % (w(bx0, 0, bz), w(bx1, 1, bz))]
desk += [setblock((x, 0, DESK_Z), "criticalmass:reactor_alloy_dashboard[facing=north,join_cw=%s,join_ccw=%s]" % (str(x + 1 in DESKS).lower(), str(x - 1 in DESKS).lower())) for x in sorted(DESKS)]
wiring = []
for x, parts in sorted(DESKS.items()):
    for slot, part in enumerate(parts):
        wiring += fit((x, 0, DESK_Z), slot, part)
wiring += bind((bx1, by1, bz), CONTROLLER, key="Reactor") + bind((bx1, by1, bz), TURBINE, key="Turbine")

place = ["fill %s %s minecraft:air" % (w(x0, 0, z0), w(x1, 12, z1)),
         "fill %s %s minecraft:smooth_stone" % (w(x0, -1, z0), w(x1, -1, z1))]
place += vessel + [setblock(p, "criticalmass:reactor_viewport") for p in VIEWPORTS] + plant + safety + bay + desk
place += [setblock(p, "criticalmass:reinforced_pipe") for p in PIPES]
place += [setblock(p, "minecraft:air") for p in PIPES] + [setblock(p, "criticalmass:reinforced_pipe") for p in PIPES]
place += ["schedule function %s:reactor/start 100t replace" % NS]

start_here = [merge(CONTROLLER, "Coolant", "criticalmass:primary_coolant", 6000, 6000 * 80.0),
              merge((6, 3, 6), "Secondary", "minecraft:water", 1000, 1000 * 80.0),
              merge((6, 6, 6), "Coolant", "minecraft:water", 1000, 1000 * 80.0),
              merge((8, 7, 6), "Primary", "minecraft:water", 1000, 1000 * 80.0),
              merge((8, 7, 6), "Secondary", "minecraft:water", 1000, 0),
              tank(ECCS_TANK, "criticalmass:primary_coolant", 4000),
              merge((-5, 0, 0), "Buffer", "minecraft:water", 4000, 0),
              merge((-5, 0, 1), "Buffer", "minecraft:water", 4000, 0),
              "data merge block %s {TankContent:{Fluid:{id:\"minecraft:water\",amount:1000}}}" % w(10, 4, 2),
              "data merge block %s {TankContent:{Fluid:{id:\"minecraft:water\",amount:1000}}}" % w(*MAKEUP)]
start_here += [merge(p, "Buffer", "minecraft:water", 500, 0) for p in FILL] + [tank(FEEDWATER, "minecraft:water", 1500, 1500 * 80.0)]
start_here += [merge((11, 4, 9), "Buffer", "minecraft:water", 250, 0)]
start_here += wiring
start_here += ["criticalmass fuel %s fill" % w(*CONTROLLER),
               setblock((6, 3, 4), "minecraft:air"),
               setblock((8, 3, 4), "create:copper_valve_handle[facing=east]"),
               setblock((4, 2, 6), "create:creative_motor[facing=up]"),
               setblock((7, 0, 6), "create:creative_motor[facing=up]"),
               setblock((10, 0, 9), "create:creative_motor[facing=up]"),
               setblock(TOWER_MOTOR, "create:creative_motor[facing=up]{ScrollValue:24}")]
for pos, facing, motor, lever in DRIVES:
    start_here += [setblock(pos, "criticalmass:control_rod_drive[facing=%s]" % facing),
                   setblock(motor, "create:creative_motor[facing=%s]" % facing)]
start_here += ["tellraw @a {\"text\":\"Plant built: fuelled, pumps running, rods in. Start it by stepping the rods out with the RODS OUT button on the desk and watching the period, or run the reactor/run function to jump to power.\",\"color\":\"green\"}"]

run_here = ["criticalmass steady %s %.2f %d" % (w(*CONTROLLER), RUN_POWER, RUN_COOLANT)]
run_here += ["tellraw @a {\"text\":\"Reactor set to power with equilibrium xenon. Hold it steady with the rod buttons on the desk.\",\"color\":\"green\"}"]

build = ["kill @e[type=minecraft:marker,tag=%s]" % TAG,
         "summon minecraft:marker ~%d ~ ~%d {Tags:[\"%s\"]}" % (1 - x0, 1 - z0, TAG),
         "execute at @e[type=minecraft:marker,tag=%s,limit=1] run function %s:reactor/place" % (TAG, NS),
         "tellraw @s {\"text\":\"Building the reactor plant. It is filled and fuelled in 5 seconds.\",\"color\":\"yellow\"}"]
start = ["execute as @e[type=minecraft:marker,tag=%s] at @s run function %s:reactor/start_here" % (TAG, NS)]
run = ["execute as @e[type=minecraft:marker,tag=%s,limit=1] at @s run function %s:reactor/run_here" % (TAG, NS)]

FILES = {"build": build, "place": place, "start": start, "start_here": start_here, "run": run, "run_here": run_here}


def write(root):
    os.makedirs(os.path.join(root, "data", NS, "function", "reactor"), exist_ok=True)
    with open(os.path.join(root, "pack.mcmeta"), "w", newline="\n") as f:
        json.dump({"pack": {"pack_format": 48, "description": "Builds a running Critical Mass reactor plant"}}, f, indent=1)
    for name, cmds in FILES.items():
        with open(os.path.join(root, "data", NS, "function", "reactor", name + ".mcfunction"), "w", newline="\n") as f:
            f.write("\n".join(cmds) + "\n")


if __name__ == "__main__":
    for target in sys.argv[1:]:
        write(target)
        print("wrote", target)
