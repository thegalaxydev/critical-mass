# Generated art sources

Every texture in `src/main/resources/assets/criticalmass/textures/` that came from generation is listed here.
Model: `generator` via the image tool built-in `generate` tool (`image_tool.py --edit`). Raw outputs (downscaled to 512 px JPEG) are in `art/generated/`.
All art is a candidate for replacement by hand-made art later.




## Whose art each texture is

This table is the current state and overrides anything further down. Only the rows marked Author are the author's own work. Script means a script in `art/tools/` drew it, and Generated means it is a cleaned generated image: both are placeholders waiting for the author's art, and neither is hand-made. Approved marks a placeholder the author has accepted as it is. The sections below record how each texture, and each earlier version of it, was made.

| Texture | Art | Made with |
|---|---|---|
| Reactor Vessel Casing and its crack overlays | Author | `art/originals/` |
| Shielding Concrete, Shielded Glass | Author | `art/originals/` |
| Steel Block, Reactor Alloy Block | Author | `art/originals/` |
| Open and Sealed Waste Canister | Author | `art/originals/` |
| Reinforced Valve | Author | `art/originals/` |
| Circulation Pump | Author | `art/blockbench/mechanical_pump.bbmodel` |
| Radiator Panel | Author | `art/blockbench/radiator.bbmodel` |
| Cooling Tower Packing | Author | `art/originals/cooling_tower*.png`, `art/blockbench/cooling_tower_packing*.bbmodel` |
| Steam Turbine | Author's model, script texture | `art/tools/turbine_texture.py`, painted from the author's textures at the author's request |
| Relief Valve | Script, approved 2026-11-15 | `art/tools/relief_valve_texture.py` |
| Reactor Controller | Script | `art/tools/controller_texture.py` |
| Reactor Signal Interface | Script | `art/tools/signal_interface_texture.py` |
| Primary Coolant Port, Shielded Hatch | Script | `art/tools/handmade.py` |
| Reactor Service Port | Script | `art/tools/service_port_texture.py`; its sides are the author's Reactor Vessel Casing itself |
| Heat Exchanger, Condenser | Script | `art/tools/exchanger_texture.py` |
| Boiler Heating Plate | Script | `art/tools/handmade_plant.py` |
| Fuel, Control and Coolant Channel, Ruined Fuel Channel | Script | `art/tools/channel_texture.py`, in the tones of the author's Control Rod icon and Reactor Vessel Casing |
| Spent Fuel Rack | Script | `art/tools/rack_texture.py` |
| Shielded Cask | Script | `art/tools/cask_texture.py` |
| Control Rod Drive and its rod | Script | `art/tools/rod_drive_texture.py`; the cogwheel is Create's own model |
| Pressurizer | Script | `art/tools/pressurizer_texture.py`; the dial on it is the Dashboard Gauge |
| Centrifuge | Script | `art/tools/centrifuge_texture.py` |
| Reactor Viewport and its cracked stages, Corium | Script | `art/tools/plant_blocks.py` |
| Plant Screen | Script | `art/tools/screen_blocks.py`: the face is painted, the frame is Create's Display Board recoloured |
| Reinforced Pipe | Script | Create's pipe sheets recoloured |
| Uranium Ore, Deepslate Uranium Ore | Script | `art/tools/ore_paint.py`: vanilla stone with spots taken from the Raw Uranium icon |
| Reactor Alloy Dashboard texture, Dashboard Alarm | Not recorded | Their sections say hand-made without saying by whom |
| Dashboard and its other parts | Create: Loconautics | Used with its authors' permission |
| Steel Ingot, Nugget and Plate, Control Rod, Turbine Blade, Sealing Gasket, Welding Torch, Dosimeter, Survey Meter, Decontamination Compound, Ceramic Fuel Pellet, Reactor Alloy Ingot and Sheet | Author | `art/originals/` |
| Raw Uranium, Crushed Uranium, Uranium Concentrate, Fuel Cladding, the four Fuel Assembly icons, the four Protective Suit icons and the worn suit layers | Generated | |
| Enriched Uranium, Depleted Uranium, Plant Linker | Script | `art/tools/plant_blocks.py`; the two uranium icons are recolours of Uranium Concentrate |
| Primary Coolant, Contaminated Coolant and Steam fluids | Generated | |
| Alarm sound | Script | `art/tools/alarm_sound.py` |

