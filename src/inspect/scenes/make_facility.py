import json
import math
import os
import sys

NS = "criticalmass_facility"
TAG = "cm_facility_origin"
PROP = "cm_facility_prop"
TEMP = "cm_facility_bind"

AIR = "minecraft:air"
SC = "criticalmass:shielding_concrete"
SG = "criticalmass:shielded_glass"
STEEL = "criticalmass:steel_block"
ALLOY = "criticalmass:reactor_alloy_block"
PIPE = "criticalmass:reinforced_pipe"
SHELL = "minecraft:light_gray_concrete"
PLINTH = "minecraft:gray_concrete"
BAND = "minecraft:cyan_terracotta"
YARD = "minecraft:gray_concrete"
FLOOR = "minecraft:smooth_stone"
CLEAN = "minecraft:light_gray_concrete"
WHITE = "minecraft:white_concrete"
YELLOW = "minecraft:yellow_concrete"
BLACK = "minecraft:black_concrete"
LAMP = "minecraft:sea_lantern"
BARS = "minecraft:iron_bars"
IRON = "create:industrial_iron_block"
WINDOW = "create:industrial_iron_window"
GRATE = "minecraft:iron_trapdoor[half=top,open=false,facing=north]"
STAIR = "minecraft:polished_andesite_stairs[facing=%s,half=bottom]"
GIRDER_X = "create:metal_girder[x=true,z=false]"
GIRDER_Z = "create:metal_girder[x=false,z=true]"
CHAIN = "minecraft:chain[axis=y]"
LANTERN = "minecraft:lantern[hanging=true]"
WATER = "minecraft:water"

AREA = ((-21, -36), (53, 34))
TOP = 30
OFFSET = (25, 40)

CONTROLLER = (-2, 3, 0)
SERVICE = (0, 5, 0)
OUTLET = (0, 3, 2)
INLET = (0, 3, -2)
CORE = ["frf", "cfc", "frf"]
KIND = {"f": "fuel_channel", "r": "control_channel", "c": "coolant_channel"}
VIEWPORTS = [(2, y, z) for z in (-1, 1) for y in (2, 3, 4)] + [(0, 2, 2), (0, 4, 2)]
DRIVES = [((0, 6, -1), "south"), ((0, 6, 1), "north")]
DRIVE_COGS = [(1, 6, -1), (1, 6, 0), (1, 6, 1)]
DRIVE_MOTOR = (1, 7, 0)
PRESSURIZER = (3, 3, -2)
ECCS_PORT, ECCS_PUMP, ECCS_TANK = (-2, 2, -1), (-3, 2, -1), (-4, 2, -1)
ECCS_COG, ECCS_CLUTCH, ECCS_MOTOR, ECCS_LEVER = (-3, 3, -1), (-4, 3, -1), (-5, 3, -1), (-4, 4, -1)

PRIMARY_PUMP = (2, 3, 4)
EXCHANGERS = [(6, 3, 0), (6, 4, 0), (6, 5, 0)]
PRIMARY_VALVE = (6, 4, -2)
PRIMARY = ([(0, 3, 3), (0, 3, 4), (1, 3, 4)] + [(x, 3, 4) for x in range(3, 7)] + [(6, 3, 3), (6, 3, 2), (6, 3, 1), (6, 5, -1), (6, 5, -2), (6, 3, -2), (6, 3, -3), (6, 3, -4)]
           + [(x, 3, -4) for x in range(5, -1, -1)] + [(0, 3, -3)])

TURBINE = (22, 8, 0)
RELIEF = (15, 8, 0)
STEAM_VALVE = (17, 8, 0)
CONDENSERS = [(24, 3, 0), (25, 3, 0)]
FEED_TANK = [(28, y, 0) for y in (1, 2, 3)]
FEED_PUMP = (26, 1, 2)
STEAM = ([(6, y, 0) for y in range(6, 9)] + [(x, 8, 0) for x in range(7, 15)] + [(16, 8, 0), (18, 8, 0), (19, 8, 0), (20, 8, 0)]
         + [(22, 6, 0), (22, 5, 0), (22, 4, 0), (22, 3, 0), (23, 3, 0)] + [(26, 3, 0), (27, 3, 0)]
         + [(28, 1, 1), (28, 1, 2), (27, 1, 2)] + [(x, 1, 2) for x in range(25, 6, -1)] + [(7, 1, 1), (7, 1, 0), (6, 1, 0), (6, 2, 0)])

TOWER = (24, -22)
FILL = [(TOWER[0], y, TOWER[1]) for y in range(2, 6)]
TOWER_MOTOR = (TOWER[0], 1, TOWER[1])
TOWER_FAN = (TOWER[0], 6, TOWER[1])
COOLING_PUMP = (26, 2, -5)
MAKEUP_PIPE, MAKEUP_PUMP, MAKEUP_TANK = (23, 3, TOWER[1]), (22, 3, TOWER[1]), (21, 3, TOWER[1])
MAKEUP_COG, MAKEUP_MOTOR = (22, 4, TOWER[1]), (21, 4, TOWER[1])
COOLING = ([(25, 2, 0), (25, 2, -1)] + [(24, 2, z) for z in range(-1, TOWER[1] + 1, -1)] + [(24, 3, TOWER[1] + 2), (24, 4, TOWER[1] + 2), (24, 5, TOWER[1] + 2), (24, 5, TOWER[1] + 1)]
           + [(25, 2, TOWER[1]), (26, 2, TOWER[1])] + [(26, 2, z) for z in range(TOWER[1] + 1, -5)]
           + [(26, 2, -4), (26, 3, -4), (26, 4, -4), (25, 4, -4), (24, 4, -4), (24, 4, -3), (24, 4, -2), (24, 4, -1), (24, 4, 0)])
PIPES = PRIMARY + STEAM + COOLING

GENERATORS = [(28, 8, 0)]
GEN_BREAKER = (33, 7, -3)
BUS20 = [(36, 7, -3), (37, 7, -3), (38, 7, -3)]
IN20, OUT20 = (44, 7, -3), (46, 7, -3)
MAIN_T = (47, 1, -3)
GRID_BREAKER = (49, 1, -3)
GRID = (51, 1, -3)
UNIT_T = (48, 1, -6)
OUT6, IN6 = (46, 3, -6), (44, 3, -6)
STATION_BREAKER = (43, 1, -5)
BUS6 = [(39, 1, -5), (40, 1, -5), (41, 1, -5)]
STANDBY_ENGINE, STANDBY_GEN, STANDBY_T, TRANSFER = (38, 1, 7), (39, 1, 7), (41, 1, 7), (43, 1, 7)
START_LINE = [(x, 1, 8) for x in range(38, 44)]
BATTERY = (37, 1, -5)
C_NORMAL, C_LOAD, C1, C2, C3, C4 = (44, 5, 1), (42, 5, 3), (37, 5, -4), (30, 5, -3), (21, 5, 4), (26, 5, 1)
W_IN, P_IN, P_HIGH = (14, 4, 5), (9, 4, 5), (9, 9, -4)
T_IN, T_OUT, T_FLOOR = (23, 4, -8), (23, 4, -10), (23, 1, -20)
PRIMARY_MOTOR = (PRIMARY_PUMP[0], 2, PRIMARY_PUMP[2])
FEED_MOTOR = (FEED_PUMP[0], 2, FEED_PUMP[2])
COOLING_MOTOR = (COOLING_PUMP[0], 1, COOLING_PUMP[2])
INSULATORS = [(IN20, "west"), (OUT20, "east"), (OUT6, "east"), (IN6, "west"), (C_NORMAL, "down"), (C_LOAD, "down"), (C1, "down"), (C2, "down"), (C3, "down"), (C4, "down"),
              (W_IN, "east"), (P_IN, "west"), (P_HIGH, "west"), (T_IN, "south"), (T_OUT, "north"), (T_FLOOR, "up")]
