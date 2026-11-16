import os
import sys

import bbmodel

NAME = "shielded_cask"
M = "criticalmass:block/"


def main():
    parts, textures = bbmodel.load(NAME)
    for group in ("body", "lid"):
        assert parts.get(group), "the model needs a group called " + group
    particle = textures["1"]
    block = os.path.join(bbmodel.ASSETS, "models", "block")
    bbmodel.write(os.path.join(block, NAME + ".json"), bbmodel.model(parts["body"], textures, particle))
    lid = bbmodel.model(parts["lid"], textures, particle)
    lid.pop("parent")
    bbmodel.write(os.path.join(block, NAME + "_lid.json"), lid)
    bbmodel.write(os.path.join(block, NAME + "_item.json"), bbmodel.model(parts["body"] + parts["lid"], textures, particle))
    bbmodel.write(os.path.join(bbmodel.ASSETS, "models", "item", NAME + ".json"), {"parent": M + NAME + "_item"})
    bbmodel.write(os.path.join(bbmodel.ASSETS, "blockstates", NAME + ".json"), {"variants": {"": {"model": M + NAME}}})
    print(len(parts["body"]), "body cubes,", len(parts["lid"]), "lid cubes")
    if len(sys.argv) > 1:
        bbmodel.preview(M + NAME + "_item", sys.argv[1], views=((215, 25), (200, 60), (180, 0)), size=360)


if __name__ == "__main__":
    main()
