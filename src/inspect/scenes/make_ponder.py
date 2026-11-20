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


def pipe(*open_sides):
    return "create:fluid_pipe[%s]" % ",".join("%s=%s" % (side, "true" if side in open_sides else "false") for side in ("north", "south", "east", "west", "up", "down"))


def water_cannon():
    blocks = plate(9, "minecraft:grass_block")
    blocks += [((4, 1, 4), "create:gearbox[axis=x]"), ((4, 2, 4), "criticalmass:water_cannon")]
    blocks += [((4, 1, z), "create:shaft[axis=z]") for z in (5, 6, 7, 8)]
    blocks += [((4, 2, z), pipe("north", "south")) for z in (0, 1, 2)] + [((4, 2, 3), "create:mechanical_pump[facing=south]")]
    return blocks, []


RPIPE = "criticalmass:reinforced_pipe"
KIND = {"f": "criticalmass:fuel_channel", "r": "criticalmass:control_channel", "c": "criticalmass:coolant_channel"}


def settle(pipes):
    return ["setblock %d %d %d minecraft:air" % p for p, _ in pipes] + ["setblock %d %d %d %s" % (p[0], p[1], p[2], b) for p, b in pipes]


def vessel(o, rows, parts):
    blocks = []
    for x in range(5):
        for y in range(5):
            for z in range(5):
                pos = (o[0] + x, o[1] + y, o[2] + z)
                if pos in parts:
                    blocks.append((pos, parts[pos]))
                elif x in (0, 4) or y in (0, 4) or z in (0, 4):
                    blocks.append((pos, CASING))
                elif rows[z - 1][x - 1] in KIND:
                    blocks.append((pos, KIND[rows[z - 1][x - 1]]))
    return blocks


def reactor_vessel():
    parts = {(1, 3, 3): "criticalmass:reactor_controller[facing=west]", (3, 5, 3): "criticalmass:reactor_service_port[facing=up]",
             (2, 3, 1): "criticalmass:primary_coolant_port[facing=north,mode=inlet]", (4, 3, 1): "criticalmass:primary_coolant_port[facing=north,mode=outlet]"}
    parts.update({(3, y, 1): "criticalmass:reactor_viewport" for y in (2, 3, 4)})
    return plate(7) + vessel((1, 1, 1), ["cfc", "frf", "cfc"], parts), []


def rods():
    parts = {(1, 3, 3): "criticalmass:reactor_controller[facing=west]", (3, 5, 3): "criticalmass:reactor_service_port[facing=up]",
             (5, 3, 3): "criticalmass:primary_coolant_port[facing=east,mode=inlet]", (3, 3, 5): "criticalmass:primary_coolant_port[facing=south,mode=outlet]"}
    blocks = plate(7) + vessel((1, 1, 1), [".rf", "..c", "..."], parts)
    blocks += [((3, 6, 2), "criticalmass:control_rod_drive[facing=north]"), ((2, 6, 2), "create:cogwheel[axis=y]")]
    return blocks, []


def rod_control():
    parts = {(1, 3, 3): "criticalmass:reactor_controller[facing=west]", (3, 5, 3): "criticalmass:reactor_service_port[facing=up]",
             (5, 3, 3): "criticalmass:primary_coolant_port[facing=east,mode=inlet]", (3, 3, 5): "criticalmass:primary_coolant_port[facing=south,mode=outlet]"}
    blocks = plate(7) + vessel((1, 1, 1), [".rf", "..c", "..."], parts)
    blocks += [((3, 6, 2), "criticalmass:control_rod_drive[facing=north]{Bank:3,ScrollValue:3}"), ((2, 6, 2), "create:cogwheel[axis=y]")]
    desks = (2, 3, 4)
    blocks += [((x, 1, 0), "criticalmass:reactor_alloy_dashboard[facing=north,join_cw=%s,join_ccw=%s]" % (str(x + 1 in desks).lower(), str(x - 1 in desks).lower())) for x in desks]
    fitted = [(2, 0, "lever", 3, 7, "ROD LEVER D"), (2, 1, "gauge", 6, 4, "BANK D"), (3, 0, "button", 5, 4, "REG LOWER"), (3, 1, "button", 4, 2, "REG RAISE"),
              (4, 0, "gauge", 12, 4, "REG SETPOINT"), (4, 1, "gauge", 7, 4, "NEUTRON POWER")]
    return blocks, ["criticalmass fuel 1 3 3 fill"] + ["criticalmass desk %d 1 0 %d %s 1 3 3 %d %d %s" % part for part in fitted]


