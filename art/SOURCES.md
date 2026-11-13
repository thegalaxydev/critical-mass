# Generated art sources

Every texture in `src/main/resources/assets/criticalmass/textures/` that came from generation is listed here.
Model: `generator` via the image tool built-in `generate` tool (`image_tool.py --edit`). Raw outputs (downscaled to 512 px JPEG) are in `art/generated/`.
All art is a candidate for replacement by hand-made art later.




## Hand-made block textures (2026-10-19)

`art/tools/handmade.py` draws these from Create textures (MIT) plus hand-placed pixels, replacing the generated versions:
- `reactor_controller`, `reactor_service_port`, `reactor_signal_interface`, `primary_coolant_port` (fronts and nozzle): the same casing frame with a brass-bezel monitor, a round brass-rimmed hatch, a red crystal indicator, and copper flanges with blue (inlet) and red (outlet) collars.
- `shielded_glass`: Create's tiled glass with an iron frame and tinted panes.
- `shielded_hatch`: Create's train trapdoor recoloured to iron with a hazard band.
- `shielding_concrete`: hand-drawn poured slabs.
- Plant machines (`art/tools/handmade_plant.py`): `heat_exchanger` and `condenser` on Create's copper casing with colour-coded port flanges, a tube-bundle side and a fin-grille side; `emergency_coolant_tank` on Create's fluid tank recoloured to dark steel with red bands, a sight glass and a valve wheel; `cooling_tower_packing` on Create's andesite casing with wood slats; `boiler_heating_plate` and `relief_valve` hand-drawn with Create's copper, brass and iron ramps.
- Remaining blocks (`art/tools/handmade_rest.py`, which paints each model face from its UVs): `steam_turbine`, `control_rod_drive`, the three channels, `fuel_assembly_bench`, `spent_fuel_rack` (solid iron sides, open top with four black wells and the brass-capped assembly used by the 1 to 4 fill models), `shielded_cask`, `alarm_siren`, `instrumentation_panel` and `docking_service_connector`, using Create's andesite, brass, copper and iron ramps, brass shaft sockets and colour-coded flanges. Rotor and pinion textures keep their cleaned generated versions.
## Block texture passover (2026-10-19)

All block textures except the pipe, ores and crack overlays were cleaned by `art/tools/despeckle.py`: greys snapped to one cool-grey ramp built from Create's railway casing and industrial iron tones, thin frames and bevels restored to the clean blockout tones, and single-pixel speckle merged into its surroundings while straight lines, signal colours and locked functional pixels are kept. The steam turbine window band was made opaque except a centre slit, with inward liner faces, to fit the smaller rotor disc.
## steel_ingot, steel_nugget and steel_plate (not generated)

- Used by: `textures/item/steel_ingot.png`, `textures/item/steel_nugget.png`, `textures/item/steel_plate.png`
- Source: hand-made by the author, originals in `art/originals/`.
- Date: 2026-11-10

## control_rod, turbine_blade, sealing_gasket, welding_torch, dosimeter, survey_meter and decontamination_compound (not generated)

- Used by: `textures/item/control_rod.png`, `textures/item/turbine_blade.png`, `textures/item/sealing_gasket.png`, `textures/item/welding_torch.png`, `textures/item/dosimeter.png`, `textures/item/survey_meter.png`, `textures/item/decontamination_compound.png`
- Source: hand-made by the author, originals in `art/originals/` (the welding torch is `portable_welder.png`).
- Date: 2026-11-11

## sealed_waste_canister and open_waste_canister (not generated)

- Used by: `textures/block/sealed_waste_canister.png`, `textures/block/open_waste_canister.png` and `models/block/sealed_waste_canister.json` (the open canister and the incomplete canister reuse the model)
- Source: hand-made by the author, texture originals in `art/originals/`.
- Date: 2026-11-10

## steel_block and reactor_alloy_block (not generated)

- Used by: `textures/block/steel_block.png`, `textures/block/reactor_alloy_block.png`
- Source: hand-made by the author, originals in `art/originals/`.
- Date: 2026-11-10

## alarm_siren

- Used by: `textures/block/alarm_siren.png`
- Raw output: `art/generated/alarm_siren-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: alarm siren, 32x32 game pixels.
Atlas for an alarm siren: iron mounting base with bolts, brass horn body with a dark mouth, and two small red beacon lamps (one unlit dark red, one lit bright red).

```

