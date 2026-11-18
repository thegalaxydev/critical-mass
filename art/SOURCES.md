# Generated art sources

Every texture in `src/main/resources/assets/criticalmass/textures/` that is a generated image is listed here.
Raw outputs (downscaled to 512 px JPEG) are in `art/generated/`.
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
| Boiler Heating Plate | Script | `art/tools/boiler_plate_texture.py` |
| Generator, Circuit Breaker, Grid Connection, Insulator, Electric Motor, Bus Bar, Transformer, Disconnect Switch, Earthing Switch, Station Battery, Diesel Engine, Transfer Switch, Surge Arrester, Power Wire, Power Cable, Transmission Line | Script | `art/tools/power_texture.py` |
| Fuel Oil | Script | `art/tools/fuel_texture.py` |
| Burnt-Out Equipment | Script | `art/tools/burnt_texture.py` darkens the author's Steel Block and cuts a blown-out panel into it |
| Effect sprites: steam, glow, shockwave ring, spark, mote and arc streak | Script | `art/tools/effect_texture.py` |
| Sounds other than the alarm and the nuclear explosion | Script | `art/tools/sfx.py` |
| Nuclear explosion sound | Generated | A generated sound effect, trimmed and levelled |
| Fallout Ash | Script | `art/tools/ash_texture.py`: a grey-green ash and a sheet of green specks the game draws over it at full brightness |
| Fallout Ash (item), Strontium-90, Radioisotope Cell and the cell marker on the Station Battery | Script | `art/tools/isotope_art.py` |
| Water Cannon | Script | `art/tools/water_cannon_texture.py`, on `art/blockbench/water_cannon.bbmodel` |
| Charred Log | Script | `art/tools/charred_texture.py`: vanilla's oak log repainted in the Corium's crust tones |
| Fuel, Control and Coolant Channel, Ruined Fuel Channel | Script | `art/tools/channel_texture.py`, in the tones of the author's Control Rod icon and Reactor Vessel Casing |
| Spent Fuel Rack | Script | `art/tools/rack_texture.py` |
| Shielded Cask | Script | `art/tools/cask_texture.py` |
| Control Rod Drive and its rod | Script | `art/tools/rod_drive_texture.py`; the cogwheel is Create's own model |
| Pressurizer | Script | `art/tools/pressurizer_texture.py`; the dial on it is the Dashboard Gauge |
| Centrifuge | Script | `art/tools/centrifuge_texture.py` |
| Reactor Viewport and its cracked stages | Script | `art/tools/viewport_texture.py`: the frame is the author's Reactor Vessel Casing, the pane is in the tones of the author's Shielded Glass and the cracks are the author's casing cracks |
| Corium | Script | `art/tools/corium_texture.py` |
| Plant Screen | Script | `art/tools/screen_blocks.py`: the face is painted, the frame is Create's Display Board recoloured |
| Reinforced Pipe | Script | Create's pipe sheets recoloured |
| Uranium Ore, Deepslate Uranium Ore | Script | `art/tools/ore_paint.py`: vanilla's iron ore with its ore pixels in the Raw Uranium icon's greens |
| Reactor Alloy Dashboard texture, Dashboard Alarm | Not recorded | Their sections say hand-made without saying by whom |
| Dashboard and its other parts | Create: Loconautics | Used with its authors' permission |
| Steel Ingot, Nugget and Plate, Control Rod, Turbine Blade, Sealing Gasket, Welding Torch, Dosimeter, Survey Meter, Decontamination Compound, Ceramic Fuel Pellet, Reactor Alloy Ingot and Sheet | Author | `art/originals/` |
| Raw Uranium, Crushed Uranium, Uranium Concentrate, Fuel Cladding, the four Fuel Assembly icons, the four Protective Suit icons and the worn suit layers | Generated | |
| Enriched Uranium, Depleted Uranium | Script | `art/tools/plant_blocks.py`: recolours of Uranium Concentrate |
| Plant Linker | Script | `art/tools/linker_texture.py`, in the tones of the author's Dosimeter and Survey Meter |
| Primary Coolant, Contaminated Coolant and Steam fluids | Generated | |
| Alarm sound | Script | `art/tools/alarm_sound.py` |

