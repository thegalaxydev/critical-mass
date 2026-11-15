import os
import sys

from PIL import Image

import bbmodel

BLOCK = os.path.join(bbmodel.ASSETS, "models", "block")
M = "criticalmass:block/"
ROD_LEVELS = 4
GLOW_LEVELS = 3


def cut(element, level):
    if level == ROD_LEVELS:
        return dict(element, faces={d: f for d, f in element["faces"].items() if d != "down"})
    frm, to = element["from"], element["to"]
    height = to[1] - frm[1]
    keep = height * level / ROD_LEVELS
    faces = {}
    for d, face in element["faces"].items():
        g = dict(face)
        if d not in ("up", "down"):
            u0, v0, u1, v1 = face["uv"]
            g["uv"] = [u0, v0, u1, bbmodel.tidy(v0 + (v1 - v0) * level / ROD_LEVELS)]
        faces[d] = g
    return {"name": element["name"], "from": [frm[0], bbmodel.tidy(to[1] - keep), frm[2]], "to": to, "faces": faces}


def whole(name, groups):
    parts, textures = bbmodel.load(name)
    for g in groups:
        assert parts.get(g), "%s needs a group called %s" % (name, g)
    return parts, textures


def main():
    parts, textures = whole("fuel_channel", ("plates", "rods"))
    bbmodel.write(os.path.join(BLOCK, "fuel_channel.json"), bbmodel.model(parts["plates"] + parts["rods"], textures, textures["0"]))
    for level in range(1, GLOW_LEVELS + 1):
        name = "fuel_channel_glow_%d" % level
        bbmodel.write(os.path.join(BLOCK, name + ".json"), {"parent": M + "fuel_channel", "textures": {"0": M + name, "particle": M + name}})
    parts, textures = whole("control_channel", ("frame", "rod"))
    for level in range(ROD_LEVELS + 1):
        rod = [cut(e, level) for e in parts["rod"]] if level else []
        bbmodel.write(os.path.join(BLOCK, "control_channel_rod_%d.json" % level), bbmodel.model(parts["frame"] + rod, textures, textures["0"]))
    bbmodel.write(os.path.join(BLOCK, "control_channel.json"), bbmodel.model(parts["frame"] + [cut(e, ROD_LEVELS) for e in parts["rod"]], textures, textures["0"]))
    parts, textures = whole("coolant_channel", ("frame", "tubes"))
    bbmodel.write(os.path.join(BLOCK, "coolant_channel.json"), bbmodel.model(parts["frame"] + parts["tubes"], textures, textures["0"]))
    print("exported the channel models")
    if len(sys.argv) > 1:
        names = ("fuel_channel", "fuel_channel_glow_2", "control_channel_rod_4", "control_channel_rod_2", "control_channel_rod_0", "coolant_channel")
        shots = []
        for name in names:
            path = os.path.join(sys.argv[1], name + "_tmp.png")
            bbmodel.preview(M + name, path, views=((225, 25),), size=300)
            shots.append(Image.open(path).copy())
            os.remove(path)
        sheet = Image.new("RGBA", (300 * len(shots), 300), (88, 110, 74, 255))
        for i, shot in enumerate(shots):
            sheet.alpha_composite(shot, (300 * i, 0))
        sheet.save(os.path.join(sys.argv[1], "channels_preview.png"))


if __name__ == "__main__":
    main()
