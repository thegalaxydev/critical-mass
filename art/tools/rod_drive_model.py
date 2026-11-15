import os
import sys

import bbmodel

NAME = "control_rod_drive"
COG_LIFT = 7


def main():
    parts, textures = bbmodel.load(NAME)
    assert parts.get("body"), "the model needs a group called body"
    body = parts["body"]
    particle = textures["0"]
    block = os.path.join(bbmodel.ASSETS, "models", "block")
    bbmodel.write(os.path.join(block, NAME + ".json"), bbmodel.model(body, textures, particle))
    cog, cog_textures = bbmodel.borrowed("create:block/cogwheel_shaftless", "cog")
    shaft, shaft_textures = bbmodel.borrowed("create:block/shaft_half", "shaft")
    shown = body + [bbmodel.lifted(e, COG_LIFT) for e in cog] + shaft + [bbmodel.turned(e) for e in shaft]
    both = dict(textures)
    both.update(cog_textures)
    both.update(shaft_textures)
    bbmodel.write(os.path.join(block, NAME + "_item.json"), bbmodel.model(shown, both, particle))
    bbmodel.write(os.path.join(bbmodel.ASSETS, "models", "item", NAME + ".json"), {"parent": "criticalmass:block/" + NAME + "_item"})
    print(len(body), "body cubes,", len(shown), "in the item model")
    if len(sys.argv) > 1:
        bbmodel.preview("criticalmass:block/" + NAME + "_item", sys.argv[1])


if __name__ == "__main__":
    main()
