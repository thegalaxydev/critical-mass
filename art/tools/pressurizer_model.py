import os
import sys

import bbmodel

NAME = "pressurizer"
M = "criticalmass:block/"
GUI = {"gui": {"rotation": [30, 225, 0], "translation": [0, -2.5, 0], "scale": [0.4, 0.4, 0.4]},
       "ground": {"translation": [0, 2, 0], "scale": [0.2, 0.2, 0.2]},
       "fixed": {"translation": [0, -3, 0], "scale": [0.35, 0.35, 0.35]},
       "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
       "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.3, 0.3, 0.3]},
       "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.3, 0.3, 0.3]}}


def main():
    parts, textures = bbmodel.load(NAME)
    for group in ("base", "top"):
        assert parts.get(group), "the model needs a group called " + group
    particle = textures["0"]
    block = os.path.join(bbmodel.ASSETS, "models", "block")
    lowered = [bbmodel.lifted(e, -16) for e in parts["top"]]
    bbmodel.write(os.path.join(block, NAME + ".json"), bbmodel.model(parts["base"], textures, particle))
    bbmodel.write(os.path.join(block, NAME + "_top.json"), bbmodel.model(lowered, textures, particle))
    bbmodel.write(os.path.join(block, NAME + "_item.json"), bbmodel.model(parts["base"] + parts["top"], textures, particle, display=GUI))
    bbmodel.write(os.path.join(bbmodel.ASSETS, "models", "item", NAME + ".json"), {"parent": M + NAME + "_item"})
    states = os.path.join(bbmodel.ASSETS, "blockstates")
    bbmodel.write(os.path.join(states, NAME + ".json"), {"variants": {"": {"model": M + NAME}}})
    bbmodel.write(os.path.join(states, NAME + "_top.json"), {"variants": {"": {"model": M + NAME + "_top"}}})
    print(len(parts["base"]), "base cubes,", len(parts["top"]), "top cubes")
    if len(sys.argv) > 1:
        bbmodel.preview(M + NAME + "_item", sys.argv[1], views=((225, 20), (180, 0)), size=420, tall=True)


if __name__ == "__main__":
    main()
