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
- Operating manual (circuits, a minimal plant, control room wiring, start-up, shutdown, faults, expansion): `docs/OPERATING.md`. Overhaul plan and status: `docs/OVERHAUL.md`.
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

**Core.** The core is a neutron chain reaction, not a heater with a dial. What it does depends on its reactivity, measured in pcm (1 pcm is a change of 0.001%).
- **Reactivity balance:** fresh fuel is worth +8,500 pcm, falling to 65% of that as it burns out. Against it stand the control rods, boron in the coolant (7 pcm per ppm), hot fuel (2 pcm per °C), hot coolant (10 pcm per °C) and xenon. A vessel under half full loses its moderator and cannot be critical.
- **Subcritical, critical, supercritical:** below zero the core sits at a tiny source level. At zero it holds whatever power it has. Above zero power climbs, and the reactor period says how fast: the time for power to grow by a factor of 2.7. A period under 3 seconds trips the reactor, and so does neutron power above 118%.
- **Delayed neutrons:** 650 pcm of the reaction waits on delayed neutrons, which is what makes it controllable. Add more than that at once and the core is prompt critical: power multiplies several times a second and the fuel melts before the rods arrive.
- **Rods:** a rod fully in takes 10,000 pcm off each fuel column it touches, averaged over the whole core, so a rod only controls the fuel beside it. Worth follows an S-curve: the middle of the stroke does most of the work, the ends very little. A rod also pushes power away from the fuel beside it.
- **Rod motion:** a Control Rod Drive moves its rod at up to 4% a second, and a rod bank button moves the bank by 0.5%. Without drives the controller's redstone signal moves the rods at 2% a second. On a trip every rod drops in 2 seconds.
- **Power:** each fuel column makes 50 HU/t per block at 100% neutron power, plus 25% for each neighbouring fuel column.
- **Temperature feedback:** fuel runs hotter than the coolant in proportion to its power, and both push reactivity down as they heat. Raise the rods a little and power rises until the extra heat cancels what you added. Cool the coolant and power rises by itself, so drawing more steam raises reactor power.
- **Fuel cooling:** each coolant column beside a fuel column carries 0.25 HU/t per block for every degree the fuel is above the coolant, at 64 mB/t of flow or more, and less in proportion below that. Nothing moves without flow. A fuel column with no coolant column beside it is not cooled at all. A hot spot is flagged at 800 °C and the fuel melts at 1,800 °C.
- **Xenon:** fission makes iodine, which decays into xenon, which absorbs neutrons. At steady full power xenon costs 2,500 pcm. After a shutdown the iodine keeps decaying, so xenon climbs to about one and a half times that around 8 minutes later, then fades over the next hour. A core without enough spare reactivity cannot restart until it has.
- **Decay heat:** fission products give 7% of the power the fuel was making, halving every minute after shutdown.
- **Readings:** goggles on the controller show neutron power, reactivity, period, xenon and boron. The same readings are Display Link sources, with a neutronics summary for a Display Board.
- **Boron:** the reactivity term is modelled, but nothing in the world adds or removes boron yet. Operators with permission can set it with `/criticalmass boron <controller> <ppm>`.
- **Jump to power:** `/criticalmass steady <controller> <fraction> [coolant °C]` puts a reactor at that power with matching fuel temperatures and xenon, optionally sets the coolant temperature, and sets the rods to the position that holds it.
- If the vessel breaks while running, the reactor trips. It stays tripped until it is reset with the rods commanded in.

**Casing integrity.**
- Casing next to a fuel column hotter than 800 °C loses integrity and shows cracks: stage 1 below 75%, stage 2 below 50%, stage 3 below 25%.
- Below 25% the reactor counts as damaged and cannot restart until it is repaired.
- Holding use on cracked casing with a Welding Torch for 2 seconds welds it: each weld restores 50% and uses a Steel Plate from your inventory and a point of the torch's durability (128 welds; repair it with steel plates on an anvil).
- Integrity is saved with the controller and also shows on the blocks themselves.

