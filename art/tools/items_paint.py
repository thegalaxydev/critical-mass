from art import P, Canvas

T = (0, 0, 0, 0)


def outline_pass(c, mat_of_edge=None):
    W, H = c.w, c.h
    src = c.img.copy()
    for y in range(H):
        for x in range(W):
            if src.getpixel((x, y))[3] == 0:
                continue
            edge = False
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if not (0 <= nx < W and 0 <= ny < H) or src.getpixel((nx, ny))[3] == 0:
                    edge = True
            if edge:
                r, g, b, a = src.getpixel((x, y))
                c.px(x, y, (max(0, int(r * 0.45)), max(0, int(g * 0.45)), max(0, int(b * 0.45)), 255))
    for y in range(H):
        for x in range(W):
            if c.img.getpixel((x, y))[3] == 0:
                c.lock.putpixel((x, y), 255)


def assembly(c, state="fresh"):
    rod_light = ("ceramic", 4) if state != "spent" else ("steel", 4)
    rod_dark = ("ceramic", 2) if state != "spent" else ("steel", 2)
    if state == "damaged":
        rod_light, rod_dark = ("iron", 3), ("steel", 3)
    c.rect(5, 0, 11, 0, P("brass", 5))
    c.rect(4, 1, 11, 1, P("brass", 4))
    c.rect(4, 2, 11, 2, P("brass", 2))
    for y in range(3, 13):
        for x in range(4, 12):
            if x in (4, 11):
                c.px(x, y, P("iron", 3))
            elif state == "empty":
                c.px(x, y, T, True)
            else:
                m, i = rod_light if (x % 2 == 1) else rod_dark
                c.px(x, y, P(m, i))
    for y in (5, 10):
        c.rect(4, y, 11, y, P("iron", 5))
    c.rect(4, 13, 11, 13, P("brass", 4))
    c.rect(4, 14, 11, 14, P("brass", 2))
    c.rect(5, 15, 10, 15, P("brass", 1))
    if state == "spent":
        for (x, y) in ((6, 7), (8, 8), (7, 11), (9, 6)):
            c.px(x, y, P("amber", 2), True)
        c.px(12, 5, P("iron", 4), True)
        c.rect(12, 6, 14, 9, P("hazard", 2), True)
        c.px(13, 7, P("ink", 0), True)
        c.px(13, 8, P("ink", 0), True)
        c.hline(12, 14, 9, P("hazard", 0), True)
    if state == "damaged":
        for y in range(6, 10):
            c.px(9, y, T, True)
        for i, (x, y) in enumerate(((10, 7), (11, 8), (12, 9), (13, 10), (14, 11))):
            c.px(x, y, P("iron", 5), True)
            c.px(x, y + 1, P("steel", 2), True)
        c.px(7, 10, P("steel", 1), True)
        c.px(8, 10, T, True)
        for (x, y) in ((5, 8), (7, 4), (6, 12)):
            c.px(x, y, P("contaminated", 3), True)
    outline_pass(c)


def survey_meter(c):
    c.rect(4, 2, 7, 2, P("iron", 4))
    c.px(3, 3, P("iron", 4))
    c.px(8, 3, P("iron", 4))
    c.rect(1, 5, 10, 14, P("brass", 3))
    c.rect(2, 6, 9, 13, P("brass", 4))
    c.rect(2, 4, 3, 4, P("brass", 2))
    c.rect(8, 4, 9, 4, P("brass", 2))
    c.ellipse(2, 6, 7, 11, P("fabric", 5))
    c.px(4, 9, P("ink", 0))
    c.px(5, 8, P("ink", 0))
    c.px(6, 7, P("ink", 0))
    c.rect(8, 12, 9, 13, P("rubber", 2))
    c.px(9, 7, P("amber", 2), True)
    for i, (x, y) in enumerate(((11, 12), (12, 11), (12, 10), (13, 9))):
        c.px(x, y, P("rubber", 3))
    c.rect(12, 2, 14, 8, P("iron", 4))
    c.rect(13, 1, 13, 1, P("iron", 5))
    c.vline(12, 2, 8, P("iron", 2))
    c.rect(12, 7, 14, 8, P("brass", 3))
    outline_pass(c)


def dosimeter(c):
    c.rect(6, 1, 9, 3, P("iron", 4))
    c.rect(7, 2, 8, 2, T)
    c.rect(3, 4, 12, 14, P("brass", 3))
    c.rect(4, 5, 11, 13, P("brass", 4))
    c.rect(5, 6, 10, 8, P("ink", 0), True)
    c.rect(6, 7, 7, 7, P("amber", 3), True)
    c.rect(5, 10, 10, 12, P("fabric", 4))
    for x in (6, 8, 10):
        c.px(x, 11, P("signal_bars" if False else "green", 2), True) if x < 10 else c.px(x, 11, P("red", 2), True)
    outline_pass(c)