## Script-painted block textures (2026-10-19)

`art/tools/handmade.py` draws these from Create textures (MIT) plus pixels the script places, replacing the earlier generated versions. They are placeholders, not the author's art:
- `primary_coolant_port` (fronts and nozzle): the casing frame with copper flanges and blue (inlet) and red (outlet) collars.
- `shielded_hatch`: Create's train trapdoor recoloured to iron with a hazard band.
- Plant machines (`art/tools/handmade_plant.py`): `emergency_coolant_tank` on Create's fluid tank recoloured to dark steel with red bands, a sight glass and a valve wheel.
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
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

## protective_layer_1

- Used by: `textures/models/armor/protective_layer_1.png`
- Raw output: `art/generated/armor__protective_layer_1-v1.jpg`
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

## protective_layer_2

- Used by: `textures/models/armor/protective_layer_2.png`
- Raw output: `art/generated/armor__protective_layer_2-v1.jpg`
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

## boiler_heating_plate

- Used by: `textures/block/boiler_heating_plate.png` and the models `block/boiler_heating_plate.json`, `boiler_heating_plate_warm.json` and `boiler_heating_plate_hot.json`
- Source: `art/blockbench/boiler_heating_plate.bbmodel`. The group `body` is a steel base with a post at each corner, a copper plate on top that touches the tank above, and a flanged pipe socket on the two ends. Each of the groups `coil_cold`, `coil_warm` and `coil_hot` holds the same coil of three tube rows joined by two bends, painted for one heat level, so show one group at a time in Blockbench. The hot coil is exported at full brightness.
- Texture: `art/tools/boiler_plate_texture.py` paints the plate and coil from the copper tones in `docs/art/palette.json` and the sockets in the author's Steel Block tones. The base and posts use the Steel Block texture itself.
- Status: script-painted placeholder, not yet approved.
- Repainting: run the tool with `fresh`. Without it the sheet is left alone, so hand edits are safe. `art/tools/boiler_plate_model.py` then exports the three models and the blockstate.
- Date: 2026-11-16

## generator, circuit_breaker, grid_connection, insulator, electric_motor, bus_bar, transformer and the three wires

- Used by: `textures/block/generator.png`, `circuit_breaker.png`, `grid_connection.png`, `insulator.png`, `electric_motor.png`, `bus_bar.png`, `transformer.png` and `textures/item/power_wire.png`, `power_cable.png` and `transmission_line.png`, and the models `block/generator.json`, `circuit_breaker.json`, `circuit_breaker_closed.json`, `grid_connection.json`, `insulator.json`, `electric_motor.json`, `bus_bar.json` and `transformer.json`
- Source: one Blockbench file each in `art/blockbench/`: `generator.bbmodel`, `circuit_breaker.bbmodel`, `grid_connection.bbmodel`, `insulator.bbmodel`, `electric_motor.bbmodel`, `bus_bar.bbmodel`, `transformer.bbmodel`, `disconnect_switch.bbmodel`, `earthing_switch.bbmodel`, `station_battery.bbmodel`, `diesel_engine.bbmodel`, `transfer_switch.bbmodel` and `surge_arrester.bbmodel`.
  - The generator is a barrel between two flanges with a copper winding band and a terminal stud on one side. The game turns it so the stud is on top when the shaft lies flat.
  - The breaker is a steel cabinet with a door, a handle and two lamps. The groups `open` and `closed` each hold the handle and lamps for one state, so show one at a time. Its top has the two terminal rings.
  - The grid connection is a ribbed tank with three bushings. Wires land on the middle one.
  - The insulator is a ceramic post with two sheds on a steel foot and a copper cap.
  - The motor is a finned frame on two skids with an end bell at the shaft end, a fan cover at the other and a terminal box on top. Its shaft is Create's own half shaft, drawn by the game.
  - The bus bar is drawn standing up: a copper bar with a ceramic stand-off and foot plate on its north side. The game lays it flat so the stand-off points down.
  - The transformer is a tank with a bank of cooling fins on its front and back, a lid, a tall bushing on the high side, a short one on the low side and a small oil tank between them.
  - The disconnect switch is two ceramic posts with copper caps on a steel base. The groups `open` and `closed` hold the blade standing up on one post or lying across both, so show one at a time. Both share the sheet `switchgear.png` with the earthing switch.
  - The earthing switch is one post beside a striped earth bar, with the blade stowed up the bar in `open` and bridging to the post in `closed`.
  - The surge arrester is a taller post than the insulator: a dark red housing with three sheds on a steel foot, a steel plate and a copper cap.
  - The station battery is the breaker's cabinet with a door of nine cell caps over a charge strip, and one terminal ring on top.
  - The diesel engine is a block on a skid with a cylinder head, an exhaust stack and a flywheel housing at the shaft end. Its shaft is Create's own half shaft, drawn by the game.
  - The transfer switch is the same cabinet with a hub and a handle. The groups `normal`, `standby` and `off` each hold the handle and the two lamps for one position, so show one at a time: the handle points at the side whose supply is in use and hangs down between them.
