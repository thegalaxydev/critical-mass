import json
import os

O = (40, -60, 40)
SHAPES = {
    "single": [(0, 0, 0)],
    "straight": [(0, 0, 0), (1, 0, 0), (2, 0, 0)],
    "corner": [(0, 0, 0), (1, 0, 0), (1, 0, 1)],
    "tee": [(0, 0, 0), (1, 0, 0), (2, 0, 0), (1, 0, 1)],
    "cross": [(0, 0, 0), (1, 0, 0), (2, 0, 0), (1, 0, 1), (1, 0, -1), (1, 1, 0)],
    "riser": [(0, 0, 0), (0, 1, 0), (0, 2, 0), (1, 2, 0)],
}


def place(block, dz):
    cmds = []
    x = 0
    for shape in SHAPES.values():
        for p in shape:
            cmds.append("/setblock %d %d %d %s" % (O[0] + x + p[0], O[1] + 1 + p[1], O[2] + dz + p[2], block))
        x += 5
    return cmds


steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative", "/tp @p 52 -55 34 0 35"], "wait": 40, "gui": False, "camera": "first"},
         {"cmds": place("create:fluid_pipe", 0) + place("criticalmass:reinforced_pipe", 5), "wait": 10},
         {"cmds": [c.replace(" criticalmass:reinforced_pipe", " minecraft:air") for c in place("criticalmass:reinforced_pipe", 5)], "wait": 5},
         {"cmds": place("criticalmass:reinforced_pipe", 5) + place("criticalmass:reinforced_pipe", 5)[::-1], "wait": 20},
         {"cmds": ["/setblock 53 -56 33 minecraft:barrier", "/tp @p 53.5 -55 33.5 facing 53.5 -60.6 44.0"], "wait": 20}, {"shot": "overview", "wait": 2},
         {"cmds": ["/setblock 43 -58 50 minecraft:barrier", "/tp @p 43.5 -57 50.5 facing 46.0 -60.4 44.5"], "wait": 20}, {"shot": "close", "wait": 2},
         {"cmds": ["/setblock 58 -58 50 minecraft:barrier", "/tp @p 58.5 -57 50.5 facing 56.0 -60.4 44.5"], "wait": 20}, {"shot": "close2", "wait": 2},
         {"cmds": ["/setblock 66 -58 50 minecraft:barrier", "/tp @p 66.5 -57 50.5 facing 65.5 -58.5 45.5"], "wait": 20}, {"shot": "riser", "wait": 2},
         {"cmds": ["/item replace entity @p weapon.mainhand with criticalmass:reinforced_pipe", "/item replace entity @p hotbar.1 with create:fluid_pipe",
                   "/tp @p 53.5 -55 33.5 facing 53.5 -60.6 44.0"], "wait": 10, "gui": True}, {"shot": "held", "wait": 2}]
json.dump({"name": "pipes", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "pipes.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
