import base64
import json
import math
import os

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SOURCE = os.path.join(ROOT, "art", "blockbench", "steam_turbine.bbmodel")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "criticalmass")
NAME = "steam_turbine"
TEXTURE = "criticalmass:block/" + NAME
NORMALS = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}
CORNERS = {
    "down": ((0, 0, 1), (0, 0, 0), (1, 0, 0), (1, 0, 1)),
    "up": ((0, 1, 0), (0, 1, 1), (1, 1, 1), (1, 1, 0)),
    "north": ((1, 1, 0), (1, 0, 0), (0, 0, 0), (0, 1, 0)),
    "south": ((0, 1, 1), (0, 0, 1), (1, 0, 1), (1, 1, 1)),
    "west": ((0, 1, 0), (0, 0, 0), (0, 0, 1), (0, 1, 1)),
    "east": ((1, 1, 1), (1, 0, 1), (1, 0, 0), (1, 1, 0)),
}
DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.25, 0.25, 0.25]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.12, 0.12, 0.12]},
    "fixed": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [0.25, 0.25, 0.25]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.16, 0.16, 0.16]},
    "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.16, 0.16, 0.16]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.12, 0.12, 0.12]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.12, 0.12, 0.12]},
}


def tidy(value):
    r = round(value, 4)
    return int(r) if r == int(r) else r


def write(path, obj):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=1)
        f.write("\n")


def spin(cube):
    r = cube.get("rotation")
    if isinstance(r, dict):
        return r["axis"], r["angle"], r["origin"]
    if r and any(r):
        assert not r[1] and not r[2], cube["name"]
        return "x", r[0], cube["origin"]
    return "x", 0, [0, 0, 0]


def turned(cube, v, point):
    axis, angle, origin = spin(cube)
    a = math.radians(angle)
    o = origin if point else (0, 0, 0)
    x, y, z = v[0] - o[0], v[1] - o[1], v[2] - o[2]
    if axis == "x":
        x, y, z = x, y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a)
    else:
        x, y, z = x * math.cos(a) + z * math.sin(a), y, -x * math.sin(a) + z * math.cos(a)
    return (x + o[0], y + o[1], z + o[2])


def corners(cube, face):
    return [turned(cube, tuple(cube["to"][i] if pick[i] else cube["from"][i] for i in range(3)), True) for pick in CORNERS[face]]


def corner_uv(face):
    uv = face["uv"]
    k = face.get("rotation", 0) // 90
    out = []
    for i in range(4):
        j = (i + k) % 4
        out.append((uv[0] if j in (0, 1) else uv[2], uv[1] if j in (0, 3) else uv[3]))
    return out


def solve(goal):
    found = []
    for rot in (0, 90, 180, 270):
        c = {}
        for i in range(4):
            c[(i + rot // 90) % 4] = goal[i]
        (a, b), (cc, d) = c[0], c[2]
        if abs(c[1][0] - a) < 1e-6 and abs(c[1][1] - d) < 1e-6 and abs(c[3][0] - cc) < 1e-6 and abs(c[3][1] - b) < 1e-6:
            found.append(((a > cc) + (b > d), rot, [a, b, cc, d]))
    assert found, goal
    return min(found)[1:]


def upright(cube):
    axis, angle, origin = spin(cube)
    out = {"name": cube["name"]}
    out["from"] = [16 - cube["to"][1], cube["from"][0], cube["from"][2]]
    out["to"] = [16 - cube["from"][1], cube["to"][0], cube["to"][2]]
    if angle:
        out["rotation"] = {"angle": angle, "axis": "y", "origin": [16 - origin[1], origin[0], origin[2]]}
    out["faces"] = {}
    for name, normal in NORMALS.items():
        before = next(k for k, n in NORMALS.items() if n == (normal[1], -normal[0], normal[2]))
        face = cube["faces"][before]
        if face.get("texture") is None:
            out["faces"][name] = {"uv": [0, 0, 0, 0], "texture": None}
            continue
        moved = [((16 - p[1], p[0], p[2]), uv) for p, uv in zip(corners(cube, before), corner_uv(face))]
        goal = []
        for p in corners(out, name):
            near = min(moved, key=lambda m: sum((m[0][i] - p[i]) ** 2 for i in range(3)))
            assert sum((near[0][i] - p[i]) ** 2 for i in range(3)) < 1e-6, cube["name"]
            goal.append(near[1])
        rot, uv = solve(goal)
        out["faces"][name] = {"uv": uv, "texture": face["texture"]}
        if rot:
            out["faces"][name]["rotation"] = rot
    return out


def element(cube, scale):
    faces = {}
    for d, face in cube["faces"].items():
        if face.get("texture") is None:
            continue
        g = {"uv": [tidy(v * scale) for v in face["uv"]], "texture": "#0"}
        if face.get("rotation"):
            g["rotation"] = face["rotation"]
        faces[d] = g
    out = {"name": cube["name"], "from": [tidy(v) for v in cube["from"]], "to": [tidy(v) for v in cube["to"]]}
    assert all(-16 <= v <= 32 for v in out["from"] + out["to"]), cube["name"]
    axis, angle, origin = spin(cube)
    if angle:
        assert angle in (-45, -22.5, 22.5, 45), cube["name"]
        out["rotation"] = {"angle": angle, "axis": axis, "origin": [tidy(v) for v in origin]}
    out["faces"] = faces
    return out


def main():
    project = json.load(open(SOURCE, encoding="utf-8"))
    scale = 16.0 / project["textures"][0].get("uv_width", project["resolution"]["width"])
    still = [c for c in project["elements"] if not c["name"].startswith(("rotor", "shaft"))]
    moving = [c for c in project["elements"] if c["name"].startswith(("rotor", "shaft"))]
    with open(os.path.join(ASSETS, "textures", "block", NAME + ".png"), "wb") as f:
        f.write(base64.b64decode(project["textures"][0]["source"].split(",", 1)[1]))
    textures = {"0": TEXTURE, "particle": TEXTURE}
    housing = {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": textures, "elements": [element(c, scale) for c in still]}
    rotor = {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": textures, "elements": [element(upright(c), scale) for c in moving]}
    whole = {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": textures, "elements": [element(c, scale) for c in still + moving], "display": DISPLAY}
    write(os.path.join(ASSETS, "models", "block", NAME + ".json"), housing)
    write(os.path.join(ASSETS, "models", "block", NAME + "_rotor.json"), rotor)
    write(os.path.join(ASSETS, "models", "item", NAME + ".json"), whole)
    print(len(still), "fixed cubes,", len(moving), "rotor cubes")


if __name__ == "__main__":
    main()