BUSHINGS = [(45, 7, -3), (45, 3, -6), (13, 4, 5), (23, 4, -9)]
HIGH, LOW, LINE, LOAD = 0, 1, 0, 1
ON_NORMAL, ON_STANDBY, TO_LOAD = 0, 1, 2
WIRES = [
    ("cable", GENERATORS[-1], 0, GEN_BREAKER, LINE), ("cable", GEN_BREAKER, LOAD, BUS20[0], 0), ("cable", BUS20[2], 0, IN20, 0), ("cable", IN20, 0, OUT20, 0),
    ("cable", OUT20, 0, MAIN_T, LOW), ("line", MAIN_T, HIGH, GRID_BREAKER, LOAD), ("line", GRID_BREAKER, LINE, GRID, 0),
    ("cable", OUT20, 0, UNIT_T, HIGH), ("cable", UNIT_T, LOW, OUT6, 0), ("cable", OUT6, 0, IN6, 0), ("cable", IN6, 0, STATION_BREAKER, LINE), ("cable", STATION_BREAKER, LOAD, C_NORMAL, 0),
    ("cable", C_NORMAL, 0, TRANSFER, ON_NORMAL), ("cable", STANDBY_GEN, 0, STANDBY_T, HIGH), ("cable", STANDBY_T, LOW, TRANSFER, ON_STANDBY),
    ("cable", TRANSFER, TO_LOAD, C_LOAD, 0), ("cable", C_LOAD, 0, BUS6[2], 0), ("cable", BATTERY, 0, BUS6[0], 0),
    ("wire", BUS6[1], 0, C1, 0), ("wire", C1, 0, C2, 0), ("wire", C2, 0, COOLING_MOTOR, 0), ("wire", C2, 0, C4, 0), ("wire", C4, 0, FEED_MOTOR, 0), ("wire", C2, 0, C3, 0),
    ("wire", C3, 0, W_IN, 0), ("wire", W_IN, 0, P_IN, 0), ("wire", P_IN, 0, PRIMARY_MOTOR, 0), ("wire", P_IN, 0, P_HIGH, 0), ("wire", P_HIGH, 0, DRIVE_MOTOR, 0),
    ("wire", C1, 0, T_IN, 0), ("wire", T_IN, 0, T_OUT, 0), ("wire", T_OUT, 0, T_FLOOR, 0), ("wire", T_FLOOR, 0, TOWER_MOTOR, 0), ("wire", T_FLOOR, 0, MAKEUP_MOTOR, 0),
]
RUN_LOAD = 0.1
SYNC_AFTER = 700

SCREENS = ((23, 9), (31, 13), 19)
SIDE_SCREENS = [((19, 10), (21, 12), GENERATORS[0]), ((33, 10), (35, 12), GRID)]
SIDE_COG = (22, 11, 19)
SCREEN_COG = (32, 11, 19)
SCREEN_MOTOR = (32, 11, 20)
DESK_Y, DESK_Z = 7, 15
DESKS = {
    22: [("sync", "gauge", GEN_BREAKER, 0, 4, "SYNC ANGLE"), ("hertz", "gauge", GENERATORS[0], 1, 4, "FREQUENCY")],
    23: [("gen_open", "button", GEN_BREAKER, 1, 1, "GEN OPEN"), ("gen_close", "button", GEN_BREAKER, 0, 2, "GEN CLOSE")],
    24: [("turbine_reset", "button", TURBINE, 1, 7, "TURBINE RESET"), ("trip", "button", TURBINE, 0, 1, "TURBINE TRIP")],
    25: [("speed", "gauge", TURBINE, 6, 4, "TURBINE SPEED"), ("shaft", "gauge", TURBINE, 5, 4, "TURBINE POWER")],
    26: [("pressure", "gauge", CONTROLLER, 2, 4, "PRESSURE"), ("coolant", "gauge", CONTROLLER, 1, 4, "COOLANT")],
    27: [("power", "gauge", CONTROLLER, 5, 4, "HEAT OUTPUT"), ("alarm", "alarm", CONTROLLER, 0, 0, "ALARM")],
    28: [("rod_out", "button", CONTROLLER, 3, 2, "RODS OUT"), ("rod_in", "button", CONTROLLER, 2, 4, "RODS IN")],
    29: [("reset", "button", CONTROLLER, 1, 7, "RESET"), ("scram", "button", CONTROLLER, 0, 0, "SCRAM")],
    30: [("primary_flow", "gauge", PRIMARY_PUMP, 0, 4, "PRIMARY FLOW"), ("feed_flow", "gauge", FEED_PUMP, 0, 4, "FEED FLOW")],
    31: [("lower", "button", GENERATORS[0], 1, 4, "LOWER"), ("raise", "button", GENERATORS[0], 0, 2, "RAISE")],
    32: [("megawatts", "gauge", GENERATORS[0], 0, 4, "GENERATOR MW"), ("grid", "gauge", GRID, 0, 4, "GRID MW")],
}
RACKS = [(x, -1, z) for x in (-8, -6) for z in (21, 23)]
RUN_POWER = 0.7
RUN_COOLANT = 125

blocks = {}
late = []
machines = {}
props = []
fluid = set()


def put(x, y, z, b):
    blocks[(x, y, z)] = b


def box(x0, y0, z0, x1, y1, z1, b):
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                blocks[(x, y, z)] = b


def carve(x0, y0, z0, x1, y1, z1):
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                blocks.pop((x, y, z), None)


def walls(x0, y0, z0, x1, y1, z1, b):
    box(x0, y0, z0, x1, y1, z0, b)
    box(x0, y0, z1, x1, y1, z1, b)
    box(x0, y0, z0, x0, y1, z1, b)
    box(x1, y0, z0, x1, y1, z1, b)


def machine(pos, b, wet=False):
    machines[pos] = b
    blocks.pop(pos, None)
    if wet:
        fluid.add(pos)


def attach(pos, b):
    late.append((pos, b))
    blocks.pop(pos, None)


def sign(pos, facing, *lines):
    text = ",".join("'{\"text\":\"%s\"}'" % line for line in (list(lines) + ["", "", "", ""])[:4])
    attach(pos, "minecraft:birch_wall_sign[facing=%s]{front_text:{messages:[%s]}}" % (facing, text))


def post(pos, rotation, *lines):
    text = ",".join("'{\"text\":\"%s\"}'" % line for line in (list(lines) + ["", "", "", ""])[:4])
    attach(pos, "minecraft:birch_sign[rotation=%d]{front_text:{messages:[%s]}}" % (rotation, text))


def door(x, y, z, facing, hinge="left"):
    for half, dy in (("lower", 0), ("upper", 1)):
        attach((x, y + dy, z), "create:andesite_door[facing=%s,half=%s,hinge=%s,open=false,visible=true]" % (facing, half, hinge))


