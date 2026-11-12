import json
import math
import os

import make_reactor_datapack as m

EYE = 1.62
SLOPE = math.tan(math.radians(22.5))
STRETCH = 1 / math.cos(math.radians(22.5))
BASE = (16 + 1.2) / 16.0
OX, OY, OZ = 300, -60, 300
CX, CY, CZ = m.CONSOLE
HOME = "/tp @p %d %d %d" % (OX, OY, OZ)


def look(pos, target):
    return "/tp @p %.2f %.2f %.2f facing %.3f %.3f %.3f" % (OX + pos[0], OY + pos[1], OZ + pos[2], OX + target[0], OY + target[1] - EYE, OZ + target[2])


def ctl(column, u, v):
    across = column * 16 + u
    depth = v / STRETCH / 16
    return (CX + 3 - across / 16.0, CY + BASE + depth * SLOPE, CZ + depth)


def click(column, u, v, wait=3):
    x, y, z = ctl(column, u, v)
    return [{"cmds": [look((x, 0, CZ - 1.4), (x, y, z))], "wait": 12}, {"use": [OX + CX, OY + CY, OZ + CZ, "up"], "wait": wait}]


def ledger(wait):
    return {"cmds": ["/criticalmass ledger"], "wait": wait}


wide = look((CX + 0.5, 1, 5.5), (CX + 0.5, 3.0, 12.0))
desk = look((CX + 0.5, 3, 7.6), (CX + 0.5, 1.4, 9.9))
wall = look((CX + 0.5, 0, 8.4), (CX + 0.5, 4.2, 12.0))
steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/weather clear", "/gamemode creative", "/clear @p", HOME], "wait": 60, "gui": False, "camera": "first", "slot": 1},
         {"cmds": ["/" + c for c in m.place], "wait": 100},
         {"cmds": ["/" + c for c in m.start_here], "wait": 200},
         {"cmds": [wide], "wait": 40, "closeScreen": True}, {"shot": "shutdown", "wait": 2},
         {"cmds": [HOME], "wait": 10},
         {"cmds": ["/criticalmass ledger"] + ["/" + c for c in m.run_here], "wait": 400},
         ledger(5),
         {"cmds": [wide], "wait": 40}, {"shot": "running", "wait": 2},
         {"cmds": [wall], "wait": 40}, {"shot": "wall", "wait": 2},
         {"cmds": ["/setblock %d %d %d minecraft:barrier" % (OX + CX, OY + 2, OZ + 7), desk], "wait": 40}, {"shot": "desk", "wait": 2},
         {"cmds": ["/setblock %d %d %d minecraft:air" % (OX + CX, OY + 2, OZ + 7)], "wait": 5}]
steps += click(0, 8, 10) + [{"cmds": ["/data get block %d %d %d" % (OX + CX, OY + CY, OZ + CZ)], "wait": 5}]
steps += click(3, 4.5, 23) + click(4, 11.5, 10)
steps += click(0, 8, 23, 40)
steps += [ledger(5), {"cmds": ["/setblock %d %d %d minecraft:barrier" % (OX + CX, OY + 2, OZ + 7), desk], "wait": 40}, {"shot": "scram_a", "wait": 6}, {"shot": "scram_b", "wait": 2},
          {"cmds": ["/setblock %d %d %d minecraft:air" % (OX + CX, OY + 2, OZ + 7)], "wait": 5}]
steps += click(2, 8, 9, 20)
steps += [{"cmds": ["/setblock %d %d %d minecraft:barrier" % (OX + CX, OY + 2, OZ + 7), desk], "wait": 40}, {"shot": "acknowledged", "wait": 2},
          {"cmds": ["/setblock %d %d %d minecraft:air" % (OX + CX, OY + 2, OZ + 7), wide], "wait": 40}, {"shot": "tripped", "wait": 2},
          {"cmds": ["/data get block %d %d %d" % (OX + CX, OY + CY, OZ + CZ)], "wait": 5}, ledger(5)]
json.dump({"name": "room", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "control_room.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
