import json
import os

EYE = 1.62


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px, py, pz, tx, ty - EYE, tz)


build = [
    "/fill 36 -59 36 52 -54 44 minecraft:air",
    "/setblock 39 -59 40 create:fluid_tank",
    "/setblock 40 -59 40 create:fluid_pipe",
    "/setblock 41 -59 40 create:fluid_pipe",
    "/setblock 42 -59 40 create:fluid_valve[facing=up,axis_along_first=false]",
    "/setblock 43 -59 40 create:fluid_pipe",
    "/setblock 44 -59 40 create:fluid_pipe",
    "/setblock 44 -58 40 create:fluid_pipe",
    "/setblock 45 -59 40 create:fluid_tank",
    "/setblock 40 -59 40 minecraft:air", "/setblock 40 -59 40 create:fluid_pipe",
    "/setblock 44 -59 40 minecraft:air", "/setblock 44 -59 40 create:fluid_pipe",
    "/gamemode survival",
    "/item replace entity @p hotbar.0 with criticalmass:reactor_alloy_ingot 8",
]
overview = look(42.5, -57.4, 36.2, 42.5, -58.6, 40.5)
checks = ["/execute if block %d -59 40 criticalmass:reinforced_pipe" % x for x in (40, 41, 43, 44)] + ["/execute if block 42 -59 40 criticalmass:reinforced_valve",
                                                                                                    "/data get entity @p Inventory"]
steps = [
    {"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative", "/tp @p 42 -60 34"], "wait": 40, "gui": False, "camera": "first"},
    {"cmds": build, "wait": 30},
    {"cmds": ["/setblock 42 -59 36 minecraft:barrier", overview], "wait": 30}, {"shot": "plain", "wait": 2},
    {"slot": 0, "wait": 2},
    {"use": [41, -59, 40, "north"], "wait": 10}, {"shot": "one_pipe", "wait": 2},
    {"use": [42, -59, 40, "north"], "wait": 10},
    {"use": [44, -59, 40, "north"], "wait": 10},
    {"cmds": checks, "wait": 10}, {"shot": "reinforced", "wait": 2},
    {"cmds": ["/gamemode creative"], "wait": 5},
]
json.dump({"name": "reinforce", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "reinforce.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
