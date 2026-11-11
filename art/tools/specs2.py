import paint2
from modelgen import Model
from specs import shaft_y

CASE = ("ext", "criticalmass:block/reactor_vessel_casing", [0, 0, 16, 16])
ALL6 = ("north", "south", "east", "west", "up", "down")


def C(key, base=("steel", 3), frame=None, ops=(), outline=True):
    return (key, "comp", {"base": base, "frame": frame, "ops": list(ops), "outline": outline})


def cube_faces(m, faces, frm=(0, 0, 0), to=(16, 16, 16), name=None):
    return m.box(list(frm), list(to), faces, name=name)


def casing_module(name, front):
    m = Model(name)
    cube_faces(m, {"north": front, "south": CASE, "east": CASE, "west": CASE, "up": CASE, "down": CASE})
    return m


def controller():
    return casing_module("reactor_controller", C("front", ("iron", 3), ("iron", 2), [
        ("inset", 2, 2, 13, 13, "brass", 3), ("rect", 4, 3, 11, 8, "ink", 0, True), ("grid", 5, 4, 3, 2, 2, "green", 0),
        ("lamps", 11, [4, 7, 10], ["green", "amber", "red"]), ("bolts", [(1, 1), (14, 1), (1, 14), (14, 14)])]))


def channel(name, content):
    m = Model(name)
    if content == "fuel":
        side = C("side", ("steel", 3), ("iron", 2), [("inset", 4, 1, 11, 14, "steel", 1), ("bars", "v", 5, 10, 2, "ceramic", False, 1, 2, 13), ("rect", 4, 5, 11, 5, "iron", 5), ("rect", 4, 10, 11, 10, "iron", 5)])
        top = C("top", ("steel", 2), ("iron", 2), [("grid", 3, 3, 3, 3, 4, "ceramic", 3)])
    elif content == "control":
        side = C("side", ("steel", 3), ("iron", 2), [("inset", 4, 1, 11, 14, "steel", 0), ("bars", "v", 5, 10, 2, "lead", False, 1, 2, 13), ("rect", 12, 2, 13, 13, "brass", 2), ("px", [(12, 3), (13, 3)], "amber", 3)])
        top = C("top", ("steel", 2), ("iron", 2), [("grid", 3, 3, 3, 3, 4, "lead", 1)])
    else:
        side = C("side", ("steel", 3), ("iron", 2), [("inset", 4, 1, 11, 14, "steel", 1), ("bars", "v", 5, 10, 3, "copper", False, 2, 1, 14)])
        top = C("top", ("steel", 2), ("iron", 2), [("hole", 4, 4, 5, 5, "copper"), ("hole", 10, 4, 11, 5, "copper"), ("hole", 4, 10, 5, 11, "copper"), ("hole", 10, 10, 11, 11, "copper")])
    cube_faces(m, {"north": side, "south": side, "east": side, "west": side, "up": top, "down": top})
    return m


def service_port():
    return casing_module("reactor_service_port", C("front", ("iron", 3), ("iron", 2), [
        ("ring", 8, 8, 6.5, "brass", ("steel", 2)), ("ring", 8, 8, 3.5, "brass", ("steel", 1)), ("rect", 7, 3, 8, 12, "brass", 3),
        ("bolts", [(8, 1), (8, 14), (1, 8), (14, 8)], "iron")]))


def coolant_port(mode):
    col = "coolant" if mode == "inlet" else "red"
    m = Model("primary_coolant_port_" + mode, atlas_name="primary_coolant_port")
    return m, col


def coolant_ports():
    a = Model("primary_coolant_port_inlet", atlas_name="primary_coolant_port")
    b = Model("primary_coolant_port_outlet", share=a)
    for m, col, key in ((a, "coolant", "front_in"), (b, "red", "front_out")):
        front = C(key, ("iron", 3), ("iron", 2), [("rect", 3, 3, 12, 12, "copper", 3), ("rect", 4, 4, 11, 11, col, 2), ("hole", 5, 5, 10, 10), ("bolts", [(3, 3), (12, 3), (3, 12), (12, 12)])])
        cube_faces(m, {"north": front, "south": CASE, "east": CASE, "west": CASE, "up": CASE, "down": CASE})
    side = C("nozzle_side", ("copper", 2), None, [("rect", 0, 0, 7, 0, "copper", 4)], outline=False)
    b.box([4, 4, -2], [12, 12, 0], {"north": C("nozzle", ("copper", 3), ("copper", 1), [("rect", 1, 1, 6, 6, "red", 2), ("hole", 2, 2, 5, 5)]),
                                    "east": side, "west": side, "up": side, "down": side}, name="nozzle")
    return a, b


