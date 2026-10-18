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

## Prototype coolant loop (milestone 1)

The first playable loop proves the heat path: vessel, pump, turbine. Values live in `content/thermal/CMThermal.java`.

1. Build a vessel: a Reactor Controller touching Reactor Vessel Casing and two Primary Coolant Ports. Set one port to outlet and one to inlet with Create's wrench.
2. Fill the controller with water or primary coolant (right-click with a bucket, or pipe it in with Create). It holds 8,000 mB.
3. Run Reinforced Pipe from the outlet port to the back of a Circulation Pump. Run it from the pump's front up into the bottom of a Steam Turbine, then from the turbine's top back to the inlet port. The loop must be a single path with no branches.
4. Turn the pump's shaft. It moves 2 mB/t per RPM, up to 256 mB/t, and stops when the shaft stops or the network is overstressed.
5. Give the controller a redstone signal to run the test heater (+2,000 HU/t while it holds coolant).
6. The turbine extracts heat from the coolant passing through it and supplies 4 SU of stress capacity per HU/t, at a fixed 16 RPM.

Wear Create's goggles to read coolant, temperature, stored heat, flow, loop status and stress capacity on each block.
Game tests in `com.criticalmass.gametest` check save/load, the heater rate, volume and heat conservation, stopping without rotation, and turbine capacity (`./gradlew runGameTestServer`).
`src/inspect/scenes/make_loop.py` builds the same loop in game for screenshots.

## In-game inspection harness

`src/inspect/` is a client-only helper mod (`criticalmass_inspect`) that is loaded only by the `runInspect` task.
It creates a flat creative world, runs a JSON scene of commands, waits, camera moves and screenshots, writes command feedback to
`run-inspect/screenshots/<scene>_log.txt`, then quits.

```
./gradlew runInspect -PinspectScript=src/inspect/scenes/showroom.json
```

`src/inspect/scenes/make_showroom.py` generates `showroom.json`: every block state, kinetic blocks on creative motors, an overstress test,
fluids in Create tanks, items, the worn outfit in several poses, a small reactor installation, and a Sable sub-level that falls and comes to rest tilted.

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