## protective_layer_1

- Used by: `textures/models/armor/protective_layer_1.png`
- Raw output: `art/generated/armor__protective_layer_1-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod armor texture sheet in the exact pixel-art style of the Create mod's items (references attached): a single object, chunky readable silhouette, dark outline that is a darker shade of each material (not pure black), 4 to 6 shade ramps per material, light from the top-left, crisp hand-placed square pixels, no anti-aliasing, no text or letters, no background, no drop shadow.

The FIRST attached image is a 64x32 armor UV sheet blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE sprite pixel. Repaint it in place as a finished 64x32 pixel-art sprite on that same coarse grid, keeping the exact silhouette (magenta = transparent, keep it magenta) and the same parts in the same places.

64x32 Minecraft armor texture sheet (outer layer) using the standard humanoid UV layout. Top-left 32x16 = hood (head cube: top, bottom, right, front, left, back faces): off-white rubberised canvas, the front face has a brass-rimmed open face window (keep the window transparent magenta) and a small iron respirator below it, yellow hazard strips on the sides. Top-right 32x16 region stays fully transparent magenta. Middle (x16-40, y16-32) = suit torso: canvas with a brass zip down the front, yellow hazard stripe across, brass rivets. Right block (x40-56, y16-32) = sleeves: canvas with a yellow band and black rubber gloves at the bottom. Left block (x0-16, y16-32) = boots: only the lower half is painted as black rubber boots with a yellow band; the upper half stays transparent magenta.

```

## protective_layer_2

- Used by: `textures/models/armor/protective_layer_2.png`
- Raw output: `art/generated/armor__protective_layer_2-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod armor texture sheet in the exact pixel-art style of the Create mod's items (references attached): a single object, chunky readable silhouette, dark outline that is a darker shade of each material (not pure black), 4 to 6 shade ramps per material, light from the top-left, crisp hand-placed square pixels, no anti-aliasing, no text or letters, no background, no drop shadow.

The FIRST attached image is a 64x32 armor UV sheet blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE sprite pixel. Repaint it in place as a finished 64x32 pixel-art sprite on that same coarse grid, keeping the exact silhouette (magenta = transparent, keep it magenta) and the same parts in the same places.

64x32 Minecraft armor texture sheet (inner leggings layer) using the standard humanoid UV layout. Only two areas are painted, everything else stays transparent magenta: the lower part of the torso block (x16-40, y16-32) as a canvas waist band with a brass belt and buckle, and the leg block (x0-16, y16-32) as off-white rubberised canvas trousers with a yellow knee band and black rubber ankle cuffs.

```

## boiler_heating_plate

- Used by: `textures/block/boiler_heating_plate.png`
- Raw output: `art/generated/boiler_heating_plate-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: boiler heating plate, 32x32 game pixels.
Atlas for a boiler heating plate (thin slab under a boiler): top face is a copper heating grille of parallel bars inside an iron frame (the heat-transfer face), dark steel sides with a copper strip, front/back faces with a copper connection port and black bore.

```

## circulation_pump

- Used by: `textures/block/circulation_pump.png` and the pump's body, wheel and item models
- Source: modelled and painted by the author in Blockbench, saved as `art/blockbench/mechanical_pump.bbmodel`. `art/tools/pump_model.py` splits its "pipe" and "flywheel" groups into the game models. The flywheel started from `art/tools/flywheel_bbmodel.py` and the pipe faces from the Reinforced Pipe flange.
- Date: 2026-11-12

## condenser

- Used by: `textures/block/condenser.png`
- Raw output: `art/generated/condenser-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: condenser, 32x32 game pixels.
Atlas for a steam condenser block: sides with horizontal copper cooling-coil fins, front face with a large iron exhaust-steam intake opening above a small blue feedwater outlet, back face with blue and red cooling-water flanges, top with an iron grille grid.

```

## control_channel

- Used by: `textures/block/control_channel.png`
- Raw output: `art/generated/control_channel-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: control channel, 32x32 game pixels.
Reactor control-rod channel column. Side: dark steel frame with a recessed window showing dark lead-grey absorber rods and a narrow brass insertion-position strip on the right with a small amber marker. Top/bottom: 3x3 grid of dark absorber rod ends.

```

## control_rod_drive

