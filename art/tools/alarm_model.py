import base64
import io
import json
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODEL = os.path.join(ROOT, "art", "blockbench", "alarm.bbmodel")
ORIGINALS = os.path.join(ROOT, "art", "originals")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass")
PARTS = os.path.join(ASSETS, "textures", "block", "dashboard_part")
M = "criticalmass:"
SHEETS = (("alarm_atlas", False), ("alarm_atlas_lit", True))
SIZE = 64
COLORS = 35
PER_ROW = 5
CELL = (9, 8)
BUTTON_CELL = (12, 6)
SIDE = (0, 0, 4, 7)
TOP = (4, 0, 4, 4)
CAGE_AREA = (46, 0, 18, 64)
STEEL = [(24, 27, 32), (38, 42, 48), (56, 61, 68), (78, 84, 92), (106, 113, 121), (142, 149, 157)]
UNLIT_SIDE = ["LHLB", "LHBD", "BLBD", "BLBD", "BBBD", "BBDD", "DDDK"]
UNLIT_TOP = ["LLBB", "LHLB", "BLBD", "BBDD"]
LIT_SIDE = ["HWHL", "HWLB", "LWLB", "LHLB", "LHLB", "LLBB", "BBBD"]
LIT_TOP = ["HHLL", "HWHL", "LHLB", "LLBB"]


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def scale(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c)


