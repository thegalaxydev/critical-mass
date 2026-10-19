import json
import os
from reactor_layout import at, loop, start, vessel

O = (10, -61, 10)
w = at(O)
EYE = 1.62


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px + O[0], py + O[1], pz + O[2], tx + O[0], ty + O[1] - EYE, tz + O[2])


mistakes = ["/setblock %s minecraft:air" % w(2, 5, 4), "/setblock %s criticalmass:primary_coolant_port[facing=north,mode=inlet]" % w(4, 5, 1),
            "/setblock %s minecraft:stone" % w(2, 4, 4)]
repairs = ["/setblock %s criticalmass:reactor_vessel_casing" % w(2, 5, 4), "/setblock %s criticalmass:reactor_vessel_casing" % w(4, 5, 1),
           "/setblock %s criticalmass:coolant_channel" % w(2, 4, 4)]
pipes, fix = loop(O)
steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative", "/item replace entity @p armor.head with create:goggles"], "wait": 10, "gui": True, "camera": "first"},
         {"cmds": vessel(O) + mistakes, "wait": 20},
         {"cmds": ["/setblock %s minecraft:barrier" % w(-2, 7, -2), look(-1.5, 8.0, -1.5, 3.5, 4.5, 3.5)], "wait": 40},
         {"shot": "invalid_overview", "wait": 5},
         {"cmds": [look(-1.6, 1.0, 3.2, 1.0, 3.93, 3.12)], "wait": 30},
         {"shot": "invalid_tooltip", "wait": 5},
         {"cmds": ["/criticalmass ledger"], "wait": 2},
         {"cmds": repairs + pipes, "wait": 10}, {"cmds": fix, "wait": 10},
         {"cmds": start(O), "wait": 300},
         {"cmds": ["/criticalmass ledger", look(-1.6, 1.0, 3.2, 1.0, 3.93, 3.12)], "wait": 30},
         {"shot": "running_tooltip", "wait": 5},
         {"cmds": [look(-1.5, 8.0, -1.5, 3.5, 4.5, 3.5)], "wait": 30},
         {"shot": "running_overview", "wait": 1900},
         {"cmds": ["/criticalmass ledger", look(-1.6, 1.0, 3.2, 1.0, 3.93, 3.12)], "wait": 30},
         {"shot": "hot_spot_tooltip", "wait": 5},
         {"cmds": ["/setblock %s minecraft:air" % w(5, 3, 2)], "wait": 5},
         {"cmds": ["/criticalmass ledger"], "wait": 30},
         {"shot": "tripped_tooltip", "wait": 5}]
json.dump({"name": "reactor", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "reactor.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