- Used by: `textures/block/control_rod_drive.png`
- Raw output: `art/generated/control_rod_drive-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: control rod drive, 64x64 game pixels.
Atlas for a control rod drive: iron base plate with bolts, brass drive housing with dark side windows, a red emergency latch lever on the front, a back face with an iron shaft socket ring and black hole, and an iron toothed rack.

```

## control_rod_drive_pinion

- Used by: `textures/block/control_rod_drive_pinion.png`
- Raw output: `art/generated/control_rod_drive_pinion-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: control rod drive pinion, 16x16 game pixels.
Atlas for the rotating pinion gear of the control rod drive: small iron toothed gear seen face-on and its toothed edge strip.

```

## coolant_channel

- Used by: `textures/block/coolant_channel.png`
- Raw output: `art/generated/coolant_channel-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: coolant channel, 32x32 game pixels.
Reactor coolant channel column. Side: dark steel frame with a recessed window showing vertical copper coolant pipes. Top/bottom: steel plate with four copper-rimmed coolant bores (black holes).

```

## cooling_tower_packing

- Used by: `textures/block/cooling_tower_packing.png`
- Raw output: `art/generated/cooling_tower_packing-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: cooling tower packing, 32x32 game pixels.
Atlas for cooling tower fill packing: horizontal dark wooden slats with transparent gaps between them (keep gaps magenta) and thin iron edge posts; top view with vertical slats and transparent gaps.

```

## docking_service_connector

- Used by: `textures/block/docking_service_connector.png`
- Raw output: `art/generated/docking_service_connector-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: docking service connector, 32x32 game pixels.
Atlas for a docking service connector: iron face with a brass clamping collar ring around a copper service socket, small yellow alignment arrow marks at each edge, one face with an amber status lamp and one with a green status lamp, iron sides with a brass band, brass clamp jaws.

```

## emergency_coolant_tank

- Used by: `textures/block/emergency_coolant_tank.png`
- Raw output: `art/generated/emergency_coolant_tank-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: emergency coolant tank, 32x32 game pixels.
Atlas for an emergency coolant tank: riveted dark steel tank side with two red warning bands and a vertical glass sight window showing blue coolant, top cap with a red ring and a brass fill port with a black opening.

```

## contaminated_coolant

- Used by: `textures/fluid/contaminated_coolant_still.png + _flow.png`
- Raw output: `art/generated/fluid__contaminated_coolant-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod fluid texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a 16x16 tile blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE tile pixel. Repaint it in place as a finished 16x16 pixel-art tile on that same coarse grid, keeping the exact tile (magenta = transparent, keep it magenta) and the same parts in the same places.

Tileable 16x16 Minecraft fluid texture for contaminated reactor coolant: murky olive yellow-green liquid with darker sludge swirls and a few sickly bright specks, seamless on all four edges.
```

## primary_coolant

- Used by: `textures/fluid/primary_coolant_still.png + _flow.png`
- Raw output: `art/generated/fluid__primary_coolant-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod fluid texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a 16x16 tile blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE tile pixel. Repaint it in place as a finished 16x16 pixel-art tile on that same coarse grid, keeping the exact tile (magenta = transparent, keep it magenta) and the same parts in the same places.

Tileable 16x16 Minecraft fluid surface texture for a pressurised primary reactor coolant: deep blue with lighter cyan ripples, like vanilla water but bluer and denser, seamless on all four edges.
```

## steam

- Used by: `textures/fluid/steam_still.png + _flow.png`
- Raw output: `art/generated/fluid__steam-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod fluid texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a 16x16 tile blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE tile pixel. Repaint it in place as a finished 16x16 pixel-art tile on that same coarse grid, keeping the exact tile (magenta = transparent, keep it magenta) and the same parts in the same places.

Tileable 16x16 Minecraft fluid texture for steam: soft pale grey-white swirling vapour wisps, low contrast, seamless on all four edges.
```

## fuel_assembly_bench

- Used by: `textures/block/fuel_assembly_bench.png`
- Raw output: `art/generated/fuel_assembly_bench-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: fuel assembly bench, 64x64 game pixels.
Atlas for a fuel assembly workbench: dark wooden worktop with a steel tray holding a pale ceramic part, iron legs and iron apron sides with recessed panels, front with a small brass drawer pull, and a small brass clamp jig with a hole.

```

## fuel_channel

