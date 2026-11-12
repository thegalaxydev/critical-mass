import json
import os

from PIL import Image

from handmade import AMBER, BRASS, COPPER, GREEN, IRON, OUT, RED, STEEL, bevel, put, rect
from handmade_plant import opening
from handmade_rest import casing, plate

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
RES = os.path.join(ROOT, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "criticalmass")
DATA = os.path.join(RES, "data")
M = "criticalmass:"
ROTATIONS = {"north": 0, "east": 90, "south": 180, "west": 270}
QUADS = {"a": [0, 0, 8, 8], "b": [8, 0, 16, 8], "c": [0, 8, 8, 16], "d": [8, 8, 16, 16]}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n", encoding="utf8") as f:
        json.dump(obj, f, indent=1)
        f.write("\n")


def cube(name, faces, render_type=None):
    model = {"parent": "minecraft:block/block", "textures": {"0": M + "block/" + name, "particle": M + "block/" + name},
             "elements": [{"from": [0, 0, 0], "to": [16, 16, 16],
                           "faces": {d: {"uv": QUADS[q], "texture": "#0", "cullface": d} for d, q in faces.items()}}]}
    if render_type:
        model["render_type"] = render_type
    return model


def horizontal(name):
    return {"variants": {"facing=" + d: ({"model": M + "block/" + name, "y": y} if y else {"model": M + "block/" + name}) for d, y in ROTATIONS.items()}}


def loot(name):
    return {"type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": M + name}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": "criticalmass:blocks/" + name}


def atlas(quads):
    img = Image.new("RGBA", (32, 32))
    for key, painter in quads.items():
        u0, v0, _, _ = QUADS[key]
        img.paste(painter(16, 16), (u0 * 2, v0 * 2))
    return img


def generator_texture():
    def front(w, h):
        img = casing(IRON, STEEL)(w, h)
        for y in (4, 7, 10):
            rect(img, 4, y, 11, y, COPPER[3])
            put(img, 4, y, COPPER[5])
            put(img, 11, y, COPPER[1])
        put(img, 12, 12, AMBER[3])
        put(img, 3, 12, BRASS[4])
        return img

    def back(w, h):
        img = casing(IRON, STEEL)(w, h)
        opening(img, 4, 4, 8, BRASS, IRON)
        return img

    def side(w, h):
        img = casing(IRON, STEEL)(w, h)
        for x in range(3, 13):
            ramp = COPPER if x % 2 else STEEL
            for y in range(4, 12):
                put(img, x, y, ramp[4 if y == 4 else 1 if y == 11 else 3] if x % 2 else STEEL[2])
        rect(img, 3, 7, 12, 8, IRON[2])
        return img

    def top(w, h):
        img = plate(IRON, 3)(w, h)
        for x in range(3, 13, 2):
            rect(img, x, 3, x, 12, IRON[1])
            rect(img, x + 1, 3, x + 1, 12, IRON[4])
        return img
    return atlas({"a": front, "b": back, "c": side, "d": top})


BLOCKS = {
    "generator": {
        "lang": "Generator",
        "texture": generator_texture,
        "model": lambda n: cube(n, {"north": "a", "south": "b", "east": "c", "west": "c", "up": "d", "down": "d"}),
        "state": horizontal,
    },
}

LANG = {
    "criticalmass.generator.load": "Electrical load",
    "criticalmass.generator.breaker_open": "Breaker open: full redstone signal",
    "criticalmass.goggles.generator": "Generator",
    "criticalmass.goggles.generator_output": "Output: %s FE/t (set to %s)",
    "criticalmass.goggles.generator_stored": "Buffer: %s / %s FE",
}


def lang(entries):
    path = os.path.join(ASSETS, "lang", "en_us.json")
    data = json.load(open(path, encoding="utf8"))
    changed = False
    for k, v in entries.items():
        if data.get(k) != v:
            data[k] = v
            changed = True
    if changed:
        with open(path, "w", newline="\n", encoding="utf8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")


def pickaxe(names):
    path = os.path.join(DATA, "minecraft", "tags", "block", "mineable", "pickaxe.json")
    data = json.load(open(path, encoding="utf8"))
    for n in names:
        if M + n not in data["values"]:
            data["values"].append(M + n)
    with open(path, "w", newline="\n", encoding="utf8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def main():
    entries = dict(LANG)
    for name, d in BLOCKS.items():
        if d.get("texture"):
            d["texture"]().save(os.path.join(OUT, name + ".png"))
        for model_name, model in (d.get("models") or {name: d["model"](name)}).items():
            write(os.path.join(ASSETS, "models", "block", model_name + ".json"), model)
        write(os.path.join(ASSETS, "blockstates", name + ".json"), d["state"](name))
        write(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": M + "block/" + d.get("item", name)})
        write(os.path.join(DATA, "criticalmass", "loot_table", "blocks", name + ".json"), loot(name))
        entries["block.criticalmass." + name] = d["lang"]
    pickaxe(BLOCKS)
    lang(entries)
    print("plant blocks:", ", ".join(BLOCKS))


if __name__ == "__main__":
    main()
