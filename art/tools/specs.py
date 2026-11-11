from modelgen import Model

AXIS_TEX = "create:block/axis"
AXIS_TOP = "create:block/axis_top"


def shaft_y(m, y0=0, y1=16):
    m.box([6, y0, 6], [10, y1, 10], {
        "north": ("ext", AXIS_TEX, [6, 0, 10, 16]), "south": ("ext", AXIS_TEX, [6, 0, 10, 16]),
        "east": ("ext", AXIS_TEX, [6, 0, 10, 16]), "west": ("ext", AXIS_TEX, [6, 0, 10, 16]),
        "up": ("ext", AXIS_TOP, [6, 6, 10, 10]), "down": ("ext", AXIS_TOP, [6, 6, 10, 10])}, name="shaft")


def cube(name, face_kind="casing", **params):
    m = Model(name)
    f = ("face", face_kind, params)
    m.box([0, 0, 0], [16, 16, 16], {d: f for d in ("north", "south", "east", "west", "up", "down")})
    return m


def pump(axis="x", share=None):
    m = Model("circulation_pump" if axis == "x" else "circulation_pump_vertical", atlas_name="circulation_pump", share=share)
    edge_v = ("flange_edge_v", "edge", {"mat": "copper", "base": 3})
    edge_h = ("flange_edge_h", "edge", {"mat": "copper", "base": 3})
    m.box([4, 4, 0], [12, 12, 2], {"north": ("flange_out", "flange", {"collar": "red"}), "east": edge_v, "west": edge_v, "up": edge_h, "down": edge_h, "south": None}, name="outlet")
    m.box([4, 4, 14], [12, 12, 16], {"south": ("flange_in", "flange", {"collar": "coolant"}), "east": edge_v, "west": edge_v, "up": edge_h, "down": edge_h}, name="inlet")
    m.box([5, 5, 2], [11, 11, 14], {"east": ("pipe_h", "pipe_side", {"along": "h"}), "west": ("pipe_h", "pipe_side", {"along": "h"}),
                                    "up": ("pipe_v", "pipe_side", {"along": "v"}), "down": ("pipe_v", "pipe_side", {"along": "v"})}, name="pipe")
    if axis == "x":
        sock = ("housing_socket", "socket", {"mat": "steel", "ring": "brass", "hole": 4})
        top = ("housing_top", "arrow", {"mat": "steel", "arrow_mat": "brass", "point": "up"})
        bottom = ("housing_bottom", "plate", {"mat": "steel", "rivets": True})
        m.box([2, 2, 4], [14, 14, 12], {"east": sock, "west": sock, "up": top, "down": bottom,
                                        "north": ("housing_front", "plate", {"mat": "steel", "rivets": True, "seam": "h"}),
                                        "south": ("housing_front", "plate", {"mat": "steel", "rivets": True, "seam": "h"})}, name="housing")
    else:
        sock = ("housing_socket_v", "socket", {"mat": "steel", "ring": "brass", "hole": 4})
        side = ("housing_side_v", "arrow", {"mat": "steel", "arrow_mat": "brass", "point": "left"})
        m.box([2, 2, 4], [14, 14, 12], {"up": sock, "down": sock, "east": side, "west": side,
                                        "north": ("housing_front", "plate", {"mat": "steel", "rivets": True, "seam": "h"}),
                                        "south": ("housing_front", "plate", {"mat": "steel", "rivets": True, "seam": "h"})}, name="housing")
    return m


def pump_rotor():
    m = Model("circulation_pump_rotor")
    shaft_y(m)
    hub = ("hub", "hub", {"mat": "brass"})
    edge = ("hub_edge", "edge", {"mat": "brass", "base": 2})
    for y0, y1 in ((0, 2), (14, 16)):
        m.box([5.5, y0, 5.5], [10.5, y1, 10.5], {"up": hub, "down": hub, "north": edge, "south": edge, "east": edge, "west": edge}, name="hub")
    return m


