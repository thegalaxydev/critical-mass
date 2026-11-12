import json
import os

EYE = 1.62


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px, py, pz, tx, ty - EYE, tz)


row1 = ["criticalmass:circulation_pump[facing=east,axis_along_first=true]", "criticalmass:steam_turbine[axis=x]",
        "criticalmass:control_rod_drive[facing=north]", "criticalmass:fuel_channel", "criticalmass:control_channel", "criticalmass:coolant_channel"]
row2 = ["criticalmass:fuel_assembly_bench[facing=south]", "criticalmass:spent_fuel_rack", "criticalmass:shielded_cask",
        "criticalmass:alarm_siren[facing=south]", "minecraft:air", "criticalmass:docking_service_connector[facing=south]"]
cmds = ["/setblock %d -59 40 %s" % (40 + 2 * i, b) for i, b in enumerate(row1)]
cmds += ["/setblock %d -59 44 %s" % (40 + 2 * i, b) for i, b in enumerate(row2)]
cmds += ["/setblock 46 -58 40 create:creative_motor[facing=down]", "/setblock 40 -59 39 create:creative_motor[facing=south]"]
steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative", "/tp @p 45 -60 34"], "wait": 40, "gui": False, "camera": "first"},
         {"cmds": cmds, "wait": 30},
         {"cmds": ["/setblock 43 -57 35 minecraft:barrier", look(43.5, -56, 35.5, 43.5, -59.0, 40.5)], "wait": 30}, {"shot": "row1a", "wait": 2},
         {"cmds": ["/setblock 49 -57 35 minecraft:barrier", look(49.5, -56, 35.5, 49.5, -59.0, 40.5)], "wait": 30}, {"shot": "row1b", "wait": 2},
         {"cmds": ["/setblock 43 -57 49 minecraft:barrier", look(43.5, -56, 49.5, 43.5, -59.0, 44.5)], "wait": 30}, {"shot": "row2a", "wait": 2},
         {"cmds": ["/setblock 49 -57 49 minecraft:barrier", look(49.5, -56, 49.5, 49.5, -59.0, 44.5)], "wait": 30}, {"shot": "row2b", "wait": 2},
         {"cmds": ["/setblock 45 -55 42 minecraft:barrier", look(45.5, -54, 42.5, 44.0, -59.0, 40.5)], "wait": 30}, {"shot": "top1", "wait": 2}]
json.dump({"name": "restlook", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "restlook.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
