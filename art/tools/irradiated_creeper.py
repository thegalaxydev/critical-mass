import base64
import io
import json
import math
import os
import random
import shutil
import sys
import uuid

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass")
BLOCKBENCH = os.path.join(ROOT, "art", "blockbench")
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "criticalmass", "client", "entity")
NAME = "irradiated_creeper"
SIZE = 64
NEEDED = ("body", "head", "jaw", "second_head", "spare_leg", "flap", "belly", "hump", "boil_a", "boil_b", "boil_c", "eye_growth", "leg_front_right", "leg_front_left", "leg_hind_right", "leg_hind_left")


def part(name, pivot, turn=(0, 0, 0), cubes=(), children=()):
    return {"name": name, "pivot": pivot, "turn": turn, "cubes": list(cubes), "children": list(children)}


def cube(name, uv, origin, size):
    return {"name": name, "uv": uv, "origin": origin, "size": size, "inflate": 0}


MODEL = [
    part("body", (0, 18, 0), (8, 0, 4), [cube("torso", (16, 16), (-4, -12, -2), (8, 12, 4))], [
        part("belly", (1, -4, -2), (0, 0, 3), [cube("belly", (0, 36), (-4, -3, -3), (7, 6, 3))], [
            part("boil_c", (1, 0, -3), (0, 0, 0), [cube("boil_c", (58, 10), (-1, -1, -1), (2, 2, 1))]),
        ]),
        part("hump", (-2, -10, 2), (-12, 0, -10), [cube("hump", (20, 36), (-3, -3, 0), (6, 5, 3))], [
            part("boil_a", (-1, -3, 1.5), (0, 0, 0), [cube("boil_a", (52, 4), (-1, -2, -1), (2, 2, 2))]),
            part("spur_a", (2, -3, 2), (-25, 0, 28), [cube("spur", (24, 45), (-1, -3, -1), (1, 3, 1))]),
            part("spur_b", (-3, -2, 2.5), (-40, 0, -24), [cube("spur", (24, 45), (0, -3, -1), (1, 3, 1))]),
        ]),
        part("boil_b", (4, -7, 0), (0, 0, 0), [cube("boil_b", (48, 10), (0, -2, -1), (2, 3, 3))]),
        part("ribs", (-4, -8, 0), (0, 0, 0), [cube("rib_top", (0, 45), (-1, -2, -2), (1, 1, 4)), cube("rib_mid", (0, 45), (-1, 0, -2), (1, 1, 4)), cube("rib_low", (0, 45), (-1, 2, -2), (1, 1, 4))]),
        part("spare_leg", (-4, -3, 1), (-10, 0, 25), [cube("spare_leg", (52, 27), (-3, 0, -2), (3, 5, 3))]),
        part("second_head", (4, -12, 0), (0, -20, 30), [cube("second_head", (32, 0), (-1, -5, -3), (5, 5, 5))]),
        part("head", (0, -12, 0), (-6, 0, -10), [cube("head", (0, 0), (-4, -8, -4), (8, 8, 8))], [
            part("jaw", (0, -0.5, -1), (18, 0, 0), [cube("jaw", (40, 36), (-3, 0, -3), (6, 2, 3))]),
            part("eye_growth", (2, -5, -4), (0, 0, 0), [cube("eye_growth", (52, 0), (-1, -1, -1), (3, 3, 1))]),
            part("scalp", (-2, -8, 2), (6, 0, -8), [cube("scalp", (32, 10), (-2, -2, -2), (4, 2, 4))], [
                part("spur_c", (1, -2, 1), (-30, 0, 16), [cube("spur", (24, 45), (-1, -3, 0), (1, 3, 1))]),
            ]),
            part("flap", (-4, -1, -1), (0, 0, 6), [cube("flap", (18, 45), (-1, 0, -1), (1, 4, 2))]),
        ]),
    ]),
    part("leg_front_right", (-2, 18, -4), (0, 16, 0), [cube("club_leg", (40, 16), (-3, 0, -3), (5, 6, 5))]),
    part("leg_front_left", (2, 18, -4), (0, -12, 0), [cube("front_leg", (0, 16), (-2, 0, -2), (4, 6, 4))]),
    part("leg_hind_right", (-2, 18, 4), (0, -10, 0), [cube("withered_leg", (40, 27), (-2, 0, -1), (3, 6, 3))]),
    part("leg_hind_left", (2, 18, 4), (0, 14, 0), [cube("hind_leg", (0, 26), (-2, 0, -2), (4, 6, 4)), cube("leg_growth", (10, 45), (2, 1, -1), (2, 2, 2))]),
]


def walk(parts, at=(0, 0, 0)):
    for p in parts:
        here = tuple(a + b for a, b in zip(at, p["pivot"]))
        yield p, here
        yield from walk(p["children"], here)


