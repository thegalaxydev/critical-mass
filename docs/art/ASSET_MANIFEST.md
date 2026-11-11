# Asset manifest

Generated 2026-10-19T10:48:00 from `art/tools/manifest.py`. Machine-readable version: `ASSET_MANIFEST.json` (UV regions, model and texture paths, kinetic specs).

Minecraft 1.21.1, NeoForge 21.1.262, Create 6.0.10-280, Aeronautics 1.3.2, Sable 2.0.6. 16 texels per block; items 16x16.

## Blocks (29)

| Registry ID | Textures | Generation | Integration | Verified | Issues |
|---|---|---|---|---|---|
| `criticalmass:reactor_vessel_casing` | `block/reactor_vessel_casing.png` 16x16 | generated | integrated | iso: yes, in_game: yes, installation: yes, moving_body: yes | No connected textures: tiled vessels show a frame on every block. Connected-texture support is a follow-up. |
| `criticalmass:circulation_pump` | `block/circulation_pump.png` 64x64<br>`block/circulation_pump_rotor.png` 16x16 | generated | integrated | iso: yes, in_game: yes, installation: yes, moving_body: yes, rotation_with_network: yes, rotation_on_tilted_body: yes, stops_when_overstressed: yes |  |
| `criticalmass:steam_turbine` | `block/steam_turbine.png` 32x32<br>`block/steam_turbine_rotor.png` 16x16 | generated | integrated | iso: yes, in_game: yes, installation: yes, moving_body: yes, rotation_with_network: yes, rotation_on_tilted_body: yes | Single block; the spec describes a 3 to 7 block multiblock. |
| `criticalmass:instrumentation_panel` | `block/instrumentation_panel.png` 32x32 | generated | integrated | iso: yes, in_game: yes, installation: yes, moving_body: yes | Needles and lamps are static artwork, drawn at rest/unlit: there is no controller link or telemetry yet. |
| `criticalmass:reactor_controller` | `block/reactor_controller.png` 16x16 | generated | integrated | iso: yes, in_game: yes, installation: yes |  |
| `criticalmass:fuel_channel` | `block/fuel_channel.png` 32x32 | generated | integrated | iso: yes, in_game: yes, installation: yes |  |
| `criticalmass:control_channel` | `block/control_channel.png` 32x32 | generated | integrated | iso: yes, in_game: yes, installation: yes |  |
| `criticalmass:control_rod_drive` | `block/control_rod_drive.png` 64x64<br>`block/control_rod_drive_pinion.png` 16x16 | generated | integrated | iso: yes, in_game: yes, installation: yes, moving_body: yes, rotation_with_network: yes, rotation_on_tilted_body: yes | The rack is static: rod insertion control is not implemented. |
| `criticalmass:coolant_channel` | `block/coolant_channel.png` 32x32 | generated | integrated | iso: yes, in_game: yes, installation: yes |  |
| `criticalmass:reactor_service_port` | `block/reactor_service_port.png` 16x16 | generated | integrated | iso: yes, in_game: yes, installation: yes |  |
| `criticalmass:primary_coolant_port` | `block/primary_coolant_port.png` 32x32 | generated | integrated | iso: yes, in_game: yes, installation: yes |  |
| `criticalmass:reinforced_pipe` | `block/reinforced_pipe.png` 32x32 | generated | integrated | iso: yes, in_game: yes, installation: yes | Carries coolant for a pump loop (prototype): pipes hold no fluid of their own and a loop must be a single path without branches. |
| `criticalmass:heat_exchanger` | `block/heat_exchanger.png` 32x32 | generated | integrated | iso: yes, in_game: yes, installation: yes |  |
| `criticalmass:condenser` | `block/condenser.png` 32x32 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:cooling_tower_packing` | `block/cooling_tower_packing.png` 32x32 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:radiator_panel` | `block/radiator_panel.png` 32x32 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:emergency_coolant_tank` | `block/emergency_coolant_tank.png` 32x32 | generated | integrated | iso: yes, in_game: yes, installation: yes |  |
| `criticalmass:relief_valve` | `block/relief_valve.png` 32x32 | generated | integrated | iso: yes, in_game: yes, installation: yes |  |
| `criticalmass:steam_bypass_valve` | `block/steam_bypass_valve.png` 32x32 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:boiler_heating_plate` | `block/boiler_heating_plate.png` 32x32 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:shielding_concrete` | `block/shielding_concrete.png` 16x16 | generated | integrated | iso: yes, in_game: yes, installation: yes |  |
| `criticalmass:shielded_glass` | `block/shielded_glass.png` 16x16 | generated | integrated | iso: yes, in_game: yes, installation: yes | Translucent; depth sorting against other translucent blocks has not been checked. |
| `criticalmass:shielded_hatch` | `block/shielded_hatch.png` 16x16 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:fuel_assembly_bench` | `block/fuel_assembly_bench.png` 64x64 | generated | integrated | iso: yes, in_game: yes, installation: yes |  |
| `criticalmass:spent_fuel_rack` | `block/spent_fuel_rack.png` 32x32 | generated | integrated | iso: yes, in_game: yes, installation: yes |  |
| `criticalmass:shielded_cask` | `block/shielded_cask.png` 32x32 | generated | integrated | iso: yes, in_game: yes, installation: yes |  |
| `criticalmass:reactor_signal_interface` | `block/reactor_signal_interface.png` 16x16 | generated | integrated | iso: yes, in_game: yes, installation: yes |  |
| `criticalmass:alarm_siren` | `block/alarm_siren.png` 32x32 | generated | integrated | iso: yes, in_game: yes, installation: yes, moving_body: yes |  |
| `criticalmass:docking_service_connector` | `block/docking_service_connector.png` 32x32 | generated | integrated | iso: yes, in_game: yes | Connected state exists only as a blockstate; docking behaviour is not implemented. |

