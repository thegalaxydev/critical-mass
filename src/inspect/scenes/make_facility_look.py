import json
import os
import sys

import make_facility as m

EYE = 1.62
O = (300, -60, 300)
w = m.absolute(O)
VIEWS = {
    "aerial_se": ((66, 40, 52), (14, 4, 0), True),
    "aerial_nw": ((-30, 36, -50), (16, 4, -6), True),
    "aerial_sw": ((-34, 30, 48), (10, 4, 6), True),
    "entrance": ((36, 1, 44), (32, 6, 10), False),
    "switchyard": ((55, 3, 5), (48, 3, -4), False),
    "switchyard_wall": ((50, 2, -11), (46, 4, -3), False),
    "generator": ((31, 7, 4), (29, 8, -1), False),
    "unit_bus": ((34, 7, 2), (37, 8, -4), False),
    "station_bus": ((38, 1, -1), (41, 2, -6), False),
    "standby": ((40, 1, 3), (41, 2, 8), False),
    "penetration": ((6, 1, 9), (7, 4, 4), False),
    "side_screens": ((27, 7, 12), (27, 11, 19), False),
    "tower_outside": ((47, 2, -33), (24, 13, -22), False),
    "tower_inside": ((27, 1, -15), (24, 9, -24), False),
    "containment": ((0, 1, 9), (0, 4, 0), False),
    "containment_east": ((9, 1, -5), (0, 3, 1), False),
    "containment_west": ((-9, 1, -4), (1, 4, 1), False),
    "containment_top": ((-3, 6, 3), (4, 9, -4), False),
    "turbine_deck": ((41, 7, 6), (22, 8, -1), False),
    "turbine_west": ((15, 7, -6), (30, 8, 1), False),
    "turbine_basement": ((40, 1, 6), (24, 3, -1), False),
    "condenser": ((30, 1, -3), (24, 3, 0), False),
    "control_room": ((27, 7, 11), (27, 10, 19), False),
    "control_desk": ((27, 7, 13), (27, 8, 16), False),
    "control_window": ((27, 7, 17), (25, 8, 0), False),
    "relay_room": ((16, 7, 11), (15, 8, 19), False),
    "lobby": ((39, 1, 11), (31, 2, 19), False),
    "change_room": ((25, 1, 12), (25, 2, 20), False),
    "corridor": ((11, 1, 14), (-10, 2, 14), False),
    "fuel_pool": ((6, 1, 18), (-7, 0, 23), False),
    "fuel_east": ((-2, 1, 24), (6, 1, 23), False),
}


NIGHT = ("aerial_se", "aerial_sw", "entrance", "tower_outside", "switchyard")


def look(name):
    pos, target, _ = VIEWS[name]
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (O[0] + pos[0] + 0.5, O[1] + pos[1] - 1, O[2] + pos[2] + 0.5,
                                                         O[0] + target[0] + 0.5, O[1] + target[1] - 0.5 - EYE, O[2] + target[2] + 0.5)


def shots(names, tag, wait=30):
    out = []
    for name in names:
        out += [{"cmds": [look(name)], "wait": wait, "gui": False}, {"shot": tag + "_" + name, "wait": 2}]
    return out


def scene(names, run=True, night=False, away=False):
    perches = ["/setblock %s minecraft:barrier" % w(pos[0], pos[1] - 1, pos[2]) for pos, _, high in VIEWS.values() if high]
    steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/weather clear", "/gamemode creative", "/clear @p",
                       "/tp @p %d %d %d" % (O[0] - 24, O[1], O[2] + 40)], "wait": 80, "gui": False, "camera": "first", "slot": 1},
             {"cmds": ["/" + c for c in m.place(w)], "wait": 40},
             {"cmds": ["/" + c for c in m.summon_props(O)] + perches, "wait": 100},
             {"cmds": ["/" + c for c in m.start(w)], "wait": 200},
             {"cmds": ["/criticalmass ledger"], "wait": 5, "closeScreen": True}]
    if not run:
        steps += shots(names, "cold")
    if run:
        steps += [{"cmds": ["/" + c for c in m.run(w)], "wait": m.SYNC_AFTER}, {"cmds": ["/" + c for c in m.sync(w)], "wait": 40},
                  {"cmds": ["/" + c for c in m.load(w)], "wait": 300}, {"cmds": ["/criticalmass ledger"], "wait": 5}]
        steps += shots(names, "run")
        steps += [{"cmds": ["/criticalmass ledger"], "wait": 5}]
    if night:
        steps += [{"cmds": ["/time set 18000"], "wait": 60}]
        steps += shots([n for n in names if n in NIGHT], "night")
    if away:
        steps += [{"cmds": ["/time set 6000", "/tp @p %d %d %d" % (O[0] + 2000, O[1], O[2])], "wait": 300, "note": "away"},
                  {"cmds": ["/tp @p %d %d %d" % (O[0] - 24, O[1], O[2] + 40)], "wait": 300, "note": "back"},
                  {"cmds": ["/criticalmass ledger"], "wait": 5}]
    return steps


if __name__ == "__main__":
    names = [n for n in sys.argv[1].split(",") if n] if len(sys.argv) > 1 and sys.argv[1] != "all" else list(VIEWS)
    run = "norun" not in sys.argv
    night = "night" in sys.argv
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "facility.json")
    steps = scene(names, run, night, "away" in sys.argv)
    json.dump({"name": "facility", "steps": steps}, open(path, "w", newline="\n"), indent=1)
    print(len(steps), "steps")
