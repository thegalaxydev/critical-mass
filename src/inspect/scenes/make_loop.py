import json
import os

EYE = 1.62

O = (10, -61, 10)


def w(x, y, z):
    return "%d %d %d" % (x + O[0], y + O[1], z + O[2])


pipes = [(2, 2, 5), (4, 2, 5), (4, 3, 5), (4, 5, 5), (4, 5, 4), (4, 5, 3), (4, 5, 2), (4, 5, 1), (3, 5, 1), (2, 5, 1), (2, 4, 1), (2, 3, 1), (2, 2, 1)]
casing = [(3, 2, 3), (3, 3, 3), (2, 3, 2), (2, 3, 4), (3, 3, 2), (3, 3, 4), (3, 2, 2), (3, 2, 4), (2, 3, 3)]
build = ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative"]
build += ["/setblock %s criticalmass:reactor_vessel_casing" % w(*c) for c in casing]
build += ["/setblock %s criticalmass:reactor_controller[facing=west]" % w(2, 2, 3),
          "/setblock %s criticalmass:primary_coolant_port[facing=south,mode=outlet]" % w(2, 2, 4),
          "/setblock %s criticalmass:primary_coolant_port[facing=north,mode=inlet]" % w(2, 2, 2),
          "/setblock %s criticalmass:circulation_pump[facing=east,axis_along_first=true]" % w(3, 2, 5),
          "/setblock %s criticalmass:steam_turbine[axis=x]" % w(4, 4, 5)]
build += ["/setblock %s criticalmass:reinforced_pipe" % w(*p) for p in pipes]
fix = ["/setblock %s minecraft:air" % w(*p) for p in pipes] + ["/setblock %s criticalmass:reinforced_pipe" % w(*p) for p in pipes]
steps = [{"cmds": build, "wait": 10, "gui": True, "camera": "first"},
         {"cmds": fix, "wait": 10},
         {"cmds": ["/data merge block %s {Coolant:{Fluid:{id:\"criticalmass:primary_coolant\",amount:6000},Heat:0.0d}}" % w(2, 2, 3),
                   "/item replace entity @p armor.head with create:goggles",
                   "/setblock %s create:shaft[axis=x]" % w(5, 4, 5), "/setblock %s create:stressometer[facing=south,axis_along_first=true]" % w(6, 4, 5)], "wait": 10},
         {"cmds": ["/tp @p %.1f %.1f %.1f facing %.1f %.1f %.1f" % (O[0] - 0.5, O[1] + 1.4, O[2] + 3.5, O[0] + 2.5, O[1] + 2.5 - EYE, O[2] + 3.5)], "wait": 40},
         {"shot": "controller_cold", "wait": 5},
         {"cmds": ["/setblock %s creative_motor_placeholder" % w(3, 1, 5)], "wait": 1},
         {"cmds": ["/setblock %s create:creative_motor[facing=up]" % w(3, 1, 5), "/setblock %s minecraft:redstone_block" % w(2, 1, 3)], "wait": 300},
         {"shot": "controller_hot", "wait": 5},
         {"cmds": ["/tp @p %.1f %.1f %.1f facing %.1f %.1f %.1f" % (O[0] + 3.5, O[1] + 1.4, O[2] + 8.0, O[0] + 3.5, O[1] + 2.5 - EYE, O[2] + 5.5)], "wait": 30},
         {"shot": "pump_running", "wait": 5},
         {"cmds": ["/setblock %s minecraft:barrier" % w(4, 3, 8), "/tp @p %.1f %.1f %.1f facing %.1f %.1f %.1f" % (O[0] + 4.5, O[1] + 4.0, O[2] + 8.0, O[0] + 4.5, O[1] + 4.5 - EYE, O[2] + 5.5)], "wait": 30},
         {"shot": "turbine_running", "wait": 5},
         {"cmds": ["/setblock %s minecraft:barrier" % w(6, 3, 8), "/tp @p %.1f %.1f %.1f facing %.1f %.1f %.1f" % (O[0] + 6.5, O[1] + 4.0, O[2] + 8.0, O[0] + 6.5, O[1] + 4.5 - EYE, O[2] + 5.5)], "wait": 30},
         {"shot": "stressometer", "wait": 5},
         {"cmds": ["/setblock %s minecraft:air" % w(3, 1, 5), "/tp @p %.1f %.1f %.1f facing %.1f %.1f %.1f" % (O[0] + 3.5, O[1] + 1.4, O[2] + 8.0, O[0] + 3.5, O[1] + 2.5 - EYE, O[2] + 5.5)], "wait": 60},
         {"shot": "pump_stopped", "wait": 5},
         {"cmds": ["/setblock %s minecraft:barrier" % w(9, 4, 11), "/tp @p %.1f %.1f %.1f facing %.1f %.1f %.1f" % (O[0] + 9.5, O[1] + 5.0, O[2] + 11.0, O[0] + 3.0, O[1] + 3.0 - EYE, O[2] + 3.0)], "wait": 30},
         {"shot": "overview"}, {"wait": 5}]
steps = [s for s in steps if "creative_motor_placeholder" not in json.dumps(s)]
json.dump({"name": "loop", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "loop.json"), "w"), indent=1)
print(len(steps), "steps")
