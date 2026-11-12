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
- **Primary loop:** run pipe from the outlet port, through a Circulation Pump (back to front) and any machines on the way, to the inlet port. The controller holds the vessel coolant (8,000 mB).
- **Closed loops:** a pump can also drive a loop that comes back to its own back face without touching a vessel. Use this for the secondary circuit.
- **Paths:** every loop must be a single path without branches.
- **Pipes:** Reinforced Pipe is a Create fluid pipe: it joins Create pipes, pumps and tanks and carries any fluid. Every pipe block in a loop holds 100 mB of coolant, so a new loop takes extra coolant to fill, and the coolant travels through it.
- **Bursting:** Create's copper, glass and encased pipes carry coolant below 100 °C, but steam or coolant at 100 °C or hotter bursts them (no drop, scalds anyone close). That goes for loops and for Create pumps pulling hot coolant out of a machine or port: the nearest ordinary pipe on that run bursts. Use Reinforced Pipe on hot legs and steam lines.
- **Create pumps:** coolant ports and the coolant machines accept Create pipes on their pipe faces, so a mechanical pump can fill or drain them. Coolant keeps its heat when a Create pump moves it between two of these machines.
- **Pump:** moves 2 mB/t per RPM, up to 256 mB/t, and stops when its shaft stops. Stress is 4 SU per RPM while circulating, a quarter of that while idle, and stops rising past 128 RPM.
- **Heat Exchanger:** the primary circuit enters and leaves through its front and back, the secondary through its top and bottom. The two never mix. Heat crosses from the hotter side only while both sides are flowing.
- **Steam Turbine:** takes coolant through its bottom and top. It draws heat from that coolant and supplies 4 SU per HU/t at a fixed 16 RPM. (It runs on hot water or coolant until steam exists.)

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
- **Emergency Coolant Tank:**
  - Touching the vessel, it injects room-temperature coolant on a trip or loss of flow.
  - A redstone signal disarms it, and a comparator reads its fill level.
  - It won't inject when tilted past 30° on a moving body.
- **Readings on displays:** the plant has no gauges of its own. Put a Create Display Link on a machine and point it at a wall of Plant Screens, a Display Board, Nixie Tubes, a sign or a lectern.
  - The Reactor Controller offers a status summary (status, hottest fuel, coolant temperature, pressure, flow and heat output, one per line), a list of active alarms, and each reading on its own, including rod bank A to D insertion.
  - A Reactor Signal Interface in the vessel wall offers the same reactor readings, so one reactor can feed several Display Links.
  - Pumps offer flow and speed. The turbine offers speed, power, shaft load, steam admission, temperature, pressure and level. The generator offers output, stored energy and speed. The Pressurizer offers pressure and charge. Rod drives offer their insertion. Tanks, the tower parts, the condenser, heat exchanger, radiator and relief valve offer temperature, pressure and level.
  - Readings refresh once a second. A Display Board only flips while it turns at 30 RPM or more, which is Create's own rule.
  - A comparator on the Reactor Controller reads the hottest fuel column against the 1,100 °C trip temperature (0 to 15).
- **Plant Linker:** the tool that wires panels to machines. Use it on a machine to remember it, then on a panel to bind that panel to it. Sneak-use in the air to clear it. Each panel shows what it is bound to in goggles, so several reactors side by side never share a panel by accident. A panel named in an anvil shows that name on its face.
- **Plant Screen:** a thin wall screen with green text. A wall of them shows the reactor display, or Display Link text like a Display Board.
  - **Reactor display:** use the Plant Linker on a Reactor Controller and then on any screen of a wall at least 9 wide and 5 high. The wall then shows the whole reactor by itself.
    - Four trend graphs cover the last two minutes: neutron power, pressure, coolant temperature and xenon. Each scales itself to what it has seen.
    - The centre panel shows the status, with trips in red, and every reading: neutron power, heat output, period, reactivity, fuel and coolant temperature, pressure, flow, rod position, xenon and boron.
    - Below that is a list of the latest events with their age: status changes, trips, and alarms raised and cleared.
    - Link a Steam Turbine and a Generator the same way to add turbine speed and output.
    - A linked wall that is too small says so on its face and in goggles. An empty linker unlinks the wall, and it goes back to Display Link text.
  - Screens placed beside and above each other join into one wall with a single frame. Each block gives three lines and about five characters.
  - It runs on Forge Energy, 1 FE/t per screen. Feed any one screen and it shares with the screens touching it, so one Generator beside the wall is enough. A dark wall keeps its text for when power returns.