def turbine():
    m = Model("steam_turbine", render_type="minecraft:cutout")
    end = ("end_plate", "socket", {"mat": "iron", "ring": "brass", "hole": 4, "clear": True})
    ev = ("end_edge_v", "edge", {"mat": "iron", "base": 3})
    eh = ("end_edge_h", "edge", {"mat": "iron", "base": 3})
    for x0, x1, outer in ((0, 2, "west"), (14, 16, "east")):
        inner = "east" if outer == "west" else "west"
        m.box([x0, 1, 1], [x1, 15, 15], {outer: end, inner: ("end_inner", "plate", {"mat": "iron", "base": 2}), "north": ev, "south": ev, "up": eh, "down": eh}, name="end_plate")
    drum = ("drum_side", "plate", {"mat": "steel", "rivets": True, "seam": "v"})
    drum_top = ("drum_top", "plate", {"mat": "steel", "rivets": True})
    inner = ("drum_inner", "plate", {"mat": "steel", "base": 1, "frame": 0, "outline": False})
    m.box([2, 2, 2], [7, 14, 14], {"north": drum, "south": drum, "up": drum_top, "down": drum_top, "east": inner}, name="drum_a")
    m.box([9, 2, 2], [14, 14, 14], {"north": drum, "south": drum, "up": drum_top, "down": drum_top, "west": inner}, name="drum_b")
    sl = ("window", "slots", {"mat": "steel", "bar_mat": "brass"})
    m.box([7, 2, 2], [9, 14, 14], {"north": sl, "south": sl, "up": sl, "down": sl}, name="window")
    pe = ("port_edge", "edge", {"mat": "copper", "base": 3})
    m.box([5, 14, 5], [11, 16, 11], {"up": ("steam_inlet", "flange", {"collar": "red", "hole": 2}), "north": pe, "south": pe, "east": pe, "west": pe}, name="steam_inlet")
    pe2 = ("port_edge_out", "edge", {"mat": "iron", "base": 3})
    m.box([5, 0, 5], [11, 2, 11], {"down": ("exhaust", "flange", {"mat": "iron", "collar": "steam", "hole": 2}), "north": pe2, "south": pe2, "east": pe2, "west": pe2}, name="exhaust")
    return m


def turbine_rotor():
    m = Model("steam_turbine_rotor", render_type="minecraft:cutout")
    shaft_y(m)
    disc = ("blade_disc", "blade_disc", {"mat": "brass", "blade": "iron"})
    de = ("disc_edge", "edge", {"mat": "iron", "base": 4})
    m.box([4, 7, 4], [12, 9, 12], {"up": disc, "down": disc, "north": de, "south": de, "east": de, "west": de}, name="disc")
    return m


def instrument_panel():
    m = Model("instrumentation_panel")
    ev = ("panel_edge_v", "edge", {"mat": "brass", "base": 3})
    eh = ("panel_edge_h", "edge", {"mat": "brass", "base": 3})
    m.box([1, 1, 13], [15, 15, 16], {"north": ("panel_face", "gauge_panel", {"mat": "steel", "bezel": "brass"}), "east": ev, "west": ev, "up": eh, "down": eh,
                                     "south": ("panel_back", "plate", {"mat": "iron", "base": 2})}, name="panel")
    return m


SLICE_MODELS = {
    "reactor_vessel_casing": lambda: cube("reactor_vessel_casing", "casing", mat="steel", frame_mat="iron"),
    "circulation_pump": None,
    "circulation_pump_vertical": None,
    "circulation_pump_rotor": pump_rotor,
    "steam_turbine": turbine,
    "steam_turbine_rotor": turbine_rotor,
    "instrumentation_panel": instrument_panel,
}


def build_slice():
    models = {}
    for k, f in SLICE_MODELS.items():
        if f:
            models[k] = f()
    a = pump("x")
    b = pump("y", share=a)
    models["circulation_pump"] = a
    models["circulation_pump_vertical"] = b
    return models


