import json
import os
from reactor_layout import CONTROLLER, at, loop, start, vessel

O = (300, -60, 300)
w = at(O)
EYE = 1.62


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px + O[0], py + O[1], pz + O[2], tx + O[0], ty + O[1] - EYE, tz + O[2])


def shot(name, stand, target):
    return [{"cmds": ["/setblock %s minecraft:barrier" % w(stand[0], stand[1] - 1, stand[2]), look(stand[0] + 0.5, stand[1], stand[2] + 0.5, *target)], "wait": 25},
            {"shot": name, "wait": 2}]


pipes, fix = loop(O)
drives = ["/setblock %s criticalmass:control_rod_drive[facing=west]" % w(3, 6, z) for z in (2, 4)]
drives += ["/setblock %s create:creative_motor[facing=west]" % w(4, 6, z) for z in (2, 4)]
withdraw = ["/setblock %s minecraft:redstone_block" % w(2, 6, z) for z in (2, 4)]
tank = []
controller_view = ((-2, 0, 3), (1.0, 3.93, 3.12))
drive_view = ((3, 7, -1), (3.5, 6.5, 2.5))
L = {"cmds": ["/criticalmass ledger"], "wait": 2}
steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative", "/tp @p %d -55 %d" % (O[0] - 6, O[2] - 6)], "wait": 60, "gui": True, "camera": "first"},
         {"cmds": ["/item replace entity @p armor.head with create:goggles", "/fill %s %s minecraft:smooth_stone" % (w(-3, -1, -3), w(10, -1, 10))], "wait": 10},
         {"cmds": vessel(O) + pipes + drives + tank, "wait": 10}, {"cmds": fix, "wait": 10},
         {"cmds": start(O, motor=True, power=False), "wait": 40}, L]
steps += shot("cold", *controller_view)
steps += [{"cmds": withdraw, "wait": 30}, L]
steps += shot("drive_moving", *drive_view)
steps += shot("starting", *controller_view)
steps += [{"wait": 250}, L]
steps += shot("running", *controller_view)
steps += shot("drive_holding", *drive_view)
steps += [{"note": "SECTION lost flow"}, {"cmds": ["/setblock %s minecraft:air" % w(4, 2, 6)], "wait": 60}, L]
steps += shot("tripped", *controller_view)
steps += shot("drive_failsafe", *drive_view)
steps += shot("tank", (-2, 2, 0), (0.5, 2.5, 2.5))
steps += [{"note": "SECTION breach"}, {"cmds": ["/data merge block %s {Integrity:[{P:[I;4,-1,-1],V:0.0d}]}" % w(*CONTROLLER)], "wait": 40}, L]
steps += shot("breached", *controller_view)
steps += shot("breach_crack", (8, 2, 2), (5.5, 2.5, 2.5))
steps += [{"wait": 5}]
json.dump({"name": "operation", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "operation.json"), "w", newline="\n"), indent=1)
print("operation", len(steps), "steps")
