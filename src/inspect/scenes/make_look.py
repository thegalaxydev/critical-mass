import json
import os
from reactor_layout import at, vessel

O = (40, -60, 40)
w = at(O)
EYE = 1.62


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px, py, pz, tx, ty - EYE, tz)


cmds = vessel(O)[:-1] + ["/setblock %s criticalmass:reactor_signal_interface[facing=west]" % w(1, 4, 2),
                         "/fill %s %s criticalmass:shielding_concrete" % (w(-3, 1, -1), w(-3, 5, 7)),
                         "/fill %s %s criticalmass:shielded_glass" % (w(-3, 3, 2), w(-3, 4, 4)),
                         "/setblock %s minecraft:air" % w(-3, 1, 5), "/setblock %s minecraft:air" % w(-3, 2, 5),
                         "/setblock %s criticalmass:shielded_hatch[facing=east,half=bottom,open=false]" % w(-3, 3, 5)]
steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative", "/tp @p 30 -60 44"], "wait": 40, "gui": False, "camera": "first"},
         {"cmds": cmds, "wait": 20},
         {"cmds": ["/setblock 38 -57 47 minecraft:barrier", look(38.5, -56, 47.5, 41.0, -57.0, 43.0)], "wait": 30}, {"shot": "west", "wait": 2},
         {"cmds": ["/setblock 47 -54 49 minecraft:barrier", look(47.5, -53, 49.5, 43.0, -55.0, 43.0)], "wait": 30}, {"shot": "top", "wait": 2},
         {"cmds": ["/setblock 45 -57 49 minecraft:barrier", look(45.5, -56, 49.5, 43.0, -57.5, 45.0)], "wait": 30}, {"shot": "south", "wait": 2},
         {"cmds": ["/setblock 34 -58 44 minecraft:barrier", look(34.5, -57, 44.5, 37.0, -57.5, 44.0)], "wait": 30}, {"shot": "wall", "wait": 2}]
json.dump({"name": "look", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "look.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
