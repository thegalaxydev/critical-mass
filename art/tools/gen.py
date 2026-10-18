import json
import os
import shutil
import sys
import argparse
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import art
import paint
import specs

ROOT = art.ROOT
ART = art.ART
MOD = os.environ.get("CCM_MOD", r"D:\Minecraft\create-critical-mass-1.21.1")
RES = os.path.join(MOD, "src", "main", "resources", "assets", "criticalmass")
NS = "criticalmass"

STYLE = (
    "Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): "
    "crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, "
    "recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, "
    "flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting."
)
UVNOTE = (
    "The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. "
    "Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is "
    "and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. "
    "Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas."
)

STYLE_ITEM = (
    "Minecraft mod inventory item sprite in the exact pixel-art style of the Create mod's items (references attached): a single object, chunky readable silhouette, "
    "dark outline that is a darker shade of each material (not pure black), 4 to 6 shade ramps per material, light from the top-left, crisp hand-placed square pixels, "
    "no anti-aliasing, no text or letters, no background, no drop shadow."
)
ITEM_UVNOTE = (
    "The FIRST attached image is a 16x16 sprite blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE sprite pixel. "
    "Repaint it in place as a finished 16x16 pixel-art sprite on that same coarse grid, keeping the exact silhouette (magenta = transparent, keep it magenta) and the same parts in the same places."
)

ACCENTS = {"steel": ["iron"], "iron": ["steel"], "copper": ["iron"], "brass": [], "fabric": ["rubber"], "rubber": ["fabric"]}


def p(*a):
    return os.path.join(ART, *a)


def ensure_dirs():
    for d in ("blockout", "lock", "template", "generated", "final", "preview"):
        os.makedirs(p(d), exist_ok=True)


