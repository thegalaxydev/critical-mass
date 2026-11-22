import json
import math
import os
import sys

NS = "criticalmass_factory"
TAG = "cm_factory_origin"
M = "criticalmass:"

STEEL_BLOCK = "criticalmass:steel_block"
SHELL = "minecraft:light_gray_concrete"
PLINTH = "minecraft:gray_concrete"
BAND = "minecraft:cyan_terracotta"
FLOOR = "minecraft:smooth_stone"
CLEAN = "minecraft:light_gray_concrete"
YELLOW = "minecraft:yellow_concrete"
BLACK = "minecraft:black_concrete"
LAMP = "minecraft:sea_lantern"
BARS = "minecraft:iron_bars"
IRON = "create:industrial_iron_block"
WINDOW = "create:industrial_iron_window"
GRATE = "minecraft:iron_trapdoor[half=top,open=false,facing=north]"
STAIR = "minecraft:polished_andesite_stairs[facing=%s,half=bottom]"
GIRDER_X = "create:metal_girder[axis=x,x=true,z=false]"
GIRDER_Z = "create:metal_girder[axis=z,x=false,z=true]"
ROD = "minecraft:lightning_rod[facing=up]"
GRAVEL = "minecraft:gravel"

KINDLED = "create:blaze_burner[blaze=kindled]{isCreative:1b}"
SEETHING = "create:blaze_burner[blaze=seething]{isCreative:1b,fuelLevel:2,burnTimeRemaining:10000}"
WATER_TANK = 'create:creative_fluid_tank{Size:1,Height:1,TankContent:{Fluid:{id:"minecraft:water",amount:1000}}}'
TUNNEL = "create:brass_tunnel[axis=x,shape=straight]{ScrollValue:6}"
CASING = "create:andesite_casing"
VAULT = "create:item_vault[axis=x,large=false]"
SINK = "create:creative_crate"
SIDES = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east"}
TUNNEL_Z = "create:brass_tunnel[axis=z,shape=straight]{ScrollValue:6}"
TANK = "create:fluid_tank"
LABELS = {"criticalmass:chartrine": "Chartrine", "minecraft:iron_ingot": "Iron", "minecraft:charcoal": "Charcoal", "create:zinc_ingot": "Zinc", "minecraft:quartz": "Quartz",
          "minecraft:dried_kelp": "Dried Kelp", "minecraft:iron_nugget": "Iron Nuggets", "minecraft:lapis_lazuli": "Lapis", "create:scoria": "Scoria", "minecraft:coal": "Coal",
          "minecraft:gravel": "Gravel"}
BAYS = ["mill", "enrichment", "melt", "assembly", "fluids", "shops"]
WING = ("fluids", "shops")

machines = {}
drives = []
stops = []
bins = []
late = []
world = {}
attached = []
wires = []
bay = ["mill"]


def machine(pos, block):
    assert pos not in machines, "two machines at %s: %s and %s" % (pos, machines[pos], block)
    machines[pos] = block


def off(pos, facing, n=1):
    dx, dz = SIDES[facing]
    return (pos[0] + dx * n, pos[1], pos[2] + dz * n)


def up(pos, n=1):
    return (pos[0], pos[1] + n, pos[2])


def motor(pos, facing, speed, name, heavy=False):
    assert pos not in machines and all(d[0] != pos for d in drives), "a motor in the way at %s" % (pos,)
    drives.append((pos, facing, speed, name, bay[0], heavy))


def drive(shaft, side, speed, name):
    axis = "z" if side in ("north", "south") else "x"
    at = off(shaft, side)
    machine(at, "create:shaft[axis=%s]" % axis)
    motor(off(at, side), OPPOSITE[side], speed, name)


def belt(cells, facing, side=None, speed=32, name=""):
    for i, pos in enumerate(cells):
        machine(pos, "create:belt[facing=%s,part=%s,slope=horizontal]" % (facing, "start" if i == 0 else "end" if i == len(cells) - 1 else "middle"))
    if side:
        drive(cells[0], side, speed, name)
    return cells


def run(a, b):
    axis = 0 if a[0] != b[0] else 2
    step = 1 if b[axis] > a[axis] else -1
    return [tuple(v if i == axis else a[i] for i in range(3)) for v in range(a[axis], b[axis] + step, step)]


def dock(pos, item, facing, amount=0):
    machine(pos, 'create:creative_crate{Filter:{id:"%s",count:1}}' % item)
    if pos[1] == 2:
        machine((pos[0], 1, pos[2]), CASING)
    lip = off(pos, facing)
    if amount:
        machine(lip, 'create:brass_belt_funnel[facing=%s,shape=retracted]{Filter:{id:"%s",count:1},FilterAmount:%d,UpTo:0b}' % (facing, item, amount))
    else:
        machine(lip, "create:andesite_belt_funnel[facing=%s,shape=retracted]" % facing)
    bins.append((pos, LABELS[item], "Bin"))


def lip(pos, facing):
    machine(off(pos, facing), "create:andesite_belt_funnel[facing=%s,shape=retracted]" % facing)
    return (pos[0] + SIDES[facing][0], pos[1] - 1, pos[2] + SIDES[facing][1])


def take(end, facing):
    machine(up(end), "create:andesite_belt_funnel[facing=%s,shape=retracted]" % OPPOSITE[facing])
    return up(off(end, facing))


def pull(cell, toward, item=None):
    block = "create:%s_belt_funnel[facing=%s,shape=pulling]" % ("brass" if item else "andesite", OPPOSITE[toward])
    machine(up(cell), block + ('{Filter:{id:"%s",count:1}}' % item if item else ""))
    return up(off(cell, toward))


def push(cell, source_side):
    machine(up(cell), "create:andesite_belt_funnel[facing=%s,shape=pushing]" % OPPOSITE[source_side])


def store(pos, block=VAULT, label=None):
    machine(pos, block)
    if pos[1] > 1 and (pos[0], pos[1] - 1, pos[2]) not in machines:
        machine((pos[0], pos[1] - 1, pos[2]), CASING)
    if label:
        bins.append((pos,) + label)


def basin(pos, out, tool, heat, cog_side, name, only=None, speed=128):
    machine(pos, "create:basin[facing=%s]" % out + ('{Filter:{id:"%s",count:1}}' % only if only else ""))
    machine((pos[0], pos[1] - 1, pos[2]), heat or CASING)
    head = up(pos, 2)
    if tool == "mixer":
        machine(head, "create:mechanical_mixer")
        cog = off(head, cog_side)
        machine(cog, "create:cogwheel[axis=y]")
        motor(up(cog), "down", speed, name)
    else:
        machine(head, "create:mechanical_press[facing=%s]" % cog_side)
        drive(head, cog_side, speed, name)


def press(cell, side, name):
    head = up(cell, 2)
    machine(head, "create:mechanical_press[facing=%s]" % side)
    drive(head, side, 64, name)


def sync(cells, tunnel=TUNNEL):
    for cell in cells:
        machine(up(cell), tunnel)
        late.append((up(cell), "{ScrollValue:6}"))


def stop(pos):
    stops.append(pos)


def rel(arm, target):
    return "[I;%d,%d,%d]" % tuple(target[i] - arm[i] for i in range(3))