def taken(pos):
    return pos in blocks or pos in machines or pos in PIPES or any(pos == at for at, _ in late)


def pendant(x, top, z, bottom):
    cells = [(x, y, z) for y in range(top, bottom - 1, -1)]
    if any(taken(c) for c in cells) or not taken((x, top + 1, z)):
        return
    for c in cells[:-1]:
        attach(c, CHAIN)
    attach(cells[-1], LANTERN)


def wall_lamp(x, y, z):
    if (x, y, z) in blocks:
        put(x, y, z, LAMP)


def stairs(x, y, z, dx, dz, steps, facing):
    for i in range(steps):
        put(x + dx * i, y + i, z + dz * i, STAIR % facing)


def yard():
    (x0, z0), (x1, z1) = AREA
    box(x0, 0, z0, x1, 0, z1, YARD)
    fx0, fz0, fx1, fz1 = x0 + 1, z0 + 1, x1 - 1, z1 - 1
    walls(fx0, 1, fz0, fx1, 2, fz1, BARS)
    for x in range(fx0, fx1 + 1):
        for z in (fz0, fz1):
            if (x - fx0) % 8 == 0 or x == fx1:
                box(x, 1, z, x, 3, z, STEEL)
    for z in range(fz0, fz1 + 1):
        for x in (fx0, fx1):
            if (z - fz0) % 8 == 0 or z == fz1:
                box(x, 1, z, x, 3, z, STEEL)
    for x, z in ((fx0, fz0), (fx1, fz0), (fx0, fz1), (fx1, fz1)):
        put(x, 4, z, LAMP)
    carve(34, 1, fz1, 37, 2, fz1)
    for x in (33, 38):
        box(x, 1, fz1, x, 4, fz1, STEEL)
        put(x, 5, fz1, LAMP)
    box(34, 0, 22, 37, 0, fz1 + 1, "minecraft:smooth_stone")
    for z in range(23, fz1 + 1, 2):
        put(34, 0, z, YELLOW)
        put(37, 0, z, YELLOW)


def corridor():
    box(-12, 0, 12, 12, 0, 16, FLOOR)
    walls(-12, 1, 12, 13, 4, 16, SHELL)
    walls(-12, 1, 12, 13, 1, 16, PLINTH)
    box(-12, 5, 12, 13, 5, 16, PLINTH)
    for x in range(-11, 13):
        put(x, 0, 14, YELLOW if x % 2 == 0 else BLACK)
    for x in range(-10, 13, 3):
        put(x, 5, 14, LAMP)


def containment():
    for x in range(-13, 14):
        for z in range(-13, 14):
            d = math.hypot(x, z)
            if d < 10.5:
                put(x, 0, z, YELLOW if 6.5 <= d < 7.5 else CLEAN)
                if d >= 9.5:
                    put(x, 12, z, STEEL)
            elif d < 12.5:
                for y in range(0, 16):
                    put(x, y, z, SC if d < 11.5 else (PLINTH if y < 2 else SHELL))
            for y in range(16, 26):
                t = y - 15
                if (d / 12.5) ** 2 + (t / 9.0) ** 2 <= 1 and (d / 10.5) ** 2 + (t / 7.5) ** 2 > 1:
                    put(x, y, z, SC if (d / 11.5) ** 2 + (t / 8.25) ** 2 <= 1 else SHELL)
    for deg in range(10, 360, 20):
        x, z = round(10.9 * math.cos(math.radians(deg))), round(10.9 * math.sin(math.radians(deg)))
        if math.hypot(x, z) >= 10.5:
            for y in (3, 8, 14):
                put(x, y, z, LAMP)
    for deg in range(0, 360, 30):
        x, z = round(10.1 * math.cos(math.radians(deg))), round(10.1 * math.sin(math.radians(deg)))
        wall_lamp(x, 18, z)
        x, z = round(6.4 * math.cos(math.radians(deg))), round(6.4 * math.sin(math.radians(deg)))
        wall_lamp(x, 21, z)
    for x, z in ((-4, -4), (4, -4), (-4, 4), (4, 4), (-8, 0), (8, -6), (0, -8), (-6, 6), (5, 8), (-6, -6), (9, 3)):
        put(x, 0, z, LAMP)
    for x in range(-9, 10):
        for z in (-1, 1):
            put(x, 13, z, GIRDER_X)
    box(-1, 13, -1, 1, 13, 1, IRON)
    put(0, 13, 0, LAMP)
    for y in (12, 11, 10):
        attach((0, y, 0), CHAIN)
    attach((0, 9, 0), "minecraft:grindstone[face=ceiling,facing=north]")
    ring = []
    for deg in range(0, 360, 30):
        x, z = round(9.7 * math.cos(math.radians(deg))), round(9.7 * math.sin(math.radians(deg)))
        if blocks.get((x, 12, z)) == STEEL and (x, z) not in ring:
            ring.append((x, z))
            attach((x, 11, z), LANTERN)
    for x in (-6, -3, 3, 6):
        for z in (-1, 1):
            attach((x, 12, z), LANTERN)
    for x in range(-4, 5):
        for z in range(-4, 5):
            edge = max(abs(x), abs(z))
            if edge >= 3:
                put(x, 5, z, GRATE)
            if edge == 4 and (x, z) != (-4, 3):
                put(x, 6, z, BARS)
    stairs(-9, 1, 3, 1, 0, 5, "east")
    for i in range(5):
        put(-9 + i, 2 + i, 2, BARS)
        put(-9 + i, 2 + i, 4, BARS)
    for x, z in ((6, 4), (6, -4)):
        box(x, 1, z, x, 2, z, STEEL)
    put(2, 1, 4, STEEL)
    put(3, 1, -2, STEEL)
    box(-4, 1, -1, -3, 1, -1, STEEL)
    box(-5, 1, -1, -5, 2, -1, STEEL)
    carve(0, 1, 11, 0, 2, 12)
    door(0, 1, 12, "north")
    sign((1, 2, 13), "south", "Containment", "Airlock")
    sign((-6, 2, -1), "west", "Emergency", "Injection")


def vessel():
    for x in range(-2, 3):
        for y in range(1, 6):
            for z in range(-2, 3):
                if max(abs(x), abs(z)) == 2 or y in (1, 5):
                    machine((x, y, z), "criticalmass:reactor_vessel_casing")
    machine(CONTROLLER, "criticalmass:reactor_controller[facing=west]")
    machine(SERVICE, "criticalmass:reactor_service_port[facing=up]")
    machine(OUTLET, "criticalmass:primary_coolant_port[facing=south,mode=outlet]", True)
    machine(INLET, "criticalmass:primary_coolant_port[facing=north,mode=inlet]", True)
    machine(ECCS_PORT, "criticalmass:primary_coolant_port[facing=west,mode=inlet]", True)
    for k, row in enumerate(CORE):
        for i, ch in enumerate(row):
            for y in (2, 3, 4):
                machine((i - 1, y, k - 1), "criticalmass:" + KIND[ch])
    for p in VIEWPORTS:
        machine(p, "criticalmass:reactor_viewport")
    machine(PRESSURIZER, "criticalmass:pressurizer")
    machine((PRESSURIZER[0], PRESSURIZER[1] - 1, PRESSURIZER[2]), "create:blaze_burner[blaze=kindled]{isCreative:1b}")
    machine(ECCS_PUMP, "create:mechanical_pump[facing=east]", True)
    machine(ECCS_TANK, "create:fluid_tank", True)
    machine(ECCS_COG, "create:cogwheel[axis=x]")
    machine(ECCS_CLUTCH, "create:clutch[axis=x]")
    machine(ECCS_MOTOR, "create:creative_motor[facing=east]")
    attach(ECCS_LEVER, "minecraft:lever[face=floor,facing=north,powered=true]")