- Used by: `textures/block/fuel_channel.png`
- Raw output: `art/generated/fuel_channel-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: fuel channel, 32x32 game pixels.
Reactor fuel channel column. Side: dark steel frame with an iron border and a recessed vertical window showing pale ceramic fuel rods behind two bright iron spacer bands. Top/bottom: iron-framed steel plate with a 3x3 grid of ceramic fuel-rod ends.

```

## heat_exchanger

- Used by: `textures/block/heat_exchanger.png`
- Raw output: `art/generated/heat_exchanger-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: heat exchanger, 32x32 game pixels.
Atlas for a shell-and-tube heat exchanger block: dark steel shell sides with copper bands and bolts, end caps with a round iron manhole ring, a primary face with two copper flanges (red and blue collars, black bores), and a secondary face with a blue feedwater flange and a pale steam flange.

```

## instrumentation_panel

- Used by: `textures/block/instrumentation_panel.png`
- Raw output: `art/generated/instrumentation_panel-v2.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: instrumentation panel, 32x32 game pixels.
Atlas for a wall-mounted reactor instrumentation panel. Main 14x14 face: dark steel panel with two round analogue needle gauges in brass bezels (cream dial faces, small tick marks, black needles resting at the low end), a black horizontal bar-graph strip, and a row of three small unlit indicator lamps (green, amber, red) in brass sockets. Also brass edge strips and an iron back plate.

```

## ceramic_fuel_pellet (not generated)

- Used by: `textures/item/ceramic_fuel_pellet.png`
- Source: hand-made by the author, original in `art/originals/ceramic_fuel_pellet.png`. Replaces the earlier generated icon.
- Date: 2026-11-10
## crushed_uranium

- Used by: `textures/item/crushed_uranium.png`
- Raw output: `art/generated/item__crushed_uranium-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod inventory item sprite in the exact pixel-art style of the Create mod's items (references attached): a single object, chunky readable silhouette, dark outline that is a darker shade of each material (not pure black), 4 to 6 shade ramps per material, light from the top-left, crisp hand-placed square pixels, no anti-aliasing, no text or letters, no background, no drop shadow.

The FIRST attached image is a 16x16 sprite blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE sprite pixel. Repaint it in place as a finished 16x16 pixel-art sprite on that same coarse grid, keeping the exact silhouette (magenta = transparent, keep it magenta) and the same parts in the same places.

Item: crushed uranium.
Create-style crushed ore: a small pile of yellow-green granules and grit, like Create's crushed raw ore items.

```

## damaged_fuel_assembly

- Used by: `textures/item/damaged_fuel_assembly.png`
- Raw output: `art/generated/item__damaged_fuel_assembly-v2.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod inventory item sprite in the exact pixel-art style of the Create mod's items (references attached): a single object, chunky readable silhouette, dark outline that is a darker shade of each material (not pure black), 4 to 6 shade ramps per material, light from the top-left, crisp hand-placed square pixels, no anti-aliasing, no text or letters, no background, no drop shadow.

The FIRST attached image is a 16x16 sprite blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE sprite pixel. Repaint it in place as a finished 16x16 pixel-art sprite on that same coarse grid, keeping the exact silhouette (magenta = transparent, keep it magenta) and the same parts in the same places.

Item: damaged fuel assembly.
The same fuel assembly but damaged: one fuel rod bent and sticking out diagonally from the bundle, a broken spacer grid, scorched grey rods and a few sickly green contamination specks.

```

## empty_fuel_assembly

- Used by: `textures/item/empty_fuel_assembly.png`
- Raw output: `art/generated/item__empty_fuel_assembly-v2.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod inventory item sprite in the exact pixel-art style of the Create mod's items (references attached): a single object, chunky readable silhouette, dark outline that is a darker shade of each material (not pure black), 4 to 6 shade ramps per material, light from the top-left, crisp hand-placed square pixels, no anti-aliasing, no text or letters, no background, no drop shadow.

The FIRST attached image is a 16x16 sprite blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE sprite pixel. Repaint it in place as a finished 16x16 pixel-art sprite on that same coarse grid, keeping the exact silhouette (magenta = transparent, keep it magenta) and the same parts in the same places.

Item: empty fuel assembly.
An empty reactor fuel assembly frame: brass top and bottom end fittings, two iron spacer grids and thin iron corner posts, with see-through gaps where fuel rods will go (keep those gaps transparent).

```