DESCRIPTIONS = {
    "reactor_vessel_casing": "Heavy reactor pressure-vessel casing plate, one 16x16 face used on all six sides. Outer 2-pixel riveted iron frame with bright rivet heads at the corners and mid edges; inner recessed dark blue-grey steel plate with a horizontal welded seam across the middle. Industrial, heavy, slightly worn, no rust streaks.",
    "circulation_pump": "Atlas for a shaft-driven coolant circulation pump. Regions: copper pipe flanges with dark bore holes (outlet flange has a red collar ring, inlet flange a blue collar ring), copper pipe body strips with iron bands, dark steel pump housing faces with rivets and a horizontal seam, housing faces with a brass ring around a black shaft socket, and a housing plate with an embossed brass flow arrow. Copper should look like Create's copper pipes.",
    "steam_turbine": "Atlas for a steam turbine section. Regions: iron end plates with a brass ring around a transparent shaft hole (keep the hole magenta), riveted dark steel drum plates with a vertical seam, very dark inner drum walls, a slotted inspection band of thin brass bars with magenta gaps between them (keep gaps magenta), a copper steam inlet flange with a red collar and small bore, an iron exhaust flange with a pale grey collar, and thin edge strips.",
    "steam_turbine_rotor": "Atlas for the rotating turbine rotor: a round bladed disc seen face-on, polished iron blades alternating light and dark around a brass hub, magenta outside the circle (keep magenta), plus a thin iron rim strip.",
    "circulation_pump_rotor": "Atlas for the pump drive hub: a square brass drive flange plate with a darker outline, notch marks at the middle of each edge and an iron centre boss, plus a thin brass edge strip.",
    "instrumentation_panel": "Atlas for a wall-mounted reactor instrumentation panel. Main 14x14 face: dark steel panel with two round analogue needle gauges in brass bezels (cream dial faces, small tick marks, black needles resting at the low end), a black horizontal bar-graph strip, and a row of three small unlit indicator lamps (green, amber, red) in brass sockets. Also brass edge strips and an iron back plate.",
}


BLOCK_DEFS = {
    "reactor_vessel_casing": {"state": ("raw", {"variants": {"crack=%d" % i: {"model": "criticalmass:block/reactor_vessel_casing" + ("" if i == 0 else "_crack_%d" % i)} for i in range(4)}}), "item": "block", "lang": "Reactor Vessel Casing"},
    "circulation_pump": {"state": ("dir_axis", "circulation_pump", "x", "circulation_pump_vertical", "y"), "item": ("merged", "circulation_pump", "circulation_pump_rotor"), "lang": "Circulation Pump"},
    "steam_turbine": {"state": ("h_axis", "steam_turbine"), "item": ("merged", "steam_turbine", "steam_turbine_rotor"), "lang": "Steam Turbine"},
    "instrumentation_panel": {"state": ("horizontal", "instrumentation_panel"), "item": "block", "lang": "Instrumentation Panel"},
}

ITEM_DEFS = {
    "fuel_assembly": {"lang": "Fresh Fuel Assembly"},
    "survey_meter": {"lang": "Survey Meter", "handheld": True},
    "protective_hood": {"lang": "Protective Hood"},
    "protective_suit": {"lang": "Protective Suit"},
    "protective_leggings": {"lang": "Protective Leggings"},
    "protective_boots": {"lang": "Protective Boots"},
}


def build_all():
    import specs2
    models = build_slice()
    models.update(specs2.build_rest())
    return models


PILLAR = {"variants": {"axis=y": {"model": "criticalmass:block/%s"}, "axis=x": {"model": "criticalmass:block/%s", "x": 90, "y": 90}, "axis=z": {"model": "criticalmass:block/%s", "x": 90}}}


def _pillar(name):
    import json
    return json.loads(json.dumps(PILLAR).replace("%s", name))


def _facing_prop(model_by_value, prop):
    rot = {"north": (0, 0), "east": (0, 90), "south": (0, 180), "west": (0, 270), "up": (270, 0), "down": (90, 0)}
    out = {}
    for f, (x, y) in rot.items():
        for val, model in model_by_value.items():
            v = {"model": "criticalmass:block/" + model}
            if x:
                v["x"] = x
            if y:
                v["y"] = y
            out["facing=%s,%s=%s" % (f, prop, val)] = v
    return {"variants": out}


