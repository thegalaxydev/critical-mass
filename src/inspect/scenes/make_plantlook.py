import json
import os

EYE = 1.62


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px, py, pz, tx, ty - EYE, tz)


blocks = [("criticalmass:heat_exchanger[facing=south]", 40), ("criticalmass:condenser[facing=south]", 42), ("criticalmass:emergency_coolant_tank", 44),
          ("criticalmass:radiator_panel[facing=south]", 46), ("criticalmass:boiler_heating_plate", 48), ("criticalmass:relief_valve[facing=south]", 50),
          ("criticalmass:reinforced_valve[facing=up,axis_along_first=false]", 52)]
cmds = ["/setblock %d -59 40 %s" % (x, b) for b, x in blocks]
cmds += ["/fill 54 -59 40 54 -57 40 criticalmass:cooling_tower_packing"]
steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative", "/tp @p 46 -60 34"], "wait": 40, "gui": False, "camera": "first"},
         {"cmds": cmds, "wait": 20},
         {"cmds": ["/setblock 44 -57 35 minecraft:barrier", look(44.5, -56, 35.5, 44.5, -59.0, 40.5)], "wait": 30}, {"shot": "a", "wait": 2},
         {"cmds": ["/setblock 51 -57 35 minecraft:barrier", look(51.5, -56, 35.5, 51.5, -59.0, 40.5)], "wait": 30}, {"shot": "b", "wait": 2},
         {"cmds": ["/setblock 47 -56 45 minecraft:barrier", look(47.5, -55, 45.5, 46.0, -59.0, 40.5)], "wait": 30}, {"shot": "back", "wait": 2}]
json.dump({"name": "plantlook", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "plantlook.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
