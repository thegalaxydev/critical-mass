import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
PIPES = [(2, 2, 5), (4, 2, 5), (4, 3, 5), (4, 5, 5), (4, 5, 4), (4, 5, 3), (4, 5, 2), (4, 5, 1), (3, 5, 1), (2, 5, 1), (2, 4, 1), (2, 3, 1), (2, 2, 1)]
CASING = [(3, 2, 3), (3, 3, 3), (2, 3, 2), (2, 3, 4), (3, 3, 2), (3, 3, 4), (3, 2, 2), (3, 2, 4), (2, 3, 3)]


def loop(o, platform=True, motor=True, heater=True, coolant=6000):
    def w(x, y, z):
        return "%d %d %d" % (x + o[0], y + o[1], z + o[2])

    cmds = []
    if platform:
        cmds.append("/fill %s %s create:andesite_casing" % (w(0, 0, 0), w(7, 0, 7)))
    cmds += ["/setblock %s criticalmass:reactor_vessel_casing" % w(*c) for c in CASING]
    cmds += ["/setblock %s criticalmass:reactor_controller[facing=west]" % w(2, 2, 3),
             "/setblock %s criticalmass:primary_coolant_port[facing=south,mode=outlet]" % w(2, 2, 4),
             "/setblock %s criticalmass:primary_coolant_port[facing=north,mode=inlet]" % w(2, 2, 2),
             "/setblock %s criticalmass:circulation_pump[facing=east,axis_along_first=true]" % w(3, 2, 5),
             "/setblock %s criticalmass:steam_turbine[axis=x]" % w(4, 4, 5)]
    cmds += ["/setblock %s criticalmass:reinforced_pipe" % w(*p) for p in PIPES]
    fix = ["/setblock %s minecraft:air" % w(*p) for p in PIPES] + ["/setblock %s criticalmass:reinforced_pipe" % w(*p) for p in PIPES]
    fill = ["/data merge block %s {Coolant:{Fluid:{id:\"criticalmass:primary_coolant\",amount:%d},Heat:0.0d}}" % (w(2, 2, 3), coolant)]
    run = []
    if motor:
        run.append("/setblock %s create:creative_motor[facing=up]" % w(3, 1, 5))
    if heater:
        run.append("/setblock %s minecraft:redstone_block" % w(2, 1, 3))
    return cmds, fix, fill, run


def write(name, steps):
    json.dump({"name": name, "steps": steps}, open(os.path.join(HERE, name + ".json"), "w", newline="\n"), indent=1)
    print(name, len(steps), "steps")
