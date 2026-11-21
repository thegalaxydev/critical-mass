import io
import os
import sys
import zipfile

import numpy as np
from PIL import Image

from pipe_models import create_jar
from plant_style import BLOCK

STONES = ["andesite", "asurine", "calcite", "crimsite", "deepslate", "diorite", "dripstone", "granite", "limestone", "ochrum", "scorchia", "scoria", "tuff", "veridium"]
KINDS = {"cut": "cut/%s_cut", "cut_polished": "polished/%s_cut_polished", "cut_brick": "brick/%s_cut_brick", "cut_small_brick": "small_brick/%s_cut_small_brick",
         "cut_layered": "layered/%s_cut_layered", "cut_pillar": "pillar/%s_cut_pillar", "cut_cap": "cap/%s_cut_cap", "cut_slab": "slab/%s_cut_slab",
         "cut_layered_connected": "layered/%s_cut_layered_connected", "cut_pillar_connected": "pillar/%s_cut_pillar_connected", "cut_cap_connected": "cap/%s_cut_cap_connected"}
BASE = "assets/create/textures/block/palettes/stone_types/"
SHADE = [(46, 56, 20), (59, 68, 18)]
FILL_LEVEL = 0.93
FILL_CONTRAST = 0.42
RELIEF = 1.0


def luminance(rgb):
    return rgb[..., 0] * 0.299 + rgb[..., 1] * 0.587 + rgb[..., 2] * 0.114


def layouts():
    jar = zipfile.ZipFile(create_jar())
    names = set(jar.namelist())
    stacks = {kind: [] for kind in KINDS}
    alphas = {}
    for stone in STONES:
        loaded = {}
        for kind, pattern in KINDS.items():
            name = BASE + pattern % stone + ".png"
            if name in names:
                loaded[kind] = np.asarray(Image.open(io.BytesIO(jar.read(name))).convert("RGBA"), dtype=float)
        if "cut" not in loaded:
            continue
        pooled = np.concatenate([luminance(a)[a[..., 3] > 0].ravel() for a in loaded.values()])
        spread = np.percentile(pooled, 98) - np.percentile(pooled, 2)
        level = np.median(luminance(loaded["cut"]))
        for kind, a in loaded.items():
            stacks[kind].append((luminance(a) - level) / spread)
            alphas.setdefault(kind, a[..., 3] > 0)
    return {kind: np.median(np.stack(stack), axis=0) for kind, stack in stacks.items()}, alphas


def natural():
    return np.asarray(Image.open(os.path.join(BLOCK, "chartrine_0.png")).convert("RGB"), dtype=float)


def tones(rgb):
    found = sorted({tuple(int(v) for v in c) for c in rgb.reshape(-1, 3)}, key=sum)
    return SHADE + found


def fill(rgb):
    lum = luminance(rgb)
    soft = sum(np.roll(np.roll(lum, dy, 0), dx, 1) * w for dy, dx, w in
               ((0, 0, 4), (0, 1, 2), (0, -1, 2), (1, 0, 2), (-1, 0, 2), (1, 1, 1), (1, -1, 1), (-1, 1, 1), (-1, -1, 1))) / 16
    return lum.mean() * FILL_LEVEL + (soft - soft.mean()) * FILL_CONTRAST


def painted(layout, alpha, base, ramp, spread):
    h, w = layout.shape
    tiled = np.tile(base, (h // 16, w // 16))
    target = tiled + layout * spread * RELIEF
    levels = np.array([c[0] * 0.299 + c[1] * 0.587 + c[2] * 0.114 for c in ramp])
    out = Image.new("RGBA", (w, h))
    for y in range(h):
        for x in range(w):
            if alpha[y, x]:
                out.putpixel((x, y), ramp[int(np.argmin(np.abs(levels - target[y, x])))] + (255,))
    return out


def main():
    rgb = natural()
    ramp = tones(rgb)
    base = fill(rgb)
    lum = luminance(rgb)
    spread = np.percentile(lum, 98) - np.percentile(lum, 2)
    maps, alphas = layouts()
    made = {kind: painted(maps[kind], alphas[kind], base, ramp, spread) for kind in KINDS}
    preview = next((a for a in sys.argv[1:] if a.endswith(".png")), None)
    if preview:
        single = [k for k in KINDS if not k.endswith("_connected")]
        sheet = Image.new("RGBA", ((len(single) + 1) * 34, 34 + 130), (40, 40, 40, 255))
        first = Image.open(os.path.join(BLOCK, "chartrine_0.png")).convert("RGBA")
        for i, img in enumerate([first] + [made[k] for k in single]):
            for dy in (0, 16):
                for dx in (0, 16):
                    sheet.paste(img, (i * 34 + dx, dy))
        x = 0
        for kind in KINDS:
            if kind.endswith("_connected"):
                sheet.paste(made[kind], (x, 34))
                x += made[kind].width + 2
        sheet.resize((sheet.width * 5, sheet.height * 5), Image.NEAREST).save(preview)
    for kind, img in made.items():
        path = os.path.join(BLOCK, "chartrine_%s.png" % kind)
        if os.path.exists(path) and "fresh" not in sys.argv:
            print("left alone; pass fresh to rebuild", os.path.basename(path))
            continue
        if "dry" in sys.argv:
            continue
        img.save(path)
        print("wrote", os.path.basename(path))
    print(len(ramp), "tones", ["#%02x%02x%02x" % t for t in ramp])


if __name__ == "__main__":
    main()
