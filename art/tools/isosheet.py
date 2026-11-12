import os
import sys
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import iso

Y_TO_X = lambda c: (c[1], 16 - c[0], c[2])
Y_TO_Z = lambda c: (c[0], 16 - c[2], c[1])


def views(name, refs, yaws=(225, 45, 135, 315), size=256):
    return [(name + " yaw " + str(y), iso.render(refs, size, y)) for y in yaws]


def sheet(entries, out, cols=4, bg=(46, 48, 53)):
    w = max(i.width for _, i in entries)
    h = max(i.height for _, i in entries) + 18
    rows = (len(entries) + cols - 1) // cols
    S = Image.new("RGBA", (cols * w, rows * h), bg + (255,))
    d = ImageDraw.Draw(S)
    for n, (label, im) in enumerate(entries):
        x, y = (n % cols) * w, (n // cols) * h
        S.alpha_composite(im, (x, y))
        d.text((x + 6, y + h - 16), label, fill=(225, 220, 205, 255))
    S.save(out)
    print(out, S.size)


if __name__ == "__main__":
    out = sys.argv[1]
    which = sys.argv[2] if len(sys.argv) > 2 else "slice"
    E = []
    if which == "slice":
        E += views("casing", ["criticalmass:block/reactor_vessel_casing"], (225, 45))
        E += views("pump", ["criticalmass:block/circulation_pump", ("criticalmass:block/circulation_pump_rotor", Y_TO_X)])
        E += views("pump vertical-shaft", ["criticalmass:block/circulation_pump_vertical", "criticalmass:block/circulation_pump_rotor"], (225, 45))
        E += views("turbine", ["criticalmass:block/steam_turbine", ("criticalmass:block/steam_turbine_rotor", Y_TO_X)])
    if which == "rest":
        names = ["reactor_controller", "fuel_channel", "control_channel", "coolant_channel", "reactor_service_port", "primary_coolant_port_inlet", "primary_coolant_port_outlet",
                 "heat_exchanger", "condenser", "cooling_tower_packing", "radiator_panel", "relief_valve", "boiler_heating_plate",
                 "shielding_concrete", "shielded_glass", "shielded_hatch", "spent_fuel_rack", "shielded_cask", "reactor_signal_interface",
                 "docking_service_connector", "docking_service_connector_connected"]
        for n in names:
            E.append((n, iso.render(["criticalmass:block/" + n], 200, 45)))
        E.append(("control_rod_drive", iso.render(["criticalmass:block/control_rod_drive", ("criticalmass:block/control_rod_drive_pinion", lambda c: (c[0], 16 - c[2], c[1]))], 200, 45)))
        E.append(("control_rod_drive back", iso.render(["criticalmass:block/control_rod_drive", ("criticalmass:block/control_rod_drive_pinion", lambda c: (c[0], 16 - c[2], c[1]))], 200, 225)))
    sheet(E, out, cols=6 if which == "rest" else 4)