def primary():
    machine(PRIMARY_PUMP, "criticalmass:circulation_pump[facing=east,axis_along_first=true]", True)
    for pos in EXCHANGERS:
        machine(pos, "criticalmass:heat_exchanger[facing=north]", True)
    machine(PRIMARY_VALVE, "criticalmass:reinforced_valve[facing=north,axis_along_first=true]", True)
    machine((5, 4, -2), "create:creative_motor[facing=east]")
    post((8, 1, 0), 12, "Steam", "Generator")


def hall():
    x0, x1, z0, z1 = 13, 45, -9, 9
    box(x0, 0, z0, x1, 0, z1, FLOOR)
    walls(x0, 1, z0, x1, 17, z1, SHELL)
    walls(x0, 1, z0, x1, 2, z1, PLINTH)
    walls(x0, 16, z0, x1, 16, z1, BAND)
    box(x0, 17, z0, x1, 17, z1, PLINTH)
    for x in range(x0, x1 + 1, 8):
        for z in (z0, z1):
            box(x, 1, z, x, 17, z, PLINTH)
    for x in range(x0 + 1, x1, 8):
        box(x + 2, 12, z0, x + 5, 14, z0, WINDOW)
    for z in (-6, -5, -4, 4, 5, 6):
        box(x1, 12, z, x1, 14, z, WINDOW)
    for x in range(15, x1, 4):
        put(x, 9, z0, LAMP)
        put(x, 11, z1, LAMP)
        put(x, 3, z0, LAMP)
        put(x, 3, z1, LAMP)
    for z in (-6, -2, 2, 6):
        put(x1, 9, z, LAMP)
        put(x1, 4, z, LAMP)
    for z in (-6, 6):
        put(x0, 3, z, LAMP)
        put(x0, 9, z, LAMP)
    box(14, 6, -8, 44, 6, 8, FLOOR)
    for x in range(20, 25):
        for z in range(-2, 3):
            if x in (20, 24) or abs(z) == 2:
                put(x, 6, z, YELLOW if (x + z) % 2 == 0 else BLACK)
    for x in range(26, 33):
        for z in (-2, 2):
            put(x, 6, z, YELLOW if (x + z) % 2 == 0 else BLACK)
    carve(21, 6, -8, 28, 6, -5)
    box(20, 7, -4, 29, 7, -4, BARS)
    box(20, 7, -8, 20, 7, -5, BARS)
    box(29, 7, -8, 29, 7, -5, BARS)
    carve(39, 6, -8, 44, 6, -8)
    stairs(43, 1, -8, -1, 0, 6, "west")
    box(39, 7, -7, 44, 7, -7, BARS)
    for x in range(16, 44, 6):
        box(x, 16, -8, x, 16, 8, GIRDER_Z)
    box(14, 12, -8, 44, 12, -8, STEEL)
    box(14, 12, 8, 44, 12, 8, STEEL)
    for x in (36, 37):
        box(x, 13, -8, x, 13, 8, GIRDER_Z)
    box(36, 13, -3, 37, 13, -1, IRON)
    for y in (12, 11):
        attach((36, y, -2), CHAIN)
    attach((36, 10, -2), "minecraft:grindstone[face=ceiling,facing=east]")
    box(11, 1, -3, 13, 10, 3, SC)
    box(x0, 18, z0, x1, 18, z1, PLINTH)
    carve(x0 + 1, 18, z0 + 1, x1 - 1, 18, z1 - 1)
    for x in (18, 26, 34):
        box(x, 18, -5, x + 2, 18, -3, IRON)
        put(x + 1, 19, -4, "minecraft:iron_trapdoor[half=bottom,open=false,facing=north]")
    for x, z in ((34, 4), (38, 4)):
        for dx in (0, 1):
            for dz in (0, 1):
                for y in (1, 2, 3):
                    machine((x + dx, y, z + dz), "create:fluid_tank")
    post((36, 1, 3), 8, "Demineralised", "Water")
    for x in range(39, 44):
        for z in range(2, 7):
            if x in (39, 43) or z in (2, 6):
                put(x, 6, z, YELLOW if (x + z) % 2 == 0 else BLACK)
    put(41, 7, 4, "minecraft:barrel[facing=up]")
    put(42, 7, 4, "minecraft:barrel[facing=up]")
    put(42, 8, 4, "create:andesite_casing")
    put(41, 7, 5, "create:andesite_casing")
    put(40, 7, 3, "create:andesite_casing")
    carve(45, 1, 3, 45, 2, 3)
    door(45, 1, 3, "west")
    sign((44, 3, 3), "west", "Turbine Hall", "Basement")
    carve(33, 1, 9, 33, 2, 9)
    door(33, 1, 9, "north")
    carve(38, 7, 9, 38, 8, 9)
    door(38, 7, 9, "north")
    box(21, 8, 9, 33, 10, 9, SG)
    sign((14, 3, 4), "east", "Feedwater", "To Reactor")
    for x in range(15, 44, 4):
        for z in (-6, -2, 2, 6):
            pendant(x, 16, z, 10)
            if (x, 6, z) in blocks and not taken((x, 5, z)):
                attach((x, 5, z), LANTERN)
    put(45, 4, 1, LAMP)
    put(45, 4, 5, LAMP)
    post((24, 1, 1), 0, "Condenser")
    post((29, 1, 1), 0, "Feedwater", "Tank")
    post((27, 1, -5), 12, "Circulating", "Water Pump")
    post((27, 1, 3), 0, "Feedwater", "Pump")


def turbine():
    machine(RELIEF, "criticalmass:relief_valve[facing=up]", True)
    machine(STEAM_VALVE, "criticalmass:reinforced_valve[facing=north,axis_along_first=false]", True)
    machine((17, 9, 0), "create:creative_motor[facing=down]{ScrollValue:-16}")
    for pos in CONDENSERS:
        machine(pos, "criticalmass:condenser[facing=east]", True)
    for p in FEED_TANK:
        machine(p, "create:fluid_tank", True)
    machine(FEED_PUMP, "criticalmass:circulation_pump[facing=west,axis_along_first=true]", True)
    machine(COOLING_PUMP, "criticalmass:circulation_pump[facing=south,axis_along_first=false]", True)
    put(24, 7, 0, STEEL)
    machine((24, 8, 0), "create:shaft[axis=x]")
    put(25, 7, 0, STEEL)
    machine((25, 8, 0), "create:stressometer[facing=south,axis_along_first=true]")
    put(26, 7, 0, STEEL)
    machine((26, 8, 0), "create:speedometer[facing=south,axis_along_first=true]")
    put(27, 7, 0, STEEL)
    machine((27, 8, 0), "create:shaft[axis=x]")
    for pos in GENERATORS:
        put(pos[0], 7, pos[2], IRON)
        machine(pos, "criticalmass:generator[axis=x]")
    sign((28, 7, 1), "south", "Generator", "20 kV 50 Hz")
    sign((17, 7, 1), "south", "Main Steam", "Stop Valve")
    put(17, 7, 0, STEEL)
    put(20, 7, 0, STEEL)


