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

**Loop.**
1. Fill the controller with coolant (it holds 8,000 mB).
2. Run Reinforced Pipe from the outlet port to the back of a Circulation Pump, from its front up through the bottom of a Steam Turbine, and from the turbine's top back to the inlet port. The loop must be one path with no branches.
3. The pump moves 2 mB/t per RPM, up to 256 mB/t, and stops when its shaft stops.
4. The turbine draws heat from the coolant and supplies 4 SU per HU/t at a fixed 16 RPM.

Game tests in `com.criticalmass.gametest` cover vessel validation, core heat, conservation, tripping and turbine capacity (`./gradlew runGameTestServer`).
`src/inspect/scenes/make_reactor.py` builds a vessel with mistakes, repairs it, runs it until a hot spot forms, then breaks it, with goggle screenshots at each stage.

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