## Script-painted block textures (2026-10-19)

`art/tools/handmade.py` draws these from Create textures (MIT) plus pixels the script places, replacing the earlier generated versions. They are placeholders, not the author's art:
- `primary_coolant_port` (fronts and nozzle): the casing frame with copper flanges and blue (inlet) and red (outlet) collars.
- `shielded_hatch`: Create's train trapdoor recoloured to iron with a hazard band.
- Plant machines (`art/tools/handmade_plant.py`): `emergency_coolant_tank` on Create's fluid tank recoloured to dark steel with red bands, a sight glass and a valve wheel; `boiler_heating_plate` drawn by the script with Create's copper, brass and iron ramps.
- Remaining blocks (`art/tools/handmade_rest.py`, which paints each model face from its UVs): `fuel_assembly_bench`, `alarm_siren` and `instrumentation_panel`, using Create's andesite, brass, copper and iron ramps, brass shaft sockets and colour-coded flanges.
## Block texture passover (2026-10-19)

All block textures except the pipe, ores and crack overlays were cleaned by `art/tools/despeckle.py`: greys snapped to one cool-grey ramp built from Create's railway casing and industrial iron tones, thin frames and bevels restored to the clean blockout tones, and single-pixel speckle merged into its surroundings while straight lines, signal colours and locked functional pixels are kept. The steam turbine window band was made opaque except a centre slit, with inward liner faces, to fit the smaller rotor disc.
## steel_ingot, steel_nugget and steel_plate (the author's own)

- Used by: `textures/item/steel_ingot.png`, `textures/item/steel_nugget.png`, `textures/item/steel_plate.png`
- Source: hand-made by the author, originals in `art/originals/`.
- Date: 2026-11-10

## control_rod, turbine_blade, sealing_gasket, welding_torch, dosimeter, survey_meter and decontamination_compound (the author's own)

- Used by: `textures/item/control_rod.png`, `textures/item/turbine_blade.png`, `textures/item/sealing_gasket.png`, `textures/item/welding_torch.png`, `textures/item/dosimeter.png`, `textures/item/survey_meter.png`, `textures/item/decontamination_compound.png`
- Source: hand-made by the author, originals in `art/originals/` (the welding torch is `portable_welder.png`).
- Date: 2026-11-11

## sealed_waste_canister and open_waste_canister (the author's own)

- Used by: `textures/block/sealed_waste_canister.png`, `textures/block/open_waste_canister.png` and `models/block/sealed_waste_canister.json` (the open canister and the incomplete canister reuse the model)
- Source: hand-made by the author, texture originals in `art/originals/`.
- Date: 2026-11-10

## steel_block and reactor_alloy_block (the author's own)

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

## control_rod_drive

- Used by: `textures/block/control_rod_drive.png`, `textures/block/control_rod_shaft.png` and the drive's block and item models
- Source: `art/blockbench/control_rod_drive.bbmodel`, one group called `body`: a bolted base, the two halves of a gear case with room for a cogwheel between them, a cap plate and a brass rod sleeve. `art/tools/rod_drive_texture.py` paints the sheet. The base and cap plate are the author's Steel Block, the gear case halves are plain panels in the frame and inset colours of the author's Reactor Vessel Casing, and the sleeve is brass. Nothing else is drawn on it: the cogwheel and the sleeve are the only accents. The same tool repaints the rod as a threaded spindle in the Steel Block's tones and keeps its brass cap.
- Moving parts: none of our own. The cogwheel between the case halves is Create's own `cogwheel_shaftless` model, at the height of any Cogwheel placed beside it, and it is how the drive takes rotation. The item model includes a copy of it so the icon shows the whole drive.
- Status: script-painted placeholder, not yet approved.
- Repainting: run the tool with `fresh`. Without it the sheet is left alone, so hand edits are safe. `art/tools/rod_drive_model.py` then exports the models.
- Date: 2026-11-15

