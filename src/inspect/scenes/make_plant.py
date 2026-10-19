import json
import os
from reactor_layout import EXCHANGER, PLANT_TURBINE, at, plant, start, vessel

O = (600, -60, 600)
w = at(O)
EYE = 1.62


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px + O[0], py + O[1], pz + O[2], tx + O[0], ty + O[1] - EYE, tz + O[2])


cmds, fix, fill = plant(O)
L = {"cmds": ["/criticalmass ledger"], "wait": 2}
steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative", "/tp @p %d -59 %d" % (O[0] - 6, O[2] - 6)], "wait": 60, "gui": True, "camera": "first"},
         {"cmds": ["/item replace entity @p armor.head with create:goggles", "/fill %s %s minecraft:smooth_stone" % (w(-2, -1, -2), w(11, -1, 9))], "wait": 10},
         {"cmds": vessel(O) + cmds, "wait": 10}, {"cmds": fix, "wait": 10},
         {"cmds": start(O) + fill, "wait": 600}, L,
         {"cmds": ["/setblock %s minecraft:barrier" % w(6, 2, 10), look(6.5, 3.0, 10.5, 6.5, 3.5, 6.5)], "wait": 30},
         {"shot": "exchanger", "wait": 5},
         {"cmds": ["/setblock %s minecraft:barrier" % w(6, 5, 10), look(6.5, 6.0, 10.5, 6.5, 6.5, 6.5)], "wait": 30},
         {"shot": "turbine", "wait": 5},
         {"cmds": ["/setblock %s minecraft:barrier" % w(8, 5, 10), look(8.5, 6.0, 10.5, 8.5, 6.5, 6.5)], "wait": 30},
         {"shot": "stressometer", "wait": 5},
         {"cmds": ["/setblock %s minecraft:barrier" % w(13, 9, 13), look(13.5, 10.0, 13.5, 4.0, 3.0, 3.0)], "wait": 30},
         {"shot": "overview", "wait": 5},
         {"note": "SECTION freeze"}, L,
         {"cmds": ["/tp @p %d 100 %d" % (O[0] + 3000, O[2] + 3000)], "wait": 400}, L,
         {"cmds": [look(13.5, 10.0, 13.5, 4.0, 3.0, 3.0)], "wait": 1}, L, {"wait": 40}, L, {"wait": 5}]
json.dump({"name": "plant", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "plant.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