## Items (25)

| Registry ID | Textures | Generation | Integration | Verified | Issues |
|---|---|---|---|---|---|
| `criticalmass:fuel_assembly` | `item/fuel_assembly.png` 16x16 | generated | integrated | iso: yes, in_game: yes | No remaining-fuel overlay: fuel item data is not implemented yet (tsk-070). |
| `criticalmass:survey_meter` | `item/survey_meter.png` 16x16 | generated | integrated | iso: yes, in_game: yes | Item has no behaviour yet (measurement is task tsk-073). |
| `criticalmass:protective_hood` | `item/protective_hood.png` 16x16 | generated | integrated | iso: yes, in_game: yes, worn: yes, worn_poses_front_back_sneak_walk_ride_swim: yes, armor_stand: yes, slim_model_same_layers_in_code: yes | First-person sleeves are not applicable: vanilla does not render armor on first-person arms.<br>Slim (Alex) model: checked in the 1.21.1 client code, not with an Alex skin in game. PlayerRenderer bakes PLAYER_SLIM_INNER/OUTER_ARMOR for slim players, and LayerDefinitions registers them with the same layer definitions as the wide model, so the outfit geometry and texture are identical. |
| `criticalmass:protective_suit` | `item/protective_suit.png` 16x16 | generated | integrated | iso: yes, in_game: yes, worn: yes, worn_poses_front_back_sneak_walk_ride_swim: yes, armor_stand: yes, slim_model_same_layers_in_code: yes |  |
| `criticalmass:protective_leggings` | `item/protective_leggings.png` 16x16 | generated | integrated | iso: yes, in_game: yes, worn: yes, worn_poses_front_back_sneak_walk_ride_swim: yes, armor_stand: yes, slim_model_same_layers_in_code: yes |  |
| `criticalmass:protective_boots` | `item/protective_boots.png` 16x16 | generated | integrated | iso: yes, in_game: yes, worn: yes, worn_poses_front_back_sneak_walk_ride_swim: yes, armor_stand: yes, slim_model_same_layers_in_code: yes |  |
| `criticalmass:raw_uranium` | `item/raw_uranium.png` 16x16 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:crushed_uranium` | `item/crushed_uranium.png` 16x16 | generated | integrated | iso: yes, in_game: yes | Create already ships create:crushed_raw_uranium (enabled by uranium tags); this addon item may be redundant. Needs a design decision. |
| `criticalmass:uranium_concentrate` | `item/uranium_concentrate.png` 16x16 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:ceramic_fuel_pellet` | `item/ceramic_fuel_pellet.png` 16x16 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:fuel_cladding` | `item/fuel_cladding.png` 16x16 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:empty_fuel_assembly` | `item/empty_fuel_assembly.png` 16x16 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:spent_fuel_assembly` | `item/spent_fuel_assembly.png` 16x16 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:damaged_fuel_assembly` | `item/damaged_fuel_assembly.png` 16x16 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:reactor_alloy_ingot` | `item/reactor_alloy_ingot.png` 16x16 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:reactor_alloy_sheet` | `item/reactor_alloy_sheet.png` 16x16 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:shielding_plate` | `item/shielding_plate.png` 16x16 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:control_rod` | `item/control_rod.png` 16x16 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:turbine_blade` | `item/turbine_blade.png` 16x16 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:sealing_gasket` | `item/sealing_gasket.png` 16x16 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:dosimeter` | `item/dosimeter.png` 16x16 | generated | integrated | iso: yes, in_game: yes | Item has no behaviour yet (task tsk-074). |
| `criticalmass:decontamination_compound` | `item/decontamination_compound.png` 16x16 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:repair_kit` | `item/repair_kit.png` 16x16 | generated | integrated | iso: yes, in_game: yes |  |
| `criticalmass:sealed_waste_canister` | `item/sealed_waste_canister.png` 16x16 | generated | integrated | iso: yes, in_game: yes |  |
| `TBD` | none | deferred | deferred | n/a | Deferred: depends on a filter/purification system that is not designed. No art made on purpose. |

