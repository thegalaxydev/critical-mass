import os
import sys

from PIL import Image

import create_style
from plant_style import BLOCK

SHEETS = ("spent_fuel_rack", "reactor_controller", "control_channel", "coolant_channel", "fuel_channel", "fuel_channel_glow_1", "fuel_channel_glow_2", "fuel_channel_glow_3",
          "reactor_signal_interface", "pressurizer", "electric_motor", "shielded_cask", "generator", "diesel_engine", "circuit_breaker", "transfer_switch")


def main():
    if "fresh" not in sys.argv:
        print("left alone; pass fresh to add the sheen to the flat panels of", ", ".join(SHEETS))
        return
    wanted = [name for name in sys.argv[1:] if name in SHEETS] or SHEETS
    for name in wanted:
        path = os.path.join(BLOCK, name + ".png")
        img = Image.open(path).convert("RGBA")
        print(name, create_style.enliven(img), "pixels")
        img.save(path)


if __name__ == "__main__":
    main()
