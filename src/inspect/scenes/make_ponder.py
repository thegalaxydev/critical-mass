import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.normpath(os.path.join(HERE, "..", "..", "main", "resources", "assets", "criticalmass", "ponder"))
LIGHT, DARK = "minecraft:white_concrete", "minecraft:snow_block"
STEEL = "criticalmass:steel_block"
CASING = "criticalmass:reactor_vessel_casing"
UP = "criticalmass:insulator[facing=up]"


def plate(size, floor=None):
    return [((x, 0, z), floor or (LIGHT if (x + z) % 2 == 0 else DARK)) for x in range(size) for z in range(size)]


def wires():
    blocks = plate(7)
    blocks += [((1, 1, 5), "criticalmass:transformer[facing=north]{ScrollValue:2}"), ((5, 1, 5), "criticalmass:bus_bar[axis=x]"), ((3, 1, 5), STEEL), ((3, 2, 5), UP)]
    for z in (1, 2, 3):
        blocks += [((1, 1, z), UP), ((5, 1, z), UP)]
    return blocks, []


def breaker():
    blocks = plate(7)
    blocks += [((1, 1, 3), "criticalmass:bus_bar[axis=z]"), ((3, 1, 3), "criticalmass:circuit_breaker[facing=south,closed=false]"),
               ((5, 1, 3), "criticalmass:electric_motor[facing=east]{ScrollValue:32}"), ((6, 1, 3), "create:shaft[axis=x]")]
    return blocks, []


def synchronising():
    blocks = plate(9)
    blocks += [((0, 1, 4), "create:shaft[axis=x]"), ((1, 1, 4), "criticalmass:generator[axis=x]"), ((3, 1, 4), "criticalmass:circuit_breaker[facing=south,closed=false]"),
               ((5, 1, 4), "criticalmass:transformer[facing=north]{ScrollValue:0}"), ((7, 1, 4), "criticalmass:grid_connection")]
    return blocks, []


def standby():
    blocks = plate(9)
    blocks += [((1, 1, 5), "criticalmass:diesel_engine[facing=east]"), ((2, 1, 5), "criticalmass:generator[axis=x]"), ((4, 1, 5), "criticalmass:transformer[facing=south]{ScrollValue:2}"),
               ((6, 1, 5), "criticalmass:transfer_switch[facing=north]"), ((1, 1, 2), "criticalmass:station_battery[facing=south]"),
               ((7, 1, 2), "criticalmass:circuit_breaker[facing=south,closed=true]")]
    blocks += [((x, 1, 2), "criticalmass:bus_bar[axis=x]") for x in (3, 4, 5)]
    blocks += [((x, 1, 4), "minecraft:redstone_wire") for x in range(1, 7)]
    return blocks, []


def repair():
    blocks = plate(7)
    blocks += [((3, 1, 3), "criticalmass:transformer[facing=north]{ScrollValue:2}"), ((1, 1, 3), UP), ((5, 1, 3), UP)]
    return blocks, []


def bypass():
    blocks = plate(7)
    blocks += [((x, y, 5), CASING) for x in (2, 3, 4) for y in (1, 2, 3) if (x, y) != (3, 2)]
    blocks += [((3, 2, 5), "criticalmass:reactor_controller[facing=north]"), ((3, 1, 2), "criticalmass:reactor_alloy_dashboard[facing=north,join_cw=false,join_ccw=false]")]
    return blocks, ["criticalmass desk 3 1 2 0 switch 3 2 5 0 1 TRIP BYPASS", "criticalmass desk 3 1 2 1 alarm 3 2 5 0 0 ALARM"]


def meltdown():
    blocks = [(pos, "criticalmass:shielding_concrete" if 2 <= pos[0] <= 4 and 2 <= pos[2] <= 4 else b) for pos, b in plate(7)]
    blocks += [((x, 1, z), STEEL) for x in (2, 4) for z in (2, 4) if (x, z) != (2, 2)]
    blocks += [((x, 2, z), CASING) for x in (2, 3, 4) for z in (2, 3, 4)]
    blocks += [((4, y, z), CASING) for y in (3, 4) for z in (2, 3, 4)] + [((x, y, 4), CASING) for y in (3, 4) for x in (2, 3)]
    for y in (3, 4):
        blocks += [((2, y, 2), "criticalmass:fuel_channel"), ((3, y, 2), "criticalmass:coolant_channel"), ((2, y, 3), "criticalmass:control_channel"), ((3, y, 3), "criticalmass:fuel_channel")]
    return blocks, []


