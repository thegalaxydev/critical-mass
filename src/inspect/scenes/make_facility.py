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
ROD = "minecraft:lightning_rod[facing=up]"

AREA = ((-30, -48), (76, 46))
TOP = 36
OFFSET = (34, 52)

HALF = 4
VESSEL_TOP = 9
CONTROLLER = (-HALF, 5, 0)
SERVICE = (0, VESSEL_TOP, 0)
KIND = {"f": "fuel_channel", "r": "control_channel", "c": "coolant_channel"}
CORE = ["".join("f" if (ix + iz) % 2 == 0 else "r" if ix % 2 == 1 else "c" for ix in range(7)) for iz in range(7)]
VIEWPORTS = [(HALF, y, z) for y in (4, 5, 6) for z in (-2, -1, 1, 2)] + [(x, y, HALF) for x in (-2, 2) for y in (4, 5, 6)]
DRIVES = [((x, VESSEL_TOP + 1, z), "south") for x in (-2, 0, 2) for z in (-3, -1, 1, 3)]
DRIVE_COGS = [(x, VESSEL_TOP + 1, z) for x in (-3, -1, 1, 3) for z in (-3, -1, 1, 3)] + [(3, VESSEL_TOP + 1, z) for z in (-2, 0, 2)]
DRIVE_MOTOR = (3, VESSEL_TOP + 2, 0)
PRESSURIZER = (-5, 4, 3)
ECCS_PORT, ECCS_PUMP, ECCS_TANK = (-HALF, 3, -2), (-5, 3, -2), (-6, 3, -2)
ECCS_COG, ECCS_CLUTCH, ECCS_MOTOR, ECCS_LEVER = (-5, 4, -2), (-6, 4, -2), (-7, 4, -2), (-6, 5, -2)
RI, RO = 15.5, 17.5
UNLOAD_ARM, LOAD_ARM = (-1, 12, -2), (1, 12, -2)
REFUEL_COGS = [(0, 12, -2)]
REFUEL_MOTOR = (0, 13, -2)
REFUEL_RACKS, REFUEL_DEPOT = [(-2, 10, -5), (-3, 10, -5)], (2, 10, -5)
REFUEL_BREAKER = (5, 11, -5)
BANKS = {-2: 0, 0: 3, 2: 1}
ALL_BANKS = 4
CHANNELS = ["temperature", "coolant_temperature", "pressure", "level", "flow", "power"] + ["rods_" + "abcd"[b] for b in sorted(set(BANKS.values()))] \
    + ["neutron", "period", "reactivity", "xenon", "boron", "setpoint"]
PRESSES = ["scram", "reset", "rod_in", "rod_out", "regulator_raise", "regulator_lower"]

SG_X = 10
SG_Y = range(3, 11)
CROSS = 8
HALL = (18, 58, -17, 17)
DECK = 6


def route(*points):
    cells = []
    for a, b in zip(points, points[1:]):
        if a == b:
            continue
        assert sum(1 for i in range(3) if a[i] != b[i]) == 1, "a pipe run has to follow one axis: %s to %s" % (a, b)
        axis = next(i for i in range(3) if a[i] != b[i])
        step = 1 if b[axis] > a[axis] else -1
        for v in range(a[axis], b[axis] + step, step):
            cell = tuple(v if i == axis else a[i] for i in range(3))
            if not cells or cells[-1] != cell:
                cells.append(cell)
    return cells


class Loop:
    def __init__(self, name, s):
        self.name = name
        self.s = s
        self.outlet = (0, 7, HALF * s)
        self.inlet = (3, 5, HALF * s)
        self.facing = "south" if s > 0 else "north"
        self.pump = (6, 3, 10 * s)
        self.motor = (6, 2, 10 * s)
        self.valve = (3, 4, 5 * s)
        self.exchanger = (SG_X, CROSS, 8 * s)
        self.hot = route((0, 7, 5 * s), (0, 7, 6 * s), (SG_X, 7, 6 * s), (SG_X, CROSS, 6 * s), (SG_X, CROSS, 7 * s))
        self.cold = route((SG_X, CROSS, 9 * s), (SG_X, CROSS, 10 * s), (SG_X, 3, 10 * s), (7, 3, 10 * s)) + route((5, 3, 10 * s), (3, 3, 10 * s), (3, 3, 5 * s)) + [(3, 5, 5 * s)]
        self.pipes = self.hot + self.cold


class Train:
    def __init__(self, name, z0, tower):
        self.name = name
        self.z0 = z0
        self.tower = tower
        tx, tz = tower
        self.exchangers = [(SG_X, y, z0) for y in SG_Y]
        self.relief = (21, 8, z0)
        self.steam_valve = (24, 8, z0)
        self.turbine = (30, 8, z0)
        self.generator = (36, 8, z0)
        self.breaker = (40, 7, z0)
        self.condensers = [(x, 3, z0) for x in range(32, 40)]
        self.feed_tank = [(42, y, z0) for y in (1, 2, 3)]
        self.feed_pump = (40, 1, z0 + 3)
        self.feed_motor = (40, 2, z0 + 3)
        self.heater = (30, 4, z0)
        self.fill_pump, self.fill_cog, self.fill_clutch, self.fill_switch, self.fill_motor = (43, 1, z0), (43, 1, z0 - 1), (42, 1, z0 - 1), (42, 2, z0 - 1), (41, 1, z0 - 1)
        self.fill_tank = [(44, 1, z0)]
        self.fill_drop = (41, 5, z0 - 1 if z0 < 0 else z0 - 2)
        columns = [tx + 4, tx, tx - 4]
        self.fills = [[(x, y, tz) for y in range(2, 14)] for x in columns]
        self.fill = [cell for column in self.fills for cell in column]
        last = columns[-1]
        self.makeup_pipe, self.makeup_pump = (last, 3, tz + 1), (last, 3, tz + 2)
        self.makeup_intake = [(last, y, tz + 3) for y in (3, 2, 1)]
        self.makeup_cog, self.makeup_motor = (last, 4, tz + 2), (last, 4, tz + 3)
        self.steam = route((SG_X, 11, z0), (12, 11, z0), (12, 8, z0), (20, 8, z0)) + route((22, 8, z0), (23, 8, z0)) + route((25, 8, z0), (28, 8, z0)) \
            + route((30, 6, z0), (30, 5, z0)) + route((30, 3, z0), (31, 3, z0)) + route((40, 3, z0), (41, 3, z0)) \
            + route((42, 1, z0 + 1), (42, 1, z0 + 3), (41, 1, z0 + 3)) + route((39, 1, z0 + 3), (30, 1, z0 + 3), (30, 4, z0 + 3), (30, 4, z0 + 1)) \
            + route((30, 4, z0 - 1), (28, 4, z0 - 1), (28, 4, z0 + 3), (28, 1, z0 + 3), (SG_X, 1, z0 + 3), (SG_X, 1, z0), (SG_X, 2, z0))
        out, hot = tx - 6, tx + 6
        between = []
        for x in columns[:-1]:
            between += route((x - 1, 2, tz), (x - 2, 2, tz), (x - 2, 13, tz), (x - 3, 13, tz))
        if z0 < 0:
            self.circ_pump = (out, 2, -14)
            supply = route((last - 1, 2, tz), (out, 2, tz), (out, 2, -15)) + route((out, 2, -13), (out, 2, z0 - 2), (32, 2, z0 - 2), (32, 4, z0 - 2), (32, 4, z0))
            back = route((39, 2, z0), (39, 2, z0 - 4), (hot, 2, z0 - 4), (hot, 2, tz), (hot, 13, tz), (hot - 1, 13, tz))
        else:
            self.circ_pump = (out, 2, -16)
            supply = route((last - 1, 2, tz), (out, 2, tz), (out, 2, -17)) + route((out, 2, -15), (out, 3, -15), (out, 3, z0 - 4), (out, 4, z0 - 4), (out, 4, z0 - 1), (32, 4, z0 - 1), (32, 4, z0))
            back = route((39, 2, z0), (39, 2, z0 - 2), (46, 2, z0 - 2), (46, 2, -19), (hot, 2, -19), (hot, 2, tz), (hot, 13, tz), (hot - 1, 13, tz))
        self.circ_motor = (self.circ_pump[0], 1, self.circ_pump[2])
        self.cooling = supply + back + between
        self.loops = [self.steam, self.cooling]


LOOPS_PRIMARY = [Loop("a", -1), Loop("b", 1)]
LA, LB = LOOPS_PRIMARY
TRAINS = [Train("a", -8, (26, -32)), Train("b", 8, (50, -32))]
TA, TB = TRAINS
LOOPS = [loop.pipes for loop in LOOPS_PRIMARY] + [loop for t in TRAINS for loop in t.loops]
PIPES = [cell for loop in LOOPS for cell in loop]
PIPE_SET = set(PIPES)

