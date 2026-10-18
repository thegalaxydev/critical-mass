import os, sys
from PIL import Image, ImageDraw
import iso
from qa_sheets import block_refs
ART = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "art")
names = sys.argv[2:]
out = sys.argv[1]
cells = []
for n in names:
    if n.startswith("item:"):
        k = n[5:]
        a = Image.open(os.path.join(ART, "blockout", "item__" + k + ".png")).convert("RGBA").resize((160, 160), Image.NEAREST)
        b = Image.open(os.path.join(ART, "final", "item__" + k + ".png")).convert("RGBA").resize((160, 160), Image.NEAREST)
        cells += [(k + " blockout", a), (k + " final", b)]
    else:
        refs = {"primary_coolant_port": ["criticalmass:block/primary_coolant_port_outlet"]}.get(n, block_refs(n))
        cells += [(n + " 225", iso.render(refs, 180, 225)), (n + " 45", iso.render(refs, 180, 45))]
w, h = 180, 198
cols = 4
rows = (len(cells) + cols - 1) // cols
S = Image.new("RGBA", (cols * w, rows * h), (46, 48, 53, 255))
d = ImageDraw.Draw(S)
for i, (l, im) in enumerate(cells):
    x, y = (i % cols) * w, (i // cols) * h
    S.alpha_composite(im, (x + (w - im.width) // 2, y))
    d.text((x + 4, y + h - 15), l, fill=(225, 220, 205, 255))
S.save(out)
