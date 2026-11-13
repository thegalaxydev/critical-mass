import json
import os

EYE = 1.62


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px, py, pz, tx, ty - EYE, tz)


build = [
    "/fill 34 -59 34 52 -52 50 minecraft:air",
    "/setblock 40 -57 40 criticalmass:radiator_panel",
    "/setblock 40 -56 40 criticalmass:reinforced_pipe", "/setblock 40 -55 40 criticalmass:reinforced_pipe",
    "/setblock 40 -58 40 criticalmass:reinforced_pipe", "/setblock 40 -59 40 criticalmass:reinforced_pipe",
    "/setblock 40 -56 40 minecraft:air", "/setblock 40 -56 40 criticalmass:reinforced_pipe",
    "/setblock 40 -58 40 minecraft:air", "/setblock 40 -58 40 criticalmass:reinforced_pipe",
    "/setblock 44 -57 40 criticalmass:radiator_panel",
    "/setblock 44 -56 40 create:fluid_pipe", "/setblock 44 -55 40 create:fluid_pipe",
    "/setblock 44 -58 40 create:fluid_pipe", "/setblock 44 -59 40 create:fluid_pipe",
    "/setblock 44 -56 40 minecraft:air", "/setblock 44 -56 40 create:fluid_pipe",
    "/setblock 44 -58 40 minecraft:air", "/setblock 44 -58 40 create:fluid_pipe",
    "/setblock 48 -59 40 criticalmass:radiator_panel",
    "/setblock 37 -58 40 minecraft:stone", "/setblock 37 -57 40 criticalmass:reinforced_pipe", "/setblock 37 -56 40 criticalmass:reinforced_pipe",
    "/item replace entity @p hotbar.0 with criticalmass:radiator_panel",
    "/item replace entity @p hotbar.1 with minecraft:air",
]
wide = look(43.0, -55.6, 35.2, 43.0, -57.0, 40.5)
close = look(41.9, -55.9, 37.8, 40.5, -56.6, 40.5)
steps = [
    {"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative", "/tp @p 42 -60 34"], "wait": 40, "gui": False, "camera": "first"},
    {"cmds": build, "wait": 40},
    {"cmds": ["/setblock 43 -57 35 minecraft:barrier", wide], "wait": 30, "slot": 1}, {"shot": "wide", "wait": 2},
    {"cmds": ["/setblock 41 -57 37 minecraft:barrier", close], "wait": 30}, {"shot": "close", "wait": 2},
    {"cmds": ["/setblock 38 -57 37 minecraft:barrier", look(38.9, -55.6, 37.8, 37.5, -56.4, 40.5)], "wait": 30}, {"shot": "plain", "wait": 2},
    {"cmds": ["/setblock 41 -59 37 minecraft:barrier", look(41.9, -57.4, 37.8, 40.5, -57.6, 40.5)], "wait": 30}, {"shot": "below", "wait": 2},
    {"cmds": [wide], "slot": 0, "gui": True, "wait": 20}, {"shot": "held", "wait": 2},
]
json.dump({"name": "radiator", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "radiator.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