## Fluids (4)

| Registry ID | Textures | Generation | Integration | Verified | Issues |
|---|---|---|---|---|---|
| `criticalmass:primary_coolant` | `fluid/primary_coolant_still.png` 16x256<br>`fluid/primary_coolant_flow.png` 32x512 | generated | integrated | in_game_create_tank: yes, animated: yes | No world block or bucket by design; it is only visible inside Create tanks and pipes (animation confirmed in a tank in game). Textures are stitched into the block atlas through assets/minecraft/atlases/blocks.json (directory source fluid/). |
| `criticalmass:steam` | `fluid/steam_still.png` 16x256<br>`fluid/steam_flow.png` 32x512 | generated | integrated | in_game_create_tank: yes, animated: yes | No world block or bucket by design; it is only visible inside Create tanks and pipes (animation confirmed in a tank in game). Textures are stitched into the block atlas through assets/minecraft/atlases/blocks.json (directory source fluid/). |
| `criticalmass:contaminated_coolant` | `fluid/contaminated_coolant_still.png` 16x256<br>`fluid/contaminated_coolant_flow.png` 32x512 | generated | integrated | in_game_create_tank: yes, animated: yes | No world block or bucket by design; it is only visible inside Create tanks and pipes (animation confirmed in a tank in game). Textures are stitched into the block atlas through assets/minecraft/atlases/blocks.json (directory source fluid/). |
| `minecraft:water` | none | vanilla | vanilla | n/a |  |

## Kinetic blocks

### `criticalmass:circulation_pump`

- **role**: consumer
- **impact su**: 4.0 per RPM (CMStress.CIRCULATION_PUMP_IMPACT, provisional)
- **block class**: DirectionalAxisKineticBlock (facing = outlet, axis_along_first selects the shaft axis perpendicular to facing)
- **shaft faces**: both faces on the rotation axis (hasShaftTowards: face.axis == rotation axis)
- **axis**: perpendicular to the fluid axis; 2 options per facing
- **placement**: 6 facings x 2 shaft orientations; facing defaults to looking direction, shift reverses
- **rotating**: circulation_pump_rotor partial: Create shaft (create:block/axis) plus two brass drive hubs on the shaft faces
- **stationary**: housing, copper pipe body, red outlet and blue inlet flanges
- **direction**: rotor turns exactly with network speed and direction (Create SingleAxisRotatingVisual / KineticBlockEntityRenderer); 1:1 with input shaft
- **renderer**: Flywheel visual SingleAxisRotatingVisual.of(CIRCULATION_PUMP_ROTOR); fallback BER AxialRotorRenderer when visualization is unsupported
- **partials**: circulation_pump_rotor