def hood(c):
    for y in range(1, 15):
        w = {1: 3, 2: 5, 3: 6}.get(y, 6)
        c.rect(8 - w, y, 7 + w, y, P("fabric", 4))
    c.rect(2, 13, 13, 14, P("fabric", 2))
    c.rect(5, 5, 10, 9, P("brass", 4))
    c.rect(6, 6, 9, 8, P("ink", 0), True)
    c.rect(6, 10, 9, 12, P("iron", 3))
    c.rect(7, 11, 8, 11, P("rubber", 1), True)
    c.vline(2, 4, 12, P("hazard", 2))
    c.vline(13, 4, 12, P("hazard", 2))
    outline_pass(c)


def suit(c):
    c.rect(1, 2, 14, 4, P("fabric", 4))
    c.rect(3, 4, 12, 14, P("fabric", 4))
    c.rect(1, 4, 2, 11, P("fabric", 3))
    c.rect(13, 4, 14, 11, P("fabric", 3))
    c.rect(1, 10, 2, 11, P("rubber", 2))
    c.rect(13, 10, 14, 11, P("rubber", 2))
    c.rect(6, 2, 9, 3, T, True)
    c.vline(7, 4, 14, P("brass", 3))
    c.vline(8, 4, 14, P("brass", 2))
    c.rect(3, 9, 12, 9, P("hazard", 2))
    for (x, y) in ((5, 6), (10, 6), (5, 12), (10, 12)):
        c.px(x, y, P("brass", 5), True)
    outline_pass(c)


def leggings(c):
    c.rect(3, 1, 12, 3, P("fabric", 3))
    c.rect(3, 4, 6, 14, P("fabric", 4))
    c.rect(9, 4, 12, 14, P("fabric", 4))
    c.rect(7, 4, 8, 6, P("fabric", 4))
    c.rect(3, 2, 12, 2, P("brass", 3))
    c.rect(3, 9, 6, 9, P("hazard", 2))
    c.rect(9, 9, 12, 9, P("hazard", 2))
    c.rect(3, 13, 6, 14, P("rubber", 2))
    c.rect(9, 13, 12, 14, P("rubber", 2))
    outline_pass(c)


def boots(c):
    for x0 in (1, 9):
        c.rect(x0, 6, x0 + 4, 12, P("rubber", 3))
        c.rect(x0, 12, x0 + 5, 14, P("rubber", 1))
        c.rect(x0, 8, x0 + 4, 8, P("hazard", 2))
        c.px(x0 + 2, 10, P("brass", 4), True)
    outline_pass(c)


ITEMS = {
    "empty_fuel_assembly": lambda c: assembly(c, "empty"),
    "fuel_assembly": lambda c: assembly(c, "fresh"),
    "spent_fuel_assembly": lambda c: assembly(c, "spent"),
    "damaged_fuel_assembly": lambda c: assembly(c, "damaged"),
    "survey_meter": survey_meter,
    "dosimeter": dosimeter,
    "protective_hood": hood,
    "protective_suit": suit,
    "protective_leggings": leggings,
    "protective_boots": boots,
}

ITEM_DESCRIPTIONS = {
    "empty_fuel_assembly": "An empty reactor fuel assembly frame: brass top and bottom end fittings, two iron spacer grids and thin iron corner posts, with see-through gaps where fuel rods will go (keep those gaps transparent).",
    "fuel_assembly": "A fresh sealed reactor fuel assembly: tall square bundle of pale ceramic-and-steel fuel rods held by two bright iron spacer grids, polished brass top and bottom end fittings. Clean and new.",
    "spent_fuel_assembly": "The same fuel assembly after use: darkened steel rods with a few faint glowing amber hot spots, dulled fittings, and a small yellow-and-black hazard tag hanging from the side on a wire.",
    "damaged_fuel_assembly": "The same fuel assembly but damaged: one fuel rod bent and sticking out diagonally from the bundle, a broken spacer grid, scorched grey rods and a few sickly green contamination specks.",
    "survey_meter": "A handheld radiation survey meter: chunky brass instrument box with a carry handle on top, a round cream analogue dial with a black needle, a small amber indicator light, and a separate iron detector probe on a short black cable to the right.",
    "dosimeter": "A small personal dosimeter badge: brass clip at the top, rectangular brass body with a dark readout window showing an amber value and a cream scale strip with green and red marks.",
    "protective_hood": "A protective hazard hood: off-white rubberised canvas hood with a brass-rimmed open face window, a small iron respirator filter at the mouth, and yellow hazard trim strips down the sides.",
    "protective_suit": "A protective hazard suit jacket: off-white rubberised canvas with a brass zip down the middle, brass rivet buckles, a yellow hazard stripe across the chest and black rubber cuffs.",
    "protective_leggings": "Protective hazard trousers: off-white rubberised canvas with a brass-buckled waist belt, yellow hazard bands at the knees and black rubber ankle cuffs.",
    "protective_boots": "A pair of heavy black rubber hazard boots with thick soles, a yellow hazard band and a brass buckle.",
}