## fuel_assembly

- Used by: `textures/item/fuel_assembly.png`
- Raw output: `art/generated/item__fuel_assembly-v2.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod inventory item sprite in the exact pixel-art style of the Create mod's items (references attached): a single object, chunky readable silhouette, dark outline that is a darker shade of each material (not pure black), 4 to 6 shade ramps per material, light from the top-left, crisp hand-placed square pixels, no anti-aliasing, no text or letters, no background, no drop shadow.

The FIRST attached image is a 16x16 sprite blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE sprite pixel. Repaint it in place as a finished 16x16 pixel-art sprite on that same coarse grid, keeping the exact silhouette (magenta = transparent, keep it magenta) and the same parts in the same places.

Item: fuel assembly.
A fresh sealed reactor fuel assembly: tall square bundle of pale ceramic-and-steel fuel rods held by two bright iron spacer grids, polished brass top and bottom end fittings. Clean and new.

```

## fuel_cladding

- Used by: `textures/item/fuel_cladding.png`
- Raw output: `art/generated/item__fuel_cladding-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod inventory item sprite in the exact pixel-art style of the Create mod's items (references attached): a single object, chunky readable silhouette, dark outline that is a darker shade of each material (not pure black), 4 to 6 shade ramps per material, light from the top-left, crisp hand-placed square pixels, no anti-aliasing, no text or letters, no background, no drop shadow.

The FIRST attached image is a 16x16 sprite blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE sprite pixel. Repaint it in place as a finished 16x16 pixel-art sprite on that same coarse grid, keeping the exact silhouette (magenta = transparent, keep it magenta) and the same parts in the same places.

Item: fuel cladding.
A long thin polished silver-grey metal tube lying diagonally from bottom-left to top-right, open dark end at the bottom.

```

## protective_boots

- Used by: `textures/item/protective_boots.png`
- Raw output: `art/generated/item__protective_boots-v2.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod inventory item sprite in the exact pixel-art style of the Create mod's items (references attached): a single object, chunky readable silhouette, dark outline that is a darker shade of each material (not pure black), 4 to 6 shade ramps per material, light from the top-left, crisp hand-placed square pixels, no anti-aliasing, no text or letters, no background, no drop shadow.

The FIRST attached image is a 16x16 sprite blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE sprite pixel. Repaint it in place as a finished 16x16 pixel-art sprite on that same coarse grid, keeping the exact silhouette (magenta = transparent, keep it magenta) and the same parts in the same places.

Item: protective boots.
A pair of heavy black rubber hazard boots with thick soles, a yellow hazard band and a brass buckle.

```

## protective_hood

- Used by: `textures/item/protective_hood.png`
- Raw output: `art/generated/item__protective_hood-v2.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod inventory item sprite in the exact pixel-art style of the Create mod's items (references attached): a single object, chunky readable silhouette, dark outline that is a darker shade of each material (not pure black), 4 to 6 shade ramps per material, light from the top-left, crisp hand-placed square pixels, no anti-aliasing, no text or letters, no background, no drop shadow.

The FIRST attached image is a 16x16 sprite blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE sprite pixel. Repaint it in place as a finished 16x16 pixel-art sprite on that same coarse grid, keeping the exact silhouette (magenta = transparent, keep it magenta) and the same parts in the same places.

Item: protective hood.
A protective hazard hood: off-white rubberised canvas hood with a brass-rimmed open face window, a small iron respirator filter at the mouth, and yellow hazard trim strips down the sides.

```

## protective_leggings

- Used by: `textures/item/protective_leggings.png`
- Raw output: `art/generated/item__protective_leggings-v2.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod inventory item sprite in the exact pixel-art style of the Create mod's items (references attached): a single object, chunky readable silhouette, dark outline that is a darker shade of each material (not pure black), 4 to 6 shade ramps per material, light from the top-left, crisp hand-placed square pixels, no anti-aliasing, no text or letters, no background, no drop shadow.

The FIRST attached image is a 16x16 sprite blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE sprite pixel. Repaint it in place as a finished 16x16 pixel-art sprite on that same coarse grid, keeping the exact silhouette (magenta = transparent, keep it magenta) and the same parts in the same places.

Item: protective leggings.
Protective hazard trousers: off-white rubberised canvas with a brass-buckled waist belt, yellow hazard bands at the knees and black rubber ankle cuffs.

```

