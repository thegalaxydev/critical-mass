import os
import sys
from PIL import Image, ImageChops, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import iso
import specs
import gen
from isosheet import Y_TO_X, Y_TO_Z

MOD = gen.MOD
SHOTS = os.path.join(MOD, "run-inspect", "screenshots")
OUT = os.path.join(MOD, "docs", "art", "qa")
BG = (46, 48, 53)
INK = (225, 220, 205, 255)


def grid(entries, out, cols, title=None, bg=BG):
    w = max(i.width for _, i in entries)
    h = max(i.height for _, i in entries) + 18
    top = 26 if title else 0
    rows = (len(entries) + cols - 1) // cols
    S = Image.new("RGBA", (cols * w, rows * h + top), bg + (255,))
    d = ImageDraw.Draw(S)
    if title:
        d.text((8, 8), title, fill=INK)
    for n, (label, im) in enumerate(entries):
        x, y = (n % cols) * w, (n // cols) * h + top
        S.alpha_composite(im.convert("RGBA"), (x + (w - im.width) // 2, y))
        d.text((x + 6, y + h - 16), label, fill=INK)
    S.convert("RGB").save(os.path.join(OUT, out))
    print(out, S.size)


def shot(name, scale=0.5):
    p = os.path.join(SHOTS, "showroom_" + name + ".png")
    im = Image.open(p).convert("RGBA")
    return im.resize((int(im.width * scale), int(im.height * scale)), Image.LANCZOS)


def diff(a, b, scale=0.5):
    A = Image.open(os.path.join(SHOTS, "showroom_" + a + ".png")).convert("RGB")
    B = Image.open(os.path.join(SHOTS, "showroom_" + b + ".png")).convert("RGB")
    m = ImageChops.difference(A, B).convert("L").point(lambda v: 255 if v > 24 else 0)
    base = A.convert("L").convert("RGB").point(lambda v: v // 3)
    red = Image.new("RGB", A.size, (255, 60, 40))
    out = Image.composite(red, base, m).convert("RGBA")
    return out.resize((int(out.width * scale), int(out.height * scale)), Image.LANCZOS)


def block_refs(name):
    rot = {"circulation_pump": [("criticalmass:block/circulation_pump_rotor", Y_TO_X)], "steam_turbine": [("criticalmass:block/steam_turbine_rotor", Y_TO_X)],
           "control_rod_drive": [("criticalmass:block/control_rod_drive_pinion", Y_TO_Z)]}
    base = {"primary_coolant_port": "primary_coolant_port_inlet"}.get(name, name)
    return ["criticalmass:block/" + base] + rot.get(name, [])


def item_tex(n):
    return Image.open(os.path.join(gen.RES, "textures", "item", n + ".png")).convert("RGBA")


def items_native():
    names = list(specs.ITEM_DEFS)
    bgs = [(139, 139, 139), (55, 55, 55), (198, 198, 198)]
    cell = 18
    S = Image.new("RGB", (len(names) * cell + 8, len(bgs) * cell + 8), BG)
    for r, bg in enumerate(bgs):
        for i, n in enumerate(names):
            x, y = 4 + i * cell, 4 + r * cell
            S.paste(bg, (x, y, x + 18, y + 18))
            S.paste(item_tex(n), (x + 1, y + 1), item_tex(n))
    S.save(os.path.join(OUT, "items_native_1x.png"))
    S.resize((S.width * 3, S.height * 3), Image.NEAREST).save(os.path.join(OUT, "items_native_3x.png"))
    grid([(n, item_tex(n).resize((128, 128), Image.NEAREST)) for n in names], "items_enlarged.png", 6, "Items, 16x16 at 8x")


def main(which):
    os.makedirs(OUT, exist_ok=True)
    if which in ("all", "blocks"):
        grid([(n, iso.render(block_refs(n), 160, 45)) for n in specs.BLOCK_DEFS], "blocks_iso.png", 6, "All 29 blocks, iso at the in-game inventory angle (front on the right face)")
        faces = []
        for n in ("circulation_pump", "steam_turbine", "control_rod_drive", "primary_coolant_port", "reactor_controller", "relief_valve", "reactor_service_port", "heat_exchanger"):
            for y in (225, 45, 135, 315):
                faces.append(("%s %d" % (n, y), iso.render(block_refs(n), 150, y)))
        grid(faces, "functional_faces.png", 4, "Functional faces: four views each (shafts, ports, fronts)")
        states = [("port inlet", ["criticalmass:block/primary_coolant_port_inlet"]), ("port outlet", ["criticalmass:block/primary_coolant_port_outlet"])]
        grid([(l, iso.render(r, 160, 45)) for l, r in states], "block_states.png", 6, "Block states (shape and colour both change)")
    if which in ("all", "items"):
        items_native()
        life = ["empty_fuel_assembly", "fuel_assembly", "spent_fuel_assembly", "damaged_fuel_assembly"]
        grid([(n, item_tex(n).resize((160, 160), Image.NEAREST)) for n in life] + [(n + " (gray)", item_tex(n).convert("LA").convert("RGBA").resize((160, 160), Image.NEAREST)) for n in life],
             "fuel_lifecycle.png", 4, "Fuel assembly lifecycle: colour and greyscale (states must read without colour)")
    if which in ("all", "outfit"):
        arm = [(f, Image.open(os.path.join(gen.RES, "textures", "models", "armor", f + ".png")).convert("RGBA").resize((384, 192), Image.NEAREST)) for f in ("protective_layer_1", "protective_layer_2")]
        grid(arm, "outfit_layers.png", 2, "Armor layers 64x32 at 6x")
        grid([(n, shot("worn_" + n)) for n in ("front", "back", "sneak", "walk", "ride", "swim")] + [("stands front/back", shot("outfit_front")), ("stand back", shot("outfit_back"))],
             "outfit_worn.png", 4, "Protective outfit worn: poses")
        grid([("first person meter + dosimeter", shot("fp_meter_dosimeter")), ("first person fuel", shot("fp_fuel"))], "first_person.png", 2)
    if which in ("all", "game"):
        grid([("9 pumps on 1 water wheel (overstressed)", shot("overstress_a")), ("diff over 8 ticks: nothing moves", diff("overstress_a", "overstress_b")),
              ("relieved: pumps removed", shot("relieved_a")), ("diff over 8 ticks: hubs move", diff("relieved_a", "relieved_b"))], "stopped_vs_operating.png", 2, "Stopped vs operating (red = changed pixels)")
        kin = []
        for n in ("pumps", "turbines", "roddrives", "pipes"):
            kin += [(n, shot("kin_" + n + "_a")), (n + " diff", diff("kin_" + n + "_a", "kin_" + n + "_b"))]
        grid(kin, "kinetics.png", 2, "Kinetic blocks on creative motors (red = rotating pixels over 6 ticks)")
        grid([("installation", shot("install_a")), ("diff", diff("install_a", "install_b")), ("front", shot("install_front_a")), ("front diff", diff("install_front_a", "install_front_b"))],
             "installation.png", 2, "Small reactor installation")
        grid([("assembled body", shot("ship_before_a")), ("before assembly: diff over 6 ticks", diff("ship_before_a", "ship_before_b")), ("resting tilted on a ridge", shot("ship_tilted_a")),
              ("tilted: diff over 7 ticks", diff("ship_tilted_a", "ship_tilted_b")), ("tilted, other side", shot("ship_tilted_side_a")), ("other side diff", diff("ship_tilted_side_a", "ship_tilted_side_b"))],
             "aeronautics_body.png", 2, "Sable/Aeronautics sub-level: blocks and rotation on a moving, tilted body")
        grid([("tanks", shot("fluid_tanks_a"))] + [(n, Image.open(os.path.join(gen.RES, "textures", "fluid", n + "_still.png")).convert("RGBA").crop((0, 0, 16, 16)).resize((160, 160), Image.NEAREST)) for n in specs.FLUID_LANG],
             "fluids.png", 4, "Fluids in Create tanks and still frame 0")
        grid([("row %d" % i, shot("row_%02d" % i)) for i in range(8)], "world_rows.png", 2, "Every block state placed in world")
        grid([(os.path.basename(f)[9:-4], shot(os.path.basename(f)[9:-4])) for f in sorted(os.listdir(SHOTS)) if f.startswith("showroom_hotbar_") for f in [os.path.join(SHOTS, f)]], "hotbars.png", 3, "Hotbar icons in game")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "all")
