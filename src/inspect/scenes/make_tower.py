import json
import os

import make_reactor_datapack as m

OX, OY, OZ = 300, -60, 300
EYE = 1.62


def at(pos):
    return "%d %d %d" % (OX + pos[0], OY + pos[1], OZ + pos[2])


def look(pos, target):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (OX + pos[0], OY + pos[1], OZ + pos[2], OX + target[0], OY + target[1] - EYE, OZ + target[2])


HOME = "/tp @p %d %d %d" % (OX, OY, OZ)
state = ["/data get block %s Speed" % at((8, 6, 9)), "/data get block %s Speed" % at(m.FILL[0]), "/criticalmass ledger"]
steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/weather clear", "/gamemode creative", "/clear @p", HOME], "wait": 60, "gui": False, "camera": "first", "slot": 1},
         {"cmds": ["/" + c for c in m.place], "wait": 100},
         {"cmds": ["/" + c for c in m.start_here], "wait": 200},
         {"cmds": state, "wait": 5},
         {"cmds": ["/" + c for c in m.run_here], "wait": 1200},
         {"cmds": state, "wait": 5},
         {"cmds": ["/setblock %s minecraft:barrier" % at((6, 2, 11)), look((6.5, 3.0, 11.5), (7.2, 3.6, 9.5))], "wait": 40}, {"shot": "makeup", "wait": 2},
         {"cmds": ["/setblock %s minecraft:barrier" % at((10, 3, 13)), look((10.5, 4.0, 13.5), (8.5, 4.5, 9.5))], "wait": 40}, {"shot": "tower", "wait": 2},
         {"cmds": ["/setblock %s minecraft:barrier" % at((11, 6, 12)), look((11.5, 7.0, 12.5), (8.5, 5.6, 9.5))], "wait": 40}, {"shot": "fan", "wait": 2},
         {"cmds": ["/setblock %s minecraft:barrier" % at((10, 1, 12)), look((10.5, 2.0, 12.5), (8.5, 3.8, 9.5))], "wait": 40}, {"shot": "column", "wait": 2},
         {"cmds": [HOME], "wait": 600},
         {"cmds": state, "wait": 5}, {"wait": 2}]
json.dump({"name": "tower", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "tower.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