## cooling_tower_packing

- Used by: nothing since 2026-11-12. The block now uses the hand-painted `art/originals/cooling_tower.png` and the models in `art/blockbench/cooling_tower_packing*.bbmodel`, exported by `art/tools/tower_model.py`.
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

## fuel_channel, control_channel, coolant_channel and ruined_channel

- Used by: `textures/block/fuel_channel.png`, `fuel_channel_glow_1.png` to `_3.png`, `control_channel.png`, `coolant_channel.png`, `ruined_channel.png` and the channels' models
- Source: `art/blockbench/fuel_channel.bbmodel` (groups `plates` and `rods`), `control_channel.bbmodel` (`frame` and `rod`) and `coolant_channel.bbmodel` (`frame` and `tubes`). Each channel is two thin end plates with real rods between them, so a stack reads as rods running through spacer grids and a viewport shows the rows behind. Fuel is nine silver rods between gold plates, after the author's fuel assembly and Control Rod icons. Control is four guide tubes round a gold absorber rod, which the exporter cuts to five lengths for the rod states. Coolant is four copper tubes.
- Texture: `art/tools/channel_texture.py` paints flat ramps only. The silver and gold are sampled from the author's Control Rod icon, the dark plates and guide tubes from the author's Reactor Vessel Casing, the copper from `docs/art/palette.json`. The three glow sheets recolour only the rods. The Ruined Fuel Channel is the fuel layout in scorched tones.
- Status: script-painted placeholders, not yet approved.
- Repainting: run the tool with `fresh`. Without it the sheets are left alone, so hand edits are safe. `art/tools/channel_model.py` then exports the models.
- Date: 2026-11-15

## heat_exchanger and condenser

- Used by: `textures/block/heat_exchanger.png`, `textures/block/condenser.png` and the two blocks' models
- Source: `art/blockbench/heat_exchanger.bbmodel` and `condenser.bbmodel`, each one group called `body`: a barrel with a flange at each end. The exchanger's barrel stands upright, so a stack of them reads as one column of bolted sections. The condenser's barrel lies along its facing, so a row of them reads as one long shell. Where a pipe joins the barrel's side, the blockstate adds the Reinforced Pipe's own flanged stub, the same model the Relief Valve uses.
- Texture: `art/tools/exchanger_texture.py` paints flat ramps with two plain hoops. The Heat Exchanger is copper from `docs/art/palette.json` with flanges of the author's Steel Block. The Condenser is the Steel Block's tones with copper flanges.
- Status: script-painted placeholders, not yet approved.
- Repainting: run the tool with `fresh`. Without it the sheets are left alone, so hand edits are safe. `art/tools/exchanger_model.py` then exports the models and blockstates.
- Date: 2026-11-15

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

## ceramic_fuel_pellet (the author's own)

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

## reactor_alloy_ingot (the author's own)

- Used by: `textures/item/reactor_alloy_ingot.png`
- Source: hand-made by the author, original in `art/originals/reactor_alloy.png`.
- Date: 2026-11-10
## reactor_alloy_sheet (the author's own)

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

## pressurizer

