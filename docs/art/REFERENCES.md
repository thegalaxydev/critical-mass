# Visual reference breakdown

Inspected from the exact dependency jars this project builds against:
Create 6.0.10-280, Create Aeronautics 1.3.2 (Simulated / Offroad / Aeronautics jars), Sable 2.0.6, Flywheel 1.0.6, NeoForge 21.1.262, Minecraft 1.21.1.

Both Create and Create Aeronautics license their **code** as MIT and their **assets** as All Rights Reserved.
No reference pixels are copied into this project's textures. References were studied for palette, density and construction, and given to the image generator only as style references.
Where our models need Create's real parts, they reference them at runtime by resource location (`create:block/axis`, `create:block/axis_top`), which is the standard addon approach and does not redistribute Create's files.

| Reference (inspected file) | What we took from it | Applied to |
| --- | --- | --- |
| `create:block/andesite_casing`, `brass_casing`, `copper_casing` | 16x16 faces; 2-px material frame around a recessed centre panel; 1-px dark outer outline; light top/left, dark bottom/right bevel; 5-7 shade ramps | Reactor Vessel Casing frame-and-panel layout; every casing-sided module (controller, ports, signal interface, docking connector) |
| `create:block/axis`, `axis_top` | Shaft is a 4-px square bar on a 16-px block; rotation is read through its texture, not geometry | Our rotors reference these textures directly so shafts match Create exactly |
| `create:block/gearbox`, `gearbox_top` | Circular socket framed by a casing ring marks a real shaft face | Brass socket rings on the pump housing and turbine end plates are only drawn where `hasShaftTowards` returns true |
| `create:block/pump` (32x32 atlas) | Multi-part machines pack all faces into one 32x32 sheet at 16 texels/block | All multi-element models use packed atlases (32 or 64 px) at the same 16 texels/block density |
| `create:block/pipes`, `fluid_valve` | Copper ramp, flange faces with dark bores, banded pipe sides | Coolant ports, pump flanges, exchanger/condenser ports |
| `create:block/fluid_tank`, `fluid_tank_top`, `fluid_tank_window` | Window strips show contents; riveted ends | Emergency Coolant Tank sight glass; Heat Exchanger end caps |
| `create:block/engine`, steam engine parts | Copper piston housing language; exhaust and inlet read as flanged openings | Steam Turbine steam inlet (top, red collar) vs exhaust (bottom, pale collar) |
| `create:block/gauge`, `boiler_gauge` | Cream dial, dark needle, brass bezel | Instrumentation Panel dial and bar meters |
| `create:item/precision_mechanism`, `brass_sheet`, `iron_sheet`, `brass_ingot`, `electron_tube` | Item outline is a darker shade of the material, not black; 3/4 view; chunky silhouettes | All 24 item sprites; alloy ingot and sheet share one blue-grey ramp |
| `create:item/goggles`, `copper_diving_helmet`, `copper_diving_layer_1` | Wearables use brass fittings over a soft base material | Protective outfit: off-white rubberised canvas, brass rims and buckles, black rubber cuffs and boots |
| `create:item/crushed_raw_uranium` | Create already ships a crushed uranium item (shown when a uranium tag exists) | Flagged as an open design issue for our Crushed Uranium |
| `simulated:block/iron_gearbox`, `swivel_bearing`, `physics_assembler` | Aeronautics machinery uses mid-grey riveted iron with corner rivets and dark recessed sockets | Iron frames, rivets and end plates on vessel blocks, turbine and rod drive |
| `aeronautics:block/propeller_bearing`, `gyroscopic_propeller_bearing` | Brass-framed dark sockets on iron; 32x32 atlases for mechanical parts | Pump housing sockets, rod drive housing |
| `simulated:block/dock`, `dock_powered` | Powered/unpowered state shown by a lamp and border colour, not by geometry change | Docking Service Connector amber (disconnected) / green with clamp jaws (connected) |
| `aeronautics:block/steam_vent/*` | Copper vent base with iron head; redstone state textures | Relief Valve vent horn and copper flange |

## Palette

Ramps live in `docs/art/palette.json` (also used by the generator). Steel, iron, copper and brass ramps are hand-tuned from colours sampled in the references above; ceramic, uranium, canvas, rubber, hazard, signal and fluid ramps are original.
