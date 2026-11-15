import os
import sys

import bbmodel

NAME = "centrifuge"
M = "criticalmass:block/"


def main():
    parts, textures = bbmodel.load(NAME)
    for group in ("frame", "rotor"):
        assert parts.get(group), "the model needs a group called " + group
    particle = textures["0"]
    block = os.path.join(bbmodel.ASSETS, "models", "block")
    bbmodel.write(os.path.join(block, NAME + ".json"), bbmodel.model(parts["frame"], textures, particle))
    rotor = bbmodel.model(parts["rotor"], textures, particle)
    rotor.pop("parent")
    bbmodel.write(os.path.join(block, NAME + "_rotor.json"), rotor)
    bbmodel.write(os.path.join(block, NAME + "_item.json"), bbmodel.model(parts["frame"] + parts["rotor"], textures, particle))
    bbmodel.write(os.path.join(bbmodel.ASSETS, "models", "item", NAME + ".json"), {"parent": M + NAME + "_item"})
    bbmodel.write(os.path.join(bbmodel.ASSETS, "blockstates", NAME + ".json"), {"variants": {"": {"model": M + NAME}}})
    print(len(parts["frame"]), "frame cubes,", len(parts["rotor"]), "rotor cubes")
    if len(sys.argv) > 1:
        bbmodel.preview(M + NAME + "_item", sys.argv[1], views=((225, 25), (180, 0), (200, 60)), size=360)


if __name__ == "__main__":
    main()
