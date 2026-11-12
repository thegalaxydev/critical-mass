import json
import os

EYE = 1.62


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px, py, pz, tx, ty - EYE, tz)


V = "criticalmass:reinforced_valve[facing=up,axis_along_first=false]"
VERTICAL = "criticalmass:reinforced_valve[facing=north,axis_along_first=true]"
build = [
    "/fill 36 -59 36 52 -55 44 minecraft:air",
    "/setblock 39 -59 40 criticalmass:reinforced_pipe",
    "/setblock 40 -59 40 " + V,
    "/setblock 41 -59 40 criticalmass:reinforced_pipe",
    "/setblock 40 -59 39 create:copper_valve_handle[facing=north]",
    "/setblock 43 -59 40 create:fluid_pipe",
    "/setblock 44 -59 40 " + V,
    "/setblock 45 -59 40 create:fluid_pipe",
    "/setblock 44 -58 40 create:fluid_pipe",
    "/setblock 48 -59 40 criticalmass:reinforced_pipe",
    "/setblock 48 -58 40 " + VERTICAL,
    "/setblock 48 -57 40 criticalmass:reinforced_pipe",
    "/setblock 47 -58 40 create:copper_valve_handle[facing=west]",
    "/item replace entity @p hotbar.0 with criticalmass:reinforced_valve",
    "/item replace entity @p hotbar.1 with minecraft:air",
]
overview = look(44.5, -56.0, 35.5, 44.5, -58.5, 40.5)
close = look(40.5, -57.0, 37.5, 40.5, -58.5, 40.5)
steps = [
    {"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative", "/tp @p 44 -60 34"], "wait": 40, "gui": False, "camera": "first"},
    {"cmds": build, "wait": 30},
    {"cmds": ["/setblock 44 -57 35 minecraft:barrier", overview], "wait": 30}, {"shot": "overview_closed", "wait": 2},
    {"cmds": ["/setblock 40 -58 37 minecraft:barrier", close], "wait": 20}, {"shot": "closeup_closed", "wait": 2},
    {"slot": 1, "wait": 2},
    {"use": [40, -59, 39, "north"], "wait": 4}, {"shot": "closeup_turning", "wait": 30},
    {"shot": "closeup_after_turn", "wait": 2},
    {"sneak": True, "wait": 2}, {"use": [40, -59, 39, "north"], "wait": 4}, {"sneak": False, "wait": 30},
    {"shot": "closeup_after_sneak_turn", "wait": 2},
    {"use": [47, -58, 40, "west"], "wait": 30},
    {"sneak": True, "wait": 2}, {"use": [47, -58, 40, "west"], "wait": 4}, {"sneak": False, "wait": 30},
    {"cmds": [overview], "wait": 20}, {"shot": "overview_after", "wait": 2},
    {"slot": 0, "gui": True, "wait": 10}, {"shot": "held", "wait": 2},
]
json.dump({"name": "valve", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "valve.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
