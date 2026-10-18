import sys
import json
from PIL import Image, ImageDraw

spec = json.load(open(sys.argv[1], encoding="utf8"))
out = sys.argv[2]
scale = spec.get("scale", 8)
cols = spec.get("cols", 6)
pad = 12
label_h = 16
bg = tuple(spec.get("bg", [40, 42, 46]))
items = []
for entry in spec["items"]:
    path, label = entry[0], entry[1]
    im = Image.open(path).convert("RGBA")
    if len(entry) > 2 and entry[2]:
        x, y, w, h = entry[2]
        im = im.crop((x, y, x + w, y + h))
    s = entry[3] if len(entry) > 3 else scale
    im = im.resize((im.width * s, im.height * s), Image.NEAREST)
    items.append((im, label))
cell_w = max(i.width for i, _ in items) + pad
rows = (len(items) + cols - 1) // cols
row_heights = []
for r in range(rows):
    row = items[r * cols:(r + 1) * cols]
    row_heights.append(max(i.height for i, _ in row) + label_h + pad)
W = cols * cell_w + pad
H = sum(row_heights) + pad
sheet = Image.new("RGBA", (W, H), bg + (255,))
d = ImageDraw.Draw(sheet)
y = pad
for r in range(rows):
    x = pad
    for im, label in items[r * cols:(r + 1) * cols]:
        checker = Image.new("RGBA", im.size, (70, 72, 78, 255))
        cd = ImageDraw.Draw(checker)
        step = max(4, scale)
        for cy in range(0, im.height, step):
            for cx in range(0, im.width, step):
                if (cx // step + cy // step) % 2:
                    cd.rectangle([cx, cy, cx + step - 1, cy + step - 1], fill=(85, 88, 95, 255))
        checker.alpha_composite(im)
        sheet.paste(checker, (x, y))
        d.text((x, y + im.height + 2), label, fill=(230, 225, 210, 255))
        x += cell_w
    y += row_heights[r]
sheet.save(out)
print(out, sheet.size)