def pipe_parts():
    core = Model("reinforced_pipe_core", atlas_name="reinforced_pipe")
    arm = Model("reinforced_pipe_arm", share=core)
    end = C("core", ("copper", 3), None, [("rect", 0, 0, 9, 0, "copper", 5), ("rect", 0, 9, 9, 9, "copper", 1), ("rect", 1, 3, 8, 3, "iron", 3), ("rect", 1, 6, 8, 6, "iron", 3)])
    core.box([3, 3, 3], [13, 13, 13], {d: end for d in ALL6}, name="core")
    side_h = C("arm_h", ("copper", 3), None, [("rect", 0, 0, 9, 0, "copper", 5), ("rect", 0, 2, 9, 2, "iron", 4)], outline=False)
    side_v = C("arm_v", ("copper", 3), None, [("rect", 0, 0, 0, 9, "copper", 5), ("rect", 2, 0, 2, 9, "iron", 4)], outline=False)
    arm.box([3, 3, 0], [13, 13, 3], {"north": C("arm_end", ("copper", 3), ("copper", 1), [("hole", 3, 3, 6, 6), ("bolts", [(1, 1), (8, 1), (1, 8), (8, 8)])]),
                                    "east": side_h, "west": side_h, "up": side_v, "down": side_v}, name="arm")
    return core, arm


def heat_exchanger():
    m = Model("heat_exchanger")
    shell = C("shell", ("steel", 3), ("iron", 2), [("rect", 0, 3, 15, 4, "copper", 3), ("rect", 0, 11, 15, 12, "copper", 3), ("bolts", [(2, 3), (13, 3), (2, 11), (13, 11)])])
    top = C("top", ("steel", 3), ("iron", 2), [("ring", 8, 8, 6, "iron", ("steel", 2)), ("bolts", [(8, 2), (8, 13), (2, 8), (13, 8)])])
    prim = C("primary", ("steel", 3), ("iron", 2), [("rect", 2, 4, 6, 11, "copper", 3), ("rect", 3, 5, 5, 10, "red", 2), ("hole", 3, 6, 5, 9),
                                                    ("rect", 9, 4, 13, 11, "copper", 3), ("rect", 10, 5, 12, 10, "coolant", 2), ("hole", 10, 6, 12, 9)])
    sec = C("secondary", ("steel", 3), ("iron", 2), [("rect", 2, 4, 6, 11, "copper", 3), ("rect", 3, 5, 5, 10, "coolant", 3), ("hole", 3, 6, 5, 9),
                                                     ("rect", 9, 4, 13, 11, "iron", 4), ("rect", 10, 5, 12, 10, "steam", 2), ("hole", 10, 6, 12, 9)])
    cube_faces(m, {"north": prim, "south": sec, "east": shell, "west": shell, "up": top, "down": top})
    return m


def condenser():
    m = Model("condenser")
    side = C("side", ("steel", 3), ("iron", 2), [("bars", "h", 3, 12, 2, "copper", False, 1, 2, 13)])
    front = C("front", ("steel", 3), ("iron", 2), [("rect", 3, 2, 12, 8, "iron", 4), ("hole", 5, 3, 10, 7, "iron"), ("rect", 5, 10, 10, 13, "copper", 3), ("rect", 6, 11, 9, 12, "coolant", 2), ("px", [(7, 11), (8, 11)], "ink", 0)])
    back = C("back", ("steel", 3), ("iron", 2), [("rect", 2, 5, 6, 10, "copper", 3), ("rect", 3, 6, 5, 9, "coolant", 2), ("rect", 9, 5, 13, 10, "copper", 3), ("rect", 10, 6, 12, 9, "red", 2)])
    top = C("top", ("steel", 3), ("iron", 2), [("grid", 2, 2, 4, 4, 3, "iron", 2)])
    cube_faces(m, {"north": front, "south": back, "east": side, "west": side, "up": top, "down": top})
    return m


