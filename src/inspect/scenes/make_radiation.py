import json
import os
from reactor_layout import CONTROLLER, at, loop, start, vessel

O = (40, -60, 40)
w = at(O)
EYE = 1.62
R = (40, -60, 64)
B = (80, -60, 0)
CENTER = (43.5, -56.5, 43.5)


def hot(slot, residual):
    return ("{Slot:%db,id:\"criticalmass:spent_fuel_assembly\",count:1,components:{\"criticalmass:fuel_state\":{id:[I;%d,7,7,7],remaining:0.0d,condition:0.9d,"
            "residual:%.2fd,discharge_residual:%.2fd}}}") % (slot, slot + 1, residual, residual)


def items(residual):
    return "Items:[%s]" % ",".join(hot(i, residual) for i in range(4))


def probe(x, y, z):
    return "/criticalmass radiation %.2f %.2f %.2f" % (x, y, z)


def look_from(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px, py, pz, tx, ty - EYE, tz)


wall = lambda block: "/fill 48 -60 40 48 -53 47 %s" % block
build, fix = loop(O)
dist = [probe(CENTER[0] + d, CENTER[1], CENTER[2]) for d in (3, 5, 9, 17)]
behind = probe(50.5, CENTER[1], CENTER[2])
hole_out = (46, -58, 42)
steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative", "/tp @p 52 -60 44"], "wait": 40, "gui": False, "camera": "first"},
         {"cmds": vessel(O) + build, "wait": 10}, {"cmds": fix, "wait": 10},
         {"cmds": start(O), "wait": 300},
         {"note": "SECTION distance"}, {"cmds": dist + ["/criticalmass ledger"], "wait": 5},
         {"note": "SECTION shielding"}, {"cmds": [behind], "wait": 2},
         {"cmds": [wall("criticalmass:shielding_concrete"), behind], "wait": 2},
         {"cmds": [wall("criticalmass:shielded_glass"), behind], "wait": 2},
         {"cmds": [wall("criticalmass:shielded_hatch[open=false]"), behind], "wait": 2},
         {"cmds": [wall("criticalmass:shielded_hatch[open=true]"), behind], "wait": 2},
         {"cmds": [wall("minecraft:air")], "wait": 2},
         {"cmds": ["/item replace entity @p weapon.mainhand with criticalmass:survey_meter", "/item replace entity @p armor.head with create:goggles",
                   look_from(47.5, -60, 43.5, 44.0, -57.0, 43.5)], "wait": 40, "gui": True},
         {"shot": "meter_near", "wait": 2},
         {"cmds": ["/fill 46 -60 40 46 -53 47 criticalmass:shielding_concrete", look_from(47.5, -60, 43.5, 44.0, -57.0, 43.5)], "wait": 40},
         {"shot": "meter_behind_concrete", "wait": 2},
         {"cmds": ["/fill 46 -60 40 46 -53 47 minecraft:air"], "wait": 5},
         {"note": "SECTION storage"},
         {"cmds": ["/setblock %d %d %d criticalmass:spent_fuel_rack{%s}" % (R[0], R[1], R[2], items(0.4)),
                   "/setblock %d %d %d criticalmass:spent_fuel_rack{%s,Buffer:{Fluid:{id:\"minecraft:water\",amount:4000},Heat:0.0d}}" % (R[0] + 8, R[1], R[2], items(0.4)),
                   "/setblock %d %d %d criticalmass:shielded_cask{%s}" % (R[0] + 16, R[1], R[2], items(0.4))], "wait": 5},
         {"cmds": [probe(R[0] + 2.0, R[1] + 0.5, R[2] + 0.5), probe(R[0] + 10.0, R[1] + 0.5, R[2] + 0.5), probe(R[0] + 18.0, R[1] + 0.5, R[2] + 0.5)], "wait": 5},
         {"note": "SECTION breach"},
         {"cmds": ["/data merge block %s {Integrity:[{P:[I;4,-1,-1],V:0.0d}]}" % w(*CONTROLLER)], "wait": 100},
         {"cmds": [probe(hole_out[0] + 0.5, hole_out[1] + 0.5, hole_out[2] + 0.5), "/criticalmass ledger"], "wait": 5},
         {"cmds": [look_from(47.5, -60, 42.5, 45.0, -58.5, 42.5)], "useItem": True, "wait": 40},
         {"shot": "contamination_meter", "wait": 2},
         {"cmds": ["/item replace entity @p weapon.mainhand with criticalmass:dosimeter"], "wait": 60},
         {"shot": "dosimeter", "inventory": True, "wait": 2},
         {"cmds": ["/item replace entity @p armor.head with criticalmass:protective_hood", "/item replace entity @p armor.chest with criticalmass:protective_suit",
                   "/item replace entity @p armor.legs with criticalmass:protective_leggings", "/item replace entity @p armor.feet with criticalmass:protective_boots"], "wait": 100},
         {"inventory": True, "wait": 2},
         {"cmds": ["/setblock 49 -60 42 create:fluid_tank", "/item replace entity @p weapon.mainhand with criticalmass:decontamination_compound 4"], "wait": 5},
         {"useItem": True, "wait": 5, "inventory": True},
         {"use": [45, -58, 42, "east"], "wait": 5},
         {"use": [45, -57, 42, "east"], "wait": 5},
         {"cmds": [probe(hole_out[0] + 0.5, hole_out[1] + 0.5, hole_out[2] + 0.5), "/item replace entity @p armor.head with create:goggles", look_from(47.5, -60, 41.5, 49.5, -59.5, 42.5)], "wait": 20},
         {"shot": "runoff_tank", "wait": 2},
         {"cmds": ["/item replace entity @p weapon.mainhand with criticalmass:dosimeter"], "wait": 5},
         {"useItem": True, "wait": 10},
         {"shot": "dosimeter_summary", "wait": 2},
         {"note": "SECTION moving body"},
         {"cmds": ["/fill %d %d %d %d %d %d create:andesite_casing" % (B[0], B[1], B[2], B[0] + 2, B[1], B[2] + 2),
                   "/setblock %d %d %d criticalmass:spent_fuel_rack{%s}" % (B[0] + 1, B[1] + 1, B[2] + 1, items(5.0)),
                   "/tp @p %d %d %d" % (B[0] - 6, B[1] + 1, B[2] + 1)], "gui": False, "wait": 20},
         {"cmds": [probe(B[0] + 3.5, B[1] + 1.5, B[2] + 1.5)], "wait": 2},
         {"cmds": ["/sable assemble area %d %d %d %d %d %d" % (B[0], B[1], B[2], B[0] + 2, B[1] + 1, B[2] + 2)], "wait": 40},
         {"cmds": ["/sable paused true", "/sable teleport @n %d %d %d" % (B[0] + 20, B[1] + 10, B[2] + 20)], "wait": 20},
         {"cmds": ["/sable info @n", probe(B[0] + 3.5, B[1] + 1.5, B[2] + 1.5), probe(B[0] + 23.5, B[1] + 11.5, B[2] + 21.5), probe(B[0] + 21.5, B[1] + 14.5, B[2] + 21.5)], "wait": 5},
         {"cmds": ["/item replace entity @p weapon.mainhand with criticalmass:survey_meter", "/tp @p %d %d %d" % (B[0] + 24, B[1] + 9, B[2] + 21),
                   look_from(B[0] + 24.5, B[1] + 9, B[2] + 21.5, B[0] + 21.5, B[1] + 11.0, B[2] + 21.5)], "wait": 40, "gui": True},
         {"shot": "meter_body", "wait": 2},
         {"cmds": ["/sable paused false"], "wait": 40},
         {"cmds": ["/sable info @n", probe(B[0] + 3.5, B[1] + 1.5, B[2] + 1.5), probe(B[0] + 23.5, B[1] + 11.5, B[2] + 21.5)], "wait": 5}]
json.dump({"name": "radiation", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "radiation.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
