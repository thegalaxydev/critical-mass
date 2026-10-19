import json
import os

O = (40, -60, 40)
EYE = 1.62
BENCH = (1, 0, 1)
STAND = (1.5, 0.0, 3.5)
DAMAGED = "criticalmass:damaged_fuel_assembly[criticalmass:fuel_state={id:[I;11,22,33,44],remaining:0.42d,condition:0.0d,burned:52000.0d,damage_cause:\"overheat\"}]"


def w(x, y, z):
    return "%d %d %d" % (x + O[0], y + O[1], z + O[2])


def look(px, py, pz, tx, ty, tz):
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (px + O[0], py + O[1], pz + O[2], tx + O[0], ty + O[1] - EYE, tz + O[2])


bench_view = look(STAND[0], STAND[1], STAND[2], BENCH[0] + 0.5, BENCH[1] + 1.0, BENCH[2] + 0.5)
use = [BENCH[0] + O[0], BENCH[1] + O[1], BENCH[2] + O[2], "up"]
give = ["/item replace entity @p hotbar.0 with criticalmass:empty_fuel_assembly",
        "/item replace entity @p hotbar.1 with criticalmass:fuel_cladding 4",
        "/item replace entity @p hotbar.2 with criticalmass:ceramic_fuel_pellet 8",
        "/item replace entity @p hotbar.3 with minecraft:air",
        "/item replace entity @p hotbar.4 with criticalmass:shielding_plate 2",
        "/item replace entity @p hotbar.5 with criticalmass:sealing_gasket 1",
        "/item replace entity @p hotbar.6 with " + DAMAGED,
        "/item replace entity @p armor.head with create:goggles"]
steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode survival", "/difficulty peaceful", "/tp @p %.1f -60 %.1f" % (O[0] + STAND[0], O[2] + STAND[2])],
          "wait": 40, "gui": True, "camera": "first"},
         {"cmds": ["/setblock %s criticalmass:fuel_assembly_bench[facing=south]" % w(*BENCH)] + give + [bench_view], "wait": 10},
         {"slot": 0, "wait": 2}, {"use": use, "wait": 5}, {"shot": "bench_empty", "wait": 2}]
for loop in range(4):
    steps += [{"slot": 1, "wait": 2}, {"use": use, "wait": 3},
              {"slot": 2, "wait": 2}, {"use": use, "wait": 3}, {"use": use, "wait": 3},
              {"slot": 3, "sneak": True, "wait": 4}, {"use": use, "wait": 3}, {"sneak": False, "wait": 4}]
    if loop == 0:
        steps += [{"cmds": [bench_view], "wait": 5}, {"shot": "bench_loop1", "wait": 2}]
steps += [{"cmds": [bench_view], "wait": 5}, {"shot": "bench_done", "wait": 2},
          {"slot": 3, "wait": 2}, {"use": use, "wait": 5, "inventory": True},
          {"slot": 6, "wait": 2}, {"use": use, "wait": 5}, {"cmds": [bench_view], "wait": 5}, {"shot": "bench_damaged", "wait": 2},
          {"slot": 5, "wait": 2}, {"use": use, "wait": 3}, {"shot": "bench_gasket_first", "wait": 2},
          {"slot": 4, "wait": 2}, {"use": use, "wait": 3}, {"use": use, "wait": 3},
          {"slot": 5, "wait": 2}, {"use": use, "wait": 5}, {"cmds": [bench_view], "wait": 5}, {"shot": "bench_canister", "wait": 2},
          {"slot": 3, "wait": 2}, {"use": use, "wait": 5, "inventory": True},
          {"jei": "criticalmass:fuel_assembly", "wait": 30}, {"shot": "jei_fuel_assembly", "wait": 2}, {"closeScreen": True, "wait": 5},
          {"jei": "criticalmass:uranium_concentrate", "wait": 30}, {"shot": "jei_concentrate", "wait": 2}, {"closeScreen": True, "wait": 5},
          {"jei": "criticalmass:ceramic_fuel_pellet", "wait": 30}, {"shot": "jei_pellet", "wait": 2}, {"closeScreen": True, "wait": 5},
          {"jei": "create:crushed_raw_uranium", "wait": 30}, {"shot": "jei_crushed", "wait": 2}, {"closeScreen": True, "wait": 5},
          {"cmds": ["/gamemode creative", "/fill %s %s minecraft:stone" % (w(6, 0, -6), w(14, 8, 2)), "/fill %s %s minecraft:deepslate" % (w(6, 0, -6), w(14, 2, 2)),
                    "/place feature criticalmass:uranium_ore %s" % w(10, 1, -2), "/place feature criticalmass:uranium_ore %s" % w(10, 6, -2),
                    "/fill %s %s minecraft:air replace minecraft:stone" % (w(6, 0, -6), w(14, 8, 2)), "/fill %s %s minecraft:air replace minecraft:deepslate" % (w(6, 0, -6), w(14, 2, 2)),
                    "/setblock %s criticalmass:uranium_ore" % w(4, 0, -1), "/setblock %s criticalmass:deepslate_uranium_ore" % w(5, 0, -1),
                    look(3.5, 0.0, 6.5, 9.0, 2.5, -2.0)], "wait": 40, "gui": False},
         {"shot": "ore", "wait": 2}]
json.dump({"name": "production", "steps": steps}, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "production.json"), "w", newline="\n"), indent=1)
print(len(steps), "steps")