def cooling_tower():
    m = Model("cooling_tower_packing", render_type="minecraft:cutout")
    side = C("side", ("wood", 3), ("andesite_fake", 0) if False else None, [("bars", "h", 0, 15, 3, "wood", True, 2), ("rect", 0, 0, 0, 15, "iron", 3), ("rect", 15, 0, 15, 15, "iron", 3)], outline=False)
    top = C("top", ("wood", 3), None, [("bars", "v", 0, 15, 3, "wood", True, 2), ("rect", 0, 0, 15, 0, "iron", 3), ("rect", 0, 15, 15, 15, "iron", 3)], outline=False)
    cube_faces(m, {"north": side, "south": side, "east": side, "west": side, "up": top, "down": top})
    return m


def radiator():
    m = Model("radiator_panel", render_type="minecraft:cutout")
    fins = C("fins", ("copper", 3), ("brass", 1), [("bars", "v", 1, 14, 2, "copper", False, 1, 1, 14)])
    edge_v = C("edge_v", ("brass", 3), None, [], outline=False)
    edge_h = C("edge_h", ("brass", 3), None, [("rect", 2, 0, 3, 3, "copper", 2), ("rect", 12, 0, 13, 3, "copper", 2)], outline=False)
    m.box([0, 0, 12], [16, 16, 16], {"north": fins, "south": C("back", ("iron", 2), ("iron", 1), []), "east": edge_v, "west": edge_v, "up": edge_h, "down": edge_h}, name="panel")
    return m


def emergency_tank():
    m = Model("emergency_coolant_tank")
    side = C("side", ("steel", 3), ("iron", 2), [("rect", 0, 3, 15, 4, "red", 2), ("rect", 0, 11, 15, 12, "red", 2), ("rect", 6, 5, 9, 10, "glass", 2, True), ("rect", 7, 6, 7, 9, "coolant", 3, True)])
    top = C("top", ("steel", 3), ("iron", 2), [("ring", 8, 8, 6, "red", ("steel", 3)), ("rect", 6, 6, 9, 9, "brass", 3), ("hole", 7, 7, 8, 8)])
    cube_faces(m, {"north": side, "south": side, "east": side, "west": side, "up": top, "down": top})
    return m


def relief_valve():
    m = Model("relief_valve")
    fl_edge = C("flange_edge", ("copper", 2), None, [], outline=False)
    m.box([3, 3, 12], [13, 13, 16], {"south": C("flange", ("copper", 3), ("copper", 1), [("hole", 3, 3, 6, 6), ("bolts", [(1, 1), (8, 1), (1, 8), (8, 8)])]),
                                     "north": C("flange_front", ("copper", 3), ("copper", 1), []), "east": fl_edge, "west": fl_edge, "up": fl_edge, "down": fl_edge}, name="flange")
    body = C("body", ("brass", 3), ("brass", 1), [("rect", 0, 2, 7, 2, "brass", 5)])
    m.box([4, 4, 6], [12, 12, 12], {"north": C("body_front", ("brass", 3), ("brass", 1), [("ring", 4, 4, 3, "iron")]), "east": body, "west": body, "up": body, "down": body}, name="body")
    spring = C("spring", ("iron", 3), None, [("bars", "h", 0, 3, 2, "iron", False)], outline=False)
    m.box([6, 6, 2], [10, 10, 6], {"north": C("cap", ("iron", 4), ("iron", 2), []), "east": spring, "west": spring, "up": spring, "down": spring}, name="spring")
    horn = C("horn", ("iron", 3), None, [], outline=False)
    m.box([7, 10, 3], [9, 14, 5], {"up": C("horn_mouth", ("iron", 2), None, [("rect", 0, 0, 1, 1, "ink", 0, True)], outline=False), "north": horn, "south": horn, "east": C("horn_s", ("iron", 3), None, [], outline=False), "west": C("horn_s", ("iron", 3), None, [], outline=False)}, name="vent")
    return m


def boiler_plate():
    m = Model("boiler_heating_plate")
    top = C("top", ("copper", 2), ("iron", 2), [("bars", "h", 3, 12, 2, "copper", False, 1, 2, 13), ("rect", 2, 2, 13, 2, "iron", 4)])
    side = C("side", ("steel", 3), ("iron", 2), [("rect", 0, 1, 15, 1, "copper", 3)])
    port = C("port", ("steel", 3), ("iron", 2), [("rect", 5, 0, 10, 5, "copper", 3), ("hole", 6, 1, 9, 4)])
    m.box([0, 0, 0], [16, 6, 16], {"up": top, "down": C("bottom", ("steel", 2), ("iron", 1), []), "north": port, "south": port, "east": side, "west": side}, name="plate")
    return m


