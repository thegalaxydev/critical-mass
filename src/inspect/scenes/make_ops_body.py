import json
import os
from body_common import write
from reactor_layout import at, loop, start, vessel

O = (400, -60, 400)
w = at(O)
pipes, fix = loop(O)
drives = ["/setblock %s criticalmass:control_rod_drive[facing=west]" % w(3, 6, z) for z in (2, 4)]
drives += ["/setblock %s create:creative_motor[facing=west]" % w(4, 6, z) for z in (2, 4)]
withdraw = ["/setblock %s minecraft:redstone_block" % w(2, 6, z) for z in (2, 4)]
L = {"cmds": ["/criticalmass ledger", "/sable info @n"], "wait": 3}
steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode spectator", "/tp @p %d -40 %d" % (O[0] + 14, O[2] - 10)], "wait": 40, "gui": False},
         {"cmds": ["/fill %s %s create:andesite_casing" % (w(-1, 0, -1), w(7, 0, 7))] + vessel(O) + pipes + drives, "wait": 10}, {"cmds": fix, "wait": 10},
         {"cmds": start(O, motor=True, power=False) + withdraw, "wait": 200}, L,
         {"cmds": ["/sable assemble area %s %s" % (w(-1, 0, -1), w(7, 7, 7))], "wait": 60}, L,
         {"cmds": ["/sable paused true", "/sable teleport @n %d -30 %d" % (O[0] + 3, O[2] + 3)], "wait": 5},
         {"cmds": ["/sable paused false", "/sable physics impulse @n angular 0 0 1500 global", "/sable physics impulse @n angular 600 0 0 global"], "wait": 160},
         {"shot": "flipped", "wait": 2}, L,
         {"note": "SECTION scram while inverted"}, {"invoke": {"offset": [0, 0, 0], "method": "scram", "far": True}, "wait": 3}, L,
         {"wait": 60}, L, {"shot": "after_scram", "wait": 5}]
write("ops_body", steps)