def tower():
    cx, cz = TOWER
    for x in range(cx - 13, cx + 14):
        for z in range(cz - 13, cz + 14):
            d = math.hypot(x - cx, z - cz)
            pad = (abs(x - cx) <= 4 and abs(z - cz) <= 2) or (abs(x - cx - 1) <= 3 and z > cz)
            if d < 11.5:
                if d >= 9.5 or pad:
                    put(x, 0, z, FLOOR)
                else:
                    put(x, 0, z, WATER)
                    put(x, -1, z, SC)
            for y in range(1, 30):
                r = 7.0 * math.sqrt(1 + ((y - 21) / 17.9) ** 2)
                if r - 0.5 <= d < r + 0.5:
                    if y <= 3 and (math.degrees(math.atan2(z - cz, x - cx)) + 9) % 18 >= 6:
                        continue
                    put(x, y, z, PLINTH if y in (4, 29) else SHELL)
    for x in range(cx - 8, cx + 9):
        if x != cx:
            put(x, 5, cz, STEEL)
    for z in range(cz - 8, cz):
        put(cx, 5, z, STEEL)
    for p in FILL:
        machine(p, "criticalmass:cooling_tower_packing", True)
    machine(TOWER_FAN, "create:encased_fan[facing=up]")
    machine(MAKEUP_PIPE, "create:fluid_pipe", True)
    machine(MAKEUP_PUMP, "create:mechanical_pump[facing=east]", True)
    machine(MAKEUP_TANK, "create:creative_fluid_tank", True)
    machine(MAKEUP_COG, "create:cogwheel[axis=x]")
    machine(MAKEUP_MOTOR, "criticalmass:electric_motor[facing=east]{ScrollValue:16}")
    box(21, 1, cz, 22, 2, cz, STEEL)
    for z in (-12, -16):
        put(24, 1, z, STEEL)
        put(26, 1, z, STEEL)
    post((21, 1, cz + 2), 0, "Make-up", "Water")


def aux():
    x0, x1, z0, z1 = 13, 41, 9, 21
    box(x0, 0, z0 + 1, x1, 0, z1, FLOOR)
    walls(x0, 1, z0, x1, 14, z1, SHELL)
    walls(x0, 1, z0, x1, 2, z1, PLINTH)
    walls(x0, 13, z0, x1, 13, z1, BAND)
    box(x0, 14, z0, x1, 14, z1, PLINTH)
    box(x0 + 1, 6, z0 + 1, x1 - 1, 6, z1 - 1, FLOOR)
    box(21, 1, 10, 21, 5, 20, WHITE)
    box(29, 1, 10, 29, 5, 20, WHITE)
    carve(21, 1, 14, 21, 2, 14)
    door(21, 1, 14, "east")
    carve(29, 1, 14, 29, 2, 14)
    door(29, 1, 14, "east")
    carve(13, 1, 14, 13, 2, 14)
    door(13, 1, 14, "east")
    carve(35, 1, 21, 36, 2, 21)
    door(35, 1, 21, "north", "left")
    door(36, 1, 21, "north", "right")
    sign((34, 3, 22), "south", "Critical Mass", "Generating", "Station")
    box(x0, 15, z0 + 1, x1, 15, z1, PLINTH)
    carve(x0 + 1, 15, z0 + 1, x1 - 1, 15, z1 - 1)
    for x in (22, 30):
        box(x, 15, 14, x + 2, 15, 16, IRON)
    for x in (34, 37):
        put(x, 4, 21, LAMP)
    for x in range(14, 21):
        put(x, 0, 14, YELLOW if x % 2 == 0 else BLACK)
    box(22, 1, 20, 28, 1, 20, "minecraft:smooth_stone_slab[type=bottom]")
    box(22, 1, 10, 28, 1, 10, "minecraft:smooth_stone_slab[type=bottom]")
    for i, x in enumerate((23, 25, 27)):
        props.append(((x + 0.5, 1.5, 20.5), "minecraft:armor_stand", "{Tags:[\"%s\"],Rotation:[180f,0f],ArmorItems:[{id:\"criticalmass:protective_boots\",count:1},{id:\"criticalmass:protective_leggings\",count:1},{id:\"criticalmass:protective_suit\",count:1},{id:\"criticalmass:protective_hood\",count:1}]}" % PROP))
    sign((24, 3, 10), "south", "Change Room", "Suit up before", "the airlock")
    sign((20, 3, 12), "west", "Controlled", "Area")
    box(30, 1, 10, 34, 1, 10, "minecraft:polished_andesite")
    for x in (31, 33):
        put(x, 2, 10, "minecraft:potted_fern")
    for z in (10, 11, 12):
        put(40, 1, z, "create:blue_seat")
    for x, z in ((30, 20), (39, 20)):
        put(x, 1, z, "minecraft:polished_andesite")
        put(x, 2, z, "minecraft:flowering_azalea_leaves[persistent=true]")
    for x in (35, 36):
        for z in (19, 20):
            attach((x, 1, z), "minecraft:gray_carpet")
    sign((30, 3, 14), "east", "Security", "Authorised", "personnel only")
    for z in (16, 17, 18, 19):
        for half in ("lower", "upper"):
            attach((22, 1 if half == "lower" else 2, z), "minecraft:iron_door[facing=east,half=%s,hinge=left,open=false]" % half)
            attach((28, 1 if half == "lower" else 2, z), "minecraft:iron_door[facing=west,half=%s,hinge=left,open=false]" % half)
    box(15, 1, 19, 18, 1, 19, "minecraft:polished_andesite")
    put(16, 2, 19, "minecraft:potted_fern")
    sign((17, 3, 20), "north", "Health", "Physics")
    box(33, 4, 22, 38, 4, 23, "minecraft:smooth_stone_slab[type=bottom]")
    for x in (33, 38):
        box(x, 1, 23, x, 3, 23, STEEL)
    box(31, 2, 21, 33, 4, 21, WINDOW)
    box(38, 2, 21, 39, 4, 21, WINDOW)
    box(14, 9, 21, 17, 11, 21, WINDOW)
    box(37, 9, 21, 40, 11, 21, WINDOW)
    box(41, 9, 14, 41, 11, 15, WINDOW)
    box(41, 2, 14, 41, 4, 15, WINDOW)
    carve(40, 6, 15, 40, 6, 19)
    stairs(40, 1, 19, 0, -1, 6, "north")
    box(39, 7, 15, 39, 7, 20, BARS)
    put(40, 7, 20, BARS)
    box(36, 7, 10, 36, 13, 20, WHITE)
    carve(36, 7, 12, 36, 8, 12)
    door(36, 7, 12, "east")
    box(18, 7, 10, 18, 13, 20, WHITE)
    carve(18, 7, 12, 18, 8, 12)
    door(18, 7, 12, "east")
    box(19, 6, 10, 35, 6, 19, "minecraft:gray_concrete")
    box(19, 7, 20, 35, 13, 20, SC)
    for x in (20, 23, 26, 29, 32, 35):
        for z in (11, 14, 17):
            put(x, 14, z, LAMP)
    for x in (15, 16):
        for z in (12, 16):
            put(x, 14, z, LAMP)
    for z in (11, 14, 17):
        put(38, 14, z, LAMP)
    for z in (12, 17):
        put(41, 10, z, LAMP)
        put(13, 10, z, LAMP)
        put(41, 3, z, LAMP)
    for z in (15, 18):
        put(18, 10, z, LAMP)
        put(36, 10, z, LAMP)
    for x in (14, 15, 16):
        for z in (19, 20):
            box(x, 7, z, x, 9, z, IRON)
    for x in (14, 15, 16):
        attach((x, 8, 18), "minecraft:lever[face=wall,facing=north,powered=%s]" % ("true" if x == 15 else "false"))
    sign((17, 9, 16), "west", "Relay Room")
    sign((37, 9, 12), "east", "Control Room")
    sign((38, 9, 10), "south", "Turbine Deck")
    for x in range(15, 40, 4):
        for z in (11, 15, 19):
            if not taken((x, 5, z)) and (x, 6, z) in blocks:
                attach((x, 5, z), LANTERN)
    for x in (22, 27, 32):
        pendant(x, 13, 12, 11)