GENERATORS = [t.generator for t in TRAINS]
GEN_BREAKERS = [t.breaker for t in TRAINS]
TURBINES = [t.turbine for t in TRAINS]
BUS20 = [(46, 7, -1), (46, 7, 0), (46, 7, 1)]
IN20, OUT20 = (57, 10, 0), (59, 10, 0)
P_A, P_B = (43, 10, -5), (43, 10, 5)
MAIN_T = (62, 1, -3)
GRID_BREAKER = (64, 1, -3)
GRID = (66, 1, -3)
UNIT_T = (63, 1, -6)
ARRESTER = (63, 1, -1)
OUT6, IN6 = (59, 3, -6), (57, 3, -6)
STATION_BREAKER = (55, 1, -5)
BUS6 = [(51, 1, -5), (52, 1, -5), (53, 1, -5)]
BATTERY = (49, 1, -5)
BATTERY_B = (49, 1, -7)
STANDBY_ENGINE, STANDBY_GEN, STANDBY_T, TRANSFER = (50, 1, 7), (51, 1, 7), (53, 1, 7), (55, 1, 7)
START_LINE = [(x, 1, 8) for x in range(50, 56)]
FEED_REACTOR, FEED_A, FEED_B, FEED_FUEL = (50, 1, -9), (52, 1, -9), (54, 1, -9), (56, 1, -9)
FEEDERS = [FEED_REACTOR, FEED_A, FEED_B, FEED_FUEL]
FUEL_UP, FUEL_2, FUEL_3 = (56, 5, -9), (40, 5, 3), (20, 5, 3)
C_NORMAL, C_D, C_LOAD, C_L2 = (55, 5, -4), (55, 5, 6), (53, 5, 6), (53, 5, -4)
R_UP, R2, H_IN, C_IN, C_HI = (48, 5, -3), (30, 5, 0), (19, 5, 0), (15, 5, 0), (15, 12, 0)
A_UP, A2, A_F, A3, N_A_IN, N_A_OUT = (48, 5, -11), (43, 5, -11), (41, 5, -4), (22, 5, -13), (22, 2, -16), (22, 2, -18)
B3 = (42, 5, -16)
B_UP, B2, B3_UP, N_B_IN, N_B_OUT = (50, 5, -3), (43, 5, 13), (48, 5, -14), (48, 3, -16), (48, 3, -18)
CELL_Z = 38
SAWS = [(x, 1, CELL_Z + 1) for x in (3, 4, 5, 6)]
SAW_DRIVES = [(x, 1, CELL_Z + 2) for x in (3, 4, 5, 6)]
SAW_MOTOR = (6, 1, CELL_Z + 3)
FEED_PORT = (6, 1, CELL_Z)
SHEAR_DEPOT = (2, 1, CELL_Z + 1)
DISSOLVER_BURNER, DISSOLVER, DISSOLVER_MIXER = (-1, 1, CELL_Z + 1), (-1, 2, CELL_Z + 1), (-1, 4, CELL_Z + 1)
DISSOLVER_COG, DISSOLVER_ARM, DISSOLVER_MOTOR = (0, 4, CELL_Z + 1), (1, 4, CELL_Z + 1), (0, 5, CELL_Z + 1)
PRODUCT = (-1, 1, CELL_Z)
WASTE_PUMP, WASTE_PIPE = (-2, 1, CELL_Z), (-3, 1, CELL_Z)
WASTE_TANK = [(-4, y, CELL_Z) for y in (1, 2, 3)]
WATER_PUMP = (-2, 2, CELL_Z + 1)
WATER_TANK = [(-3, 2, CELL_Z + 1), (-3, 3, CELL_Z + 1)]
PUMP_COG, PUMP_MOTOR = (-2, 1, CELL_Z + 1), (-3, 1, CELL_Z + 1)
HALL_OUT, AUX_IN, AUX_OUT, YARD_OUT, CELL_OUT, CELL_IN = (19, 5, 16), (19, 5, 18), (19, 5, 32), (19, 5, 34), (12, 5, CELL_Z + 2), (10, 5, CELL_Z + 2)
CELL_MID, CELL_WEST = (0, 5, CELL_Z + 4), (-4, 5, CELL_Z + 4)
BENCH_Z = CELL_Z - 1
BENCH_BELT = [(x, 1, BENCH_Z) for x in (-5, -6, -7, -8, -9)]
BENCH_DEPOT = (-10, 1, BENCH_Z)
BENCH_SPOUT, BENCH_DEPLOYER, BENCH_PRESS, BENCH_GEARBOX = (-6, 3, BENCH_Z), (-7, 3, BENCH_Z), (-8, 3, BENCH_Z), (-9, 3, BENCH_Z)
BENCH_COGS = [(-9, y, CELL_Z) for y in (1, 2, 3)]
BENCH_MOTOR = (-9, 3, CELL_Z + 1)
CANNING_PUMP, CANNING_PIPE, CANNING_COG, CANNING_MOTOR = (-5, 3, CELL_Z), (-6, 3, CELL_Z), (-5, 3, CELL_Z + 1), (-4, 3, CELL_Z + 1)
WELL, WELL_PUMP, WELL_COG, WELL_MOTOR = (43, 0, -2), (44, 1, -2), (44, 1, -3), (43, 1, -3)
WELL_MAIN = route((45, -1, TA.z0), (45, -1, TB.z0))
WELL_PIPES = [(43, 1, -2)] + route((45, 1, -2), (45, 0, -2)) + route((45, 1, TA.z0), (45, 0, TA.z0)) + WELL_MAIN + route((45, 0, TB.z0), (45, 1, TB.z0))
CELL_WELL, CELL_WELL_PUMP = (-1, 0, CELL_Z + 6), (-2, 1, CELL_Z + 2)
CELL_WELL_PIPES = route((-1, 1, CELL_Z + 2), (-1, 1, CELL_Z + 6)) + [(-3, 1, CELL_Z + 2), (-3, 2, CELL_Z + 2)]
INSULATORS = [(IN20, "west"), (OUT20, "east"), (P_A, "up"), (P_B, "up"), (OUT6, "east"), (IN6, "west"), (C_NORMAL, "down"), (C_D, "down"), (C_LOAD, "down"), (C_L2, "down"),
              (R_UP, "down"), (R2, "down"), (H_IN, "east"), (C_IN, "west"), (C_HI, "west"),
              (A_UP, "down"), (A2, "down"), (A_F, "down"), (A3, "down"), (N_A_IN, "south"), (N_A_OUT, "north"),
              (B_UP, "down"), (B2, "down"), (B3_UP, "down"), (B3, "down"), (N_B_IN, "south"), (N_B_OUT, "north"),
              (TA.fill_drop, "down"), (TB.fill_drop, "down"), (HALL_OUT, "north"), (AUX_IN, "south"), (AUX_OUT, "north"), (YARD_OUT, "south"), (CELL_OUT, "east"), (CELL_IN, "west"),
              (CELL_MID, "north"), (CELL_WEST, "north"), (FUEL_UP, "down"), (FUEL_2, "down"), (FUEL_3, "down")]
BUSHINGS = [(58, 10, 0), (58, 3, -6), (16, 5, 0), (17, 5, 0), (18, 5, 0), (22, 2, -17), (48, 3, -17), (19, 5, 17), (19, 5, 33), (11, 5, CELL_Z + 2)]
SLOTS = [(47, 4, -23)]
HIGH, LOW, LINE, LOAD = 0, 1, 0, 1
ON_NORMAL, ON_STANDBY, TO_LOAD = 0, 1, 2
WIRES = [
    ("cable", TA.generator, 0, TA.breaker, LINE), ("cable", TA.breaker, LOAD, P_A, 0), ("cable", P_A, 0, BUS20[0], 0), ("cable", TB.generator, 0, TB.breaker, LINE), ("cable", TB.breaker, LOAD, P_B, 0), ("cable", P_B, 0, BUS20[2], 0),
    ("cable", BUS20[1], 0, IN20, 0), ("cable", IN20, 0, OUT20, 0), ("cable", OUT20, 0, MAIN_T, LOW), ("line", MAIN_T, HIGH, GRID_BREAKER, LOAD), ("line", GRID_BREAKER, LINE, GRID, 0),
    ("line", ARRESTER, 0, GRID_BREAKER, LOAD), ("cable", OUT20, 0, UNIT_T, HIGH), ("cable", UNIT_T, LOW, OUT6, 0), ("cable", OUT6, 0, IN6, 0), ("cable", IN6, 0, STATION_BREAKER, LINE),
    ("cable", STATION_BREAKER, LOAD, C_NORMAL, 0), ("cable", C_NORMAL, 0, C_D, 0), ("cable", C_D, 0, TRANSFER, ON_NORMAL), ("cable", STANDBY_GEN, 0, STANDBY_T, HIGH),
    ("cable", STANDBY_T, LOW, TRANSFER, ON_STANDBY), ("cable", TRANSFER, TO_LOAD, C_LOAD, 0), ("cable", C_LOAD, 0, C_L2, 0), ("cable", C_L2, 0, BUS6[2], 0), ("cable", BATTERY, 0, BUS6[0], 0), ("cable", BATTERY_B, 0, BUS6[0], 0),
    ("cable", BUS6[0], 0, FEED_REACTOR, LINE), ("cable", BUS6[1], 0, FEED_A, LINE), ("cable", BUS6[2], 0, FEED_B, LINE),
    ("wire", FEED_REACTOR, LOAD, R_UP, 0), ("wire", R_UP, 0, R2, 0), ("wire", R2, 0, H_IN, 0), ("wire", H_IN, 0, C_IN, 0), ("wire", C_IN, 0, LA.motor, 0), ("wire", C_IN, 0, LB.motor, 0),
    ("wire", C_IN, 0, C_HI, 0), ("wire", C_HI, 0, DRIVE_MOTOR, 0), ("wire", C_HI, 0, REFUEL_BREAKER, LINE), ("wire", REFUEL_BREAKER, LOAD, REFUEL_MOTOR, 0),
    ("wire", FEED_A, LOAD, A_UP, 0), ("wire", A_UP, 0, A2, 0), ("wire", A2, 0, A_F, 0), ("wire", A_F, 0, TA.feed_motor, 0), ("wire", A2, 0, A3, 0), ("wire", A3, 0, TA.circ_motor, 0),
    ("wire", A3, 0, N_A_IN, 0), ("wire", N_A_IN, 0, N_A_OUT, 0), ("wire", N_A_OUT, 0, TA.makeup_motor, 0),
    ("wire", FEED_B, LOAD, B_UP, 0), ("wire", B_UP, 0, B2, 0), ("wire", B2, 0, TB.feed_motor, 0), ("wire", FEED_B, LOAD, B3_UP, 0), ("wire", B3_UP, 0, B3, 0), ("wire", B3, 0, TB.circ_motor, 0),
    ("wire", FEED_B, LOAD, N_B_IN, 0), ("wire", N_B_IN, 0, N_B_OUT, 0), ("wire", N_B_OUT, 0, TB.makeup_motor, 0),
    ("wire", A_F, 0, TA.fill_drop, 0), ("wire", TA.fill_drop, 0, TA.fill_motor, 0), ("wire", B2, 0, TB.fill_drop, 0), ("wire", TB.fill_drop, 0, TB.fill_motor, 0),
    ("wire", A_F, 0, WELL_MOTOR, 0),
    ("cable", BUS6[2], 0, FEED_FUEL, LINE), ("cable", FEED_FUEL, LOAD, FUEL_UP, 0), ("cable", FUEL_UP, 0, FUEL_2, 0), ("cable", FUEL_2, 0, FUEL_3, 0),
    ("cable", FUEL_3, 0, HALL_OUT, 0), ("cable", HALL_OUT, 0, AUX_IN, 0), ("cable", AUX_IN, 0, AUX_OUT, 0), ("cable", AUX_OUT, 0, YARD_OUT, 0), ("cable", YARD_OUT, 0, CELL_OUT, 0),
    ("cable", CELL_OUT, 0, CELL_IN, 0), ("wire", CELL_IN, 0, SAW_MOTOR, 0), ("wire", CELL_IN, 0, CELL_MID, 0), ("wire", CELL_MID, 0, DISSOLVER_MOTOR, 0), ("wire", CELL_MID, 0, CELL_WEST, 0),
    ("wire", CELL_MID, 0, PUMP_MOTOR, 0), ("wire", CELL_WEST, 0, BENCH_MOTOR, 0), ("wire", CELL_WEST, 0, CANNING_MOTOR, 0),
]
SPEEDS = {"primary": 32, "feed": 16, "circ": 32, "makeup": 8, "drive": 16, "refuel": 32, "fill": 16, "well": 16, "shear": 32, "dissolver": 32, "process": 16, "bench": -32, "canning": 16}
RAMP = ((0.3, 126), (0.6, 153), (0.9, 179))
PART_LOAD, RUN_LOAD = 0.3, 0.6
RUN_POWER, RUN_COOLANT = RAMP[-1]

AUX = (18, 48, 17, 33)
DESK_Y, DESK_Z = 7, 24
SCREEN_Z = 28
SCREENS = [((27, 9), (35, 13), CONTROLLER, TA.turbine), ((19, 10), (21, 12), TA.generator, None), ((23, 10), (25, 12), TA.turbine, None),
           ((37, 10), (39, 12), TB.turbine, None), ((41, 10), (43, 12), TB.generator, None)]
SCREEN_COGS = [(22, 11), (36, 11), (40, 11)]
MIMIC_X = 44
EAST_SCREENS = [((19, 9), (23, 12), BUS6[1]), ((25, 10), (27, 12), GRID)]
EAST_COGS = [(13, 21), (13, 26)]


def turbine_desk(t, tag):
    return [[("sync", "gauge", t.breaker, 0, 4, tag + " SYNC ANGLE"), ("hertz", "gauge", t.generator, 1, 4, tag + " FREQUENCY")],
            [("gen_open", "button", t.breaker, 1, 1, tag + " GEN OPEN"), ("gen_close", "button", t.breaker, 0, 2, tag + " GEN CLOSE")],
            [("reset", "button", t.turbine, 1, 7, tag + " TURB RESET"), ("trip", "button", t.turbine, 0, 1, tag + " TURB TRIP")],
            [("speed", "gauge", t.turbine, 6, 4, tag + " SPEED"), ("shaft", "gauge", t.turbine, 5, 4, tag + " POWER")],
            [("lower", "button", t.generator, 1, 4, tag + " LOWER"), ("raise", "button", t.generator, 0, 2, tag + " RAISE")],
            [("megawatts", "gauge", t.generator, 0, 4, tag + " GEN MW"), ("feed", "gauge", t.feed_pump, 0, 4, tag + " FEED FLOW")]]