def blob(c, pts, mat, base=3):
    for (x, y) in pts:
        c.px(x, y, P(mat, base))


def raw_uranium(c):
    rows = {3: (6, 10), 4: (4, 11), 5: (3, 12), 6: (3, 13), 7: (2, 13), 8: (2, 13), 9: (3, 13), 10: (3, 12), 11: (4, 12), 12: (5, 10)}
    for y, (a, b) in rows.items():
        c.rect(a, y, b, y, P("uranium", 3))
    for (x, y) in ((5, 5), (9, 7), (6, 9), (11, 10), (4, 8)):
        c.px(x, y, P("steel", 2))
    for (x, y) in ((6, 4), (7, 5), (10, 5), (4, 7)):
        c.px(x, y, P("uranium", 5))
    outline_pass(c)


def crushed_uranium(c):
    grains = [(3, 10), (6, 9), (9, 9), (12, 10), (4, 12), (7, 12), (10, 12), (5, 7), (8, 6), (11, 7), (7, 4)]
    for (x, y) in grains:
        c.rect(x - 1, y - 1, x + 1, y, P("uranium", 3))
        c.px(x - 1, y - 1, P("uranium", 5))
        c.px(x + 1, y, P("uranium", 1))
    c.rect(2, 13, 13, 13, P("uranium", 2))
    for (x, y) in ((6, 10), (9, 7), (11, 11)):
        c.px(x, y, P("steel", 3))
    outline_pass(c)


def concentrate(c):
    for y in range(6, 14):
        half = (y - 4) if y < 10 else 6
        c.rect(8 - half, y, 7 + half, y, P("hazard", 3 if y < 9 else 2))
    c.rect(1, 13, 14, 13, P("hazard", 1))
    for (x, y) in ((7, 6), (6, 8), (9, 9), (4, 11), (11, 11)):
        c.px(x, y, P("amber", 4))
    outline_pass(c)


def pellet(c):
    for (x0, y0) in ((2, 8), (7, 5), (9, 10)):
        c.rect(x0, y0, x0 + 4, y0 + 3, P("ceramic", 2))
        c.rect(x0, y0, x0 + 4, y0, P("ceramic", 4))
        c.px(x0 + 1, y0 + 1, P("ceramic", 3))
    outline_pass(c)


def diag(c, mat, base, length=12, width=2, start=(2, 13)):
    x, y = start
    for i in range(length):
        for w in range(width):
            c.px(x + i, y - i - w, P(mat, base + (1 if w == 0 else -1)))
            c.px(x + i + 1, y - i - w, P(mat, base))


def cladding(c):
    diag(c, "iron", 4, 11, 2, (2, 13))
    c.px(2, 13, P("ink", 0))
    c.px(13, 2, P("iron", 2))
    outline_pass(c)


def ingot(c, mat="lead"):
    c.rect(3, 6, 12, 10, P(mat, 3))
    c.rect(4, 5, 13, 5, P(mat, 5))
    c.rect(2, 7, 2, 10, P(mat, 2))
    c.rect(3, 11, 12, 11, P(mat, 1))
    c.rect(4, 6, 12, 6, P(mat, 4))
    outline_pass(c)


