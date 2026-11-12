import json
import os

from PIL import Image

from handmade import AMBER, BRASS, COPPER, GREEN, IRON, OUT, RED, STEEL, bevel, put, rect
from handmade_plant import opening
from handmade_rest import LEAD, casing, plate

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


def viewport_base():
    img = casing(IRON, STEEL)(16, 16)
    for y in range(3, 13):
        for x in range(3, 13):
            img.putpixel((x, y), (118, 158, 178, 64))
    for k in range(4):
        img.putpixel((4 + k, 7 - k), (206, 232, 242, 120))
        img.putpixel((8 + k, 11 - k), (206, 232, 242, 96))
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        put(img, x, y, IRON[5])
    return img


def viewport_textures():
    base = viewport_base()
    out = {"reactor_viewport": base}
    for stage in (1, 2, 3):
        crack = Image.open(os.path.join(OUT, "casing_crack_%d.png" % stage)).convert("RGBA").resize((16, 16), Image.NEAREST)
        img = base.copy()
        img.alpha_composite(crack)
        out["reactor_viewport_crack_%d" % stage] = img
    return out


def viewport_models():
    models = {}
    for stage in range(4):
        name = "reactor_viewport" if stage == 0 else "reactor_viewport_crack_%d" % stage
        models[name] = {"parent": "minecraft:block/cube_all", "textures": {"all": M + "block/" + name}, "render_type": "minecraft:translucent"}
    return models


def viewport_state(name):
    return {"variants": {"crack=%d" % i: {"model": M + "block/" + (name if i == 0 else "%s_crack_%d" % (name, i))} for i in range(4)}}


PILLAR = {"y": {}, "x": {"x": 90, "y": 90}, "z": {"x": 90}}
WINDOW_DARK = (0x17, 0x19, 0x1c)
GLOWS = {1: {(0x17, 0x19, 0x1c): (14, 44, 84), (0x1e, 0x21, 0x26): (18, 54, 98), (0x23, 0x26, 0x2b): (16, 48, 90), (0xd2, 0xc7, 0xb4): (196, 222, 240), (0x8f, 0x82, 0x73): (112, 150, 190)},
         2: {(0x17, 0x19, 0x1c): (26, 96, 168), (0x1e, 0x21, 0x26): (34, 112, 186), (0x23, 0x26, 0x2b): (30, 104, 176), (0xd2, 0xc7, 0xb4): (232, 246, 255), (0x8f, 0x82, 0x73): (150, 204, 240)},
         3: {(0x17, 0x19, 0x1c): (126, 34, 14), (0x1e, 0x21, 0x26): (146, 44, 16), (0x23, 0x26, 0x2b): (136, 38, 14), (0xd2, 0xc7, 0xb4): (255, 226, 160), (0x8f, 0x82, 0x73): (244, 150, 70)}}