def within(name, part, *args):
    was = bay[0]
    bay[0] = name
    out = part(*args)
    bay[0] = was
    return out


def arm(pos, takes, gives, cog_side, name, speed=128):
    for target, _ in takes + gives:
        assert math.dist(pos, target) < 5, "the %s arm cannot reach %s" % (name, target)
    points = ['{Type:"create:%s",Pos:%s,Mode:"TAKE"}' % (kind, rel(pos, t)) for t, kind in takes] + ['{Type:"create:%s",Pos:%s,Mode:"DEPOSIT"}' % (kind, rel(pos, t)) for t, kind in gives]
    machine(pos, "create:mechanical_arm[ceiling=false]{InteractionPoints:[%s]}" % ",".join(points))
    cog = off(pos, cog_side)
    if cog not in machines:
        machine(cog, "create:cogwheel[axis=y]")
        motor(up(cog), "down", speed, name)


U, MZ = 0, -10
ORE = (2, 6, U)
CLAY, GLOW, CLAY_SINK = (7, 2, U + 1), (8, 2, U + 1), (9, 2, U + 1)
CONCENTRATE = (12, 2, U)
CENTRIFUGES = [(x, 2, U + 1) for x in (15, 16, 17, 18)]
DEPLETED = (20, 2, U + 3)
PELLET = (24, 2, U + 2)
STEEL = (10, 2, MZ)
STEEL_STORE = (14, 2, MZ)
ALLOY = (22, 2, MZ)
SHEET_STORE = (33, 2, MZ)
SHEETS = (33, 1, MZ + 3)
GASKET = (34, 2, -5)
GASKETS = (40, 1, -6)
PELLET_STORE, PELLET_SETS = (36, 2, -4), (38, 2, -5)
CRAFT_X, CRAFT_Z = 37, -8
RX, RZ = 41, -10
CLADDING = (40, 1, MZ - 1)
PELLETS = (42, 2, -9)
FUEL = (43, 2, -5)


def mill():
    dock(ORE, M + "chartrine", "east", 16)
    belt(run((3, 5, U), (4, 5, U)), "east", "south", 32, "ore feed")
    for x, speed in ((4, 128), (6, -128)):
        machine((x, 3, U), "create:crushing_wheel[axis=z]")
        drive((x, 3, U), "south", speed, "crusher")
    stop((6, 4, U))
    stop((6, 5, U))
    m1 = belt(run((5, 1, U), (11, 1, U)), "east", "south", 32, "mill belt")
    assert pull(m1[2], "south", "minecraft:clay_ball") == CLAY and pull(m1[3], "south", "minecraft:glowstone_dust") == GLOW
    assert pull(m1[4], "south", "minecraft:clay_ball") == CLAY_SINK
    store(CLAY, VAULT, ("Clay", "Store"))
    store(GLOW, SINK, ("Glowstone", "Dispatch"))
    store(CLAY_SINK, SINK, ("Clay", "Dispatch"))
    assert take(m1[-1], "east") == CONCENTRATE
    basin(CONCENTRATE, "east", "mixer", None, "west", "concentrate mixer")
    pump = off(CONCENTRATE, "south")
    machine(pump, "create:mechanical_pump[facing=north]")
    machine(off(pump, "south"), WATER_TANK)
    machine(up(pump), "create:cogwheel[axis=z]")
    motor(off(up(pump), "south"), "north", 32, "water pump")


def enrichment():
    e1 = belt(run((13, 1, U), (20, 1, U)), "east", "north", -32, "concentrate belt")
    stop((21, 1, U))
    e2 = belt(run((15, 1, U + 2), (23, 1, U + 2)), "east", "south", 32, "product belt")
    for i, pos in enumerate(CENTRIFUGES):
        assert pull(e1[2 + i], "south") == pos
        machine(pos, M + "centrifuge")
        machine(up(pos), "create:encased_chain_drive[axis=y]")
        push(e2[i], "north")
    for pos in (CENTRIFUGES[0], CENTRIFUGES[2]):
        motor(up(pos, 2), "down", 128, "centrifuges", True)
    assert pull(e2[5], "south", M + "depleted_uranium") == DEPLETED
    store(DEPLETED, VAULT, ("Depleted", "Uranium"))
    assert take(e2[-1], "east") == PELLET
    basin(PELLET, "east", "press", KINDLED, "south", "pellet press")
    belt(run((25, 1, U + 2), (35, 1, U + 2)), "east", "south", 32, "pellet belt")
    riser = belt(run((36, 1, U + 2), (36, 1, -3)), "north", "east", 32, "pellet riser")
    assert take(riser[-1], "north") == PELLET_STORE
    store(PELLET_STORE, VAULT, ("Pellet", "Store"))


def melt_shop():
    dock((2, 2, MZ), "minecraft:iron_ingot", "east")
    dock((2, 2, MZ + 1), "minecraft:charcoal", "east")
    trunk = belt(run((3, 1, MZ), (9, 1, MZ)), "east", "north", -32, "steel feed")
    side = belt(run((3, 1, MZ + 1), (5, 1, MZ + 1)), "east", "south", 32, "steel feed")
    sync([trunk[2], side[2]])
    stop((6, 1, MZ + 1))
    assert take(trunk[-1], "east") == STEEL
    basin(STEEL, "east", "mixer", SEETHING, "north", "steel mixer")
    s1 = belt(run((11, 1, MZ), (13, 1, MZ)), "east", "south", 32, "steel belt")
    assert take(s1[-1], "east") == STEEL_STORE
    store(STEEL_STORE, VAULT, ("Steel", "Store"))
    dock((14, 2, MZ + 1), "create:zinc_ingot", "east")
    dock((14, 2, MZ - 1), "minecraft:quartz", "east")
    south = belt(run((15, 1, MZ + 1), (17, 1, MZ + 1)), "east", "south", 32, "alloy feed")
    north = belt(run((15, 1, MZ - 1), (17, 1, MZ - 1)), "east", "north", -32, "alloy feed")
    trunk = belt(run((15, 1, MZ), (21, 1, MZ)), "east")
    machine(up(trunk[0]), 'create:brass_belt_funnel[facing=east,shape=retracted]{Filter:{id:"criticalmass:steel_ingot",count:1},FilterAmount:2,UpTo:0b}')
    sync([north[2], trunk[2], south[2]])
    stop((18, 1, MZ - 1))
    stop((18, 1, MZ + 1))
    assert take(trunk[-1], "east") == ALLOY
    basin(ALLOY, "east", "mixer", KINDLED, "north", "alloy mixer")
    a1 = belt(run((23, 1, MZ), (32, 1, MZ)), "east", "south", 32, "sheet belt")
    press(a1[2], "south", "sheet press")
    assert take(a1[-1], "east") == SHEET_STORE
    store(SHEET_STORE, VAULT, ("Sheet", "Store"))
    k = belt(run((33, 1, MZ - 1), (39, 1, MZ - 1)), "east", "north", -32, "cladding belt")
    push(k[0], "south")
    press(k[3], "north", "cladding press")
    f = belt(run((33, 1, MZ + 1), (33, 1, MZ + 2)), "south", "west", 32, "frame sheets")
    machine(up(f[0]), "create:andesite_belt_funnel[facing=south,shape=retracted]")
    machine(SHEETS, "create:depot")
    machine(CLADDING, "create:depot")


