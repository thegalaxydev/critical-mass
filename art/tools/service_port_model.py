import os
import sys

import bbmodel

NAME = "reactor_service_port"
M = "criticalmass:block/"
FACING = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "south": {"x": 90, "y": 180}, "east": {"x": 90, "y": 90}, "west": {"x": 90, "y": 270}}


def main():
    parts, textures = bbmodel.load(NAME)
    for group in ("body", "door_west"):
        assert parts.get(group), "the model needs a group called " + group
    particle = textures["1"]
    block = os.path.join(bbmodel.ASSETS, "models", "block")
    west = parts["door_west"]
    east = [bbmodel.turned(e) for e in west]
    bbmodel.write(os.path.join(block, NAME + ".json"), bbmodel.model(parts["body"], textures, particle))
    bbmodel.write(os.path.join(block, NAME + "_door_west.json"), bbmodel.model(west, textures, particle))
    bbmodel.write(os.path.join(block, NAME + "_door_east.json"), bbmodel.model(east, textures, particle))
    bbmodel.write(os.path.join(block, NAME + "_item.json"), bbmodel.model(parts["body"] + west + east, textures, particle))
    bbmodel.write(os.path.join(bbmodel.ASSETS, "models", "item", NAME + ".json"), {"parent": M + NAME + "_item"})
    bbmodel.write(os.path.join(bbmodel.ASSETS, "blockstates", NAME + ".json"),
                  {"variants": {"facing=" + f: dict({"model": M + NAME}, **turn) for f, turn in FACING.items()}})
    print(len(parts["body"]), "body cubes,", len(west), "door cubes")
    if len(sys.argv) > 1:
        bbmodel.preview(M + NAME + "_item", sys.argv[1], views=((225, 35), (135, 35), (180, 89)))


if __name__ == "__main__":
    main()