- Used by: `textures/block/pressurizer.png` and the models `block/pressurizer.json`, `pressurizer_top.json` and `pressurizer_item.json`
- Source: `art/blockbench/pressurizer.bbmodel`, in two groups: `base` is the foot flange and the lower barrel with its heater band, `top` is the upper barrel, the shoulder flange, the cap and the nozzle. The vessel stands two blocks tall, so the exporter writes `top` as its own block model, lowered one block.
- Texture: `art/tools/pressurizer_texture.py` paints a barrel in the Reactor Vessel Casing's navy with one copper heater band, flanges and cap in the author's Steel Block tones and a brass nozzle.
- The dial a player fits on its side is not part of this art: it is the Dashboard Gauge, drawn by the same code as on a dashboard.
- Status: script-painted placeholder, not yet approved.
- Repainting: run the tool with `fresh`. Without it the sheet is left alone, so hand edits are safe. `art/tools/pressurizer_model.py` then exports the models and blockstates.
- Date: 2026-11-15

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

- Used by: `textures/block/reactor_controller.png` and the models `block/reactor_controller.json`, `reactor_controller_standby.json`, `reactor_controller_running.json` and `reactor_controller_tripped.json`
- Source: `art/blockbench/reactor_controller.bbmodel`. The group `body` is the casing block with its front cut into a well two pixels deep, a plate at the back of the well and a brass bezel on it. Each of the groups `display_off`, `display_standby`, `display_running` and `display_tripped` holds the screen glass and the three lamps for one state, all in the same place, so show one group at a time in Blockbench. Cubes whose names end in `_lit` are exported at full brightness.
- Texture: `art/tools/controller_texture.py` paints the plate and the well from the Reactor Vessel Casing's own tones, the bezel in Create's brass tones, and the screens and lamps from the signal colours in `docs/art/palette.json`. The sides, top, bottom, back and front frame use the casing texture itself.
- Status: script-painted placeholder, not yet approved.
- Repainting: run the tool with `fresh`. Without it the sheet is left alone, so hand edits are safe. `art/tools/controller_model.py` then exports the four models and the blockstate.
- Date: 2026-11-15

## reactor_service_port

- Used by: `textures/block/reactor_service_port.png` and the port's body, door and item models
- Source: `art/blockbench/reactor_service_port.bbmodel`, modelled with the hatch facing up. The group `body` is the shell, a steel hatch ring round a dark well, a bar across the well and a brass-ringed boss on the bar where a Valve Handle or a shaft turns the hatch. The group `door_west` is one sliding door; the exporter mirrors it for the other side, and the renderer slides the pair apart under the ring as the hatch opens.
- Texture: the four sides and the underside use the author's Reactor Vessel Casing texture directly, so the port sits in the vessel wall like the casing round it. `art/tools/service_port_texture.py` paints the small sheet for the top: the ring is the author's Steel Block, the well and doors are the casing's inset tones, the bar is steel and the boss is brass.
- Status: script-painted placeholder, not yet approved.
- Repainting: run the tool with `fresh`. Without it the sheet is left alone, so hand edits are safe. `art/tools/service_port_model.py` then exports the models and the blockstate.
- Date: 2026-11-15

## spent_fuel_rack

- Used by: `textures/block/spent_fuel_rack.png` and the models `block/spent_fuel_rack.json`, its four `_assembly_` models and `spent_fuel_rack_item.json`
- Source: `art/blockbench/spent_fuel_rack.bbmodel`, in two groups. `body` is an open-topped tub: a steel base, a post at each corner, a side panel set in a pixel between them, and a top rim with a cross that makes four cells. `assembly` is one fuel assembly standing in the first cell; the exporter stands a copy in each of the other three.
- Texture: `art/tools/rack_texture.py` paints the side panels from the Reactor Vessel Casing's own tones, the assembly in the author's Steel Block tones with a cap in Create's brass tones. The base, posts and rim use the Steel Block texture itself.
- The water in the tub is not part of the model: `RackRenderer` draws the stored fluid at its real level.
- Status: script-painted placeholder, not yet approved.
- Repainting: run the tool with `fresh`. Without it the sheet is left alone, so hand edits are safe. `art/tools/rack_model.py` then exports the models and the blockstate.
- Date: 2026-11-16

