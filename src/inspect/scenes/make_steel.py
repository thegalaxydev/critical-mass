import json
import os

EYE = 1.62


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px, py, pz, tx, ty - EYE, tz)


items = ["steel_ingot", "steel_nugget", "steel_plate", "reactor_alloy_ingot", "reactor_alloy_sheet", "steel_block", "reactor_alloy_block"]
build = ["/fill 36 -59 36 52 -55 44 minecraft:air",
         "/setblock 43 -59 40 criticalmass:steel_block",
         "/setblock 45 -59 40 criticalmass:reactor_alloy_block",
         "/setblock 44 -59 40 criticalmass:reactor_vessel_casing"]
build += ["/item replace entity @p hotbar.%d with criticalmass:%s" % (i, n) for i, n in enumerate(items)]
steps = [
    {"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative", "/tp @p 44 -60 34"], "wait": 40, "gui": False, "camera": "first"},
    {"cmds": build, "wait": 30},
    {"cmds": ["/setblock 44 -58 37 minecraft:barrier", look(44.5, -57.0, 37.5, 44.5, -58.5, 40.5)], "wait": 30},
    {"slot": 0, "gui": True, "wait": 10}, {"shot": "blocks_and_hotbar", "wait": 2},
    {"jei": "criticalmass:steel_ingot", "wait": 30}, {"shot": "jei_steel_ingot", "wait": 2}, {"closeScreen": True, "wait": 5},
    {"jei": "criticalmass:reactor_alloy_ingot", "wait": 30}, {"shot": "jei_reactor_alloy", "wait": 2}, {"closeScreen": True, "wait": 5},
    {"jei": "criticalmass:steel_plate", "wait": 30}, {"shot": "jei_steel_plate", "wait": 2}, {"closeScreen": True, "wait": 5},
]
json.dump({"name": "steel", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "steel.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