def concrete():
    m = Model("shielding_concrete")
    f = C("face", ("concrete", 3), None, [("noise", "concrete", [1, 2, 4], 3), ("noise", "iron", [5], 11)], outline=False)
    cube_faces(m, {d: f for d in ALL6})
    return m


def shield_glass():
    m = Model("shielded_glass", render_type="minecraft:translucent")
    f = C("face", ("glass", 3), ("lead", 2), [("bars", "v", 5, 10, 5, "lead", False, 1, 1, 14), ("bars", "h", 5, 10, 5, "lead", False, 1, 1, 14), ("alpha", 170)], outline=False)
    cube_faces(m, {d: f for d in ALL6})
    return m


def hatch():
    m = Model("shielded_hatch")
    f = C("face", ("concrete", 3), ("iron", 2), [("inset", 2, 2, 13, 13, "lead", 3), ("stripe", 7, "hazard", True, 2), ("rect", 6, 3, 9, 4, "brass", 3), ("bolts", [(3, 3), (12, 3), (3, 12), (12, 12)])])
    cube_faces(m, {d: f for d in ALL6})
    return m


def bench():
    m = Model("fuel_assembly_bench")
    top = C("top", ("wood", 3), ("iron", 1), [("rect", 3, 5, 12, 10, "steel", 2), ("rect", 4, 6, 11, 9, "ceramic", 3)])
    side = C("side", ("iron", 3), ("iron", 1), [("inset", 2, 2, 13, 5, "steel", 2)])
    front = C("front", ("iron", 3), ("iron", 1), [("inset", 2, 2, 13, 5, "steel", 2), ("rect", 6, 3, 9, 3, "brass", 4)])
    m.box([0, 10, 0], [16, 16, 16], {"up": top, "down": C("under", ("iron", 2), None, []), "north": front, "south": side, "east": side, "west": side}, name="top")
    leg = C("leg", ("iron", 3), None, [("rect", 0, 0, 0, 9, "iron", 5)], outline=False)
    for (x, z) in ((1, 1), (12, 1), (1, 12), (12, 12)):
        m.box([x, 0, z], [x + 3, 10, z + 3], {"north": leg, "south": leg, "east": leg, "west": leg, "down": C("foot", ("iron", 2), None, [], outline=False)}, name="leg")
    jig_s = C("jig_side", ("brass", 3), ("brass", 1), [("rect", 1, 1, 4, 1, "brass", 5)])
    m.box([5, 16, 6], [11, 19, 10], {"up": C("jig_top", ("brass", 3), ("brass", 1), [("hole", 2, 1, 3, 2)]), "north": jig_s, "south": jig_s, "east": C("jig_end", ("brass", 3), ("brass", 1), []), "west": C("jig_end", ("brass", 3), ("brass", 1), [])}, name="clamp_jig")
    return m


def rack():
    m = Model("spent_fuel_rack", render_type="minecraft:cutout")
    side = C("side", ("iron", 4), None, [("bars", "v", 0, 15, 5, "iron", True, 1), ("rect", 0, 0, 15, 1, "iron", 4), ("rect", 0, 14, 15, 15, "iron", 2), ("rect", 0, 7, 15, 7, "iron", 3)], outline=False)
    top = C("top", ("iron", 4), None, [("bars", "v", 0, 15, 5, "iron", True, 1), ("bars", "h", 0, 15, 5, "iron", False, 1)], outline=False)
    cube_faces(m, {"north": side, "south": side, "east": side, "west": side, "up": top, "down": top})
    return m


