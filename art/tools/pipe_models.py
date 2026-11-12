import itertools
import json
import os
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass")
NS = "criticalmass"
TEXTURE = NS + ":block/reinforced_pipe"
CONNECTED = NS + ":block/reinforced_pipe_connected"
SIDES = ["north", "east", "south", "west", "up", "down"]
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "down", "down": "up"}
AXES = {"x": ("east", "west"), "y": ("up", "down"), "z": ("north", "south")}


def create_jar():
    base = os.path.join(os.path.expanduser("~"), ".gradle", "caches", "modules-2", "files-2.1", "com.simibubi.create")
    for root, _, files in os.walk(base):
        for f in files:
            if f.startswith("create-1.21.1-") and f.endswith("-slim.jar"):
                return os.path.join(root, f)
    raise SystemExit("Create jar not found in the Gradle cache")


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=1)
        f.write("\n")


def ours(create_model):
    return NS + ":block/reinforced_pipe/" + create_model.split("create:block/fluid_pipe/")[1]


def effective(when):
    fixed = {}
    choices = []
    for side, value in when.items():
        if value == "true":
            choices.append([{side: "true"}, {"rim_" + side: "true"}])
        else:
            fixed[side] = "false"
            fixed["rim_" + side] = "false"
    combos = [dict(fixed)]
    for options in choices:
        combos = [dict(c, **o) for c in combos for o in options]
    return combos[0] if len(combos) == 1 else {"OR": combos}


def multipart():
    with zipfile.ZipFile(create_jar()) as z:
        state = json.loads(z.read("assets/create/blockstates/fluid_pipe.json"))
    parts = []
    for p in state["multipart"]:
        parts.append({"when": effective(p["when"]), "apply": {"model": ours(p["apply"]["model"])}})
    for side in SIDES:
        opp = OPPOSITE[side]
        perps = [d for d in SIDES if d not in (side, opp)]
        plain = []
        for alt in ({opp: "true"}, {"rim_" + opp: "true"}):
            c = {side: "true", "rim_" + side: "false"}
            c.update(alt)
            for d in perps:
                c[d] = "false"
                c["rim_" + d] = "false"
            plain.append(c)
        parts.append({"when": {"OR": plain}, "apply": {"model": "%s:block/reinforced_pipe/connection/%s" % (NS, side)}})
        detailed = [{"rim_" + side: "true"}, {side: "true", opp: "false", "rim_" + opp: "false"}]
        for d in perps:
            detailed.append({side: "true", d: "true"})
            detailed.append({side: "true", "rim_" + d: "true"})
        parts.append({"when": {"OR": detailed}, "apply": {"model": "%s:block/reinforced_pipe/rim_connector/%s" % (NS, side)}})
    for side in SIDES:
        parts.append({"when": {"rim_" + side: "true"}, "apply": {"model": "%s:block/reinforced_pipe/rim/%s" % (NS, side)}})
    casing = []
    for axis in AXES.values():
        around = [d for d in SIDES if d not in axis]
        for three in itertools.combinations(around, 3):
            alt = effective({d: "true" for d in three})
            casing.extend(alt["OR"])
    parts.append({"when": {"OR": casing}, "apply": {"model": "%s:block/reinforced_pipe/casing" % NS}})
    return {"multipart": parts}


def main():
    state = multipart()
    write(os.path.join(ASSETS, "blockstates", "reinforced_pipe.json"), state)
    md = os.path.join(ASSETS, "models", "block")
    for p in state["multipart"]:
        name = p["apply"]["model"].split(NS + ":block/")[1]
        key = "0"
        tex = TEXTURE if name.startswith(("reinforced_pipe/connection/", "reinforced_pipe/rim_connector/", "reinforced_pipe/rim/", "reinforced_pipe/casing")) else CONNECTED
        write(os.path.join(md, name + ".json"), {"parent": "create:block/fluid_pipe/" + name.split("reinforced_pipe/")[1], "textures": {key: tex, "particle": TEXTURE}})
    write(os.path.join(md, "reinforced_pipe", "item.json"), {"parent": "create:block/fluid_pipe/item", "textures": {"1": TEXTURE, "particle": TEXTURE}})
    write(os.path.join(ASSETS, "models", "item", "reinforced_pipe.json"), {"parent": NS + ":block/reinforced_pipe/item"})
    for old in ("reinforced_pipe_core", "reinforced_pipe_arm", "reinforced_pipe_item"):
        p = os.path.join(md, old + ".json")
        if os.path.exists(p):
            os.remove(p)
    ends = os.path.join(md, "reinforced_pipe", "end")
    if os.path.isdir(ends):
        for f in os.listdir(ends):
            os.remove(os.path.join(ends, f))
        os.rmdir(ends)
    print(len(state["multipart"]), "multipart entries")


if __name__ == "__main__":
    main()
