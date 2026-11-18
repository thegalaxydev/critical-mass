import json
import os
import shutil
import sys

import bbmodel

NAME = "meltdown_siren"
M = "criticalmass:block/"
TEXTURE = M + NAME
MOVING = ("spindle",)
TURN = {"south": 0, "west": 90, "north": 180, "east": 270}
GUI = {"gui": {"rotation": [30, 45, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]}}


def load(file):
    project = json.load(open(os.path.join(bbmodel.BLOCKBENCH, file + ".bbmodel"), encoding="utf-8"))
    assert len(project["textures"]) == 1, "%s should use one texture" % file
    scale = 16.0 / project["textures"][0].get("uv_width", project["resolution"]["width"])
    elements = []
    for cube in project["elements"]:
        assert not any(cube.get("rotation") or [0, 0, 0]), "%s in %s is turned" % (cube["name"], file)
        faces = {}
        for d, face in cube["faces"].items():
            if face.get("texture") is None:
                continue
            g = {"uv": [bbmodel.tidy(v * scale) for v in face["uv"]], "texture": "#0"}
            if face.get("rotation"):
                g["rotation"] = face["rotation"]
            faces[d] = g
        elements.append({"name": cube["name"], "from": [bbmodel.tidy(v) for v in cube["from"]], "to": [bbmodel.tidy(v) for v in cube["to"]], "faces": faces})
    assert any(e["name"] in MOVING for e in elements), "%s needs a cube called spindle" % file
    return elements


def model(elements, **extra):
    return bbmodel.model(elements, {"0": TEXTURE}, TEXTURE, **extra)


def main():
    floor, wall = load(NAME), load(NAME + "_mounted")
    block = os.path.join(bbmodel.ASSETS, "models", "block")
    shutil.copyfile(os.path.join(bbmodel.BLOCKBENCH, NAME + ".png"), os.path.join(bbmodel.ASSETS, "textures", "block", NAME + ".png"))
    bbmodel.write(os.path.join(block, NAME + ".json"), model([e for e in floor if e["name"] not in MOVING]))
    bbmodel.write(os.path.join(block, NAME + "_wall.json"), model([e for e in wall if e["name"] not in MOVING]))
    spindle = model([e for e in floor if e["name"] in MOVING])
    spindle.pop("parent")
    bbmodel.write(os.path.join(block, NAME + "_spindle.json"), spindle)
    bbmodel.write(os.path.join(block, NAME + "_item.json"), model(floor, display=GUI))
    bbmodel.write(os.path.join(bbmodel.ASSETS, "models", "item", NAME + ".json"), {"parent": M + NAME + "_item"})
    variants = {}
    for facing, turn in TURN.items():
        for mounted, ref in (("false", M + NAME), ("true", M + NAME + "_wall")):
            variants["facing=%s,wall=%s" % (facing, mounted)] = dict({"model": ref}, **({"y": turn} if turn else {}))
    bbmodel.write(os.path.join(bbmodel.ASSETS, "blockstates", NAME + ".json"), {"variants": variants})
    print(len(floor), "standing cubes,", len(wall), "mounted cubes")
    if len(sys.argv) > 1:
        bbmodel.preview(M + NAME + "_item", sys.argv[1], views=((45, 20), (315, 20), (135, 20), (0, 0)), size=320)


if __name__ == "__main__":
    main()
