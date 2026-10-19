import json
import os

steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode creative", "/tp @p 40 -60 36"], "wait": 40, "gui": False, "camera": "first"},
         {"cmds": ["/setblock 40 -59 40 criticalmass:steam_turbine[axis=x]", "/setblock 41 -59 40 create:creative_motor[facing=west]",
                   "/setblock 40 -59 44 criticalmass:circulation_pump[facing=east,axis_along_first=true]", "/setblock 39 -59 44 create:creative_motor[facing=east]",
                   "/setblock 40 -59 37 minecraft:barrier", "/tp @p 40.5 -58 37.5 facing 40.5 -59.62 40.5"], "wait": 60}]
for i in range(4):
    steps += [{"shot": "turbine_%d" % i, "wait": 3}]
steps += [{"cmds": ["/setblock 42 -59 44 minecraft:barrier", "/tp @p 42.5 -58 44.5 facing 40.5 -59.62 44.5"], "wait": 30}]
for i in range(3):
    steps += [{"shot": "pump_%d" % i, "wait": 3}]
json.dump({"name": "rotors", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "rotors.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
