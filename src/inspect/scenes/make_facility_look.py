import json
import os
import sys

import make_facility as m

EYE = 1.62
O = (300, -60, 300)
w = m.absolute(O)
VIEWS = {
    "aerial_se": ((96, 46, 70), (20, 4, -4), True),
    "aerial_nw": ((-36, 40, -60), (24, 4, -8), True),
    "aerial_ne": ((96, 40, -66), (30, 6, -10), True),
    "aerial_sw": ((-40, 34, 60), (14, 4, 6), True),
    "entrance": ((42, 1, 44), (40, 7, 30), False),
    "switchyard": ((69, 3, 2), (62, 3, -4), False),
    "tower_outside": ((38, 2, -19), (30, 14, -30), False),
    "tower_inside": ((26, 1, -25), (26, 10, -32), False),
    "containment": ((-11, 1, 9), (2, 6, -1), False),
    "containment_top": ((-5, 10, -9), (6, 9, 4), False),
    "loop_a": ((1, 1, -13), (9, 5, -9), False),
    "loop_b": ((1, 1, 13), (9, 5, 9), False),
    "penetration": ((11, 1, 3), (16, 5, 0), False),
    "turbine_deck": ((54, 7, 11), (28, 9, -4), False),
    "turbine_west": ((20, 7, 0), (40, 9, 0), False),
    "generator": ((39, 7, -4), (36, 8, -8), False),
    "unit_bus": ((42, 7, 4), (47, 8, 0), False),
    "turbine_basement": ((45, 1, 0), (26, 3, -6), False),
    "condenser": ((36, 1, -3), (35, 3, -8), False),
    "circulating_pump": ((24, 1, -11), (20, 2, -14), False),
    "switchgear": ((56, 1, -13), (50, 2, -6), False),
    "feeders": ((52, 1, -13), (52, 1, -9), False),
    "battery": ((53, 1, -7), (49, 1, -6), False),
    "standby": ((49, 1, 14), (54, 2, 7), False),
    "transfer": ((55, 1, 11), (55, 2, 7), False),
    "control_room": ((33, 7, 20), (31, 10, 28), False),
    "control_desk": ((33, 7, 21), (32, 7, 25), False),
    "desk_west": ((24, 7, 21), (22, 7, 25), False),
    "desk_east": ((38, 7, 21), (41, 7, 25), False),
    "mimic": ((36, 7, 21), (44, 10, 22), False),
    "control_window": ((31, 7, 26), (31, 9, 0), False),
    "relay_room": ((30, 7, 30), (21, 8, 32), False),
    "lobby": ((45, 1, 30), (38, 2, 22), False),
    "change_room": ((31, 1, 21), (32, 2, 32), False),
    "corridor": ((16, 1, 19), (-10, 2, 19), False),
    "fuel_pool": ((6, 1, 23), (-7, 0, 28), False),
    "fuel_east": ((-2, 1, 29), (6, 1, 28), False),
    "feed_heater": ((25, 1, -7), (30, 3, -8), False),
    "feed_makeup": ((46, 1, -6), (43, 1, -8), False),
    "desk_rods": ((30, 7, 22), (30, 7, 25), False),
    "desk_feeders": ((22, 7, 20), (19, 7, 20), False),
    "reprocessing": ((7, 1, 35), (0, 2, 39), False),
    "hot_cell": ((8, 1, 41), (0, 2, 39), False),
    "reprocessing_outside": ((22, 3, 45), (4, 4, 40), False),
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
    (x0, z0), (x1, z1) = m.AREA
    home = "/tp @p %d %d %d" % (O[0] + x0 - 4, O[1], O[2] + z1 + 4)
    perches = ["/setblock %s minecraft:barrier" % w(pos[0], pos[1] - 1, pos[2]) for pos, _, high in VIEWS.values() if high]
    steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/weather clear", "/gamemode creative", "/clear @p", home,
                       "/forceload add %d %d %d %d" % (O[0] + x0, O[2] + z0, O[0] + x1, O[2] + z1)], "wait": 80, "gui": False, "camera": "first", "slot": 1},
             {"cmds": ["/" + c for c in m.place(w)], "wait": 60},
             {"cmds": ["/" + c for c in m.summon_props(O)] + perches, "wait": 100},
             {"cmds": ["/" + c for c in m.start(w)], "wait": 100},
             {"cmds": ["/" + c for c in m.top_up(w)], "wait": 100},
             {"cmds": ["/" + c for c in m.top_up(w)], "wait": 60},
             {"cmds": ["/criticalmass ledger"], "wait": 5, "closeScreen": True}]
    if not run:
        steps += shots(names, "cold")
    if run:
        steps += [{"cmds": ["/" + c for c in cmds], "wait": wait} for _, cmds, wait in m.sequence(w)]
        steps += [{"cmds": ["/criticalmass ledger"], "wait": 5}]
        steps += shots(names, "run")
        steps += [{"cmds": ["/criticalmass ledger"], "wait": 5}]
    if night:
        steps += [{"cmds": ["/time set 18000"], "wait": 60}]
        steps += shots([n for n in names if n in NIGHT], "night")
    if away:
        steps += [{"cmds": ["/time set 6000", "/forceload remove all", "/tp @p %d %d %d" % (O[0] + 2000, O[1], O[2])], "wait": 300, "note": "away"},
                  {"cmds": [home], "wait": 300, "note": "back"},
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
