import os
import sys

from PIL import Image

import bbmodel

BLOCK = os.path.join(bbmodel.ASSETS, "models", "block")
M = "criticalmass:block/"
STUB = M + "relief_valve/stub"
SIDE = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}


def stubs(faces):
    return [{"when": {d: "true"}, "apply": dict({"model": STUB}, **SIDE[d])} for d in faces]


def main():
    for name in ("heat_exchanger", "condenser"):
        parts, textures = bbmodel.load(name)
        assert parts.get("body"), "%s needs a group called body" % name
        bbmodel.write(os.path.join(BLOCK, name + ".json"), bbmodel.model(parts["body"], textures, textures["0"]))
        bbmodel.write(os.path.join(bbmodel.ASSETS, "models", "item", name + ".json"), {"parent": M + name})
    states = os.path.join(bbmodel.ASSETS, "blockstates")
    bbmodel.write(os.path.join(states, "heat_exchanger.json"), {"multipart": [{"apply": {"model": M + "heat_exchanger"}}] + stubs(("north", "east", "south", "west"))})
    turned = [{"when": {"facing": "north|south"}, "apply": {"model": M + "condenser"}}, {"when": {"facing": "east|west"}, "apply": {"model": M + "condenser", "y": 90}}]
    bbmodel.write(os.path.join(states, "condenser.json"), {"multipart": turned + stubs(("up", "down"))})
    print("exported the exchanger models")
    if len(sys.argv) > 1:
        shots = []
        for name in ("heat_exchanger", "condenser"):
            path = os.path.join(sys.argv[1], name + "_tmp.png")
            bbmodel.preview(M + name, path, views=((225, 25), (180, 0)), size=300)
            shots.append(Image.open(path).copy())
            os.remove(path)
        sheet = Image.new("RGBA", (600 * len(shots), 300), (88, 110, 74, 255))
        for i, shot in enumerate(shots):
            sheet.alpha_composite(shot, (600 * i, 0))
        sheet.save(os.path.join(sys.argv[1], "exchangers_preview.png"))


if __name__ == "__main__":
    main()
