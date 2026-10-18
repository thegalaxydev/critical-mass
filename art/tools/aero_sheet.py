import json
import os
import glob
import sys
from PIL import Image

S = sys.argv[1]
A = os.path.join(S, "refs", "aero", "x", "assets")
pick = [
    "aeronautics/textures/block/propeller_bearing.png", "aeronautics/textures/block/propeller_bearing_side.png",
    "aeronautics/textures/block/gyroscopic_propeller_bearing.png", "aeronautics/textures/block/metal_bearing_plate.png",
    "aeronautics/textures/block/smart_propeller.png", "aeronautics/textures/block/propeller.png",
    "simulated/textures/block/iron_gearbox.png", "simulated/textures/block/physics_assembler.png",
    "simulated/textures/block/swivel_bearing.png", "simulated/textures/block/steering_wheel.png",
    "simulated/textures/block/dock.png", "simulated/textures/block/dock_powered.png",
    "simulated/textures/block/torsion_spring.png", "simulated/textures/block/analog_transmission/block_on.png",
    "simulated/textures/block/altitude_sensor/altitude_sensor.png", "simulated/textures/block/absorber/absorber_base.png",
    "aeronautics/textures/block/steam_vent/steam_vent_base.png", "aeronautics/textures/block/steam_vent/steam_vent_head.png",
    "aeronautics/textures/block/adjustable_burner/adjustable_burner.png",
]
for d in ["simulated/textures/block/portable_engine", "simulated/textures/block/throttle_lever", "simulated/textures/block/rope_winch", "simulated/textures/block/gimbal_sensor"]:
    pick += [os.path.relpath(p, A).replace(os.sep, "/") for p in sorted(glob.glob(os.path.join(A, d, "*.png")))[:2]]
items = []
for p in pick:
    f = os.path.join(A, p)
    if not os.path.exists(f):
        continue
    im = Image.open(f)
    m = max(im.size)
    s = 8 if m <= 32 else 4 if m <= 64 else 2
    items.append([f, p.split("/")[-1] + " " + str(im.size), None, s])
json.dump({"items": items, "cols": 6}, open(os.path.join(S, "qa", "aerosheet.json"), "w"))
print(len(items))