- Texture: `art/tools/power_texture.py` paints all of them. The three wire icons are one coil in three sets of tones. The dark panels take the Reactor Vessel Casing's tones, the steel is the author's Steel Block, and the copper, ceramic and lamp tones come from `docs/art/palette.json`.
- The wires themselves have no texture: the game draws them as strips between the terminals, copper for Power Wire, thick and black for Power Cable and thin and silver for Transmission Line, turning orange as they heat.
- Status: script-painted placeholders, not yet approved.
- Repainting: run the tool with `fresh`. Without it the sheets are left alone, so hand edits are safe. `art/tools/power_model.py` then exports the models, the blockstates and the item models.
- Date: 2026-11-16

## Effect sprites

- Used by: `textures/particle/steam_0.png` to `steam_3.png`, `glow.png`, `ring.png`, `spark.png`, `mote.png` and `streak.png`, through the definitions in `particles/`
- Source: `art/tools/effect_texture.py` draws every sprite. The four steam puffs are lumps of round cloud in three steps of opacity; smoke and the tower plume are the same puffs tinted and sized by the game. The glow and ring are plain falloffs, drawn additively. The streak is the cross-section of an arc.
- The bolts themselves are not textures: the game runs a smooth curve through a few random points, draws it as a tapered ribbon that fades from a white core to a blue edge, and moves the points every three ticks so the bolt writhes.
- Status: script-drawn, not yet approved.
- Repainting: run the tool with `fresh`. It also rewrites the particle definitions.
- Date: 2026-11-16

## Sounds

- Used by: every file in `sounds/` except `alarm.ogg` and `nuclear_blast.ogg`, through `sounds.json`
- Source: `art/tools/sfx.py` synthesises each one from square, saw, triangle and sine waves and filtered noise at 22,050 Hz, reduced to 5 to 8 bits. Nothing is recorded or sampled.
- Takes: the Survey Meter's click is three files, `geiger.ogg`, `geiger_2.ogg` and `geiger_3.ogg`, at three pitches, so a run of clicks does not repeat one sample.
- Loops: `hum`, `generator_run`, `motor_run`, `steam_hiss`, `reactor_thrum`, `water_spray` and `pump_cavitation` are cut to whole cycles so they repeat without a click.
- Status: script-made, not yet approved. They have been checked for level and that they load; they have not been listened to in game.
- Remaking: run the tool with `fresh`. Naming sounds after it remakes only those, as in `sfx.py fresh water_spray`.
- Date: 2026-11-16

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
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