def frame_shop():
    dock((26, 2, -5), "minecraft:dried_kelp", "east", 2)
    dock((26, 2, -4), "minecraft:iron_nugget", "east")
    trunk = belt(run((27, 1, -5), (33, 1, -5)), "east", "north", -32, "gasket feed")
    side = belt(run((27, 1, -4), (29, 1, -4)), "east", "south", 32, "gasket feed")
    sync([trunk[2], side[2]])
    stop((30, 1, -4))
    assert take(trunk[-1], "east") == GASKET
    basin(GASKET, "north", "press", None, "south", "gasket press", M + "sealing_gasket")
    t = belt(run((34, 1, -6), (39, 1, -6)), "east", "north", -32, "set belt")
    h = belt(run((36, 1, -5), (37, 1, -5)), "east")
    drive(h[-1], "south", 32, "set belt")
    machine(up(h[0]), 'create:brass_belt_funnel[facing=north,shape=pushing]{Filter:{id:"criticalmass:ceramic_fuel_pellet",count:1},FilterAmount:16,UpTo:0b}')
    sync([t[3], h[1]])
    assert pull(t[4], "south", M + "ceramic_fuel_pellet") == PELLET_SETS
    store(PELLET_SETS)
    machine(GASKETS, "create:depot")
    q = belt(run((39, 1, -5), (40, 1, -5)), "east")
    machine(up(q[0]), 'create:brass_belt_funnel[facing=east,shape=retracted]{Filter:{id:"criticalmass:ceramic_fuel_pellet",count:1},FilterAmount:16,UpTo:0b}')
    x, z = CRAFT_X, CRAFT_Z
    top, north, centre, south, bottom = (x, 4, z), (x, 3, z - 1), (x, 3, z), (x, 3, z + 1), (x, 2, z)
    craft = "create:mechanical_crafter[facing=west,pointing=%s]"
    machine(top, craft % "down")
    machine(centre, craft % "down")
    machine(bottom, craft % "down")
    machine(north, craft % "right")
    machine(south, craft % "left")
    machine((x, 3, z + 2), "create:cogwheel[axis=x]")
    motor((x + 1, 3, z + 2), "west", 64, "crafters")
    arm((35, 1, -9), [(SHEETS, "depot")], [(p, "crafter") for p in (top, north, south, bottom)], "south", "frame arms")
    arm((36, 1, -7), [(GASKETS, "depot")], [(centre, "crafter")], "east", "gasket arm")
    belt(run((x, 1, z), (RX - 1, 1, z)), "east", "north", -32, "frame belt")


def assembly():
    a = belt(run((RX, 1, RZ), (RX + 3, 1, RZ)), "east", "north", -64, "loop")
    b = belt(run((RX + 4, 1, RZ), (RX + 4, 1, RZ + 3)), "south", "east", -64, "loop")
    c = belt(run((RX + 4, 1, RZ + 4), (RX + 1, 1, RZ + 4)), "west", "south", -64, "loop")
    d = belt(run((RX, 1, RZ + 5), (RX, 1, RZ + 1)), "north")
    drive(d[-1], "west", -64, "loop")
    assert pull(d[-1], "east", M + "ceramic_fuel_pellet") == PELLETS
    store(PELLETS, "create:depot")
    deploy = 'create:deployer[facing=down,axis_along_first=%s]{Mode:"USE"}'
    first = up(a[1], 2)
    machine(first, deploy % "false")
    drive(first, "north", 64, "deployers")
    pellets = [up(b[1], 2), up(b[2], 2)]
    for pos in pellets:
        machine(pos, deploy % "true")
        drive(pos, "east", 64, "deployers")
    press(c[1], "south", "loop press")
    assert pull(c[2], "south", M + "fuel_assembly") == FUEL
    store(FUEL, VAULT, ("Fuel", "Store"))
    arm((RX - 1, 1, RZ), [(CLADDING, "depot")], [(first, "deployer")], "west", "cladding arm")
    arm((43, 1, -8), [(PELLETS, "depot")], [(p, "deployer") for p in pellets], "north", "pellet arm")


AX0, AX1, AZ0, AZ1 = -1, 25, 8, 28
A_EAVE, A_ROOF = 7, 8
ROW = 25
FARM = 29
COOLANT, BORIC, OIL = (5, 2, ROW), (14, 2, ROW), (22, 2, ROW)
TANKS = [(COOLANT, "primary_coolant", "Primary", "Coolant"), (BORIC, "boric_acid", "Boric", "Acid"), (OIL, "fuel_oil", "Fuel", "Oil")]
CONCRETE, CONCRETE_STORE = (20, 2, 15), (24, 2, 15)
STEEL_END = (14, 0, 9)
CANISTER_X, CANISTER_Z = 16, 11
CANISTERS = (18, 2, 11)
PLATES, CLAYS, COALS = (6, 1, 12), (7, 1, 12), (8, 1, 12)
RESIN_X, RESIN_Z = 10, 15
CARTRIDGES = (13, 2, 15)
HIGH_BINS = [ORE, (7, 6, ROW)]


def fluid(pos, heat, product, name):
    x, _, z = pos
    basin(pos, "down", "mixer", heat, "west", name + " mixer", speed=64)
    machine((x, 2, z - 1), "create:mechanical_pump[facing=south]")
    machine((x, 2, z - 2), WATER_TANK)
    machine((x, 3, z - 1), "create:cogwheel[axis=z]")
    motor((x, 3, z - 2), "south", 32, name + " water pump")
    machine((x, 2, z + 1), 'create:smart_fluid_pipe[face=floor,facing=south]{Filter:{id:"criticalmass:%s_bucket",count:1}}' % product)
    machine((x, 2, z + 2), "create:mechanical_pump[facing=south]")
    machine((x, 3, z + 2), "create:cogwheel[axis=z]")
    motor((x, 3, z + 1), "south", 32, name + " transfer pump")
    machine((x, 2, z + 3), "create:fluid_pipe")


def fluids():
    dock((0, 2, ROW), "minecraft:lapis_lazuli", "east")
    c = belt(run((1, 1, ROW), (4, 1, ROW)), "east", "north", -32, "lapis belt")
    assert take(c[-1], "east") == COOLANT
    fluid(COOLANT, None, "primary_coolant", "coolant")
    dock(HIGH_BINS[1], "create:scoria", "east", 16)
    belt(run((8, 5, ROW), (9, 5, ROW)), "east", "south", 32, "scoria feed")
    for x, speed in ((9, 64), (11, -64)):
        machine((x, 3, ROW), "create:crushing_wheel[axis=z]")
        drive((x, 3, ROW), "south", speed, "borax crusher")
    stop((11, 4, ROW))
    stop((11, 5, ROW))
    b = belt(run((10, 1, ROW), (13, 1, ROW)), "east", "north", -32, "borax belt")
    assert take(b[-1], "east") == BORIC
    fluid(BORIC, KINDLED, "boric_acid", "boric acid")
    dock((18, 2, ROW), "minecraft:coal", "east")
    f = belt(run((19, 1, ROW), (21, 1, ROW)), "east", "north", -32, "coal belt")
    assert take(f[-1], "east") == OIL
    fluid(OIL, KINDLED, "fuel_oil", "fuel oil")