def cask():
    m = Model("shielded_cask")
    side = C("side", ("lead", 2), ("lead", 1), [("stripe", 4, "hazard", True, 2), ("rect", 0, 10, 11, 10, "lead", 4), ("bolts", [(2, 1), (9, 1)])])
    m.box([2, 0, 2], [14, 13, 14], {"north": side, "south": side, "east": side, "west": side, "down": C("base", ("lead", 1), None, [])}, name="body")
    lid_s = C("lid_side", ("steel", 3), None, [("rect", 0, 0, 13, 0, "steel", 5)], outline=False)
    m.box([1, 13, 1], [15, 15, 15], {"up": C("lid", ("steel", 3), ("iron", 2), [("ring", 7, 7, 5, "iron"), ("bolts", [(2, 2), (11, 2), (2, 11), (11, 11), (7, 1), (7, 12)])]), "north": lid_s, "south": lid_s, "east": lid_s, "west": lid_s, "down": C("lid_under", ("steel", 2), None, [])}, name="lid")
    lug = C("lug", ("iron", 4), None, [], outline=False)
    for x0 in (6,):
        m.box([x0, 15, 3], [x0 + 4, 16, 5], {"up": lug, "north": lug, "south": lug, "east": C("lug_e", ("iron", 3), None, [], outline=False), "west": C("lug_e", ("iron", 3), None, [], outline=False)}, name="lug")
        m.box([x0, 15, 11], [x0 + 4, 16, 13], {"up": lug, "north": lug, "south": lug, "east": C("lug_e", ("iron", 3), None, [], outline=False), "west": C("lug_e", ("iron", 3), None, [], outline=False)}, name="lug")
    return m


def signal_interface():
    return casing_module("reactor_signal_interface", C("front", ("iron", 3), ("iron", 2), [
        ("inset", 2, 2, 13, 13, "steel", 2), ("hole", 4, 4, 5, 5, "brass"), ("hole", 10, 4, 11, 5, "brass"), ("hole", 4, 10, 5, 11, "brass"), ("hole", 10, 10, 11, 11, "brass"),
        ("px", [(4, 7), (11, 7)], "red", 1), ("px", [(7, 4), (8, 11)], "amber", 1)]))


def sirens():
    off = Model("alarm_siren", atlas_name="alarm_siren")
    on = Model("alarm_siren_powered", share=off)
    for m, lamp in ((off, ("lamp_off", 1)), (on, ("lamp_on", 4))):
        base = C("base", ("iron", 3), ("iron", 1), [("bolts", [(1, 1), (8, 1), (1, 8), (8, 8)])])
        be = C("base_edge", ("iron", 2), None, [], outline=False)
        m.box([3, 3, 14], [13, 13, 16], {"north": base, "south": base, "east": be, "west": be, "up": C("base_edge_h", ("iron", 2), None, [], outline=False), "down": C("base_edge_h", ("iron", 2), None, [], outline=False)}, name="base")
        hs = C("horn_side", ("brass", 3), ("brass", 1), [("rect", 0, 2, 5, 2, "brass", 5)])
        m.box([5, 5, 6], [11, 11, 14], {"east": hs, "west": hs, "up": hs, "down": hs, "north": C("horn_mouth", ("brass", 2), ("brass", 4), [("rect", 1, 1, 4, 4, "ink", 0, True)])}, name="horn")
        key, li = lamp
        ls = C(key, ("red", li), None, [("rect", 0, 0, 3, 0, "red", min(4, li + 1))], outline=False)
        m.box([6, 11, 9], [10, 14, 13], {"up": ls, "north": ls, "south": ls, "east": ls, "west": ls}, name="beacon", shade=key != "lamp_on")
    return off, on


def docking():
    a = Model("docking_service_connector", atlas_name="docking_service_connector")
    b = Model("docking_service_connector_connected", share=a)
    for m, lamp_key, col in ((a, "lamp_amber", "amber"), (b, "lamp_green", "green")):
        face = C("face_" + col, ("iron", 3), ("brass", 2), [("ring", 8, 8, 6.5, "brass", ("steel", 2)), ("hole", 6, 6, 9, 9, "copper"),
                                                            ("px", [(8, 2), (7, 3), (9, 3)], "hazard", 3), ("px", [(8, 13), (7, 12), (9, 12)], "hazard", 3), ("px", [(2, 8), (3, 7), (3, 9)], "hazard", 3), ("px", [(13, 8), (12, 7), (12, 9)], "hazard", 3),
                                                            ("lamps", 1, [13], [col], True)])
        cube_faces(m, {"north": face, "south": CASE, "east": C("side", ("iron", 3), ("iron", 2), [("rect", 0, 2, 15, 3, "brass", 3)]), "west": C("side", ("iron", 3), ("iron", 2), [("rect", 0, 2, 15, 3, "brass", 3)]),
                       "up": C("side", ("iron", 3), ("iron", 2), [("rect", 0, 2, 15, 3, "brass", 3)]), "down": C("side", ("iron", 3), ("iron", 2), [("rect", 0, 2, 15, 3, "brass", 3)])})
        if m is b:
            clamp = C("clamp", ("brass", 4), None, [], outline=False)
            for (x0, x1) in ((1, 3), (13, 15)):
                m.box([x0, 6, -1], [x1, 10, 0], {"north": clamp, "east": C("clamp_e", ("brass", 3), None, [], outline=False), "west": C("clamp_e", ("brass", 3), None, [], outline=False), "up": C("clamp_t", ("brass", 3), None, [], outline=False), "down": C("clamp_t", ("brass", 3), None, [], outline=False)}, name="clamp")
    return a, b