def _pipe_multipart():
    import pipe_models
    return pipe_models.multipart()


def _trapdoor(name):
    out = {}
    ys = {"north": 0, "east": 90, "south": 180, "west": 270}
    for f, y in ys.items():
        for half in ("bottom", "top"):
            for op in ("false", "true"):
                model = "criticalmass:block/%s_%s" % (name, "open" if op == "true" else half)
                v = {"model": model}
                yy = y
                if op == "true" and half == "top":
                    v["x"] = 180
                    yy = (y + 180) % 360
                if yy:
                    v["y"] = yy
                out["facing=%s,half=%s,open=%s" % (f, half, op)] = v
    return {"variants": out}


BLOCK_DEFS.update({
    "reactor_controller": {"state": ("horizontal", "reactor_controller"), "item": "block", "lang": "Reactor Controller"},
    "fuel_channel": {"state": ("raw", _pillar("fuel_channel")), "item": "block", "lang": "Fuel Channel"},
    "control_channel": {"state": ("raw", _pillar("control_channel")), "item": "block", "lang": "Control Channel"},
    "control_rod_drive": {"state": ("horizontal", "control_rod_drive"), "item": "block", "lang": "Control Rod Drive"},
    "coolant_channel": {"state": ("raw", _pillar("coolant_channel")), "item": "block", "lang": "Coolant Channel"},
    "reactor_service_port": {"state": ("facing", "reactor_service_port"), "item": "block", "lang": "Reactor Service Port"},
    "primary_coolant_port": {"state": ("raw", _facing_prop({"inlet": "primary_coolant_port_inlet", "outlet": "primary_coolant_port_outlet"}, "mode")), "item": "primary_coolant_port_outlet", "lang": "Primary Coolant Port"},
    "reinforced_pipe": {"state": ("raw", _pipe_multipart()), "item": "reinforced_pipe/item", "lang": "Reinforced Pipe"},
    "heat_exchanger": {"state": ("horizontal", "heat_exchanger"), "item": "block", "lang": "Heat Exchanger"},
    "condenser": {"state": ("horizontal", "condenser"), "item": "block", "lang": "Condenser"},
    "cooling_tower_packing": {"state": ("simple",), "item": "block", "lang": "Cooling Tower Packing"},
    "radiator_panel": {"state": ("horizontal", "radiator_panel"), "item": "block", "lang": "Radiator Panel"},
    "emergency_coolant_tank": {"state": ("simple",), "item": "block", "lang": "Emergency Coolant Tank"},
    "relief_valve": {"state": ("facing", "relief_valve"), "item": "block", "lang": "Relief Valve"},
    "boiler_heating_plate": {"state": ("horizontal", "boiler_heating_plate"), "item": "block", "lang": "Boiler Heating Plate"},
    "shielding_concrete": {"state": ("simple",), "item": "block", "lang": "Shielding Concrete"},
    "shielded_glass": {"state": ("simple",), "item": "block", "lang": "Shielded Glass"},
    "shielded_hatch": {"state": ("raw", _trapdoor("shielded_hatch")), "item": "shielded_hatch_bottom", "lang": "Shielded Hatch"},
    "fuel_assembly_bench": {"state": ("horizontal", "fuel_assembly_bench"), "item": "block", "lang": "Fuel Assembly Bench"},
    "spent_fuel_rack": {"state": ("simple",), "item": "block", "lang": "Spent Fuel Rack"},
    "shielded_cask": {"state": ("simple",), "item": "block", "lang": "Shielded Cask"},
    "reactor_signal_interface": {"state": ("horizontal", "reactor_signal_interface"), "item": "block", "lang": "Reactor Signal Interface"},
    "alarm_siren": {"state": ("raw", _facing_prop({"false": "alarm_siren", "true": "alarm_siren_powered"}, "powered")), "item": "alarm_siren", "lang": "Alarm Siren"},
    "docking_service_connector": {"state": ("raw", _facing_prop({"false": "docking_service_connector", "true": "docking_service_connector_connected"}, "connected")), "item": "docking_service_connector", "lang": "Docking Service Connector"},
})

