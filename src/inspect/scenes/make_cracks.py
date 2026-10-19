import json
import os
from reactor_layout import CONTROLLER, at, vessel

O = (40, -60, 40)
w = at(O)
EYE = 1.62
HEAT = (2400 - 20) * 200 * 3


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px + O[0], py + O[1], pz + O[2], tx + O[0], ty + O[1] - EYE, tz + O[2])


east = look(9.5, 0.0, 3.5, 5.5, 3.0, 3.5)
steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative", "/tp @p %d -59 %d" % (O[0] + 9, O[2] + 3)], "wait": 40, "gui": False, "camera": "first"},
         {"cmds": vessel(O), "wait": 10},
         {"cmds": [east], "wait": 20},
         {"shot": "stage0", "wait": 2},
         {"cmds": ["/data merge block %s {Core:{Fuel:[{X:3,Z:0,Heat:%.1fd}]}}" % (w(*CONTROLLER), HEAT)], "wait": 150},
         {"cmds": ["/criticalmass ledger", east], "wait": 5}, {"shot": "stage1", "wait": 120},
         {"cmds": ["/criticalmass ledger", east], "wait": 5}, {"shot": "stage2", "wait": 120},
         {"cmds": ["/criticalmass ledger", east], "wait": 5}, {"shot": "stage3", "wait": 2},
         {"cmds": ["/item replace entity @p armor.head with create:goggles", look(-1.6, 0.0, 3.2, 1.0, 3.93, 3.12)], "wait": 30, "gui": True},
         {"shot": "damaged_tooltip", "wait": 5}]
json.dump({"name": "cracks", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "cracks.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