def concrete():
    trunk = run(lip(DEPLETED, "south"), (20, 1, 14))
    within("enrichment", belt, trunk, "south", "west", 32, "depleted belt")
    dock((21, 2, 9), "minecraft:gravel", "south", 2)
    side = belt(run((21, 1, 10), (21, 1, 12)), "south", "east", -32, "gravel feed")
    sync([trunk[8], side[2]], TUNNEL_Z)
    assert trunk[8][2] == side[2][2]
    stop((21, 1, 13))
    assert take(trunk[-1], "south") == CONCRETE
    basin(CONCRETE, "east", "mixer", None, "north", "concrete mixer", speed=64)
    machine((19, 2, 15), "create:mechanical_pump[facing=east]")
    machine((18, 2, 15), WATER_TANK)
    machine((19, 3, 15), "create:cogwheel[axis=x]")
    motor((18, 3, 15), "east", 32, "concrete water pump")
    out = belt(run((21, 1, 15), (23, 1, 15)), "east", "south", 32, "concrete belt")
    assert take(out[-1], "east") == CONCRETE_STORE
    store(CONCRETE_STORE, VAULT, ("Shielding", "Concrete"))


def canisters():
    under = (STEEL_STORE[0], 1, STEEL_STORE[2])
    machines[under] = 'create:brass_funnel[facing=down,extracting=true]{Filter:{id:"criticalmass:steel_ingot",count:1},FilterAmount:1,UpTo:0b}'
    floor = run((under[0], 0, under[2]), STEEL_END)
    assert len(floor) <= 20
    within("melt", belt, floor, "south", "west", 32, "steel conveyor")
    head = (14, 2, 6)
    machine(head, "create:mechanical_press[facing=west]")
    within("mill", drive, head, "west", 64, "plate press")
    x, z = CANISTER_X, CANISTER_Z
    left, middle, right, bottom = (x, 3, z - 1), (x, 3, z), (x, 3, z + 1), (x, 2, z)
    craft = "create:mechanical_crafter[facing=west,pointing=%s]"
    machine(left, craft % "right")
    machine(middle, craft % "down" + "{Cover:1b}")
    machine(right, craft % "left")
    machine(bottom, craft % "down")
    machine((x, 3, z + 2), "create:cogwheel[axis=x]")
    motor((x + 1, 3, z + 2), "west", 64, "canister crafters")
    arm((15, 1, 10), [(STEEL_END, "belt")], [(p, "crafter") for p in (left, right, bottom)], "south", "canister arm", 12)
    out = belt(run((x, 1, z), (x + 1, 1, z)), "east", "north", -32, "canister belt")
    assert take(out[-1], "east") == CANISTERS
    store(CANISTERS, VAULT, ("Waste", "Canisters"))


def cartridges():
    clay = run(lip(CLAY, "south"), (7, 1, 11))
    within("mill", belt, clay, "south", "east", -32, "clay belt")
    machine(CLAYS, "create:depot")
    dock((2, 2, 12), "minecraft:iron_ingot", "east")
    i = belt(run((3, 1, 12), (5, 1, 12)), "east", "south", 32, "iron belt")
    press(i[1], "north", "iron press")
    machine(PLATES, "create:depot")
    machine(COALS, "create:depot")
    machine(up(COALS), "create:andesite_funnel[facing=down,extracting=true]")
    machine(up(COALS, 2), 'create:creative_crate{Filter:{id:"minecraft:coal",count:1}}')
    bins.append((up(COALS, 2), "Coal", "Bin"))
    x, z = RESIN_X, RESIN_Z
    top, north, centre, south, bottom = (x, 4, z), (x, 3, z - 1), (x, 3, z), (x, 3, z + 1), (x, 2, z)
    craft = "create:mechanical_crafter[facing=west,pointing=%s]"
    machine(top, craft % "down")
    machine(centre, craft % "down")
    machine(bottom, craft % "down")
    machine(north, craft % "right")
    machine(south, craft % "left")
    machine((x, 3, z + 2), "create:cogwheel[axis=x]")
    motor((x + 1, 3, z + 2), "west", 64, "cartridge crafters")
    arm((8, 1, 14), [(PLATES, "depot")], [(top, "crafter"), (bottom, "crafter")], "east", "cartridge arms")
    arm((9, 1, 13), [(COALS, "depot")], [(centre, "crafter")], "south", "cartridge arms")
    arm((9, 1, 15), [(CLAYS, "depot")], [(north, "crafter"), (south, "crafter")], "north", "cartridge arms")
    out = belt(run((x, 1, z), (x + 2, 1, z)), "east", "south", 32, "cartridge belt")
    assert take(out[-1], "east") == CARTRIDGES
    store(CARTRIDGES, VAULT, ("Resin", "Cartridges"))


HX0, HX1, HZ0, HZ1 = -1, 53, -16, 8
EAVE, ROOF, LIGHT = 9, 10, 13
LX0, LX1, LZ0, LZ1 = 2, 50, -9, 0
HANG = 9
DECK = 5
GRID, ARRESTER, GRID_BREAKER, MAIN_T = (38, 1, 18), (40, 1, 15), (38, 1, 15), (34, 1, 12)
INCOMERS = [((31, 3, 9), (31, 3, 7), 1), ((33, 3, 9), (33, 3, 7), 3), ((35, 3, 9), (35, 3, 7), 5)]
BUS6 = [(30 + i, 1, 7) for i in range(6)]
SLOTS = {"shops": 0, "fluids": 1, "mill": 2, "enrichment": 3, "melt": 4, "assembly": 5}
FEEDERS = {name: (30 + i, 1, 6) for name, i in SLOTS.items()}
RISERS = {name: (30 + i, HANG, 5) for name, i in SLOTS.items()}
BUSHINGS = {"shops": ((24, 5, 7), (24, 5, 9)), "fluids": ((22, 5, 7), (22, 5, 9))}
RISERS["shops"] = BUSHINGS["shops"][1]
RISERS["fluids"] = (23, A_EAVE, 22)
HANGS = {name: A_EAVE for name in WING}
TAP = 4
STAGGER = 60
HIGH, LOW, LINE, LOAD = 0, 1, 0, 1
STACK = [(9, -18), (10, -18), (9, -17), (10, -17)]
MONITOR = (52, 3, -4)
SIGNS = [((8, 1, -1), 8, "Mill", "Crushing and", "washing"), ((17, 1, -1), 8, "Enrichment", "Centrifuges"), ((24, 1, -1), 8, "Pelleting", "Ceramic Fuel", "Pellets"),
         ((8, 1, -6), 0, "Melt Shop", "Steel and", "Reactor Alloy"), ((24, 1, -7), 0, "Rolling", "Sheet and", "Fuel Cladding"), ((30, 1, -7), 0, "Frame Shop", "Gaskets and", "frames"),
         ((40, 1, -4), 0, "Assembly Hall", "Fuel", "Assemblies"), ((1, 1, -6), 12, "Receiving"), ((38, 1, 5), 8, "6 kV", "Switchgear"),
         ((32, 1, 13), 12, "Works", "Transformer", "220 / 6 kV"), ((36, 1, 14), 12, "Grid", "Breaker"), ((36, 1, 19), 12, "220 kV", "Grid"),
         ((4, 1, 18), 0, "Cartridge Shop", "Resin", "Cartridges"), ((15, 1, 18), 0, "Canister Shop", "Open Waste", "Canisters"), ((22, 1, 18), 0, "Concrete Plant", "Shielding", "Concrete"),
         ((3, 1, 22), 8, "Coolant", "Primary", "Coolant"), ((11, 1, 22), 8, "Boron", "Borax and", "Boric Acid"), ((20, 1, 22), 8, "Fuel Oil", "for the", "Diesel Engines")]