def control_room():
    (sx0, sy0), (sx1, sy1), sz = SCREENS
    box(sx0 - 1, 7, sz, sx1 + 1, sy1, sz, STEEL)
    box(sx0, 7, sz, sx1, sy0 - 1, sz, ALLOY)
    for x in range(sx0, sx1 + 1):
        for y in range(sy0, sy1 + 1):
            machine((x, y, sz), "criticalmass:plant_screen[facing=north]")
    machine(SCREEN_COG, "create:cogwheel[axis=z]")
    machine(SIDE_COG, "create:cogwheel[axis=z]")
    for (x0, y0), (x1, y1), _ in SIDE_SCREENS:
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                machine((x, y, sz), "criticalmass:plant_screen[facing=north]")
    for x in sorted(DESKS):
        machine((x, DESK_Y, DESK_Z), "criticalmass:reactor_alloy_dashboard[facing=north,join_cw=%s,join_ccw=%s]" % (str(x + 1 in DESKS).lower(), str(x - 1 in DESKS).lower()))
    for x in (25, 27, 29):
        put(x, 7, 13, "create:gray_seat")
    for x in range(22, 33):
        put(x, 6, 14, CLEAN)
        put(x, 6, 13, CLEAN)
    box(20, 7, 18, 21, 7, 19, IRON)
    box(33, 7, 18, 34, 7, 19, IRON)
    put(20, 8, 19, "minecraft:lectern[facing=north]")


def fuel_building():
    x0, x1, z0, z1 = -12, 11, 16, 29
    box(x0, 0, z0, x1, 0, z1, CLEAN)
    walls(x0, 1, z0, x1, 8, z1, SHELL)
    walls(x0, 1, z0, x1, 2, z1, PLINTH)
    walls(x0, 8, z0, x1, 8, z1, BAND)
    box(x0, 9, z0, x1, 9, z1, PLINTH)
    walls(x0 + 1, 1, z0 + 1, x1 - 1, 8, z1 - 1, SC)
    carve(-2, 1, 16, -2, 2, 17)
    door(-2, 1, 16, "north")
    for x in (-8, -3, 2, 7):
        for z in (20, 25):
            put(x, 9, z, LAMP)
    box(x0, 10, z0, x1, 10, z1, PLINTH)
    carve(x0 + 1, 10, z0 + 1, x1 - 1, 10, z1 - 1)
    box(-10, -2, 19, -4, -2, 26, SC)
    walls(-10, -1, 19, -4, 0, 26, SC)
    box(-9, -1, 20, -5, 0, 25, WATER)
    for p in RACKS:
        machine(p, "criticalmass:spent_fuel_rack")
    walls(-10, 1, 19, -4, 1, 26, BARS)
    carve(-4, 1, 22, -4, 1, 23)
    for x in range(-11, -2):
        for z in (18, 27):
            put(x, 0, z, YELLOW if x % 2 == 0 else BLACK)
    for z in range(18, 28):
        put(-3, 0, z, YELLOW if z % 2 == 0 else BLACK)
    box(1, 0, 19, 5, 0, 22, "minecraft:gray_concrete")
    for x in (1, 3, 5):
        machine((x, 1, 20), "criticalmass:shielded_cask")
    sign((3, 2, 18), "south", "Dry Cask", "Storage")
    for y in (1, 2, 3):
        machine((8, y, 25), "criticalmass:centrifuge")
    machine((8, 4, 25), "create:creative_motor[facing=down]{ScrollValue:128}")
    machine((7, 1, 25), "criticalmass:open_waste_canister")
    put(9, 1, 25, STEEL)
    put(9, 1, 26, STEEL)
    put(8, 1, 26, STEEL)
    sign((7, 2, 27), "north", "Fuel", "Processing")
    for x in (2, 4):
        machine((x, -1, 26), "criticalmass:sealed_waste_canister")
        machine((x, 0, 26), "criticalmass:shielded_hatch[half=top,open=false,facing=north]")
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, 0, 26 + dz) in blocks and not blocks[(x + dx, 0, 26 + dz)].startswith("criticalmass:shielded_hatch"):
                put(x + dx, 0, 26 + dz, YELLOW)
        put(x, -2, 26, SC)
    sign((3, 2, 27), "north", "Waste Vault")
    sign((-7, 2, 18), "south", "Spent Fuel", "Pool")
    for x in range(-9, 9):
        put(x, 7, 22, GIRDER_X)
    box(-1, 7, 21, 0, 7, 23, IRON)
    for y in (6, 5):
        attach((0, y, 22), CHAIN)
    attach((0, 4, 22), "minecraft:grindstone[face=ceiling,facing=north]")
    for x in (-7, 3, 8):
        put(x, 3, 17, LAMP)
    for x in (-7, -1, 5):
        put(x, 3, 28, LAMP)
    for z in (20, 25):
        put(10, 3, z, LAMP)
    for x in (-8, -4, 0, 4, 8):
        for z in (19, 25):
            pendant(x, 8, z, 5)
    box(-12, 3, 21, -12, 5, 24, WINDOW)
    box(-11, 3, 21, -11, 5, 24, SG)


def switchyard():
    box(46, 0, -8, 51, 0, 1, "minecraft:gravel")
    box(46, 1, -8, 51, 1, -8, BARS)
    box(46, 1, 1, 51, 1, 1, BARS)
    carve(48, 1, 1, 48, 1, 1)
    for z in (-5, -1):
        box(51, 1, z, 51, 5, z, STEEL)
        put(51, 6, z, "minecraft:lightning_rod[facing=up]")
    box(51, 5, -4, 51, 5, -2, GIRDER_Z)
    machine(MAIN_T, "criticalmass:transformer[facing=north]{ScrollValue:0}")
    machine(GRID_BREAKER, "criticalmass:circuit_breaker[facing=north,closed=false]")
    machine(GRID, "criticalmass:grid_connection")
    machine(UNIT_T, "criticalmass:transformer[facing=north]{ScrollValue:2}")
    post((47, 1, -1), 8, "Main", "Transformer", "220 / 20 kV")
    post((49, 1, -1), 8, "Grid", "Breaker")
    post((50, 1, -5), 0, "220 kV", "Grid")
    post((48, 1, -7), 0, "Unit", "Transformer", "20 / 6 kV")