- **Control panels:** thin wall panels, as deep as a Plant Screen, with their controls on the front. Mount them side by side or above each other to build your own control board. Each one faces you when placed, and a wrench turns it. Look at a control and use it. A panel named in an anvil shows that name on its face.
  - **SCRAM Panel:** links to a Reactor Controller. SCRAM trips the reactor and RESET clears the trip. A redstone pulse into the panel presses SCRAM, so a comparator on the controller wired to it is a protective trip you built yourself.
  - **Rod Control Panel:** links to a Reactor Controller. IN and OUT step every rod bank by 0.5%.
  - **Alarm Panel:** links to a Reactor Controller. Six lamps: trip, no flow through a hot core, fuel overheating, coolant below half, casing damage, and boiling. A new alarm flashes red and gives a redstone signal (for an Alarm Siren) until you press ACK. It then stays amber until the fault clears. A comparator counts the active alarms.
  - **Turbine Panel:** links to a Steam Turbine. TRIP and RESET, and a redstone pulse presses TRIP.
  - **Breaker Panel:** links to a Generator. Its switch closes and opens the breaker and follows the breaker's real state.
  - **Lever Panel:** links to nothing. Use it to switch a full redstone signal on and off.
  - **Dial Panel:** links to nothing. Use it to switch its signal on and off, and sneak-use it to raise the strength from 1 to 15. It suits anything that takes an analog signal, such as the controller's rod signal or a generator's load.
  - Panels ignore each other's signals, so a lever beside the SCRAM Panel does not trip the reactor.
- **Pumps and valves** are switched with Create's own parts: a clutch or gearshift and a lever on the pump shaft, a Valve Handle or powered shaft on the Reinforced Valve.
- **Primary pressure:** vessel coolant boils at 350 °C unless it is pressurized. Coolant hotter than its boiling point flashes to steam and is lost, a quarter of the excess each tick, and the Alarm Panel's boiling lamp lights.
- **Pressurizer:** touching the vessel, it raises the primary pressure with electric heaters, and every bar adds 2 °C to the boiling point (100 bar gives 548 °C, the 150 bar maximum 648 °C).
  - Set the pressure on its value box. Heating takes 64 FE/t and raises 0.25 bar per tick; pressure leaks 0.02 bar per tick, so holding needs a trickle of power. Feed it from a Generator or any Forge Energy source.
  - Without power the pressure falls back and hot coolant starts to boil, so a plant running above 350 °C depends on its own electricity or a backup supply.
  - A redstone signal switches the heaters off. A comparator reads the pressure.
- **Breach:** a casing block at zero integrity breaches the vessel, which then leaks coolant.
- **Meltdown:** an accident builds in stages, and each stage gives time to act.
  1. **Overheating:** above 800 °C fuel wears fast, the casing beside it cracks, and the Alarm Panel's hot lamp lights. The reactor trips at 1,100 °C, but decay heat keeps heating fuel that has no cooling.
  2. **Melting:** at 1,800 °C a fuel column melts. Its assembly is gone, its channel blocks become Ruined Fuel Channels, the coolant is contaminated, and the molten fuel pools on the vessel floor as corium that keeps its decay heat.
  3. **Quench or burn:** coolant in the vessel takes 1% of the corium's heat each tick, so keeping the vessel flooded (the Emergency Coolant Tank helps) cools it before it gets through. Above 1,000 °C the corium eats the floor casing under it, about 0.1 integrity per second.
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
- **Generator:** takes shaft power on its back and makes Forge Energy on every other face.
  - Set the load on its front value box (0 to 2,048 FE/t). 1 SU of shaft power gives 0.25 FE/t at 90% efficiency (both configurable).
  - It never takes more than the shaft has spare, eases off as its buffer fills, and pushes energy into neighbours.
  - A redstone signal lowers the load; a full signal opens the breaker.
  - Whatever shaft power the generator leaves is free for Create machinery on the same shaft.
