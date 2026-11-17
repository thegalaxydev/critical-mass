import os
import sys

import bbmodel

NAME = "water_cannon"
M = "criticalmass:block/"


def main():
    parts, textures = bbmodel.load(NAME)
    for group in ("base", "head"):
        assert parts.get(group), "the model needs a group called " + group
    particle = textures["0"]
    block = os.path.join(bbmodel.ASSETS, "models", "block")
    bbmodel.write(os.path.join(block, NAME + ".json"), bbmodel.model(parts["base"], textures, particle))
    head = bbmodel.model(parts["head"], textures, particle)
    head.pop("parent")
    bbmodel.write(os.path.join(block, NAME + "_head.json"), head)
    bbmodel.write(os.path.join(block, NAME + "_item.json"), bbmodel.model(parts["base"] + parts["head"], textures, particle))
    bbmodel.write(os.path.join(bbmodel.ASSETS, "models", "item", NAME + ".json"), {"parent": M + NAME + "_item"})
    bbmodel.write(os.path.join(bbmodel.ASSETS, "blockstates", NAME + ".json"), {"variants": {"": {"model": M + NAME}}})
    print(len(parts["base"]), "base cubes,", len(parts["head"]), "head cubes")
    if len(sys.argv) > 1:
        bbmodel.preview(M + NAME + "_item", sys.argv[1], views=((225, 25), (135, 20), (180, 0), (200, 60)), size=360, tall=True)


if __name__ == "__main__":
    main()
