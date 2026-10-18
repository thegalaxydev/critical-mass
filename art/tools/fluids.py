import json
import os
import random
from PIL import Image
from art import P, RAMPS, Canvas, nearest

FLUIDS = {
    "primary_coolant": {"mat": "coolant", "alpha": 220, "desc": "Tileable 16x16 Minecraft fluid surface texture for a pressurised primary reactor coolant: deep blue with lighter cyan ripples, like vanilla water but bluer and denser, seamless on all four edges."},
    "steam": {"mat": "steam", "alpha": 150, "desc": "Tileable 16x16 Minecraft fluid texture for steam: soft pale grey-white swirling vapour wisps, low contrast, seamless on all four edges."},
    "contaminated_coolant": {"mat": "contaminated", "alpha": 230, "desc": "Tileable 16x16 Minecraft fluid texture for contaminated reactor coolant: murky olive yellow-green liquid with darker sludge swirls and a few sickly bright specks, seamless on all four edges."},
}


def blockout(name):
    spec = FLUIDS[name]
    c = Canvas(16, 16)
    rnd = random.Random(name)
    ramp = spec["mat"]
    for y in range(16):
        for x in range(16):
            v = 2 + int(1.5 * ((x * 3 + y * 5) % 7) / 7) + (1 if rnd.random() < 0.15 else 0)
            c.px(x, y, P(ramp, v))
    return c


def tileable_fix(tile):
    return tile


def animate(name, tile, out_dir):
    spec = FLUIDS[name]
    alpha = spec["alpha"]
    ramp = RAMPS[spec["mat"]]
    frames = 16
    still = Image.new("RGBA", (16, 16 * frames))
    for f in range(frames):
        fr = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                sx = (x + f // 2) % 16
                sy = (y + (f // 4)) % 16
                c = tile.getpixel((sx, sy))
                c2 = tile.getpixel(((x - f // 3) % 16, (y + f // 2) % 16))
                mix = tuple((c[i] * 2 + c2[i]) // 3 for i in range(3))
                q = nearest(mix, ramp)
                fr.putpixel((x, y), q + (alpha,))
        still.paste(fr, (0, f * 16))
    flow = Image.new("RGBA", (32, 32 * frames))
    for f in range(frames):
        fr = Image.new("RGBA", (32, 32))
        for y in range(32):
            for x in range(32):
                c = tile.getpixel((x % 16, (y - f * 2) % 16))
                fr.putpixel((x, y), nearest(c[:3], ramp) + (alpha,))
        flow.paste(fr, (0, f * 32))
    os.makedirs(out_dir, exist_ok=True)
    still.save(os.path.join(out_dir, name + "_still.png"))
    flow.save(os.path.join(out_dir, name + "_flow.png"))
    json.dump({"animation": {"frametime": 3, "interpolate": True}}, open(os.path.join(out_dir, name + "_still.png.mcmeta"), "w"))
    json.dump({"animation": {"frametime": 2}}, open(os.path.join(out_dir, name + "_flow.png.mcmeta"), "w"))
    return still, flow
