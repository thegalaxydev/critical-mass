import os
import sys

import bbmodel

NAME = "spent_fuel_rack"
M = "criticalmass:block/"
CELLS = ((0, 0), (6, 0), (0, 6), (6, 6))


def moved(element, dx, dz):
    out = dict(element)
    out["from"] = [bbmodel.tidy(element["from"][0] + dx), element["from"][1], bbmodel.tidy(element["from"][2] + dz)]
    out["to"] = [bbmodel.tidy(element["to"][0] + dx), element["to"][1], bbmodel.tidy(element["to"][2] + dz)]
    return out


def main():
    parts, textures = bbmodel.load(NAME)
    for group in ("body", "assembly"):
        assert parts.get(group), "the model needs a group called " + group
    particle = textures["1"]
    block = os.path.join(bbmodel.ASSETS, "models", "block")
    stood = [[moved(e, dx, dz) for e in parts["assembly"]] for dx, dz in CELLS]
    bbmodel.write(os.path.join(block, NAME + ".json"), bbmodel.model(parts["body"], textures, particle))
    for i, cubes in enumerate(stood):
        bbmodel.write(os.path.join(block, NAME + "_assembly_%d.json" % (i + 1)), bbmodel.model(cubes, textures, particle))
    bbmodel.write(os.path.join(block, NAME + "_item.json"), bbmodel.model(parts["body"] + stood[0] + stood[3], textures, particle))
    bbmodel.write(os.path.join(bbmodel.ASSETS, "models", "item", NAME + ".json"), {"parent": M + NAME + "_item"})
    multipart = [{"apply": {"model": M + NAME}}]
    for i in range(1, 5):
        multipart.append({"when": {"fill": "|".join(str(n) for n in range(i, 5))}, "apply": {"model": M + NAME + "_assembly_%d" % i}})
    bbmodel.write(os.path.join(bbmodel.ASSETS, "blockstates", NAME + ".json"), {"multipart": multipart})
    print(len(parts["body"]), "body cubes,", len(parts["assembly"]), "assembly cubes in each of", len(CELLS), "cells")
    if len(sys.argv) > 1:
        bbmodel.preview(M + NAME + "_item", sys.argv[1], views=((215, 30), (200, 65), (180, 89)), size=360)


if __name__ == "__main__":
    main()