def hues():
    sheet = Image.open(os.path.join(PARTS, "big_button_atlas.png")).convert("RGBA")
    out = []
    for c in range(COLORS):
        x0, y0 = BUTTON_CELL[0] * (c % PER_ROW), BUTTON_CELL[1] * (c // PER_ROW)
        pixels = sorted((sheet.getpixel((x0 + x, y0 + y))[:3] for y in range(6) for x in range(6)), key=sum)
        out.append((pixels[3], pixels[len(pixels) // 2], pixels[-4]))
    return out


def roles(dark, mid, light, lit):
    if lit:
        return {"W": mix(light, (255, 255, 255), 0.8), "H": mix(light, (255, 255, 255), 0.45), "L": light, "B": mid, "D": dark, "K": scale(dark, 0.8)}
    return {"H": scale(light, 0.86), "L": scale(mid, 0.8), "B": scale(mid, 0.62), "D": scale(dark, 0.6), "K": scale(dark, 0.42)}


def stamp(img, origin, rows, colors):
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            img.putpixel((origin[0] + x, origin[1] + y), colors[key] + (255,))


def lamps(lit):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for c, (dark, mid, light) in enumerate(hues()):
        ox, oy = CELL[0] * (c % PER_ROW), CELL[1] * (c // PER_ROW)
        colors = roles(dark, mid, light, lit)
        stamp(img, (ox + SIDE[0], oy + SIDE[1]), LIT_SIDE if lit else UNLIT_SIDE, colors)
        stamp(img, (ox + TOP[0], oy + TOP[1]), LIT_TOP if lit else UNLIT_TOP, colors)
    return img


def strip(length, bright):
    tones = [bright, bright - 1, bright - 1, bright - 2, bright - 2, bright - 3]
    return [max(0, tones[min(len(tones) - 1, i * len(tones) // length)]) for i in range(length)]


def draw(img, key, where):
    what = key[0]
    x, y, w, h = where

    def put(i, j, tone):
        img.putpixel((x + i, y + j), STEEL[tone] + (255,))
    if what == "ring_top":
        for j in range(h):
            for i in range(w):
                if i not in (0, w - 1) and j not in (0, h - 1):
                    continue
                tone = 4 if j == 0 or i == 0 else 1
                if (i, j) == (0, 0):
                    tone = 5
                elif (i, j) == (w - 1, h - 1):
                    tone = 0
                elif (i, j) in ((w - 1, 0), (0, h - 1)):
                    tone = 2
                put(i, j, tone)
    elif what == "ring_side":
        for i, tone in enumerate(strip(w, 4)):
            put(i, 0, tone)
    elif what == "arch_bar":
        for i, tone in enumerate(strip(w, 5)):
            put(i, 0, tone)
    elif what == "arch_face":
        for i, tone in enumerate(strip(w, 5)):
            put(i, 0, tone)
        for j in range(1, h):
            put(0, j, 4 if j < h - 2 else 3)
            if w > 1:
                put(w - 1, j, 2 if j < h - 2 else 1)
    elif what == "arch_edge":
        for j, tone in enumerate(strip(h, 4)):
            put(0, j, tone)


def cage(img, rects):
    x, y, w, h = CAGE_AREA
    for j in range(h):
        for i in range(w):
            img.putpixel((x + i, y + j), (0, 0, 0, 0))
    for key, where in rects.items():
        draw(img, key, where)


def px(v):
    return max(1, int(v + 0.5))


def kind(e):
    size = [abs(e["to"][i] - e["from"][i]) for i in range(3)]
    if size == [8, 2, 8]:
        return "base"
    if size[0] == 4 and size[2] == 4:
        return "lamp"
    if size[1] <= 1.01:
        return "ring"
    return "arch"


def thin_x(e):
    return abs(e["to"][0] - e["from"][0]) < abs(e["to"][2] - e["from"][2])


def straighten(e):
    rotation = e.get("rotation")
    if not rotation or abs(rotation[1]) != 90:
        return
    ox, oz = e["origin"][0], e["origin"][2]
    (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
    e["from"] = [ox - (z1 - oz), y0, oz - (x1 - ox)]
    e["to"] = [ox - (z0 - oz), y1, oz - (x0 - ox)]
    e["rotation"] = [0, 0, 0]


def wants(e):
    sx, sy, sz = [abs(e["to"][i] - e["from"][i]) for i in range(3)]
    if kind(e) == "ring":
        n = max(px(sx), px(sz))
        out = {d: ("ring_side", n, 1) for d in ("north", "east", "south", "west")}
        out["up"] = out["down"] = ("ring_top", n, n)
        return out
    wide = px(max(sx, sz))
    tall = px(sy)
    faces, edges = (("east", "west"), ("north", "south")) if thin_x(e) else (("north", "south"), ("east", "west"))
    out = {d: ("arch_face", wide, tall) for d in faces}
    out.update({d: ("arch_edge", 1, tall) for d in edges})
    out["up"] = ("arch_bar", wide, 1)
    return out


def pack(keys):
    x0, y0, width, height = CAGE_AREA
    rects = {}
    x, y, shelf = 0, 0, 0
    for key in sorted(keys, key=lambda k: (-k[2], -k[1], k[0])):
        w, h = key[1], key[2]
        if x + w > width:
            x, y, shelf = 0, y + shelf + 1, 0
        if y + h > height:
            raise SystemExit("the cage pieces no longer fit beside the lamp colours")
        rects[key] = (x0 + x, y0 + y, w, h)
        x += w + 1
        shelf = max(shelf, h)
    return rects


def bars(model):
    return [e for e in model["elements"] if kind(e) in ("ring", "arch")]


def layout(model):
    if any(e.get("rotation") and any(e["rotation"]) for e in bars(model)):
        raise SystemExit("a cage piece is rotated: run this script with map so it is straightened first")
    return pack({key for e in bars(model) for key in wants(e).values()})


def atlases(fresh, recage, rects):
    out = {}
    for name, lit in SHEETS:
        path = os.path.join(ORIGINALS, name + ".png")
        new = fresh or not os.path.exists(path)
        img = lamps(lit) if new else Image.open(path).convert("RGBA")
        if new or recage:
            cage(img, rects)
            img.save(path)
            print("painted" if new else "repainted the cage on", name)
        img.save(os.path.join(PARTS, name + ".png"))
        out[name] = img
    return out


def rect(r, flip_u=False):
    x, y, w, h = r
    return [x + w, y, x, y + h] if flip_u else [x, y, x + w, y + h]


def rename(model):
    names = {"ring": 0, "arch": 0}
    for e in model["elements"]:
        straighten(e)
        what = kind(e)
        if what in ("base", "lamp"):
            e["name"] = what
        elif what == "ring":
            e["name"] = ("ring_low", "ring_mid", "ring_high")[min(2, names["ring"])]
            names["ring"] += 1
        else:
            e["name"] = ("arch_across", "arch_along")[min(1, names["arch"])]
            names["arch"] += 1


def remap(model, rects):
    for e in model["elements"]:
        what = kind(e)
        if what == "lamp":
            for d in ("north", "east", "south", "west"):
                e["faces"][d] = {"uv": rect(SIDE), "texture": 0}
            e["faces"]["up"] = {"uv": rect(TOP), "texture": 0}
            e["faces"]["down"] = {"uv": [0, 0, 0, 0], "texture": None}
        elif what in ("ring", "arch"):
            chosen = wants(e)
            faces = {}
            for d in ("north", "east", "south", "west", "up", "down"):
                key = chosen.get(d)
                if key is None:
                    faces[d] = {"uv": [0, 0, 0, 0], "texture": None}
                    continue
                face = {"uv": rect(rects[key], key[0] == "arch_face" and d in ("south", "west")), "texture": 0}
                if key[0] == "arch_bar" and thin_x(e):
                    face["rotation"] = 90
                faces[d] = face
            e["faces"] = faces


def embed(model, sheets):
    body = next((t for t in model["textures"] if "big_body" in t.get("name", "")), None)
    textures = []
    for i, (name, lit) in enumerate(SHEETS):
        buf = io.BytesIO()
        sheets[name].save(buf, "PNG")
        base = dict(model["textures"][0])
        base.update({"name": name + ".png", "relative_path": "../originals/" + name + ".png", "path": os.path.join(ORIGINALS, name + ".png"), "width": SIZE, "height": SIZE,
                     "uv_width": SIZE, "uv_height": SIZE, "id": str(i), "source": "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()})
        base.pop("uuid", None)
        textures.append(base)
    if body:
        body = dict(body)
        body["id"] = "2"
        textures.append(body)
    model["textures"] = textures
    for e in model["elements"]:
        if e["name"] == "base":
            for face in e["faces"].values():
                if face.get("texture") is not None:
                    face["texture"] = 2


def element(e, offset=(0, 0, 0), texture="#0"):
    faces = {}
    for d, face in e["faces"].items():
        if face.get("texture") is None:
            continue
        out = {"uv": [v * 16.0 / SIZE for v in face["uv"]], "texture": texture}
        if face.get("rotation"):
            out["rotation"] = face["rotation"]
        faces[d] = out
    return {"from": [e["from"][i] + offset[i] for i in range(3)], "to": [e["to"][i] + offset[i] for i in range(3)], "faces": faces}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=1)
        f.write("\n")


def export(model):
    atlas = {"0": M + "block/dashboard_part/alarm_atlas", "particle": M + "block/dashboard_part/alarm_atlas"}
    lamp = [e for e in model["elements"] if kind(e) == "lamp"]
    cage_bars = bars(model)
    write(os.path.join(ASSETS, "models", "block", "dashboard_part", "alarm_lamp.json"), {"render_type": "minecraft:cutout", "textures": atlas, "elements": [element(e) for e in lamp]})
    write(os.path.join(ASSETS, "models", "block", "dashboard_part", "alarm_cage.json"), {"render_type": "minecraft:cutout", "textures": atlas, "elements": [element(e) for e in cage_bars]})
    body = json.load(open(os.path.join(ASSETS, "models", "block", "dashboard_part", "big_body.json"), encoding="utf-8"))
    shift = (4, 0, 4)
    parts = [dict(e, **{"from": [e["from"][i] + shift[i] for i in range(3)], "to": [e["to"][i] + shift[i] for i in range(3)],
                        "faces": {d: dict(f, texture="#1") for d, f in e["faces"].items()}}) for e in body["elements"]]
    for e in parts:
        e.pop("rotation", None)
    scale_body = body.get("texture_size", [16, 16])[0] / 16.0
    for e in parts:
        for f in e["faces"].values():
            f["uv"] = [v / scale_body for v in f["uv"]]
    write(os.path.join(ASSETS, "models", "item", "dashboard_alarm.json"), {
        "parent": M + "item/dashboard_button", "render_type": "minecraft:cutout",
        "textures": {"0": atlas["0"], "1": body["textures"]["0"], "particle": atlas["0"]},
        "elements": parts + [element(e, shift) for e in lamp + cage_bars]})


def main():
    args = sys.argv[1:]
    model = json.load(open(MODEL, encoding="utf-8"))
    if "map" in args:
        rename(model)
    rects = layout(model)
    sheets = atlases("paint" in args, "cage" in args or "map" in args, rects)
    if "map" in args:
        remap(model, rects)
        embed(model, sheets)
        with open(MODEL, "w", encoding="utf-8", newline="\n") as f:
            json.dump(model, f)
        print("mapped", os.path.relpath(MODEL, ROOT))
        for key, where in sorted(rects.items()):
            print(" ", key, where)
    export(model)
    print("exported alarm_lamp, alarm_cage, item")


if __name__ == "__main__":
    main()