def electrics():
    for pos in BUSHINGS:
        put(pos[0], pos[1], pos[2], IRON)
    machine(GEN_BREAKER, "criticalmass:circuit_breaker[facing=south,closed=false]")
    for pos in BUS20:
        machine(pos, "criticalmass:bus_bar[axis=x]")
    post((33, 7, -2), 0, "Generator", "Breaker")
    post((37, 7, -2), 0, "20 kV", "Unit Bus")
    machine(STATION_BREAKER, "criticalmass:circuit_breaker[facing=west,closed=false]")
    for pos in BUS6:
        machine(pos, "criticalmass:bus_bar[axis=x]")
    post((40, 1, -4), 0, "6 kV", "Station Bus")
    post((43, 1, -4), 0, "Station", "Supply", "Breaker")
    for pos in [STANDBY_ENGINE, STANDBY_GEN, STANDBY_T, TRANSFER, BATTERY] + START_LINE:
        assert not taken(pos), "standby set in the way at %s" % (pos,)
    machine(STANDBY_ENGINE, "criticalmass:diesel_engine[facing=east]{Tank:{Fluid:{id:\"criticalmass:fuel_oil\",amount:4000}}}")
    machine(STANDBY_GEN, "criticalmass:generator[axis=x]")
    machine(STANDBY_T, "criticalmass:transformer[facing=south]{ScrollValue:2}")
    machine(TRANSFER, "criticalmass:transfer_switch[facing=north]")
    machine(BATTERY, "criticalmass:station_battery[facing=south]{Charge:3072000.0d}")
    for pos in START_LINE:
        attach(pos, "minecraft:redstone_wire")
    sign((38, 3, 8), "north", "Standby", "Diesel", "Engine")
    sign((41, 3, 8), "north", "Standby", "Transformer", "20 / 6 kV")
    sign((43, 3, 8), "north", "Transfer", "Switch")
    post((36, 1, -5), 0, "Station", "Battery")
    for pos, facing in INSULATORS:
        assert pos not in PIPES and pos not in machines, "insulator in the way at %s" % (pos,)
        attach(pos, "criticalmass:insulator[facing=%s]" % facing)
    sign((14, 5, 5), "east", "Containment", "Penetration")


