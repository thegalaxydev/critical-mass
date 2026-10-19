import json
import os
from reactor_layout import PRIMARY, at, start, vessel

O = (200, -60, 200)
w = at(O)
EYE = 1.62
COOLED = ["cfc", "rcr", "cfc"]
STEAM = [(6, 1, 6), (6, 2, 6), (6, 5, 6), (6, 7, 6), (7, 7, 6), (9, 7, 6), (10, 7, 6), (10, 6, 6), (10, 5, 6), (10, 4, 6), (10, 3, 6),
         (10, 2, 6), (10, 1, 6), (9, 1, 6), (8, 1, 6)]
COOLING = [(8, 6, 6), (8, 5, 6), (8, 4, 6), (8, 4, 7), (8, 4, 8), (9, 1, 9), (11, 1, 9), (11, 2, 9), (11, 3, 9), (11, 5, 9), (11, 6, 9),
           (11, 7, 9), (11, 8, 9), (11, 9, 9), (11, 9, 8), (11, 9, 7), (11, 9, 6), (10, 9, 6), (9, 9, 6), (8, 9, 6), (8, 8, 6)]
PIPES = PRIMARY + STEAM + COOLING


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px + O[0], py + O[1], pz + O[2], tx + O[0], ty + O[1] - EYE, tz + O[2])


def merge(pos, key, fluid, amount, heat):
    return "/data merge block %s {%s:{Fluid:{id:\"%s\",amount:%d},Heat:%.1fd}}" % (w(*pos), key, fluid, amount, heat)


build = vessel(O) + [
    "/setblock %s criticalmass:circulation_pump[facing=east,axis_along_first=true]" % w(4, 3, 6),
    "/setblock %s criticalmass:heat_exchanger[facing=east]" % w(6, 3, 6),
    "/setblock %s criticalmass:circulation_pump[facing=west,axis_along_first=true]" % w(7, 1, 6),
    "/setblock %s criticalmass:relief_valve[facing=south]" % w(6, 4, 6),
    "/setblock %s criticalmass:steam_turbine[axis=x]" % w(6, 6, 6),
    "/setblock %s create:stressometer[facing=south,axis_along_first=true]" % w(7, 6, 6),
    "/setblock %s criticalmass:condenser[facing=east]" % w(8, 7, 6),
    "/fill %s %s criticalmass:cooling_tower_packing" % (w(8, 1, 9), w(8, 4, 9)),
    "/setblock %s criticalmass:circulation_pump[facing=east,axis_along_first=true]" % w(10, 1, 9),
    "/setblock %s criticalmass:radiator_panel[facing=south]" % w(11, 4, 9),
] + ["/setblock %s criticalmass:reinforced_pipe" % w(*p) for p in PIPES]
fix = ["/setblock %s minecraft:air" % w(*p) for p in PIPES] + ["/setblock %s criticalmass:reinforced_pipe" % w(*p) for p in PIPES]
fill = [merge((1, 3, 3), "Coolant", "criticalmass:primary_coolant", 6000, 6000 * 200.0),
        merge((6, 3, 6), "Secondary", "minecraft:water", 1000, 1000 * 80.0),
        merge((6, 6, 6), "Coolant", "minecraft:water", 1000, 1000 * 80.0),
        merge((8, 7, 6), "Primary", "minecraft:water", 1000, 1000 * 80.0),
        merge((8, 7, 6), "Secondary", "minecraft:water", 1000, 0)]
fill += [merge((8, y, 9), "Buffer", "minecraft:water", 500, 0) for y in range(1, 5)]
fill += [merge((11, 4, 9), "Buffer", "minecraft:water", 250, 0)]
motors = ["/setblock %s create:creative_motor[facing=up]" % w(4, 2, 6), "/setblock %s create:creative_motor[facing=up]" % w(7, 0, 6),
          "/setblock %s create:creative_motor[facing=up]" % w(10, 0, 9)]
L = {"cmds": ["/criticalmass ledger"], "wait": 2}


def shot(name, stand, target):
    return [{"cmds": ["/setblock %s minecraft:barrier" % w(stand[0], stand[1] - 1, stand[2]), look(stand[0] + 0.5, stand[1], stand[2] + 0.5, *target)], "wait": 30},
            {"shot": name, "wait": 3}]


steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative", "/tp @p %d -55 %d" % (O[0] - 6, O[2] - 6)], "wait": 60, "gui": True, "camera": "first"},
         {"cmds": ["/item replace entity @p armor.head with create:goggles", "/fill %s %s minecraft:smooth_stone" % (w(-2, -1, -2), w(14, -1, 13))], "wait": 10},
         {"cmds": build, "wait": 10}, {"cmds": fix, "wait": 10},
         {"cmds": fill + start(O, motor=False, power=False)[1:] + motors + ["/setblock %s minecraft:lever[face=wall,facing=west,powered=true]" % w(0, 3, 3)], "wait": 900}, L]
steps += shot("exchanger", (6, 3, 9), (6.5, 3.5, 6.5))
steps += shot("turbine", (5, 6, 9), (6.5, 6.5, 6.5))
steps += shot("stressometer", (7, 6, 9), (7.5, 6.5, 6.5))
steps += shot("condenser", (8, 7, 3), (8.5, 7.5, 6.5))
steps += shot("tower", (5, 3, 9), (8.5, 3.5, 9.5))
steps += shot("radiator", (11, 4, 12), (11.5, 4.5, 9.7))
steps += shot("overview", (16, 12, 16), (6.0, 4.0, 6.0))
steps += [{"note": "SECTION trip"}, {"cmds": ["/setblock %s minecraft:air" % w(7, 6, 6)], "wait": 400}, L]
steps += shot("turbine_tripped", (5, 6, 9), (6.5, 6.5, 6.5))
steps += shot("relief_after_trip", (6, 4, 9), (6.5, 4.5, 6.5))
steps += [{"wait": 5}]
json.dump({"name": "steam_plant", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "steam_plant.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