def primary_loop():
    parts = {(6, 3, 4): "criticalmass:reactor_controller[facing=north]", (6, 5, 6): "criticalmass:reactor_service_port[facing=up]",
             (4, 2, 6): "criticalmass:primary_coolant_port[facing=west,mode=outlet]", (5, 2, 4): "criticalmass:primary_coolant_port[facing=north,mode=inlet]"}
    blocks = plate(9) + vessel((4, 1, 4), ["cfc", "frf", "cfc"], parts)
    blocks += [((2, 1, 6), "create:shaft[axis=y]"), ((2, 2, 6), "criticalmass:circulation_pump[facing=west,axis_along_first=true]"),
               ((1, 2, 4), "criticalmass:heat_exchanger[facing=north]"), ((1, 3, 4), "criticalmass:heat_exchanger[facing=north]"),
               ((7, 1, 3), "create:blaze_burner[blaze=kindled]"), ((7, 2, 3), "criticalmass:pressurizer")]
    pipes = [((p), RPIPE) for p in ((3, 2, 6), (1, 2, 6), (1, 2, 5), (1, 2, 3), (1, 2, 2), (2, 2, 2), (3, 2, 2), (4, 2, 2), (5, 2, 2), (5, 2, 3))]
    pipes += [((1, 1, 4), pipe("up", "down")), ((1, 4, 4), pipe("up", "down"))]
    return blocks + pipes, settle(pipes)


def steam():
    blocks = plate(9)
    blocks += [((4, 6, 4), "minecraft:air"), ((4, 5, 4), "criticalmass:steam_turbine[facing=west]"), ((2, 5, 4), "create:shaft[axis=x]"), ((1, 5, 4), "create:shaft[axis=x]"),
               ((7, 3, 4), "criticalmass:heat_exchanger[facing=east]"), ((7, 5, 4), "criticalmass:relief_valve[facing=north]"),
               ((1, 2, 4), "criticalmass:condenser[facing=west]"), ((5, 1, 2), "create:shaft[axis=y]"),
               ((5, 2, 2), "criticalmass:circulation_pump[facing=east,axis_along_first=true]")]
    ring = [(7, 4, 4), (6, 5, 4), (4, 3, 4), (4, 2, 4), (3, 2, 4), (2, 2, 4), (0, 2, 4), (0, 2, 3), (0, 2, 2), (1, 2, 2), (2, 2, 2), (3, 2, 2), (4, 2, 2),
            (6, 2, 2), (7, 2, 2), (7, 2, 3), (7, 2, 4), (6, 3, 4), (8, 3, 4)]
    pipes = [(p, RPIPE) for p in ring] + [((1, 3, 4), pipe("up", "down")), ((1, 1, 4), pipe("up", "down"))]
    return blocks + pipes, settle(pipes)


def cooling():
    blocks = plate(7)
    blocks += [((3, y, 3), "criticalmass:cooling_tower_packing") for y in (2, 3, 4, 5)]
    blocks += [((3, 6, 3), "create:encased_fan[facing=down]"), ((3, 7, 3), "create:shaft[axis=y]"), ((5, 2, 1), "criticalmass:radiator_panel[facing=north]")]
    pipes = [((3, 1, 3), pipe("up", "north"))] + [((3, 1, z), pipe("north", "south")) for z in (0, 1, 2)]
    pipes += [((3, 5, z), pipe("north", "south")) for z in (0, 1, 2)] + [((5, 3, 1), pipe("up", "down")), ((5, 1, 1), pipe("up", "down"))]
    water = ["data merge block 3 %d 3 {Buffer:{Fluid:{id:\"minecraft:water\",amount:%d},Heat:0.0d}}" % (y, 300) for y in (2, 3, 4, 5)]
    return blocks + pipes, settle(pipes) + water