def stack():
    for y in range(1, 30):
        put(15, y, 24, "minecraft:red_concrete" if (y // 4) % 2 == 0 else WHITE)
    put(15, 30, 24, LAMP)
    box(14, 1, 23, 16, 1, 25, PLINTH)
    put(15, 1, 24, "minecraft:red_concrete")


def validate():
    wet = set(PIPES) | fluid
    assert len(set(PIPES)) == len(PIPES), "a pipe is listed twice"
    for p in PIPES:
        near = [q for q in ((p[0] + 1, p[1], p[2]), (p[0] - 1, p[1], p[2]), (p[0], p[1] + 1, p[2]), (p[0], p[1] - 1, p[2]), (p[0], p[1], p[2] + 1), (p[0], p[1], p[2] - 1)) if q in wet]
        assert len(near) == 2, "pipe %s touches %s" % (p, near)
    for p in PIPES:
        assert p not in machines, "pipe inside a machine at %s" % (p,)


def turbine_ports():
    x, y, z = TURBINE
    for q in ((x - 1, y, z), (x, y - 1, z), (x, y + 1, z)):
        fluid.add(q)


def design():
    yard()
    corridor()
    aux()
    fuel_building()
    hall()
    containment()
    tower()
    stack()
    switchyard()
    control_room()
    vessel()
    primary()
    turbine()
    electrics()
    turbine_ports()
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            for dz in (-1, 0, 1):
                blocks.pop((TURBINE[0] + dx, TURBINE[1] + dy, TURBINE[2] + dz), None)
    for p in PIPES:
        blocks.pop(p, None)
    validate()


def cuboids(cells):
    cells = set(cells)
    out = []
    for p in sorted(cells, key=lambda q: (q[1], q[2], q[0])):
        if p not in cells:
            continue
        x, y, z = p
        x1 = x
        while (x1 + 1, y, z) in cells and x1 - x < 31:
            x1 += 1
        z1 = z
        while z1 - z < 31 and all((i, y, z1 + 1) in cells for i in range(x, x1 + 1)):
            z1 += 1
        y1 = y
        while y1 - y < 31 and all((i, y1 + 1, k) in cells for i in range(x, x1 + 1) for k in range(z, z1 + 1)):
            y1 += 1
        for i in range(x, x1 + 1):
            for j in range(y, y1 + 1):
                for k in range(z, z1 + 1):
                    cells.discard((i, j, k))
        out.append((x, y, z, x1, y1, z1))
    return out


def relative(x, y, z):
    return "~%d ~%d ~%d" % (x, y - 1, z)


def absolute(origin):
    def w(x, y, z):
        return "%d %d %d" % (origin[0] + x, origin[1] + y - 1, origin[2] + z)
    return w


def merge(w, pos, key, liquid, amount, heat):
    return "data merge block %s {%s:{Fluid:{id:\"%s\",amount:%d},Heat:%.1fd}}" % (w(*pos), key, liquid, amount, heat)


def tank(w, pos, liquid, amount, heat=0.0):
    held = "Fluid:{id:\"%s\",amount:%d}" % (liquid, amount)
    return "data merge block %s {TankContent:{%s},\"neoforge:attachments\":{\"criticalmass:tank_coolant\":{%s,Heat:%.1fd}}}" % (w(*pos), held, held, heat)


def setblock(w, pos, block):
    return "setblock %s %s" % (w(*pos), block)


def bind(w, panel, target, key):
    cmds = ["data merge block %s {%s:[I;0,0,0]}" % (w(*panel), key),
            "summon minecraft:marker %s {Tags:[\"%s\"]}" % (w(*target), TEMP)]
    cmds += ["execute store result block %s %s[%d] int 1 run data get entity @e[type=minecraft:marker,tag=%s,limit=1] Pos[%d]" % (w(*panel), key, i, TEMP, i) for i in range(3)]
    return cmds + ["kill @e[type=minecraft:marker,tag=%s]" % TEMP]


def place(w):
    (x0, z0), (x1, z1) = AREA
    cmds = ["kill @e[tag=%s]" % PROP]
    for x in range(x0, x1 + 1, 32):
        for z in range(z0, z1 + 1, 32):
            cmds.append("fill %s %s minecraft:air" % (w(x, 1, z), w(min(x + 31, x1), TOP, min(z + 31, z1))))
    groups = {}
    for pos, b in blocks.items():
        groups.setdefault(b, []).append(pos)
    order = sorted(groups, key=lambda b: (b == WATER, b))
    for b in order:
        for x, y, z, xx, yy, zz in cuboids(groups[b]):
            if (x, y, z) == (xx, yy, zz):
                cmds.append(setblock(w, (x, y, z), b))
            else:
                cmds.append("fill %s %s %s" % (w(x, y, z), w(xx, yy, zz), b))
    cmds += [setblock(w, pos, b) for pos, b in machines.items()]
    cmds.append(setblock(w, TURBINE, "criticalmass:steam_turbine[facing=east]{ScrollValue:64}"))
    cmds += [setblock(w, p, PIPE) for p in PIPES]
    cmds += [setblock(w, p, AIR) for p in PIPES]
    cmds += [setblock(w, p, PIPE) for p in PIPES]
    cmds += [setblock(w, pos, b) for pos, b in late]
    return cmds


def summon_props(origin):
    return ["summon %s %.1f %.1f %.1f %s" % (kind, origin[0] + pos[0], origin[1] + pos[1] - 1, origin[2] + pos[2], nbt) for pos, kind, nbt in props]


def start(w):
    (sx0, sy0), (sx1, sy1), sz = SCREENS
    cmds = [merge(w, CONTROLLER, "Coolant", "criticalmass:primary_coolant", 8000, 8000 * 80.0),
            merge(w, TURBINE, "Coolant", "minecraft:water", 1000, 1000 * 80.0),
            tank(w, ECCS_TANK, "criticalmass:primary_coolant", 4000),
            "data merge block %s {TankContent:{Fluid:{id:\"minecraft:water\",amount:1000}}}" % w(*MAKEUP_TANK),
            tank(w, FEED_TANK[0], "minecraft:water", 6000, 6000 * 80.0)]
    cmds += [merge(w, p, "Secondary", "minecraft:water", 1000, 1000 * 80.0) for p in EXCHANGERS]
    for p in CONDENSERS:
        cmds += [merge(w, p, "Primary", "minecraft:water", 1000, 1000 * 80.0), merge(w, p, "Secondary", "minecraft:water", 1000, 0)]
    cmds += [merge(w, p, "Buffer", "minecraft:water", 4000, 0) for p in RACKS]
    cmds += [merge(w, p, "Buffer", "minecraft:water", 500, 0) for p in FILL]
    for x, parts in sorted(DESKS.items()):
        for slot, (_, kind, target, mode, color, label) in enumerate(parts):
            cmds.append("criticalmass desk %s %d %s %s %d %d %s" % (w(x, DESK_Y, DESK_Z), slot, kind, w(*target), mode, color, label))
    cmds += bind(w, (sx1, sy1, sz), CONTROLLER, "Reactor") + bind(w, (sx1, sy1, sz), TURBINE, "Turbine")
    for _, (x1, y1), target in SIDE_SCREENS:
        cmds += bind(w, (x1, y1, sz), target, "Reactor")
    cmds += [setblock(w, SCREEN_MOTOR, "create:creative_motor[facing=north]"),
             "criticalmass fuel %s fill" % w(*CONTROLLER),
             setblock(w, (5, 4, -2), AIR),
             setblock(w, (7, 4, -2), "create:copper_valve_handle[facing=east]"),
             setblock(w, (17, 9, 0), AIR),
             setblock(w, (17, 9, 0), "create:copper_valve_handle[facing=up]"),
             setblock(w, PRIMARY_MOTOR, "criticalmass:electric_motor[facing=up]{ScrollValue:16}"),
             setblock(w, FEED_MOTOR, "criticalmass:electric_motor[facing=down]{ScrollValue:16}"),
             setblock(w, COOLING_MOTOR, "criticalmass:electric_motor[facing=up]{ScrollValue:16}"),
             setblock(w, TOWER_MOTOR, "criticalmass:electric_motor[facing=up]{ScrollValue:24}")]
    cmds += [setblock(w, pos, "criticalmass:control_rod_drive[facing=%s]" % facing) for pos, facing in DRIVES]
    cmds += [setblock(w, pos, "create:cogwheel[axis=y]") for pos in DRIVE_COGS]
    cmds += [setblock(w, DRIVE_MOTOR, "criticalmass:electric_motor[facing=down]{ScrollValue:16}")]
    cmds += [setblock(w, (SERVICE[0], SERVICE[1] + 1, SERVICE[2]), "create:copper_valve_handle[facing=up]")]
    cmds += ["data merge block %s {GaugeFace:\"east\"}" % w(*PRESSURIZER)]
    cmds += ["criticalmass wire %s %d %s %d %s" % (w(*a), ta, w(*b), tb, kind) for kind, a, ta, b, tb in WIRES]
    cmds += ["criticalmass breaker %s close" % w(*GRID_BREAKER), "criticalmass breaker %s close" % w(*STATION_BREAKER)]
    return cmds


def sync(w):
    return ["criticalmass breaker %s close" % w(*GEN_BREAKER)]


def load(w):
    return ["criticalmass generator %s %.2f" % (w(*GENERATORS[0]), RUN_LOAD)]


def run(w):
    return ["criticalmass steady %s %.2f %d" % (w(*CONTROLLER), RUN_POWER, RUN_COOLANT)]


def write(root):
    folder = os.path.join(root, "data", NS, "function", "facility")
    os.makedirs(folder, exist_ok=True)
    with open(os.path.join(root, "pack.mcmeta"), "w", newline="\n") as f:
        json.dump({"pack": {"pack_format": 48, "description": "Builds a running Critical Mass generating station"}}, f, indent=1)
    w = relative
    placed = place(w) + ["summon %s ~%.1f ~%.1f ~%.1f %s" % (kind, pos[0], pos[1] - 1, pos[2], nbt) for pos, kind, nbt in props]
    placed += ["schedule function %s:facility/start 100t replace" % NS]
    started = start(w) + ["tellraw @a {\"text\":\"Station built: fuelled, on grid power with its pumps running, rods in. Start it from the control room with the RODS OUT button and watch the period, or run the facility/run function to jump to power.\",\"color\":\"green\"}"]
    running = run(w) + ["schedule function %s:facility/sync %dt replace" % (NS, SYNC_AFTER),
                        "tellraw @a {\"text\":\"Reactor set to power with equilibrium xenon. The generator goes on the grid in %d seconds and takes load. Hold the reactor steady with the rod buttons in the control room.\",\"color\":\"green\"}" % (SYNC_AFTER // 20)]
    (x0, z0), (x1, z1) = AREA
    marker = "@e[type=minecraft:marker,tag=%s,limit=1]" % TAG
    span = "~%d ~%d ~%d ~%d" % (x0, z0, x1, z1)
    files = {
        "build": ["kill @e[type=minecraft:marker,tag=%s]" % TAG,
                  "summon minecraft:marker ~%d ~ ~%d {Tags:[\"%s\"]}" % (OFFSET[0], OFFSET[1], TAG),
                  "execute at %s run forceload add %s" % (marker, span),
                  "schedule function %s:facility/place 20t replace" % NS,
                  "tellraw @s {\"text\":\"Building the station. It is filled and fuelled in 6 seconds.\",\"color\":\"yellow\"}"],
        "place": ["execute as %s at @s run function %s:facility/place_here" % (marker, NS)],
        "place_here": placed,
        "start": ["execute as %s at @s run function %s:facility/start_here" % (marker, NS)],
        "start_here": started + ["forceload remove %s" % span],
        "run": ["execute as %s at @s run function %s:facility/run_here" % (marker, NS)],
        "run_here": running,
        "sync": ["execute as %s at @s run function %s:facility/sync_here" % (marker, NS)],
        "sync_here": sync(w) + ["schedule function %s:facility/load 40t replace" % NS],
        "load": ["execute as %s at @s run function %s:facility/load_here" % (marker, NS)],
        "load_here": load(w),
    }
    for name, cmds in files.items():
        with open(os.path.join(folder, name + ".mcfunction"), "w", newline="\n") as f:
            f.write("\n".join(cmds) + "\n")


design()

if __name__ == "__main__":
    for target in sys.argv[1:]:
        write(target)
        print("wrote", target)
    print(len(blocks), "blocks,", len(machines), "machines,", len(PIPES), "pipes,", len(place(relative)), "commands")
