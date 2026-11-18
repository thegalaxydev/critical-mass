# Asset manifest

Made 2026-11-17 by `art/tools/manifest.py` from the blockstates, models, textures, sounds and names in `src/main/resources/assets/criticalmass`. The same list with every model path is in `ASSET_MANIFEST.json`.

Minecraft 1.21.1, NeoForge 21.1.262, Create 6.0.10-280, Aeronautics 1.3.2+mc1.21.1, Sable 2.0.6, Loconautics 0.1.2BETA.

The Art column says whose work each texture is. Only `author` is the author's own art. `script` was drawn by a script in `art/tools/` and `generated` is a cleaned generated image: both are placeholders waiting for the author's art, and neither is hand-made. `approved` marks a placeholder the author has accepted as it is. `not recorded` means the record does not say who made it. How each one was made is in `art/SOURCES.md`.

## Blocks (58)

| Registry ID | Name | Art | Textures | Other mods' textures | Models |
|---|---|---|---|---|---|
| `criticalmass:boiler_heating_plate` | Boiler Heating Plate | script | `block/boiler_heating_plate.png` 32x32<br>`block/steel_block.png` 16x16 |  | 4 |
| `criticalmass:burnt_equipment` | Burnt-Out Equipment (no item) | script | `block/burnt_equipment.png` 16x16<br>`block/burnt_equipment_bottom.png` 16x16<br>`block/burnt_equipment_top.png` 16x16 |  | 1 |
| `criticalmass:bus_bar` | Bus Bar | script | `block/bus_bar.png` 16x16 |  | 2 |
| `criticalmass:centrifuge` | Centrifuge | script | `block/centrifuge.png` 32x32 |  | 4 |
| `criticalmass:charred_log` | Charred Log | script | `block/charred_log.png` 16x16<br>`block/charred_log_top.png` 16x16 |  | 3 |
| `criticalmass:circuit_breaker` | Circuit Breaker | script | `block/circuit_breaker.png` 32x32<br>`block/steel_block.png` 16x16 |  | 3 |
| `criticalmass:circulation_pump` | Circulation Pump | author | `block/circulation_pump.png` 32x32 |  | 5 |
| `criticalmass:condenser` | Condenser | script | `block/condenser.png` 32x32<br>`block/reinforced_pipe.png` 32x32 |  | 3 |
| `criticalmass:control_channel` | Control Channel | script | `block/control_channel.png` 32x32 |  | 7 |
| `criticalmass:control_rod_drive` | Control Rod Drive | script | `block/control_rod_drive.png` 32x32<br>`block/control_rod_shaft.png` 16x16 | `create:block/cogwheel` | 5 |
| `criticalmass:coolant_channel` | Coolant Channel | script | `block/coolant_channel.png` 32x32 |  | 2 |
| `criticalmass:cooling_tower_packing` | Cooling Tower Packing | author | `block/cooling_tower_packing.png` 32x32<br>`block/cooling_tower_packing_bottom.png` 32x32<br>`block/cooling_tower_packing_middle.png` 32x32<br>`block/cooling_tower_packing_top.png` 32x32 |  | 5 |
| `criticalmass:corium` | Corium | author's texture, script glow | `block/corium.png` 16x16<br>`block/corium_glow.png` 16x64<br>`block/corium_hot.png` 16x16 |  | 3 |
| `criticalmass:deepslate_uranium_ore` | Deepslate Uranium Ore | script | `block/deepslate_uranium_ore.png` 16x16 |  | 2 |
| `criticalmass:diesel_engine` | Diesel Engine | script | `block/diesel_engine.png` 32x32<br>`block/steel_block.png` 16x16 |  | 2 |
| `criticalmass:disconnect_switch` | Disconnect Switch | script | `block/switchgear.png` 32x32 |  | 3 |
| `criticalmass:earthing_switch` | Earthing Switch | script | `block/steel_block.png` 16x16<br>`block/switchgear.png` 32x32 |  | 3 |
| `criticalmass:electric_motor` | Electric Motor | script | `block/electric_motor.png` 32x32<br>`block/steel_block.png` 16x16 |  | 2 |
| `criticalmass:fallout_ash` | Fallout Ash | script | `block/fallout_ash.png` 16x16<br>`block/fallout_ash_glow.png` 16x64<br>`item/fallout_ash.png` 16x16 |  | 2 |
| `criticalmass:fuel_channel` | Fuel Channel | script | `block/fuel_channel.png` 32x32<br>`block/fuel_channel_glow_1.png` 32x32<br>`block/fuel_channel_glow_2.png` 32x32<br>`block/fuel_channel_glow_3.png` 32x32 |  | 5 |
| `criticalmass:generator` | Generator | script | `block/generator.png` 32x32<br>`block/steel_block.png` 16x16 |  | 2 |
| `criticalmass:grid_connection` | Grid Connection | script | `block/grid_connection.png` 32x32<br>`block/steel_block.png` 16x16 |  | 2 |
| `criticalmass:heat_exchanger` | Heat Exchanger | script | `block/heat_exchanger.png` 32x32<br>`block/reinforced_pipe.png` 32x32 |  | 3 |
| `criticalmass:insulator` | Insulator | script | `block/insulator.png` 16x16 |  | 2 |
| `criticalmass:meltdown_siren` | Meltdown Siren | author's model, script texture edited by the author | `block/meltdown_siren.png` 64x64 |  | 5 |
| `criticalmass:open_waste_canister` | Open Waste Canister | author | `block/open_waste_canister.png` 32x32<br>`block/sealed_waste_canister.png` 32x32 |  | 3 |
| `criticalmass:part_panel` | Dashboard Parts (no item) | none of its own | `block/steel_block.png` 16x16 |  | 1 |
| `criticalmass:plant_screen` | Plant Screen | script | `block/plant_screen.png` 16x16<br>`block/plant_screen_back.png` 16x16<br>`block/plant_screen_inside.png` 16x16<br>`block/plant_screen_side.png` 16x16<br>`block/plant_screen_top.png` 16x16 |  | 2 |
| `criticalmass:pressurizer` | Pressurizer | script | `block/pressurizer.png` 32x32 |  | 3 |
| `criticalmass:pressurizer_top` | Pressurizer (no item) | script | `block/pressurizer.png` 32x32 |  | 1 |
| `criticalmass:primary_coolant_port` | Primary Coolant Port | script | `block/primary_coolant_port.png` 32x32<br>`block/reactor_vessel_casing.png` 16x16 |  | 3 |
| `criticalmass:radiator_panel` | Radiator | author | `block/radiator_panel.png` 32x32 |  | 2 |
| `criticalmass:reactor_alloy_block` | Block of Reactor Alloy | author | `block/reactor_alloy_block.png` 16x16 |  | 2 |
| `criticalmass:reactor_alloy_dashboard` | Reactor Alloy Dashboard | not recorded | `block/reactor_alloy_dashboard.png` 64x64 |  | 5 |
| `criticalmass:reactor_controller` | Reactor Controller | script | `block/reactor_controller.png` 32x32<br>`block/reactor_vessel_casing.png` 16x16 |  | 5 |
| `criticalmass:reactor_service_port` | Reactor Service Port | script | `block/reactor_service_port.png` 32x32<br>`block/reactor_vessel_casing.png` 16x16 |  | 5 |
| `criticalmass:reactor_signal_interface` | Reactor Signal Interface | script | `block/reactor_signal_interface.png` 32x32<br>`block/reactor_vessel_casing.png` 16x16 |  | 11 |
| `criticalmass:reactor_vessel_casing` | Reactor Vessel Casing | author | `block/casing_crack_1.png` 16x16<br>`block/casing_crack_2.png` 16x16<br>`block/casing_crack_3.png` 16x16<br>`block/reactor_vessel_casing.png` 16x16<br>`block/reactor_vessel_casing_connected.png` 128x128 |  | 5 |
| `criticalmass:reactor_viewport` | Reactor Viewport | script | `block/reactor_viewport.png` 32x32<br>`block/reactor_viewport_crack_1.png` 32x32<br>`block/reactor_viewport_crack_2.png` 32x32<br>`block/reactor_viewport_crack_3.png` 32x32 |  | 5 |
| `criticalmass:reinforced_pipe` | Reinforced Pipe | script | `block/reinforced_pipe.png` 32x32<br>`block/reinforced_pipe_connected.png` 32x32 |  | 57 |
| `criticalmass:reinforced_valve` | Reinforced Valve | author | `block/reinforced_valve.png` 32x32<br>`block/reinforced_valve_closed.png` 16x16<br>`block/reinforced_valve_open.png` 16x16 |  | 7 |
| `criticalmass:relief_valve` | Relief Valve | script, approved | `block/reinforced_pipe.png` 32x32<br>`block/relief_valve.png` 32x32 |  | 4 |
| `criticalmass:ruined_channel` | Ruined Fuel Channel | script | `block/ruined_channel.png` 32x32 |  | 2 |
| `criticalmass:sealed_waste_canister` | Sealed Waste Canister | author | `block/sealed_waste_canister.png` 32x32 |  | 2 |
| `criticalmass:shielded_cask` | Shielded Cask | script | `block/shielded_cask.png` 32x32<br>`block/steel_block.png` 16x16 |  | 4 |
| `criticalmass:shielded_glass` | Shielded Glass | author | `block/shielded_glass.png` 16x16<br>`block/shielded_glass_connected.png` 128x128 |  | 2 |
| `criticalmass:shielded_hatch` | Shielded Hatch | script | `block/shielded_hatch.png` 16x16 |  | 5 |
| `criticalmass:shielding_concrete` | Shielding Concrete | author | `block/shielding_concrete.png` 16x16 |  | 2 |
| `criticalmass:spent_fuel_rack` | Spent Fuel Rack | script | `block/spent_fuel_rack.png` 32x32<br>`block/steel_block.png` 16x16 |  | 7 |
| `criticalmass:station_battery` | Station Battery | script | `block/station_battery.png` 32x32<br>`block/station_battery_cell.png` 16x16<br>`block/steel_block.png` 16x16 |  | 3 |
| `criticalmass:steam_turbine` | Steam Turbine | author's model, script texture | `block/steam_turbine.png` 128x128 |  | 4 |
| `criticalmass:steam_turbine_structure` | Steam Turbine (no item) | author's model, script texture | `block/steam_turbine.png` 128x128 |  | 1 |
| `criticalmass:steel_block` | Block of Steel | author | `block/steel_block.png` 16x16 |  | 2 |
| `criticalmass:surge_arrester` | Surge Arrester | script | `block/surge_arrester.png` 16x16 |  | 2 |
| `criticalmass:transfer_switch` | Transfer Switch | script | `block/steel_block.png` 16x16<br>`block/transfer_switch.png` 32x32 |  | 4 |
| `criticalmass:transformer` | Transformer | script | `block/transformer.png` 32x32 |  | 2 |
| `criticalmass:uranium_ore` | Uranium Ore | script | `block/uranium_ore.png` 16x16 |  | 2 |
| `criticalmass:water_cannon` | Water Cannon | script | `block/water_cannon.png` 32x32 |  | 4 |