**Loops.**
- **Primary loop:** run pipe from the outlet port, through a Circulation Pump (back to front) and any machines on the way, to the inlet port. The controller holds the vessel coolant (8,000 mB). A pump placed beside a pipe, tank or machine turns to point into it, like Create's Mechanical Pump; sneak to keep the facing you chose.
- **Closed loops:** a pump can also drive a loop that comes back to its own back face without touching a vessel. Use this for the secondary circuit.
- **Paths:** every loop must be a single path without branches.
- **Pipes:** loops are plumbed with Create's fluid pipes, copper, glass or encased. Plain pipes carry anything, but they are not sealed or shielded: they leak under pressure and give off radiation while they hold primary coolant. Reinforced Pipe does neither. Every pipe block in a loop holds 100 mB of coolant, so a new loop takes extra coolant to fill, and the coolant travels through it.
- **Reinforcing:** use a Reactor Alloy Ingot on a placed Fluid Pipe or Fluid Valve and it becomes a Reinforced Pipe or Reinforced Valve where it stands, keeping its connections, the way Create's casings are made. A Deployer holding the ingot does the same to pipes and valves on a belt or depot. Reinforce before filling: a pipe that already holds coolant loses what was in that block.
- **Leaks:** a plain pipe or Fluid Valve loses 0.004 mB a tick for each bar above 1, up to 1 mB a tick. For steam that is the steam pressure in the pipe. For primary coolant it is the pressure the Pressurizer holds, so a loop at 100 bar loses about 0.4 mB a tick from every plain block, and what leaks contaminates the spot. A loop with no pressure on it does not leak.
- **Radiation:** every plain block holding primary coolant is a small radiation source, five times stronger if the coolant is contaminated.
- **Bursting:** any pipe, reinforced or not, bursts when the steam inside it passes 12 bar (no drop, scalds anyone close), so put a Relief Valve in every steam loop.
- **Create pumps:** coolant ports and the coolant machines accept Create pipes on their pipe faces, so a mechanical pump can fill or drain them. Coolant keeps its heat when a Create pump moves it between two of these machines.
- **Pump:** moves 2 mB/t per RPM, up to 256 mB/t, and stops when its shaft stops. Stress is 4 SU per RPM while circulating, a quarter of that while idle, and stops rising past 128 RPM.
- **Heat Exchanger:** the primary circuit enters and leaves through its front and back, the secondary through its top and bottom. The two never mix. Heat crosses from the hotter side only while both sides are flowing.
- **Steam Turbine:** fills a three-block cube and is placed like Create's Large Water Wheel, with the big end facing you. Steam goes in through the back, the exhaust leaves through the bottom or the top, whichever has a pipe, and the shaft comes out of the front. It draws heat from that coolant and supplies 4 SU per HU/t at a fixed 16 RPM. (It runs on hot water or coolant until steam exists.)

**Operation and safety.**
- **States:** the controller shows Cold, Starting, Running, Tripped (with the cause), Cooling Down, Damaged or Breached. State and residual heat survive saving.
- **Rod drives:**
  - A Control Rod Drive on the top face above a control column moves that column's rods; its redstone signal withdraws them.
  - Rods move at a rate set by shaft speed, up to 0.02 per tick, and the drive uses stress only while moving.
  - Columns without a drive follow the controller's redstone signal.
  - **Visible travel:** the rod stands up out of the drive as it withdraws, one block of rod per block of core height, and drops back in as it inserts. Inside the vessel each control channel shows how far its rod reaches.
  - **Banks:** every drive belongs to a bank, A to D. Use an empty hand on the drive to change it (sneak to go back). A bank target set on the reactor moves every drive in that bank and overrides their redstone signals.
- **Reactor Viewport:** a window that takes the place of a vessel casing block (craft casing with Shielded Glass). It cracks, welds and shields like casing. Through it, fuel channels are dark when idle, glow dim blue at low power or on decay heat, bright blue at power and orange-red when overheating, and they light their surroundings.
- **Fail-safe:** losing shaft power, overstress or a trip releases the drive's spring latch and drives the rods fully in within 2 seconds, in any orientation.
- **Start-up check:** rods will not withdraw, and the reactor will not start, until coolant is flowing through the vessel and the vessel is at least half full. The controller says which is missing. Pumps turn on shaft power, so a cold plant needs rotation from outside before it can make its own.
- **Power response:** fission power ramps towards what the rods ask for, taking 3 seconds from zero to full, so heat follows a rod move with a delay. A trip or shutdown cuts fission at once and leaves decay heat.
- **Temperature feedback:** hot fuel and hot coolant both hold power down. Fuel above 800 °C loses up to half its output by 1,800 °C, and vessel coolant above 250 °C loses up to half by 550 °C. A plant running hot settles at a lower power, and drawing more steam cools the coolant and brings power back up.
- **Trips:** the reactor trips if the vessel turns invalid, primary flow stops for 2 seconds, the core passes 1,100 °C, or the casing is damaged or breached.
  - To reset, turn off the controller signal and set every drive to insert.
