PIPES = [(3, 3, 6), (5, 3, 6), (6, 3, 6), (6, 4, 6), (6, 6, 6), (6, 6, 5), (6, 6, 4), (6, 6, 3), (6, 6, 2), (6, 6, 1),
         (6, 6, 0), (5, 6, 0), (4, 6, 0), (3, 6, 0), (3, 5, 0), (3, 4, 0), (3, 3, 0)]
COLUMNS = ["crc", "fff", "crc"]
KIND = {"f": "fuel_channel", "r": "control_channel", "c": "coolant_channel"}
CONTROLLER = (1, 3, 3)
REDSTONE = (0, 3, 3)
MOTOR = (4, 2, 6)


def at(o):
    def w(x, y, z):
        return "%d %d %d" % (x + o[0], y + o[1], z + o[2])
    return w


def vessel(o):
    w = at(o)
    cmds = ["/fill %s %s criticalmass:reactor_vessel_casing hollow" % (w(1, 1, 1), w(5, 5, 5)),
            "/setblock %s criticalmass:reactor_controller[facing=west]" % w(*CONTROLLER),
            "/setblock %s criticalmass:reactor_service_port[facing=up]" % w(3, 5, 3),
            "/setblock %s criticalmass:primary_coolant_port[facing=south,mode=outlet]" % w(3, 3, 5),
            "/setblock %s criticalmass:primary_coolant_port[facing=north,mode=inlet]" % w(3, 3, 1)]
    for k, row in enumerate(COLUMNS):
        for i, ch in enumerate(row):
            cmds.append("/fill %s %s criticalmass:%s" % (w(2 + i, 2, 2 + k), w(2 + i, 4, 2 + k), KIND[ch]))
    return cmds


def loop(o):
    w = at(o)
    cmds = ["/setblock %s criticalmass:circulation_pump[facing=east,axis_along_first=true]" % w(4, 3, 6),
            "/setblock %s criticalmass:steam_turbine[axis=x]" % w(6, 5, 6)]
    cmds += ["/setblock %s criticalmass:reinforced_pipe" % w(*p) for p in PIPES]
    fix = ["/setblock %s minecraft:air" % w(*p) for p in PIPES] + ["/setblock %s criticalmass:reinforced_pipe" % w(*p) for p in PIPES]
    return cmds, fix


def start(o, coolant=6000, motor=True, power=True):
    w = at(o)
    cmds = ["/data merge block %s {Coolant:{Fluid:{id:\"criticalmass:primary_coolant\",amount:%d},Heat:0.0d}}" % (w(*CONTROLLER), coolant)]
    if motor:
        cmds.append("/setblock %s create:creative_motor[facing=up]" % w(*MOTOR))
    if power:
        cmds.append("/setblock %s minecraft:lever[face=wall,facing=west,powered=true]" % w(*REDSTONE))
    return cmds
