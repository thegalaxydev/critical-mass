import os
import sys

from PIL import Image

import bbmodel

NAME = "boiler_heating_plate"
M = "criticalmass:block/"
LEVELS = ("cold", "warm", "hot")
FACING = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}}
GLOW = {"block_light": 15, "sky_light": 15, "ambient_occlusion": False}


def named(level):
    return NAME if level == "cold" else NAME + "_" + level


def coil(elements, level):
    if level != "hot":
        return elements
    return [dict(e, shade=False, neoforge_data=dict(GLOW)) for e in elements]


def main():
    parts, textures = bbmodel.load(NAME)
    for group in ("body",) + tuple("coil_" + level for level in LEVELS):
        assert parts.get(group), "the model needs a group called " + group
    particle = textures["1"]
    block = os.path.join(bbmodel.ASSETS, "models", "block")
    for level in LEVELS:
        bbmodel.write(os.path.join(block, named(level) + ".json"), bbmodel.model(parts["body"] + coil(parts["coil_" + level], level), textures, particle))
    bbmodel.write(os.path.join(bbmodel.ASSETS, "models", "item", NAME + ".json"), {"parent": M + named("warm")})
    variants = {}
    for facing, turn in FACING.items():
        for heat, level in enumerate(LEVELS):
            variants["facing=%s,heat=%d" % (facing, heat)] = dict({"model": M + named(level)}, **turn)
    bbmodel.write(os.path.join(bbmodel.ASSETS, "blockstates", NAME + ".json"), {"variants": variants})
    print(len(parts["body"]), "body cubes,", len(parts["coil_cold"]), "coil cubes in each of", len(LEVELS), "heat levels")
    if len(sys.argv) > 1:
        shots = []
        for level in LEVELS:
            path = sys.argv[1].replace(".png", "_" + level + ".png")
            bbmodel.preview(M + named(level), path, views=((215, 20), (270, 0), (180, 0)), size=300)
            with Image.open(path) as shot:
                shots.append(shot.copy())
            os.remove(path)
        sheet = Image.new("RGBA", (shots[0].width, shots[0].height * len(shots)))
        for i, shot in enumerate(shots):
            sheet.paste(shot, (0, shot.height * i))
        sheet.save(sys.argv[1])


if __name__ == "__main__":
    main()
