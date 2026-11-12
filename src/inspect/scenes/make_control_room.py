import json
import os

import make_reactor_datapack as m

EYE = 1.62
SNEAK_EYE = 1.27
FACE_Z = 13 / 16.0
OX, OY, OZ = 300, -60, 300
HOME = "/tp @p %d %d %d" % (OX, OY, OZ)
PANEL = {name: pos for pos, name, _ in m.PANELS}
MID = sum(pos[0] for pos in PANEL.values()) / len(PANEL) + 0.5
DESK_Z = m.DESK[1]


def look(pos, target, eye=EYE):
    return "/tp @p %.2f %.2f %.2f facing %.3f %.3f %.3f" % (OX + pos[0], OY + pos[1], OZ + pos[2], OX + target[0], OY + target[1] - eye, OZ + target[2])


def ctl(name, u, v):
    x, y, z = PANEL[name]
    return (x + 1 - u / 16.0, y + v / 16.0, z + FACE_Z)


def click(name, u, v, wait=3, sneak=False):
    x, y, z = ctl(name, u, v)
    px, py, pz = PANEL[name]
    steps = [{"cmds": [look((x, 0, DESK_Z - 1.6), (x, y, z), SNEAK_EYE if sneak else EYE)], "wait": 12, "sneak": sneak}, {"use": [OX + px, OY + py, OZ + pz, "up"], "wait": wait}]
    return steps + ([{"sneak": False, "wait": 2}] if sneak else [])


def ledger(wait):
    return {"cmds": ["/criticalmass ledger"], "wait": wait}


wide = look((MID, 1, 5.5), (MID, 3.6, 12.0))
screen = look((MID, 0, 7.4), (MID, 4.0, 12.0))
left_screen = look((MID + 2.4, 0, 9.4), (MID + 2.9, 4.3, 12.0))
desk = look((MID, 0, 8.7), (MID, 1.5, 12.0))
left = look((MID + 2.0, 0, 10.2), (MID + 2.0, 1.5, 12.0))
right = look((MID - 2.0, 0, 10.2), (MID - 2.0, 1.5, 12.0))
side = look((MID + 5.5, 0, 10.0), (MID, 1.5, 12.0))
state = ["/data get block %d %d %d" % (OX + pos[0], OY + pos[1], OZ + pos[2]) for pos in PANEL.values()]
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
         {"wait": 2}]
steps += click("rod_panel", 4.5, 7.8) + click("lever_panel", 8, 6.0) + click("dial_panel", 8, 7.8) + click("dial_panel", 8, 7.8, sneak=True) + click("dial_panel", 8, 7.8, sneak=True)
steps += [{"cmds": state, "wait": 5}]
steps += click("scram_panel", 5.0, 7.8, 40)
steps += [ledger(5), {"cmds": [desk], "wait": 40}, {"shot": "scram_a", "wait": 6}, {"shot": "scram_b", "wait": 2}, {"wait": 2}]
steps += click("alarm_panel", 4.5, 3.4, 20)
steps += [{"cmds": [desk], "wait": 40}, {"shot": "acknowledged", "wait": 2},
          {"wait": 200}, {"cmds": [wide], "wait": 40, "gui": False}, {"shot": "tripped", "wait": 2},
          {"cmds": state, "wait": 5}, ledger(5)]
steps += [{"cmds": ["/item replace entity @p hotbar.%d with criticalmass:%s" % (i, name) for i, name in enumerate(PANEL)] + ["/item replace entity @p hotbar.7 with criticalmass:plant_screen"], "wait": 20, "gui": True},
          {"shot": "items", "wait": 2}]
json.dump({"name": "room", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "control_room.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