- **Residual heat:** fuel keeps producing decay heat after shutdown (up to 7% of its running output, halving every minute), so keep the pumps and cooling running after a trip.
- **Emergency cooling:** build it from Create parts. Give the vessel a spare inlet port, set a Mechanical Pump against it and a Fluid Tank of primary coolant behind the pump, and hold the pump's shaft off with a Clutch until it is needed. Work the Clutch with a lever, or from the Alarm Panel's signal through a redstone torch.
- **Readings on displays:** the plant has no gauges of its own. Put a Create Display Link on a machine and point it at a wall of Plant Screens, a Display Board, Nixie Tubes, a sign or a lectern.
  - The Reactor Controller offers a status summary (status, hottest fuel, coolant temperature, pressure, flow and heat output, one per line), a list of active alarms, and each reading on its own, including rod bank A to D insertion.
  - A Reactor Signal Interface in the vessel wall offers the same reactor readings, so one reactor can feed several Display Links.
  - Pumps offer flow and speed. The turbine offers speed, power, shaft load, steam admission, temperature, pressure and level. The Pressurizer offers pressure. Rod drives offer their insertion. Tanks, the tower parts, the condenser, heat exchanger, radiator and relief valve offer temperature, pressure and level.
  - Readings refresh once a second. A Display Board only flips while it turns at 30 RPM or more, which is Create's own rule.
  - A comparator on the Reactor Controller reads the hottest fuel column against the 1,100 °C trip temperature (0 to 15).
- **Plant Linker:** the tool that links a Plant Screen wall to its machines. Use it on a machine to remember it, then on a screen of the wall. Sneak-use in the air to clear it.
- **Plant Screen:** a screen with green text, built like Create's Display Board in a steel frame. A wall of them shows the reactor display, or Display Link text like a Display Board.
  - **Reactor display:** use the Plant Linker on a Reactor Controller and then on any screen of a wall at least 9 wide and 5 high. The wall then shows the whole reactor by itself.
    - Four trend graphs cover the last two minutes: neutron power, pressure, coolant temperature and xenon. Each scales itself to what it has seen.
    - The centre panel shows the status, with trips in red, and every reading: neutron power, heat output, period, reactivity, fuel and coolant temperature, pressure, flow, rod position, xenon and boron.
    - Below that is a list of the latest events with their age: status changes, trips, and alarms raised and cleared.
    - Link a Steam Turbine the same way to add its speed and shaft load.
    - A linked wall that is too small says so on its face and in goggles. An empty linker unlinks the wall, and it goes back to Display Link text.
  - Screens placed beside and above each other join into one wall. Each block gives three lines and about five characters.
  - It runs on rotation, like Create's Display Board. Each screen holds a cogwheel that meshes with its neighbours, so one turning cogwheel beside any screen drives the whole wall. Each screen costs 0.25 SU per RPM. A wall stays lit for up to 20 seconds after it stops turning, and a dark wall keeps its text for when power returns.