def regions(c):
    u, v = c["uv"]
    w, h, d = c["size"]
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "west": (u, v + d, d, h), "north": (u + d, v + d, w, h), "east": (u + d + w, v + d, d, h), "south": (u + d + w + d, v + d, w, h)}


def find(name):
    for p, _ in walk(MODEL):
        for c in p["cubes"]:
            if c["name"] == name:
                return c
    raise KeyError(name)


SKIN = [(0x26, 0x3d, 0x27), (0x35, 0x52, 0x2c), (0x49, 0x6b, 0x33), (0x60, 0x86, 0x3d), (0x7b, 0x9f, 0x4b), (0x97, 0xb5, 0x5e), (0xb3, 0xc8, 0x7a), (0xcc, 0xd8, 0x9b)]
DEAD = [(0x2c, 0x2d, 0x22), (0x40, 0x40, 0x2f), (0x57, 0x55, 0x3d), (0x6f, 0x6c, 0x4e), (0x88, 0x85, 0x62), (0xa0, 0x9d, 0x78)]
PALE = [(0x6f, 0x8a, 0x52), (0x8c, 0xa3, 0x68), (0xa8, 0xba, 0x80), (0xc1, 0xcd, 0x9b), (0xd6, 0xdc, 0xb7), (0xe4, 0xe7, 0xd0)]
FLESH = [(0x24, 0x0e, 0x12), (0x45, 0x19, 0x1e), (0x69, 0x29, 0x2b), (0x8c, 0x3f, 0x39), (0xae, 0x5f, 0x4e)]
PUS = [(0x6a, 0x8a, 0x1c), (0x9c, 0xc0, 0x2a), (0xcb, 0xe4, 0x48), (0xee, 0xfa, 0x9a)]
BONE = [(0x8f, 0x88, 0x6e), (0xc9, 0xc1, 0xa2), (0xe8, 0xe2, 0xc8)]
BLACK = (0x0a, 0x0c, 0x07)
SOCKET = (0x1b, 0x2e, 0x1b)
SIDES = ("west", "north", "east", "south")