def rod_drive():
    m = Model("control_rod_drive")
    base_s = C("base_side", ("iron", 3), ("iron", 1), [])
    m.box([0, 0, 0], [16, 3, 16], {"up": C("base_top", ("iron", 3), ("iron", 1), [("bolts", [(1, 1), (14, 1), (1, 14), (14, 14)])]), "down": C("base_down", ("iron", 2), None, []), "north": base_s, "south": base_s, "east": base_s, "west": base_s}, name="base")
    hside = C("housing_side", ("brass", 3), ("brass", 1), [("rect", 2, 3, 9, 7, "ink", 0, True), ("rect", 2, 8, 9, 8, "brass", 5)])
    m.box([3, 3, 3], [13, 12, 13], {"east": hside, "west": hside, "north": C("housing_front", ("brass", 3), ("brass", 1), [("rect", 3, 2, 6, 6, "red", 2), ("rect", 4, 3, 5, 5, "red", 4)]),
                                    "south": C("housing_back", ("brass", 3), ("brass", 1), [("ring", 5, 4.5, 3.5, "iron", ("steel", 1)), ("hole", 4, 3, 5, 4)]), "up": C("housing_top", ("brass", 3), ("brass", 1), [("hole", 4, 4, 5, 5)])}, name="housing")
    rack = C("rack", ("iron", 4), None, [("bars", "h", 0, 3, 2, "iron", False)], outline=False)
    m.box([7, 12, 7], [9, 16, 9], {"north": rack, "south": rack, "east": rack, "west": rack, "up": C("rack_top", ("iron", 5), None, [], outline=False)}, name="rack")
    return m


def rod_drive_pinion():
    m = Model("control_rod_drive_pinion")
    shaft_y(m, 8, 16)
    gear = C("pinion", ("iron", 4), None, [("ring", 3, 3, 3, "iron"), ("px", [(0, 2), (5, 3), (2, 0), (3, 5)], "iron", 2)], outline=False)
    ge = C("pinion_edge", ("iron", 3), None, [("bars", "v", 0, 5, 2, "iron", False)], outline=False)
    m.box([5, 6, 5], [11, 8, 11], {"up": gear, "down": gear, "north": ge, "south": ge, "east": ge, "west": ge}, name="pinion")
    return m


def build_rest():
    models = {}
    models["reactor_controller"] = controller()
    for n, c in (("fuel_channel", "fuel"), ("control_channel", "control"), ("coolant_channel", "coolant")):
        models[n] = channel(n, c)
    models["reactor_service_port"] = service_port()
    a, b = coolant_ports()
    models[a.name], models[b.name] = a, b
    models["heat_exchanger"] = heat_exchanger()
    models["condenser"] = condenser()
    models["cooling_tower_packing"] = cooling_tower()
    models["radiator_panel"] = radiator()
    models["emergency_coolant_tank"] = emergency_tank()
    models["relief_valve"] = relief_valve()
    models["boiler_heating_plate"] = boiler_plate()
    models["shielding_concrete"] = concrete()
    models["shielded_glass"] = shield_glass()
    models["shielded_hatch"] = hatch()
    models["fuel_assembly_bench"] = bench()
    models["spent_fuel_rack"] = rack()
    models["shielded_cask"] = cask()
    models["reactor_signal_interface"] = signal_interface()
    off, on = sirens()
    models[off.name], models[on.name] = off, on
    d0, d1 = docking()
    models[d0.name], models[d1.name] = d0, d1
    models["control_rod_drive"] = rod_drive()
    models["control_rod_drive_pinion"] = rod_drive_pinion()
    return models