EXTRA_MODELS = {
    "shielded_hatch_bottom": {"parent": "minecraft:block/template_orientable_trapdoor_bottom", "textures": {"texture": "criticalmass:block/shielded_hatch"}},
    "shielded_hatch_top": {"parent": "minecraft:block/template_orientable_trapdoor_top", "textures": {"texture": "criticalmass:block/shielded_hatch"}},
    "shielded_hatch_open": {"parent": "minecraft:block/template_orientable_trapdoor_open", "textures": {"texture": "criticalmass:block/shielded_hatch"}},
}


def _crack_model(stage):
    full = {"uv": [0, 0, 16, 16]}
    base = {d: dict(full, texture="#0") for d in ("north", "south", "east", "west", "up", "down")}
    over = {d: dict(full, texture="#crack") for d in ("north", "south", "east", "west", "up", "down")}
    return {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"0": "criticalmass:block/reactor_vessel_casing", "particle": "criticalmass:block/reactor_vessel_casing", "crack": "criticalmass:block/casing_crack_%d" % stage},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": base}, {"from": [0, 0, 0], "to": [16, 16, 16], "faces": over}]}


for _stage in (1, 2, 3):
    EXTRA_MODELS["reactor_vessel_casing_crack_%d" % _stage] = _crack_model(_stage)


DESCRIPTIONS.update({
    "reactor_controller": "Front face of the reactor controller, set into riveted iron casing: brass faceplate with a dark status screen showing a small 3x2 grid of unlit green core-map cells, three small unlit indicator lamps (green, amber, red) below it, corner bolts.",
    "fuel_channel": "Reactor fuel channel column. Side: dark steel frame with an iron border and a recessed vertical window showing pale ceramic fuel rods behind two bright iron spacer bands. Top/bottom: iron-framed steel plate with a 3x3 grid of ceramic fuel-rod ends.",
    "control_channel": "Reactor control-rod channel column. Side: dark steel frame with a recessed window showing dark lead-grey absorber rods and a narrow brass insertion-position strip on the right with a small amber marker. Top/bottom: 3x3 grid of dark absorber rod ends.",
    "coolant_channel": "Reactor coolant channel column. Side: dark steel frame with a recessed window showing vertical copper coolant pipes. Top/bottom: steel plate with four copper-rimmed coolant bores (black holes).",
    "reactor_service_port": "Front face of the reactor fuel service port in riveted iron casing: a large round brass-ringed hatch with a second inner brass ring, a vertical brass locking bar across it and four bolts.",
    "primary_coolant_port": "Atlas with two front faces of the primary coolant port in iron casing: a copper flange with a black bore, one with a blue (inlet) collar ring and one with a red (outlet) collar ring, corner bolts.",
    "reinforced_pipe": "Atlas for a thick reinforced copper pipe: pipe core faces with iron bands, long pipe side strips with highlight and an iron band, and a flanged pipe end with a black bore and four bolts.",
    "heat_exchanger": "Atlas for a shell-and-tube heat exchanger block: dark steel shell sides with copper bands and bolts, end caps with a round iron manhole ring, a primary face with two copper flanges (red and blue collars, black bores), and a secondary face with a blue feedwater flange and a pale steam flange.",
    "condenser": "Atlas for a steam condenser block: sides with horizontal copper cooling-coil fins, front face with a large iron exhaust-steam intake opening above a small blue feedwater outlet, back face with blue and red cooling-water flanges, top with an iron grille grid.",
    "cooling_tower_packing": "Atlas for cooling tower fill packing: horizontal dark wooden slats with transparent gaps between them (keep gaps magenta) and thin iron edge posts; top view with vertical slats and transparent gaps.",
    "radiator_panel": "Atlas for a wall-mounted radiator panel: front face of many thin vertical copper cooling fins inside a brass frame, brass edge strips with small copper header stubs, iron back plate.",
    "emergency_coolant_tank": "Atlas for an emergency coolant tank: riveted dark steel tank side with two red warning bands and a vertical glass sight window showing blue coolant, top cap with a red ring and a brass fill port with a black opening.",
    "relief_valve": "Atlas for a pressure relief valve: copper mounting flange with black bore and bolts, brass valve body with an iron seat ring, coiled iron spring stack, iron cap and a small iron vent horn with a dark mouth.",
    "boiler_heating_plate": "Atlas for a boiler heating plate (thin slab under a boiler): top face is a copper heating grille of parallel bars inside an iron frame (the heat-transfer face), dark steel sides with a copper strip, front/back faces with a copper connection port and black bore.",
    "shielding_concrete": "Dense radiation shielding concrete block, one face used on all sides: dark grey aggregate concrete with small lighter stone and steel flecks, uniform, tileable, no frame.",
    "shielded_glass": "Shielded leaded glass block texture: faintly tinted teal-grey glass with a fine dark lead wire mesh grid and a lead frame; keep it semi-transparent.",
    "shielded_hatch": "Heavy shielded hatch door texture: concrete-grey slab with an iron frame, recessed lead-grey panel, a black-and-yellow hazard stripe across the middle, a brass grab handle and four bolts.",
    "fuel_assembly_bench": "Atlas for a fuel assembly workbench: dark wooden worktop with a steel tray holding a pale ceramic part, iron legs and iron apron sides with recessed panels, front with a small brass drawer pull, and a small brass clamp jig with a hole.",
    "spent_fuel_rack": "Atlas for a spent fuel storage rack: iron lattice frame with vertical bars and transparent gaps between them (keep gaps magenta), horizontal braces; top view grid.",
    "shielded_cask": "Atlas for a shielded transport cask: dark lead-grey cylinder side with a black-and-yellow hazard band and bolts, steel lid with a ring of bolts, iron lifting lugs.",
    "reactor_signal_interface": "Front face of the reactor signal interface in iron casing: recessed steel panel with four brass-rimmed jack sockets (black holes) and small unlit red and amber indicator pips.",
    "alarm_siren": "Atlas for an alarm siren: iron mounting base with bolts, brass horn body with a dark mouth, and two small red beacon lamps (one unlit dark red, one lit bright red).",
    "docking_service_connector": "Atlas for a docking service connector: iron face with a brass clamping collar ring around a copper service socket, small yellow alignment arrow marks at each edge, one face with an amber status lamp and one with a green status lamp, iron sides with a brass band, brass clamp jaws.",
    "control_rod_drive": "Atlas for a control rod drive: iron base plate with bolts, brass drive housing with dark side windows, a red emergency latch lever on the front, a back face with an iron shaft socket ring and black hole, and an iron toothed rack.",
    "control_rod_drive_pinion": "Atlas for the rotating pinion gear of the control rod drive: small iron toothed gear seen face-on and its toothed edge strip.",
})