class Painter:
    def __init__(self):
        self.skin = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
        self.glow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
        self.rng = random.Random(8086)

    def put(self, x, y, colour, glow=0.0):
        if not (0 <= x < SIZE and 0 <= y < SIZE):
            return
        self.skin.putpixel((x, y), tuple(colour) + (255,))
        if glow > 0:
            self.glow.putpixel((x, y), tuple(min(255, int(v * glow)) for v in colour) + (255,))
        else:
            self.glow.putpixel((x, y), (0, 0, 0, 0))

    def face(self, name, side):
        return Face(self, regions(find(name))[side])

    def pick(self, colours, index):
        return colours[max(0, min(len(colours) - 1, int(round(index))))]

    def hide(self, name, colours=SKIN, base=3.6, fall=1.6, sides=None):
        c = find(name)
        for side, (x, y, w, h) in regions(c).items():
            if sides and side not in sides:
                continue
            above = [0] * w
            for j in range(h):
                row = []
                i = 0
                while i < w:
                    run = self.rng.choice((1, 2, 2, 3, 3, 4))
                    step = above[i] if self.rng.random() < 0.55 else self.rng.choice((-1, 0, 0, 0, 1))
                    row += [step] * run
                    i += run
                row = row[:w]
                for i in range(w):
                    level = base + row[i]
                    if side in SIDES:
                        level -= fall * j / max(1, h - 1)
                        if j == 0:
                            level += 0.6
                        if j == h - 1:
                            level -= 0.5
                    elif side == "top":
                        level += 0.9
                    else:
                        level -= 1.6
                    self.put(x + i, y + j, self.pick(colours, level))
                above = row

    def lump(self, name, glow=0.5):
        c = find(name)
        for side, rect in regions(c).items():
            f = Face(self, rect)
            for j in range(f.h):
                for i in range(f.w):
                    rim = min(i, f.w - 1 - i, j, f.h - 1 - j) == 0 and min(f.w, f.h) > 2
                    level = (1 if rim else 2) - (1 if side == "bottom" else 0) - (1 if side in SIDES and j == f.h - 1 else 0)
                    f.set(i, j, self.pick(PUS, level), glow * (0.6 + 0.2 * level))
            if side != "bottom" and f.w >= 2 and f.h >= 2:
                f.set(f.w // 2, max(0, f.h // 2 - 1), PUS[3], glow)


class Face:
    def __init__(self, painter, rect):
        self.p = painter
        self.x, self.y, self.w, self.h = rect

    def set(self, i, j, colour, glow=0.0):
        if 0 <= i < self.w and 0 <= j < self.h:
            self.p.put(self.x + i, self.y + j, colour, glow)

    def fill(self, i0, j0, i1, j1, colour, glow=0.0):
        for j in range(j0, j1 + 1):
            for i in range(i0, i1 + 1):
                self.set(i, j, colour, glow)

    def tone(self, i, j, level, colours=SKIN):
        self.set(i, j, self.p.pick(colours, level))

    def flesh(self, i, j, low=1, high=4):
        self.set(i, j, FLESH[self.p.rng.randint(low, high)])

    def wound(self, cells, deep=()):
        cells = set(cells)
        for i, j in cells:
            edge = any((i + a, j + b) not in cells for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            self.set(i, j, FLESH[3 if edge and (i + j) % 2 else 2 if edge else 1])
        for i, j in deep:
            self.set(i, j, FLESH[0])
        for i, j in cells:
            if (i, j + 1) not in cells:
                self.tone(i, j + 1, 1)

    def boil(self, i, j, glow=0.9):
        self.set(i, j, PUS[3], glow)
        self.set(i + 1, j, PUS[2], glow * 0.85)
        self.set(i, j + 1, PUS[2], glow * 0.85)
        self.set(i + 1, j + 1, PUS[1], glow * 0.7)
        self.tone(i, j + 2, 1)
        self.tone(i + 1, j + 2, 1)
        self.set(i + 2, j + 1, FLESH[3])
        self.set(i - 1, j, FLESH[3])

    def vein(self, points, glow=0.7, colour=None):
        for (i0, j0), (i1, j1) in zip(points, points[1:]):
            steps = max(abs(i1 - i0), abs(j1 - j0), 1)
            for s in range(steps + 1):
                self.set(round(i0 + (i1 - i0) * s / steps), round(j0 + (j1 - j0) * s / steps), colour or PUS[1], glow)

    def crease(self, points, level=1, colours=SKIN):
        for (i0, j0), (i1, j1) in zip(points, points[1:]):
            steps = max(abs(i1 - i0), abs(j1 - j0), 1)
            for s in range(steps + 1):
                self.tone(round(i0 + (i1 - i0) * s / steps), round(j0 + (j1 - j0) * s / steps), level, colours)

    def claws(self, columns, missing=()):
        for i in columns:
            self.set(i, self.h - 1, BLACK if i in missing else BONE[2])
            if i not in missing:
                self.tone(i, self.h - 2, 1)


def paint():
    p = Painter()
    p.hide("head", SKIN, 4.0, 1.4)
    p.hide("torso", SKIN, 3.6, 2.0)
    p.hide("front_leg", SKIN, 3.4, 1.6)
    p.hide("hind_leg", SKIN, 3.0, 1.6)
    p.hide("club_leg", SKIN, 3.8, 1.4)
    p.hide("second_head", SKIN, 4.4, 1.2)
    p.hide("jaw", SKIN, 3.2, 0.6)
    p.hide("spare_leg", PALE, 2.6, 2.4)
    p.hide("belly", PALE, 3.4, 1.4)
    p.hide("flap", PALE, 2.6, 1.0)
    p.hide("hump", DEAD, 2.8, 1.4)
    p.hide("scalp", DEAD, 3.0, 1.0)
    p.hide("withered_leg", DEAD, 2.6, 1.8)
    p.hide("eye_growth", SKIN, 3.0, 0.0)
    for name in ("boil_a", "boil_b", "boil_c", "leg_growth"):
        p.lump(name)
    for cube_name in ("rib_top", "spur"):
        for side, rect in regions(find(cube_name)).items():
            f = Face(p, rect)
            for j in range(f.h):
                for i in range(f.w):
                    level = 0 if side == "bottom" else 2 if side == "top" else 1 + (1 if j < f.h / 2 and (i + j) % 2 == 0 else 0)
                    f.set(i, j, BONE[min(2, level)])
            if cube_name == "spur" and side in SIDES:
                f.set(0, f.h - 1, FLESH[2])

    for side, rect in regions(find("eye_growth")).items():
        f = Face(p, rect)
        for j in range(f.h):
            for i in range(f.w):
                f.set(i, j, FLESH[3 if (i + j) % 2 else 2])
    eye = p.face("eye_growth", "north")
    eye.fill(0, 0, 2, 2, PUS[2], 0.9)
    eye.set(0, 0, FLESH[3])
    eye.set(2, 2, PUS[1], 0.7)
    eye.set(0, 2, PUS[1], 0.7)
    eye.set(1, 0, PUS[3], 1.0)
    eye.set(2, 1, FLESH[3], 0.3)
    eye.set(1, 1, BLACK)

    face = p.face("head", "north")
    for i, j in ((1, 1), (2, 1), (0, 2)):
        face.tone(i, j, 2)
    face.fill(1, 2, 2, 3, BLACK)
    face.set(1, 2, SOCKET)
    face.tone(0, 4, 5)
    face.set(1, 4, FLESH[1])
    face.set(1, 5, FLESH[2])
    face.set(1, 6, FLESH[3])
    face.tone(2, 4, 5)
    for i, j in ((4, 1), (5, 1), (6, 1), (7, 1), (4, 2), (4, 3), (5, 5), (6, 5), (7, 5)):
        face.flesh(i, j, 2, 4)
    face.fill(5, 2, 7, 4, FLESH[0])
    face.fill(3, 4, 4, 4, BLACK)
    face.fill(2, 5, 5, 6, BLACK)
    face.set(2, 5, SOCKET)
    face.set(5, 5, SOCKET)
    face.set(2, 7, BLACK)
    face.set(4, 7, BLACK)
    face.set(5, 7, BLACK)
    face.set(3, 7, BONE[1])
    face.set(3, 5, BONE[2])
    face.tone(1, 7, 1)
    face.tone(6, 6, 1)
    face.tone(6, 7, 1)
    face.tone(0, 5, 2)
    face.tone(7, 6, 2)
    face.vein([(7, 0), (6, 0), (5, 0)])

    top = p.face("head", "top")
    top.wound([(5, 4), (6, 4), (4, 5), (5, 5), (6, 5), (5, 6), (6, 6), (7, 5)], deep=[(5, 5), (6, 5)])
    top.vein([(5, 7), (4, 7)])
    top.vein([(4, 4), (3, 3), (2, 3), (1, 2)])
    cheek = p.face("head", "west")
    cheek.crease([(6, 3), (6, 6)])
    cheek.crease([(4, 4), (4, 6)])
    cheek.vein([(0, 1), (1, 1), (2, 2), (3, 2), (4, 1)])
    for j in (1, 2, 3, 4):
        cheek.flesh(7, j, 2, 4)
    left = p.face("head", "east")
    left.boil(4, 4)
    left.vein([(0, 0), (1, 1), (2, 1), (3, 2), (4, 3)])
    for j in (1, 2, 3, 4, 5):
        left.flesh(0, j, 2, 4)
    back = p.face("head", "south")
    for j in range(1, 5):
        for i in range(4, 8):
            back.tone(i, j, 3 - (i + j) % 2 - j * 0.3, DEAD)
    back.vein([(7, 5), (5, 5), (4, 6), (2, 6), (1, 7)])
    back.crease([(6, 5), (6, 7)])
    p.face("head", "bottom").fill(0, 0, 7, 7, FLESH[0])

    jaw = p.face("jaw", "north")
    jaw.fill(1, 0, 4, 0, BLACK)
    jaw.set(1, 0, BONE[1])
    jaw.set(3, 0, BONE[2])
    jaw.set(4, 0, BONE[0])
    jaw.set(2, 1, FLESH[2])
    jaw.tone(5, 1, 1)
    inside = p.face("jaw", "top")
    inside.fill(0, 0, 5, 2, FLESH[0])
    inside.fill(1, 1, 4, 1, FLESH[2])
    for i in (0, 2, 3, 5):
        inside.set(i, 2, BONE[1 if i % 2 else 2])

    chest = p.face("torso", "north")
    chest.fill(0, 1, 0, 7, FLESH[0])
    for j in (2, 4, 6):
        chest.set(0, j, BONE[1])
    for j in range(1, 8):
        chest.flesh(1, j, 2, 4)
    chest.vein([(2, 2), (3, 1), (4, 1), (5, 2), (6, 2), (7, 1)])
    flank = p.face("torso", "west")
    flank.fill(0, 1, 3, 7, FLESH[0])
    for j in (1, 3, 5, 7):
        for i in range(4):
            flank.flesh(i, j, 0, 2)
    flank.set(1, 3, PUS[2], 1.0)
    flank.set(2, 3, PUS[1])
    flank.set(2, 5, PUS[2], 1.0)
    for i in range(4):
        flank.flesh(i, 0, 2, 4)
        flank.flesh(i, 8, 2, 4)
    flank.set(1, 9, FLESH[1])
    flank.set(1, 10, FLESH[2])
    flank.set(3, 9, FLESH[2])
    spine = p.face("torso", "south")
    spine.fill(7, 1, 7, 7, FLESH[0])
    for j in (2, 4, 6):
        spine.set(7, j, BONE[1])
    for j in range(1, 8):
        spine.flesh(6, j, 2, 4)
    for j in (5, 7, 9, 11):
        spine.set(3, j, BONE[1])
        spine.set(4, j, BONE[0])
        spine.tone(3, j - 1, 1)
        spine.tone(4, j - 1, 1)
    spine.vein([(0, 6), (1, 7), (2, 7), (2, 9), (1, 10)])
    side = p.face("torso", "east")
    for i, j in ((0, 3), (0, 4), (0, 5), (1, 2), (2, 2), (3, 2), (1, 6), (2, 6), (3, 6)):
        side.flesh(i, j, 2, 4)
    side.vein([(1, 7), (1, 9), (2, 10), (3, 10)])

    gut = p.face("belly", "north")
    for i, j in ((3, 2), (3, 3), (6, 2), (6, 3), (4, 1), (5, 1), (4, 4), (5, 4)):
        gut.flesh(i, j, 3, 4)
    gut.vein([(3, 1), (2, 0), (1, 0)])
    gut.vein([(3, 4), (2, 5), (1, 5), (0, 4)])
    gut.crease([(0, 2), (1, 2), (2, 3)], 1, PALE)
    p.face("belly", "bottom").wound([(2, 0), (3, 0), (4, 0), (3, 1), (4, 1), (2, 1)], deep=[(3, 0)])
    p.face("belly", "top").crease([(1, 1), (3, 2), (5, 1)], 1, PALE)

    hump = p.face("hump", "south")
    hump.boil(3, 2)
    hump.vein([(0, 1), (1, 1), (2, 2)])
    p.face("hump", "top").vein([(0, 1), (2, 1), (3, 2), (5, 2)])
    p.face("scalp", "top").vein([(0, 2), (1, 2), (2, 1), (3, 1)])
    p.face("scalp", "south").crease([(0, 0), (1, 1), (3, 1)], 0, DEAD)

    small = p.face("second_head", "north")
    small.set(1, 1, BLACK)
    small.tone(0, 1, 1)
    small.tone(1, 0, 1)
    small.set(3, 2, PUS[3], 1.0)
    small.set(3, 1, FLESH[2])
    small.set(4, 2, FLESH[3])
    small.set(2, 3, BLACK)
    small.set(2, 4, BLACK)
    small.set(1, 3, BLACK)
    small.tone(1, 2, 1)
    small.set(3, 4, FLESH[2])
    p.face("second_head", "top").wound([(1, 2), (2, 2), (1, 3), (2, 3), (3, 3)], deep=[(2, 3)])
    p.face("second_head", "east").vein([(0, 2), (1, 1), (3, 1), (4, 0)])
    p.face("second_head", "bottom").fill(0, 0, 4, 4, FLESH[1])

    p.face("front_leg", "north").claws((0, 2, 3))
    p.face("front_leg", "north").vein([(1, 0), (1, 1), (2, 2), (2, 3)])
    p.face("front_leg", "east").wound([(1, 1), (2, 1), (1, 2), (2, 2), (2, 3)], deep=[(1, 2)])
    p.face("hind_leg", "north").claws((0, 2, 3), missing=(2,))
    p.face("hind_leg", "south").vein([(0, 0), (1, 1), (1, 3), (2, 4)])
    club = p.face("club_leg", "north")
    for i, j in ((0, 1), (1, 1), (0, 2), (4, 3)):
        club.flesh(i, j, 3, 4)
    club.boil(2, 1)
    club.claws((0, 2, 4))
    p.face("club_leg", "west").vein([(0, 0), (1, 1), (1, 2), (2, 3), (3, 3), (4, 4)])
    thin = p.face("withered_leg", "north")
    thin.fill(1, 1, 1, 4, BONE[1])
    thin.set(1, 2, BONE[2])
    thin.set(0, 1, FLESH[2])
    thin.set(2, 3, FLESH[2])
    thin.claws((0, 2), missing=(0,))
    p.face("withered_leg", "south").fill(1, 2, 1, 4, BONE[0])
    p.face("withered_leg", "west").fill(1, 1, 1, 4, BONE[0])
    spare = p.face("spare_leg", "north")
    spare.claws((0, 2))
    spare.crease([(1, 0), (1, 1), (2, 2)], 1, PALE)
    p.face("spare_leg", "top").wound([(0, 0), (1, 0), (2, 0), (1, 1), (0, 1), (2, 1)], deep=[(1, 1)])
    for side_name in SIDES:
        strip = p.face("flap", side_name)
        for j in range(2, strip.h):
            for i in range(strip.w):
                strip.set(i, j, FLESH[min(4, j)])
    return p.skin, p.glow


def encoded(image):
    data = io.BytesIO()
    image.save(data, "PNG")
    return "data:image/png;base64," + base64.b64encode(data.getvalue()).decode("ascii")


def ident(*key):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, "criticalmass/" + NAME + "/" + "/".join(str(k) for k in key)))


