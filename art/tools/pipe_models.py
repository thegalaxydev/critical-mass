import json
import os
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass")
NS = "criticalmass"
TEXTURE = NS + ":block/reinforced_pipe"
CONNECTED = NS + ":block/reinforced_pipe_connected"
SIDES = ["north", "east", "south", "west", "up", "down"]
AXIS = {"north": "z", "south": "z", "east": "x", "west": "x", "up": "y", "down": "y"}
END_UV = [8, 0, 16, 8]


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


def multipart():
    with zipfile.ZipFile(create_jar()) as z:
        state = json.loads(z.read("assets/create/blockstates/fluid_pipe.json"))
    parts = []
    for p in state["multipart"]:
        parts.append({"when": p["when"], "apply": {"model": ours(p["apply"]["model"])}})
    for side in SIDES:
        parts.append({"when": {side: "true"}, "apply": {"model": "%s:block/reinforced_pipe/connection/%s" % (NS, side)}})
    for side in SIDES:
        when = {d: "false" for d in SIDES if AXIS[d] != AXIS[side]}
        when[side] = "false"
        parts.append({"when": when, "apply": {"model": "%s:block/reinforced_pipe/end/%s" % (NS, side)}})
    return {"multipart": parts}


def end_cap(side):
    face = {"uv": END_UV, "texture": "#0"}
    return {"parent": "block/block", "textures": {"0": CONNECTED, "particle": TEXTURE},
            "elements": [{"from": [4, 4, 4], "to": [12, 12, 12], "faces": {side: face}}]}


def main():
    state = multipart()
    write(os.path.join(ASSETS, "blockstates", "reinforced_pipe.json"), state)
    md = os.path.join(ASSETS, "models", "block")
    for p in state["multipart"]:
        name = p["apply"]["model"].split(NS + ":block/")[1]
        if name.startswith("reinforced_pipe/end/"):
            write(os.path.join(md, name + ".json"), end_cap(name.rsplit("/", 1)[1]))
            continue
        key = "0"
        tex = TEXTURE if name.startswith("reinforced_pipe/connection/") else CONNECTED
        write(os.path.join(md, name + ".json"), {"parent": "create:block/fluid_pipe/" + name.split("reinforced_pipe/")[1], "textures": {key: tex, "particle": TEXTURE}})
    write(os.path.join(md, "reinforced_pipe", "item.json"), {"parent": "create:block/fluid_pipe/item", "textures": {"1": TEXTURE, "particle": TEXTURE}})
    write(os.path.join(ASSETS, "models", "item", "reinforced_pipe.json"), {"parent": NS + ":block/reinforced_pipe/item"})
    for old in ("reinforced_pipe_core", "reinforced_pipe_arm", "reinforced_pipe_item"):
        p = os.path.join(md, old + ".json")
        if os.path.exists(p):
            os.remove(p)
    print(len(state["multipart"]), "multipart entries")


if __name__ == "__main__":
    main()
