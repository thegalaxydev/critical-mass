import json
import os
import sys

import reactor_layout
from reactor_layout import PRIMARY

STEAM = [(6, 1, 6), (6, 2, 6), (6, 5, 6), (6, 7, 6), (7, 7, 6), (9, 7, 6), (10, 7, 6), (10, 6, 6), (10, 5, 6), (10, 4, 6), (10, 3, 6),
         (10, 2, 6), (10, 1, 6), (9, 1, 6), (8, 1, 6)]
COOLING = [(8, 6, 6), (8, 5, 6), (8, 4, 6), (8, 4, 7), (8, 4, 8), (9, 1, 9), (11, 1, 9), (11, 2, 9), (11, 3, 9), (11, 5, 9), (11, 6, 9),
           (11, 7, 9), (11, 8, 9), (11, 9, 9), (11, 9, 8), (11, 9, 7), (11, 9, 6), (10, 9, 6), (9, 9, 6), (8, 9, 6), (8, 8, 6)]
PIPES = PRIMARY + STEAM + COOLING
NS = "criticalmass_demo"
TAG = "cm_reactor_origin"


def rel(o):
    def w(x, y, z):
        return "~%d ~%d ~%d" % (x + o[0], y + o[1], z + o[2])
    return w


reactor_layout.at = rel
w = rel((0, 0, 0))


def merge(pos, key, fluid, amount, heat):
    return "data merge block %s {%s:{Fluid:{id:\"%s\",amount:%d},Heat:%.1fd}}" % (w(*pos), key, fluid, amount, heat)


def strip(cmds):
    return [c[1:] if c.startswith("/") else c for c in cmds]


vessel = [c for c in strip(reactor_layout.vessel((0, 0, 0))) if not c.startswith("criticalmass fuel")]
place = ["fill %s %s minecraft:air" % (w(-2, 0, -2), w(14, 12, 13)),
         "fill %s %s minecraft:smooth_stone" % (w(-2, -1, -2), w(14, -1, 13))] + vessel + [
    "setblock %s criticalmass:circulation_pump[facing=east,axis_along_first=true]" % w(4, 3, 6),
    "setblock %s criticalmass:heat_exchanger[facing=east]" % w(6, 3, 6),
    "setblock %s criticalmass:circulation_pump[facing=west,axis_along_first=true]" % w(7, 1, 6),
    "setblock %s criticalmass:relief_valve[facing=south]" % w(6, 4, 6),
    "setblock %s criticalmass:steam_turbine[axis=x]" % w(6, 6, 6),
    "setblock %s create:stressometer[facing=south,axis_along_first=true]" % w(7, 6, 6),
    "setblock %s criticalmass:condenser[facing=east]" % w(8, 7, 6),
    "fill %s %s criticalmass:cooling_tower_packing" % (w(8, 1, 9), w(8, 4, 9)),
    "setblock %s criticalmass:circulation_pump[facing=east,axis_along_first=true]" % w(10, 1, 9),
    "setblock %s criticalmass:radiator_panel[facing=south]" % w(11, 4, 9),
] + ["setblock %s criticalmass:reinforced_pipe" % w(*p) for p in PIPES]
place += ["setblock %s minecraft:air" % w(*p) for p in PIPES] + ["setblock %s criticalmass:reinforced_pipe" % w(*p) for p in PIPES]
place += ["schedule function %s:reactor/start 40t replace" % NS]

start_here = [merge((1, 3, 3), "Coolant", "criticalmass:primary_coolant", 6000, 6000 * 200.0),
              merge((6, 3, 6), "Secondary", "minecraft:water", 1000, 1000 * 80.0),
              merge((6, 6, 6), "Coolant", "minecraft:water", 1000, 1000 * 80.0),
              merge((8, 7, 6), "Primary", "minecraft:water", 1000, 1000 * 80.0),
              merge((8, 7, 6), "Secondary", "minecraft:water", 1000, 0)]
start_here += [merge((8, y, 9), "Buffer", "minecraft:water", 500, 0) for y in range(1, 5)]
start_here += [merge((11, 4, 9), "Buffer", "minecraft:water", 250, 0)]
start_here += ["criticalmass fuel %s fill" % w(*reactor_layout.CONTROLLER),
               "setblock %s create:creative_motor[facing=up]" % w(4, 2, 6),
               "setblock %s create:creative_motor[facing=up]" % w(7, 0, 6),
               "setblock %s create:creative_motor[facing=up]" % w(10, 0, 9),
               "setblock %s minecraft:lever[face=wall,facing=west,powered=true]" % w(*reactor_layout.REDSTONE),
               "tellraw @a {\"text\":\"Reactor plant filled, fuelled and started. Flip the lever on the controller side to stop it.\",\"color\":\"green\"}"]

build = ["kill @e[type=minecraft:marker,tag=%s]" % TAG,
         "summon minecraft:marker ~3 ~ ~3 {Tags:[\"%s\"]}" % TAG,
         "execute at @e[type=minecraft:marker,tag=%s,limit=1] run function %s:reactor/place" % (TAG, NS),
         "tellraw @s {\"text\":\"Building the reactor plant. It starts in 2 seconds.\",\"color\":\"yellow\"}"]
start = ["execute as @e[type=minecraft:marker,tag=%s] at @s run function %s:reactor/start_here" % (TAG, NS),
         "kill @e[type=minecraft:marker,tag=%s]" % TAG]

FILES = {"build": build, "place": place, "start": start, "start_here": start_here}


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
