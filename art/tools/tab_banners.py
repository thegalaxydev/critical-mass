import math
import os
import random
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass", "textures", "gui", "banner")
W, H = 160, 16
BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


class Canvas:
    def __init__(self, fill):
        self.rows = [[fill] * W for _ in range(H)]

    def put(self, x, y, colour):
        x, y = int(x), int(y)
        if 0 <= x < W and 0 <= y < H:
            self.rows[y][x] = colour

    def get(self, x, y):
        return self.rows[max(0, min(H - 1, int(y)))][max(0, min(W - 1, int(x)))]

    def box(self, x0, y0, x1, y1, colour):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.put(x, y, colour)

    def edge(self, x0, y0, x1, y1, colour):
        for x in range(x0, x1 + 1):
            self.put(x, y0, colour)
            self.put(x, y1, colour)
        for y in range(y0, y1 + 1):
            self.put(x0, y, colour)
            self.put(x1, y, colour)

    def disc(self, cx, cy, r, colour):
        for y in range(int(cy - r) - 1, int(cy + r) + 2):
            for x in range(int(cx - r) - 1, int(cx + r) + 2):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r + 0.3:
                    self.put(x, y, colour)

    def line(self, x0, y0, x1, y1, colour):
        steps = int(max(abs(x1 - x0), abs(y1 - y0), 1))
        for i in range(steps + 1):
            self.put(round(x0 + (x1 - x0) * i / steps), round(y0 + (y1 - y0) * i / steps), colour)

    def title(self, tint, start=50, end=82):
        speck = mix(tint, (255, 255, 255), 0.08)
        for y in range(H):
            for x in range(end):
                solid = x < start or BAYER[y % 4][x % 4] / 16.0 >= (x - start) / float(end - start)
                if solid:
                    self.rows[y][x] = speck if x % 4 == 1 and y % 4 == 1 else tint

    def framed(self):
        image = Image.new("RGBA", (W + 2, H + 2), (255, 255, 255, 255))
        for x in range(W + 2):
            image.putpixel((x, 0), (55, 55, 55, 255))
        for y in range(H + 2):
            image.putpixel((0, y), (55, 55, 55, 255))
        image.putpixel((W + 1, 0), (139, 139, 139, 255))
        image.putpixel((0, H + 1), (139, 139, 139, 255))
        for y in range(H):
            for x in range(W):
                image.putpixel((x + 1, y + 1), self.rows[y][x] + (255,))
        return image