def refuelling():
    blocks = plate(7)
    blocks += [((x, y, z), CASING) for x in (3, 4, 5) for y in (1, 2) for z in (3, 4, 5) if (x, y, z) != (4, 2, 4)]
    blocks += [((4, 2, 4), "criticalmass:reactor_service_port[facing=up]"), ((4, 3, 4), "create:copper_valve_handle[facing=up]"),
               ((1, 1, 4), "criticalmass:spent_fuel_rack"), ((4, 1, 1), "criticalmass:shielded_cask")]
    return blocks, []


def feedwater():
    blocks = plate(9)
    blocks += [((6, 2, 3), "criticalmass:heat_exchanger[facing=east]"), ((4, 2, 3), "criticalmass:condenser[facing=west]"), ((2, 2, 3), "create:fluid_tank"),
               ((2, 2, 2), "create:stockpile_switch[facing=south,target=wall]"), ((3, 1, 5), "create:shaft[axis=y]"),
               ((3, 2, 5), "criticalmass:circulation_pump[facing=east,axis_along_first=true]"), ((1, 2, 3), "create:mechanical_pump[facing=east]"),
               ((1, 3, 3), "create:cogwheel[axis=x]"), ((0, 3, 3), "create:clutch[axis=x]")]
    ring = [(8, 2, 3), (7, 2, 3), (5, 2, 3), (3, 2, 3), (2, 2, 4), (2, 2, 5), (4, 2, 5), (5, 2, 5), (6, 2, 5), (6, 1, 5), (6, 1, 4), (6, 1, 3), (6, 3, 3), (6, 4, 3)]
    pipes = [(p, RPIPE) for p in ring] + [((4, 3, 3), pipe("up", "down")), ((4, 1, 3), pipe("up", "down")), ((0, 2, 3), pipe("east", "west"))]
    return blocks + pipes, settle(pipes)


def siren():
    blocks = plate(7)
    blocks += [((1, y, z), "criticalmass:reactor_vessel_casing") for y in (1, 2) for z in (3, 4, 5) if (y, z) != (1, 4)]
    blocks += [((1, 1, 4), "criticalmass:reactor_controller[facing=west]"), ((5, 1, 5), "criticalmass:shielding_concrete"), ((5, 2, 5), "criticalmass:shielding_concrete")]
    blocks += [((5, 2, 4), "criticalmass:meltdown_siren[facing=north,wall=true]"), ((3, 1, 2), "criticalmass:meltdown_siren[facing=north,wall=false]"),
               ((4, 1, 2), "minecraft:lever[face=floor,facing=north,powered=false]")]
    return blocks, []


def enrichment():
    blocks = plate(7)
    blocks += [((3, y, 3), "criticalmass:centrifuge") for y in (1, 2, 3)] + [((3, 4, 3), "create:shaft[axis=y]")]
    return blocks, []


DEPLOYER = "create:deployer[facing=down,axis_along_first=false]"
PRESS = "create:mechanical_press[facing=north]"
BURNER = "create:blaze_burner[blaze=kindled]"


def fuel_assembly():
    blocks = plate(7)
    blocks += [((1, 1, 2), BURNER), ((1, 2, 2), "create:basin"), ((1, 4, 2), PRESS)]
    blocks += [((4, 1, 3), "create:depot"), ((4, 3, 3), DEPLOYER), ((3, 3, 3), PRESS)]
    return blocks, []