def project(skin, glow):
    elements, groups = [], []

    def face_uv(c):
        out = {}
        for side, (x, y, w, h) in regions(c).items():
            name = {"top": "up", "bottom": "down", "west": "east", "east": "west"}.get(side, side)
            out[name] = {"uv": [x, y, x + w, y + h], "texture": 0}
        return out

    def build(parts, at):
        nodes = []
        for p in parts:
            here = tuple(a + b for a, b in zip(at, p["pivot"]))
            children = []
            for c in p["cubes"]:
                x, y, z = (here[i] + c["origin"][i] for i in range(3))
                w, h, d = c["size"]
                cid = ident("cube", p["name"], c["name"])
                elements.append({"name": c["name"], "box_uv": True, "render_order": "default", "rescale": False, "locked": False, "shade": True, "light_emission": 0, "export": True, "scope": 0,
                                 "allow_mirror_modeling": True, "from": [-(x + w), 24 - (y + h), z], "to": [-x, 24 - y, z + d], "autouv": 0, "color": 0, "inflate": c["inflate"],
                                 "origin": [-here[0], 24 - here[1], here[2]], "uv_offset": list(c["uv"]), "faces": face_uv(c), "type": "cube", "uuid": cid})
                children.append(cid)
            children += build(p["children"], here)
            gid = ident("group", p["name"])
            groups.append({"name": p["name"], "uuid": gid, "export": True, "locked": False, "scope": 0, "selected": False, "visibility": True, "_static": {"properties": {}, "temp_data": {}},
                           "origin": [-here[0], 24 - here[1], here[2]], "rotation": [-p["turn"][0], -p["turn"][1], p["turn"][2]], "color": 0, "children": [], "reset": False, "shade": True,
                           "mirror_uv": False, "autouv": 0, "isOpen": True, "primary_selected": False})
            nodes.append({"uuid": gid, "isOpen": True, "children": children})
        return nodes

    outliner = build(MODEL, (0, 0, 0))
    def texture(index, file, image):
        return {"path": os.path.join(BLOCKBENCH, file), "name": file, "relative_path": file, "folder": "entity", "namespace": "criticalmass", "id": str(index), "group": "", "scope": 0,
                "width": SIZE, "height": SIZE, "uv_width": SIZE, "uv_height": SIZE, "particle": False, "use_as_default": index == 0, "layers_enabled": False, "sync_to_project": "",
                "file_format": "png", "render_mode": "default" if index == 0 else "emissive", "render_sides": "auto", "wrap_mode": "limited", "pbr_channel": "color", "fps": 7, "frame_time": 1,
                "frame_order_type": "loop", "frame_order": "", "frame_interpolate": False, "visible": True, "internal": True, "saved": True, "uuid": ident("texture", index), "source": encoded(image)}

    return {"meta": {"format_version": "5.0", "model_format": "modded_entity", "box_uv": True}, "name": NAME, "model_identifier": "", "modded_entity_entity_class": "IrradiatedCreeper",
            "modded_entity_version": "1.17", "modded_entity_flip_y": True, "visible_box": [1, 1, 0], "variable_placeholders": "", "variable_placeholder_buttons": [], "unhandled_root_fields": {},
            "resolution": {"width": SIZE, "height": SIZE}, "elements": elements, "groups": groups, "outliner": outliner, "textures": [texture(0, NAME + ".png", skin), texture(1, NAME + "_glow.png", glow)]}