## protective_suit

- Used by: `textures/item/protective_suit.png`
- Raw output: `art/generated/item__protective_suit-v2.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod inventory item sprite in the exact pixel-art style of the Create mod's items (references attached): a single object, chunky readable silhouette, dark outline that is a darker shade of each material (not pure black), 4 to 6 shade ramps per material, light from the top-left, crisp hand-placed square pixels, no anti-aliasing, no text or letters, no background, no drop shadow.

The FIRST attached image is a 16x16 sprite blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE sprite pixel. Repaint it in place as a finished 16x16 pixel-art sprite on that same coarse grid, keeping the exact silhouette (magenta = transparent, keep it magenta) and the same parts in the same places.

Item: protective suit.
A protective hazard suit jacket: off-white rubberised canvas with a brass zip down the middle, brass rivet buckles, a yellow hazard stripe across the chest and black rubber cuffs.

```

## raw_uranium

- Used by: `textures/item/raw_uranium.png`
- Raw output: `art/generated/item__raw_uranium-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod inventory item sprite in the exact pixel-art style of the Create mod's items (references attached): a single object, chunky readable silhouette, dark outline that is a darker shade of each material (not pure black), 4 to 6 shade ramps per material, light from the top-left, crisp hand-placed square pixels, no anti-aliasing, no text or letters, no background, no drop shadow.

The FIRST attached image is a 16x16 sprite blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE sprite pixel. Repaint it in place as a finished 16x16 pixel-art sprite on that same coarse grid, keeping the exact silhouette (magenta = transparent, keep it magenta) and the same parts in the same places.

Item: raw uranium.
Raw uranium ore lump in the style of vanilla raw iron/copper: knobbly dull yellow-green mineral chunk with dark grey flecks.

```

## reactor_alloy_ingot (not generated)

- Used by: `textures/item/reactor_alloy_ingot.png`
- Source: hand-made by the author, original in `art/originals/reactor_alloy.png`.
- Date: 2026-11-10
## reactor_alloy_sheet (not generated)

- Used by: `textures/item/reactor_alloy_sheet.png`
- Source: hand-made by the author, original in `art/originals/reactor_alloy_sheet.png`.
- Date: 2026-11-10
## spent_fuel_assembly

- Used by: `textures/item/spent_fuel_assembly.png`
- Raw output: `art/generated/item__spent_fuel_assembly-v2.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod inventory item sprite in the exact pixel-art style of the Create mod's items (references attached): a single object, chunky readable silhouette, dark outline that is a darker shade of each material (not pure black), 4 to 6 shade ramps per material, light from the top-left, crisp hand-placed square pixels, no anti-aliasing, no text or letters, no background, no drop shadow.

The FIRST attached image is a 16x16 sprite blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE sprite pixel. Repaint it in place as a finished 16x16 pixel-art sprite on that same coarse grid, keeping the exact silhouette (magenta = transparent, keep it magenta) and the same parts in the same places.

Item: spent fuel assembly.
The same fuel assembly after use: darkened steel rods with a few faint glowing amber hot spots, dulled fittings, and a small yellow-and-black hazard tag hanging from the side on a wire.

```

## uranium_concentrate

- Used by: `textures/item/uranium_concentrate.png`
- Raw output: `art/generated/item__uranium_concentrate-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod inventory item sprite in the exact pixel-art style of the Create mod's items (references attached): a single object, chunky readable silhouette, dark outline that is a darker shade of each material (not pure black), 4 to 6 shade ramps per material, light from the top-left, crisp hand-placed square pixels, no anti-aliasing, no text or letters, no background, no drop shadow.

The FIRST attached image is a 16x16 sprite blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE sprite pixel. Repaint it in place as a finished 16x16 pixel-art sprite on that same coarse grid, keeping the exact silhouette (magenta = transparent, keep it magenta) and the same parts in the same places.

Item: uranium concentrate.
A small heaped pile of bright yellow concentrate powder (stylised, game-like) on a flat base.

```

## primary_coolant_port

- Used by: `textures/block/primary_coolant_port.png`
- Raw output: `art/generated/primary_coolant_port-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: primary coolant port, 32x32 game pixels.
Atlas with two front faces of the primary coolant port in iron casing: a copper flange with a black bore, one with a blue (inlet) collar ring and one with a red (outlet) collar ring, corner bolts.

```