def reprocessing():
    blocks = plate(9)
    blocks += [((1, 1, 6), "criticalmass:spent_fuel_rack"), ((1, 1, 4), "create:shaft[axis=x]")]
    blocks += [((x, 1, 4), "create:mechanical_saw[facing=up,axis_along_first=true,flipped=false]") for x in (2, 3, 4, 5)]
    blocks += [((7, 1, 4), BURNER), ((7, 2, 4), "create:basin"), ((7, 4, 4), "create:mechanical_mixer")]
    return blocks, ["data merge block 1 1 6 {Buffer:{Fluid:{id:\"minecraft:water\",amount:4000},Heat:0.0d}}"]


def waste_canister():
    blocks = plate(7)
    blocks += [((3, 1, 3), "create:depot"), ((3, 3, 3), "create:spout"), ((2, 3, 3), DEPLOYER), ((4, 3, 3), PRESS)]
    blocks += [((1, 1, 5), "criticalmass:sealed_waste_canister"), ((5, 1, 5), "criticalmass:shielded_cask")]
    return blocks, []


def control_desk():
    blocks = plate(7)
    blocks += [((x, y, 5), CASING) for x in (2, 3, 4) for y in (1, 2, 3) if (x, y) != (3, 2)]
    blocks += [((3, 2, 5), "criticalmass:reactor_controller[facing=north]"), ((1, 1, 4), "create:redstone_link[receiver=true,facing=up,powered=false]"),
               ((0, 1, 4), "minecraft:redstone_lamp"), ((3, 3, 4), "criticalmass:part_panel[face=wall,facing=north]{Centered:1b}")]
    desks = (2, 3, 4)
    blocks += [((x, 1, 2), "criticalmass:reactor_alloy_dashboard[facing=north,join_cw=%s,join_ccw=%s]" % (str(x + 1 in desks).lower(), str(x - 1 in desks).lower())) for x in desks]
    fitted = [(2, 0, "button", "3 2 5", 0, 1, "SCRAM"), (2, 1, "alarm", "3 2 5", 0, 0, "ALARM"), (3, 0, "gauge", "3 2 5", 6, 4, "NEUTRON POWER"),
              (3, 1, "gauge", "3 2 5", 1, 4, "COOLANT"), (4, 0, "switch", "1 1 4", 0, 2, "PUMP"), (4, 1, "lever", "1 1 4", 0, 4, "VALVE")]
    return blocks, ["criticalmass desk %d 1 2 %d %s %s %d %d %s" % part for part in fitted] + ["criticalmass desk 3 3 4 0 gauge 3 2 5 3 4 LEVEL"]


def plant_screen():
    blocks = plate(9)
    blocks += [((x, y, 6), "criticalmass:plant_screen[facing=north]") for x in (2, 3, 4, 5, 6) for y in (1, 2, 3)]
    blocks += [((7, 1, 6), "create:cogwheel[axis=z]"), ((4, 1, 2), "create:shaft[axis=y]"), ((4, 2, 2), "criticalmass:circulation_pump[facing=west,axis_along_first=true]"),
               ((4, 3, 2), "create:display_link[facing=up,powered=false]")]
    return blocks, []


def recovery():
    concrete = "criticalmass:shielding_concrete"
    blocks = [(pos, concrete if 1 <= pos[0] <= 7 and 1 <= pos[2] <= 7 else b) for pos, b in plate(9)]
    blocks += [((x, y, 7), concrete) for x in range(1, 8) for y in (1, 2, 3)] + [((7, y, z), concrete) for z in range(3, 7) for y in (1, 2, 3)]
    blocks += [((4, 1, 4), "criticalmass:corium[hot=true]"), ((2, 1, 6), "criticalmass:core_debris"), ((6, 1, 5), "criticalmass:core_debris"), ((5, 1, 2), "criticalmass:core_debris"),
               ((2, 1, 3), "criticalmass:wreckage"), ((3, 1, 3), "criticalmass:wreckage"), ((2, 2, 3), "criticalmass:wreckage"), ((1, 1, 1), "criticalmass:shielded_cask"),
               ((5, 1, 6), "create:fluid_tank")]
    return blocks, []


