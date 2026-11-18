import os
import sys

from PIL import Image

from plant_style import BLOCK

ORIGINAL = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "originals", "fallout_ash_glow.png")
FRAME_TIME = 6


def specks():
    frame = Image.open(ORIGINAL).convert("RGBA")
    assert frame.size == (16, 16), "the ash glow frame should be 16 by 16"
    lit = [((x, y), frame.getpixel((x, y))[:3]) for y in range(16) for x in range(16) if frame.getpixel((x, y))[3]]
    tones = sorted(set(c for _, c in lit), key=lambda c: c[0] * 0.3 + c[1] * 0.59 + c[2] * 0.11)
    return lit, tones


def glow():
    lit, tones = specks()
    top = len(tones) - 1
    period = max(1, 2 * top)
    sheet = Image.new("RGBA", (16, 16 * period))
    for i, ((x, y), colour) in enumerate(lit):
        at = tones.index(colour)
        start = at if i % 2 == 0 else (period - at) % period
        for frame in range(period):
            p = (start + frame) % period
            sheet.putpixel((x, y + 16 * frame), tones[p if p <= top else period - p] + (255,))
    return sheet, len(lit), period


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to animate the fallout ash glow again from the author's frame in art/originals")
        return
    sheet, count, frames = glow()
    sheet.save(os.path.join(BLOCK, "fallout_ash_glow.png"))
    with open(os.path.join(BLOCK, "fallout_ash_glow.png.mcmeta"), "w", encoding="utf8", newline="\n") as f:
        f.write('{\n "animation": {\n  "frametime": %d,\n  "interpolate": true\n }\n}\n' % FRAME_TIME)
    print("animated the ash glow:", count, "specks over", frames, "frames")


if __name__ == "__main__":
    main()