## emergency_coolant_tank

- Used by: `textures/block/emergency_coolant_tank.png`
- Raw output: `art/generated/emergency_coolant_tank-v1.jpg`
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

## contaminated_coolant

- Used by: `textures/fluid/contaminated_coolant_still.png + _flow.png`
- Raw output: `art/generated/fluid__contaminated_coolant-v1.jpg`
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

## primary_coolant

- Used by: `textures/fluid/primary_coolant_still.png + _flow.png`
- Raw output: `art/generated/fluid__primary_coolant-v1.jpg`
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

## steam

- Used by: `textures/fluid/steam_still.png + _flow.png`
- Raw output: `art/generated/fluid__steam-v1.jpg`
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

## fuel_assembly_bench

- Used by: `textures/block/fuel_assembly_bench.png`
- Raw output: `art/generated/fuel_assembly_bench-v1.jpg`
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

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
- Texture: `art/tools/exchanger_texture.py` paints each barrel as a banded tube with two hoops, a highlight row over a shadow row, using the shading in `art/tools/create_style.py`. The Heat Exchanger is in Create's copper tones with flanges of the author's Steel Block. The Condenser is the Steel Block's tones with flanges painted as a framed copper plate with a diagonal sheen.
- Status: script-painted placeholders, not yet approved.
- Repainting: run the tool with `fresh`. Without it the sheets are left alone, so hand edits are safe. `art/tools/exchanger_model.py` then exports the models and blockstates.
- Date: 2026-11-15

## instrumentation_panel

- Used by: `textures/block/instrumentation_panel.png`
- Raw output: `art/generated/instrumentation_panel-v2.jpg`
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

## ceramic_fuel_pellet (the author's own)

- Used by: `textures/item/ceramic_fuel_pellet.png`
- Source: hand-made by the author, original in `art/originals/ceramic_fuel_pellet.png`. Replaces the earlier generated icon.
- Date: 2026-11-10
## crushed_uranium

- Used by: `textures/item/crushed_uranium.png`
- Raw output: `art/generated/item__crushed_uranium-v1.jpg`
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

## damaged_fuel_assembly

- Used by: `textures/item/damaged_fuel_assembly.png`
- Raw output: `art/generated/item__damaged_fuel_assembly-v2.jpg`
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

## empty_fuel_assembly

- Used by: `textures/item/empty_fuel_assembly.png`
- Raw output: `art/generated/item__empty_fuel_assembly-v2.jpg`
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

## fuel_assembly

- Used by: `textures/item/fuel_assembly.png`
- Raw output: `art/generated/item__fuel_assembly-v2.jpg`
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

## fuel_cladding

- Used by: `textures/item/fuel_cladding.png`
- Raw output: `art/generated/item__fuel_cladding-v1.jpg`
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

## protective_boots

- Used by: `textures/item/protective_boots.png`
- Raw output: `art/generated/item__protective_boots-v2.jpg`
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

## protective_hood

- Used by: `textures/item/protective_hood.png`
- Raw output: `art/generated/item__protective_hood-v2.jpg`
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

## protective_leggings

- Used by: `textures/item/protective_leggings.png`
- Raw output: `art/generated/item__protective_leggings-v2.jpg`
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

## protective_suit

- Used by: `textures/item/protective_suit.png`
- Raw output: `art/generated/item__protective_suit-v2.jpg`
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

## raw_uranium

- Used by: `textures/item/raw_uranium.png`
- Raw output: `art/generated/item__raw_uranium-v1.jpg`
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

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
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

## uranium_concentrate

- Used by: `textures/item/uranium_concentrate.png`
- Raw output: `art/generated/item__uranium_concentrate-v1.jpg`
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

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
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

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
- Processing: generated from the UV blockout, then deterministic finalize (crop, majority sample, material-ramp quantize, locked pixels)
- Date: 2026-10-18

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