def styleboard():
    out = p("styleboard.png")
    T = os.path.join(ROOT, "refs", "create", "assets", "create", "textures", "block")
    A = os.path.join(ROOT, "refs", "aero", "x", "assets", "simulated", "textures", "block")
    A2 = os.path.join(ROOT, "refs", "aero", "x", "assets", "aeronautics", "textures", "block")
    picks = [os.path.join(T, n) for n in ("andesite_casing.png", "brass_casing.png", "copper_casing.png", "gearbox.png", "fluid_tank.png", "fluid_tank_top.png")]
    picks += [os.path.join(A, n) for n in ("iron_gearbox.png",)] + [os.path.join(T, n) for n in ("pump.png", "engine.png", "pipes.png")]
    picks += [os.path.join(A, "swivel_bearing.png"), os.path.join(A2, "propeller_bearing.png")]
    tiles = []
    for f in picks:
        im = Image.open(f).convert("RGBA")
        if im.size[0] == 16:
            im = im.resize((32, 32), Image.NEAREST)
        tiles.append(im.crop((0, 0, 32, 32)))
    cols = 4
    rows = (len(tiles) + cols - 1) // cols
    board = Image.new("RGBA", (cols * 34, rows * 34), (40, 40, 44, 255))
    for i, t in enumerate(tiles):
        board.alpha_composite(t, ((i % cols) * 34 + 1, (i // cols) * 34 + 1))
    board = board.resize((board.width * 8, board.height * 8), Image.NEAREST)
    board.convert("RGB").save(out)
    return out


def build_models():
    models = specs.build_slice() if not hasattr(specs, "build_all") else specs.build_all()
    for m in models.values():
        m.pack()
    return models


def write_models(models):
    md = os.path.join(RES, "models", "block")
    os.makedirs(md, exist_ok=True)
    for name, m in models.items():
        with open(os.path.join(md, name + ".json"), "w", encoding="utf8") as f:
            json.dump(m.to_json(NS), f, indent=1)


def blockouts(models, only=None):
    ensure_dirs()
    done = set()
    for name, m in models.items():
        if m.share or m.atlas in done:
            continue
        if only and m.atlas not in only:
            continue
        done.add(m.atlas)
        c = paint.paint_model(m)
        c.img.save(p("blockout", m.atlas + ".png"))
        c.lock.save(p("lock", m.atlas + ".png"))
        k = art.template_fit(c.img, p("template", m.atlas + ".png"), 30)
        art.preview(p("blockout", m.atlas + ".png"), p("preview", m.atlas + "_blockout.png"), 16 if m.atlas_size <= 32 else 8)
        print("blockout", m.atlas, m.atlas_size, "template scale", k)
    return done


def describe(model):
    return getattr(specs, "DESCRIPTIONS", {}).get(model, "")


def run(atlas, version, note="", force=False, extra_refs=()):
    ensure_dirs()
    tpl = p("template", atlas + ".png")
    board = p("styleboard.png") if os.path.exists(p("styleboard.png")) else styleboard()
    size = Image.open(p("blockout", atlas + ".png")).size
    prompt = "\n".join([STYLE, "", UVNOTE, "", "Texture: %s, %dx%d game pixels." % (atlas.replace("_", " "), size[0], size[1]), describe(atlas), note])
    out = p("generated", "%s-v%d.png" % (atlas, version))
    code, log = art.run_generator(prompt, out, [tpl, board] + list(extra_refs), edit=True, force=force)
    if code != 0:
        print("GENERATION FAILED", atlas, log.encode("ascii", "replace").decode())
        return None
    art.log_source(atlas, prompt, [os.path.relpath(out, ROOT)], atlas)
    return out


def finalize(atlas, generated_png, texture_root="block", cleanup=True):
    orig_src = generated_png
    blk = Image.open(p("blockout", atlas + ".png")).convert("RGBA")
    lock = Image.open(p("lock", atlas + ".png")).convert("L")
    final = p("final", atlas + ".png")
    generated_png = art.crop_fit(generated_png, p("template", atlas + ".png"), p("generated", "_crop_" + atlas + ".png"))
    src = Image.open(generated_png).convert("RGBA")
    W, H = blk.size
    k = max(1, 1024 // max(W, H))
    if src.size != (W * k, H * k):
        src = src.resize((W * k, H * k), Image.LANCZOS)
        tmp = p("generated", "_resized_" + atlas + ".png")
        src.save(tmp)
        generated_png = tmp
    art.finalize(None, blk, lock, generated_png, final, ACCENTS, cleanup=cleanup)
    json.dump({"src": os.path.basename(orig_src)}, open(p("final", atlas + ".json"), "w"))
    art.preview(final, p("preview", atlas + ".png"), 16 if max(W, H) <= 32 else 8)
    dst = os.path.join(RES, "textures", texture_root, atlas + ".png")
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copyfile(final, dst)
    print("final", atlas, "->", dst)
    return final


def item_blockouts(names):
    import items_paint
    ensure_dirs()
    for n in names:
        c = art.Canvas(16, 16)
        items_paint.ITEMS[n](c)
        key = "item__" + n
        c.img.save(p("blockout", key + ".png"))
        c.lock.save(p("lock", key + ".png"))
        art.template_fit(c.img, p("template", key + ".png"), 30)
        art.preview(p("blockout", key + ".png"), p("preview", key + "_blockout.png"), 16)
        print("item blockout", n)


def item_refs():
    T = os.path.join(ROOT, "refs", "create", "assets", "create", "textures", "item")
    out = p("item_styleboard.png")
    if not os.path.exists(out):
        names = ["precision_mechanism", "brass_sheet", "electron_tube", "goggles", "wrench", "copper_diving_helmet", "brass_ingot", "crushed_raw_uranium", "copper_backtank" if False else "sand_paper"]
        tiles = [Image.open(os.path.join(T, n + ".png")).convert("RGBA") for n in names if os.path.exists(os.path.join(T, n + ".png"))]
        board = Image.new("RGBA", (len(tiles) * 18, 18), (60, 62, 66, 255))
        for i, im in enumerate(tiles):
            board.alpha_composite(im, (i * 18 + 1, 1))
        board.resize((board.width * 12, board.height * 12), Image.NEAREST).convert("RGB").save(out)
    return out


def run_item(n, version, note="", force=False):
    import items_paint
    key = "item__" + n
    prompt = chr(10).join([STYLE_ITEM, "", ITEM_UVNOTE, "", "Item: %s." % n.replace("_", " "), items_paint.ITEM_DESCRIPTIONS.get(n, ""), note])
    out = p("generated", "%s-v%d.png" % (key, version))
    code, log = art.run_generator(prompt, out, [p("template", key + ".png"), item_refs()], edit=True, force=force)
    if code != 0:
        print("GENERATION FAILED", n, log.encode("ascii", "replace").decode())
        return None
    art.log_source(key, prompt, [os.path.relpath(out, ROOT)], "item/" + n)
    return out


def finalize_item(n, src):
    import sprite_reduce
    orig_src = src
    key = "item__" + n
    blk = Image.open(p("blockout", key + ".png")).convert("RGBA")
    lock = Image.open(p("lock", key + ".png")).convert("L")
    final = p("final", key + ".png")
    src = art.crop_fit(src, p("template", key + ".png"), p("generated", "_crop_" + key + ".png"))
    s = Image.open(src).convert("RGBA").resize((1024, 1024), Image.NEAREST)
    s.save(p("generated", "_crop_" + key + ".png"))
    art.finalize(None, blk, lock, p("generated", "_crop_" + key + ".png"), final, ACCENTS, cleanup=False)
    json.dump({"src": os.path.basename(orig_src)}, open(p("final", key + ".json"), "w"))
    art.preview(final, p("preview", key + ".png"), 16)
    dst = os.path.join(RES, "textures", "item", n + ".png")
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copyfile(final, dst)
    print("final item", n, "->", dst)


def armor_blockouts():
    import armor_paint
    ensure_dirs()
    for n, fn in (("protective_layer_1", armor_paint.layer1), ("protective_layer_2", armor_paint.layer2)):
        c = fn()
        key = "armor__" + n
        c.img.save(p("blockout", key + ".png"))
        c.lock.save(p("lock", key + ".png"))
        art.template_fit(c.img, p("template", key + ".png"), 16)
        art.preview(p("blockout", key + ".png"), p("preview", key + "_blockout.png"), 8)
        print("armor blockout", n)


def run_armor(n, version, note="", force=False):
    import armor_paint
    key = "armor__" + n
    refs = [p("template", key + ".png")]
    T = os.path.join(ROOT, "refs", "create", "assets", "create", "textures", "models", "armor", "copper_diving_layer_1.png")
    if os.path.exists(T):
        ref = p("armor_styleref.png")
        if not os.path.exists(ref):
            Image.open(T).convert("RGBA").resize((512, 256), Image.NEAREST).save(ref)
        refs.append(ref)
    prompt = chr(10).join([STYLE_ITEM.replace("inventory item sprite", "armor texture sheet"), "", ITEM_UVNOTE.replace("16x16 sprite", "64x32 armor UV sheet").replace("16x16", "64x32"), "", armor_paint.DESCRIPTIONS[n], note])
    out = p("generated", "%s-v%d.png" % (key, version))
    code, log = art.run_generator(prompt, out, refs, edit=True, force=force)
    if code != 0:
        print("GENERATION FAILED", n, log.encode("ascii", "replace").decode())
        return None
    art.log_source(key, prompt, [os.path.relpath(out, ROOT)], "models/armor/" + n)
    return out


def finalize_armor(n, src):
    key = "armor__" + n
    orig_src = src
    blk = Image.open(p("blockout", key + ".png")).convert("RGBA")
    lock = Image.open(p("lock", key + ".png")).convert("L")
    src = art.crop_fit(src, p("template", key + ".png"), p("generated", "_crop_" + key + ".png"))
    s = Image.open(src).convert("RGBA")
    if s.size != (1024, 512):
        s = s.resize((1024, 512), Image.LANCZOS)
        src = p("generated", "_resized_" + key + ".png")
        s.save(src)
    final = p("final", key + ".png")
    art.finalize(None, blk, lock, src, final, ACCENTS, cleanup=False)
    json.dump({"src": os.path.basename(orig_src)}, open(p("final", key + ".json"), "w"))
    art.preview(final, p("preview", key + ".png"), 8)
    dst = os.path.join(RES, "textures", "models", "armor", n + ".png")
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copyfile(final, dst)
    print("final armor", n, "->", dst)


def write_assets():
    import assets
    lang = {"itemGroup.criticalmass.main": "Create: Critical Mass"}
    md = os.path.join(RES, "models", "block")
    for name, d in specs.BLOCK_DEFS.items():
        st = d["state"]
        if st[0] == "simple":
            bs = assets.bs_simple(name)
        elif st[0] == "dir_axis":
            bs = assets.bs_dir_axis(st[1], st[2], st[3], st[4])
        elif st[0] == "h_axis":
            bs = assets.bs_h_axis(st[1])
        elif st[0] == "horizontal":
            bs = assets.bs_horizontal(st[1])
        elif st[0] == "facing":
            bs = assets.bs_facing(st[1])
        else:
            bs = st[1]
        for en, ej in getattr(specs, "EXTRA_MODELS", {}).items():
            assets.write(RES, os.path.join("models", "block", en + ".json"), ej)
        assets.write(RES, os.path.join("blockstates", name + ".json"), bs)
        it = d["item"]
        if it == "block":
            im = {"parent": "%s:block/%s" % (NS, name)}
        elif isinstance(it, tuple) and it[0] == "merged":
            housing = json.load(open(os.path.join(md, it[1] + ".json"), encoding="utf8"))
            rotor = json.load(open(os.path.join(md, it[2] + ".json"), encoding="utf8"))
            im = assets.merged_item_model(housing, rotor)
        else:
            im = {"parent": "%s:block/%s" % (NS, it)}
        assets.write(RES, os.path.join("models", "item", name + ".json"), im)
        lang["block.criticalmass." + name] = d["lang"]
    core = json.load(open(os.path.join(md, "reinforced_pipe_core.json"), encoding="utf8")) if os.path.exists(os.path.join(md, "reinforced_pipe_core.json")) else None
    if core:
        arm = json.load(open(os.path.join(md, "reinforced_pipe_arm.json"), encoding="utf8"))
        item = {"parent": "minecraft:block/block", "textures": core["textures"], "elements": core["elements"] + arm["elements"] + [assets.flip_z(e) for e in arm["elements"]]}
        assets.write(RES, os.path.join("models", "block", "reinforced_pipe_item.json"), item)
    for name, d in specs.ITEM_DEFS.items():
        assets.write(RES, os.path.join("models", "item", name + ".json"), assets.item_generated(name, d.get("handheld", False)))
        lang["item.criticalmass." + name] = d["lang"]
    for k, v in getattr(specs, "FLUID_LANG", {}).items():
        lang["fluid.criticalmass." + k] = v
        lang["fluid_type.criticalmass." + k] = v
    lp = os.path.join(RES, "lang", "en_us.json")
    if os.path.exists(lp):
        for k, v in json.load(open(lp, encoding="utf8")).items():
            if k.startswith("criticalmass."):
                lang.setdefault(k, v)
    assets.write(RES, os.path.join("lang", "en_us.json"), lang)
    print("assets written:", len(specs.BLOCK_DEFS), "blocks,", len(specs.ITEM_DEFS), "items")


def placeholders():
    n = 0
    for f in sorted(os.listdir(p("blockout"))):
        key = f[:-4]
        if not f.endswith(".png") or os.path.exists(p("final", f)) or key.startswith(("armor__", "fluid__")):
            continue
        if key.startswith("item__"):
            dst = os.path.join(RES, "textures", "item", key[6:] + ".png")
        else:
            dst = os.path.join(RES, "textures", "block", f)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copyfile(p("blockout", f), dst)
        n += 1
        print("placeholder", key)
    print(n, "placeholders installed")


def fluid_blockouts():
    import fluids
    ensure_dirs()
    for n in fluids.FLUIDS:
        c = fluids.blockout(n)
        key = "fluid__" + n
        c.img.save(p("blockout", key + ".png"))
        c.lock.save(p("lock", key + ".png"))
        art.template_fit(c.img, p("template", key + ".png"), 30)
        print("fluid blockout", n)


def run_fluid(n, version, force=False):
    import fluids
    key = "fluid__" + n
    prompt = chr(10).join([STYLE.replace("block texture", "fluid texture"), "", ITEM_UVNOTE.replace("sprite", "tile").replace("silhouette", "tile"), "", fluids.FLUIDS[n]["desc"]])
    out = p("generated", "%s-v%d.png" % (key, version))
    code, log = art.run_generator(prompt, out, [p("template", key + ".png"), p("styleboard.png")], edit=True, force=force)
    if code != 0:
        print("GENERATION FAILED", n, log.encode("ascii", "replace").decode())
        return None
    art.log_source(key, prompt, [os.path.relpath(out, ROOT)], "fluid/" + n)
    return out


def finalize_fluid(n, src):
    import fluids
    key = "fluid__" + n
    orig_src = src
    blk = Image.open(p("blockout", key + ".png")).convert("RGBA")
    lock = Image.open(p("lock", key + ".png")).convert("L")
    src = art.crop_fit(src, p("template", key + ".png"), p("generated", "_crop_" + key + ".png"))
    Image.open(src).convert("RGBA").resize((1024, 1024), Image.NEAREST).save(p("generated", "_crop_" + key + ".png"))
    final = p("final", key + ".png")
    art.finalize(None, blk, lock, p("generated", "_crop_" + key + ".png"), final, {}, cleanup=False)
    json.dump({"src": os.path.basename(orig_src)}, open(p("final", key + ".json"), "w"))
    fluids.animate(n, Image.open(final).convert("RGBA"), os.path.join(RES, "textures", "fluid"))
    print("final fluid", n)


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("cmd")
    ap.add_argument("names", nargs="*")
    ap.add_argument("--v", type=int, default=1)
    ap.add_argument("--note", default="")
    ap.add_argument("--force", action="store_true")
    ap.add_argument("--src")
    ap.add_argument("--root", default="block")
    a = ap.parse_args()
    if a.cmd == "styleboard":
        print(styleboard())
    elif a.cmd == "models":
        ms = build_models()
        write_models(ms)
        blockouts(ms, a.names or None)
    elif a.cmd == "generated":
        for n in a.names:
            out = run(n, a.v, a.note, a.force)
            if out:
                finalize(n, out, a.root)
    elif a.cmd == "assets":
        write_assets()
    elif a.cmd == "placeholders":
        placeholders()
    elif a.cmd == "armor":
        armor_blockouts()
    elif a.cmd == "generated_armor":
        for n in a.names:
            out = run_armor(n, a.v, a.note, a.force)
            if out:
                finalize_armor(n, out)
    elif a.cmd == "finalize_armor":
        for n in a.names:
            finalize_armor(n, a.src or p("generated", "armor__%s-v%d.png" % (n, a.v)))
    elif a.cmd == "fluids":
        fluid_blockouts()
    elif a.cmd == "generated_fluid":
        for n in a.names:
            out = run_fluid(n, a.v, a.force)
            if out:
                finalize_fluid(n, out)
    elif a.cmd == "finalize_fluid":
        for n in a.names:
            finalize_fluid(n, a.src or p("generated", "fluid__%s-v%d.png" % (n, a.v)))
    elif a.cmd == "items":
        item_blockouts(a.names)
    elif a.cmd == "generated_item":
        for n in a.names:
            out = run_item(n, a.v, a.note, a.force)
            if out:
                finalize_item(n, out)
    elif a.cmd == "finalize_item":
        for n in a.names:
            finalize_item(n, a.src or p("generated", "item__%s-v%d.png" % (n, a.v)))
    elif a.cmd == "finalize":
        for n in a.names:
            finalize(n, a.src or p("generated", "%s-v%d.png" % (n, a.v)), a.root)