SCENES = {"rod_control": rod_control, "siren": siren, "wires": wires, "breaker": breaker, "synchronising": synchronising, "standby": standby, "repair": repair, "bypass": bypass, "meltdown": meltdown, "fallout": fallout,
          "reactor_vessel": reactor_vessel, "rods": rods, "primary_loop": primary_loop, "steam": steam, "cooling": cooling, "refuelling": refuelling, "water_cannon": water_cannon, "feedwater": feedwater,
          "enrichment": enrichment, "fuel_assembly": fuel_assembly, "reprocessing": reprocessing, "waste_canister": waste_canister, "control_desk": control_desk,
          "plant_screen": plant_screen, "recovery": recovery}
VIEW = {"rod_control": "criticalmass:dashboard_lever", "siren": "criticalmass:meltdown_siren", "wires": "criticalmass:power_wire", "breaker": "criticalmass:circuit_breaker", "synchronising": "criticalmass:generator", "standby": "criticalmass:transfer_switch",
        "repair": "criticalmass:transformer", "bypass": "criticalmass:dashboard_switch", "meltdown": "criticalmass:corium", "fallout": "criticalmass:survey_meter",
        "reactor_vessel": "criticalmass:reactor_vessel_casing", "rods": "criticalmass:control_rod_drive", "primary_loop": "criticalmass:circulation_pump",
        "steam": "criticalmass:steam_turbine", "cooling": "criticalmass:cooling_tower_packing", "refuelling": "criticalmass:spent_fuel_rack", "water_cannon": "criticalmass:water_cannon", "feedwater": "create:stockpile_switch",
        "enrichment": "criticalmass:centrifuge", "fuel_assembly": "criticalmass:ceramic_fuel_pellet", "reprocessing": "criticalmass:sheared_fuel",
        "waste_canister": "criticalmass:open_waste_canister", "control_desk": "criticalmass:reactor_alloy_dashboard", "plant_screen": "criticalmass:plant_screen", "recovery": "criticalmass:core_debris"}
SCROLL = {"feedwater": 1}


def build(names=None):
    steps = [{"cmds": ["/gamemode creative", "/gamerule doTileDrops false", "/forceload add -16 -16 31 31", "/tp @p 4 6 12"], "wait": 60, "gui": False}]
    for name, make in SCENES.items():
        if names and name not in names:
            continue
        blocks, extra = make()
        size = [max(p[i] for p, _ in blocks) + 1 for i in range(3)]
        steps.append({"cmds": ["/fill -1 0 -1 15 10 15 minecraft:air"], "wait": 10})
        steps.append({"cmds": ["/setblock %d %d %d %s" % (p[0], p[1], p[2], b) for p, b in blocks], "wait": 10})
        steps.append({"cmds": ["/" + c for c in extra], "wait": 30})
        steps.append({"structure": {"from": [0, 0, 0], "size": size, "file": "src/main/resources/assets/criticalmass/ponder/%s.nbt" % name}, "wait": 10})
    return steps


def view(names, lang):
    steps = [{"cmds": ["/gamemode creative", "/time set 6000"], "wait": 60, "gui": True}]
    if lang:
        steps.append({"ponderLang": lang, "wait": 5})
    for name in names:
        steps.append({"ponder": VIEW[name], "wait": 40})
        if name in SCROLL:
            steps.append({"ponderScroll": SCROLL[name], "wait": 20})
        for i, at in enumerate((60, 250, 500, 750, 1000, 1250, 1500, 1750)):
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
        steps, name = build([n for n in sys.argv[2].split(",") if n] if len(sys.argv) > 2 and sys.argv[2] != "all" else None), "ponder_build"
    else:
        names = [n for n in sys.argv[2].split(",") if n] if len(sys.argv) > 2 and sys.argv[2] != "all" else list(SCENES)
        steps, name = view(names, sys.argv[3] if len(sys.argv) > 3 else None), "ponder"
    json.dump({"name": name, "steps": steps}, open(os.path.join(HERE, name + ".json"), "w", newline="\n"), indent=1)
    print(len(steps), "steps")
