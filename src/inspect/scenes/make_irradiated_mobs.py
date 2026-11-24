import json
import os

EYE = 1.62
MOBS = [("pig", 1.0), ("cow", 1.4), ("sheep", 1.3), ("chicken", 0.8), ("wolf", 0.9), ("zombie", 1.9), ("skeleton", 1.9), ("spider", 0.9), ("enderman", 2.9), ("villager", 1.9),
        ("horse", 1.6), ("rabbit", 0.6), ("cat", 0.7), ("husk", 1.9), ("witch", 1.9), ("pillager", 1.9), ("goat", 1.3), ("fox", 0.7), ("llama", 1.8), ("slime", 1.0)]
STILL = "NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f],ArmorItems:[{},{},{},{id:\"minecraft:stone_button\",count:1}]"
Z = 44.5
BACK = 49.5


def x(i):
    return 38.5 + i * 4


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px, py, pz, tx, ty - EYE, tz)


def shot(name, wait=20):
    return [{"wait": wait}, {"shot": name, "wait": 2}]


summons = []
for i, (mob, _) in enumerate(MOBS):
    extra = ",Size:2" if mob == "slime" else ""
    summons.append("/summon minecraft:%s %.1f -60 %.1f {%s,Tags:[\"rad\"]%s}" % (mob, x(i), Z, STILL, extra))
    summons.append("/summon minecraft:%s %.1f -60 %.1f {%s%s}" % (mob, x(i), BACK, STILL, extra))

steps = [
    {"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamerule doMobSpawning false", "/weather clear", "/difficulty normal", "/gamemode creative", "/kill @e[type=!player]",
              "/tp @p 46 -60 34"], "wait": 40, "gui": False, "camera": "first"},
    {"cmds": ["/fill 34 -61 30 122 -61 56 minecraft:smooth_stone", "/fill 34 -60 30 122 -52 56 minecraft:air", "/kill @e[type=item]"], "wait": 20},
    {"cmds": summons, "wait": 20},
    {"cmds": ["/criticalmass irradiate @e[tag=rad]", "/data get entity @e[type=minecraft:pig,tag=rad,limit=1] attributes"], "wait": 30},
]
for i, (mob, tall) in enumerate(MOBS):
    back = 3.6 + tall * 1.5
    steps += [{"cmds": [look(x(i) - 1.4, -60.0 + tall * 0.25, Z - back, x(i), -60 + tall * 0.75, Z + 1.5)]}, *shot(mob, 12)]
for mob in ("pig", "enderman", "cow", "skeleton", "chicken", "zombie", "spider", "wolf"):
    i = [m for m, _ in MOBS].index(mob)
    tall = MOBS[i][1]
    steps += [{"cmds": [look(x(i) + 2.0 + tall * 0.7, -60.0 + tall * 0.5, Z - 2.2 - tall * 0.9, x(i), -60 + tall * 0.75, Z)]}, *shot(mob + "_close", 12)]
    steps += [{"cmds": [look(x(i) - 2.2 - tall * 0.7, -60.0 + tall * 0.6, Z + 2.4 + tall * 0.8, x(i), -60 + tall * 0.75, Z)]}, *shot(mob + "_behind", 12)]
steps += [
    {"cmds": ["/time set 18000", look(x(4), -58.0, Z - 9, x(4), -59.0, Z)]}, *shot("night_row", 40),
    {"cmds": ["/time set 6000", "/kill @e[type=!player]"], "wait": 30},
    {"cmds": ["/kill @e[type=item]", "/fill 34 -60 30 122 -58 56 minecraft:air replace criticalmass:fallout_ash", "/gamemode survival", "/tp @p 46.5 -60 36.5 0 0",
              "/summon minecraft:pig 46.5 -60 44.5 {Tags:[\"rad\"]}", "/summon minecraft:cow 49.5 -60 44.5 {Tags:[\"rad\"]}", "/summon minecraft:chicken 43.5 -60 44.5 {Tags:[\"rad\"]}",
              "/criticalmass irradiate @e[tag=rad]"], "wait": 50},
    {"shot": "attack_a", "wait": 2}, {"wait": 30}, {"shot": "attack_b", "wait": 2},
    {"cmds": ["/data get entity @p Health", "/gamemode creative", "/kill @e[type=!player]"], "wait": 20},
]
json.dump({"name": "irradiated_mobs", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "irradiated_mobs.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
