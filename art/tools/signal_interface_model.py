import os
import sys

from PIL import Image

import bbmodel

NAME = "reactor_signal_interface"
M = "criticalmass:block/"
FUNCTIONS = ("readings", "scram", "reset", "rods_in", "rods_out", "alarm")
STEP = 2
FACING = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}}
GLOW = {"block_light": 15, "sky_light": 15, "ambient_occlusion": False}


def slid(element, places):
    out = dict(element)
    out["from"] = [bbmodel.tidy(element["from"][0] - STEP * places)] + element["from"][1:]
    out["to"] = [bbmodel.tidy(element["to"][0] - STEP * places)] + element["to"][1:]
    return out


def lit(elements):
    return [dict(e, shade=False, neoforge_data=dict(GLOW)) for e in elements]


def main():
    parts, textures = bbmodel.load(NAME)
    for group in ("body", "lens", "lens_lit", "tab"):
        assert parts.get(group), "the model needs a group called " + group
    particle = textures["1"]
    block = os.path.join(bbmodel.ASSETS, "models", "block")
    tabs = {name: [slid(e, i) for e in parts["tab"]] for i, name in enumerate(FUNCTIONS)}
    bbmodel.write(os.path.join(block, NAME + ".json"), bbmodel.model(parts["body"], textures, particle))
    bbmodel.write(os.path.join(block, NAME + "_lens.json"), bbmodel.model(parts["lens"], textures, particle))
    bbmodel.write(os.path.join(block, NAME + "_lens_lit.json"), bbmodel.model(lit(parts["lens_lit"]), textures, particle))
    for name, cubes in tabs.items():
        bbmodel.write(os.path.join(block, NAME + "_tab_" + name + ".json"), bbmodel.model(cubes, textures, particle))
    bbmodel.write(os.path.join(block, NAME + "_item.json"), bbmodel.model(parts["body"] + parts["lens"] + tabs["readings"], textures, particle))
    bbmodel.write(os.path.join(block, NAME + "_item_lit.json"), bbmodel.model(parts["body"] + lit(parts["lens_lit"]) + tabs["alarm"], textures, particle))
    bbmodel.write(os.path.join(bbmodel.ASSETS, "models", "item", NAME + ".json"), {"parent": M + NAME + "_item"})
    multipart = []
    for facing, turn in FACING.items():
        multipart.append({"when": {"facing": facing}, "apply": dict({"model": M + NAME}, **turn)})
        multipart.append({"when": {"facing": facing, "powered": "false"}, "apply": dict({"model": M + NAME + "_lens"}, **turn)})
        multipart.append({"when": {"facing": facing, "powered": "true"}, "apply": dict({"model": M + NAME + "_lens_lit"}, **turn)})
        for name in FUNCTIONS:
            multipart.append({"when": {"facing": facing, "function": name}, "apply": dict({"model": M + NAME + "_tab_" + name}, **turn)})
    bbmodel.write(os.path.join(bbmodel.ASSETS, "blockstates", NAME + ".json"), {"multipart": multipart})
    print(len(parts["body"]), "body cubes,", len(FUNCTIONS), "tab places")
    if len(sys.argv) > 1:
        shots = []
        for ref in (M + NAME + "_item", M + NAME + "_item_lit"):
            path = sys.argv[1].replace(".png", "_part.png")
            bbmodel.preview(ref, path, views=((0, 0), (35, 20)), size=300)
            shots.append(Image.open(path).transpose(Image.FLIP_LEFT_RIGHT))
            os.remove(path)
        sheet = Image.new("RGBA", (shots[0].width, shots[0].height * 2))
        for i, shot in enumerate(shots):
            sheet.paste(shot, (0, shot.height * i))
        sheet.save(sys.argv[1])
    os.remove(os.path.join(block, NAME + "_item_lit.json"))


if __name__ == "__main__":
    main()
