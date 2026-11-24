import json
import os

EYE = 1.62
MOB = "criticalmass:irradiated_creeper"
ALL = "@e[type=%s]" % MOB
ONE = "@e[type=%s,limit=1]" % MOB
STILL = "{NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}"


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px, py, pz, tx, ty - EYE, tz)


def shot(name, wait=20):
    return [{"wait": wait}, {"shot": name, "wait": 2}]


steps = [
    {"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamerule doMobSpawning false", "/weather clear", "/difficulty normal", "/gamemode creative", "/kill @e[type=!player]",
              "/tp @p 46 -60 34"], "wait": 40, "gui": False, "camera": "first"},
    {"cmds": ["/fill 36 -61 30 58 -61 52 minecraft:smooth_stone", "/fill 36 -60 30 58 -52 52 minecraft:air", "/kill @e[type=item]",
              "/summon %s 44.5 -60 40.5 %s" % (MOB, STILL), "/summon minecraft:creeper 47.5 -60 40.5 %s" % STILL], "wait": 30},
    {"cmds": [look(46.0, -60.0, 35.5, 46.0, -58.4, 40.5)]}, *shot("beside_a_creeper"),
    {"cmds": [look(44.5, -60.0, 37.6, 44.5, -58.2, 40.5)]}, *shot("front"),
    {"cmds": [look(42.4, -60.0, 38.2, 44.5, -58.4, 40.5)]}, *shot("front_right"),
    {"cmds": [look(46.4, -60.0, 38.4, 44.5, -58.4, 40.5)]}, *shot("front_left"),
    {"cmds": [look(41.4, -60.0, 40.5, 44.5, -58.6, 40.5)]}, *shot("right_side"),
    {"cmds": [look(43.0, -60.0, 43.4, 44.5, -58.6, 40.5)]}, *shot("back_right"),
    {"cmds": [look(46.2, -60.0, 43.2, 44.5, -58.6, 40.5)]}, *shot("back_left"),
    {"cmds": ["/setblock 44 -58 38 minecraft:barrier", look(44.5, -57.0, 38.5, 44.5, -58.8, 40.5)]}, *shot("from_above"),
    {"cmds": ["/setblock 44 -58 38 minecraft:air", "/time set 18000", look(46.0, -60.0, 36.5, 46.0, -58.4, 40.5)]}, *shot("night_beside_a_creeper", 40),
    {"cmds": [look(44.5, -60.0, 38.0, 44.5, -58.2, 40.5)]}, *shot("night_front"),
    {"cmds": ["/time set 6000", "/data merge entity %s {Fuse:300,ignited:1b}" % ONE, look(46.0, -60.0, 37.0, 44.5, -58.4, 40.5)]},
    *shot("swelling_half", 150), *shot("swelling_most", 110),
    {"cmds": ["/kill %s" % ALL, "/kill @e[type=minecraft:creeper]"], "wait": 30},
    {"cmds": ["/kill @e[type=item]", "/summon %s 44.5 -60 40.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f],powered:1b}" % MOB, look(44.5, -60.0, 37.2, 44.5, -58.4, 40.5)]},
    *shot("charged", 30),
    {"cmds": ["/kill %s" % ALL], "wait": 30},
    {"cmds": ["/kill @e[type=item]", "/fill 38 -60 45 56 -59 45 minecraft:barrier", "/fill 38 -60 47 56 -59 47 minecraft:barrier", "/fill 38 -60 46 38 -59 46 minecraft:barrier",
              "/summon minecraft:cat 39.5 -60 46.5 {NoAI:1b}", "/summon %s 42.5 -60 46.5 {PersistenceRequired:1b,Rotation:[-90f,0f]}" % MOB, look(46.5, -60.0, 42.2, 46.0, -58.8, 46.5)]},
    *shot("walking_a", 22), *shot("walking_b", 3), *shot("walking_c", 3), *shot("walking_d", 3),
    {"cmds": ["/kill %s" % ALL, "/kill @e[type=minecraft:cat]", "/fill 38 -60 45 56 -59 47 minecraft:air"], "wait": 30},
    {"cmds": ["/kill @e[type=item]", "/fill 38 -60 42 52 -60 42 minecraft:grass_block", "/summon %s 45.5 -60 45.5 {NoAI:1b,ignited:1b}" % MOB, "/setblock 45 -57 36 minecraft:barrier",
              look(45.5, -56.0, 36.5, 45.5, -60.0, 45.5)]},
    *shot("burst", 70),
    {"cmds": ["/execute positioned 45.5 -60 45.5 run fill ~-7 ~-3 ~-7 ~7 ~3 ~7 minecraft:air replace criticalmass:fallout_ash", "/item replace entity @p hotbar.0 with criticalmass:irradiated_creeper_spawn_egg",
              "/data get entity @p Inventory"], "wait": 10},
    {"slot": 0, "gui": True, "wait": 5}, {"shot": "spawn_egg", "wait": 2},
]
json.dump({"name": "irradiated_creeper", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "irradiated_creeper.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