def breaker_desk(pos, tag):
    return [("open", "button", pos, 1, 1, tag + " OPEN"), ("close", "button", pos, 0, 2, tag + " CLOSE")]


BENCH = [[("supply", "alarm", TRANSFER, 0, 0, "STATION SUPPLY"), ("battery", "alarm", BATTERY, 0, 0, "BATTERY")]] \
    + turbine_desk(TA, "A") \
    + [[("flow_a", "gauge", LA.pump, 0, 4, "LOOP A FLOW"), ("flow_b", "gauge", LB.pump, 0, 4, "LOOP B FLOW")],
       [("pressure", "gauge", CONTROLLER, 2, 4, "PRESSURE"), ("coolant", "gauge", CONTROLLER, 1, 4, "COOLANT")],
       [("power", "gauge", CONTROLLER, 5, 4, "HEAT OUTPUT"), ("alarm", "alarm", CONTROLLER, 0, 0, "ALARM")],
       [("rods_all", "lever", CONTROLLER, ALL_BANKS, 7, "ROD LEVER ALL"), ("rods_d", "lever", CONTROLLER, 3, 7, "ROD LEVER D")],
       [("rod_out", "button", CONTROLLER, PRESSES.index("rod_out"), 2, "RODS OUT"), ("rod_in", "button", CONTROLLER, PRESSES.index("rod_in"), 4, "RODS IN")],
       [("reg_lower", "button", CONTROLLER, PRESSES.index("regulator_lower"), 4, "REG LOWER"), ("reg_raise", "button", CONTROLLER, PRESSES.index("regulator_raise"), 2, "REG RAISE")],
       [("setpoint", "gauge", CONTROLLER, CHANNELS.index("setpoint"), 4, "REG SETPOINT"), ("neutron", "gauge", CONTROLLER, CHANNELS.index("neutron"), 4, "NEUTRON POWER")],
       [("reset", "button", CONTROLLER, 1, 7, "RESET"), ("scram", "button", CONTROLLER, 0, 0, "SCRAM")],
       [("bypass", "switch", CONTROLLER, 0, 1, "TRIP BYPASS"), ("level", "gauge", CONTROLLER, 3, 4, "LEVEL")]] \
    + turbine_desk(TB, "B") \
    + [[("grid", "gauge", GRID, 0, 4, "GRID MW"), ("grid_alarm", "alarm", GRID, 0, 0, "GRID")],
       breaker_desk(GRID_BREAKER, "GRID BKR"), breaker_desk(STATION_BREAKER, "STN SUPPLY")]
DESKS = {19 + i: parts for i, parts in enumerate(BENCH)}
SIDE_X = 19
SIRENS = [((37, 12, AUX[3] + 1), "south", True), ((SIDE_X, 11, 20), "east", True)]
SIDE_DESKS = {18 + i: parts for i, parts in enumerate([breaker_desk(FEED_REACTOR, "RX FEEDER"), breaker_desk(FEED_A, "A FEEDER"), breaker_desk(FEED_B, "B FEEDER"),
                                                       breaker_desk(FEED_FUEL, "FUEL FEEDER")])}
FILL_BELOW, FILL_ABOVE = 10000, 12000
RACKS = [(x, -1, z) for x in (-8, -6) for z in (26, 28)]

blocks = {}
late = []
machines = {}
props = []
ports = set()
reserved = set()
columns = set()


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


def machine(pos, b):
    machines[pos] = b
    blocks.pop(pos, None)


def attach(pos, b):
    late.append((pos, b))
    blocks.pop(pos, None)


def taken(pos):
    return pos in blocks or pos in machines or pos in PIPE_SET or pos in reserved or any(pos == at for at, _ in late)


def deco(x, y, z, b):
    if not taken((x, y, z)):
        put(x, y, z, b)


def stand(pos, b=STEEL):
    for y in range(1, pos[1]):
        deco(pos[0], y, pos[2], b)


def join(pipe, target):
    assert pipe in PIPE_SET, "no pipe at %s to join to %s" % (pipe, target)
    ports.add((pipe, target))


def sign(pos, facing, *lines):
    text = ",".join("'{\"text\":\"%s\"}'" % line for line in (list(lines) + ["", "", "", ""])[:4])
    attach(pos, "minecraft:birch_wall_sign[facing=%s]{front_text:{messages:[%s]}}" % (facing, text))


def post(pos, rotation, *lines):
    assert not taken(pos), "sign post in the way at %s" % (pos,)
    text = ",".join("'{\"text\":\"%s\"}'" % line for line in (list(lines) + ["", "", "", ""])[:4])
    attach(pos, "minecraft:birch_sign[rotation=%d]{front_text:{messages:[%s]}}" % (rotation, text))


def door(x, y, z, facing, hinge="left"):
    carve(x, y, z, x, y + 1, z)
    for half, dy in (("lower", 0), ("upper", 1)):
        attach((x, y + dy, z), "create:andesite_door[facing=%s,half=%s,hinge=%s,open=false,visible=true]" % (facing, half, hinge))


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


def stripes(x0, z0, x1, z1, y):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                if (x, y, z) in blocks:
                    put(x, y, z, YELLOW if (x + z) % 2 == 0 else BLACK)


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
    carve(40, 1, fz1, 43, 2, fz1)
    for x in (39, 44):
        box(x, 1, fz1, x, 4, fz1, STEEL)
        put(x, 5, fz1, LAMP)
    box(40, 0, 34, 43, 0, fz1 + 1, FLOOR)
    for z in range(35, fz1 + 1, 2):
        put(40, 0, z, YELLOW)
        put(43, 0, z, YELLOW)
    box(59, 0, 2, 62, 0, 40, FLOOR)
    box(44, 0, 37, 62, 0, 40, FLOOR)
    for x in (52, 66):
        for z in (12, 24, 36):
            box(x, 1, z, x, 5, z, STEEL)
            put(x, 6, z, LAMP)
    for x in (-22, -8, 8):
        box(x, 1, -30, x, 5, -30, STEEL)
        put(x, 6, -30, LAMP)
    for z in (-20, 0, 20):
        box(-25, 1, z, -25, 5, z, STEEL)
        put(-25, 6, z, LAMP)


def containment():
    for x in range(-18, 19):
        for z in range(-18, 19):
            d = math.hypot(x, z)
            if d < RI:
                put(x, 0, z, YELLOW if 12.5 <= d < 13.5 else CLEAN)
                if d >= RI - 1:
                    put(x, 16, z, STEEL)
            elif d < RO:
                for y in range(0, 20):
                    put(x, y, z, SC if d < RI + 1 else (PLINTH if y < 2 else BAND if y == 18 else SHELL))
            for y in range(20, 33):
                t = y - 19
                if (d / RO) ** 2 + (t / 12.0) ** 2 <= 1 and (d / RI) ** 2 + (t / 10.0) ** 2 > 1:
                    put(x, y, z, SC if (d / (RI + 1)) ** 2 + (t / 11.0) ** 2 <= 1 else SHELL)
    for deg in range(10, 360, 20):
        x, z = round(16.0 * math.cos(math.radians(deg))), round(16.0 * math.sin(math.radians(deg)))
        if RI <= math.hypot(x, z) < RI + 1:
            for y in (3, 9, 14):
                put(x, y, z, LAMP)
    for deg in range(0, 360, 30):
        for radius, y in ((14.6, 22), (10.8, 26), (5.6, 28)):
            x, z = round(radius * math.cos(math.radians(deg))), round(radius * math.sin(math.radians(deg)))
            wall_lamp(x, y, z)
    for x, z in ((-9, -9), (9, 0), (-9, 9), (0, 11), (0, -11), (-12, 0), (12, -3), (12, 3), (7, -13), (7, 13), (-3, 13), (-3, -13)):
        put(x, 0, z, LAMP)
    for x in range(-14, 15):
        for z in (-1, 1):
            if math.hypot(x, z) < RI - 1:
                put(x, 17, z, GIRDER_X)
    box(-1, 17, -1, 1, 17, 1, IRON)
    put(0, 17, 0, LAMP)
    for y in (16, 15, 14):
        attach((0, y, 0), CHAIN)
    attach((0, 13, 0), "minecraft:grindstone[face=ceiling,facing=north]")
    for deg in range(0, 360, 30):
        x, z = round(14.6 * math.cos(math.radians(deg))), round(14.6 * math.sin(math.radians(deg)))
        if blocks.get((x, 16, z)) == STEEL:
            attach((x, 15, z), LANTERN)
    for x in range(-6, 7):
        for z in range(-6, 7):
            edge = max(abs(x), abs(z))
            if edge in (5, 6):
                put(x, 9, z, GRATE)
            if edge == 6 and (x, z) != (-6, 0):
                put(x, 10, z, BARS)
    for x, z in ((-6, -6), (6, -6), (-6, 6), (6, 6)):
        box(x, 1, z, x, 8, z, STEEL)
    stairs(-8, 1, -9, 0, 1, 9, "south")
    for i in range(9):
        put(-9, 2 + i, -9 + i, BARS)
    for x, z in ((-8, 0), (-7, 0), (-7, -1)):
        put(x, 9, z, GRATE)
    for x, z in ((-9, 0), (-8, 1), (-7, 1)):
        put(x, 10, z, BARS)
    box(-8, 1, -1, -8, 8, -1, STEEL)
    box(-8, 1, -5, -8, 4, -5, STEEL)
    carve(0, 1, 16, 0, 2, 17)
    door(0, 1, 17, "north")
    sign((1, 2, 18), "south", "Reactor", "Building", "Airlock")
    sign((-1, 3, 15), "north", "Exit")
    for z0, z1 in ((-3, 3),):
        for z in range(z0, z1 + 1):
            for y in range(1, 7):
                put(-17, y, z, YELLOW if (y + z) % 2 == 0 else BLACK)
                put(-16, y, z, IRON)
    sign((-15, 3, 0), "east", "Equipment", "Hatch")
    post((-6, 1, -4), 4, "Emergency", "Injection")
    post((-7, 1, 3), 4, "Pressurizer")
    post((-4, 10, -5), 8, "Spent Fuel", "Transfer Racks")
    post((3, 10, -5), 8, "Fresh Fuel")
    post((5, 10, -4), 4, "Refuelling", "Arms", "Breaker")
    post((12, 1, -8), 12, "Steam", "Generator A")
    post((12, 1, 8), 12, "Steam", "Generator B")
    post((6, 1, -12), 0, "Loop A", "Coolant Pump")
    post((6, 1, 12), 8, "Loop B", "Coolant Pump")
    sign((15, 3, 1), "west", "Electrical", "Penetration")