- **Control desk:** the Reactor Alloy Dashboard and its parts. The desk and part models and the part textures come from Create: Loconautics, used with its authors' permission; the desk has its own reactor alloy colours. Loconautics does not need to be installed.
  - **Reactor Alloy Dashboard:** a sloped desk. Desks placed side by side join into one run of up to 5, and each holds two parts, one in each half. Craft it from a Display Board, two Reactor Alloy Ingots and a Block of Industrial Iron.
  - **Parts:** Dashboard Button, Switch, Lever, Gauge and Alarm. Sneak-use a part on its target to bind it, then use it on the half of a desk you want it in. A part named in an anvil keeps that name as its label. The Plant Linker re-binds a part already on a desk.
  - **Button:** on a Reactor Controller it is SCRAM, reset, rods in or rods out; on a Steam Turbine it is trip or reset; on a receiving Redstone Link it sends a full signal for one second. A redstone pulse into the desk presses its buttons.
  - **Switch and Lever:** bind to a receiving Redstone Link. The switch holds it on or off. The lever sets any strength from 0 to 15 by where you click along it. Two controls on one link do not fight: the strongest wins.
  - **Gauge:** binds to any plant machine and shows one of its readings on a needle; using it shows the number.
  - **Alarm:** a caged lamp. It binds to a Reactor Controller and watches one alarm or all of them. A new alarm flashes the lamp, sounds a klaxon and makes the desk give a redstone signal until you press the lamp; it then stays lit until the fault clears. A comparator counts the active alarms.
  - **Settings:** use a wrench on a part to open its settings. Pick the body (steel, red, dark, brass or reactor alloy), the cap, knob, lamp or needle colour, the style where there is one (large button, ball lever), the dial face of a gauge, the backplate, an inverted signal, a label, and what the part does: a button's action, a gauge's reading, the alarm it watches.
  - **Goggles:** look at a part to see its label, what it does, the block it is connected to and where, and its state. A missing target shows in red.
  - **Taking parts off:** sneak-use a wrench on a part, or break the desk while aiming at a part to take only that part.
  - **With Create: Loconautics installed:** its Gauge Button, Switch and Lever are hidden and their recipes switched off, because ours do the same job. Its dashboards and its Locomotive Gauge stay. Using one of our parts on an empty Loconautics dashboard turns that desk into ours in the same colour, and dye recolours our desks to any Loconautics colour. A desk recoloured this way drops the matching Loconautics dashboard. Without Loconautics those desks show in reactor alloy.
  - **Reactor Signal Interface:** for redstone instead of direct binding. Set it in the vessel wall and use a wrench on it to pick its job.
    - SCRAM, reset, rods in and rods out each act once on a redstone pulse. A rod pulse moves every bank 0.5%.
    - Alarm gives a redstone signal while any alarm is active: send it through a Redstone Link to a Steam Whistle on plant steam, a bell or note blocks.
    - Readings only is the setting it starts on, for Display Links.
    - Use it with an empty hand to see what it is set to.
  - **Rods by signal strength:** a signal at a Control Rod Drive or the Reactor Controller sets the rods coarsely, 0 for fully in and 15 for fully out.
  - **Turbine by signal:** a full signal at the Steam Turbine shuts its steam off and resets an overspeed trip. Weaker signals limit the governor.
- **Pumps and valves** are switched with Create's own parts: a clutch or gearshift and a lever on the pump shaft, a Valve Handle or powered shaft on a Reinforced Valve.
- **Primary pressure:** vessel coolant boils at 350 °C unless it is pressurized. Coolant hotter than its boiling point flashes to steam and is lost, a quarter of the excess each tick, and the boiling alarm is raised.
- **Pressurizer:** touching the vessel, it raises the primary pressure with heaters, and every bar adds 2 °C to the boiling point (100 bar gives 548 °C, the 150 bar maximum 648 °C).
  - Set the pressure on its value box. The heaters need a lit Blaze Burner directly under the Pressurizer: a kindled burner raises 0.25 bar per tick and a seething one twice that. Pressure leaks 0.02 bar per tick, so holding it means keeping the burner fed.
  - With the burner out the pressure falls back and hot coolant starts to boil, so a plant running above 350 °C depends on that burner.
  - A redstone signal switches the heaters off. A comparator reads the pressure.
