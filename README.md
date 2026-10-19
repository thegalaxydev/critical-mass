# Create: Critical Mass

Nuclear engineering addon for [Create](https://modrinth.com/mod/create) and [Create Aeronautics](https://modrinth.com/mod/create-aeronautics).
Design specs, tasks, and decisions live in the development console: open `index.html` in a browser.
Its data is stored per browser and per file location, so don't move `index.html`, and use its Export JSON button for backups.

## Requirements

- JDK 21 (Temurin recommended). Gradle picks it up from `JAVA_HOME` or provisions one through the toolchain resolver.
- Nothing else: the Gradle wrapper downloads Gradle, Minecraft, NeoForge, and all mod dependencies.

## Common tasks

| Command | What it does |
| --- | --- |
| `./gradlew build` | Compiles and produces `build/libs/criticalmass-<version>.jar` |
| `./gradlew runClient` | Dev client with Create, Sable, and Create Aeronautics loaded |
| `./gradlew runServer` | Dedicated dev server (`run/` folder; accept the EULA there on first run) |
| `./gradlew runGameTestServer` | Runs registered game tests headless and exits |
| `./gradlew runData` | Data generation into `src/generated/resources` |
| `./gradlew runInspect -PinspectScript=<scene.json>` | Scripted in-game screenshots (see below) |

IntelliJ IDEA: open the folder as a Gradle project. Run configurations are generated automatically on sync.

## Visual assets

- Blocks, items, fluids and the protective outfit are registered and textured. Status per asset: `docs/art/ASSET_MANIFEST.md` (and `.json`).
- Art direction and the texture pipeline: `docs/art/STYLE.md`. Reference breakdown: `docs/art/REFERENCES.md`.
- QA contact sheets (iso views, functional faces, item sizes, outfit poses, kinetics, Aeronautics body): `docs/art/qa/`.
- Generated art and its provenance (prompts, raw outputs, processing): `art/SOURCES.md`, `art/generated/`. The scripts that drew blockouts and finalized textures: `art/tools/`.
- Fluid textures live in `textures/fluid/` and are added to the block atlas by `assets/minecraft/atlases/blocks.json`.

## Reactor and coolant loop

Values live in `content/thermal/CMThermal.java`.

**Vessel.** A vessel is a box of Reactor Vessel Casing, odd on every side, from 5x5x5 up to 11x11x13.
- Exactly one Reactor Controller sits on a side face and faces outward.
- A Primary Coolant Port set to inlet and one set to outlet go on faces, facing outward (toggle with a wrench).
- A Reactor Service Port goes on the top face, facing up.
- Only casing may sit on edges and corners.
- The inside holds vertical columns of fuel, control or coolant channels, one type per column; empty columns are allowed.

With goggles on, invalid blocks are outlined in red, and the controller lists problems and missing parts.

**Core.**
- Each fuel column makes 50 HU/t per block, plus 25% for each neighbouring fuel column.
- Each neighbouring control column scales that by (1 - rod insertion).
- Rods withdraw with the controller's redstone signal (15 = fully withdrawn) until control rod drives move them.
- Coolant columns pass 5% per tick of a neighbouring fuel column's heat into the vessel coolant, but only while the pump moves coolant through the vessel.
- A fuel column without enough cooling heats up and is flagged as a hot spot at 800 °C.
- If the vessel breaks while running, the reactor trips. It stays tripped until the redstone signal is removed.

**Casing integrity.**
- Casing next to a fuel column hotter than 800 °C loses integrity and shows cracks: stage 1 below 75%, stage 2 below 50%, stage 3 below 25%.
- Below 25% the reactor counts as damaged and cannot restart until it is repaired.
- Right-clicking cracked casing with a Repair Kit restores 50% and uses up the kit.
- Integrity is saved with the controller and also shows on the blocks themselves.

**Loops.**
- **Primary loop:** run Reinforced Pipe from the outlet port, through a Circulation Pump (back to front) and any machines on the way, to the inlet port. The controller holds the vessel coolant (8,000 mB).
- **Closed loops:** a pump can also drive a loop that comes back to its own back face without touching a vessel. Use this for the secondary circuit.
- **Paths:** every loop must be a single path without branches.
- **Pump:** moves 2 mB/t per RPM, up to 256 mB/t, and stops when its shaft stops. Stress is 4 SU per RPM while circulating, a quarter of that while idle, and stops rising past 128 RPM.
- **Heat Exchanger:** the primary circuit enters and leaves through its front and back, the secondary through its top and bottom. The two never mix. Heat crosses from the hotter side only while both sides are flowing.
- **Steam Turbine:** takes coolant through its bottom and top. It draws heat from that coolant and supplies 4 SU per HU/t at a fixed 16 RPM. (It runs on hot water or coolant until steam exists.)

**Operation and safety.**
- **States:** the controller shows Cold, Starting, Running, Tripped (with the cause), Cooling Down, Damaged or Breached. State and residual heat survive saving.
- **Rod drives:**
  - A Control Rod Drive on the top face above a control column moves that column's rods; its redstone signal withdraws them.
  - Rods move at a rate set by shaft speed, up to 0.02 per tick, and the drive uses stress only while moving.
  - Columns without a drive follow the controller's redstone signal.
- **Fail-safe:** losing shaft power, overstress or a trip releases the drive's spring latch and drives the rods fully in within 2 seconds, in any orientation.
- **Trips:** the reactor trips if the vessel turns invalid, primary flow stops for 2 seconds, the core passes 1,100 °C, or the casing is damaged or breached.
  - To reset, turn off the controller signal and set every drive to insert.
- **Residual heat:** fuel keeps producing decay heat after shutdown (up to 7% of its running output, halving every minute), so keep the pumps and cooling running after a trip.
- **Emergency Coolant Tank:**
  - Touching the vessel, it injects room-temperature coolant on a trip or loss of flow.
  - A redstone signal disarms it, and a comparator reads its fill level.
  - It won't inject when tilted past 30° on a moving body.
- **Breach:** a casing block at zero integrity breaches the vessel, which then leaks coolant.
  - Repair with Repair Kits: hold use for 2 seconds; each kit restores 50%.
  - Kits are refused on a breached reactor until residual heat falls below 20 HU/t.

**Steam and cooling.**
- **Steam:** water and steam are one medium. Water boils at 100 °C once it holds 80 HU per mB, and fully boils after another 540 HU per mB. A buffer shows as Steam once any of it has boiled. Pressure runs from 1 bar at saturation to 10 bar fully boiled.
- **Steam loop:** heat exchanger secondary, then turbine, then condenser, with a pump driving the closed loop.
- **Turbine:** draws only on steam energy, 2% per tick, giving 4 SU per HU/t at 16 RPM. A redstone signal closes it as a governor. It trips if nothing is attached to its shaft.
- **Steam Bypass Valve:** its redstone signal sends that fraction of steam past any turbine downstream in the same loop.
- **Relief Valve:** fits inline. It senses the loop's peak pressure, vents through its front from 8 bar down to 6 bar, and gives a redstone signal while venting. Vented water and heat are logged.
- **Rupture:** any steam buffer above 12 bar ruptures and loses its contents (logged).
- **Condenser:** steam side front/back, cooling water top/bottom. It condenses only while cooling water flows.
- **Cooling loop:** condenser cooling side, then Cooling Tower Packing and/or Radiator Panels, then back.
  - Tower packing stacks up to 12 high. Water enters the top block from the side and leaves the bottom block from the side. It needs open sky, evaporates a little water, and stops past 15° of tilt on a moving body.
  - Radiators reject per open face, with an airflow bonus from the body's speed capped at 2×.
- **Boiler Heating Plate:** sits in the primary loop (front/back) under a Create boiler tank. It heats the boiler (level 1 from 120 °C, level 2 from 200 °C) and draws heat only while that boiler runs with water. A redstone signal turns it off.
- **Heat accounting:** every unit of heat is stored, turned into stress by a turbine, sent to a Create boiler, rejected to the air, or vented. `/criticalmass ledger` shows each.

**Chunk unloading.** An unloaded reactor freezes. It keeps its exact state and does nothing until it reloads, and a vessel that is only partly loaded waits instead of tripping.

Game tests in `com.criticalmass.gametest` cover vessel validation, core heat, tripping, port rules, pump stress, the exchanger, turbine capacity, a 10,000-tick conservation run of the full two-loop plant, steam phase and pressure, turbine admission, bypass, condenser, cooling tower, radiator, boiler plate, relief and rupture, and turbine-trip handling (`./gradlew runGameTestServer`).
`src/inspect/scenes/make_reactor.py` builds a vessel with mistakes, repairs it, runs it until a hot spot forms, then breaks it, with goggle screenshots at each stage. `make_plant.py` builds the two-loop plant with a heat exchanger, then leaves and returns to check the unload freeze. `make_cracks.py` overheats a fuel column and records the casing cracking. `make_steam_plant.py` builds all three circuits (primary, steam, cooling) and trips the turbine. `make_operation.py` walks a reactor with rod drives through start-up, a lost-flow trip with emergency injection, and a breach; `make_ops_body.py` scrams a reactor tipped over on a Sable body.

`/criticalmass ledger` (operators) lists every loaded controller, pump and turbine and prints the totals for coolant, stored heat, heat added and heat extracted. `balance` should always be 0.

### Moving bodies

`src/inspect/scenes/make_body_tests.py` checks the loop on a Sable sub-level:
- 10 assemble/disassemble cycles, with no duplicated blocks, coolant or heat
- running while the body moves and rotates
- teleporting across chunk borders
- a save/quit/reload while assembled and after disassembly

Results are in the console's Create Aeronautics compatibility entry.

## In-game inspection harness

`src/inspect/` is a client-only helper mod (`criticalmass_inspect`) that is loaded only by the `runInspect` task.
It creates a flat creative world, runs a JSON scene of commands, waits, camera moves and screenshots, writes command feedback to
`run-inspect/screenshots/<scene>_log.txt`, then quits.

```
./gradlew runInspect -PinspectScript=src/inspect/scenes/showroom.json
```

`src/inspect/scenes/make_showroom.py` generates `showroom.json`: every block state, kinetic blocks on creative motors, an overstress test,
fluids in Create tanks, items, the worn outfit in several poses, a small reactor installation, and a Sable sub-level that falls and comes to rest tilted.

Besides commands, waits and screenshots, scenes can use `"rejoin": true` (save, quit and reopen the same world) and
`"invoke": {"offset": [x, y, z], "method": "name", "far": true}` (call a public no-argument method on the block entity at that offset from the loaded Reactor Controller; `far` picks the controller on a sub-level).

## Target and dependency versions

All versions live in `gradle.properties`.

| Dependency | Version | Why this version |
| --- | --- | --- |
| Minecraft / NeoForge | 1.21.1 / 21.1.262 | NeoForge 1.21.1 MDK default |
| Create | 6.0.10-280 | Latest Create release; matches Create's official addon setup page |
| Ponder / Flywheel / Registrate | 1.0.82 / 1.0.6 / MC1.21-1.3.0+67 | Versions paired with Create 6.0.10 on the Create wiki |
| Sable | 2.0.6 | Physics engine for Aeronautics bodies; Aeronautics 1.3.2 requires `[2.0.0,3.0.0)` |
| Create Aeronautics | 1.3.2+mc1.21.1 | Dev runtime only, pulled from Modrinth's Maven to build and fly test vehicles |

Create is a hard dependency. Sable is declared optional in `neoforge.mods.toml` until the mobile-integration
design settles whether stationary-only play should work without it.

Maven sources: `maven.createmod.net` (Create, Ponder, Flywheel), `mvn.devos.one/snapshots` (Registrate),
`maven.ryanhcode.dev/releases` (Sable), `maven.blamejared.com` (Veil, required by Sable), `api.modrinth.com/maven` (Aeronautics).

## License

All Rights Reserved until a license is chosen (`mod_license` in `gradle.properties`).
Create Aeronautics code is MIT, its assets are All Rights Reserved; this project compiles against it and does not redistribute it.
