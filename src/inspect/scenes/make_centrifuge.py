import json
import os

EYE = 1.62


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px, py, pz, tx, ty - EYE, tz)


build = [
    "/fill 36 -59 36 52 -54 44 minecraft:air",
    "/setblock 40 -59 40 minecraft:smooth_stone",
    "/setblock 40 -58 40 criticalmass:centrifuge",
    "/setblock 40 -57 40 criticalmass:centrifuge",
    "/setblock 40 -56 40 create:creative_motor[facing=down]{ScrollValue:64}",
    "/setblock 44 -59 40 criticalmass:centrifuge",
    "/item replace entity @p hotbar.0 with criticalmass:uranium_concentrate 8",
    "/item replace entity @p hotbar.1 with criticalmass:centrifuge",
    "/item replace entity @p hotbar.2 with criticalmass:enriched_uranium",
    "/item replace entity @p hotbar.3 with criticalmass:depleted_uranium",
    "/item replace entity @p hotbar.4 with minecraft:air",
    "/item replace entity @p armor.head with create:goggles",
]
overview = look(42.0, -57.0, 36.5, 42.0, -58.0, 40.5)
close = look(40.5, -57.0, 37.6, 40.5, -57.5, 40.5)
contents = ["/data get block 40 -57 40 Items", "/data get block 40 -57 40 Status"]
steps = [
    {"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative", "/tp @p 42 -60 34"], "wait": 40, "gui": False, "camera": "first"},
    {"cmds": build, "wait": 30},
    {"cmds": ["/setblock 42 -58 36 minecraft:barrier", overview], "wait": 30}, {"shot": "overview", "wait": 2},
    {"cmds": ["/setblock 40 -58 37 minecraft:barrier", close], "wait": 20},
    {"slot": 0, "wait": 2}, {"use": [40, -57, 40, "north"], "wait": 20, "gui": True},
    {"cmds": contents, "wait": 10}, {"shot": "too_slow", "wait": 2},
    {"cmds": ["/setblock 40 -56 40 minecraft:air", "/setblock 40 -56 40 create:creative_motor[facing=down]{ScrollValue:256}"], "wait": 230},
    {"cmds": contents, "wait": 10}, {"shot": "enriched", "wait": 2},
    {"slot": 4, "wait": 2}, {"use": [40, -57, 40, "north"], "wait": 10},
    {"cmds": contents + ["/data get entity @p Inventory"], "wait": 10}, {"shot": "collected", "wait": 2},
]
json.dump({"name": "centrifuge", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "centrifuge.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