def vessel():
    for x in range(-HALF, HALF + 1):
        for y in range(1, VESSEL_TOP + 1):
            for z in range(-HALF, HALF + 1):
                if max(abs(x), abs(z)) == HALF or y in (1, VESSEL_TOP):
                    machine((x, y, z), "criticalmass:reactor_vessel_casing")
    machine(CONTROLLER, "criticalmass:reactor_controller[facing=west]")
    machine(SERVICE, "criticalmass:reactor_service_port[facing=up]")
    for loop in LOOPS_PRIMARY:
        machine(loop.outlet, "criticalmass:primary_coolant_port[facing=%s,mode=outlet]" % loop.facing)
        machine(loop.inlet, "criticalmass:primary_coolant_port[facing=%s,mode=inlet]" % loop.facing)
    machine(ECCS_PORT, "criticalmass:primary_coolant_port[facing=west,mode=inlet]")
    for iz, row in enumerate(CORE):
        for ix, ch in enumerate(row):
            for y in range(2, VESSEL_TOP):
                machine((ix - 3, y, iz - 3), "criticalmass:" + KIND[ch])
    for p in VIEWPORTS:
        machine(p, "criticalmass:reactor_viewport")
    machine(PRESSURIZER, "criticalmass:pressurizer")
    machine((PRESSURIZER[0], PRESSURIZER[1] - 1, PRESSURIZER[2]), "create:blaze_burner[blaze=kindled]{isCreative:1b}")
    stand((PRESSURIZER[0], PRESSURIZER[1] - 1, PRESSURIZER[2]))
    machine(ECCS_PUMP, "create:mechanical_pump[facing=east]")
    machine(ECCS_TANK, "create:fluid_tank")
    machine(ECCS_COG, "create:cogwheel[axis=x]")
    machine(ECCS_CLUTCH, "create:clutch[axis=x]")
    machine(ECCS_MOTOR, "create:creative_motor[facing=east]")
    attach(ECCS_LEVER, "minecraft:lever[face=floor,facing=north,powered=true]")
    for pos in (ECCS_PUMP, ECCS_TANK):
        stand(pos)
    box(-7, 1, -2, -7, 3, -2, STEEL)
    reserved.update([DRIVE_MOTOR, REFUEL_MOTOR, (SERVICE[0], SERVICE[1] + 1, SERVICE[2])] + [pos for pos, _ in DRIVES] + DRIVE_COGS)
    refuelling()


def reach(arm, target):
    return "[I;%d,%d,%d]" % tuple(target[i] - arm[i] for i in range(3))


def refuelling():
    for arm, targets in ((UNLOAD_ARM, [SERVICE] + REFUEL_RACKS), (LOAD_ARM, [SERVICE, REFUEL_DEPOT])):
        assert all(math.dist(arm, target) < 5 for target in targets), "an arm cannot reach from %s" % (arm,)
    racks = ",".join("{Type:\"criticalmass:spent_fuel_rack\",Pos:%s,Mode:\"DEPOSIT\"}" % reach(UNLOAD_ARM, rack) for rack in REFUEL_RACKS)
    machine(UNLOAD_ARM, "create:mechanical_arm[ceiling=true]{InteractionPoints:[{Type:\"criticalmass:service_port\",Pos:%s,Mode:\"TAKE\"},%s]}" % (reach(UNLOAD_ARM, SERVICE), racks))
    machine(LOAD_ARM, "create:mechanical_arm[ceiling=true]{InteractionPoints:[{Type:\"create:depot\",Pos:%s,Mode:\"TAKE\"},{Type:\"criticalmass:service_port\",Pos:%s,Mode:\"DEPOSIT\"}]}"
            % (reach(LOAD_ARM, REFUEL_DEPOT), reach(LOAD_ARM, SERVICE)))
    for pos in REFUEL_COGS:
        machine(pos, "create:cogwheel[axis=y]")
    for rack in REFUEL_RACKS:
        machine(rack, "criticalmass:spent_fuel_rack")
    machine(REFUEL_DEPOT, "create:depot")
    machine(REFUEL_BREAKER, "criticalmass:circuit_breaker[facing=west,closed=false]")
    put(REFUEL_BREAKER[0], REFUEL_BREAKER[1] - 1, REFUEL_BREAKER[2], STEEL)
    box(0, 13, -5, 0, 13, -3, GIRDER_Z)
    box(0, 10, -6, 0, 13, -6, STEEL)


def primary(loop):
    machine(loop.pump, "criticalmass:circulation_pump[facing=west,axis_along_first=true]")
    stand(loop.motor)
    machine(loop.valve, "criticalmass:reinforced_valve[facing=north,axis_along_first=true]")
    machine((loop.valve[0] - 1, loop.valve[1], loop.valve[2]), "create:creative_motor[facing=east]")
    reserved.update([loop.motor, (loop.valve[0] + 1, loop.valve[1], loop.valve[2])])
    s = loop.s
    join(loop.hot[0], loop.outlet)
    join(loop.hot[-1], loop.exchanger)
    join(loop.cold[0], loop.exchanger)
    join((7, 3, 10 * s), loop.pump)
    join((5, 3, 10 * s), loop.pump)
    join((3, 3, 5 * s), loop.valve)
    join((3, 5, 5 * s), loop.valve)
    join((3, 5, 5 * s), loop.inlet)


def train(t):
    z0 = t.z0
    for pos in t.exchangers:
        machine(pos, "criticalmass:heat_exchanger[facing=north]")
    machine(t.relief, "criticalmass:relief_valve[facing=up]")
    machine(t.steam_valve, "criticalmass:reinforced_valve[facing=north,axis_along_first=false]")
    machine((t.steam_valve[0], t.steam_valve[1] + 1, t.steam_valve[2]), "create:creative_motor[facing=down]{ScrollValue:-16}")
    for pos in t.condensers:
        machine(pos, "criticalmass:condenser[facing=east]")
    for pos in t.feed_tank:
        machine(pos, "create:fluid_tank")
    machine(t.feed_pump, "criticalmass:circulation_pump[facing=west,axis_along_first=true]")
    machine(t.heater, "criticalmass:heat_exchanger[facing=north]")
    for pos in t.fill_tank:
        machine(pos, "create:fluid_tank")
    machine(t.fill_pump, "create:mechanical_pump[facing=west]")
    machine(t.fill_cog, "create:cogwheel[axis=x]")
    machine(t.fill_clutch, "create:clutch[axis=x]")
    machine(t.fill_switch, "create:stockpile_switch[facing=south,target=wall]{OnAboveAmount:%d.0f,OffBelowAmount:%d.0f}" % (FILL_ABOVE, FILL_BELOW))
    machine(t.circ_pump, "criticalmass:circulation_pump[facing=south,axis_along_first=false]")
    tx, ty, tz = t.turbine
    for dx, b in ((2, "create:shaft[axis=x]"), (3, "create:stressometer[facing=south,axis_along_first=true]"), (4, "create:speedometer[facing=south,axis_along_first=true]"), (5, "create:shaft[axis=x]")):
        put(tx + dx, 7, tz, STEEL)
        machine((tx + dx, 8, tz), b)
    put(t.generator[0], 7, t.generator[2], IRON)
    machine(t.generator, "criticalmass:generator[axis=x]")
    machine(t.breaker, "criticalmass:circuit_breaker[facing=south,closed=false]")
    for x in (t.relief[0], t.steam_valve[0], tx - 2):
        put(x, 7, z0, STEEL)
    for pos in t.fill:
        machine(pos, "criticalmass:cooling_tower_packing")
    for column in t.fills:
        put(column[0][0], 1, column[0][2], STEEL)
    machine(t.makeup_pipe, "create:fluid_pipe")
    machine(t.makeup_pump, "create:mechanical_pump[facing=north]")
    for pos in t.makeup_intake:
        machine(pos, "create:fluid_pipe")
    x, y, z = t.makeup_intake[-1]
    assert blocks.get((x, y - 1, z)) == WATER, "the make-up intake at %s is not over the basin" % (t.makeup_intake[-1],)
    machine(t.makeup_cog, "create:cogwheel[axis=z]")
    stand(t.makeup_pump)
    x, y, z = t.makeup_motor
    assert not any(taken((x - 1, j, z)) for j in range(1, y + 2)), "the make-up motor's post is in the way at %d %d" % (x - 1, z)
    box(x - 1, 0, z, x - 1, y + 1, z, STEEL)
    put(t.makeup_motor[0], t.makeup_motor[1] + 1, t.makeup_motor[2], STEEL)
    put(t.makeup_cog[0], t.makeup_cog[1] + 1, t.makeup_cog[2], STEEL)
    stand(t.circ_motor)
    reserved.update([t.feed_motor, t.circ_motor, t.makeup_motor, t.fill_motor])
    hx, hy, hz = t.heater
    join((hx, hy + 1, hz), t.heater)
    join((hx, hy - 1, hz), t.heater)
    join((hx, hy, hz + 1), t.heater)
    join((hx, hy, hz - 1), t.heater)
    join((SG_X, 11, z0), t.exchangers[-1])
    join((SG_X, 2, z0), t.exchangers[0])
    join((20, 8, z0), t.relief)
    join((22, 8, z0), t.relief)
    join((23, 8, z0), t.steam_valve)
    join((25, 8, z0), t.steam_valve)
    join((28, 8, z0), (tx - 1, ty, tz))
    join((30, 6, z0), (tx, ty - 1, tz))
    join((31, 3, z0), t.condensers[0])
    join((40, 3, z0), t.condensers[-1])
    join((41, 3, z0), t.feed_tank[-1])
    join((42, 1, z0 + 1), t.feed_tank[0])
    join((41, 1, z0 + 3), t.feed_pump)
    join((39, 1, z0 + 3), t.feed_pump)
    join((32, 4, z0), t.condensers[0])
    join((39, 2, z0), t.condensers[-1])
    for column in t.fills:
        x, _, z = column[0]
        join((x + 1, 13, z), column[-1])
        join((x - 1, 2, z), column[0])
    cx, cy, cz = t.circ_pump
    join((cx, cy, cz - 1), t.circ_pump)
    join((cx, cy, cz + 1), t.circ_pump)


def tower(t):
    cx, cz = t.tower
    for x in range(cx - 13, cx + 14):
        for z in range(cz - 13, cz + 14):
            d = math.hypot(x - cx, z - cz)
            if d < 11.5:
                if d >= 9.5:
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
    for deg in range(0, 360, 45):
        x, z = cx + round(7.6 * math.cos(math.radians(deg))), cz + round(7.6 * math.sin(math.radians(deg)))
        wall_lamp(x, 29, z)


def towers():
    for t in TRAINS:
        tower(t)
    post((TA.tower[0] - 6, 1, -24), 8, "Cooling", "Tower A")
    post((TB.tower[0] + 2, 1, -21), 8, "Cooling", "Tower B")
    for x, z in ((24, -19), (50, -20)):
        box(x, 1, z, x, 9, z, STEEL)
        put(x, 10, z, ROD)