INSULATORS = []


def taken(pos):
    return pos in world or pos in machines or pos in motor_cells


def put(x, y, z, b):
    world[(x, y, z)] = b


def box(x0, y0, z0, x1, y1, z1, b):
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                world[(x, y, z)] = b


def carve(x0, y0, z0, x1, y1, z1):
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                world.pop((x, y, z), None)


def deco(x, y, z, b):
    if not taken((x, y, z)):
        put(x, y, z, b)


def attach(pos, block):
    assert not taken(pos), "something in the way at %s for %s" % (pos, block)
    attached.append((pos, block))
    motor_cells.add(pos)


def post(pos, rotation, *lines):
    text = ",".join("'{\"text\":\"%s\"}'" % line for line in (list(lines) + ["", "", "", ""])[:4])
    attach(pos, "minecraft:birch_sign[rotation=%d]{front_text:{messages:[%s]}}" % (rotation, text))


def wall(x, z, pillar, glazed, top=EAVE):
    for y in range(1, top + 1):
        block = LAMP if pillar and y == top - 2 else PLINTH if y == 1 or pillar else BAND if y == top - 1 else SHELL
        if not pillar and glazed and glazed[0] <= y <= glazed[1]:
            block = WINDOW
        put(x, y, z, block)


def shell():
    box(HX0, 0, HZ0, HX1, 0, HZ1, FLOOR)
    box(0, 0, -3, 52, 0, -1, CLEAN)
    box(0, 0, -4, 52, 0, -4, YELLOW)
    box(0, 0, 0, 52, 0, 0, YELLOW)
    for x in range(39, 48):
        for z in range(-12, -3):
            if x in (39, 47) or z in (-12, -4):
                put(x, 0, z, YELLOW if (x + z) % 2 == 0 else BLACK)
    for x in range(HX0, HX1 + 1):
        pillar = (x - HX0) % 6 == 0
        glazed = (3, 6) if (x - HX0) % 6 in (2, 3, 4) else None
        wall(x, HZ0, pillar, glazed)
        wall(x, HZ1, pillar, glazed)
    for z in range(HZ0, HZ1 + 1):
        pillar = (z - HZ0) % 6 == 0
        wall(HX0, z, pillar, (5, 6) if (z - HZ0) % 6 in (2, 3, 4) else None)
        wall(HX1, z, pillar, (2, 7))
    box(HX0, ROOF, HZ0, HX1, ROOF, HZ1, PLINTH)
    for x in range(HX0, HX1 + 1):
        put(x, ROOF, HZ0, SHELL)
        put(x, ROOF, HZ1, SHELL)
    for z in range(HZ0, HZ1 + 1):
        put(HX0, ROOF, z, SHELL)
        put(HX1, ROOF, z, SHELL)
    carve(LX0 + 1, ROOF, LZ0 + 1, LX1 - 1, ROOF, LZ1 - 1)
    for x in range(LX0, LX1 + 1):
        for z in (LZ0, LZ1):
            for y in (ROOF + 1, ROOF + 2):
                put(x, y, z, IRON if (x - LX0) % 6 == 0 else WINDOW)
    for z in range(LZ0, LZ1 + 1):
        for x in (LX0, LX1):
            for y in (ROOF + 1, ROOF + 2):
                put(x, y, z, SHELL)
    box(LX0, LIGHT, LZ0, LX1, LIGHT, LZ1, PLINTH)
    for x in range(LX0 + 3, LX1, 6):
        box(x, ROOF, LZ0 + 1, x, ROOF, LZ1 - 1, GIRDER_Z)
        for z in (-6, -3):
            put(x, LIGHT, z, LAMP)
    for x in range(HX0 + 3, HX1, 6):
        for z in (-13, 4):
            put(x, ROOF, z, LAMP)


def annex():
    box(AX0, 0, AZ0 + 1, AX1, 0, AZ1, FLOOR)
    box(0, 0, 19, AX1 - 1, 0, 21, CLEAN)
    box(0, 0, 18, AX1 - 1, 0, 18, YELLOW)
    box(0, 0, 22, AX1 - 1, 0, 22, YELLOW)
    for x in range(AX0, AX1 + 1):
        wall(x, AZ1, (x - AX0) % 6 == 0 or x == AX1, (3, 5) if (x - AX0) % 6 in (2, 3, 4) else None, A_EAVE)
    for z in range(AZ0 + 1, AZ1 + 1):
        pillar = (z - AZ0) % 6 == 0 or z == AZ1
        glazed = (3, 5) if (z - AZ0) % 6 in (2, 3, 4) else None
        wall(AX0, z, pillar, glazed, A_EAVE)
        wall(AX1, z, pillar, glazed, A_EAVE)
    box(AX0, A_ROOF, AZ0 + 1, AX1, A_ROOF, AZ1, PLINTH)
    for x in range(AX0, AX1 + 1):
        put(x, A_ROOF, AZ1, SHELL)
    for z in range(AZ0 + 1, AZ1 + 1):
        put(AX0, A_ROOF, z, SHELL)
        put(AX1, A_ROOF, z, SHELL)
    for x in range(AX0 + 3, AX1 - 2):
        for z in (19, 20, 21):
            put(x, A_ROOF, z, IRON if (x - AX0) % 6 == 0 else WINDOW)
    for x in range(AX0 + 3, AX1, 6):
        for z in (13, 25):
            put(x, A_ROOF, z, LAMP)
    carve(1, 1, AZ0, 2, 3, AZ0)
    for x, y in ((CLAY[0], 1), (CLAY[0], 2), (DEPLETED[0], 1), (DEPLETED[0], 2), (STEEL_END[0], 1)):
        carve(x, y, AZ0, x, y, AZ0)
    carve(AX0, 1, 19, AX0, 4, 21)
    box(AX0, 0, 19, AX0, 0, 21, PLINTH)
    for y in range(1, 6):
        for z in (18, 22):
            put(AX0, y, z, YELLOW if y % 2 else BLACK)
    for z in (19, 20, 21):
        put(AX0, 5, z, YELLOW if z % 2 else BLACK)
    box(AX0 - 2, 6, 18, AX0 - 1, 6, 22, IRON)
    for z in range(18, 23):
        put(AX0 - 2, 0, z, YELLOW if z % 2 else BLACK)
    post((-3, 1, 23), 4, "Chemistry", "Wing")


def farm():
    box(2, 0, FARM, AX1, 0, FARM + 4, GRAVEL)
    for x in range(2, AX1 + 1):
        put(x, 1, FARM + 4, PLINTH)
    for z in range(FARM, FARM + 5):
        put(2, 1, z, PLINTH)
        put(AX1, 1, z, PLINTH)
    for pos, _, *label in TANKS:
        box(pos[0] - 1, 1, FARM, pos[0] + 1, 6, FARM + 2, TANK)
        post((pos[0], 2, FARM + 4), 0, *label)
    for x in (2, 9, 18, AX1):
        box(x, 2, FARM + 4, x, 4, FARM + 4, BARS)
        put(x, 5, FARM + 4, LAMP)