## Reactor Viewport, Corium, Ruined Fuel Channel, Uranium Ores and Plant Linker

- Reactor Viewport: `art/blockbench/reactor_viewport.bbmodel`, in two groups. `frame` is twelve bars two pixels thick round the edges of the block, and `glass` is a pane set two pixels back on every side. `art/tools/viewport_texture.py` paints the sheet and its three cracked stages, and `art/tools/viewport_model.py` exports the models.
- Corium: a plain cube. `art/tools/corium_texture.py` paints a crust of plates split by one fissure and two branches, a hot version with the fissure lit, and `corium_glow.png`, which the hot model draws over the crust at full brightness and which pulses over four frames. The blockstate turns each block one of four ways so a pool does not repeat.
- Ruined Fuel Channel: `art/blockbench/ruined_channel.bbmodel`, in three groups: `plates`, `rods` (stumps standing on the bottom plate and stubs hanging from the top one, with one rod left whole) and `slag`. `art/tools/channel_texture.py` paints its sheet in the Corium's crust tones and `art/tools/channel_model.py` exports it.
- Uranium Ore and Deepslate Uranium Ore: `art/tools/ore_paint.py` takes vanilla's iron ore and deepslate iron ore and repaints only their ore pixels.
- Plant Linker: `art/tools/linker_texture.py` draws a handset with a screen, two buttons and an aerial.
- Status: script-painted placeholders, not yet approved. Each script paints only when run with `fresh`.
- Date: 2026-11-17

## Plant Screen

- Used by: `textures/block/plant_screen.png`
- Source: painted by `art/tools/screen_blocks.py`.

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
- Source: synthesised by `art/tools/alarm_sound.py`: three short horn blasts a second on one fixed pitch, each a rough chord of two notes a semitone apart over the octave below, with its harmonics lifted around 3 kHz. Two seconds long so it repeats cleanly. Run the tool with `fresh` to write it again.

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

## water_cannon

- Used by: `textures/block/water_cannon.png` and the models `block/water_cannon.json`, `water_cannon_head.json` and `water_cannon_item.json`
- Source: `art/blockbench/water_cannon.bbmodel`, in two groups. `base` is the housing that stands still, 12 high so a pipe meets it on any side. `head` is the part that turns: a brass swivel, a copper body and a barrel with a brass nozzle, tilted up 22.5 degrees and pointing north. The game turns the head about the middle of the block.
- Texture: `art/tools/water_cannon_texture.py` paints the housing from the author's Steel Block, with a copper pipe collar on each side and a brass ring round the shaft socket underneath, and the head in Create's brass and copper tones.
- Status: script-painted placeholder, not yet approved.
- Repainting: run the tool with `fresh`. `art/tools/water_cannon_model.py` then exports the models and the blockstate. If the barrel is moved, `NOZZLE_OUT` and `NOZZLE_UP` in `WaterCannonBlockEntity` say where the jet leaves it.
- Date: 2026-11-17

## fallout_ash (item), strontium_90, sheared_fuel, radioisotope_cell and station_battery_cell

- Used by: `textures/item/fallout_ash.png`, `textures/item/strontium_90.png`, `textures/item/sheared_fuel.png`, `textures/item/radioisotope_cell.png` and `textures/block/station_battery_cell.png`, the lit cell that `block/station_battery_cell.json` puts on the door of a Station Battery with a cell fitted
- Source: drawn by `art/tools/isotope_art.py` from pixel maps in the script. The cell's casing takes its tones from the author's Reactor Alloy Ingot.
- Status: script-painted placeholders, not yet approved.
- Repainting: run the tool with `fresh`, and name the pieces after it to repaint only those, as in `isotope_art.py fresh sheared_fuel`. `art/tools/power_model.py` writes the cell model and the battery's blockstate. The Partly Sheared Fuel Assembly has no art of its own: its model shows the Spent Fuel Assembly.
- Date: 2026-11-17