def turbine_hall():
    x0, x1, z0, z1 = HALL
    box(x0, 0, z0, x1, 0, z1, FLOOR)
    walls(x0, 1, z0, x1, 20, z1, SHELL)
    walls(x0, 1, z0, x1, 2, z1, PLINTH)
    walls(x0, 18, z0, x1, 18, z1, BAND)
    box(x0, 20, z0, x1, 20, z1, PLINTH)
    walls(x0, 21, z0, x1, 21, z1, PLINTH)
    for x in range(x0, x1 + 1, 8):
        for z in (z0, z1):
            box(x, 1, z, x, 20, z, PLINTH)
    for z in (z0, -9, 0, 9, z1):
        box(x1, 1, z, x1, 20, z, PLINTH)
    for x in range(x0 + 1, x1, 8):
        box(x + 2, 14, z0, x + 5, 16, z0, WINDOW)
        box(x + 2, 15, z1, x + 5, 16, z1, WINDOW)
    for z in (-13, -5, 3, 11):
        box(x1, 14, z, x1, 16, z + 2, WINDOW)
    for x in range(x0 + 3, x1, 4):
        for z in (z0, z1):
            wall_lamp(x, 3, z)
            wall_lamp(x, 8, z)
            wall_lamp(x, 12, z)
    for z in (-13, -6, 6, 13):
        wall_lamp(x1, 8, z)
        wall_lamp(x1, 12, z)
        wall_lamp(x0, 3, z)
        wall_lamp(x0, 8, z)
        wall_lamp(x0, 12, z)
    box(x0 + 1, DECK, z0 + 1, x1 - 1, DECK, z1 - 1, FLOOR)
    for x in range(22, 56, 8):
        for z in (-12, -4, 4, 12):
            if not any(taken((x, y, z)) for y in range(1, DECK)):
                columns.add((x, z))
                box(x, 1, z, x, DECK - 1, z, STEEL)
    for t in TRAINS:
        stripes(28, t.z0 - 2, 37, t.z0 + 2, DECK)
        stripes(20, t.z0 - 1, 25, t.z0 + 1, DECK)
    stripes(49, -6, 55, 6, DECK)
    carve(25, DECK, z0 + 1, 30, DECK, z0 + 1)
    stairs(30, 1, z0 + 1, -1, 0, 6, "west")
    box(24, 7, z0 + 2, 31, 7, z0 + 2, BARS)
    put(31, 7, z0 + 1, BARS)
    box(x0 + 1, 14, z0 + 1, x1 - 1, 14, z0 + 1, STEEL)
    box(x0 + 1, 14, z1 - 1, x1 - 1, 14, z1 - 1, STEEL)
    for x in (34, 35):
        box(x, 15, z0 + 1, x, 15, z1 - 1, GIRDER_Z)
    box(34, 15, -2, 35, 15, 2, IRON)
    for y in (14, 13, 12):
        attach((34, y, 0), CHAIN)
    attach((34, 11, 0), "minecraft:grindstone[face=ceiling,facing=east]")
    for x in range(22, x1, 6):
        box(x, 19, z0 + 1, x, 19, z1 - 1, GIRDER_Z)
    for x in (24, 36, 48):
        for z in (-9, 9):
            box(x, 21, z - 1, x + 2, 21, z + 1, IRON)
            put(x + 1, 22, z, "minecraft:iron_trapdoor[half=bottom,open=false,facing=north]")
    for x in range(22, x1, 6):
        for z in (-12, -4, 4, 12):
            pendant(x, 18, z, 13)
    for t, tag in ((TA, "A"), (TB, "B")):
        side = 1 if t.z0 < 0 else -1
        facing = "south" if side > 0 else "north"
        sign((t.generator[0], 7, t.z0 + side), facing, "Generator " + tag, "20 kV 50 Hz")
        sign((t.steam_valve[0], 7, t.z0 + side), facing, "Main Steam " + tag, "Stop Valve")
        sign((t.relief[0], 7, t.z0 + side), facing, "Relief", "Valve " + tag)
        post((t.breaker[0], 7, t.z0 + 2 * side), 0 if side > 0 else 8, "Generator " + tag, "Breaker")
        post((34, 1, t.z0 + 2), 0, "Condenser " + tag)
        post((43, 1, t.z0 + 2), 0, "Feedwater", "Tank " + tag)
        post((37, 1, t.z0 + 4), 0, "Feedwater", "Pump " + tag)
        post((29, 1, t.z0 + 4), 0, "Feedwater", "Heater " + tag)
        post((45, 1, t.z0 + 1), 0, "Make-up", "Water " + tag)
        post((t.circ_pump[0] + 1, 1, t.circ_pump[2] + 2), 0, "Circulating", "Water Pump " + tag)
    post((47, 7, 3), 4, "20 kV", "Unit Bus")
    for x in (55, 56):
        for z in (14, 15):
            for y in (7, 8, 9):
                machine((x, y, z), "create:fluid_tank")
    post((54, 7, 13), 12, "Demineralised", "Water")
    put(52, 7, -11, "minecraft:barrel[facing=up]")
    put(53, 7, -11, "minecraft:barrel[facing=up]")
    put(53, 8, -11, "create:andesite_casing")
    put(52, 7, -12, "create:andesite_casing")
    put(54, 7, -10, "create:andesite_casing")
    door(36, 1, z0, "south")
    put(36, 3, z0, LAMP)


def basement_lights():
    x0, x1, z0, z1 = HALL
    for x in range(x0 + 3, 47, 4):
        for z in range(z0 + 3, z1 - 1, 4):
            if (x, DECK, z) in blocks and not taken((x, DECK - 1, z)):
                attach((x, DECK - 1, z), LANTERN)
    for x in range(x0 + 4, 46, 6):
        for z in (-1, 1):
            if not taken((x, 0, z)) or blocks.get((x, 0, z)) == FLOOR:
                put(x, 0, z, LAMP)


def rooms():
    for za, zb, name in ((-16, -2, "switch"), (2, 16, "diesel")):
        edge = zb if name == "switch" else za
        box(47, 1, za, 47, 3, zb, PLINTH)
        box(47, 4, za, 47, 5, zb, BARS)
        box(47, 1, edge, 57, 3, edge, PLINTH)
        box(47, 4, edge, 57, 5, edge, BARS)
    door(50, 1, -2, "north")
    door(52, 1, 2, "south")
    door(58, 1, 0, "east")
    sign((49, 3, -1), "south", "6 kV", "Switchgear", "Room")
    sign((51, 3, 1), "north", "Standby", "Diesel", "Room")
    sign((57, 3, 1), "west", "Switchyard")
    post((52, 1, -3), 8, "6 kV", "Station Bus")
    post((56, 1, -3), 8, "Station", "Supply", "Breaker")
    sign((48, 3, -6), "east", "Station", "Batteries")
    post((51, 1, -9), 12, "Reactor", "Feeder")
    post((53, 1, -9), 12, "Train A", "Feeder")
    post((55, 1, -9), 12, "Train B", "Feeder")
    post((57, 1, -9), 12, "Fuel", "Building", "Feeder")
    sign((50, 3, 16), "north", "Standby", "Diesel", "Engine")
    sign((53, 3, 16), "north", "Standby", "Transformer", "20 / 6 kV")
    sign((55, 3, 16), "north", "Transfer", "Switch")
    for x in (56, 57):
        for z in (12, 13):
            for y in (1, 2):
                machine((x, y, z), "create:fluid_tank")
    post((55, 1, 12), 4, "Fuel Oil")
    x1 = HALL[1]
    box(50, 4, HALL[3], 55, 4, HALL[3], BARS)
    for x in range(50, 57, 3):
        for z in (-12, -6, 6, 12):
            if not taken((x, 5, z)):
                attach((x, 5, z), LANTERN)
    put(x1, 4, -2, LAMP)
    put(x1, 4, 2, LAMP)


def electrics():
    for pos in BUSHINGS:
        put(pos[0], pos[1], pos[2], IRON)
    for pos in BUS20:
        machine(pos, "criticalmass:bus_bar[axis=z]")
    machine(STATION_BREAKER, "criticalmass:circuit_breaker[facing=west,closed=false]")
    for pos in BUS6:
        machine(pos, "criticalmass:bus_bar[axis=x]")
    for pos in FEEDERS:
        machine(pos, "criticalmass:circuit_breaker[facing=south,closed=false]")
    machine(STANDBY_ENGINE, "criticalmass:diesel_engine[facing=east]{Tank:{Fluid:{id:\"criticalmass:fuel_oil\",amount:4000}}}")
    machine(STANDBY_GEN, "criticalmass:generator[axis=x]")
    machine(STANDBY_T, "criticalmass:transformer[facing=south]{ScrollValue:2}")
    machine(TRANSFER, "criticalmass:transfer_switch[facing=north]")
    for pos in (BATTERY, BATTERY_B):
        machine(pos, "criticalmass:station_battery[facing=south]{Charge:3072000.0d}")
    for pos in START_LINE:
        attach(pos, "minecraft:redstone_wire")
    for pos, facing in INSULATORS:
        assert pos not in PIPE_SET and pos not in machines, "insulator in the way at %s" % (pos,)
        back = {"down": (0, 1, 0), "up": (0, -1, 0), "west": (1, 0, 0), "east": (-1, 0, 0), "north": (0, 0, 1), "south": (0, 0, -1)}[facing]
        support = (pos[0] + back[0], pos[1] + back[1], pos[2] + back[2])
        if not taken(support):
            put(support[0], support[1], support[2], IRON)
        if facing == "up":
            for y in range(support[1] - 1, 0, -1):
                if taken((support[0], y, support[2])):
                    break
                put(support[0], y, support[2], STEEL)
        attach(pos, "criticalmass:insulator[facing=%s]" % facing)
    for kind, a, _, b, _ in WIRES:
        assert math.dist(a, b) <= 23.5, "the %s from %s to %s is too long: %.1f" % (kind, a, b, math.dist(a, b))


def switchyard():
    box(59, 0, -10, 70, 0, 1, "minecraft:gravel")
    box(59, 1, -10, 70, 1, -10, BARS)
    box(70, 1, -10, 70, 1, 1, BARS)
    box(60, 1, 1, 70, 1, 1, BARS)
    for z in (-8, 0):
        box(68, 1, z, 68, 7, z, STEEL)
        put(68, 8, z, ROD)
    box(68, 7, -7, 68, 7, -1, GIRDER_Z)
    box(60, 1, -9, 60, 8, -9, STEEL)
    put(60, 9, -9, ROD)
    machine(MAIN_T, "criticalmass:transformer[facing=north]{ScrollValue:0}")
    machine(GRID_BREAKER, "criticalmass:circuit_breaker[facing=north,closed=false]")
    machine(GRID, "criticalmass:grid_connection")
    machine(UNIT_T, "criticalmass:transformer[facing=north]{ScrollValue:2}")
    machine(ARRESTER, "criticalmass:surge_arrester")
    post((62, 1, -1), 8, "Main", "Transformer", "220 / 20 kV")
    post((64, 1, -1), 8, "Grid", "Breaker")
    post((66, 1, -5), 0, "220 kV", "Grid")
    post((63, 1, -8), 0, "Unit", "Transformer", "20 / 6 kV")
    post((61, 1, 0), 8, "Surge", "Arrester")


def corridor():
    box(-12, 0, 17, 18, 0, 21, FLOOR)
    walls(-12, 1, 17, 18, 4, 21, SHELL)
    walls(-12, 1, 17, 18, 1, 21, PLINTH)
    box(-12, 5, 17, 18, 5, 21, PLINTH)
    for x in range(-11, 18):
        put(x, 0, 19, YELLOW if x % 2 == 0 else BLACK)
    for x in range(-10, 18, 3):
        put(x, 5, 19, LAMP)