def docks():
    ground = {(x, z) for x in range(-12, HX1 + 4) for z in range(HZ0 - 3, HZ1 + 4)} | {(x, z) for x in range(-12, AX1 + 4) for z in range(HZ1 + 4, FARM + 9)}
    for x, z in ground:
        if (x, 0, z) not in world:
            edge = any(n not in ground for n in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)))
            put(x, 0, z, PLINTH if x < HX0 or edge else "minecraft:stone_bricks")
    for z0, z1 in ((-14, -12), (-2, 0)):
        carve(HX0, 1, z0, HX0, 4, z1)
        box(HX0, 0, z0, HX0, 0, z1, PLINTH)
        for y in range(1, 6):
            for z in (z0 - 1, z1 + 1):
                put(HX0, y, z, YELLOW if y % 2 else BLACK)
        for z in range(z0, z1 + 1):
            put(HX0, 5, z, YELLOW if z % 2 else BLACK)
        box(HX0 - 2, 6, z0 - 1, HX0 - 1, 6, z1 + 1, IRON)
        for z in range(z0 - 1, z1 + 2):
            put(HX0 - 2, 0, z, YELLOW if z % 2 else BLACK)
    for x0, z0, block in ((-9, 3, M + "chartrine"), (-9, -10, "minecraft:raw_iron_block"), (-9, -5, "minecraft:coal_block")):
        box(x0, 1, z0, x0 + 5, 1, z0 + 3, block)
        box(x0 + 1, 2, z0 + 1, x0 + 4, 2, z0 + 2, block)
        box(x0 + 2, 3, z0 + 1, x0 + 3, 3, z0 + 1, block)
    for z in (-11, 1):
        post((-3, 1, z), 4, "Receiving", "Dock")


def gallery():
    for i in range(1, DECK + 1):
        for z in (-3, -2):
            deco(i, i, z, STAIR % "east")
    for x in range(DECK + 1, 52):
        for z in range(-4, 0):
            deco(x, DECK, z, GRATE)
        for z in (-4, -1):
            deco(x, DECK + 1, z, BARS)
    for z in range(-4, 0):
        deco(52, DECK + 1, z, BARS)
        deco(52, DECK, z, GRATE)
    for z in (-4, -1):
        deco(DECK, DECK + 1, z, BARS)
    for x in range(8, 52, 7):
        for z in (-4, -1):
            if not any(taken((x, y, z)) for y in range(1, DECK)):
                box(x, 1, z, x, DECK - 1, z, STEEL_BLOCK)


def stands():
    for pos in [d[0] for d in drives] + HIGH_BINS + [up(b, -1) for b in HIGH_BINS]:
        for y in range(pos[1] - 1, 0, -1):
            if taken((pos[0], y, pos[2])):
                break
            put(pos[0], y, pos[2], STEEL_BLOCK)


def stack():
    for x, z in STACK:
        box(x, 1, z, x, 18, z, IRON)
        put(x, 19, z, PLINTH)
        attach((x, 20, z), "minecraft:campfire[lit=true,signal_fire=true,facing=north]")
    box(9, 6, -17, 10, 7, -17, IRON)


def yard():
    box(26, 0, HZ1 + 1, 42, 0, 20, GRAVEL)
    for x in range(26, 43):
        put(x, 1, 20, BARS)
    for z in range(HZ1 + 1, 21):
        put(26, 1, z, BARS)
        if z != 14:
            put(42, 1, z, BARS)
    for x in (30, 41):
        box(x, 1, 18, x, 7, 18, STEEL_BLOCK)
        attach((x, 9, 18), ROD)
    box(30, 8, 18, 41, 8, 18, GIRDER_X)
    machine(GRID, "criticalmass:grid_connection")
    machine(GRID_BREAKER, "criticalmass:circuit_breaker[facing=north,closed=false]")
    machine(ARRESTER, "criticalmass:surge_arrester")
    machine(MAIN_T, "criticalmass:transformer[facing=north]{ScrollValue:1,Tap:%d}" % TAP)
    for pos in BUS6:
        machine(pos, "criticalmass:bus_bar[axis=x]")
    for pos in FEEDERS.values():
        machine(pos, "criticalmass:circuit_breaker[facing=north,closed=false]")
    wires.extend([("line", GRID, 0, GRID_BREAKER, LINE), ("line", GRID_BREAKER, LOAD, MAIN_T, HIGH), ("line", ARRESTER, 0, GRID_BREAKER, LOAD)])
    for outside, inside, bar in INCOMERS:
        put(outside[0], outside[1], HZ1, IRON)
        INSULATORS.extend([(outside, "south"), (inside, "north")])
        wires.extend([("cable", MAIN_T, LOW, outside, 0), ("cable", outside, 0, inside, 0), ("cable", inside, 0, BUS6[bar], 0)])
    for name, i in SLOTS.items():
        wires.append(("cable", BUS6[i], 0, FEEDERS[name], LINE))
        post((30 + i, 1, 5), 8, name.capitalize(), "Feeder")
        if name in BUSHINGS:
            inside, outside = BUSHINGS[name]
            put(inside[0], inside[1], HZ1, IRON)
            INSULATORS.extend([(inside, "north"), (outside, "south")])
            wires.extend([("cable", FEEDERS[name], LOAD, inside, 0), ("cable", inside, 0, outside, 0)])
            if RISERS[name] != outside:
                wires.append(("cable", outside, 0, RISERS[name], 0))
                INSULATORS.append((RISERS[name], "down"))
        else:
            wires.append(("cable", FEEDERS[name], LOAD, RISERS[name], 0))
            INSULATORS.append((RISERS[name], "down"))


def feed(name):
    motors = sorted([d for d in drives if d[4] == name], key=lambda d: (d[0][0], d[0][2]))
    hangers = []
    high = HANGS.get(name, HANG)
    for i in range(0, len(motors), 2):
        pair = motors[i:i + 2]
        pos = (round(sum(d[0][0] for d in pair) / len(pair)), high, round(sum(d[0][2] for d in pair) / len(pair)))
        while pos in spare or taken(pos):
            pos = (pos[0] + 1, high, pos[2])
        spare[pos] = 4 - len(pair)
        hangers.append(pos)
        INSULATORS.append((pos, "down"))
        for d in pair:
            wires.append(("cable" if d[5] else "wire", pos, 0, d[0], 0))
    riser = RISERS[name]
    spare[riser] = 3 if name in WING else 2
    reach = {riser: 0}
    for pos in sorted(hangers, key=lambda p: math.dist(p, riser)):
        near = min((p for p in reach if spare[p] > 0), key=lambda p: math.dist(p, pos) + (reach[p] if name in WING else 0))
        wires.append(("cable", near, 0, pos, 0))
        spare[near] -= 1
        spare[pos] -= 1
        reach[pos] = reach[near] + math.dist(near, pos)