- **Breach:** a casing block at zero integrity breaches the vessel, which then leaks coolant.
- **Meltdown:** an accident builds in stages, and each stage gives time to act.
  1. **Overheating:** above 800 °C fuel wears fast, the casing beside it cracks, and the overheating alarm is raised. The reactor trips at 1,100 °C, but decay heat keeps heating fuel that has no cooling.
  2. **Melting:** at 1,800 °C a fuel column melts. Its assembly is gone, its channel blocks become Ruined Fuel Channels, the coolant is contaminated, and the molten fuel pools on the vessel floor as corium that keeps its decay heat.
  3. **Quench or burn:** coolant in the vessel takes 1% of the corium's heat each tick, so keeping the vessel flooded (an emergency injection line helps) cools it before it gets through. Above 1,000 °C the corium eats the floor casing under it, about 0.1 integrity per second.
  4. **Breach:** when that floor block fails, the corium leaves the vessel as a hot Corium block and contaminates the ground around it.
  5. **Outside:** hot Corium glows, burns whoever stands on it, is a strong radiation source and melts down through up to 3 solid blocks (configurable). Shielding Concrete and obsidian stop it. Water beside it cools it six times faster.
- **Afterwards:** the vessel is invalid until every Ruined Fuel Channel and the holed casing are replaced. Corium cools to dark slag that can be mined, but the slag and ruined channels stay radioactive as blocks and as items, so they need shielded storage. Breaking the controller of a melted core drops its corium.
- **Config:** `accidents.meltdown` turns melting off, `accidents.coriumDepth` limits burn-through, and `accidents.coriumContamination` scales the release.
  - Repair with the Welding Torch: hold use for 2 seconds; each weld restores 50%.
  - Welding is refused on a breached reactor until residual heat falls below 20 HU/t.

**Steam and cooling.**
- **Steam:** water and steam are one medium. Water boils at 100 °C once it holds 80 HU per mB, and fully boils after another 540 HU per mB. A buffer shows as Steam once any of it has boiled. Pressure runs from 1 bar at saturation to 10 bar fully boiled.
- **Steam loop:** heat exchanger secondary, then turbine, then condenser, with a pump driving the closed loop.
- **Turbine:** can draw up to 2% of its steam energy per tick, giving 4 SU per HU/t. That ceiling is its stress capacity. Its rated speed (1 to 256 RPM, default 16) is set on the value box on its side.
  - **Governor:** a governor valve follows the shaft load, so the turbine only uses the steam its load needs plus its own losses. The valve moves 5% per tick. Unused steam carries on to the condenser.
  - **Overspeed:** when load drops faster than the valve closes, the rotor speeds up. Past 110% of rated speed the turbine trips and shuts its valve. Losing the whole load at once trips it; losing half does not.
  - **Reset:** give the turbine a full redstone signal to close the governor, then release it. A redstone signal also limits how far the valve opens.
  - It also trips if nothing is attached to its shaft.
