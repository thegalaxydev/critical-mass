import json
import os
from reactor_layout import CONTROLLER, REDSTONE, at, loop, start, vessel

O = (40, -60, 40)
w = at(O)
EYE = 1.62
PORT = (3, 5, 3)
WET = (1, 6, 1)
DRY = (1, 6, 5)
CASK = (5, 6, 3)
STAND = (2.5, 6.0, 3.5)


def a(p):
    return [p[0] + O[0], p[1] + O[1], p[2] + O[2]]


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px + O[0], py + O[1], pz + O[2], tx + O[0], ty + O[1] - EYE, tz + O[2])


def at_block(p, dy=0.5):
    return look(STAND[0], STAND[1], STAND[2], p[0] + 0.5, p[1] + dy, p[2] + 0.5)


controller_view = look(-1.6, 0.0, 3.2, 1.0, 3.93, 3.12)
port_view = at_block(PORT, 1.0)
build, fix = loop(O)
lever_off = "/setblock %s minecraft:lever[face=wall,facing=west,powered=false]" % w(*REDSTONE)
steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative", "/tp @p %d -59 %d" % (O[0] - 2, O[2] + 3)], "wait": 40, "gui": False, "camera": "first"},
         {"cmds": vessel(O) + build, "wait": 10}, {"cmds": fix, "wait": 10},
         {"cmds": start(O), "wait": 300},
         {"cmds": ["/item replace entity @p armor.head with create:goggles", controller_view], "wait": 30, "gui": True},
         {"shot": "running", "wait": 2},
         {"cmds": ["/criticalmass fuel %s burn 0.0002" % w(*CONTROLLER)], "wait": 80},
         {"cmds": [controller_view], "wait": 10}, {"shot": "spent", "wait": 2},
         {"cmds": [lever_off], "wait": 40},
         {"cmds": [port_view], "wait": 20}, {"shot": "port_waiting", "wait": 2},
         {"cmds": ["/item replace entity @p weapon.mainhand with criticalmass:fuel_assembly"], "slot": 0, "wait": 5},
         {"use": a(PORT), "wait": 3}, {"shot": "refuel_blocked", "wait": 2},
         {"cmds": ["/item replace entity @p weapon.mainhand with minecraft:air"], "wait": 5},
         {"use": a(PORT), "wait": 25},
         {"slot": 1, "wait": 2}, {"use": a(PORT), "wait": 25},
         {"cmds": [port_view], "wait": 10}, {"shot": "port_ready", "inventory": True, "wait": 2},
         {"cmds": ["/setblock %s criticalmass:spent_fuel_rack" % w(*WET), "/setblock %s criticalmass:spent_fuel_rack" % w(*DRY),
                   "/data merge block %s {Buffer:{Fluid:{id:\"minecraft:water\",amount:4000},Heat:0.0d}}" % w(*WET)], "wait": 5},
         {"slot": 0, "wait": 2}, {"use": a(WET), "wait": 3},
         {"slot": 1, "wait": 2}, {"use": a(DRY), "wait": 40},
         {"cmds": [at_block(WET)], "wait": 10}, {"shot": "rack_wet", "wait": 2},
         {"cmds": [at_block(DRY)], "wait": 10}, {"shot": "rack_dry", "wait": 2},
         {"cmds": ["/tick sprint 2400"], "wait": 300},
         {"cmds": [at_block(DRY)], "wait": 10}, {"shot": "rack_dry_later", "wait": 2},
         {"cmds": [at_block(WET)], "wait": 10}, {"shot": "rack_wet_later", "wait": 2},
         {"cmds": ["/criticalmass ledger"], "wait": 5},
         {"cmds": [look(STAND[0], STAND[1], STAND[2], STAND[0], 40.0, STAND[2] + 1)], "wait": 5},
         {"rejoin": True, "wait": 300},
         {"cmds": [at_block(WET)], "wait": 20, "gui": True}, {"shot": "rack_after_rejoin", "wait": 2},
         {"cmds": ["/tick sprint 4800"], "wait": 500},
         {"cmds": ["/setblock %s criticalmass:shielded_cask" % w(*CASK)], "slot": 0, "wait": 5},
         {"use": a(WET), "wait": 3}, {"slot": 1, "wait": 2}, {"use": a(DRY), "wait": 3},
         {"slot": 0, "wait": 2}, {"use": a(CASK), "wait": 3}, {"slot": 1, "wait": 2}, {"use": a(CASK), "wait": 5},
         {"cmds": [at_block(CASK)], "wait": 10}, {"shot": "cask_loaded", "inventory": True, "wait": 2},
         {"cmds": ["/setblock %s minecraft:air destroy" % w(*CASK), "/tp @p %.2f %.2f %.2f" % (CASK[0] + O[0] + 0.5, CASK[1] + O[1], CASK[2] + O[2] + 0.5)], "wait": 30},
         {"cmds": [at_block(CASK)], "slot": 0, "inventory": True, "wait": 10},
         {"use": [CASK[0] + O[0], CASK[1] + O[1] - 1, CASK[2] + O[2], "up"], "wait": 10},
         {"cmds": [at_block(CASK)], "wait": 10}, {"shot": "cask_placed", "inventory": True, "wait": 2},
         {"cmds": ["/criticalmass ledger"], "wait": 5}]
json.dump({"name": "fuel", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "fuel.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