def hang():
    for pos, facing in INSULATORS:
        back = {"down": (0, 1, 0), "north": (0, 0, 1), "south": (0, 0, -1)}[facing]
        support = (pos[0] + back[0], pos[1] + back[1], pos[2] + back[2])
        if support not in world or world[support] in (GIRDER_Z, WINDOW):
            put(support[0], support[1], support[2], IRON)
            if LZ0 < support[2] < LZ1 and facing == "down":
                for z in range(LZ0 + 1, LZ1):
                    deco(support[0], ROOF, z, GIRDER_Z)
        attach(pos, "criticalmass:insulator[facing=%s]" % facing)
    for kind, a, _, b, _ in wires:
        assert math.dist(a, b) <= 23.5, "the %s from %s to %s is too long: %.1f" % (kind, a, b, math.dist(a, b))
    count = {}
    for _, a, ta, b, tb in wires:
        for end in ((a, ta), (b, tb)):
            count[end] = count.get(end, 0) + 1
            assert count[end] <= 4, "too many wires on %s" % (end,)


def signs():
    for pos, rotation, *lines in SIGNS:
        post(pos, rotation, *lines)
    for pos, *lines in bins:
        post(up(pos), 0 if pos[2] <= -4 or AZ0 < pos[2] < 18 else 8, *lines)


motor_cells = set()
spare = {}


def design():
    for name, parts in (("mill", [mill]), ("enrichment", [enrichment]), ("melt", [melt_shop]), ("assembly", [frame_shop, assembly]), ("fluids", [fluids]), ("shops", [concrete, canisters, cartridges])):
        bay[0] = name
        for part in parts:
            part()
    for pos in stops:
        machine(pos, "minecraft:glass")
    motor_cells.update(d[0] for d in drives)
    shell()
    annex()
    farm()
    docks()
    yard()
    stands()
    signs()
    gallery()
    attach(MONITOR, "criticalmass:area_radiation_monitor[facing=west,alarm=false]{ScrollValue:10}")
    stack()
    for name in BAYS:
        feed(name)
    hang()
    put(45, 1, HZ1, "minecraft:air")
    put(45, 2, HZ1, "minecraft:air")
    for half, y in (("lower", 1), ("upper", 2)):
        attached.append(((45, y, HZ1), "create:andesite_door[facing=south,half=%s,hinge=left,open=false,visible=true]" % half))


def relative(x, y, z):
    return "~%d ~%d ~%d" % (x, y - 1, z)


def absolute(origin):
    def w(x, y, z):
        return "%d %d %d" % (origin[0] + x, origin[1] + y, origin[2] + z)
    return w


def bounds():
    cells = list(world) + list(machines) + [d[0] for d in drives] + [a[0] for a in attached]
    return tuple(min(c[i] for c in cells) for i in range(3)), tuple(max(c[i] for c in cells) for i in range(3))


def fills(w):
    rows = {}
    for (x, y, z), block in world.items():
        rows.setdefault((y, z), []).append((x, block))
    cmds = []
    for (y, z), cells in sorted(rows.items()):
        cells.sort()
        start = 0
        for i in range(1, len(cells) + 1):
            if i == len(cells) or cells[i][1] != cells[start][1] or cells[i][0] != cells[i - 1][0] + 1:
                a, b, block = cells[start][0], cells[i - 1][0], cells[start][1]
                cmds.append("setblock %s %s" % (w(a, y, z), block) if a == b else "fill %s %s %s" % (w(a, y, z), w(b, y, z), block))
                start = i
    return cmds


def place(w, creative=False):
    low, high = bounds()
    cmds = ["fill %s %s minecraft:air" % (w(low[0], y, low[2]), w(high[0], min(y + 7, high[1] + 1), high[2])) for y in range(1, high[1] + 2, 8)]
    cmds += ["kill @e[type=item,distance=..120]"] + fills(w)
    cmds += ["setblock %s %s" % (w(*pos), block) for pos, block in machines.items()]
    kind = "create:creative_motor" if creative else "criticalmass:electric_motor"
    cmds += ["setblock %s %s[facing=%s]{ScrollValue:%d}" % (w(*d[0]), kind, d[1], d[2]) for d in drives]
    cmds += ["setblock %s %s" % (w(*pos), block) for pos, block in attached]
    cmds += ["data merge block %s %s" % (w(*pos), nbt) for pos, nbt in late]
    return cmds


def start(w):
    cmds = ["criticalmass wire %s %d %s %d %s" % (w(*a), ta, w(*b), tb, kind) for kind, a, ta, b, tb in wires]
    return cmds + ["criticalmass breaker %s close" % w(*GRID_BREAKER)]


def energise(w, name):
    return "criticalmass breaker %s close" % w(*FEEDERS[name])


SPOTS = [("coolant tank", (COOLANT[0] - 1, 1, FARM), "TankContent"), ("boric acid tank", (BORIC[0] - 1, 1, FARM), "TankContent"), ("fuel oil tank", (OIL[0] - 1, 1, FARM), "TankContent"),
         ("concrete", CONCRETE_STORE, "Inventory"), ("canisters", CANISTERS, "Inventory"), ("cartridges", CARTRIDGES, "Inventory"), ("clay store", CLAY, "Inventory"),
         ("plates", PLATES, "HeldItem.Item"), ("clays", CLAYS, "HeldItem.Item"), ("coals", COALS, "HeldItem.Item"), ("steel end", STEEL_END, "Inventory"),
         ("coolant basin", COOLANT, "InputItems.Items"), ("boric basin", BORIC, "InputItems.Items"), ("oil basin", OIL, "InputItems.Items"), ("concrete basin", CONCRETE, "InputItems.Items"),
         ("fuel", FUEL, "Inventory"), ("depleted", DEPLETED, "Inventory"), ("steel store", STEEL_STORE, "Inventory"), ("sheet store", SHEET_STORE, "Inventory"), ("sheets", SHEETS, "HeldItem.Item"),
         ("gaskets", GASKETS, "HeldItem.Item"), ("cladding", CLADDING, "HeldItem.Item"), ("pellets", PELLETS, "HeldItem.Item"), ("pellet store", PELLET_STORE, "Inventory"), ("pellet sets", PELLET_SETS, "Inventory"), ("loop", (RX, 1, RZ), "Inventory"), ("pellet deployer", (RX + 4, 3, RZ + 1), "HeldItem"),
         ("concentrate basin", CONCENTRATE, "InputItems.Items"), ("pellet basin", PELLET, "InputItems.Items"), ("steel basin", STEEL, "InputItems.Items"), ("alloy basin", ALLOY, "InputItems.Items"),
         ("gasket basin", GASKET, "InputItems.Items")]