## Items (41)

| Registry ID | Name | Art | Textures | Other mods' textures | Models |
|---|---|---|---|---|---|
| `criticalmass:ceramic_fuel_pellet` | Ceramic Fuel Pellet | author | `item/ceramic_fuel_pellet.png` 16x16 |  | 1 |
| `criticalmass:control_rod` | Control Rod | author | `item/control_rod.png` 16x16 |  | 1 |
| `criticalmass:damaged_fuel_assembly` | Damaged Fuel Assembly | generated | `item/damaged_fuel_assembly.png` 16x16 |  | 1 |
| `criticalmass:dashboard_alarm` | Dashboard Alarm | not recorded | `block/dashboard_part/alarm_atlas.png` 64x64<br>`block/dashboard_part/big_body_atlas.png` 64x64<br>`block/dashboard_part/body_atlas.png` 64x64<br>`block/dashboard_part/button_atlas.png` 64x64<br>`block/dashboard_part/plate_atlas.png` 64x64 |  | 2 |
| `criticalmass:dashboard_button` | Dashboard Button | Create: Loconautics, with permission | `block/dashboard_part/body_atlas.png` 64x64<br>`block/dashboard_part/button_atlas.png` 64x64<br>`block/dashboard_part/plate_atlas.png` 64x64 |  | 1 |
| `criticalmass:dashboard_gauge` | Dashboard Gauge | Create: Loconautics, with permission | `block/dashboard_part/body_atlas.png` 64x64<br>`block/dashboard_part/gauge_atlas.png` 64x64<br>`block/dashboard_part/plate_atlas.png` 64x64 |  | 1 |
| `criticalmass:dashboard_lever` | Dashboard Lever | Create: Loconautics, with permission | `block/dashboard_part/lever_atlas.png` 64x64<br>`block/dashboard_part/slim_body_atlas.png` 64x64<br>`block/dashboard_part/slim_plate_atlas.png` 64x64 |  | 1 |
| `criticalmass:dashboard_switch` | Dashboard Switch | Create: Loconautics, with permission | `block/dashboard_part/slim_body_atlas.png` 64x64<br>`block/dashboard_part/slim_plate_atlas.png` 64x64<br>`block/dashboard_part/switch_atlas.png` 64x64 |  | 1 |
| `criticalmass:decontamination_compound` | Decontamination Compound | author | `item/decontamination_compound.png` 16x16 |  | 1 |
| `criticalmass:depleted_uranium` | Depleted Uranium | script | `item/depleted_uranium.png` 16x16 |  | 1 |
| `criticalmass:dosimeter` | Dosimeter | author | `item/dosimeter.png` 16x16 |  | 1 |
| `criticalmass:empty_fuel_assembly` | Empty Fuel Assembly | generated | `item/empty_fuel_assembly.png` 16x16 |  | 1 |
| `criticalmass:enriched_uranium` | Enriched Uranium | script | `item/enriched_uranium.png` 16x16 |  | 1 |
| `criticalmass:fuel_assembly` | Fresh Fuel Assembly | generated | `item/fuel_assembly.png` 16x16 |  | 1 |
| `criticalmass:fuel_cladding` | Fuel Cladding | generated | `item/fuel_cladding.png` 16x16 |  | 1 |
| `criticalmass:incomplete_fuel_assembly` | Incomplete Fuel Assembly | generated | `item/empty_fuel_assembly.png` 16x16 |  | 1 |
| `criticalmass:incomplete_sheared_fuel` | Partly Sheared Fuel Assembly | generated | `item/spent_fuel_assembly.png` 16x16 |  | 1 |
| `criticalmass:incomplete_waste_canister` | Incomplete Waste Canister | author | `block/open_waste_canister.png` 32x32<br>`block/sealed_waste_canister.png` 32x32 |  | 3 |
| `criticalmass:plant_linker` | Plant Linker | script | `item/plant_linker.png` 16x16 |  | 1 |
| `criticalmass:power_cable` | Power Cable | script | `item/power_cable.png` 16x16 |  | 1 |
| `criticalmass:power_wire` | Power Wire | script | `item/power_wire.png` 16x16 |  | 1 |
| `criticalmass:protective_boots` | Protective Boots | generated | `item/protective_boots.png` 16x16 |  | 1 |
| `criticalmass:protective_hood` | Protective Hood | generated | `item/protective_hood.png` 16x16 |  | 1 |
| `criticalmass:protective_leggings` | Protective Leggings | generated | `item/protective_leggings.png` 16x16 |  | 1 |
| `criticalmass:protective_suit` | Protective Suit | generated | `item/protective_suit.png` 16x16 |  | 1 |
| `criticalmass:radioisotope_cell` | Radioisotope Cell | script | `item/radioisotope_cell.png` 16x16 |  | 1 |
| `criticalmass:raw_uranium` | Raw Uranium | generated | `item/raw_uranium.png` 16x16 |  | 1 |
| `criticalmass:reactor_alloy_ingot` | Reactor Alloy Ingot | author | `item/reactor_alloy_ingot.png` 16x16 |  | 1 |
| `criticalmass:reactor_alloy_sheet` | Reactor Alloy Sheet | author | `item/reactor_alloy_sheet.png` 16x16 |  | 1 |
| `criticalmass:sealing_gasket` | Sealing Gasket | author | `item/sealing_gasket.png` 16x16 |  | 1 |
| `criticalmass:sheared_fuel` | Sheared Spent Fuel | script | `item/sheared_fuel.png` 16x16 |  | 1 |
| `criticalmass:spent_fuel_assembly` | Spent Fuel Assembly | generated | `item/spent_fuel_assembly.png` 16x16 |  | 1 |
| `criticalmass:steel_ingot` | Steel Ingot | author | `item/steel_ingot.png` 16x16 |  | 1 |
| `criticalmass:steel_nugget` | Steel Nugget | author | `item/steel_nugget.png` 16x16 |  | 1 |
| `criticalmass:steel_plate` | Steel Plate | author | `item/steel_plate.png` 16x16 |  | 1 |
| `criticalmass:strontium_90` | Strontium-90 | script | `item/strontium_90.png` 16x16 |  | 1 |
| `criticalmass:survey_meter` | Survey Meter | author | `item/survey_meter.png` 16x16 |  | 1 |
| `criticalmass:transmission_line` | Transmission Line | script | `item/transmission_line.png` 16x16 |  | 1 |
| `criticalmass:turbine_blade` | Turbine Blade | author | `item/turbine_blade.png` 16x16 |  | 1 |
| `criticalmass:uranium_concentrate` | Uranium Concentrate | generated | `item/uranium_concentrate.png` 16x16 |  | 1 |
| `criticalmass:welding_torch` | Welding Torch | author | `item/welding_torch.png` 16x16 |  | 1 |