## radiator_panel

- Used by: `textures/block/radiator_panel.png` and the radiator's block model
- Source: modelled and painted by the author in Blockbench, saved as `art/blockbench/radiator.bbmodel`, with the texture also kept as `art/originals/radiator.png`. `art/tools/radiator_model.py` exports it into the game.
- Date: 2026-11-12

## reactor_controller

- Used by: `textures/block/reactor_controller.png`
- Raw output: `art/generated/reactor_controller-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: reactor controller, 16x16 game pixels.
Front face of the reactor controller, set into riveted iron casing: brass faceplate with a dark status screen showing a small 3x2 grid of unlit green core-map cells, three small unlit indicator lamps (green, amber, red) below it, corner bolts.

```

## reactor_service_port

- Used by: `textures/block/reactor_service_port.png`
- Raw output: `art/generated/reactor_service_port-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: reactor service port, 16x16 game pixels.
Front face of the reactor fuel service port in riveted iron casing: a large round brass-ringed hatch with a second inner brass ring, a vertical brass locking bar across it and four bolts.

```

## reactor_signal_interface

- Used by: `textures/block/reactor_signal_interface.png`
- Raw output: `art/generated/reactor_signal_interface-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: reactor signal interface, 16x16 game pixels.
Front face of the reactor signal interface in iron casing: recessed steel panel with four brass-rimmed jack sockets (black holes) and small unlit red and amber indicator pips.

```

## casing_crack_1, casing_crack_2 and casing_crack_3 (not generated)

- Used by: `textures/block/casing_crack_1.png`, `textures/block/casing_crack_2.png`, `textures/block/casing_crack_3.png` (translucent crack layer of the cracked Reactor Vessel Casing models)
- Source: the author recoloured Minecraft's `destroy_stage_5` (original in `art/originals/casing_destroy_stage_5.png`); the other nine stages in `art/casing_break/` keep Minecraft's crack layout with the same colours and opacity. Cracks 1, 2 and 3 are stages 3, 6 and 9.
- Date: 2026-11-11

## reactor_vessel_casing (not generated)