- **Reinforced Valve:** fits inline in any loop, with a shaft through its side like Create's fluid valve. Turning the shaft forwards opens it and backwards closes it; a Valve Handle on the shaft is the usual way to work it. While it is closed nothing circulates in that loop.
- **Relief Valve:** fits inline. It senses the loop's peak pressure, vents through its front from 8 bar down to 6 bar, and gives a redstone signal while venting. Vented water and heat are logged.
- **Rupture:** any steam buffer above 12 bar ruptures and loses its contents (logged).
- **Condenser:** steam side front/back, cooling water top/bottom. It condenses only while cooling water flows.
- **Cooling loop:** condenser cooling side, then Cooling Tower Packing and/or Radiator Panels, then back.
- **Cooling tower:** build it bottom to top: a Cooling Tower Basin, up to 12 Cooling Tower Packing blocks as fill, a Cooling Tower Distributor, and optionally a Cooling Tower Fan on top. The fan is one block, but its blades sweep the eight blocks around it like a water wheel, so keep those clear.
  - Pipe hot water into a side of the distributor. It falls through the fill into the basin, and the basin's side outlet returns it to the loop.
  - **Airflow** sets how hard each fill block works. Bare packing with open sky gets 25%. A built tower with open sky above the distributor gets 25% plus 5% per extra fill block, up to 75%. A fan adds 100% at 64 RPM and a little more above that, and needs no open sky. The fan takes its shaft from above and costs 2 SU per RPM.
  - Rejected heat evaporates water (1 mB per 2,000 HU), so the basin slowly empties. Top it up by bucket or with Create pipes into its sides; a comparator reads its level.
  - The distributor shows fill count, airflow and heat rejection in goggles, and a vapour plume rises while the tower works.
- **Feedwater Tank:** a 4,000 mB surge tank that sits inline in the steam loop (sides) and takes makeup water through its top. A comparator reads its level.
- **Heat transfer follows flow:** exchangers, fill, radiators and turbines work on the water passing through them, so they perform the same whether they are brim-full or just carrying the loop's flow.
  - Tower packing stacks up to 12 high. Water enters the top block from the side and leaves the bottom block from the side. It needs open sky, evaporates a little water, and stops past 15° of tilt on a moving body.
  - Radiators reject per open face, with an airflow bonus from the body's speed capped at 2×.
- **Boiler Heating Plate:** sits in the primary loop (front/back) under a Create boiler tank. It heats the boiler (level 1 from 120 °C, level 2 from 200 °C) and draws heat only while that boiler runs with water. A redstone signal turns it off.
- **Heat accounting:** every unit of heat is stored, turned into stress by a turbine, sent to a Create boiler, rejected to the air, or vented. `/criticalmass ledger` shows each.

**Fuel.**
- **Assemblies:** each fuel column holds one Fuel Assembly and produces heat only while it is loaded. Assemblies keep their state on the item: lifecycle ID, remaining fuel, condition, residual heat, burn history and damage cause. They stack to 1. Advanced tooltips (F3+H) show the lifecycle ID.
- **Burnup:** remaining fuel falls with the heat the column produces. An assembly lasts 120,000 ticks of full-power operation by default (`fuel.lifetimeTicks` in the server config) and then becomes a Spent Fuel Assembly.
- **Wear and damage:** a full burn costs 25% condition (`fuel.normalWear`). Above 800 °C the column loses condition much faster. At zero the assembly fails, becomes a Damaged Fuel Assembly, and trips a running reactor (cause: fuel failure).
- **Service Port:** loads and unloads fuel from the vessel top.
  - Insert a fresh assembly by hand, hopper or funnel. It goes straight into a free fuel channel.
  - Spent and damaged assemblies come out automatically once the reactor stops, one at a time, and wait in the port until taken by hand or extracted by a funnel. A redstone signal on the port unloads everything.
  - Insertion is refused while the reactor runs or starts, while an assembly is waiting in the port, or when every channel is loaded. Goggles and the action bar give the reason.
  - Damaged and spent assemblies can never be reinserted.
