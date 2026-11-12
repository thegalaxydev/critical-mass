import json
import math
import os

EYE = 1.62
SLOPE = math.tan(math.radians(22.5))
STRETCH = 1 / math.cos(math.radians(22.5))
BASE = (16 + 1.2) / 16.0
M = (40, -60, 40)


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.3f %.3f %.3f" % (px, py, pz, tx, ty - EYE, tz)


def ctl(column, u, v):
    across = column * 16 + u
    depth = v / STRETCH / 16
    return (M[0] + 3 - across / 16.0, M[1] + BASE + depth * SLOPE, M[2] + depth)


def click(column, u, v):
    x, y, z = ctl(column, u, v)
    block = [int(math.floor(x)), M[1] + 1, M[2] + (1 if v / STRETCH > 16 else 0)]
    return [{"cmds": [look(x, -60, 38.6, x, y, z)], "wait": 12}, {"use": block + ["up"], "wait": 3}]


setup = ["/time set 6000", "/gamerule doDaylightCycle false", "/weather clear", "/gamemode creative", "/tp @p 40 -60 36",
         "/fill 30 -60 30 70 -50 60 minecraft:air", "/fill 30 -61 30 70 -61 60 minecraft:smooth_stone", "/clear @p"]
build = ["/setblock 40 -60 40 criticalmass:control_console[facing=north]",
         "/fill 37 -60 44 43 -56 44 criticalmass:shielding_concrete",
         "/fill 38 -59 43 42 -57 43 criticalmass:plant_screen[facing=north]",
         "/setblock 40 -60 50 criticalmass:control_console[facing=east]",
         "/setblock 61 -59 41 minecraft:stone"]
lines = ["REACTOR  RUNNING", "Core     412 C", "Coolant  120 C", "Pressure 100 bar", "Flow     640 mB/t", "Power    554 HU/t", "NEUTRON  74 %", "Period   999 s", "Xenon    87 %"]
power = ["/data merge block 42 -57 43 {Energy:200000,Lines:[%s]}" % ",".join('"%s"' % s for s in lines)]

steps = [{"cmds": setup, "wait": 40, "gui": False, "camera": "first"},
         {"cmds": build, "wait": 30},
         {"cmds": power, "wait": 30},
         {"cmds": [look(40.5, -60, 37.2, 40.5, -58.4, 41.5)], "wait": 30, "closeScreen": True}, {"shot": "front", "wait": 2},
         {"cmds": ["/setblock 40 -58 38 minecraft:barrier", look(40.5, -57, 38.5, 40.5, -58.8, 40.9)], "wait": 30}, {"shot": "desk", "wait": 2},
         {"cmds": ["/setblock 42 -58 38 minecraft:barrier", look(42.5, -57, 38.9, 42.3, -58.8, 40.8)], "wait": 30}, {"shot": "left", "wait": 2},
         {"cmds": ["/setblock 38 -58 38 minecraft:barrier", look(38.5, -57, 38.9, 38.7, -58.8, 40.8)], "wait": 30}, {"shot": "right", "wait": 2},
         {"cmds": ["/setblock 36 -59 39 minecraft:barrier", look(36.3, -58, 39.5, 40.0, -59.0, 41.0)], "wait": 30}, {"shot": "side", "wait": 2},
         {"cmds": ["/fill 36 -59 38 42 -58 39 minecraft:air"], "wait": 5, "slot": 1}]
for c in [(3, 4.5, 23), (3, 11.5, 10), (4, 11.5, 23), (4, 4.5, 10), (1, 8, 10), (0, 8, 10)]:
    steps += click(*c)
steps += click(0, 8, 23)[:1] + [{"use": [42, -59, 41, "up"], "wait": 2}, {"shot": "pressed", "wait": 10}]
steps += [{"cmds": ["/setblock 39 -58 38 minecraft:barrier", look(39.0, -57, 38.9, 39.0, -58.8, 40.8)], "wait": 30}, {"shot": "toggled", "wait": 2},
          {"cmds": ["/data get block 40 -60 40", "/item replace entity @p hotbar.0 with criticalmass:control_console", look(50.5, -60, 37.5, 50.5, -61.0, 40.5)], "wait": 20, "slot": 0},
          {"use": [50, -61, 40, "up"], "wait": 20},
          {"cmds": [look(50.5, -60, 36.5, 50.5, -59.0, 41.0)], "wait": 20}, {"shot": "placed", "wait": 2},
          {"cmds": ["/item replace entity @p hotbar.0 with criticalmass:control_console", look(60.5, -60, 36.5, 60.5, -61.0, 40.5)], "wait": 20, "gui": True},
          {"use": [60, -61, 40, "up"], "wait": 1}, {"shot": "blocked", "wait": 2},
          {"cmds": ["/setblock 51 -59 41 minecraft:air destroy"], "wait": 20, "gui": False},
          {"cmds": [look(50.5, -60, 36.5, 50.5, -59.0, 41.0)], "wait": 20}, {"shot": "removed", "wait": 2},
          {"cmds": ["/setblock 44 -58 53 minecraft:barrier", look(44.5, -57, 53.5, 40.5, -59.0, 50.5)], "wait": 30}, {"shot": "east", "wait": 2}]
json.dump({"name": "console", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "console.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
