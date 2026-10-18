import json
import os
import sys
from PIL import Image

S = sys.argv[1]
T = os.path.join(S, "refs", "create", "assets", "create", "textures")
A = os.path.join(S, "refs", "aero", "x", "assets")


def ramp_from(path, n=6, exclude_dark=8):
    im = Image.open(path).convert("RGBA")
    counts = {}
    for p in im.getdata():
        if p[3] < 200:
            continue
        rgb = p[:3]
        if sum(rgb) < exclude_dark * 3:
            continue
        counts[rgb] = counts.get(rgb, 0) + 1
    cols = sorted(counts, key=lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2])
    if len(cols) <= n:
        return cols
    step = (len(cols) - 1) / (n - 1)
    return [cols[round(i * step)] for i in range(n)]


def hexes(cols):
    return ["#%02x%02x%02x" % tuple(c) for c in cols]


ramps = {
    "copper": hexes(ramp_from(os.path.join(T, "block", "copper_casing.png"), 7)),
    "brass": hexes(ramp_from(os.path.join(T, "block", "brass_casing.png"), 7)),
    "andesite": hexes(ramp_from(os.path.join(T, "block", "andesite_casing.png"), 6)),
    "iron": hexes(ramp_from(os.path.join(A, "simulated", "textures", "block", "iron_gearbox.png"), 7)),
    "steel": ["#17191c", "#22262a", "#2e3338", "#3c4249", "#4d545c", "#626a73", "#7c858e"],
    "ceramic": ["#4a4038", "#6b5f53", "#8f8273", "#b3a694", "#d2c7b4", "#e9e1d0"],
    "uranium": ["#2e3a17", "#46561f", "#62762b", "#82993b", "#a5bd55", "#c9db82"],
    "fabric": ["#5f5a4c", "#7f7865", "#a29a83", "#c2baa2", "#dcd5be", "#efe9d6"],
    "rubber": ["#141416", "#202124", "#2e2f33", "#3f4046", "#55575e"],
    "hazard": ["#5a3d0b", "#9a6a12", "#d49a1c", "#f0c03a"],
    "signal_red": ["#4a1310", "#7d1f18", "#b8322a", "#e0573f", "#ff8a6a"],
    "signal_green": ["#173a1a", "#24602a", "#3f9443", "#6cc76a", "#a8eaa0"],
    "signal_amber": ["#5a3008", "#94520e", "#d88a1c", "#ffbe4a", "#ffe39a"],
    "glass": ["#1e3238", "#2f4a52", "#4a6e76", "#73a0a6", "#a8d2d2", "#d9f2ef"],
    "coolant": ["#0f2f4a", "#16466e", "#1f6496", "#3a8cc0", "#6cb8e0", "#a8def5"],
    "contaminated": ["#2a3010", "#3f4a17", "#5a6a1f", "#7d8f2c", "#a3b445", "#c9d77a"],
    "steam": ["#8d9297", "#a9adb1", "#c3c6c9", "#d9dbdd", "#eceeef", "#ffffff"],
    "concrete": ["#2c2e30", "#3b3e41", "#4b4f53", "#5d6166", "#71767b", "#878c91"],
    "lead": ["#262832", "#363946", "#484c5c", "#5d6274", "#767c90", "#9399ad"],
    "ink": ["#0b0b0d"],
}
out = os.path.join(S, "art", "palette.json")
os.makedirs(os.path.dirname(out), exist_ok=True)
json.dump(ramps, open(out, "w"), indent=1)
W = 16 * 8
im = Image.new("RGB", (W, 16 * len(ramps)), (30, 30, 30))
for r, (name, cols) in enumerate(ramps.items()):
    for i, h in enumerate(cols):
        c = tuple(int(h[k:k + 2], 16) for k in (1, 3, 5))
        for y in range(r * 16, r * 16 + 16):
            for x in range(i * 16, i * 16 + 16):
                im.putpixel((x, y), c)
im = im.resize((im.width * 4, im.height * 4), Image.NEAREST)
im.save(os.path.join(S, "art", "palette.png"))
for k, v in ramps.items():
    print(k, v)