### `criticalmass:steam_turbine`

- **role**: generator (GeneratingKineticBlockEntity) running on steam energy (heat above saturated water); 16 RPM whenever it delivers capacity; admission cut by redstone governor, upstream bypass valve, or a disconnected shaft
- **impact su**: capacity = delivered heat x 4 SU per HU/t (CMThermal.SU_PER_HEAT), independent of RPM; 0 with no delivered heat
- **block class**: HorizontalAxisKineticBlock (blockstate property 'axis' = x|z)
- **shaft faces**: both end plates on the axis
- **axis**: horizontal X or Z
- **placement**: axis follows player look; prefers adjoining shafts
- **rotating**: steam_turbine_rotor partial: Create shaft plus bladed disc visible through the slotted window
- **stationary**: end plates, riveted drum, slotted window band, steam inlet (top) and exhaust (bottom) flanges
- **direction**: rotor follows network speed and direction 1:1
- **renderer**: Flywheel visual SingleAxisRotatingVisual.of(STEAM_TURBINE_ROTOR); fallback BER AxialRotorRenderer
- **partials**: steam_turbine_rotor

### `criticalmass:control_rod_drive`

- **role**: consumer
- **impact su**: 1.0 per RPM (CMStress.CONTROL_ROD_DRIVE_IMPACT, provisional)
- **block class**: HorizontalKineticBlock (blockstate 'facing'; front = latch lever, faces the player when placed)
- **shaft faces**: back face only (hasShaftTowards: face == facing.getOpposite())
- **axis**: facing axis
- **placement**: 4 horizontal facings
- **rotating**: control_rod_drive_pinion partial: half shaft from the back face plus the pinion gear
- **stationary**: base plate, brass housing, red latch, rack (drawn at the stored full-insertion position; rod movement is not implemented)
- **direction**: pinion follows network speed and direction 1:1; the rack does not move because insertion control is not implemented (M4 tasks)
- **renderer**: Flywheel OrientedRotatingVisual (UP to input side); fallback BER InputSideRotorRenderer
- **partials**: control_rod_drive_pinion

## Inventory icon vs placed appearance

