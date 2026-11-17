import os
import sys

import bbmodel

NAME = "reactor_viewport"
M = "criticalmass:block/"
STAGES = 3


def cracked(stage):
    sheet = "%s%s_crack_%d" % (M, NAME, stage)
    return {"parent": M + NAME, "textures": {"0": sheet, "particle": sheet}}


def main():
    parts, textures = bbmodel.load(NAME)
    for group in ("frame", "glass"):
        assert parts.get(group), "the model needs a group called " + group
    block = os.path.join(bbmodel.ASSETS, "models", "block")
    bbmodel.write(os.path.join(block, NAME + ".json"), bbmodel.model(parts["frame"] + parts["glass"], textures, textures["0"], render_type="minecraft:translucent"))
    variants = {"crack=0": {"model": M + NAME}}
    for stage in range(1, STAGES + 1):
        bbmodel.write(os.path.join(block, "%s_crack_%d.json" % (NAME, stage)), cracked(stage))
        variants["crack=%d" % stage] = {"model": "%s%s_crack_%d" % (M, NAME, stage)}
    bbmodel.write(os.path.join(bbmodel.ASSETS, "blockstates", NAME + ".json"), {"variants": variants})
    bbmodel.write(os.path.join(bbmodel.ASSETS, "models", "item", NAME + ".json"), {"parent": M + NAME})
    print(len(parts["frame"]), "frame cubes and", len(parts["glass"]), "pane")
    if len(sys.argv) > 1:
        bbmodel.preview(M + NAME, sys.argv[1], views=((225, 25), (180, 0), (205, 10)), size=300)


if __name__ == "__main__":
    main()