def load():
    data = json.load(open(os.path.join(BLOCKBENCH, NAME + ".bbmodel"), encoding="utf-8"))
    cubes = {c["uuid"]: c for c in data["elements"]}
    groups = {g["uuid"]: g for g in data.get("groups", [])}

    def read(nodes, parent):
        out = []
        for node in nodes:
            if isinstance(node, str):
                continue
            g = dict(groups.get(node["uuid"], {}), **{k: v for k, v in node.items() if k not in ("children",)})
            origin = g.get("origin", [0, 0, 0])
            here = (-origin[0], 24 - origin[1], origin[2])
            turn = g.get("rotation", [0, 0, 0])
            made = part(g["name"], tuple(here[i] - parent[i] for i in range(3)), (-turn[0], -turn[1], turn[2]))
            for child in node["children"]:
                if not isinstance(child, str):
                    continue
                c = cubes[child]
                assert c.get("box_uv", True), "%s must use box UV" % c["name"]
                assert not c.get("mirror_uv"), "%s is mirrored, which the exporter does not write" % c["name"]
                size = tuple(c["to"][i] - c["from"][i] for i in range(3))
                origin_j = (-c["to"][0] - here[0], 24 - c["to"][1] - here[1], c["from"][2] - here[2])
                made_cube = {"name": c["name"], "uv": tuple(c.get("uv_offset", [0, 0])), "origin": origin_j, "size": size, "inflate": c.get("inflate", 0)}
                spin = c.get("rotation") or [0, 0, 0]
                if any(spin):
                    o = c.get("origin", origin)
                    at = (-o[0], 24 - o[1], o[2])
                    moved = dict(made_cube, origin=tuple(here[i] + origin_j[i] - at[i] for i in range(3)))
                    made["children"].append(part(c["name"] + "_turned", tuple(at[i] - here[i] for i in range(3)), (-spin[0], -spin[1], spin[2]), [moved]))
                else:
                    made["cubes"].append(made_cube)
            made["children"] += read(node["children"], here)
            out.append(made)
        return out

    model = read(data["outliner"], (0, 0, 0))
    names = [p["name"] for p, _ in walk(model)]
    assert len(names) == len(set(names)), "two groups share a name"
    missing = [n for n in NEEDED if n not in names]
    assert not missing, "the animation needs groups called %s" % ", ".join(missing)
    return model, (data["resolution"]["width"], data["resolution"]["height"])


