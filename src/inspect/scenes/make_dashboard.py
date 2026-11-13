import json
import os

EYE = 1.62
D = "criticalmass:dashboard"


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px, py, pz, tx, ty - EYE, tz)


def desk(x, z, facing, cw, ccw, parts, color="light_gray"):
    nbt = ",".join("Part%d:{%s}" % (i, p) for i, p in enumerate(parts) if p)
    return "/setblock %d -59 %d %s[facing=%s,join_cw=%s,join_ccw=%s,color=%s]{%s}" % (x, z, D, facing, str(cw).lower(), str(ccw).lower(), color, nbt)


build = [
    "/fill 34 -59 34 54 -50 50 minecraft:air",
    desk(40, 40, "north", True, False, ['Type:"button"', 'Type:"switch",On:1b']),
    desk(41, 40, "north", True, True, ['Type:"lever",Level:11', 'Type:"gauge",Fraction:0.6f,Readout:"310 C"']),
    desk(42, 40, "north", False, True, ['Type:"alarm",Alarms:1', 'Type:"alarm"']),
    desk(45, 40, "north", False, False, ['Type:"button"', None], "red"),
    desk(47, 41, "west", False, False, ['Type:"gauge",Fraction:0.2f', 'Type:"lever",Level:2'], "blue"),
    "/fill 39 -59 41 43 -57 41 criticalmass:reactor_vessel_casing",
    "/item replace entity @p hotbar.0 with criticalmass:dashboard",
    "/item replace entity @p hotbar.1 with minecraft:air",
    "/item replace entity @p hotbar.2 with criticalmass:dashboard_gauge",
    "/item replace entity @p hotbar.3 with criticalmass:dashboard_button",
    "/item replace entity @p hotbar.4 with criticalmass:dashboard_switch",
    "/item replace entity @p hotbar.5 with criticalmass:dashboard_lever",
    "/item replace entity @p hotbar.6 with criticalmass:dashboard_alarm",
    "/item replace entity @p hotbar.7 with criticalmass:dashboard[minecraft:block_state={color:\"red\"}]",
]
front = look(41.5, -58.9, 37.6, 41.5, -58.5, 40.5)
steps = [
    {"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative", "/tp @p 42 -60 34"], "wait": 40, "gui": False, "camera": "first"},
    {"cmds": build, "wait": 60},
    {"cmds": ["/setblock 41 -60 37 minecraft:barrier", front], "wait": 40, "slot": 1}, {"shot": "front", "wait": 8}, {"shot": "front_b", "wait": 2},
    {"cmds": ["/setblock 41 -59 39 minecraft:barrier", look(41.5, -58.0, 39.4, 41.5, -58.2, 40.6)], "wait": 30}, {"shot": "parts", "wait": 2},
    {"cmds": ["/setblock 41 -59 39 minecraft:air", "/setblock 43 -59 38 minecraft:barrier", look(43.8, -57.9, 38.3, 41.6, -58.4, 40.5)], "wait": 30}, {"shot": "angle", "wait": 2},
    {"cmds": ["/setblock 40 -59 43 minecraft:barrier", look(40.2, -57.9, 43.6, 41.5, -58.6, 40.5)], "wait": 30}, {"shot": "back", "wait": 2},
    {"cmds": ["/setblock 45 -60 38 minecraft:barrier", look(45.6, -58.8, 38.4, 46.4, -58.5, 40.8)], "wait": 30}, {"shot": "single", "wait": 2},
    {"cmds": [front], "slot": 0, "gui": True, "wait": 20}, {"shot": "held", "wait": 2},
    {"slot": 2, "wait": 10}, {"shot": "held_part", "wait": 2},
    {"slot": 6, "wait": 10}, {"shot": "held_alarm", "wait": 2},
]
json.dump({"name": "dashboard", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "dashboard.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