def aux():
    x0, x1, z0, z1 = AUX
    box(x0, 0, z0 + 1, x1, 0, z1, FLOOR)
    walls(x0, 1, z0, x1, 14, z1, SHELL)
    walls(x0, 1, z0, x1, 2, z1, PLINTH)
    walls(x0, 13, z0, x1, 13, z1, BAND)
    box(x0, 14, z0, x1, 14, z1, PLINTH)
    box(x0, 15, z0 + 1, x1, 15, z1, PLINTH)
    carve(x0 + 1, 15, z0 + 1, x1 - 1, 15, z1 - 1)
    box(x0, 15, z0, x1, 20, z0, SHELL)
    box(x0, 18, z0, x1, 18, z0, BAND)
    box(x0, 20, z0, x1, 20, z0, PLINTH)
    for x in range(HALL[0], x1 + 1, 8):
        box(x, 1, z0, x, 20, z0, PLINTH)
    box(x0 + 1, 6, z0 + 1, x1 - 1, 6, z1 - 1, FLOOR)
    for x in (24, 32):
        box(x, 15, 24, x + 2, 15, 26, IRON)
    box(27, 1, 18, 27, 5, 32, WHITE)
    box(36, 1, 18, 36, 5, 32, WHITE)
    door(27, 1, 22, "east")
    door(36, 1, 22, "east")
    door(18, 1, 19, "east")
    door(41, 1, z1, "north", "left")
    door(42, 1, z1, "north", "right")
    door(40, 1, z0, "north")
    sign((40, 3, z1 + 1), "south", "Critical Mass", "Generating", "Station")
    sign((39, 3, z0 + 1), "south", "Turbine Hall", "Basement")
    box(39, 4, z1 + 1, 44, 4, z1 + 2, "minecraft:smooth_stone_slab[type=bottom]")
    for x in (39, 44):
        box(x, 1, z1 + 2, x, 3, z1 + 2, STEEL)
        put(x, 4, z1, LAMP)
    box(38, 2, z1, 39, 4, z1, WINDOW)
    box(44, 2, z1, 46, 4, z1, WINDOW)
    box(x1, 2, 21, x1, 4, 23, WINDOW)
    box(20, 9, z1, 24, 11, z1, WINDOW)
    box(30, 9, z1, 34, 11, z1, WINDOW)
    box(40, 9, z1, 44, 11, z1, WINDOW)
    box(x1, 9, 28, x1, 11, 31, WINDOW)
    for x in range(19, 27):
        put(x, 0, 19, YELLOW if x % 2 == 0 else BLACK)
    box(29, 1, 32, 34, 1, 32, "minecraft:smooth_stone_slab[type=bottom]")
    for x in (30, 32, 34):
        props.append(((x + 0.5, 1.5, 32.5), "minecraft:armor_stand", "{Tags:[\"%s\"],Rotation:[180f,0f],ArmorItems:[{id:\"criticalmass:protective_boots\",count:1},{id:\"criticalmass:protective_leggings\",count:1},{id:\"criticalmass:protective_suit\",count:1},{id:\"criticalmass:protective_hood\",count:1}]}" % PROP))
    for z in (25, 26, 27, 28, 29):
        for half in ("lower", "upper"):
            attach((28, 1 if half == "lower" else 2, z), "minecraft:iron_door[facing=east,half=%s,hinge=left,open=false]" % half)
            attach((35, 1 if half == "lower" else 2, z), "minecraft:iron_door[facing=west,half=%s,hinge=left,open=false]" % half)
    sign((31, 3, 18), "south", "Change Room", "Suit up before", "the airlock")
    sign((26, 3, 21), "west", "Controlled", "Area")
    sign((37, 3, 21), "east", "Security", "Authorised", "personnel only")
    box(20, 1, 31, 24, 1, 31, "minecraft:polished_andesite")
    put(21, 2, 31, "minecraft:potted_fern")
    put(24, 2, 31, "minecraft:lectern[facing=north]")
    sign((22, 3, 32), "north", "Health", "Physics")
    box(38, 1, 26, 42, 1, 26, "minecraft:polished_andesite")
    for x in (38, 42):
        put(x, 2, 26, "minecraft:potted_fern")
    for z in (19, 20, 21):
        put(46, 1, z, "create:blue_seat")
    for x in (38, 45):
        put(x, 1, 32, "minecraft:polished_andesite")
        put(x, 2, 32, "minecraft:flowering_azalea_leaves[persistent=true]")
    for x in (41, 42):
        for z in (31, 32):
            attach((x, 1, z), "minecraft:gray_carpet")
    carve(47, 6, 26, 47, 6, 31)
    stairs(47, 1, 31, 0, -1, 6, "north")
    box(46, 7, 26, 46, 7, 31, BARS)
    put(47, 7, 32, BARS)
    put(46, 7, 32, BARS)
    for x in range(21, 47, 4):
        for z in (20, 25, 30):
            if not taken((x, 5, z)):
                attach((x, 5, z), LANTERN)


def control_room():
    x0, x1, z0, z1 = AUX
    box(19, 6, 18, 43, 6, 27, PLINTH)
    box(19, 7, SCREEN_Z, MIMIC_X, 13, SCREEN_Z, SC)
    box(MIMIC_X, 7, 18, MIMIC_X, 13, 27, WHITE)
    door(MIMIC_X, 7, 18, "east")
    door(42, 7, z0, "north")
    box(21, 8, z0, 40, 10, z0, SG)
    box(45, 7, SCREEN_Z, 46, 13, SCREEN_Z, WHITE)
    carve(45, 7, SCREEN_Z, 45, 8, SCREEN_Z)
    for (sx0, sy0), (sx1, sy1), _, _ in SCREENS:
        box(sx0 - 1, sy0 - 1, SCREEN_Z, sx1 + 1, sy1 + 1 if sy1 < 13 else 13, SCREEN_Z, STEEL)
    for (sx0, sy0), (sx1, sy1), _, _ in SCREENS:
        for x in range(sx0, sx1 + 1):
            for y in range(sy0, sy1 + 1):
                machine((x, y, SCREEN_Z), "criticalmass:plant_screen[facing=north]")
    for x, y in SCREEN_COGS:
        machine((x, y, SCREEN_Z), "create:cogwheel[axis=z]")
        reserved.add((x, y, SCREEN_Z + 1))
    for (za, ya), (zb, yb), _ in EAST_SCREENS:
        for z in range(za, zb + 1):
            for y in range(ya, yb + 1):
                machine((MIMIC_X, y, z), "criticalmass:plant_screen[facing=west]")
    for y, z in EAST_COGS:
        machine((MIMIC_X, y, z), "create:cogwheel[axis=x]")
        reserved.add((MIMIC_X + 1, y, z))
    for x in sorted(DESKS):
        machine((x, DESK_Y, DESK_Z), "criticalmass:reactor_alloy_dashboard[facing=north,join_cw=%s,join_ccw=%s]" % (str(x + 1 in DESKS).lower(), str(x - 1 in DESKS).lower()))
    for z in sorted(SIDE_DESKS):
        machine((SIDE_X, DESK_Y, z), "criticalmass:reactor_alloy_dashboard[facing=east,join_cw=%s,join_ccw=%s]" % (str(z + 1 in SIDE_DESKS).lower(), str(z - 1 in SIDE_DESKS).lower()))
    sign((SIDE_X, 9, 20), "east", "6 kV", "Feeders")
    for pos, facing, mounted in SIRENS:
        assert not taken(pos), "a siren is in the way at %s" % (pos,)
        attach(pos, "criticalmass:meltdown_siren[facing=%s,wall=%s]" % (facing, str(mounted).lower()))
    for x in (20, 25, 31, 37, 42):
        put(x, 7, 22, "create:gray_seat")
    box(19, 6, 22, 43, 6, 23, CLEAN)
    box(30, 7, 19, 32, 7, 19, IRON)
    put(31, 8, 19, "minecraft:lectern[facing=south]")
    for x in range(21, 43, 3):
        for z in (19, 22, 26):
            put(x, 14, z, LAMP)
    for x in (22, 28, 34, 40, 46):
        put(x, 14, 30, LAMP)
    for z in (20, 24):
        put(46, 14, z, LAMP)
    for x in (20, 21, 22, 23, 24):
        for z in (31, 32):
            box(x, 7, z, x, 9, z, IRON)
    for x in (20, 22, 24):
        attach((x, 8, 30), "minecraft:lever[face=wall,facing=north,powered=%s]" % ("true" if x == 22 else "false"))
    sign((43, 9, 19), "west", "Control Room")
    sign((45, 9, 29), "south", "Relay Room")
    sign((41, 9, z0 + 1), "south", "Turbine Deck")
    sign((47, 9, 18), "south", "Stairs Down")


def fuel_building():
    x0, x1, z0, z1 = -12, 11, 21, 34
    box(x0, 0, z0, x1, 0, z1, CLEAN)
    walls(x0, 1, z0, x1, 8, z1, SHELL)
    walls(x0, 1, z0, x1, 2, z1, PLINTH)
    walls(x0, 8, z0, x1, 8, z1, BAND)
    box(x0, 9, z0, x1, 9, z1, PLINTH)
    walls(x0 + 1, 1, z0 + 1, x1 - 1, 8, z1 - 1, SC)
    carve(-2, 1, 21, -2, 2, 22)
    door(-2, 1, 21, "north")
    for x in (-8, -3, 2, 7):
        for z in (25, 30):
            put(x, 9, z, LAMP)
    box(x0, 10, z0, x1, 10, z1, PLINTH)
    carve(x0 + 1, 10, z0 + 1, x1 - 1, 10, z1 - 1)
    box(-10, -2, 24, -4, -2, 31, SC)
    walls(-10, -1, 24, -4, 0, 31, SC)
    box(-9, -1, 25, -5, 0, 30, WATER)
    for p in RACKS:
        machine(p, "criticalmass:spent_fuel_rack")
    walls(-10, 1, 24, -4, 1, 31, BARS)
    carve(-4, 1, 27, -4, 1, 28)
    for x in range(-11, -2):
        for z in (23, 32):
            put(x, 0, z, YELLOW if x % 2 == 0 else BLACK)
    for z in range(23, 33):
        put(-3, 0, z, YELLOW if z % 2 == 0 else BLACK)
    box(1, 0, 24, 5, 0, 27, PLINTH)
    for x in (1, 3, 5):
        machine((x, 1, 25), "criticalmass:shielded_cask")
    sign((3, 2, 23), "south", "Dry Cask", "Storage")
    for y in (1, 2, 3):
        machine((8, y, 30), "criticalmass:centrifuge")
    machine((8, 4, 30), "create:creative_motor[facing=down]{ScrollValue:128}")
    machine((7, 1, 30), "criticalmass:open_waste_canister")
    put(9, 1, 30, STEEL)
    put(9, 1, 31, STEEL)
    put(8, 1, 31, STEEL)
    sign((7, 2, 32), "north", "Fuel", "Processing")
    for x in (2, 4):
        machine((x, -1, 31), "criticalmass:sealed_waste_canister")
        machine((x, 0, 31), "criticalmass:shielded_hatch[half=top,open=false,facing=north]")
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, 0, 31 + dz) in blocks:
                put(x + dx, 0, 31 + dz, YELLOW)
        put(x, -2, 31, SC)
    sign((3, 2, 32), "north", "Waste Vault")
    sign((-7, 2, 23), "south", "Spent Fuel", "Pool")
    for x in range(-9, 9):
        put(x, 7, 27, GIRDER_X)
    box(-1, 7, 26, 0, 7, 28, IRON)
    for y in (6, 5):
        attach((0, y, 27), CHAIN)
    attach((0, 4, 27), "minecraft:grindstone[face=ceiling,facing=north]")
    for x in (-7, 3, 8):
        put(x, 3, 22, LAMP)
    for x in (-7, -1, 5):
        put(x, 3, 33, LAMP)
    for z in (25, 30):
        put(10, 3, z, LAMP)
    for x in (-8, -4, 0, 4, 8):
        for z in (24, 30):
            pendant(x, 8, z, 5)
    box(-12, 3, 26, -12, 5, 29, WINDOW)
    box(-11, 3, 26, -11, 5, 29, SG)


