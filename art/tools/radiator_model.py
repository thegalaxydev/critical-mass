import base64
import json
import os
import sys

from PIL import Image

import iso

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SOURCE = os.path.join(ROOT, "art", "blockbench", "radiator.bbmodel")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass")
NAME = "radiator_panel"
TEXTURE = "criticalmass:block/" + NAME
FACINGS = ("north", "east", "south", "west")


def tidy(value):
    r = round(value, 4)
    return int(r) if r == int(r) else r


def write(path, obj):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=1)
        f.write("\n")


def main():
    project = json.load(open(SOURCE, encoding="utf-8"))
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
        elements.append({"name": cube["name"], "from": [tidy(v) for v in cube["from"]], "to": [tidy(v) for v in cube["to"]], "faces": faces})
    png = os.path.join(ASSETS, "textures", "block", NAME + ".png")
    with open(png, "wb") as f:
        f.write(base64.b64decode(project["textures"][0]["source"].split(",", 1)[1]))
    model = {"parent": "minecraft:block/block", "textures": {"0": TEXTURE, "particle": TEXTURE}, "elements": elements}
    write(os.path.join(ASSETS, "models", "block", NAME + ".json"), model)
    write(os.path.join(ASSETS, "models", "item", NAME + ".json"), {"parent": "criticalmass:block/" + NAME})
    write(os.path.join(ASSETS, "blockstates", NAME + ".json"), {"variants": {"facing=" + f: {"model": "criticalmass:block/" + NAME} for f in FACINGS}})
    lo = [min(e["from"][i] for e in elements) for i in range(3)]
    hi = [max(e["to"][i] for e in elements) for i in range(3)]
    print(len(elements), "cubes, extent", lo, hi)
    if len(sys.argv) > 1:
        iso.load_model = lambda ref: model
        iso._cache.clear()
        views = [iso.render(TEXTURE, 360, 225, 25), iso.render(TEXTURE, 360, 45, 25), iso.render(TEXTURE, 360, 180, 0), iso.render(TEXTURE, 360, 180, 89)]
        sheet = Image.new("RGBA", (360 * len(views), 360), (88, 110, 74, 255))
        for i, v in enumerate(views):
            sheet.alpha_composite(v, (360 * i, 0))
        sheet.save(sys.argv[1])


if __name__ == "__main__":
    main()