- Used by: `textures/block/reactor_vessel_casing.png`, `textures/block/reactor_vessel_casing_connected.png` (connected-texture sheet in Create's omnidirectional casing layout)
- Source: hand-made by the author, originals in `art/originals/reactor_vessel_casing.png` and `art/originals/reactor_vessel_casing_connected.png`. Replaces the earlier generated and Create-derived versions.
- Date: 2026-11-10
## relief_valve

- Used by: `textures/block/relief_valve.png`
- Raw output: `art/generated/relief_valve-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: relief valve, 32x32 game pixels.
Atlas for a pressure relief valve: copper mounting flange with black bore and bolts, brass valve body with an iron seat ring, coiled iron spring stack, iron cap and a small iron vent horn with a dark mouth.

```

## shielded_cask

- Used by: `textures/block/shielded_cask.png`
- Raw output: `art/generated/shielded_cask-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: shielded cask, 32x32 game pixels.
Atlas for a shielded transport cask: dark lead-grey cylinder side with a black-and-yellow hazard band and bolts, steel lid with a ring of bolts, iron lifting lugs.

```

## shielded_glass

- Used by: `textures/block/shielded_glass.png`
- Raw output: `art/generated/shielded_glass-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: shielded glass, 16x16 game pixels.
Shielded leaded glass block texture: faintly tinted teal-grey glass with a fine dark lead wire mesh grid and a lead frame; keep it semi-transparent.

```

## shielded_hatch

- Used by: `textures/block/shielded_hatch.png`
- Raw output: `art/generated/shielded_hatch-v1.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: shielded hatch, 16x16 game pixels.
Heavy shielded hatch door texture: concrete-grey slab with an iron frame, recessed lead-grey panel, a black-and-yellow hazard stripe across the middle, a brass grab handle and four bolts.

```

## shielding_concrete

- Used by: `textures/block/shielding_concrete.png`
- Raw output: `art/generated/shielding_concrete-v2.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: shielding concrete, 16x16 game pixels.
Dense radiation shielding concrete block, one face used on all sides: dark grey aggregate concrete with small lighter stone and steel flecks, uniform, tileable, no frame.
Dense poured radiation-shielding concrete: smooth, even mid-grey surface with only subtle, sparse small aggregate flecks in close tones, faint formwork seam lines and two small form-tie holes. Low contrast, calm, no gravel look, no high-contrast speckle.
```

## steam_turbine

- Used by: `textures/block/steam_turbine.png`
- Raw output: `art/generated/steam_turbine-v2.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: steam turbine, 32x32 game pixels.
Atlas for a steam turbine section. Regions: iron end plates with a brass ring around a transparent shaft hole (keep the hole magenta), riveted dark steel drum plates with a vertical seam, very dark inner drum walls, a slotted inspection band of thin brass bars with magenta gaps between them (keep gaps magenta), a copper steam inlet flange with a red collar and small bore, an iron exhaust flange with a pale grey collar, and thin edge strips.

```

## steam_turbine_rotor

- Used by: `textures/block/steam_turbine_rotor.png`
- Raw output: `art/generated/steam_turbine_rotor-v2.jpg`
- Processing: Generated edit of the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18
- Prompt:

```
Minecraft mod block texture in the exact pixel-art style of the Create mod and Create Aeronautics (style references attached): crisp hand-placed square pixels, 4 to 6 shade ramps per material, 1-pixel dark outlines on panel edges, a light top-left bevel and darker bottom-right edge, recessed panels, restrained edge highlights, very low noise, readable at a distance, no photorealism, no text or letters, no gradients, flat front-on orthographic texture with no perspective, no cast shadows and no directional scene lighting.

The FIRST attached image is a UV texture atlas blockout drawn on a coarse grid: every square block of one flat colour is exactly ONE texture pixel. Repaint it in place as finished pixel art on that same coarse grid, so each grid cell stays one solid colour. Keep every rectangle, edge, hole and region exactly where it is and keep each region's material: dark grey/blue-grey = riveted dark steel, light grey = iron, orange-brown = copper, gold = brass, black = holes/sockets. Magenta means transparent: leave magenta areas pure magenta. Do not add a border or background around the atlas.

Texture: steam turbine rotor, 16x16 game pixels.
Atlas for the rotating turbine rotor: a round bladed disc seen face-on, polished iron blades alternating light and dark around a brass hub, magenta outside the circle (keep magenta), plus a thin iron rim strip.

```

## Overhaul blocks (not generated)

- Used by: `textures/block/reactor_viewport*.png`, `control_channel_rod_*.png`, `fuel_channel_glow_*.png`, `control_rod_shaft.png`, `control_panel.png`, `control_panel_parts.png`, `plant_screen.png`, `tower_fan.png`, `pressurizer.png`, `corium.png`, `corium_hot.png`, `ruined_channel.png`, `textures/item/plant_linker.png`
- Source: painted by `art/tools/plant_blocks.py` and `art/tools/panel_blocks.py` from Create's iron, brass, copper and steel ramps. The channel variants are recolours of the hand-made channel textures, and the cracked viewports use the casing crack overlays.

## centrifuge, enriched_uranium and depleted_uranium

- Used by: `textures/block/centrifuge.png`, `textures/item/enriched_uranium.png`, `textures/item/depleted_uranium.png`
- Source: drawn by `art/tools/plant_blocks.py`. The two item textures are recolours of `uranium_concentrate.png`.
- Date: 2026-11-12

## reinforced_valve, reinforced_valve_open and reinforced_valve_closed

- Used by: `textures/block/reinforced_valve.png`, `textures/block/reinforced_valve_open.png`, `textures/block/reinforced_valve_closed.png`
- Source: hand-made by the author in `art/originals/`, laid out like Create's `fluid_valve.png`, `valve_open.png` and `valve_closed.png`. The installed textures have their greys recoloured to the reactor alloy's shades by `art/tools/valve_paint.py`; the brass ring and the dial keep the original colours. The models parent Create's fluid valve models.
- Date: 2026-11-10
## reinforced_pipe and reinforced_pipe_connected

- Used by: `textures/block/reinforced_pipe.png`, `textures/block/reinforced_pipe_connected.png`
- Source: Create's `pipes.png` and `pipes_connected.png` (Create, MIT licence), recoloured from copper to the reactor alloy's blue-grey shades by `art/tools/pipe_paint.py`, flange rims included. The pipe models parent Create's fluid pipe models (`art/tools/pipe_models.py`).
- Date: 2026-10-19