- **Residual heat:** an unloaded assembly takes its column's decay heat with it.
- **Spent Fuel Rack:** holds 4 assemblies and 4,000 mB of water (bucket or pipe). Residual heat decays into the water. A dry rack heats up instead, and above 300 °C it damages the fuel it holds. Cooling progress is kept on each item.
- **Shielded Cask:** holds 4 cooled spent assemblies (residual heat 0.5 HU/t or less), fresh assemblies or waste canisters. It refuses hot or damaged fuel. Breaking it drops the cask with its contents, and placing it restores them.
- **Waste is never deleted:** breaking a port, controller, rack or cask drops what it holds.
- `/criticalmass fuel <controller> fill` loads fresh fuel into every empty channel, and `/criticalmass fuel <controller> burn <fraction>` sets the remaining fuel. Both are operator commands for testing.

**Fuel production.**
- **Ore:** Uranium Ore and Deepslate Uranium Ore generate in the overworld below y 48 and drop Raw Uranium (tagged `c:raw_materials/uranium`; fortune applies). Other mods' raw uranium with that tag works too.
- **Concentrate:** crush or mill Raw Uranium into Create's Crushed Raw Uranium, then bulk wash it (or mix it with 250 mB water) into Uranium Concentrate. Washing is skipped when Mekanism, Immersive Engineering or IC2 is loaded, because Create already washes crushed uranium for those mods.
- **Pellets:** compact 2 concentrate in a heated basin into a Ceramic Fuel Pellet.
- **Steel:** mix an iron ingot with coal or charcoal in a superheated basin into a Steel Ingot, and press ingots into Steel Plates. Steel goes into the vessel casing, reinforced pipe, valves, turbine blades and the shielded blocks. Ingots also craft into nuggets and a Block of Steel.
- **Alloy:** mix 2 steel, 1 zinc and 1 nether quartz in a heated basin into 2 Reactor Alloy Ingots. Press ingots into sheets and sheets into 2 Fuel Cladding. Ingots also craft into a Block of Reactor Alloy.
- **Frame:** a mechanical crafter makes an Empty Fuel Assembly from 4 alloy sheets and a Sealing Gasket (gaskets are compacted from dried kelp and an iron nugget).
- **Assembly:** a sequenced assembly of 4 loops of deploy cladding, deploy pellet, deploy pellet, press.
  - It runs on any Create belt or depot, or on the Fuel Assembly Bench, which is a depot that also lets you do each step by hand: use the next ingredient on it, and sneak-use with an empty hand to press.
  - Goggles show the next step. Progress is stored on the item, so an interrupted assembly carries on after a reload.
- **Damaged fuel:** once cooled (in the spent fuel rack), a damaged assembly becomes a Sealed Waste Canister that keeps its lifecycle record. By hand, the bench takes 2 Steel Plates and a Sealing Gasket. On machines, it is a sequenced assembly: deploy an Open Waste Canister onto the assembly, deploy a Sealing Gasket, then press it shut. The deployer leaves hot assemblies alone, and the half-sealed canister is as radioactive as the bare assembly. Canisters go in a Shielded Cask.
- **Fire and despawn:** assemblies and canisters resist fire and never despawn.
- **Placing canisters:** a Sealed Waste Canister can be placed as a block. It stays a radiation source where it stands, and breaking it (with a pickaxe) drops the canister with its record intact.
- **Other recipes:** every block and item has a survival recipe. They are generated by `art/tools/data_recipes.py`, and JEI shows them under Create's categories.