def number(value):
    r = round(float(value), 4)
    return ("%d" % r if r == int(r) else repr(r)) + "F"


def camel(name):
    head, *rest = name.split("_")
    return head + "".join(r.capitalize() for r in rest)


def java(model, size):
    lines = []

    def emit(parts, parent):
        for p in parts:
            builder = "CubeListBuilder.create()"
            for c in p["cubes"]:
                grow = "grow" if not c["inflate"] else "grow.extend(%s)" % number(c["inflate"])
                builder += ".texOffs(%d, %d).addBox(%s, %s)" % (c["uv"][0], c["uv"][1], ", ".join(number(v) for v in tuple(c["origin"]) + tuple(c["size"])), grow)
            if any(p["turn"]):
                pose = "PartPose.offsetAndRotation(%s)" % ", ".join(number(v) for v in tuple(p["pivot"]) + tuple(math.radians(a) for a in p["turn"]))
            else:
                pose = "PartPose.offset(%s)" % ", ".join(number(v) for v in p["pivot"])
            call = '%s.addOrReplaceChild("%s", %s, %s);' % (parent, p["name"], builder, pose)
            lines.append("        " + ("PartDefinition %s = " % camel(p["name"]) if p["children"] else "") + call)
            emit(p["children"], camel(p["name"]))

    emit(model, "root")
    return "\n".join([
        "package com.criticalmass.client.entity;",
        "",
        "import net.minecraft.client.model.geom.PartPose;",
        "import net.minecraft.client.model.geom.builders.CubeDeformation;",
        "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
        "import net.minecraft.client.model.geom.builders.LayerDefinition;",
        "import net.minecraft.client.model.geom.builders.MeshDefinition;",
        "import net.minecraft.client.model.geom.builders.PartDefinition;",
        "",
        "public final class IrradiatedCreeperParts {",
        "    private IrradiatedCreeperParts() {",
        "    }",
        "",
        "    public static LayerDefinition create(CubeDeformation grow) {",
        "        MeshDefinition mesh = new MeshDefinition();",
        "        PartDefinition root = mesh.getRoot();",
    ] + lines + [
        "        return LayerDefinition.create(mesh, %d, %d);" % size,
        "    }",
        "}",
        "",
    ])