- **Power out:** the turbine's shaft is the plant's output. Put Create machinery straight on it and the governor follows whatever load is there. A Clutch on the shaft drops the load, and dropping all of it at once trips the turbine. The mod makes no electricity of its own; an addon such as Create Crafts & Additions can turn the shaft into Forge Energy.
- **Valves:** a Reinforced Valve or Create's Fluid Valve fits inline in any loop. The plain valve leaks under pressure like a plain pipe. Turning the shaft forwards opens it and backwards closes it; a Valve Handle on the shaft is the usual way to work it. On Create's valve a handle left at its default 45 degrees needs two turns, so set it to 90 degrees or more for one. While it is closed nothing circulates in that loop.
- **Relief Valve:** fits inline, and grows a flanged stub on each side a pipe joins. It senses the loop's peak pressure, vents through its front from 8 bar down to 6 bar, and gives a redstone signal while venting. Vented water and heat are logged.
- **Rupture:** any steam buffer above 12 bar ruptures and loses its contents (logged).
- **Condenser:** steam side front/back, cooling water top/bottom. It condenses only while cooling water flows.
- **Cooling loop:** condenser cooling side, then Cooling Tower Packing and/or Radiators, then back.
- **Radiator:** a 14×14×14 block that takes its pipes on top and bottom. It rejects heat through each of its four sides that is open to air, and up to twice as much on a body moving through the air.
- **Cooling tower:** build it bottom to top: a pipe to carry the cooled water away, up to 12 Cooling Tower Packing blocks as fill standing on that pipe, a pipe bringing the hot water into the side of the top fill block, and optionally one of Create's Encased Fans on top.
  - Stacked fill joins into one glazed column, and the water inside it shows how full the whole stack is.
  - A stack reads as one tank, like Create's Fluid Tanks. Goggles, Display Links, dashboard gauges and tank readers show the stack's total water, its mixed temperature and the heat it is rejecting, the same on every block. Buckets fill the stack as a whole, and water piped into a block that is full goes on into the rest of the stack. A pipe still only draws from the block it touches.
  - Every fill block carries a vertical shaft. A fan facing up on top is driven from below, through the fill; then the cooled water leaves by a pipe on the bottom block's side instead. A fan facing down is driven from above.
  - Hot water falls through the fill and leaves by the pipe under the bottom block. With no pipe underneath, it leaves by a pipe on the bottom block's side.
  - **Airflow** sets how hard each fill block works. A tower open to the sky gets 25% plus 5% per extra fill block, up to 75%. An Encased Fan on top adds 100% at 64 RPM and a little more above that, and needs no open sky. It has to blow the air up, out of the tower; one blowing down into the fill adds nothing, so reverse its rotation. The hot pipe may also come down onto the top block, with the sky or the fan above that pipe.
  - Rejected heat evaporates water (1 mB per 2,000 HU), so the loop slowly loses water. Pipe make-up water into the side of the bottom fill block with a Mechanical Pump. The stack only takes water while it has room, so the pump can be left running. The same line fills a new loop, or use buckets on the fill.
  - If the cooled water leaves by a side pipe instead of from underneath, put the make-up pipe on a different fill block, or the loop may follow it.
  - The top fill block shows the tower's airflow and total heat rejection in goggles. A tower without a fan shows a vapour plume while it works.
  - The air leaving the top is as hot as the water. Above 50 °C it scalds anything standing in the fan's air stream, or just above a tower with no fan, and it hurts more the hotter the water is.
- **Fluid Tanks in a loop:** a Create Fluid Tank of any size can sit inline in a loop as a surge tank, for example for feedwater, and the water in it keeps its temperature.
  - It needs the loop's two pipes on it. With more pipes on it, the loop leaves by the first one it finds, checking north, east, south, then west, so set make-up pumps directly against the tank.
  - Water that a Create pump adds arrives cold, and water it takes out takes its share of the heat.
- **Enrichment:** a Centrifuge turns 2 Uranium Concentrate into 1 Enriched Uranium and 1 Depleted Uranium.
  - It takes its shaft from above or below, so centrifuges stack on one shaft. It costs 8 SU per RPM.
  - It only works at 96 RPM or more. A batch takes 10 seconds at 128 RPM and 5 seconds at 256 RPM.
  - Put concentrate in by hand, funnel or hopper. Take the products out with an empty hand, a funnel or a hopper.
- **Pellets:** compact 1 Enriched Uranium in a heated basin into a Ceramic Fuel Pellet.
- **Depleted Uranium:** mix 1 with 2 gravel and water into 4 Shielding Concrete.
- **Steel:** mix an iron ingot with coal or charcoal in a superheated basin into a Steel Ingot, and press ingots into Steel Plates. Steel goes into the vessel casing, the relief valve, turbine blades and the shielded blocks. Ingots also craft into nuggets and a Block of Steel.
- **Alloy:** mix 2 steel, 1 zinc and 1 nether quartz in a heated basin into 2 Reactor Alloy Ingots. Press ingots into sheets and sheets into 2 Fuel Cladding. Ingots also craft into a Block of Reactor Alloy.
- **Frame:** a mechanical crafter makes an Empty Fuel Assembly from 4 alloy sheets and a Sealing Gasket (gaskets are compacted from dried kelp and an iron nugget).
- **Assembly:** a sequenced assembly of 4 loops of deploy cladding, deploy pellet, deploy pellet, press.
  - It runs on any Create belt or depot, with Deployers for the cladding and pellets and a Mechanical Press for the last step of each loop.
  - The item's tooltip shows the next step. Progress is stored on the item, so an interrupted assembly carries on after a reload.