**Radiation.** On by default with gentle values; everything below is in the server config (`radiation.*`).
- **Dose rate:** in mSv/min. A player receives, from every source within 48 blocks, its strength times the shielding transmission divided by (1 + distance²).
- **Sources:** running or cooling reactors (from fission and decay heat), spent and damaged assemblies, waste canisters and (weakly) fresh fuel, wherever they are: placed canisters, racks (flooded racks let 10% through), casks (1%), the service port, the bench, dropped items and player inventories. Positions are world-space, so sources on a moving Sable body are measured where the body is now.
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

Game tests in `com.criticalmass.gametest` cover vessel validation, viewports, core heat, rod banks and their in-core display, tripping, port rules, pump stress, the exchanger, turbine capacity, a 10,000-tick conservation run of the full two-loop plant, steam phase and pressure, turbine admission, an end-to-end plant scenario (blocked start, rod motion and delayed heating, generation with energy accounting, SCRAM and cooled shutdown), control panels (linker links, hit-testing of every control, panels running the reactor, turbine and breaker, alarm lamps, redstone trips, lever and dial signals), the screen wall (shared power, linked text, and the reactor display with its trends and events), meltdown (a flooded melt held in the vessel, a dry melt burning through, concrete stopping corium), start-up checks, the power ramp and temperature feedback, the built cooling tower (airflow by construction, fan, water and heat accounting), the pressurizer and coolant flashing, the turbine governor and overspeed trip, the generator (Forge Energy against shaft work, load shedding, breaker), the reinforced valve, pipes (coolant held in pipes, Create pipes bursting from heat, heat carried through Create pumps), condenser, cooling tower, radiator, boiler plate, relief and rupture, turbine-trip handling, and the fuel lifecycle (burnup, overheating damage, service port round trips and refusals, waste retention, rack cooling, cask pickup and reload), recipe loading, hand assembly at the bench with a reload in the middle, damaged-fuel recovery, ore drops, assembling fuel holders onto a Sable body without duplication, and radiation (distance and shielding, casks and flooded racks, breach contamination, cleanup runoff, contaminated coolant, protective gear, dosimeter) (`./gradlew runGameTestServer`).
`src/inspect/scenes/make_reactor.py` builds a vessel with mistakes, repairs it, runs it until a hot spot forms, then breaks it, with goggle screenshots at each stage. `make_plant.py` builds the two-loop plant with a heat exchanger, then leaves and returns to check the unload freeze. `make_cracks.py` overheats a fuel column and records the casing cracking. `make_steam_plant.py` builds all three circuits (primary, steam, cooling) and trips the turbine. `make_operation.py` walks a reactor with rod drives through start-up, a lost-flow trip with emergency injection, and a breach; `make_ops_body.py` scrams a reactor tipped over on a Sable body. `make_fuel.py` burns a core out, unloads it through the service port by hand, cools the assemblies in a flooded and a dry rack across a rejoin, then casks them and picks the cask up and places it again. `make_production.py` assembles a fuel assembly by hand at the bench in survival, seals a damaged assembly into a canister, opens the JEI pages and generates ore veins. `make_radiation.py` measures a running reactor at several distances and behind each shielding block, compares a dry rack, a flooded rack and a cask, breaches the vessel and cleans up the contamination, and follows a hot rack on a teleported Sable body.

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
| JEI | 19.69.0.456 | Dev runtime only, to check recipe pages |

Create is a hard dependency. Sable is declared optional in `neoforge.mods.toml` until the mobile-integration
design settles whether stationary-only play should work without it.

Maven sources: `maven.createmod.net` (Create, Ponder, Flywheel), `mvn.devos.one/snapshots` (Registrate),
`maven.ryanhcode.dev/releases` (Sable), `maven.blamejared.com` (Veil, required by Sable, and JEI), `api.modrinth.com/maven` (Aeronautics).

## License

All Rights Reserved until a license is chosen (`mod_license` in `gradle.properties`).
Create Aeronautics code is MIT, its assets are All Rights Reserved; this project compiles against it and does not redistribute it.