## reactor_signal_interface

- Used by: `textures/block/reactor_signal_interface.png` and the models `block/reactor_signal_interface.json`, its `_lens`, `_lens_lit` and six `_tab_` models, and `reactor_signal_interface_item.json`
- Source: `art/blockbench/reactor_signal_interface.bbmodel`. The group `body` is the same casing block with a well in its front as the Reactor Controller, with a brass bezel on the plate and a dark track under it. `lens` and `lens_lit` are the signal lens off and on, in the same place, so show one at a time in Blockbench. `tab` is the slider tab in its first place; the exporter makes the other five places by sliding it two pixels at a time.
- Texture: `art/tools/signal_interface_texture.py` paints the plate and the well from the Reactor Vessel Casing's own tones, the bezel and tab in Create's brass tones, and the lens from the red in `docs/art/palette.json`. The sides, top, bottom, back and front frame use the casing texture itself.
- Status: script-painted placeholder, not yet approved.
- Repainting: run the tool with `fresh`. Without it the sheet is left alone, so hand edits are safe. `art/tools/signal_interface_model.py` then exports the models and the blockstate.
- Date: 2026-11-15

## casing_crack_1, casing_crack_2 and casing_crack_3 (the author's own)

- Used by: `textures/block/casing_crack_1.png`, `textures/block/casing_crack_2.png`, `textures/block/casing_crack_3.png` (translucent crack layer of the cracked Reactor Vessel Casing models)
- Source: the author recoloured Minecraft's `destroy_stage_5` (original in `art/originals/casing_destroy_stage_5.png`); the other nine stages in `art/casing_break/` keep Minecraft's crack layout with the same colours and opacity. Cracks 1, 2 and 3 are stages 3, 6 and 9.
- Date: 2026-11-11

## reactor_vessel_casing (the author's own)

