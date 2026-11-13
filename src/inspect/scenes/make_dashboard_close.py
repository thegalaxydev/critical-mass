import json
import os

EYE = 1.62
D = "criticalmass:dashboard"


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px, py, pz, tx, ty - EYE, tz)


build = [
    "/fill 34 -59 34 54 -50 50 minecraft:air",
    "/setblock 40 -59 40 %s[facing=north]" % D,
    "/setblock 44 -58 40 %s[facing=north]" % D,
    "/setblock 47 -59 40 %s[facing=north,join_cw=true]" % D,
    "/setblock 48 -59 40 %s[facing=north,join_ccw=true]" % D,
    "/setblock 40 -59 38 minecraft:barrier", "/setblock 40 -58 38 minecraft:barrier",
    "/item replace entity @p hotbar.0 with minecraft:air",
]
views = [
    ("front_east", look(41.6, -60.0, 38.9, 40.5, -58.5, 40.4)),
    ("front_west", look(39.4, -60.0, 38.9, 40.5, -58.5, 40.4)),
    ("side_east", look(42.0, -60.0, 40.5, 40.5, -58.5, 40.5)),
    ("side_west", look(39.0, -60.0, 40.5, 40.5, -58.5, 40.5)),
    ("back_east", look(41.6, -60.0, 42.0, 40.5, -58.5, 40.5)),
    ("above", look(40.5, -57.0, 38.9, 40.5, -58.3, 40.4)),
    ("under", look(45.4, -60.0, 38.9, 44.5, -57.5, 40.4)),
    ("under_side", look(46.0, -60.0, 40.2, 44.6, -57.5, 40.5)),
    ("pair", look(48.0, -60.0, 38.6, 48.0, -58.5, 40.4)),
]
steps = [
    {"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/weather clear", "/gamemode creative", "/tp @p 42 -60 36"], "wait": 40, "gui": False, "camera": "first"},
    {"cmds": build, "wait": 40},
]
for name, command in views:
    steps += [{"cmds": [command], "wait": 20}, {"shot": name, "wait": 2}]
json.dump({"name": "dashclose", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "dashboard_close.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
