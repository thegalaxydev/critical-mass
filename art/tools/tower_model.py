import base64
import copy
import json
import os
import sys
import uuid

from PIL import Image

import iso

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODELS = os.path.join(ROOT, "art", "blockbench")
PAINT = os.path.join(ROOT, "art", "originals")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass")
NAME = "cooling_tower_packing"
SHAPES = {"single": (False, False), "bottom": (True, False), "middle": (True, True), "top": (False, True)}
SHELL, GLASS, PACKING, TOP = (0, 0), (16, 0), (0, 16), (16, 16)
BANDS = {SHELL: (4, 12), GLASS: (6, 10), PACKING: (4, 12)}
CORE = ([4, 2, 4], [12, 14, 12])


def model_path(shape):
    return os.path.join(MODELS, NAME + ("" if shape == "single" else "_" + shape) + ".bbmodel")


def paint_path(shape):
    return os.path.join(PAINT, "cooling_tower" + ("" if shape == "single" else "_" + shape) + ".png")


def asset_name(shape):
    return NAME + ("" if shape == "single" else "_" + shape)


def planar(frm, to, d):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    return {"north": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "south": [x0, 16 - y1, x1, 16 - y0], "west": [z0, 16 - y1, z1, 16 - y0],
            "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0], "up": [x0, z0, x1, z1], "down": [x0, 16 - z1, x1, 16 - z0]}[d]


def kind(cube):
    frm, to = cube["from"], cube["to"]
    if frm[0] == 4 and to[0] == 12 and frm[2] == 4 and to[2] == 12:
        return "core"
    if any(frm[i] == to[i] for i in range(3)):
        return "glass"
    return "shell"


def remap(cube, open_top, open_bottom):
    frm, to = cube["from"], cube["to"]
    k = kind(cube)
    cube["autouv"] = 0
    for d, face in cube["faces"].items():
        face.pop("rotation", None)
        rect = planar(frm, to, d)
        flat = rect[0] == rect[2] or rect[1] == rect[3]
        covered = (d == "up" and to[1] == 16 and open_top) or (d == "down" and frm[1] == 0 and open_bottom)
        buried = k == "core" and ((d == "up" and to[1] == 14) or (d == "down" and frm[1] == 2))
        if flat or covered or buried:
            face["texture"] = None
            face["uv"] = [0, 0, 0, 0]
            continue
        ox, oy = PACKING if k == "core" else GLASS if k == "glass" else TOP if d in ("up", "down") else SHELL
        face["uv"] = [rect[0] + ox, rect[1] + oy, rect[2] + ox, rect[3] + oy]
        face["texture"] = 0


def stretch(project, open_top, open_bottom):
    kept = []
    for cube in project["elements"]:
        frm, to = cube["from"], cube["to"]
        upright = frm[1] <= 4 and to[1] >= 12
        if not upright and ((open_top and frm[1] >= 12) or (open_bottom and to[1] <= 4)):
            continue
        if upright and open_top:
            to[1] = 16
        if upright and open_bottom:
            frm[1] = 0
        kept.append(cube)
    live = {c["uuid"] for c in kept}
    project["elements"] = kept
    project["outliner"] = [n for n in project["outliner"] if not isinstance(n, str) or n in live]


def repaint(texture, open_top, open_bottom):
    out = texture.copy()
    for (ox, oy), (a, b) in BANDS.items():
        rows = []
        if open_top:
            rows += list(range(0, a))
        if open_bottom:
            rows += list(range(b, 16))
        for r in rows:
            source = a + (r - a) % (b - a)
            out.paste(texture.crop((ox, oy + source, ox + 16, oy + source + 1)), (ox, oy + r))
    return out


def embed(project, png, shape):
    t = project["textures"][0]
    t.update({"name": os.path.basename(png), "relative_path": "../originals/" + os.path.basename(png), "path": png.replace("/", "\\"), "folder": "", "namespace": "",
              "width": 32, "height": 32, "uv_width": 32, "uv_height": 32, "saved": True,
              "source": "data:image/png;base64," + base64.b64encode(open(png, "rb").read()).decode()})
    project["resolution"] = {"width": 32, "height": 32}
    project["name"] = asset_name(shape)


def save(project, path):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(project, f, separators=(",", ":"))


def derive(shape):
    open_top, open_bottom = SHAPES[shape]
    project = copy.deepcopy(json.load(open(model_path("single"), encoding="utf-8")))
    stretch(project, open_top, open_bottom)
    for cube in project["elements"]:
        cube["uuid"] = str(uuid.uuid4())
        remap(cube, open_top, open_bottom)
    project["outliner"] = [c["uuid"] for c in project["elements"]]
    project["textures"][0]["uuid"] = str(uuid.uuid4())
    png = paint_path(shape)
    repaint(Image.open(paint_path("single")).convert("RGBA"), open_top, open_bottom).save(png)
    embed(project, png, shape)
    save(project, model_path(shape))


def tidy(value):
    r = round(value, 4)
    return int(r) if r == int(r) else r


def java(shape):
    project = json.load(open(model_path(shape), encoding="utf-8"))
    scale = 16.0 / project["textures"][0].get("uv_width", project["resolution"]["width"])
    elements = []
    for cube in project["elements"]:
        assert not any(cube.get("rotation") or [0, 0, 0]), cube["name"]
        faces = {}
        for d, face in cube["faces"].items():
            if face.get("texture") is None:
                continue
            g = {"uv": [tidy(v * scale) for v in face["uv"]], "texture": "#0"}
            if face.get("rotation"):
                g["rotation"] = face["rotation"]
            faces[d] = g
        if faces:
            elements.append({"name": cube["name"], "from": [tidy(v) for v in cube["from"]], "to": [tidy(v) for v in cube["to"]], "faces": faces})
    texture = "criticalmass:block/" + asset_name(shape)
    png = os.path.join(ASSETS, "textures", "block", asset_name(shape) + ".png")
    with open(png, "wb") as f:
        f.write(base64.b64decode(project["textures"][0]["source"].split(",", 1)[1]))
    return {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": {"0": texture, "particle": texture}, "elements": elements}


def write(path, obj):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=1)
        f.write("\n")


def main():
    args = sys.argv[1:]
    fresh = "derive" in args
    for shape in SHAPES:
        if shape != "single" and (fresh or not os.path.exists(model_path(shape))):
            derive(shape)
            print("derived", shape)
    models = {shape: java(shape) for shape in SHAPES}
    for shape, model in models.items():
        write(os.path.join(ASSETS, "models", "block", asset_name(shape) + ".json"), model)
        print(shape, len(model["elements"]), "cubes")
    write(os.path.join(ASSETS, "models", "item", NAME + ".json"), {"parent": "criticalmass:block/" + NAME})
    write(os.path.join(ASSETS, "blockstates", NAME + ".json"), {"variants": {"shape=" + shape: {"model": "criticalmass:block/" + asset_name(shape)} for shape in SHAPES}})
    previews = [a for a in args if a.endswith(".png")]
    if previews:
        iso.load_model = lambda ref: models[ref]
        iso._cache.clear()

        def lift(blocks):
            return lambda c: (c[0], c[1] + 16 * blocks, c[2])
        stack = [("bottom", lift(-1.5)), ("middle", lift(-0.5)), ("top", lift(0.5))]
        views = [iso.render("single", 360, 225, 25), iso.render(stack, 360, 225, 20, scale=360 * 4 / 60.0), iso.render(stack, 360, 180, 0, scale=360 * 4 / 60.0),
                 iso.render("bottom", 360, 225, 35), iso.render("middle", 360, 225, 35), iso.render("top", 360, 225, 35)]
        sheet = Image.new("RGBA", (360 * len(views), 360), (88, 110, 74, 255))
        for i, v in enumerate(views):
            sheet.alpha_composite(v, (360 * i, 0))
        sheet.save(previews[0])


if __name__ == "__main__":
    main()