def turn_matrix(turn):
    x, y, z = (math.radians(a) for a in turn)
    rx = np.array([[1, 0, 0], [0, math.cos(x), -math.sin(x)], [0, math.sin(x), math.cos(x)]])
    ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
    rz = np.array([[math.cos(z), -math.sin(z), 0], [math.sin(z), math.cos(z), 0], [0, 0, 1]])
    return rz @ ry @ rx


def quads(model, pose=None):
    out = []

    def visit(parts, matrix, shift):
        for p in parts:
            extra = (pose or {}).get(p["name"], (0, 0, 0))
            m = matrix @ turn_matrix(tuple(a + b for a, b in zip(p["turn"], extra)))
            s = shift + matrix @ np.array(p["pivot"], float)
            for c in p["cubes"]:
                g = c["inflate"]
                x0, y0, z0 = (v - g for v in c["origin"])
                x1, y1, z1 = (c["origin"][i] + c["size"][i] + g for i in range(3))
                u, v = c["uv"]
                w, h, d = c["size"]
                v7, v0, v1, v2, v3, v4, v5, v6 = (x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)
                for corners, (u1, w1, u2, w2) in (((v4, v3, v7, v0), (u + d, v, u + d + w, v + d)), ((v1, v2, v6, v5), (u + d + w, v + d, u + d + w + w, v)),
                                                  ((v7, v3, v6, v2), (u, v + d, u + d, v + d + h)), ((v0, v7, v2, v1), (u + d, v + d, u + d + w, v + d + h)),
                                                  ((v4, v0, v1, v5), (u + d + w, v + d, u + d + w + d, v + d + h)), ((v3, v4, v5, v6), (u + d + w + d, v + d, u + d + w + d + w, v + d + h))):
                    points = [s + m @ np.array(corner, float) for corner in corners]
                    out.append((points, [(u2, w1), (u1, w1), (u1, w2), (u2, w2)]))
            visit(p["children"], m, s)

    visit(model, np.eye(3), np.zeros(3))
    return out