def sheet(c, mat="lead", thick=False):
    pts = [(x, y) for y in range(6, 12) for x in range(2 + (11 - y) // 2, 13 + (11 - y) // 2) if x < 16]
    for (x, y) in pts:
        c.px(x, y, P(mat, 3))
    for x in range(4, 15):
        c.px(x, 6, P(mat, 5))
    if thick:
        for x in range(2, 13):
            c.px(x, 12, P(mat, 1))
            c.px(x, 13, P(mat, 0))
        for (x, y) in ((5, 8), (11, 8), (8, 10)):
            c.px(x, y, P("iron", 5), True)
    outline_pass(c)


def control_rod(c):
    diag(c, "lead", 2, 10, 2, (3, 12))
    c.rect(12, 2, 14, 4, P("brass", 4))
    c.rect(1, 12, 3, 14, P("brass", 2))
    outline_pass(c)


def turbine_blade(c):
    pts = [(3, 13), (4, 12), (4, 11), (5, 10), (6, 9), (6, 8), (7, 7), (8, 6), (9, 5), (10, 4), (11, 3), (12, 2)]
    for i, (x, y) in enumerate(pts):
        c.rect(x, y, x + 2 + (i % 3 == 0), y, P("iron", 4))
        c.px(x, y, P("iron", 6))
    c.rect(2, 13, 4, 14, P("brass", 3))
    outline_pass(c)


def gasket(c):
    c.ellipse(2, 3, 13, 13, P("rubber", 3))
    c.ellipse(5, 6, 10, 10, T)
    c.ld.ellipse([5, 6, 10, 10], fill=255)
    c.px(4, 5, P("rubber", 4))
    outline_pass(c)


def decon(c):
    c.rect(4, 4, 11, 14, P("iron", 4))
    c.rect(4, 3, 11, 3, P("iron", 5))
    c.rect(5, 2, 10, 2, P("iron", 3))
    c.rect(4, 7, 11, 11, P("coolant", 3))
    c.rect(6, 8, 9, 10, P("fabric", 5))
    c.px(7, 9, P("coolant", 1), True)
    c.px(8, 9, P("coolant", 1), True)
    outline_pass(c)


def repair_kit(c):
    c.rect(2, 7, 13, 13, P("red", 2))
    c.rect(2, 7, 13, 8, P("red", 3))
    c.rect(5, 4, 10, 4, P("iron", 4))
    c.px(5, 5, P("iron", 4))
    c.px(10, 5, P("iron", 4))
    c.px(5, 6, P("iron", 4))
    c.px(10, 6, P("iron", 4))
    c.rect(4, 9, 5, 10, P("brass", 4), True)
    c.rect(10, 9, 11, 10, P("brass", 4), True)
    outline_pass(c)


def canister(c):
    c.rect(4, 3, 11, 14, P("hazard", 2))
    c.rect(4, 3, 11, 3, P("hazard", 3))
    c.rect(4, 2, 11, 2, P("iron", 4))
    c.rect(4, 14, 11, 14, P("iron", 2))
    for x in range(4, 12):
        c.px(x, 8, P("rubber", 1) if x % 2 == 0 else P("hazard", 3))
        c.px(x, 9, P("hazard", 3) if x % 2 == 0 else P("rubber", 1))
    c.rect(6, 5, 9, 6, P("rubber", 2))
    outline_pass(c)


ITEMS.update({
    "raw_uranium": raw_uranium, "crushed_uranium": crushed_uranium, "uranium_concentrate": concentrate, "ceramic_fuel_pellet": pellet,
    "fuel_cladding": cladding, "reactor_alloy_ingot": ingot, "reactor_alloy_sheet": sheet, "shielding_plate": lambda c: sheet(c, "concrete", True),
    "control_rod": control_rod, "turbine_blade": turbine_blade, "sealing_gasket": gasket, "decontamination_compound": decon,
    "repair_kit": repair_kit, "sealed_waste_canister": canister,
})
ITEM_DESCRIPTIONS.update({
    "raw_uranium": "Raw uranium ore lump in the style of vanilla raw iron/copper: knobbly dull yellow-green mineral chunk with dark grey flecks.",
    "crushed_uranium": "Create-style crushed ore: a small pile of yellow-green granules and grit, like Create's crushed raw ore items.",
    "uranium_concentrate": "A small heaped pile of bright yellow concentrate powder (stylised, game-like) on a flat base.",
    "ceramic_fuel_pellet": "Three small dark ceramic fuel pellets (short cylinders) with lighter tops, grouped together.",
    "fuel_cladding": "A long thin polished silver-grey metal tube lying diagonally from bottom-left to top-right, open dark end at the bottom.",
    "reactor_alloy_ingot": "A reactor alloy ingot shaped like Create's brass ingot, in cool blue-grey metal with bright top highlight.",
    "reactor_alloy_sheet": "A thin reactor alloy plate shaped like Create's metal sheets, in cool blue-grey metal.",
    "shielding_plate": "A thick heavy dark concrete-grey shielding plate, visibly thicker than a normal sheet, with three bright rivets.",
    "control_rod": "A long dark lead-grey absorber rod lying diagonally with brass end caps.",
    "turbine_blade": "A single curved polished iron turbine blade lying diagonally with a brass root fitting at the bottom-left.",
    "sealing_gasket": "A black rubber sealing ring gasket with a round hole in the middle (keep the hole transparent).",
    "dosimeter": ITEM_DESCRIPTIONS.get("dosimeter", ""),
    "decontamination_compound": "A sealed metal tin of decontamination compound with a blue label band and a white label patch.",
    "repair_kit": "A red metal repair toolbox with an iron carry handle and two brass latches.",
    "sealed_waste_canister": "A squat yellow waste drum with an iron lid and base, a black-and-yellow hazard band around the middle and a dark label patch.",
})