- `reactor_vessel_casing`: inventory = item model reactor_vessel_casing (parent: block model); placed = blockstate reactor_vessel_casing (raw); crack=0..3 overlay models (Minecraft's block breaking textures destroy_stage_3, 6 and 9) follow integrity tracked by the controller: 75%+ intact, 50-75% stage 1, 25-50% stage 2, under 25% stage 3
- `circulation_pump`: inventory = item model circulation_pump (merged housing + rotor, static); placed = blockstate circulation_pump (dir_axis)
- `steam_turbine`: inventory = item model steam_turbine (merged housing + rotor, static); placed = blockstate steam_turbine (h_axis)
- `instrumentation_panel`: inventory = item model instrumentation_panel (parent: block model); placed = blockstate instrumentation_panel (horizontal)
- `reactor_controller`: inventory = item model reactor_controller (parent: block model); placed = blockstate reactor_controller (horizontal)
- `fuel_channel`: inventory = item model fuel_channel (parent: block model); placed = blockstate fuel_channel (raw); rotated pillar (axis)
- `control_channel`: inventory = item model control_channel (parent: block model); placed = blockstate control_channel (raw); rotated pillar (axis)
- `control_rod_drive`: inventory = item model control_rod_drive (parent: block model); placed = blockstate control_rod_drive (horizontal)
- `coolant_channel`: inventory = item model coolant_channel (parent: block model); placed = blockstate coolant_channel (raw); rotated pillar (axis)
- `reactor_service_port`: inventory = item model reactor_service_port (parent: block model); placed = blockstate reactor_service_port (facing)
- `primary_coolant_port`: inventory = item model primary_coolant_port (parent: block model); placed = blockstate primary_coolant_port (raw); mode=inlet|outlet toggled with Create's wrench (CoolantPortBlock.onWrenched); inlet = blue collar, outlet = red collar
- `reinforced_pipe`: inventory = item model reinforced_pipe (parent: block model); placed = blockstate reinforced_pipe (raw); six connection booleans from real neighbours (pipes, coolant port fronts, pump fluid axis, turbine top/bottom, relief valve back, PipeConnectable blocks); multipart core + arms. Does not transport fluid yet.
- `heat_exchanger`: inventory = item model heat_exchanger (parent: block model); placed = blockstate heat_exchanger (horizontal)
- `condenser`: inventory = item model condenser (parent: block model); placed = blockstate condenser (horizontal)
- `cooling_tower_packing`: inventory = item model cooling_tower_packing (parent: block model); placed = blockstate cooling_tower_packing (simple)
- `radiator_panel`: inventory = item model radiator_panel (parent: block model); placed = blockstate radiator_panel (horizontal)
- `emergency_coolant_tank`: inventory = item model emergency_coolant_tank (parent: block model); placed = blockstate emergency_coolant_tank (simple)
- `relief_valve`: inventory = item model relief_valve (parent: block model); placed = blockstate relief_valve (facing); venting=true|false; inline on pipe (faces off the vent axis), vents through its front at 8 bar until 6 bar, redstone output while venting
- `steam_bypass_valve`: inventory = item model steam_bypass_valve (parent: block model); placed = blockstate steam_bypass_valve (horizontal)
- `boiler_heating_plate`: inventory = item model boiler_heating_plate (parent: block model); placed = blockstate boiler_heating_plate (horizontal)
- `shielding_concrete`: inventory = item model shielding_concrete (parent: block model); placed = blockstate shielding_concrete (simple)
- `shielded_glass`: inventory = item model shielded_glass (parent: block model); placed = blockstate shielded_glass (simple)
- `shielded_hatch`: inventory = item model shielded_hatch (parent: block model); placed = blockstate shielded_hatch (raw); vanilla iron-trapdoor behaviour: open/close by redstone only; models use vanilla trapdoor templates
- `fuel_assembly_bench`: inventory = item model fuel_assembly_bench (parent: block model); placed = blockstate fuel_assembly_bench (horizontal)
- `spent_fuel_rack`: inventory = item model spent_fuel_rack (parent: block model); placed = blockstate spent_fuel_rack (simple)
- `shielded_cask`: inventory = item model shielded_cask (parent: block model); placed = blockstate shielded_cask (simple)
- `reactor_signal_interface`: inventory = item model reactor_signal_interface (parent: block model); placed = blockstate reactor_signal_interface (horizontal)
- `alarm_siren`: inventory = item model alarm_siren (parent: block model); placed = blockstate alarm_siren (raw); powered=true when the block receives redstone; powered model uses a lit, unshaded lamp
- `docking_service_connector`: inventory = item model docking_service_connector (parent: block model); placed = blockstate docking_service_connector (raw); connected=true model (green lamp, clamp jaws) exists, but nothing sets the state yet: docking mechanics are not implemented (reachable only by command or debug stick)

## Equipment

- `protective_hood`: `models/armor/protective_layer_1.png` (64x32), ArmorMaterial 'criticalmass:protective' (Registries.ARMOR_MATERIAL) with Layer('criticalmass:protective'); 1.21.1 humanoid armor layers
- `protective_suit`: `models/armor/protective_layer_1.png` (64x32), ArmorMaterial 'criticalmass:protective' (Registries.ARMOR_MATERIAL) with Layer('criticalmass:protective'); 1.21.1 humanoid armor layers
- `protective_leggings`: `models/armor/protective_layer_2.png` (64x32), ArmorMaterial 'criticalmass:protective' (Registries.ARMOR_MATERIAL) with Layer('criticalmass:protective'); 1.21.1 humanoid armor layers
- `protective_boots`: `models/armor/protective_layer_1.png` (64x32), ArmorMaterial 'criticalmass:protective' (Registries.ARMOR_MATERIAL) with Layer('criticalmass:protective'); 1.21.1 humanoid armor layers
