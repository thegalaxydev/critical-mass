import json
import os

EYE = 1.62
P = "criticalmass:cooling_tower_packing"


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px, py, pz, tx, ty - EYE, tz)


def water(x, y, z, amount):
    return "/data merge block %d %d %d {Buffer:{Fluid:{id:\"minecraft:water\",amount:%d},Heat:0.0d}}" % (x, y, z, amount)


build = [
    "/fill 34 -59 34 54 -50 50 minecraft:air",
    "/setblock 38 -59 40 %s" % P, water(38, -59, 40, 250),
    "/fill 41 -59 40 41 -58 40 %s" % P, water(41, -59, 40, 500), water(41, -58, 40, 100),
    "/fill 44 -59 40 44 -57 40 %s" % P, water(44, -59, 40, 500), water(44, -58, 40, 400),
    "/setblock 44 -60 40 create:creative_motor[facing=up]{ScrollValue:32}",
    "/setblock 47 -59 40 create:creative_motor[facing=up]{ScrollValue:64}",
    "/fill 47 -58 40 47 -55 40 %s" % P, water(47, -58, 40, 500), water(47, -57, 40, 500), water(47, -56, 40, 500), water(47, -55, 40, 350),
    "/setblock 47 -54 40 create:encased_fan[facing=up]",
    "/item replace entity @p hotbar.0 with %s" % P,
    "/item replace entity @p hotbar.1 with minecraft:air",
]
wide = look(43.0, -57.6, 33.5, 43.0, -57.0, 40.5)
steps = [
    {"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative", "/tp @p 42 -60 34"], "wait": 40, "gui": False, "camera": "first"},
    {"cmds": build, "wait": 60},
    {"cmds": ["/setblock 43 -59 33 minecraft:barrier", wide], "wait": 40, "slot": 1}, {"shot": "wide", "wait": 2},
    {"cmds": ["/setblock 39 -59 38 minecraft:barrier", look(39.9, -57.9, 38.2, 38.5, -58.4, 40.5)], "wait": 30}, {"shot": "single", "wait": 2},
    {"cmds": ["/setblock 45 -58 37 minecraft:barrier", look(45.8, -56.9, 37.8, 44.5, -57.4, 40.5)], "wait": 30}, {"shot": "stack", "wait": 3}, {"shot": "stack_b", "wait": 2},
    {"cmds": ["/setblock 49 -55 38 minecraft:barrier", look(49.6, -53.9, 38.2, 47.5, -54.6, 40.5)], "wait": 30}, {"shot": "fan_a", "wait": 4}, {"shot": "fan_b", "wait": 2},
    {"cmds": ["/data get block 47 -54 40 Speed", "/data get block 47 -56 40 Speed", "/data get block 44 -57 40 Speed", "/data get block 44 -58 40", "/criticalmass ledger"], "wait": 5},
    {"cmds": [wide], "slot": 0, "gui": True, "wait": 20}, {"shot": "held", "wait": 2},
]
json.dump({"name": "packing", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "packing.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