def channel_variants():
    control = Image.open(os.path.join(OUT, "control_channel.png")).convert("RGBA")
    for level in range(5):
        img = control.copy()
        rows = round(10 * level / 4)
        for y in range(3, 13):
            if y == 7:
                continue
            if y - 3 >= rows:
                for x in (4, 5, 7, 8):
                    put(img, x, y, WINDOW_DARK)
            put(img, 11, y, (0xf7, 0xcb, 0x6c))
            put(img, 12, y, (0x9e, 0x69, 0x47))
        marker = 3 + min(9, rows)
        if marker == 7:
            marker = 8
        put(img, 11, marker, (0xff, 0xbe, 0x4a))
        put(img, 12, marker, (0xd8, 0x8a, 0x1c))
        name = "control_channel_rod_%d" % level
        img.save(os.path.join(OUT, name + ".png"))
        write(os.path.join(ASSETS, "models", "block", name + ".json"), {"parent": M + "block/control_channel", "textures": {"0": M + "block/" + name, "particle": M + "block/" + name}})
    write(os.path.join(ASSETS, "blockstates", "control_channel.json"), {"variants": {
        "axis=%s,rod=%d" % (axis, level): dict({"model": M + "block/control_channel_rod_%d" % level}, **rot) for axis, rot in PILLAR.items() for level in range(5)}})
    fuel = Image.open(os.path.join(OUT, "fuel_channel.png")).convert("RGBA")
    for level, table in GLOWS.items():
        img = fuel.copy()
        for y in range(img.height):
            for x in range(img.width):
                c = img.getpixel((x, y))[:3]
                if c in table:
                    put(img, x, y, table[c])
        name = "fuel_channel_glow_%d" % level
        img.save(os.path.join(OUT, name + ".png"))
        write(os.path.join(ASSETS, "models", "block", name + ".json"), {"parent": M + "block/fuel_channel", "textures": {"0": M + "block/" + name, "particle": M + "block/" + name}})
    write(os.path.join(ASSETS, "blockstates", "fuel_channel.json"), {"variants": {
        "axis=%s,glow=%d" % (axis, level): dict({"model": M + "block/" + ("fuel_channel" if level == 0 else "fuel_channel_glow_%d" % level)}, **rot) for axis, rot in PILLAR.items() for level in range(4)}})


def rod_partials():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        band = y % 8 == 0
        put(img, 0, y, BRASS[3] if band else LEAD[4])
        put(img, 1, y, BRASS[1] if band else LEAD[2])
    bevel(img, 4, 0, 7, 1, BRASS, 3)
    bevel(img, 4, 2, 7, 5, BRASS, 3)
    img.save(os.path.join(OUT, "control_rod_shaft.png"))
    tex = {"0": M + "block/control_rod_shaft", "particle": M + "block/control_rod_shaft"}
    side = {"uv": [0, 0, 2, 16], "texture": "#0"}
    write(os.path.join(ASSETS, "models", "block", "control_rod_shaft.json"), {"textures": tex, "elements": [
        {"from": [7, 0, 7], "to": [9, 16, 9], "faces": {d: side for d in ("north", "south", "east", "west")}}]})
    cap_side = {"uv": [4, 0, 8, 2], "texture": "#0"}
    cap_top = {"uv": [4, 2, 8, 6], "texture": "#0"}
    write(os.path.join(ASSETS, "models", "block", "control_rod_cap.json"), {"textures": tex, "elements": [
        {"from": [6, 0, 6], "to": [10, 2, 10], "faces": {"north": cap_side, "south": cap_side, "east": cap_side, "west": cap_side, "up": cap_top, "down": cap_top}}]})


BLOCKS = {
    "reactor_viewport": {
        "lang": "Reactor Viewport",
        "textures": viewport_textures,
        "models": viewport_models,
        "state": viewport_state,
    },
    "generator": {
        "lang": "Generator",
        "texture": generator_texture,
        "model": lambda n: cube(n, {"north": "a", "south": "b", "east": "c", "west": "c", "up": "d", "down": "d"}),
        "state": horizontal,
    },
}

LANG = {
    "criticalmass.goggles.rod_bank": "Rod bank %s (use with an empty hand to change)",
    "criticalmass.rod.bank_set": "Rod bank %s",
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
        for tex_name, tex in (d["textures"]() if d.get("textures") else {}).items():
            tex.save(os.path.join(OUT, tex_name + ".png"))
        for model_name, model in (d["models"]() if d.get("models") else {name: d["model"](name)}).items():
            write(os.path.join(ASSETS, "models", "block", model_name + ".json"), model)
        write(os.path.join(ASSETS, "blockstates", name + ".json"), d["state"](name))
        write(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": M + "block/" + d.get("item", name)})
        write(os.path.join(DATA, "criticalmass", "loot_table", "blocks", name + ".json"), loot(name))
        entries["block.criticalmass." + name] = d["lang"]
    channel_variants()
    rod_partials()
    pickaxe(BLOCKS)
    lang(entries)
    print("plant blocks:", ", ".join(BLOCKS))


if __name__ == "__main__":
    main()