- **Damaged fuel:** once cooled (in the spent fuel rack), a damaged assembly becomes a Sealed Waste Canister that keeps its lifecycle record. It is a sequenced assembly: deploy an Open Waste Canister onto the assembly, deploy a Sealing Gasket, then press it shut. The deployer leaves hot assemblies alone, and the half-sealed canister is as radioactive as the bare assembly. Canisters go in a Shielded Cask.
- **Fire and despawn:** assemblies and canisters resist fire and never despawn.
- **Placing canisters:** a Sealed Waste Canister can be placed as a block. It stays a radiation source where it stands, and breaking it (with a pickaxe) drops the canister with its record intact.
- **Other recipes:** every block and item has a survival recipe. They are generated by `art/tools/data_recipes.py`, and JEI shows them under Create's categories.

**Radiation.** On by default with gentle values; everything below is in the server config (`radiation.*`).
- **Dose rate:** in mSv/min. A player receives, from every source within 48 blocks, its strength times the shielding transmission divided by (1 + distance²).
- **Sources:** running or cooling reactors (from fission and decay heat), spent and damaged assemblies, waste canisters and (weakly) fresh fuel, wherever they are: placed canisters, racks (flooded racks let 10% through), casks (1%), the service port, dropped items and player inventories. Positions are world-space, so sources on a moving Sable body are measured where the body is now.
- **Shielding:** attenuation per block is 3.0 for Shielding Concrete, 2.5 for a closed Shielded Hatch, 1.5 for Shielded Glass, 1.0 for vessel casing, 0.7 for water and 0.4 for other solid blocks. An open hatch is 0.05.
- **Dose and effects:** dose is kept on the player, never on an item.
  - Body dose recovers 30 mSv per minute.
  - Effects start at 250 mSv (hunger), 500 (weakness), 1,000 (nausea, weakness II) and 2,000 (wither). Death clears the body dose but not the lifetime total.
- **Survey Meter:** hold it to read the dose rate in the action bar. Use it to switch to contamination (around you and on you).
- **Dosimeter:** hold it for your dose. Use it for a summary, and sneak-use to reset its reading (the lifetime dose stays).
- **Contamination:**
  - A breach leaks coolant out of each broken casing block and leaves contamination there that never fades.
  - Standing in it gives dose and contaminates you.
  - Failed fuel turns the vessel coolant into Contaminated Coolant, which contaminates any clean coolant it mixes with.
- **Protective gear:** each piece blocks a quarter of 90% of contamination uptake and collects that contamination itself. A full set cuts direct dose by only 5%: it is not shielding.
- **Decontamination Compound:** use it on a contaminated area (3×3×3) or in the air (yourself and your gear).
  - Each use puts 250 mB of Contaminated Coolant runoff into a tank within 4 blocks. Without such a tank nothing is cleaned.
  - Solidify the runoff into Sealed Waste Canisters by sequenced assembly: spout 1,000 mB into an Open Waste Canister (3 Steel Plates in a bucket shape), deploy a Sealing Gasket, then press it shut.
- `/criticalmass radiation <x y z>` lists every source reaching that point and the total. `/criticalmass contaminate <pos> <amount>` adds contamination for testing.

**Chunk unloading.** An unloaded reactor freezes. It keeps its exact state and does nothing until it reloads, and a vessel that is only partly loaded waits instead of tripping.