ITEM_DEFS.update({
    "raw_uranium": {"lang": "Raw Uranium"}, "crushed_uranium": {"lang": "Crushed Uranium"}, "uranium_concentrate": {"lang": "Uranium Concentrate"},
    "ceramic_fuel_pellet": {"lang": "Ceramic Fuel Pellet"}, "fuel_cladding": {"lang": "Fuel Cladding"}, "empty_fuel_assembly": {"lang": "Empty Fuel Assembly"},
    "spent_fuel_assembly": {"lang": "Spent Fuel Assembly"}, "damaged_fuel_assembly": {"lang": "Damaged Fuel Assembly"},
    "reactor_alloy_ingot": {"lang": "Reactor Alloy Ingot"}, "reactor_alloy_sheet": {"lang": "Reactor Alloy Sheet"},
    "control_rod": {"lang": "Control Rod", "handheld": True}, "turbine_blade": {"lang": "Turbine Blade"}, "sealing_gasket": {"lang": "Sealing Gasket"},
    "dosimeter": {"lang": "Dosimeter"}, "decontamination_compound": {"lang": "Decontamination Compound"}, "repair_kit": {"lang": "Repair Kit"},
    "sealed_waste_canister": {"lang": "Sealed Waste Canister"},
})
FLUID_LANG = {"primary_coolant": "Primary Coolant", "steam": "Steam", "contaminated_coolant": "Contaminated Coolant"}