def render(model, skin, glow=None, yaw=25, pitch=12, scale=12, size=(360, 420), pose=None, back=(52, 60, 50)):
    w, h = size
    colour = np.zeros((h, w, 3), float)
    colour[:] = back
    depth = np.full((h, w), -1e9)
    tex = np.asarray(skin.convert("RGBA"), float)
    lit = np.asarray(glow.convert("RGBA"), float) if glow is not None else None
    cy, sy, cp, sp = math.cos(math.radians(yaw)), math.sin(math.radians(yaw)), math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    light = np.array([0.35, 0.8, 0.5])
    light /= np.linalg.norm(light)

    def view(p):
        x, y, z = p[0], 12 - p[1], -p[2]
        x, z = x * cy + z * sy, -x * sy + z * cy
        y, z = y * cp - z * sp, y * sp + z * cp
        return np.array([w / 2 + x * scale, h / 2 - y * scale + 2 * scale, z])

    for points, uvs in quads(model, pose):
        pts = [view(p) for p in points]
        a, b, c = (np.array([q[0], -q[1], q[2]]) for q in pts[:3])
        normal = np.cross(b - a, c - a)
        length = np.linalg.norm(normal)
        if length < 1e-9:
            continue
        normal /= length
        if normal[2] < 0:
            normal = -normal
        shade = 0.55 + 0.45 * max(0.0, float(normal @ light))
        for tri in ((0, 1, 2), (0, 2, 3)):
            (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = (pts[i] for i in tri)
            det = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
            if abs(det) < 1e-9:
                continue
            xa, xb = max(0, int(math.floor(min(x0, x1, x2)))), min(w - 1, int(math.ceil(max(x0, x1, x2))))
            ya, yb = max(0, int(math.floor(min(y0, y1, y2)))), min(h - 1, int(math.ceil(max(y0, y1, y2))))
            if xa > xb or ya > yb:
                continue
            gx, gy = np.meshgrid(np.arange(xa, xb + 1) + 0.5, np.arange(ya, yb + 1) + 0.5)
            l0 = ((y1 - y2) * (gx - x2) + (x2 - x1) * (gy - y2)) / det
            l1 = ((y2 - y0) * (gx - x2) + (x0 - x2) * (gy - y2)) / det
            l2 = 1 - l0 - l1
            inside = (l0 >= 0) & (l1 >= 0) & (l2 >= 0)
            z = l0 * z0 + l1 * z1 + l2 * z2
            uu = l0 * uvs[tri[0]][0] + l1 * uvs[tri[1]][0] + l2 * uvs[tri[2]][0]
            vv = l0 * uvs[tri[0]][1] + l1 * uvs[tri[1]][1] + l2 * uvs[tri[2]][1]
            ui = np.clip(np.floor(uu).astype(int), 0, tex.shape[1] - 1)
            vi = np.clip(np.floor(vv).astype(int), 0, tex.shape[0] - 1)
            sample = tex[vi, ui]
            region = depth[ya:yb + 1, xa:xb + 1]
            hit = inside & (z > region) & (sample[..., 3] > 25)
            pixel = sample[..., :3] * shade
            if lit is not None:
                pixel = np.minimum(255, pixel + 0.6 * lit[vi, ui][..., :3] * (lit[vi, ui][..., 3:4] / 255.0))
            colour[ya:yb + 1, xa:xb + 1][hit] = pixel[hit]
            region[hit] = z[hit]
    return Image.fromarray(colour.astype(np.uint8), "RGB")


def sheet(model, skin, glow, path):
    views = [(25, 10), (-35, 10), (155, 14), (-140, 14), (90, 8), (0, 60)]
    shots = [render(model, skin, glow, yaw, pitch) for yaw, pitch in views]
    out = Image.new("RGB", (shots[0].width * 3, shots[0].height * 2))
    for i, shot in enumerate(shots):
        out.paste(shot, (shot.width * (i % 3), shot.height * (i // 3)))
    out.save(path)


def main():
    args = sys.argv[1:]
    bb = os.path.join(BLOCKBENCH, NAME + ".bbmodel")
    skin_path = os.path.join(BLOCKBENCH, NAME + ".png")
    glow_path = os.path.join(BLOCKBENCH, NAME + "_glow.png")
    if "fresh" in args:
        skin, glow = paint()
        skin.save(skin_path)
        glow.save(glow_path)
        with open(bb, "w", encoding="utf-8", newline="\n") as f:
            json.dump(project(skin, glow), f)
        print("painted", skin_path, "and built", bb)
    assert os.path.exists(bb), "run with fresh once to make the Blockbench file"
    model, size = load()
    os.makedirs(JAVA, exist_ok=True)
    with open(os.path.join(JAVA, "IrradiatedCreeperParts.java"), "w", encoding="utf-8", newline="\n") as f:
        f.write(java(model, size))
    target = os.path.join(ASSETS, "textures", "entity")
    os.makedirs(target, exist_ok=True)
    shutil.copyfile(skin_path, os.path.join(target, NAME + ".png"))
    shutil.copyfile(glow_path, os.path.join(target, NAME + "_glow.png"))
    print(sum(len(p["cubes"]) for p, _ in walk(model)), "cubes in", sum(1 for _ in walk(model)), "parts")
    for arg in args:
        if arg.endswith(".png"):
            sheet(model, Image.open(skin_path), Image.open(glow_path), arg)
            print("preview", arg)


if __name__ == "__main__":
    main()