Game tests in `com.criticalmass.gametest` cover vessel validation, viewports, core heat, rod banks and their in-core display, tripping, port rules, pump stress, the exchanger, turbine capacity, a 10,000-tick conservation run of the full two-loop plant, steam phase and pressure, turbine admission, an end-to-end plant scenario (blocked start, rod motion and delayed heating, the turbine driving a machine, SCRAM and cooled shutdown), the control desk (desks joining into runs of five, parts working the reactor and a Redstone Link, settings applied, clamped and saved, the linker re-binding a part, desks running a whole plant, Signal Interfaces working the reactor from redstone pulses, and with Create: Loconautics present its colours by dye and its dashboards taking our parts), the screen wall (power from a shaft shared across it, linked text, and the reactor display with its trends and events), meltdown (a flooded melt held in the vessel, a dry melt burning through, concrete stopping corium), start-up checks, the power ramp and temperature feedback, the built cooling tower (airflow by construction, fan, water and heat accounting through a Create Fluid Tank in the loop), a Create pump injecting coolant through a spare port, the pressurizer over a Blaze Burner and coolant flashing, the turbine governor and overspeed trip, the turbine carrying Create machines (governor following the load, half the load lost, a clutch dropping all of it), the reinforced valve and Create's Fluid Valve, pipes (coolant held in pipes, hot coolant in plain pipes, plain pipes leaking under pressure and radiating, any pipe bursting from overpressure, heat carried through Create pumps, a Reactor Alloy Ingot reinforcing a placed pipe and valve), condenser, cooling tower, radiator, boiler plate, relief and rupture, turbine-trip handling, and the fuel lifecycle (burnup, overheating damage, service port round trips and refusals, waste retention, rack cooling, cask pickup and reload), recipe loading, the centrifuge (minimum speed, batches, automation), the damaged-fuel sequence keeping its record, ore drops, assembling fuel holders onto a Sable body without duplication, and radiation (distance and shielding, casks and flooded racks, breach contamination, cleanup runoff, contaminated coolant, protective gear, dosimeter) (`./gradlew runGameTestServer`).
`src/inspect/scenes/make_reactor.py` builds a vessel with mistakes, repairs it, runs it until a hot spot forms, then breaks it, with goggle screenshots at each stage. `make_plant.py` builds the two-loop plant with a heat exchanger, then leaves and returns to check the unload freeze. `make_cracks.py` overheats a fuel column and records the casing cracking. `make_steam_plant.py` builds all three circuits (primary, steam, cooling) and trips the turbine. `make_operation.py` walks a reactor with rod drives through start-up, a lost-flow trip, and a breach; `make_ops_body.py` scrams a reactor tipped over on a Sable body. `make_fuel.py` burns a core out, unloads it through the service port by hand, cools the assemblies in a flooded and a dry rack across a rejoin, then casks them and picks the cask up and places it again.  `make_radiation.py` measures a running reactor at several distances and behind each shielding block, compares a dry rack, a flooded rack and a cask, breaches the vessel and cleans up the contamination, and follows a hot rack on a teleported Sable body.

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
`"slot": n` selects a hotbar slot, `"use": [x, y, z, "face"]` right-clicks that block face with the held item, `"useItem": true` uses the held item in the air, and `"inventory": true` logs every inventory item with its components. With JEI in the dev runtime, `"jei": "<item id>"` opens JEI's recipes for that item and `"closeScreen": true` closes it.

## Target and dependency versions

All versions live in `gradle.properties`.

| Dependency | Version | Why this version |
| --- | --- | --- |
| Minecraft / NeoForge | 1.21.1 / 21.1.262 | NeoForge 1.21.1 MDK default |
| Create | 6.0.10-280 | Latest Create release; matches Create's official addon setup page |
| Ponder / Flywheel / Registrate | 1.0.82 / 1.0.6 / MC1.21-1.3.0+67 | Versions paired with Create 6.0.10 on the Create wiki |
| Sable | 2.0.6 | Physics engine for Aeronautics bodies; Aeronautics 1.3.2 requires `[2.0.0,3.0.0)` |
| Create Aeronautics | 1.3.2+mc1.21.1 | Dev runtime only, pulled from Modrinth's Maven to build and fly test vehicles |
| Create: Loconautics | 0.1.2BETA | Optional. The dashboard and part models and textures were imported from it by `art/tools/loconautics_assets.py`, with permission. Its jar in `libs/` (the folder ignores jars) joins the dev runtime so the compatibility is exercised; run Gradle with `-Ploconautics=false` to test without it |
| JEI | 19.69.0.456 | Dev runtime only, to check recipe pages |

Create is the only hard dependency. Create: Loconautics is optional.
Sable is declared optional in `neoforge.mods.toml` until the mobile-integration
design settles whether stationary-only play should work without it.

Maven sources: `maven.createmod.net` (Create, Ponder, Flywheel), `mvn.devos.one/snapshots` (Registrate),
`maven.ryanhcode.dev/releases` (Sable), `maven.blamejared.com` (Veil, required by Sable, and JEI), `api.modrinth.com/maven` (Aeronautics).

## License

All Rights Reserved until a license is chosen (`mod_license` in `gradle.properties`).
Create Aeronautics code is MIT, its assets are All Rights Reserved; this project compiles against it and does not redistribute it.