def reprocessing():
    x0, x1, z0, z1 = -12, 11, 34, 43
    box(x0, 0, z0 + 1, x1, 0, z1, CLEAN)
    for b, ya, yb in ((SHELL, 1, 7), (PLINTH, 1, 2), (BAND, 7, 7)):
        box(x0, ya, z1, x1, yb, z1, b)
        box(x0, ya, z0 + 1, x0, yb, z1, b)
        box(x1, ya, z0 + 1, x1, yb, z1, b)
    box(x0, 8, z0 + 1, x1, 8, z1, PLINTH)
    carve(-1, 1, z0 - 1, -1, 2, z0)
    door(-1, 1, z0, "south")
    door(x1, 1, 36, "west")
    sign((0, 3, z0 - 2), "north", "Reprocessing")
    sign((x1 + 1, 3, 37), "east", "Reprocessing", "Hall")
    box(x0, 3, 35, x0, 4, 36, WINDOW)
    box(x0 + 1, 1, CELL_Z, x1 - 1, 7, CELL_Z, SC)
    box(-8, 2, CELL_Z, 9, 3, CELL_Z, SG)
    carve(PRODUCT[0], PRODUCT[1] + 1, CELL_Z, PRODUCT[0], PRODUCT[1] + 1, CELL_Z)
    door(9, 1, CELL_Z, "north")
    for x in range(x0 + 1, x1):
        put(x, 0, CELL_Z - 1, YELLOW if x % 2 == 0 else BLACK)
    for x in (-8, -3, 2, 7):
        for z in (36, 41):
            put(x, 8, z, LAMP)
    machine(FEED_PORT, "create:andesite_funnel[facing=north,extracting=false]")
    for pos in SAWS:
        machine(pos, "create:mechanical_saw[facing=up,axis_along_first=false]")
    for pos in SAW_DRIVES:
        machine(pos, "create:encased_chain_drive[axis=z]")
    machine(SHEAR_DEPOT, "create:depot")
    machine(DISSOLVER_BURNER, "create:blaze_burner[blaze=kindled]{isCreative:1b}")
    machine(DISSOLVER, "create:basin[facing=north]")
    machine(DISSOLVER_MIXER, "create:mechanical_mixer")
    machine(DISSOLVER_COG, "create:cogwheel[axis=y]")
    for target in (SHEAR_DEPOT, DISSOLVER):
        assert math.dist(DISSOLVER_ARM, target) < 5, "the dissolver arm cannot reach %s" % (target,)
    machine(DISSOLVER_ARM, "create:mechanical_arm[ceiling=true]{InteractionPoints:[{Type:\"create:depot\",Pos:%s,Mode:\"TAKE\"},{Type:\"create:basin\",Pos:%s,Mode:\"DEPOSIT\"}]}"
            % (reach(DISSOLVER_ARM, SHEAR_DEPOT), reach(DISSOLVER_ARM, DISSOLVER)))
    machine(PRODUCT, "create:basin")
    machine(WASTE_PUMP, "create:mechanical_pump[facing=west]")
    machine(WASTE_PIPE, "create:fluid_pipe")
    machine(WATER_PUMP, "create:mechanical_pump[facing=east]")
    machine(PUMP_COG, "create:cogwheel[axis=x]")
    for pos in WASTE_TANK + WATER_TANK:
        machine(pos, "create:fluid_tank")
    for xa, xb in ((-3, -1), (1, 3)):
        box(xa, 5, CELL_Z + 1, xb, 5, CELL_Z + 1, GIRDER_X)
    for i, pos in enumerate(BENCH_BELT):
        machine(pos, "create:belt[facing=west,part=%s,slope=horizontal]" % ("start" if i == 0 else "end" if i == len(BENCH_BELT) - 1 else "middle"))
    machine(BENCH_DEPOT, "create:depot")
    machine(BENCH_SPOUT, "create:spout")
    machine(BENCH_DEPLOYER, "create:deployer[facing=down,axis_along_first=true]{Mode:\"USE\",Inventory:[{Slot:0b,id:\"criticalmass:sealing_gasket\",count:64}]}")
    machine(BENCH_PRESS, "create:mechanical_press[facing=east]")
    machine(BENCH_GEARBOX, "create:gearbox[axis=y]")
    for pos in BENCH_COGS:
        machine(pos, "create:cogwheel[axis=z]")
    machine(CANNING_PUMP, "create:mechanical_pump[facing=west]")
    machine(CANNING_PIPE, "create:fluid_pipe")
    machine(CANNING_COG, "create:cogwheel[axis=x]")
    for pos in (BENCH_MOTOR, CANNING_MOTOR):
        stand(pos)
    reserved.update([SAW_MOTOR, DISSOLVER_MOTOR, PUMP_MOTOR, BENCH_MOTOR, CANNING_MOTOR])
    sign((-7, 4, BENCH_Z), "north", "Canning Bench", "Open canisters", "on the belt")
    for x, y in ((-5, 2), (-5, 3), (-4, 1), (-4, 2), (-4, 3), (-3, 1), (-3, 2), (-3, 3)):
        assert not taken((x, y, BENCH_Z)), "the waste tank's shield pier is in the way at %d %d" % (x, y)
        put(x, y, BENCH_Z, SC)
    machine((9, 1, 35), "criticalmass:shielded_cask")
    sign((8, 2, 35), "south", "Transfer", "Cask")
    sign((6, 4, CELL_Z - 1), "north", "Feed Port", "Cooled spent", "fuel only")
    sign((3, 4, CELL_Z - 1), "north", "Shear Line")
    sign((-1, 4, CELL_Z - 1), "north", "Dissolver", "Product in the", "basin below")
    sign((-4, 4, CELL_Z - 1), "north", "Liquid", "Waste")
    sign((8, 4, CELL_Z - 1), "north", "Hot Cell", "Survey before", "entry")
    box(16, 1, CELL_Z, 16, 9, CELL_Z, STEEL)
    put(16, 10, CELL_Z, ROD)


def well(pit, along_x):
    x, _, z = pit
    dx, dz = (1, 0) if along_x else (0, 1)
    box(x - 1 - dx, -2, z - 1 - dz, x + 1 + dx, -1, z + 1 + dz, PLINTH)
    for i in (-1, 0, 1):
        put(x + dx * i, -1, z + dz * i, WATER)
    for i in (-1, 1):
        if not taken((x + dx * i, 1, z + dz * i)):
            put(x + dx * i, 0, z + dz * i, GRATE)
    machine(pit, "create:fluid_pipe")


def water_supply():
    supply = WELL_PIPES + CELL_WELL_PIPES + [WELL, CELL_WELL] + [cell for t in TRAINS for cell in t.makeup_intake + [t.makeup_pipe]]
    for pos in WELL_PIPES + CELL_WELL_PIPES + [WELL_PUMP, WELL_COG, WELL_MOTOR, CELL_WELL_PUMP]:
        assert pos not in machines and pos not in PIPE_SET and pos not in reserved and not any(pos == at for at, _ in late), "the water supply is in the way at %s" % (pos,)
    for x, y, z in supply:
        for q in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)):
            assert q not in PIPE_SET, "the water supply pipe at %s would join the loop pipe at %s" % ((x, y, z), q)
    reserved.add(WELL_MOTOR)
    well(WELL, False)
    for x, y, z in WELL_MAIN:
        box(x - 1, -2, z, x + 1, -1, z, PLINTH)
    for pos in WELL_PIPES:
        machine(pos, "create:fluid_pipe")
    for x, y, z in WELL_MAIN:
        if (x, 0, z) not in machines and not taken((x, 1, z)):
            put(x, 0, z, GRATE)
    machine(WELL_PUMP, "create:mechanical_pump[facing=east]")
    machine(WELL_COG, "create:cogwheel[axis=x]")
    post((42, 1, -2), 4, "Well Pump", "Make-up", "Water")
    well(CELL_WELL, True)
    for pos in CELL_WELL_PIPES:
        machine(pos, "create:fluid_pipe")
    machine(CELL_WELL_PUMP, "create:mechanical_pump[facing=west]")
    sign((1, 2, CELL_Z + 6), "south", "Process", "Water Well")


def stack():
    for y in range(1, 34):
        put(15, y, 29, "minecraft:red_concrete" if (y // 4) % 2 == 0 else WHITE)
    put(15, 34, 29, LAMP)
    box(14, 1, 28, 16, 1, 30, PLINTH)
    put(15, 1, 29, "minecraft:red_concrete")


def wiring():
    ends = set()
    for _, a, _, b, _ in WIRES:
        ends.update((a, b))
    allowed = set(BUSHINGS)
    for cell in SLOTS:
        blocks.pop(cell, None)
    for kind, a, _, b, _ in WIRES:
        steps = max(1, int(math.dist(a, b) * 4))
        sag = min(1.5, math.dist(a, b) * 0.05)
        for i in range(steps + 1):
            f = i / steps
            at = tuple(a[k] + (b[k] - a[k]) * f - (sag * 4 * f * (1 - f) if k == 1 else 0) for k in range(3))
            cell = tuple(math.floor(v + 0.5) for v in at)
            if cell in ends or cell in allowed or min(math.dist(at, a), math.dist(at, b)) < 1.3:
                continue
            assert cell not in machines and cell not in PIPE_SET and cell not in reserved, "the %s from %s to %s runs through %s" % (kind, a, b, cell)
            if (cell[0], cell[2]) in columns and cell[1] < DECK:
                columns.discard((cell[0], cell[2]))
                carve(cell[0], 1, cell[2], cell[0], DECK - 1, cell[2])
            if cell in blocks:
                assert blocks[cell] == BARS, "the %s from %s to %s runs through %s at %s" % (kind, a, b, blocks[cell], cell)
                del blocks[cell]
            for entry in [e for e in late if e[0] == cell]:
                assert entry[1] in (LANTERN, CHAIN), "the %s from %s to %s runs through %s at %s" % (kind, a, b, entry[1], cell)
                late.remove(entry)


BEHIND = {"south": (0, -1), "north": (0, 1), "east": (-1, 0), "west": (1, 0)}


def monitors():
    for pos, facing, tenths in ((-7, 3, 23), "south", 10), ((3, 3, 23), "south", 10), ((3, 3, 32), "north", 10), ((1, 3, 18), "south", 10), ((8, 5, CELL_Z - 1), "north", 20), ((-7, 5, BENCH_Z), "north", 20):
        dx, dz = BEHIND[facing]
        assert not taken(pos), "a monitor is in the way at %s" % (pos,)
        assert (pos[0] + dx, pos[1], pos[2] + dz) in blocks, "no wall behind the monitor at %s" % (pos,)
        attach(pos, "criticalmass:area_radiation_monitor[facing=%s,alarm=false]{ScrollValue:%d}" % (facing, tenths))


def validate():
    owner = {}
    for index, loop in enumerate(LOOPS):
        for cell in loop:
            assert cell not in owner, "two pipes at %s" % (cell,)
            assert cell not in machines, "pipe inside a machine at %s" % (cell,)
            owner[cell] = index
    joined = {}
    for pipe, _ in ports:
        joined[pipe] = joined.get(pipe, 0) + 1
    faces = {}
    for t in TRAINS:
        for x, y, z in t.exchangers:
            faces.update({(x, y, z + 1): (x, y, z), (x, y, z - 1): (x, y, z)})
        for x, y, z in t.condensers:
            faces.update({(x, y + 1, z): (x, y, z), (x, y - 1, z): (x, y, z)})
        for column in t.fills:
            for x, y, z in column:
                for q in ((x + 1, y, z), (x - 1, y, z), (x, y, z + 1), (x, y, z - 1)):
                    faces[q] = (x, y, z)
        x, y, z = t.heater
        for q in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)):
            faces[q] = t.heater
    for pos, b in machines.items():
        if "fluid_tank" in b:
            x, y, z = pos
            for q in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)):
                faces[q] = pos
    for cell, target in faces.items():
        assert cell not in owner or (cell, target) in ports, "pipe %s would join %s by accident" % (cell, target)
    for cell, index in owner.items():
        x, y, z = cell
        near = [q for q in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)) if q in owner]
        for q in near:
            assert owner[q] == index, "pipes of two loops touch at %s and %s" % (cell, q)
        links = len(near) + joined.get(cell, 0)
        assert links == 2, "pipe %s has %d connections: %s" % (cell, links, near)
    for pos in reserved:
        assert pos not in machines and pos not in PIPE_SET, "a part set at the start is in the way at %s" % (pos,)


def design():
    yard()
    corridor()
    aux()
    fuel_building()
    reprocessing()
    turbine_hall()
    containment()
    towers()
    stack()
    control_room()
    vessel()
    for loop in LOOPS_PRIMARY:
        primary(loop)
    for t in TRAINS:
        train(t)
    rooms()
    water_supply()
    switchyard()
    electrics()
    basement_lights()
    for t in TRAINS:
        x, y, z = t.turbine
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    blocks.pop((x + dx, y + dy, z + dz), None)
    for p in list(PIPES) + list(reserved):
        blocks.pop(p, None)
    monitors()
    wiring()
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
    for b in sorted(groups, key=lambda b: (b == WATER, b)):
        for x, y, z, xx, yy, zz in cuboids(groups[b]):
            cmds.append(setblock(w, (x, y, z), b) if (x, y, z) == (xx, yy, zz) else "fill %s %s %s" % (w(x, y, z), w(xx, yy, zz), b))
    cmds += [setblock(w, pos, b) for pos, b in machines.items()]
    cmds += [setblock(w, t.turbine, "criticalmass:steam_turbine[facing=east]{ScrollValue:64}") for t in TRAINS]
    cmds += [setblock(w, p, PIPE) for p in PIPES]
    cmds += [setblock(w, p, AIR) for p in PIPES]
    cmds += [setblock(w, p, PIPE) for p in PIPES]
    cmds += [setblock(w, pos, b) for pos, b in late]
    return cmds


