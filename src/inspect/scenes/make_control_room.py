import json
import os

import make_reactor_datapack as m

EYE = 1.62
OX, OY, OZ = 300, -60, 300
HOME = "/tp @p %d %d %d" % (OX, OY, OZ)
PART = {part[0]: (x, slot) for x, parts in m.DESKS.items() for slot, part in enumerate(parts)}
MID = sum(m.DESKS) / len(m.DESKS) + 0.5
DESK_Z = m.DESK_Z
PARTS = ["loconautics:gauge_button", "loconautics:gauge_switch", "loconautics:gauge_lever", "loconautics:locomotive_gauge", "criticalmass:dashboard_alarm"]


def look(pos, target):
    return "/tp @p %.2f %.2f %.2f facing %.3f %.3f %.3f" % (OX + pos[0], OY + pos[1], OZ + pos[2], OX + target[0], OY + target[1] - EYE, OZ + target[2])


def at(name):
    x, slot = PART[name]
    return (x + 0.25 + 0.5 * slot, 0.98, DESK_Z + 0.69)


def aim(name):
    x, y, z = at(name)
    return look((x, 0, DESK_Z - 0.9), (x, y, z))


def click(name, wait=3):
    x, _ = PART[name]
    return [{"cmds": [aim(name)], "wait": 12}, {"use": [OX + x, OY, OZ + DESK_Z, "up"], "wait": wait}]


def ledger(wait):
    return {"cmds": ["/criticalmass ledger"], "wait": wait}


wide = look((MID, 1, 4.5), (MID, 3.4, 12.0))
screen = look((MID, 0, 6.4), (MID, 4.0, 12.0))
left_screen = look((MID + 2.4, 0, 8.4), (MID + 2.9, 4.3, 12.0))
desk = look((MID, 0.6, 8.8), (MID, 0.9, DESK_Z + 0.5))
left = look((MID + 1.4, 0.4, 9.6), (MID + 1.4, 0.9, DESK_Z + 0.5))
right = look((MID - 1.4, 0.4, 9.6), (MID - 1.4, 0.9, DESK_Z + 0.5))
side = look((MID + 4.5, 0.4, 9.4), (MID, 0.9, DESK_Z + 0.5))
state = ["/data get block %d %d %d" % (OX + x, OY, OZ + DESK_Z) for x in sorted(m.DESKS)]
steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/weather clear", "/gamemode creative", "/clear @p", HOME], "wait": 60, "gui": False, "camera": "first", "slot": 1},
         {"cmds": ["/" + c for c in m.place], "wait": 100},
         {"cmds": ["/" + c for c in m.start_here], "wait": 200},
         {"cmds": [wide], "wait": 40, "closeScreen": True}, {"shot": "shutdown", "wait": 2},
         {"cmds": [HOME], "wait": 10},
         {"cmds": ["/criticalmass ledger"] + ["/" + c for c in m.run_here], "wait": 900},
         ledger(5),
         {"cmds": [wide], "wait": 40}, {"shot": "running", "wait": 2},
         {"cmds": [screen], "wait": 40}, {"shot": "screen", "wait": 2},
         {"cmds": [left_screen], "wait": 40}, {"shot": "screen_left", "wait": 2},
         {"cmds": [desk], "wait": 40}, {"shot": "desk", "wait": 2},
         {"cmds": [left], "wait": 30}, {"shot": "desk_left", "wait": 2},
         {"cmds": [right], "wait": 30}, {"shot": "desk_right", "wait": 2},
         {"cmds": [side], "wait": 30}, {"shot": "desk_side", "wait": 2},
         {"cmds": ["/item replace entity @p armor.head with create:goggles"], "wait": 5, "gui": True}]
for name in ("scram", "rod_out", "alarm", "power", "coolant", "speed", "trip"):
    steps += [{"cmds": [aim(name)], "wait": 25}, {"shot": "goggles_" + name, "wait": 2}]
steps += [{"gui": False, "wait": 2}]
steps += click("rod_out")
steps += [{"cmds": state, "wait": 5}]
steps += click("scram", 40)
steps += [ledger(5), {"cmds": [desk], "wait": 40}, {"shot": "scram_a", "wait": 6}, {"shot": "scram_b", "wait": 2},
          {"cmds": [aim("alarm")], "wait": 25, "gui": True}, {"shot": "goggles_alarm_active", "wait": 2}, {"gui": False, "wait": 2}]
steps += click("alarm", 20)
steps += [{"cmds": [desk], "wait": 40}, {"shot": "acknowledged", "wait": 2}]
steps += click("reset", 20)
steps += [{"cmds": [desk], "wait": 40}, {"shot": "reset", "wait": 2},
          {"wait": 200}, {"cmds": [wide], "wait": 40}, {"shot": "tripped", "wait": 2},
          {"cmds": state, "wait": 5}, ledger(5)]
steps += [{"cmds": ["/item replace entity @p hotbar.0 with criticalmass:reactor_alloy_dashboard"] + ["/item replace entity @p hotbar.%d with %s" % (i + 1, name) for i, name in enumerate(PARTS)]
           + ["/item replace entity @p hotbar.7 with criticalmass:plant_screen"], "wait": 20, "gui": True},
          {"shot": "items", "wait": 2}]
json.dump({"name": "room", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "control_room.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