- Used by: `textures/block/reactor_vessel_casing.png`, `textures/block/reactor_vessel_casing_connected.png` (connected-texture sheet in Create's omnidirectional casing layout)
- Source: hand-made by the author, originals in `art/originals/reactor_vessel_casing.png` and `art/originals/reactor_vessel_casing_connected.png`. Replaces the earlier generated and Create-derived versions.
- Date: 2026-11-10
## relief_valve

- Used by: `textures/block/relief_valve.png` and the valve's body, stub and item models
- Source: `art/blockbench/relief_valve.bbmodel`. The hub and the flanged stubs are the Reinforced Pipe's own junction casing, tube and rim, mapped onto the pipe's texture, so the valve always matches the line it sits in. `art/tools/relief_valve_texture.py` paints the small sheet for the parts that stand on the hub: a bolted seat flange, a spring bonnet with a brass band, a brass cap round the dark vent bore, and a red test lever. Its steel tones are sampled from the author's Reinforced Pipe texture, and the brass and red are the mod's own ramps.
- Status: script-painted, and approved by the author as it is on 2026-11-15.
- Repainting: run the tool with `fresh`. Without it the sheet is left alone, so hand edits are safe. `art/tools/relief_valve_model.py` then exports the models and the blockstate. The Blockbench file needs three groups: `hub`, `valve` (modelled with the vent pointing up) and `stub_north`.
- Date: 2026-11-15

## shielded_cask

- Used by: `textures/block/shielded_cask.png` and the models `block/shielded_cask.json`, `shielded_cask_lid.json` and `shielded_cask_item.json`
- Source: `art/blockbench/shielded_cask.bbmodel`, in two groups. `body` is the part that stands still: a barrel between a foot flange and a head flange, a trunnion on two sides, and four studs standing up from the head. `lid` is the plate the studs pass through; the game raises it up the studs when the cask is unbolted.
- Texture: `art/tools/cask_texture.py` paints the barrel in the Reactor Vessel Casing's tones with one hazard band from `docs/art/palette.json`, and the lid and studs in the author's Steel Block tones. The flanges and trunnions use the Steel Block texture itself.
- Status: script-painted placeholder, not yet approved.
- Repainting: run the tool with `fresh`. Without it the sheet is left alone, so hand edits are safe. `art/tools/cask_model.py` then exports the models and the blockstate.
- Date: 2026-11-16

## shielded_glass

- Used by: `textures/block/shielded_glass.png`, `textures/block/shielded_glass_connected.png` (connected-texture sheet in Create's omnidirectional casing layout)
- Source: hand-made by the author, originals in `art/originals/shielded_glass.png` and `art/originals/shielded_glass_connected.png`. Replaces the earlier version drawn from Create's tiled glass. No script produces it: edit the PNGs and copy them over the textures.
- Date: 2026-11-14

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
- Source: hand-made by the author, original in `art/originals/shielding_concrete.png`. Replaces the earlier version. No script produces it: edit the PNG and copy it over the texture.
- Date: 2026-11-14

## steam_turbine

- Used by: `textures/block/steam_turbine.png` and the turbine's block, glass, exhaust, rotor and item models
- Source: modelled by the author in Blockbench, saved as `art/blockbench/steam_turbine.bbmodel`. `art/tools/turbine_texture.py` paints the texture from the author's hand-made textures: the casing plates are the Steel Block, the back plate is the Reactor Vessel Casing with a pipe socket set into it, the dark inside faces, shaft and fixed vanes use the navy tones of the vessel casing's inset and the Reinforced Pipe, and the window frames and panes use the Shielded Glass tones. The tool checks that every tone it uses is still present in those four textures. The layout follows Create's casings and fan: bevelled plates with corner bolts, a flanged rim, and a solid hub with a ring of blades. Create Simulated's parts were looked at for style only and none of their pixels are used, because that art is all rights reserved. The two exhaust duct islands are the author's own painting and the tool keeps them as they are.
- Status: the model is the author's. The texture is script-painted, at the author's request.
- Repainting: run the tool with `fresh`. Without it the sheet is left alone, so hand edits are safe. `art/tools/turbine_model.py` then exports the model and texture into the game. `art/blockbench/steam_turbine_uv_guide.png` names the islands.
- Date: 2026-11-15

## Overhaul blocks (script-painted)

- Used by: `textures/block/reactor_viewport*.png`, `plant_screen.png`, `corium.png`, `corium_hot.png`, `textures/item/plant_linker.png`
- Source: painted by `art/tools/plant_blocks.py` and `art/tools/screen_blocks.py` from Create's iron, brass, copper and steel ramps. The cracked viewports use the casing crack overlays.

## Plant Screen frame

- Used by: `models/block/plant_screen.json` and `models/item/plant_screen.json`, which take Create's Display Board models (`create:block/display_board/block` and `item`) as their parent, and `textures/block/plant_screen_side.png`, `plant_screen_top.png`, `plant_screen_inside.png` and `plant_screen_back.png`.
- Source: Create's `flap_display_side.png`, `flap_display_top.png`, `flap_display_inside.png` and `dark_metal_block.png`, recoloured onto the colours of our `steel_block.png` by `art/tools/screen_blocks.py`. The recoloured originals are kept in `art/originals/plant_screen_*.png` and are only made again when the script is run with `fresh`. The screen face `plant_screen.png` is still painted by the same script.

## reactor_alloy_dashboard

- Used by: `textures/block/reactor_alloy_dashboard.png`
- Source: painted by hand on the same UV layout as Create: Loconautics' dashboard textures, so it fits their dashboard model. There is no generator for it: edit the PNG directly.

## Dashboard and parts from Create: Loconautics

- Used by: `models/block/reactor_alloy_dashboard/*.json`, `models/block/dashboard_part/*.json` except the alarm, `models/item/dashboard_button|switch|lever|gauge.json`, `textures/block/dashboard_part/*_atlas.png` except the alarm sheets, `textures/gui/dashboard_part.png`
- Source: Create: Loconautics by Lycoris, MDLOP, Basilforrealss, TugKanTheMan and Hatchet. Used with their permission, given on condition that the dashboard has its own colour theme and that Loconautics is credited. `art/tools/loconautics_assets.py` imports them from the jar in `libs/` and only writes files that are missing.
- Our additions: `art/tools/alloy_body.py` adds a reactor alloy row to the three body sheets, 24 pixels below the first frame.

## Dashboard Alarm

- Used by: `models/block/dashboard_part/alarm_lamp.json`, `alarm_cage.json`, `models/item/dashboard_alarm.json`, `textures/block/dashboard_part/alarm_atlas.png`, `alarm_atlas_lit.png`
- Source: `art/blockbench/alarm.bbmodel`, hand-made. `art/tools/alarm_model.py` exports it. The lamp and cage sheets are `art/originals/alarm_atlas.png` and `alarm_atlas_lit.png`: 35 lamp colours in cells of 9 by 8 pixels (side 4 by 7, top 4 by 4), with the cage pieces from x 46. The script paints the sheets only when they are missing or when run with `paint`, and re-maps the model's UVs only when run with `map`.

## alarm sound

- Used by: `sounds/alarm.ogg`, played by a desk with an unacknowledged Dashboard Alarm.
- Source: synthesised by `art/tools/alarm_sound.py`: two alternating horn tones built from sine harmonics, two seconds long so it repeats cleanly.

## centrifuge

- Used by: `textures/block/centrifuge.png` and the models `block/centrifuge.json`, `centrifuge_rotor.json` and `centrifuge_item.json`
- Source: `art/blockbench/centrifuge.bbmodel`, in two groups. `frame` is the part that stands still: a plate top and bottom with a post at each corner, so stacked centrifuges read as one cascade column. `rotor` is the part the shaft turns: an eight-sided drum between two square flanges on short necks. The drum is four slabs, two of them turned 45 degrees, and each slab shows only its two end faces.
- Texture: `art/tools/centrifuge_texture.py` paints the frame from the author's Steel Block, with a brass ring round the shaft socket in each plate, and the rotor in Create's brass tones with two plain hoops.
- Status: script-painted placeholder, not yet approved.
- Repainting: run the tool with `fresh`. Without it the sheet is left alone, so hand edits are safe. `art/tools/centrifuge_model.py` then exports the models and the blockstate.
- Date: 2026-11-15

## enriched_uranium and depleted_uranium

- Used by: `textures/item/enriched_uranium.png`, `textures/item/depleted_uranium.png`
- Source: drawn by `art/tools/plant_blocks.py` as recolours of `uranium_concentrate.png`.
- Date: 2026-11-12

## reinforced_valve, reinforced_valve_open and reinforced_valve_closed

- Used by: `textures/block/reinforced_valve.png`, `textures/block/reinforced_valve_open.png`, `textures/block/reinforced_valve_closed.png`
- Source: hand-made by the author in `art/originals/`, laid out like Create's `fluid_valve.png`, `valve_open.png` and `valve_closed.png`. The installed textures have their greys recoloured to the reactor alloy's shades by `art/tools/valve_paint.py`; the brass ring and the dial keep the original colours. The models parent Create's fluid valve models.
- Date: 2026-11-10
## reinforced_pipe and reinforced_pipe_connected

- Used by: `textures/block/reinforced_pipe.png`, `textures/block/reinforced_pipe_connected.png`
- Source: Create's `pipes.png` and `pipes_connected.png` (Create, MIT licence), recoloured from copper to the reactor alloy's blue-grey shades by `art/tools/pipe_paint.py`, flange rims included. The pipe models parent Create's fluid pipe models (`art/tools/pipe_models.py`).
- Date: 2026-10-19