VIEWS = {
    "aerial_se": ((74, 30, 36), (26, 4, -4), True),
    "aerial_nw": ((-24, 26, -42), (24, 4, -4), True),
    "aerial_sw": ((-28, 22, 32), (20, 4, -2), True),
    "docks": ((-18, 5, -4), (0, 3, -5), True),
    "substation": ((48, 8, 26), (35, 1, 13), True),
    "hall_west": ((7, 6, -2), (40, 4, -3), False),
    "hall_east": ((50, 6, -3), (10, 4, -2), False),
    "mill": ((12, 7, 6), (6, 2, 0), True),
    "enrichment": ((21, 6, 6), (17, 1, 1), True),
    "pelleting": ((29, 5, 6), (24, 2, 2), True),
    "melt": ((3, 7, -5), (12, 1, -11), True),
    "rolling": ((21, 7, -5), (28, 1, -11), True),
    "frames": ((30, 5, -12), (37, 2, -8), True),
    "loop": ((50, 8, -14), (43, 1, -8), True),
    "loop_floor": ((50, 1, -5), (43, 2, -8), False),
    "switchgear": ((38, 1, 3), (33, 2, 7), False),
    "wing_aerial": ((-22, 24, 52), (12, 3, 20), True),
    "tanks": ((13, 7, 46), (13, 3, 30), True),
    "wing": ((0, 1, 20), (24, 3, 19), False),
    "fluids": ((12, 6, 20), (12, 2, 26), True),
    "cartridges": ((4, 6, 17), (9, 2, 14), True),
    "canisters": ((12, 5, 14), (16, 2, 11), True),
    "concrete": ((16, 6, 18), (20, 2, 13), True),
    "crossing": ((18, 2, -2), (14, 0, -2), False),
}
NIGHT = ("aerial_se", "hall_west", "loop", "wing_aerial", "wing")


def look(origin, name):
    pos, target, _ = VIEWS[name]
    return "/tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f" % (origin[0] + pos[0] + 0.5, origin[1] + pos[1], origin[2] + pos[2] + 0.5,
                                                         origin[0] + target[0] + 0.5, origin[1] + target[1] + 0.5 - 1.62, origin[2] + target[2] + 0.5)


def shots(origin, names, tag):
    out = []
    for name in names:
        out += [{"cmds": [look(origin, name)], "wait": 30, "gui": False}, {"shot": tag + "_" + name, "wait": 2}]
    return out


def scene(minutes, names, creative=False, night=False, probe=False, origin=(300, -61, 300), pause=1200):
    w = absolute(origin)
    low, high = bounds()
    checks = [("data get block %s %s" % (w(*pos), path)).strip() for _, pos, path in SPOTS]
    checks += ["data get block %s Demand" % w(*d[0]) for d in drives if d[3] in ("crusher", "centrifuges", "loop press")]
    checks += ["execute if block %s create:metal_girder[axis=x,x=true,z=false]" % w(35, 8, 18), "execute if block %s create:metal_girder[axis=z,x=false,z=true]" % w(LX0 + 3, ROOF, -4)]
    if probe:
        checks += ["data get block %s" % w(*pos) for pos in (COOLANT, BORIC, OIL, CONCRETE, (CANISTER_X, 3, CANISTER_Z), (RESIN_X, 3, RESIN_Z), (20, 2, 12), (21, 2, 12), FEEDERS["fluids"], FEEDERS["shops"], MAIN_T)]
        checks += ["data get block %s Inventory" % w(*pos) for pos in ((20, 1, 12), (21, 1, 12), (10, 1, ROW), (16, 1, CANISTER_Z), (RESIN_X, 1, RESIN_Z))]
        checks += ["data get block %s Demand" % w(*d[0]) for d in drives if d[4] in WING]
    perches = ["/setblock %s minecraft:barrier" % w(pos[0], pos[1] - 1, pos[2]) for pos, _, high_up in VIEWS.values() if high_up]
    steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/weather clear", "/gamemode creative", "/clear @p", "/tp @p %d %d %d" % (origin[0] + 25, origin[1] + 1, origin[2] + 30),
                       "/forceload add %d %d %d %d" % (origin[0] + low[0] - 2, origin[2] + low[2] - 2, origin[0] + high[0] + 2, origin[2] + high[2] + 2)], "wait": 80, "gui": False, "camera": "first", "slot": 1},
             {"cmds": ["/" + c for c in place(w, creative)] + perches, "wait": 60},
             {"cmds": ["/" + c for c in start(w)], "wait": 40}]
    steps += [{"cmds": ["/" + energise(w, name)], "wait": STAGGER} for name in BAYS]
    for _ in range(minutes):
        steps += [{"wait": pause}, {"cmds": ["/" + c for c in checks], "closeScreen": True, "wait": 5}]
    steps += [{"cmds": ["/execute as @e[type=item,distance=..120,limit=8] run data get entity @s Pos", "/execute as @e[type=item,distance=..120,limit=8] run data get entity @s Item.id"], "wait": 5}]
    steps += shots(origin, names, "run")
    if night:
        steps += [{"cmds": ["/time set 18000"], "wait": 60}] + shots(origin, [n for n in names if n in NIGHT], "night")
    return steps


def write(root):
    folder = os.path.join(root, "data", NS, "function", "factory")
    os.makedirs(folder, exist_ok=True)
    with open(os.path.join(root, "pack.mcmeta"), "w", newline="\n") as f:
        json.dump({"pack": {"pack_format": 48, "description": "Builds a running Critical Mass fuel works"}}, f, indent=1)
    low, high = bounds()
    marker = "@e[type=minecraft:marker,tag=%s,limit=1]" % TAG
    span = "~%d ~%d ~%d ~%d" % (low[0] - 2, low[2] - 2, high[0] + 2, high[2] + 2)
    files = {
        "build": ["kill @e[type=minecraft:marker,tag=%s]" % TAG, "summon minecraft:marker ~%d ~ ~%d {Tags:[\"%s\"]}" % (-low[0] + 4, -low[2] + 4, TAG),
                  "execute at %s run forceload add %s" % (marker, span), "schedule function %s:factory/place 20t replace" % NS,
                  "tellraw @s {\"text\":\"Building the fuel works. Its line starts in five seconds.\",\"color\":\"yellow\"}"],
        "place_here": place(relative) + ["schedule function %s:factory/start 60t replace" % NS],
        "start_here": start(relative) + ["schedule function %s:factory/feed_%s %dt replace" % (NS, BAYS[0], STAGGER)],
    }
    for i, name in enumerate(BAYS):
        last = ["forceload remove %s" % span, "tellraw @a {\"text\":\"Fuel works built and on grid power. The first Fuel Assemblies reach the Fuel Store in about five minutes.\",\"color\":\"green\"}"]
        files["feed_%s_here" % name] = [energise(relative, name)] + (["schedule function %s:factory/feed_%s %dt replace" % (NS, BAYS[i + 1], STAGGER)] if i + 1 < len(BAYS) else last)
    for name in [n[:-5] for n in list(files) if n.endswith("_here")]:
        files[name] = ["execute as %s at @s run function %s:factory/%s_here" % (marker, NS, name)]
    for name, cmds in files.items():
        with open(os.path.join(folder, name + ".mcfunction"), "w", newline="\n") as f:
            f.write("\n".join(cmds) + "\n")


design()

if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    args = sys.argv[1:]
    if args and args[0] == "scene":
        minutes = int(args[1]) if len(args) > 1 else 3
        names = [n for n in args[2].split(",") if n] if len(args) > 2 and args[2] != "all" else list(VIEWS)
        json.dump({"name": "factory", "steps": scene(minutes, names, "creative" in args, "night" in args, "probe" in args, pause=100 if "quick" in args else 1200)}, open(os.path.join(here, "factory.json"), "w", newline="\n"), indent=1)
    else:
        for target in args:
            write(target)
            print("wrote", target)
    print(len(world), "blocks,", len(machines), "machines,", len(drives), "motors,", len(wires), "wires,", len(INSULATORS), "insulators,", len(place(relative)), "commands")