def reactor(frame, frames):
    c = Canvas((26, 32, 44))
    for y in range(0, H, 4):
        for x in range(0, W, 4):
            c.put(x + 2, y + 2, (32, 40, 54))
    c.box(64, 14, 159, 15, (40, 46, 58))
    hot, warm, cold = (240, 132, 58), (214, 190, 120), (78, 152, 226)
    path = [(x, 3) for x in range(99, 152)] + [(151, y) for y in range(4, 12)] + [(x, 12) for x in range(151, 98, -1)]
    for i, (x, y) in enumerate(path):
        part = i / float(len(path))
        heat = hot if x < 122 and y == 3 else mix(warm, cold, min(1.0, (part - 0.35) * 2.2)) if not (x < 122 and y == 3) and part < 0.8 else cold
        for dx, dy in ((0, -1), (0, 1), (-1, 0), (1, 0)):
            if c.get(x + dx, y + dy) in ((26, 32, 44), (32, 40, 54)):
                c.put(x + dx, y + dy, (58, 64, 78))
        c.put(x, y, mix(heat, (255, 255, 255), 0.55) if (i - frame) % 6 == 0 else mix(heat, (255, 255, 255), 0.2) if (i - frame) % 6 == 1 else heat)
    c.box(122, 0, 138, 6, (104, 112, 126))
    c.edge(122, 0, 138, 6, (60, 66, 80))
    for x in range(124, 137, 2):
        c.line(x, 1, x, 5, (136, 146, 160))
    for i, x in enumerate(range(124, 137)):
        c.put(x, 2 + (1 if (x + frame // 3) % 4 < 2 else 3), (120, 214, 170))
    c.disc(118, 12, 3, (158, 105, 71))
    c.disc(118, 12, 2, (206, 160, 90))
    turn = frame % 4
    spoke = [((-2, 0), (2, 0)), ((-1, -1), (1, 1)), ((0, -2), (0, 2)), ((1, -1), (-1, 1))][turn]
    c.line(118 + spoke[0][0], 12 + spoke[0][1], 118 + spoke[1][0], 12 + spoke[1][1], (255, 235, 140))
    c.put(118, 12, (114, 71, 49))
    c.box(84, 1, 98, 14, (64, 72, 90))
    c.edge(84, 1, 98, 14, (38, 44, 58))
    c.line(85, 2, 97, 2, (92, 102, 122))
    for x in (86, 96):
        for y in (4, 8, 12):
            c.put(x, y, (104, 114, 134))
    pulse = 0.5 + 0.5 * math.sin(frame / float(frames) * math.tau * 2)
    glow = mix((60, 170, 240), (190, 240, 255), pulse)
    c.box(89, 5, 93, 11, (16, 24, 40))
    c.box(90, 6, 92, 10, mix((30, 90, 160), glow, 0.6))
    c.put(91, 7 + frame // 4 % 3, glow)
    c.put(90, 9 - frame // 6 % 2, mix(glow, (255, 255, 255), 0.4))
    for x in (87, 91, 95):
        c.box(x, 0, x, 0, (150, 158, 172))
    c.title((22, 34, 56))
    return c


def equipment(frame, frames):
    c = Canvas((96, 84, 72))
    for x in range(64, W, 16):
        c.line(x, 0, x, 9, (84, 73, 62))
    c.line(60, 9, 159, 9, (110, 98, 84))
    c.box(60, 13, 159, 15, (58, 50, 44))
    c.box(64, 10, 159, 12, (46, 46, 52))
    for x in range(64, W):
        if (x - frame) % 4 == 0:
            c.put(x, 12, (74, 74, 84))
        if (x - frame) % 4 == 2:
            c.put(x, 10, (64, 64, 72))
    for x in (68, 154):
        c.disc(x, 13, 1.5, (130, 130, 140))
    press = 112
    for start in range(-24, W + 24, 24):
        x = start + frame + 4
        done = x > press
        if 96 < x < 136:
            c.box(x - 2, 3, x + 2, 9, (70, 76, 90))
            c.line(x - 2, 3, x + 2, 3, (196, 202, 214))
            c.line(x - 2, 9, x + 2, 9, (150, 156, 168))
            for i, rod in enumerate((x - 1, x, x + 1)):
                bright = ((244, 220, 96), (198, 236, 112), (244, 220, 96))[i] if done else ((126, 132, 146), (96, 104, 120), (126, 132, 146))[i]
                c.line(rod, 4, rod, 8, bright)
    phase = (press - 4 - frame) % 24
    near = min(phase, 24 - phase)
    drop = 3 if near == 0 else 2 if near == 1 else 1 if near == 2 else 0
    c.box(press - 4, 0, press + 4, 1, (114, 71, 49))
    c.box(press - 1, 0, press + 1, drop, (206, 160, 90))
    c.box(press - 3, drop, press + 3, drop + 1, (247, 203, 108))
    c.line(press - 3, drop + 1, press + 3, drop + 1, (158, 105, 71))
    if near == 0:
        for dx, dy in ((-5, 4), (5, 4), (-6, 6), (6, 6), (-4, 2), (4, 2)):
            c.put(press + dx, dy + 1, (255, 240, 170))
    saw = 84
    c.disc(saw, 7, 4, (176, 182, 194))
    c.disc(saw, 7, 2, (120, 126, 138))
    for i in range(8):
        angle = (i + frame % 2 * 0.5) * math.tau / 8
        c.put(round(saw + math.cos(angle) * 5), round(7 + math.sin(angle) * 5), (226, 232, 240))
    c.put(saw, 7, (60, 66, 80))
    c.box(saw - 7, 8, saw + 7, 12, (112, 90, 68))
    c.edge(saw - 7, 8, saw + 7, 12, (70, 54, 40))
    c.line(saw - 5, 8, saw + 5, 8, (40, 36, 34))
    if frame % 6 < 3:
        for dx, dy in ((5, 3), (7, 2), (6, 5)):
            c.put(saw + dx + frame % 3, dy, (255, 226, 130))
    c.box(138, 1, 152, 9, (44, 48, 58))
    c.edge(138, 1, 152, 9, (150, 156, 166))
    c.box(141, 2, 149, 8, (158, 105, 71))
    for x in range(141, 150):
        if (x + frame) % 4 == 0:
            c.line(x, 2, x, 8, (255, 235, 140))
        elif (x + frame) % 4 == 1:
            c.line(x, 2, x, 8, (206, 160, 90))
    c.line(145, 0, 145, 0, (214, 220, 230))
    c.title((70, 46, 30))
    return c


def console(frame, frames):
    c = Canvas((46, 52, 62))
    c.box(60, 0, 159, 1, (62, 70, 82))
    c.box(60, 14, 159, 15, (30, 34, 42))
    c.box(84, 2, 118, 13, (24, 28, 34))
    c.box(86, 3, 116, 12, (8, 26, 18))
    for x in range(86, 117, 6):
        c.line(x, 3, x, 12, (14, 40, 28))
    c.line(86, 8, 116, 8, (14, 40, 28))
    last = None
    for x in range(86, 117):
        t = (x + frame) / 24.0 * math.tau
        y = round(7.5 + 2.6 * math.sin(t) + 1.2 * math.sin(t * 3 + 1.0))
        if last is not None:
            c.line(x - 1, last, x, y, (90, 240, 150))
        last = y
    c.put(116, last, (210, 255, 225))
    c.line(86, 3, 116, 3, (20, 60, 40))
    for index, (x, rate, on, off) in enumerate(((124, 8, (255, 84, 70), (92, 30, 28)), (129, 12, (255, 190, 70), (96, 66, 24)), (134, 24, (110, 240, 130), (30, 84, 44)))):
        lit = frame % rate < rate // 2 if index < 2 else frame % 24 != 11
        c.disc(x, 4, 1.5, on if lit else off)
        c.put(x - 1, 3, mix(on, (255, 255, 255), 0.6) if lit else off)
        c.edge(x - 2, 2, x + 2, 6, (28, 32, 40))
    for index, cx in enumerate((142, 152)):
        c.disc(cx, 7, 4.2, (28, 32, 40))
        c.disc(cx, 7, 3.2, (224, 220, 204))
        swing = math.sin(frame / float(frames) * math.tau * (1 if index == 0 else 2) + index) * 0.9
        angle = -math.pi / 2 + swing
        c.line(cx, 7, cx + math.cos(angle) * 3, 7 + math.sin(angle) * 3, (196, 50, 44))
        c.put(cx, 7, (40, 40, 46))
    for index, x in enumerate((124, 129, 134)):
        down = (frame // 4) % 6 == index
        c.box(x - 1, 10 + (1 if down else 0), x + 1, 12, (196, 62, 50) if index == 0 else (206, 160, 90))
        c.line(x - 1, 13, x + 1, 13, (24, 28, 34))
    c.title((18, 46, 36))
    return c


def electricity(frame, frames):
    c = Canvas((22, 24, 44))
    rng = random.Random(7)
    for _ in range(26):
        c.put(rng.randrange(62, W), rng.randrange(0, 9), (58, 62, 96))
    c.box(60, 14, 159, 15, (30, 34, 54))
    left, right = 88, 151

    def sag(x):
        mid = (left + right) / 2.0
        return 4 + 6.0 * (1 - ((x - mid) / (right - mid)) ** 2)

    for x in range(left, right + 1):
        c.put(x, round(sag(x)), (150, 92, 54))
    for x in (left, right):
        c.box(x - 1, 6, x + 1, 13, (104, 110, 124))
        c.line(x + 1, 6, x + 1, 13, (72, 78, 92))
        for y in (2, 4):
            c.box(x - 2, y, x + 2, y, (222, 212, 190))
            c.put(x + 2, y, (170, 160, 140))
        c.box(x - 1, 3, x + 1, 3, (150, 140, 124))
        c.box(x - 3, 13, x + 3, 13, (72, 78, 92))
    at = left + (right - left) * frame / float(frames - 1)
    jag = random.Random(100 + frame)
    for x in range(left, right + 1):
        d = abs(x - at)
        y = round(sag(x))
        if d < 9:
            wobble = jag.choice((-1, 0, 0, 1)) if d > 1 else 0
            c.put(x, y + wobble, (255, 255, 255) if d < 3 else (150, 232, 255) if d < 6 else (84, 150, 240))
            if d < 5 and jag.random() < 0.5:
                c.put(x, y + wobble + jag.choice((-2, -1, 1, 2)), (120, 210, 255))
        elif x < at and at - x < 22:
            c.put(x, y, mix((150, 92, 54), (120, 190, 255), 1 - (at - x) / 22.0))
    for x, lit in ((left, frame < 3), (right, frame > frames - 4)):
        if lit:
            for _ in range(7):
                angle = jag.random() * math.tau
                reach = 2 + jag.random() * 3
                c.put(round(x + math.cos(angle) * reach), round(4 + math.sin(angle) * reach), (255, 250, 200) if jag.random() < 0.5 else (140, 220, 255))
            c.put(x, 4, (255, 255, 255))
    c.title((30, 26, 66))
    return c


def fallout(frame, frames):
    blast = 0.0 if frame < 8 else 1.0 if frame < 34 else max(0.0, 1 - (frame - 34) / 12.0)
    ruin = 0.0 if frame < 12 else min(1.0, (frame - 12) / 14.0) if frame < 34 else max(0.0, 1 - (frame - 36) / 11.0)
    sky_calm, sky_fire, sky_sick = (150, 196, 246), (236, 150, 84), (104, 116, 92)
    fire = max(0.0, 1 - abs(frame - 13) / 6.0) if frame >= 8 else 0.0
    sky = mix(mix(sky_calm, sky_sick, ruin), sky_fire, fire * 0.9)
    c = Canvas(sky)
    for y in range(0, 4):
        for x in range(60, W):
            if BAYER[y % 4][x % 4] < 6 - y * 2:
                c.put(x, y, mix(sky, (255, 255, 255) if ruin < 0.5 else (60, 66, 50), 0.14))
    hill = mix((108, 164, 132), (86, 88, 70), ruin)
    for x in range(60, W):
        top = 9 - round(1.6 * math.sin(x / 13.0) + 1.0 * math.sin(x / 5.0 + 2))
        for y in range(max(6, top), 11):
            c.put(x, y, hill)
    rng = random.Random(11)
    green_a, green_b = (86, 168, 84), (112, 196, 98)
    dead_a, dead_b = (94, 88, 70), (120, 112, 88)
    for x in range(60, W):
        order = rng.random()
        turned = min(1.0, max(0.0, ruin * 1.6 - order * 0.6))
        height = 10 + rng.choice((0, 0, 1))
        for y in range(height, H):
            base = green_a if (x + y) % 3 else green_b
            gone = dead_a if (x + y) % 3 else dead_b
            c.put(x, y, mix(base, gone, turned))
        if rng.random() < 0.3:
            c.put(x, height - 1, mix(green_b, dead_b, turned))
        if rng.random() < 0.07 and height + 2 < H:
            c.put(x, height + 1, mix((250, 222, 90), dead_a, min(1.0, ruin * 2)))
    trunk = mix((112, 78, 52), (40, 36, 34), min(1.0, ruin * 2))
    c.box(146, 7, 147, 12, trunk)
    leaves = max(0.0, 1 - ruin * 2.2)
    if leaves > 0:
        c.disc(146.5, 5, 3.4 * math.sqrt(leaves) + 0.4, mix((58, 140, 78), (150, 120, 60), 1 - leaves))
        if leaves > 0.6:
            c.put(145, 3, (104, 190, 110))
            c.put(147, 4, (104, 190, 110))
    else:
        c.line(147, 7, 149, 5, trunk)
        c.line(146, 8, 144, 6, trunk)
    cx = 112
    if 8 <= frame < 30:
        t = frame - 8
        rise = min(1.0, t / 7.0)
        fade = 1.0 if t < 12 else max(0.0, 1 - (t - 12) / 10.0)
        hot = max(0.0, 1 - t / 9.0)
        smoke = mix(mix((92, 84, 80), (255, 208, 112), hot), sky, 1 - fade)
        core = mix(mix((120, 98, 88), (255, 246, 210), hot), sky, 1 - fade)
        cap_y = 10 - rise * 6.5
        cap_r = 2.5 + rise * 3.5
        stem = round(cap_y + 1)
        if fade > 0.05:
            c.box(cx - 1, stem, cx + 1, 10, smoke)
            c.box(cx, stem, cx, 10, core)
            c.disc(cx, 11, 1.5 + rise * 3, smoke)
            for dx in range(-int(cap_r + 4), int(cap_r + 4) + 1):
                for dy in range(-3, 3):
                    if (dx / (cap_r + 2.5)) ** 2 + (dy / 2.6) ** 2 <= 1:
                        c.put(cx + dx, cap_y + dy, smoke if dy > -1 or abs(dx) > cap_r else core)
            if t > 3:
                ring = (t - 3) * 3.2
                for side in (-1, 1):
                    c.put(cx + side * ring, 11, mix((240, 236, 220), sky, min(1.0, (t - 3) / 14.0)))
                    c.put(cx + side * (ring - 1), 12, mix((240, 236, 220), sky, min(1.0, (t - 3) / 14.0)))
    if frame in (8, 9):
        whiten = 0.92 if frame == 8 else 0.55
        for y in range(H):
            for x in range(60, W):
                c.put(x, y, mix(c.get(x, y), (255, 250, 228), whiten))
    if 16 <= frame < 40:
        ash = random.Random(5)
        thick = min(1.0, (frame - 16) / 5.0) * (1.0 if frame < 34 else max(0.0, 1 - (frame - 34) / 6.0))
        for _ in range(46):
            x = ash.randrange(60, W)
            speed = ash.choice((1, 1, 2))
            y = (ash.randrange(0, 16) + frame * speed) % 16
            if ash.random() < thick:
                c.put(x, y, (176, 236, 120) if ash.random() < 0.5 else (126, 186, 96))
    if ruin > 0.5:
        spark = random.Random(23)
        for _ in range(12):
            x, y = spark.randrange(62, W), spark.randrange(11, H)
            if (frame + x) % 6 < 3:
                c.put(x, y, mix(c.get(x, y), (170, 255, 130), min(1.0, (ruin - 0.5) * 2)))
    c.title(mix((40, 66, 34), (48, 50, 30), ruin))
    return c


BANNERS = {"reactor": (reactor, 24), "equipment": (equipment, 24), "console": (console, 24), "fallout": (fallout, 48), "electricity": (electricity, 16)}


def sheet(name):
    paint, frames = BANNERS[name]
    out = Image.new("RGBA", (W + 2, (H + 2) * frames))
    for frame in range(frames):
        out.paste(paint(frame, frames).framed(), (0, frame * (H + 2)))
    return out


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to paint the creative tab's section banners again")
        return
    os.makedirs(OUT, exist_ok=True)
    for name in BANNERS:
        image = sheet(name)
        image.save(os.path.join(OUT, name + ".png"))
        print(name, image.size)


if __name__ == "__main__":
    main()