def summon_props(origin):
    return ["summon %s %.1f %.1f %.1f %s" % (kind, origin[0] + pos[0], origin[1] + pos[1] - 1, origin[2] + pos[2], nbt) for pos, kind, nbt in props]


def start(w):
    cmds = [merge(w, CONTROLLER, "Coolant", "criticalmass:primary_coolant", 8000, 8000 * 80.0), tank(w, ECCS_TANK, "criticalmass:primary_coolant", 4000)]
    for t in TRAINS:
        cmds += [merge(w, t.turbine, "Coolant", "minecraft:water", 1000, 1000 * 80.0),
                 tank(w, t.feed_tank[0], "minecraft:water", 16000, 16000 * 80.0)]
        cmds += [merge(w, p, "Secondary", "minecraft:water", 1000, 1000 * 80.0) for p in t.exchangers]
        cmds += [merge(w, t.heater, key, "minecraft:water", 1000, 1000 * 80.0) for key in ("Primary", "Secondary")]
        cmds += ["data merge block %s {TankContent:{Fluid:{id:\"minecraft:water\",amount:8000}}}" % w(*t.fill_tank[0]),
                 setblock(w, t.fill_motor, "criticalmass:electric_motor[facing=east]{ScrollValue:%d}" % SPEEDS["fill"])]
        for p in t.condensers:
            cmds += [merge(w, p, "Primary", "minecraft:water", 1000, 1000 * 80.0), merge(w, p, "Secondary", "minecraft:water", 1000, 0)]
        cmds += [merge(w, p, "Buffer", "minecraft:water", 500, 0) for p in t.fill]
        valve = t.steam_valve
        cmds += [setblock(w, (valve[0], valve[1] + 1, valve[2]), AIR), setblock(w, (valve[0], valve[1] + 1, valve[2]), "create:copper_valve_handle[facing=up]"),
                 setblock(w, t.feed_motor, "criticalmass:electric_motor[facing=down]{ScrollValue:%d}" % SPEEDS["feed"]),
                 setblock(w, t.circ_motor, "criticalmass:electric_motor[facing=up]{ScrollValue:%d}" % SPEEDS["circ"]),
                 setblock(w, t.makeup_motor, "criticalmass:electric_motor[facing=north]{ScrollValue:%d}" % SPEEDS["makeup"])]
    cmds += [merge(w, p, "Buffer", "minecraft:water", 4000, 0) for p in RACKS + REFUEL_RACKS]
    cmds += [setblock(w, WELL_MOTOR, "criticalmass:electric_motor[facing=east]{ScrollValue:%d}" % SPEEDS["well"]),
             setblock(w, REFUEL_MOTOR, "criticalmass:electric_motor[facing=down]{ScrollValue:%d}" % SPEEDS["refuel"]),
             "data merge block %s {TankContent:{Fluid:{id:\"minecraft:water\",amount:16000}}}" % w(*WATER_TANK[0]),
             setblock(w, SAW_MOTOR, "criticalmass:electric_motor[facing=north]{ScrollValue:%d}" % SPEEDS["shear"]),
             setblock(w, DISSOLVER_MOTOR, "criticalmass:electric_motor[facing=down]{ScrollValue:%d}" % SPEEDS["dissolver"]),
             setblock(w, PUMP_MOTOR, "criticalmass:electric_motor[facing=east]{ScrollValue:%d}" % SPEEDS["process"]),
             setblock(w, BENCH_MOTOR, "criticalmass:electric_motor[facing=north]{ScrollValue:%d}" % SPEEDS["bench"]),
             setblock(w, CANNING_MOTOR, "criticalmass:electric_motor[facing=west]{ScrollValue:%d}" % SPEEDS["canning"])]
    for _, (sx1, sy1), target, turbine in SCREENS:
        cmds += bind(w, (sx1, sy1, SCREEN_Z), target, "Reactor")
        if turbine:
            cmds += bind(w, (sx1, sy1, SCREEN_Z), turbine, "Turbine")
    for (za, _), (_, yb), target in EAST_SCREENS:
        cmds += bind(w, (MIMIC_X, yb, za), target, "Reactor")
    cmds += [setblock(w, (x, y, SCREEN_Z + 1), "create:creative_motor[facing=north]") for x, y in SCREEN_COGS]
    cmds += [setblock(w, (MIMIC_X + 1, y, z), "create:creative_motor[facing=west]") for y, z in EAST_COGS]
    cmds += ["criticalmass fuel %s fill" % w(*CONTROLLER)]
    for loop in LOOPS_PRIMARY:
        valve = loop.valve
        cmds += [setblock(w, (valve[0] - 1, valve[1], valve[2]), AIR),
                 setblock(w, (valve[0] + 1, valve[1], valve[2]), "create:copper_valve_handle[facing=east]"),
                 setblock(w, loop.motor, "criticalmass:electric_motor[facing=up]{ScrollValue:%d}" % SPEEDS["primary"])]
    cmds += [setblock(w, pos, "criticalmass:control_rod_drive[facing=%s]{Bank:%d,ScrollValue:%d}" % (facing, BANKS[pos[0]], BANKS[pos[0]])) for pos, facing in DRIVES]
    cmds += [setblock(w, pos, "create:cogwheel[axis=y]") for pos in DRIVE_COGS]
    cmds += [setblock(w, DRIVE_MOTOR, "criticalmass:electric_motor[facing=down]{ScrollValue:%d}" % SPEEDS["drive"]),
             setblock(w, (SERVICE[0], SERVICE[1] + 1, SERVICE[2]), "create:copper_valve_handle[facing=up]"),
             "data merge block %s {GaugeFace:\"west\"}" % w(*PRESSURIZER)]
    for x, parts in sorted(DESKS.items()):
        for slot, (_, kind, target, mode, color, label) in enumerate(parts):
            cmds.append("criticalmass desk %s %d %s %s %d %d %s" % (w(x, DESK_Y, DESK_Z), slot, kind, w(*target), mode, color, label))
    for z, parts in sorted(SIDE_DESKS.items()):
        for slot, (_, kind, target, mode, color, label) in enumerate(parts):
            cmds.append("criticalmass desk %s %d %s %s %d %d %s" % (w(SIDE_X, DESK_Y, z), slot, kind, w(*target), mode, color, label))
    for pos, _, _ in SIRENS:
        cmds += bind(w, pos, CONTROLLER, "Reactor")
    cmds += ["criticalmass wire %s %d %s %d %s" % (w(*a), ta, w(*b), tb, kind) for kind, a, ta, b, tb in WIRES]
    cmds += ["criticalmass breaker %s close" % w(*pos) for pos in [GRID_BREAKER, STATION_BREAKER] + FEEDERS if pos != FEED_FUEL]
    return cmds


def top_up(w):
    return [merge(w, CONTROLLER, "Coolant", "criticalmass:primary_coolant", 8000, 8000 * 80.0)]


def stage(w, index):
    power, coolant = RAMP[index]
    return ["criticalmass steady %s %.2f %d" % (w(*CONTROLLER), power, coolant)]


def sync(w):
    cmds = []
    for x, parts in sorted(DESKS.items()):
        for slot, (name, kind, target, mode, color, label) in enumerate(parts):
            if name == "sync":
                cmds.append("criticalmass desk %s %d %s %s %d %d %s" % (w(x, DESK_Y, DESK_Z), slot, kind, w(*target), mode, color, label))
    return cmds + ["criticalmass breaker %s close" % w(*pos) for pos in GEN_BREAKERS]


def load(w, share=RUN_LOAD):
    return ["criticalmass generator %s %.2f" % (w(*pos), share) for pos in GENERATORS]


def hold(w):
    return stage(w, len(RAMP) - 1)


def sequence(w):
    return [("run", stage(w, 0), 400), ("raise", stage(w, 1), 300), ("sync", sync(w), 40), ("load", load(w, PART_LOAD), 100),
            ("full", stage(w, 2), 300), ("power", load(w, RUN_LOAD), 300)]


def write(root):
    folder = os.path.join(root, "data", NS, "function", "facility")
    os.makedirs(folder, exist_ok=True)
    with open(os.path.join(root, "pack.mcmeta"), "w", newline="\n") as f:
        json.dump({"pack": {"pack_format": 48, "description": "Builds a running Critical Mass generating station"}}, f, indent=1)
    w = relative
    marker = "@e[type=minecraft:marker,tag=%s,limit=1]" % TAG
    (x0, z0), (x1, z1) = AREA
    span = "~%d ~%d ~%d ~%d" % (x0, z0, x1, z1)
    placed = place(w) + ["summon %s ~%.1f ~%.1f ~%.1f %s" % (kind, pos[0], pos[1] - 1, pos[2], nbt) for pos, kind, nbt in props]
    placed += ["schedule function %s:facility/start 100t replace" % NS]
    started = start(w) + ["schedule function %s:facility/fill 100t replace" % NS]
    filled = top_up(w) + ["schedule function %s:facility/ready 100t replace" % NS]
    ready = top_up(w) + ["forceload remove %s" % span,
                         "tellraw @a {\"text\":\"Station built: fuelled, on grid power with its pumps running, rods in. Start it from the control room with the RODS OUT button and watch the period, or run the facility/run function to bring it to power.\",\"color\":\"green\"}"]
    steps = sequence(w)
    total = sum(wait for _, _, wait in steps[:-1])
    files = {
        "build": ["kill @e[type=minecraft:marker,tag=%s]" % TAG,
                  "summon minecraft:marker ~%d ~ ~%d {Tags:[\"%s\"]}" % (OFFSET[0], OFFSET[1], TAG),
                  "execute at %s run forceload add %s" % (marker, span),
                  "schedule function %s:facility/place 20t replace" % NS,
                  "tellraw @s {\"text\":\"Building the station. It is filled and fuelled in 16 seconds.\",\"color\":\"yellow\"}"],
        "place_here": placed, "start_here": started, "fill_here": filled, "ready_here": ready,
    }
    for index, (name, cmds, wait) in enumerate(steps):
        body = list(cmds)
        if index + 1 < len(steps):
            body.append("schedule function %s:facility/%s %dt replace" % (NS, steps[index + 1][0], wait))
        if index == 0:
            body.append("tellraw @a {\"text\":\"Reactor set to 30%% power with equilibrium xenon. It is raised in three steps, the generators go on the grid on the way, and the station is at 90%% power in %d seconds. Hold the reactor steady with the rod buttons in the control room.\",\"color\":\"green\"}" % (total // 20))
        files[name + "_here"] = body
    for name in [n[:-5] for n in list(files) if n.endswith("_here")]:
        files[name] = ["execute as %s at @s run function %s:facility/%s_here" % (marker, NS, name)]
    for name, cmds in files.items():
        with open(os.path.join(folder, name + ".mcfunction"), "w", newline="\n") as f:
            f.write("\n".join(cmds) + "\n")


design()

if __name__ == "__main__":
    for target in sys.argv[1:]:
        write(target)
        print("wrote", target)
    print(len(blocks), "blocks,", len(machines), "machines,", len(PIPES), "pipes,", len(place(relative)), "commands,", len(WIRES), "wires,", len(DESKS) + len(SIDE_DESKS), "desks")