def fallout():
    blocks = plate(9, "minecraft:grass_block")
    for x in (5, 7):
        for z in (5, 7):
            blocks += [((x, 1, z), "minecraft:oak_fence"), ((x, 2, z), "minecraft:oak_fence")]
    blocks += [((x, 3, z), "minecraft:smooth_stone_slab[type=bottom]") for x in (5, 6, 7) for z in (5, 6, 7)]
    blocks += [((1, 1, 6), "criticalmass:charred_log"), ((1, 2, 6), "criticalmass:charred_log"), ((2, 1, 2), "minecraft:short_grass"), ((6, 1, 1), "minecraft:short_grass"), ((3, 1, 4), "minecraft:short_grass")]
    return blocks, []


SCENES = {"wires": wires, "breaker": breaker, "synchronising": synchronising, "standby": standby, "repair": repair, "bypass": bypass, "meltdown": meltdown, "fallout": fallout}
VIEW = {"wires": "criticalmass:power_wire", "breaker": "criticalmass:circuit_breaker", "synchronising": "criticalmass:generator", "standby": "criticalmass:transfer_switch",
        "repair": "criticalmass:transformer", "bypass": "criticalmass:dashboard_switch", "meltdown": "criticalmass:corium", "fallout": "criticalmass:survey_meter"}


def build():
    steps = [{"cmds": ["/gamemode creative", "/gamerule doTileDrops false", "/forceload add -16 -16 31 31", "/tp @p 4 6 12"], "wait": 60, "gui": False}]
    for name, make in SCENES.items():
        blocks, extra = make()
        size = [max(p[i] for p, _ in blocks) + 1 for i in range(3)]
        steps.append({"cmds": ["/fill -1 0 -1 15 8 15 minecraft:air"], "wait": 10})
        steps.append({"cmds": ["/setblock %d %d %d %s" % (p[0], p[1], p[2], b) for p, b in blocks] + ["/" + c for c in extra], "wait": 30})
        steps.append({"structure": {"from": [0, 0, 0], "size": size, "file": "src/main/resources/assets/criticalmass/ponder/%s.nbt" % name}, "wait": 10})
    return steps


def view(names, lang):
    steps = [{"cmds": ["/gamemode creative", "/time set 6000"], "wait": 60, "gui": True}]
    if lang:
        steps.append({"ponderLang": lang, "wait": 5})
    for name in names:
        steps.append({"ponder": VIEW[name], "wait": 40})
        for i, at in enumerate((60, 200, 400, 600, 800, 1100)):
            steps += [{"ponderSeek": at, "wait": 30}, {"shot": "%s_%d" % (name, i), "wait": 2}]
        steps.append({"closeScreen": True, "wait": 5})
    return steps


def lang(dump):
    path = os.path.normpath(os.path.join(OUT, "..", "lang", "en_us.json"))
    held = json.load(open(path, encoding="utf8"))
    held = {k: v for k, v in held.items() if not k.startswith("criticalmass.ponder.")}
    held.update(sorted(json.load(open(dump, encoding="utf8")).items()))
    with open(path, "w", encoding="utf8", newline="\n") as f:
        f.write(json.dumps(held, indent=2, ensure_ascii=False) + "\n")
    print(len(held), "lines")


if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else "build"
    if mode == "lang":
        lang(sys.argv[2])
        sys.exit()
    if mode == "build":
        steps, name = build(), "ponder_build"
    else:
        names = [n for n in sys.argv[2].split(",") if n] if len(sys.argv) > 2 and sys.argv[2] != "all" else list(SCENES)
        steps, name = view(names, sys.argv[3] if len(sys.argv) > 3 else None), "ponder"
    json.dump({"name": name, "steps": steps}, open(os.path.join(HERE, name + ".json"), "w", newline="\n"), indent=1)
    print(len(steps), "steps")