## Fluids (4)

| Registry ID | Name | Art | Textures | Other mods' textures | Models |
|---|---|---|---|---|---|
| `criticalmass:contaminated_coolant` | Contaminated Coolant | generated | `fluid/contaminated_coolant_flow.png` 32x512<br>`fluid/contaminated_coolant_still.png` 16x256 |  | 0 |
| `criticalmass:fuel_oil` | Fuel Oil | script | `fluid/fuel_oil_flow.png` 32x512<br>`fluid/fuel_oil_still.png` 16x256 |  | 0 |
| `criticalmass:primary_coolant` | Primary Coolant | generated | `fluid/primary_coolant_flow.png` 32x512<br>`fluid/primary_coolant_still.png` 16x256 |  | 0 |
| `criticalmass:steam` | Steam | generated | `fluid/steam_flow.png` 32x512<br>`fluid/steam_still.png` 16x256 |  | 0 |

## Other textures (15)

Dashboard part sheets, the dashboard screen, the worn suit layers and effect sprites.

| Texture | Art |
|---|---|
| `block/dashboard_part/alarm_atlas_lit.png` 64x64 | not recorded |
| `block/dashboard_part/big_button_atlas.png` 64x64 | Create: Loconautics, with permission |
| `block/dashboard_part/glass.png` 16x16 | Create: Loconautics, with permission |
| `gui/dashboard_part.png` 512x256 | Create: Loconautics, with permission |
| `models/armor/protective_layer_1.png` 64x32 | generated |
| `models/armor/protective_layer_2.png` 64x32 | generated |
| `particle/glow.png` 64x64 | script |
| `particle/mote.png` 8x8 | script |
| `particle/ring.png` 64x64 | script |
| `particle/spark.png` 8x8 | script |
| `particle/steam_0.png` 16x16 | script |
| `particle/steam_1.png` 16x16 | script |
| `particle/steam_2.png` 16x16 | script |
| `particle/steam_3.png` 16x16 | script |
| `particle/streak.png` 16x4 | script |

