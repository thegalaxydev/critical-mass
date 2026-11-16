import os
import sys

import bbmodel

NAME = "reactor_controller"
M = "criticalmass:block/"
STATES = ("off", "standby", "running", "tripped")
FACING = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}}
GLOW = {"block_light": 15, "sky_light": 15, "ambient_occlusion": False}


def display(elements):
    out = []
    for e in elements:
        e = dict(e)
        if e["name"].endswith("_lit"):
            e["shade"] = False
            e["neoforge_data"] = dict(GLOW)
        out.append(e)
    return out


def main():
    parts, textures = bbmodel.load(NAME)
    for group in ("body",) + tuple("display_" + s for s in STATES):
        assert parts.get(group), "the model needs a group called " + group
    particle = textures["1"]
    block = os.path.join(bbmodel.ASSETS, "models", "block")
    for state in STATES:
        name = NAME if state == "off" else NAME + "_" + state
        bbmodel.write(os.path.join(block, name + ".json"), bbmodel.model(parts["body"] + display(parts["display_" + state]), textures, particle))
    bbmodel.write(os.path.join(bbmodel.ASSETS, "models", "item", NAME + ".json"), {"parent": M + NAME + "_running"})
    variants = {}
    for facing, turn in FACING.items():
        for state in STATES:
            variants["facing=%s,status=%s" % (facing, state)] = dict({"model": M + (NAME if state == "off" else NAME + "_" + state)}, **turn)
    bbmodel.write(os.path.join(bbmodel.ASSETS, "blockstates", NAME + ".json"), {"variants": variants})
    print(len(parts["body"]), "body cubes,", len(parts["display_off"]), "display cubes in each of", len(STATES), "states")
    if len(sys.argv) > 1:
        sheets = []
        for state in STATES:
            path = sys.argv[1].replace(".png", "_" + state + ".png")
            bbmodel.preview(M + (NAME if state == "off" else NAME + "_" + state), path, views=((0, 0), (35, 20)), size=300)
            sheets.append(path)
        from PIL import Image
        shots = [Image.open(p).transpose(Image.FLIP_LEFT_RIGHT) for p in sheets]
        sheet = Image.new("RGBA", (shots[0].width * 2, shots[0].height * 2))
        for i, shot in enumerate(shots):
            sheet.paste(shot, (shot.width * (i % 2), shot.height * (i // 2)))
        sheet.save(sys.argv[1])
        for p in sheets:
            os.remove(p)


if __name__ == "__main__":
    main()
