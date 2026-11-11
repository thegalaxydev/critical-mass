import json
import os

EYE = 1.62


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px, py, pz, tx, ty - EYE, tz)


C = "criticalmass:sealed_waste_canister"
build = ["/fill 36 -59 36 52 -55 44 minecraft:air",
         "/setblock 42 -59 40 " + C,
         "/setblock 43 -59 40 " + C,
         "/setblock 43 -58 40 " + C,
         "/setblock 41 -59 40 criticalmass:shielded_cask",
         "/setblock 46 -59 40 criticalmass:open_waste_canister",
         "/item replace entity @p hotbar.0 with " + C,
         "/item replace entity @p hotbar.1 with minecraft:air"]
front = look(44.5, -57.0, 36.5, 44.0, -58.5, 40.5)
top = look(44.5, -55.0, 38.5, 44.0, -59.0, 40.5)
steps = [
    {"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode survival", "/tp @p 44 -60 34"], "wait": 40, "gui": False, "camera": "first"},
    {"cmds": build, "wait": 30},
    {"cmds": ["/setblock 44 -58 36 minecraft:barrier", front], "wait": 30},
    {"slot": 0, "wait": 2}, {"use": [45, -60, 40, "up"], "wait": 10},
    {"gui": True, "wait": 5}, {"shot": "front_and_hotbar", "wait": 2},
    {"cmds": ["/setblock 44 -56 38 minecraft:barrier", top], "wait": 20}, {"shot": "from_above", "wait": 2},
    {"cmds": ["/item replace entity @p hotbar.0 with " + C, "/item replace entity @p hotbar.1 with criticalmass:open_waste_canister"], "wait": 10}, {"shot": "hotbar_icon", "wait": 2},
    {"jei": "criticalmass:sealed_waste_canister", "wait": 30}, {"shot": "jei_sealed", "wait": 2}, {"closeScreen": True, "wait": 5},
    {"jei": "criticalmass:open_waste_canister", "wait": 30}, {"shot": "jei_open", "wait": 2}, {"closeScreen": True, "wait": 5},
]
json.dump({"name": "canister", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "canister.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
