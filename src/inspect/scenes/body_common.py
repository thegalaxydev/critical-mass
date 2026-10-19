import json
import os
from reactor_layout import at, loop as reactor_loop, start, vessel

HERE = os.path.dirname(os.path.abspath(__file__))
ASSEMBLER = (7, 1, 7)
ASSEMBLER_OFFSET = [6, -2, 4]


def loop(o, platform=True, motor=True, heater=True, coolant=6000):
    w = at(o)
    cmds = []
    if platform:
        cmds.append("/fill %s %s create:andesite_casing" % (w(0, 0, 0), w(7, 0, 7)))
    cmds += vessel(o)
    pipes, fix = reactor_loop(o)
    cmds += pipes
    return cmds, fix, start(o, coolant, motor=False, power=False)[:1], start(o, coolant, motor, heater)[1:]


def write(name, steps):
    json.dump({"name": name, "steps": steps}, open(os.path.join(HERE, name + ".json"), "w", newline="\n"), indent=1)
    print(name, len(steps), "steps")