## Sounds (31)

All are synthesised by `art/tools/sfx.py`, except the alarm, which is `art/tools/alarm_sound.py`, the nuclear explosion, which is a generated sound effect, and the two siren sounds, which `art/tools/siren_sound.py` cuts from a recording the author supplied.

| Sound | Subtitle | Art | File |
|---|---|---|---|
| `criticalmass:alarm` | Reactor alarm sounds | script | `sounds/alarm.ogg` |
| `criticalmass:hum` | Transformer hums | script | `sounds/hum.ogg` |
| `criticalmass:generator_run` | Generator drones | script | `sounds/generator_run.ogg` |
| `criticalmass:motor_run` | Motor whirrs | script | `sounds/motor_run.ogg` |
| `criticalmass:steam_hiss` | Steam hisses | script | `sounds/steam_hiss.ogg` |
| `criticalmass:reactor_thrum` | Reactor thrums | script | `sounds/reactor_thrum.ogg` |
| `criticalmass:breaker_close` | Breaker closes | script | `sounds/breaker_close.ogg` |
| `criticalmass:breaker_open` | Breaker opens | script | `sounds/breaker_open.ogg` |
| `criticalmass:breaker_trip` | Breaker trips | script | `sounds/breaker_trip.ogg` |
| `criticalmass:switch_throw` | Switch thrown | script | `sounds/switch_throw.ogg` |
| `criticalmass:relay_click` | Relay clicks | script | `sounds/relay_click.ogg` |
| `criticalmass:arc_zap` | Electricity arcs | script | `sounds/arc_zap.ogg` |
| `criticalmass:arc_blast` | Electrical explosion | script | `sounds/arc_blast.ogg` |
| `criticalmass:wire_sizzle` | Wire burns | script | `sounds/wire_sizzle.ogg` |
| `criticalmass:steam_vent` | Steam vents | script | `sounds/steam_vent.ogg` |
| `criticalmass:pipe_burst` | Pipe bursts | script | `sounds/pipe_burst.ogg` |
| `criticalmass:geiger` | Survey meter clicks | script | `sounds/geiger.ogg`<br>`sounds/geiger_2.ogg`<br>`sounds/geiger_3.ogg` |
| `criticalmass:order_placed` | Power order arrives | script | `sounds/order_placed.ogg` |
| `criticalmass:order_met` | Power order met | script | `sounds/order_met.ogg` |
| `criticalmass:order_missed` | Power order missed | script | `sounds/order_missed.ogg` |
| `criticalmass:wire_string` | Wire strung | script | `sounds/wire_string.ogg` |
| `criticalmass:wire_cut` | Wire cut | script | `sounds/wire_cut.ogg` |
| `criticalmass:engine_start` | Engine cranks | script | `sounds/engine_start.ogg` |
| `criticalmass:engine_run` | Engine runs | script | `sounds/engine_run.ogg` |
| `criticalmass:service` | Equipment serviced | script | `sounds/service.ogg` |
| `criticalmass:nuclear_blast` | Nuclear explosion | generated | `sounds/nuclear_blast.ogg` |
| `criticalmass:fallout` | Fallout drifts | script | `sounds/fallout.ogg` |
| `criticalmass:water_spray` | Water cannon sprays | script | `sounds/water_spray.ogg` |
| `criticalmass:pump_cavitation` | Pump cavitates | script | `sounds/pump_cavitation.ogg` |
| `criticalmass:siren` | Meltdown Siren wails | recording supplied by the author | `sounds/siren.ogg` |
| `criticalmass:siren_low` | Meltdown Siren wails | recording supplied by the author | `sounds/siren_low.ogg` |

## Problems

None: every model and texture a blockstate, item or sound points at exists, every texture is used, and everything has a name and an art record.
