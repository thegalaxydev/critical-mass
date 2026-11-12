import json
import os

EYE = 1.62


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px, py, pz, tx, ty - EYE, tz)


build = [
    "/fill 34 -59 34 52 -54 50 minecraft:air",
    "/setblock 40 -59 40 create:creative_motor[facing=up]{ScrollValue:32}",
    "/setblock 40 -58 40 criticalmass:circulation_pump[facing=east,axis_along_first=true]",
    "/setblock 39 -58 40 criticalmass:reinforced_pipe",
    "/setblock 38 -58 40 criticalmass:reinforced_pipe",
    "/setblock 41 -58 40 criticalmass:reinforced_pipe",
    "/setblock 42 -58 40 criticalmass:reinforced_pipe",
    "/setblock 39 -58 40 minecraft:air", "/setblock 39 -58 40 criticalmass:reinforced_pipe",
    "/setblock 41 -58 40 minecraft:air", "/setblock 41 -58 40 criticalmass:reinforced_pipe",
    "/setblock 45 -59 40 create:creative_motor[facing=up]{ScrollValue:32}",
    "/setblock 45 -58 40 criticalmass:circulation_pump[facing=north,axis_along_first=true]",
    "/setblock 48 -59 40 create:creative_motor[facing=up]{ScrollValue:32}",
    "/setblock 48 -58 40 criticalmass:circulation_pump[facing=north,axis_along_first=false]",
    "/setblock 45 -58 44 criticalmass:circulation_pump[facing=up,axis_along_first=true]",
    "/setblock 46 -58 44 create:creative_motor[facing=west]{ScrollValue:32}",
    "/item replace entity @p hotbar.0 with criticalmass:circulation_pump",
    "/item replace entity @p hotbar.1 with minecraft:air",
]
side = look(41.5, -56.6, 36.4, 40.5, -57.4, 40.5)
wide = look(44.5, -56.0, 34.5, 44.5, -57.6, 41.0)
steps = [
    {"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative", "/tp @p 42 -60 34"], "wait": 40, "gui": False, "camera": "first"},
    {"cmds": build, "wait": 40},
    {"cmds": ["/setblock 41 -58 36 minecraft:barrier", side], "wait": 30, "slot": 1}, {"shot": "side_a", "wait": 3}, {"shot": "side_b", "wait": 2},
    {"cmds": ["/setblock 44 -57 34 minecraft:barrier", wide], "wait": 30}, {"shot": "wide", "wait": 2},
    {"cmds": ["/data get block 40 -58 40 Speed", "/data get block 45 -58 40 Speed", "/data get block 48 -58 40 Speed", "/data get block 45 -58 44 Speed"], "wait": 5},
    {"slot": 0, "gui": True, "wait": 10}, {"shot": "held", "wait": 2},
]
json.dump({"name": "pump", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "pump.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
