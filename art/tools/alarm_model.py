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
SIZE = 64
COLORS = 35
PER_ROW = 5
CELL = (9, 8)
BUTTON_CELL = (12, 6)
SIDE = (0, 0, 4, 7)
TOP = (4, 0, 4, 4)
CAGE_X = 46
RING_TOP = (CAGE_X, 0, 6, 6)
RING_SIDE = (CAGE_X, 7, 6, 1)
ARCH_BAR = (CAGE_X, 9, 6, 1)
ARCH = (CAGE_X + 7, 0, 6, 7)
ARCH_EDGE = (CAGE_X + 14, 0, 1, 7)
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


def cage(img):
    x, y, w, h = RING_TOP
    for i in range(w):
        for j in range(h):
            if i in (0, w - 1) or j in (0, h - 1):
                tone = 4 if j == 0 or i == 0 else 1
                if (i, j) in ((0, 0),):
                    tone = 5
                if (i, j) in ((w - 1, h - 1),):
                    tone = 0
                if (i, j) in ((w - 1, 0), (0, h - 1)):
                    tone = 2
                img.putpixel((x + i, y + j), STEEL[tone] + (255,))
    x, y, w, h = RING_SIDE
    for i, tone in enumerate((4, 3, 3, 2, 2, 1)):
        img.putpixel((x + i, y), STEEL[tone] + (255,))
    x, y, w, h = ARCH_BAR
    for i, tone in enumerate((4, 5, 4, 3, 3, 2)):
        img.putpixel((x + i, y), STEEL[tone] + (255,))
    x, y, w, h = ARCH
    for i, tone in enumerate((4, 5, 4, 3, 3, 2)):
        img.putpixel((x + i, y), STEEL[tone] + (255,))
    for j in range(1, h):
        img.putpixel((x, y + j), STEEL[4 if j < 5 else 3] + (255,))
        img.putpixel((x + w - 1, y + j), STEEL[2 if j < 5 else 1] + (255,))
    x, y, w, h = ARCH_EDGE
    for j, tone in enumerate((4, 3, 3, 3, 2, 2, 1)):
        img.putpixel((x, y + j), STEEL[tone] + (255,))


def paint(lit):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for c, (dark, mid, light) in enumerate(hues()):
        ox, oy = CELL[0] * (c % PER_ROW), CELL[1] * (c // PER_ROW)
        colors = roles(dark, mid, light, lit)
        stamp(img, (ox + SIDE[0], oy + SIDE[1]), LIT_SIDE if lit else UNLIT_SIDE, colors)
        stamp(img, (ox + TOP[0], oy + TOP[1]), LIT_TOP if lit else UNLIT_TOP, colors)
    cage(img)
    return img


def atlases(fresh):
    out = {}
    for name, lit in (("alarm_atlas", False), ("alarm_atlas_lit", True)):
        path = os.path.join(ORIGINALS, name + ".png")
        if fresh or not os.path.exists(path):
            paint(lit).save(path)
            print("painted", name)
        img = Image.open(path).convert("RGBA")
        img.save(os.path.join(PARTS, name + ".png"))
        out[name] = img
    return out


def rect(r, flip_u=False):
    x, y, w, h = r
    return [x + w, y, x, y + h] if flip_u else [x, y, x + w, y + h]


def kind(e):
    size = [abs(e["to"][i] - e["from"][i]) for i in range(3)]
    if size == [8, 2, 8]:
        return "base"
    if size[0] == 4 and size[2] == 4:
        return "lamp"
    if size[1] <= 1.01:
        return "ring"
    return "arch"


def straighten(e):
    rotation = e.get("rotation")
    if not rotation or abs(rotation[1]) != 90:
        return
    ox, oz = e["origin"][0], e["origin"][2]
    (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
    e["from"] = [ox - (z1 - oz), y0, oz - (x1 - ox)]
    e["to"] = [ox - (z0 - oz), y1, oz - (x0 - ox)]
    e["rotation"] = [0, 0, 0]
    e["turned"] = True


def remap(model):
    names = {"ring": 0, "arch": 0}
    for e in model["elements"]:
        straighten(e)
        what = kind(e)
        faces = e["faces"]
        if what == "base":
            e["name"] = "base"
            continue
        if what == "lamp":
            e["name"] = "lamp"
            for d in ("north", "east", "south", "west"):
                faces[d] = {"uv": rect(SIDE), "texture": 0}
            faces["up"] = {"uv": rect(TOP), "texture": 0}
            faces["down"] = {"uv": [0, 0, 0, 0], "texture": None}
        elif what == "ring":
            e["name"] = ("ring_low", "ring_mid", "ring_high")[min(2, names["ring"])]
            names["ring"] += 1
            for d in ("north", "east", "south", "west"):
                faces[d] = {"uv": rect(RING_SIDE), "texture": 0}
            faces["up"] = {"uv": rect(RING_TOP), "texture": 0}
            faces["down"] = {"uv": rect(RING_TOP), "texture": 0}
        else:
            e["name"] = ("arch_across", "arch_along")[min(1, names["arch"])]
            names["arch"] += 1
            thin_x = abs(e["to"][0] - e["from"][0]) < abs(e["to"][2] - e["from"][2])
            wide, edge = (("east", "west"), ("north", "south")) if thin_x else (("north", "south"), ("east", "west"))
            faces[wide[0]] = {"uv": rect(ARCH), "texture": 0}
            faces[wide[1]] = {"uv": rect(ARCH, True), "texture": 0}
            for d in edge:
                faces[d] = {"uv": rect(ARCH_EDGE), "texture": 0}
            faces["up"] = {"uv": rect(ARCH_BAR), "texture": 0, "rotation": 90 if thin_x else 0}
            faces["down"] = {"uv": [0, 0, 0, 0], "texture": None}
        e.pop("turned", None)


def embed(model, sheets):
    body = next((t for t in model["textures"] if "big_body" in t.get("name", "")), None)
    textures = []
    for i, name in enumerate(("alarm_atlas", "alarm_atlas_lit")):
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
    lamp = [e for e in model["elements"] if e["name"] == "lamp"]
    bars = [e for e in model["elements"] if e["name"].startswith(("ring", "arch"))]
    write(os.path.join(ASSETS, "models", "block", "dashboard_part", "alarm_lamp.json"), {"render_type": "minecraft:cutout", "textures": atlas, "elements": [element(e) for e in lamp]})
    write(os.path.join(ASSETS, "models", "block", "dashboard_part", "alarm_cage.json"), {"render_type": "minecraft:cutout", "textures": atlas, "elements": [element(e) for e in bars]})
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
        "elements": parts + [element(e, shift) for e in lamp + bars]})


def main():
    args = sys.argv[1:]
    sheets = atlases("paint" in args)
    model = json.load(open(MODEL, encoding="utf-8"))
    if "map" in args:
        remap(model)
        embed(model, sheets)
        with open(MODEL, "w", encoding="utf-8", newline="\n") as f:
            json.dump(model, f)
        print("mapped", os.path.relpath(MODEL, ROOT))
    export(model)
    print("exported alarm_lamp, alarm_cage, item")


if __name__ == "__main__":
    main()
