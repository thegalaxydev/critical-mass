# Critical Mass

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
- Operating manual (circuits, a minimal plant, control room wiring, start-up, shutdown, faults, expansion): `docs/OPERATING.md`. Overhaul plan and status: `docs/OVERHAUL.md`. The fuel works, a factory that makes Fuel Assemblies from raw stone and metal and the coolant, Boric Acid, Fuel Oil, Shielding Concrete, Resin Cartridges and waste canisters a plant uses up: `docs/FACTORY.md`.
- Ponder scenes: there are thirty-four, listed in the Ponder index under Reactor Plant, Nuclear Fuel, Control Room, Plant Electrics and Reactor Accidents. Hold the Ponder key on a part to open its scene.
  - **Reactor Plant:** building a vessel (casing, the controller, the channels, the ports and the viewport), control rods (the Control Rod Drive), the primary loop (the Circulation Pump, Reinforced Pipe, the Heat Exchanger and the Pressurizer), the steam loop (the Steam Turbine, the Condenser and the Relief Valve), the feedwater train (the feed tank, the Threshold Switch, the feed pump and the feedwater heater, also reached from Create's Fluid Tank and Threshold Switch), rejecting heat (Cooling Tower Packing and the Radiator), refuelling (the Service Port, the Spent Fuel Rack and the Shielded Cask), refuelling one column at a time (the Fuel Channel Cap and the core map), the Area Radiation Monitor, cleaning the coolant (the Demineralizer and its Resin Cartridges), Heat Exchanger tube leaks (opened from the Heat Exchanger or a Reactor Alloy Sheet), growing Chartrine (Chartrine and the Contaminated Coolant Bucket), pump seals (the Sealing Gasket and the Circulation Pump), taking a pipe through a containment wall (Shielding Concrete and Reinforced Pipe), and holding coolant in a Fluid Tank (the Boric Acid and Primary Coolant Buckets, also reached from Create's Fluid Tank).
  - **Nuclear Fuel:** enrichment (the Centrifuge and its cascade), making a Fuel Assembly (pellets, cladding and the sequenced assembly), reprocessing spent fuel (the saws and the dissolver), and sealing waste in canisters.
  - **Control Room:** building a control desk (the Reactor Alloy Dashboard and its five parts), and Plant Screens (walls, the Plant Linker, machine displays and Display Link text).
  - **Plant Electrics:** wires, the Circuit Breaker, the Generator, the Transfer Switch and the Transformer.
  - **Reactor Accidents:** the Dashboard Switch as a bypass, Corium, the Survey Meter and Fallout Ash, the Water Cannon, and cleaning up after an explosion (Core Debris, Wreckage, Decontamination Compound and the all clear).
  - `src/inspect/scenes/make_ponder.py build` writes the scene that builds their schematics into `assets/criticalmass/ponder/`, or only the ones named after it, as in `build rods,steam`. `make_ponder.py view` writes the scene that screenshots them, and `make_ponder.py lang <dump>` puts the scene text into `en_us.json`. The scripts are in `ponder/PlantScenes.java`, `ponder/FuelScenes.java`, `ponder/ControlScenes.java` and `ponder/RecoveryScenes.java`.
- Item descriptions: the plant's blocks and items have Create's own kind of description, a summary shown while Shift is held, with the stress a kinetic part costs under it. The text is in `en_us.json` under each item's `.tooltip.summary`, with `.tooltip.condition1` and `.tooltip.behaviour1` where a part does something when used.
- JEI: Create's Mysterious Conversion category lists the two things that are made in the world and not by a recipe, Chartrine from a Contaminated Coolant Bucket and a Charred Log from a log. The Ponder scenes say how.
- Advancements: thirty-six, in one tree under a Critical Mass tab, which opens when you first dig up uranium.
  - **Fuel:** digging up Chartrine, Uranium Concentrate, enrichment, a cascade of three Centrifuges, pellets and a finished Fuel Assembly.
  - **Reactor:** setting a Reactor Controller, first criticality, 90% power, a turbine turning, a Generator on the grid and a power order met.
  - **After the reactor:** taking spent fuel out, bolting it into a cask, reprocessing it and sealing waste.
  - **Control room:** fitting a part to a desk and linking a wall of Plant Screens.
  - **Radiation:** a Survey Meter, an Area Radiation Monitor, a full protective outfit and, hidden, a lifetime dose of 1,000 mSv.
  - **Accidents:** hidden until earned: switching the protection off, a fuel column melting and a whole core going off, which lead into the seven for recovery. Holding a Used Resin Cartridge and growing Chartrine hang from the melted column.
  - Things that happen at a machine, such as criticality, are given to everyone within 64 blocks of it, 128 for a power order. They are in `data/criticalmass/advancement/`, and the ones the game cannot see by itself use the `criticalmass:milestone` trigger.
- Creative tab sections: the Critical Mass tab is laid out in six sections, each under an animated banner the width of a row: Reactor, Equipment, Console, Fallout, Electricity and Building. A banner plays while the pointer is over it. Which section an item is in, and the order inside it, is the list in `registry/TabSection.java`; anything not listed goes to the end of Equipment. The banners are generated pixel art, painted by `art/tools/tab_banners.py fresh` as strips of 162 by 18 frames in `textures/gui/banner/`. A banner's frame count and speed are set beside its section in `TabSection.java`.
- Art direction and the texture pipeline: `docs/art/STYLE.md`. Reference breakdown: `docs/art/REFERENCES.md`.
- QA contact sheets (iso views, functional faces, item sizes, outfit poses, kinetics, Aeronautics body): `docs/art/qa/`.
- Generated art and how each texture was made (raw outputs, processing): `art/SOURCES.md`, `art/generated/`. The scripts that paint textures, export models and make sounds: `art/tools/`.
- Fluid textures live in `textures/fluid/` and are added to the block atlas by `assets/minecraft/atlases/blocks.json`.

## Reactor and coolant loop

Values live in `content/thermal/CMThermal.java`.

**Vessel.** A vessel is a box of Reactor Vessel Casing, odd on every side, from 5x5x5 up to 11x11x13.
- Exactly one Reactor Controller sits on a side face and faces outward.
- A Primary Coolant Port set to inlet and one set to outlet go on faces, facing outward (toggle with a wrench).
- A Reactor Service Port goes on the top face, facing up. Its hatch is shut until it is turned open: put a Valve Handle on top and use it to open the hatch, and sneak-use it to shut it again. A shaft from above does the same, by its direction. Fuel goes in and comes out only while the hatch is open, by hand, by a funnel or hopper, or by a Mechanical Arm, which can pick the port as a target.
- A Fuel Channel Cap takes the place of a casing block in the top face, over a fuel column, and gives that one column its own way in and out. Craft it from a Reactor Vessel Casing, a Steel Plate and a Sealing Gasket.
  - **Locking:** it is locked until a Wrench turns it, and a Wrench locks it again. A redstone signal also holds it unlocked.
  - **By hand:** with the cap unlocked, use an assembly on it to lower it into that column, and an empty hand to lift the column's assembly out, spent or not.
  - **By machine:** funnels, hoppers and Mechanical Arms work a cap the way they work the Service Port, and an arm can pick it as a target. A Deployer does what a hand does, so a Deployer on a Gantry Carriage over a row of caps is a refuelling machine.
  - **Rules:** fuel moves only while the reactor is shut down. An unlocked cap keeps the reactor from starting and stops one that is running, and it shields a quarter as well as casing, so the head of an open core is no place to stand.
  - **Reading:** goggles on a cap show how much fuel its column has left, and a comparator gives the same, with 0 for an empty column. A Plant Screen wall linked to a cap or to the Service Port shows every column at once as a core map.
  - **Shuffling:** columns do not burn evenly. A column makes 25% more for each fuel column beside it and burns down that much faster. Caps let part-burnt assemblies change places, the fresher ones into the middle and the tired ones to the edge, so a core is used up evenly. The Service Port only hands out what is already spent and loads whichever column is free.
- Only casing may sit on edges and corners.
- The inside holds vertical columns of fuel, control or coolant channels, one type per column; empty columns are allowed. Each channel is an open bundle of rods between two end plates, so you can see through a column to the ones behind it.

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
- **Readings:** goggles show little, the way Create's own do: a block's state and one or two readings. On the controller that is its state, neutron power, fuel temperature and coolant. Reactivity, period, xenon and boron are on the Plant Screen and dashboard gauges, and are Display Link sources, with a neutronics summary for a Display Board.
- **Boron:** boron dissolved in the primary coolant holds reactivity down, 7 pcm for each ppm, evenly through the core. It is the slow control. Let it carry the fuel's spare reactivity so the rods can run nearly out, and change it over minutes, not seconds.
  - **Borax and Boric Acid:** crush Scoria for Borax, which comes one time in four. Mix 1 Borax with 1,000 mB of water in a heated basin into 1,000 mB of Boric Acid, which holds 7,000 ppm.
  - **Borating:** pump Boric Acid into the vessel with Create parts: a Fluid Tank of acid, a Mechanical Pump and a spare inlet port, the way emergency cooling is built. The acid joins the coolant and its boron goes with it. 1,000 mB of acid into 7,000 mB of coolant leaves 1,000 ppm.
  - **Diluting:** pump clean Primary Coolant in the same way and the figure falls.
  - **Letdown:** a full vessel takes nothing more, so draw coolant off first with a pump on a port. What is drawn off takes its share of the boron and leaves the concentration as it was. A Fluid Tank keeps the boron of the coolant let down into it, so pumped back it brings the same figure.
  - **Holding and make-up tanks:** a Create Fluid Tank keeps the boron, the heat and the contamination share of the Primary Coolant, Contaminated Coolant or Boric Acid in it, and gives each back in proportion to what is drawn off. Boric Acid and coolant pumped into the same tank mix there, so a tank can be made up to any figure and held ready. A Basin, a Spout and other Create blocks keep nothing, and neither does coolant carried in a bucket.
  - **Reading a tank:** the Plant Linker remembers a Fluid Tank. A Dashboard Gauge shows its boron, level or temperature, a Plant Screen wall shows all three, and a Display Link on the tank offers its boron and temperature.
  - **It travels with the coolant:** boron is carried the way heat is. The Circulation Pump takes it round the loop, a leak takes it out with what it spills, and coolant that boils off leaves its boron behind, so the figure rises as the level falls. Topping up a leaking plant with clean coolant dilutes it. Boric Acid goes into Primary Coolant only, never into plain water.
  - **Where to read it:** the Plant Screen's reactor display, a Dashboard Gauge bound to the controller and a Display Link all give boron in ppm.
  - **Mistakes cost:** diluting a critical reactor adds reactivity as surely as pulling rods does, and a core held down mostly by boron is not shut down by its rods alone. Boron is worth only what the water is, so the term fades as the vessel empties.
  - **Emergency boration:** fill the emergency cooling tank with Boric Acid instead of Primary Coolant and it borates the core as it cools it. Plain emergency coolant dilutes.
  - **Command:** operators with permission can set the vessel and its loop to a figure with `/criticalmass boron <controller> <ppm>`.
  - **Ponder:** the scene opens from Borax, the Primary Coolant Port and the Reactor Controller.
- **Coolant clean-up:** a Demineralizer takes the contamination out of coolant and keeps it in a Resin Cartridge, so a loop that a fuel failure has contaminated can be brought back without draining it.
  - **Contamination is carried:** coolant carries its contamination the way it carries heat and boron. A fuel failure contaminates the vessel's coolant wholly, a split keeps the share and a merge averages it. Coolant with more than one part in a hundred is Contaminated Coolant, and at or under that it is Primary Coolant again.
  - **Reading it:** the share is the Coolant activity reading, in percent. The Plant Screen's reactor display lists it under boron. A Dashboard Gauge, a Plant Screen wall and a Display Link read it from the Reactor Controller, a Fluid Tank, the Steam Turbine, the Condenser and other coolant machines, from a Heat Exchanger for its steam side, and from a Demineralizer for the coolant waiting in it. Watch it fall on the controller to follow a clean-up, and rise on a Heat Exchanger to see a tube leak.
  - **The reactor's record:** beside its contamination, coolant carries a record of how much of it a failed core made. The record follows the coolant through pipes, machines and Fluid Tanks, is shared out when coolant is watered down and decays with it. Coolant that comes from anywhere that keeps no record, such as a bucket, a Basin or an Item Drain, is still counted as fully contaminated for radiation and clean-up, but has no record. Only recorded contamination grows Chartrine (see Chartrine under the fuel chain). A Dashboard Gauge, a Plant Screen wall and a Display Link read it from the Reactor Controller and a Fluid Tank as the Reactor's share of activity, in percent.
  - **The line:** set a Demineralizer beside the vessel. One Create Mechanical Pump draws coolant off through a port and into it, and a second pumps what it has cleaned back through a spare inlet port. Pipes join every face but the top. It cleans 10 mB a tick and holds 1,000 mB waiting and 1,000 mB cleaned.
  - **What it does:** coolant goes in on any pipe and comes out clean on any other. Its heat and its boron pass with it. Clean coolant passes with or without resin, and contaminated coolant waits until there is resin to take it.
  - **How long:** each pass takes out what goes through, so the loop's share falls steadily. About four and a half times the loop's volume has to go through to bring it under one part in a hundred.
  - **Resin Cartridge:** use one on the Demineralizer to fit it and an empty hand to lift it out. A cartridge takes up what 4,000 mB of wholly contaminated coolant holds and is then spent, and the line stops. The window on the front shows the bed: amber when fresh, part brown in use and brown when spent.
  - **Used Resin Cartridge:** a cartridge that has taken anything up is radioactive, 3 mSv/min for each 1,000 mB it has cleaned and 12 when spent, the same as the coolant gave. One that still has resin goes back in. A spent one goes into a Shielded Cask, and it resists fire and never despawns.
  - **Automation:** a funnel or a Mechanical Arm fits a cartridge and takes it out once it is spent.
  - **Reading:** goggles show its state and the resin left, and a comparator gives the resin left from 0 to 15. The Plant Linker remembers it: a Dashboard Gauge and a Plant Screen wall show the resin left, a Dashboard Alarm lamp lights when the resin is spent or coolant is waiting with none fitted, and a Display Link gives the level as text.
  - **Other waste water:** Contaminated Coolant from reprocessing, from leaching Fallout Ash and from Decontamination Compound can be pumped through one as well, in place of sealing it in canisters.
  - **A Fluid Tank keeps the share:** coolant let down into a Create Fluid Tank keeps its share there and takes it along when it is pumped on. Contaminated Coolant from anywhere else, such as a Basin, a Spout or a bucket, counts as wholly contaminated.
  - **Breaking it** spills what it holds as contamination on the spot and drops the cartridge.
- **Jump to power:** `/criticalmass steady <controller> <fraction> [coolant °C]` puts a reactor at that power with matching fuel temperatures and xenon, optionally sets the coolant temperature, and sets the rods to the position that holds it.
- If the vessel breaks while running, the reactor trips. It stays tripped until it is reset with the rods commanded in.

**Casing integrity.**
- Casing next to a fuel column hotter than 800 °C loses integrity and shows cracks: stage 1 below 75%, stage 2 below 50%, stage 3 below 25%.
- Below 25% the reactor counts as damaged and cannot restart until it is repaired.
- Holding use on cracked casing with a Welding Torch for 2 seconds welds it: each weld restores 50% and uses a Steel Plate from your inventory and a point of the torch's durability (128 welds; repair it with steel plates on an anvil). Held on Wreckage the same way, it cuts the wreckage into scrap and uses no plate.
- Integrity is saved with the controller and also shows on the blocks themselves.

**Loops.**
- **Primary loop:** run pipe from the outlet port, through a Circulation Pump (back to front) and any machines on the way, to the inlet port. The controller holds the vessel coolant (8,000 mB). A pump placed beside a pipe, tank or machine turns to point into it, like Create's Mechanical Pump; sneak to keep the facing you chose.
- **Closed loops:** a pump can also drive a loop that comes back to its own back face without touching a vessel. Use this for the secondary circuit.
- **Paths:** every loop must be a single path without branches. It may pass through a Heat Exchanger twice, once through each side.
- **Pipes:** loops are plumbed with Create's fluid pipes, copper, glass or encased. Plain pipes carry anything, but they are not sealed or shielded: they leak under pressure and give off radiation while they hold primary coolant. Reinforced Pipe does neither. Every pipe block in a loop holds 100 mB of coolant, so a new loop takes extra coolant to fill, and the coolant travels through it. One Circulation Pump drives its whole loop, with no booster pumps along the way. The limit is length: a closed loop can have 255 pipes and machines in it, and a run between two vessels 255 on each side of the pump. Past that the pump stops and reports that the pipe run is too long. Used outside a loop, on a Mechanical Pump, a Reinforced Pipe is an ordinary Create pipe and the pump reaches as far along it as along any other.
- **Brackets:** Reinforced Pipe takes Create's brackets like any straight pipe, and a Wrench takes them off again. A bracketed pipe keeps its run and joins nothing laid beside it, so a bracket on a loop's pipe stops another pipe from branching into the loop.
- **Locking a run straight:** use a Wrench on the side of a Reinforced Pipe and it locks straight, the way a Create Fluid Pipe turns to glass. A locked pipe runs one way only and joins nothing laid beside it, so two runs can lie side by side without branching into each other. On a bend or a tee it locks along the arm nearest where you clicked, and a Wrench on the end of a run does nothing. A locked run shows a flange at each joint. Use the Wrench on a locked pipe to free it again, and it joins what is round it. It keeps the coolant in it both ways and drops a Reinforced Pipe when broken. A Reactor Alloy Ingot used on a Glass Fluid Pipe makes a locked Reinforced Pipe along the same run, and that keeps its coolant too.
- **Through a wall:** use Shielding Concrete on a straight or locked Reinforced Pipe and it becomes an Encased Reinforced Pipe, a full block with the pipe running through it, the way Create's casing encases a Fluid Pipe. It shields like Shielding Concrete, stops a nuclear explosion and corium as it does, and still carries coolant along its one way, so a pipe can pass through a containment wall without leaving a hole. It uses up the block of concrete. A Wrench takes the casing off, leaves a locked pipe and gives the concrete back, and breaking it drops the pipe and the concrete. A bend or a tee cannot be encased.
- **Reinforcing:** use a Reactor Alloy Ingot on a placed Fluid Pipe or Fluid Valve and it becomes a Reinforced Pipe or Reinforced Valve where it stands, keeping its connections, the way Create's casings are made. A Deployer holding the ingot does the same to pipes and valves on a belt or depot. Reinforce before filling: a pipe that already holds coolant loses what was in that block.
- **Leaks:** a plain pipe or Fluid Valve loses 0.004 mB a tick for each bar above 1, up to 1 mB a tick. For steam that is the steam pressure in the pipe. For primary coolant it is the pressure the Pressurizer holds, so a loop at 100 bar loses about 0.4 mB a tick from every plain block, and what leaks contaminates the spot. A loop with no pressure on it does not leak.
- **Radiation:** every plain block holding primary coolant is a small radiation source, five times stronger if the coolant is contaminated.
- **Bursting:** any pipe, reinforced or not, bursts when the steam inside it passes 12 bar (no drop, scalds anyone close), so put a Relief Valve in every steam loop.
- **Create pumps:** coolant ports and the coolant machines accept Create pipes on their pipe faces, so a mechanical pump can fill or drain them. Coolant keeps its heat when a Create pump moves it between two of these machines.
- **Pump:** moves 2 mB/t per RPM, up to 256 mB/t, and stops when its shaft stops. It pumps water, not steam: if more than 2% of what reaches its intake has boiled it cavitates and loses that share of its flow, down to a tenth of it. Goggles say so, and it rattles. Stress is 4 SU per RPM while circulating, a quarter of that while idle, and stops rising past 128 RPM.
  - **Seal wear:** the pump's seals wear while it moves coolant, worn out in 10 hours of running, and ten times as fast while it cavitates. Goggles show the condition once there is wear.
  - **Seal leak:** from three quarters worn the seals leak, up to 1 mB a tick when worn out, out of the pipe beside the pump and whether or not the pump is turning. Goggles show the leak in mB a tick. Water is only lost. Primary coolant also contaminates the floor at the pump, which does not fade and takes Decontamination Compound to clear, so an Area Radiation Monitor in the pump room catches the leak.
  - **New seals:** stop the pump and use a Sealing Gasket on it to take a quarter off its wear. Used on a pump that is turning, it hurts you and takes no gasket.
  - **It keeps its wear** when it is broken and placed again.
- **Heat Exchanger:** the primary circuit enters and leaves through its front and back, the secondary through its top and bottom. The two never mix while its tubes are sound. Heat crosses from the hotter side only while both sides are flowing.
  - **Tube wear:** the tubes wear while heat crosses them, faster the further apart the two sides are in temperature. At 150 °C apart or more a block is worn out in 10 hours of running, and at 50 °C apart in 30, so a taller steam generator lasts longer. Hot coolant flowing through with the steam side dry wears them out in half an hour. Goggles show the condition once there is wear.
  - **Tube leak:** from three quarters worn the tubes leak primary coolant into the secondary side, more as they wear, up to 4 mB a tick when worn out. The primary loop loses coolant, and the steam loop gains it as water, with its heat. Goggles show the leak in mB a tick.
  - **Radiation in the steam plant:** leaked coolant carries activity into the steam loop, a fifth of what Contaminated Coolant carries if it is clean and all of it if it is contaminated. It goes round the loop with the water and steam and halves every minute, so it is there only while the tubes leak. Every block and pipe it is in gives a dose, 3 mSv/min at one block for what 1,000 mB of Contaminated Coolant carries. An Area Radiation Monitor in the turbine hall set low is how a leak is caught early.
  - **Plugging tubes:** use a Reactor Alloy Sheet on the exchanger to take a quarter off its wear. Both loops must be stopped and both sides at 60 °C or under, or it burns you and takes no sheet.
  - **Alarm:** a Dashboard Alarm lamp bound to a Heat Exchanger lights while its tubes leak.
  - **It keeps its wear** when it is broken and placed again.
  - **Stacking:** exchangers set one on another, facing the same way, work as one column of up to 8, and every block adds its own exchange area. The secondary runs up through the whole column. The primary goes in through the front or back of one block and comes out of the opposite side of whichever block has the pipe there, crossing every block in between.
- **Steam Turbine:** fills a three-block cube and is placed like Create's Large Water Wheel, with the big end facing you. Steam goes in through the back, the exhaust leaves through the bottom or the top, whichever has a pipe, and the shaft comes out of the front. It draws heat from that coolant and supplies 4 SU per HU/t at a fixed 16 RPM. (It runs on hot water or coolant until steam exists.)

**Operation and safety.**
- **States:** the controller shows Cold, Starting, Running, Tripped (with the cause), Cooling Down, Damaged or Breached. State and residual heat survive saving.
  - Its face shows the state from across the room. The screen is dark while there is no sound vessel. A shut down reactor shows an amber line and the amber lamp, a starting or running one a green trace and the green lamp, and a tripped, damaged or breached one a red line and the red lamp.
- **Rod drives:**
  - A Control Rod Drive on the top face above a control column moves that column's rods; its redstone signal withdraws them.
  - Rods move at a rate set by shaft speed, up to 0.02 per tick, and the drive uses stress only while moving.
  - The drive is turned by the cogwheel round its middle. Mesh a Cogwheel beside it, or set drives side by side and they turn each other. It takes no shaft.
  - Columns without a drive follow the controller's redstone signal.
  - **Visible travel:** the rod stands up out of the drive as it withdraws, one block of rod per block of core height, and drops back in as it inserts. Inside the vessel each control channel shows how far its rod reaches.
  - **Banks:** every drive belongs to a bank, A to D. Hold use on the drive's upper case to pick it from Create's value board. A bank target set on the reactor moves every drive in that bank and overrides their redstone signals.
  - **Rod lever:** a Dashboard Lever bound to the Reactor Controller works a bank the way an in-hold-out switch does. In the middle it holds the rods where they are. Pushed up it withdraws them and pulled down it inserts them, faster the further it is from the middle: one notch moves them 0.6% a second and all seven notches 4% a second. Its settings pick bank A, B, C, D or all of them. A new lever stands at the bottom, which is rods in, and a reactor will not reset from a trip while a lever is asking for rods out.
  - **Power Regulator:** the value box on the controller's face sets a power from 1% to 100%, or Off. While it is on the controller moves bank D by itself to bring neutron power to that figure and hold it there, through xenon, fuel burn-up and changes in steam demand. It raises power no faster than a 20 second period, so it does not trip the reactor on the way up, and it leaves the other banks alone. It needs at least one drive in bank D with travel left. A trip switches it off. From a control desk, two Dashboard Buttons bound to the controller raise and lower the setpoint 5% a press: raise switches the regulator on at the power the reactor is at, lower switches it off below the last step, and a Gauge set to Regulator setpoint shows the figure.
- **Reactor Viewport:** a window that takes the place of a vessel casing block (craft casing with Shielded Glass). It cracks, welds and shields like casing. Through it, fuel channels are dark when idle, glow dim blue at low power or on decay heat, bright blue at power and orange-red when overheating, and they light their surroundings.
- **Fail-safe:** losing shaft power, overstress or a trip releases the drive's spring latch and drives the rods fully in within 2 seconds, in any orientation.
- **Start-up check:** rods will not withdraw, and the reactor will not start, until coolant is flowing through the vessel and the vessel is at least half full. The controller says which is missing. Pumps turn on shaft power, so a cold plant needs rotation from outside before it can make its own. Electric Motors fed from a Grid Connection are one way to give it.
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
- **Plant Screen:** a screen with green text, built like Create's Display Board in a steel frame. A wall of them shows a display of its own for whatever machine it is linked to, or Display Link text like a Display Board.
  - **Machine display:** use the Plant Linker on any machine a gauge can read, then on any screen of a wall at least 3 wide and 2 high. That covers the reactor, turbine, pumps, pressurizer, heat exchangers, condenser, tanks, rod drives, generators, motors, breakers, bus bars, transformers, Station Batteries, Diesel Engines, the Grid Connection, Area Radiation Monitors and Demineralizers.
    - The wall shows the machine's name and its state, trend graphs of its readings over the last two minutes, and a table of every reading.
    - How much it shows depends on its size. Three by two is one graph. A bigger wall fits more graphs, up to six, and from 5 wide the table stands beside them. A narrow wall 4 or more high puts the table under them.
    - Readings that start at zero scale themselves to what they have seen. Frequency, phase angle and temperature are graphed on a fixed window, so 50 Hz sits in the middle of 45 to 55.
  - **Core map:** use the Plant Linker on a Reactor Service Port or a Fuel Channel Cap, then on a wall at least 3 wide and 2 high. The wall draws the vessel's columns from above, with north at the top.
    - A fuel column is a square with the percent of fuel it has left, a bar of the same and its temperature. It is green in use, amber when spent or under 15%, red when damaged, over 800 °C or melted, and dim with two dashes when empty.
    - A capped column has a mark in its corner: a filled square for a locked cap and an amber outline for an unlocked one.
    - A control rod is a ring with the percent it is inserted, and a coolant column is a small dim square.
    - Beside the map, or under it on a narrow wall, the wall says whether fuel can move or why it is locked, how many columns are loaded, the lowest fuel, the hottest column and how many caps are unlocked, with a key to the marks where there is room.
    - A Dashboard Gauge does not bind to a cap or the Service Port.
    - Where the table stands beside the graphs and there is room under it, the wall lists the machine's latest changes of state with their age.
    - Linking another machine to the same wall replaces the first and starts the graphs again.
  - **One-line diagram:** a wall at least 5 wide and 3 high linked to a Bus Bar draws everything wired to that bar, however far, as a diagram. The Grid Connection or a generator is at the top, and each breaker, switch, transformer, bus, generator, motor and battery hangs from what feeds it. Insulators and arresters are left out.
    - A filled square is a closed breaker, an empty one is open and a crossed amber one has tripped. A Transfer Switch is a diamond with N, S or a dash. Live parts are bright, dead ones dim, and trouble is amber.
    - Beside each part is its reading: megawatts through a transformer, made by a generator or drawn by a motor, kiloamps through a breaker, kilovolts on a bus and the charge of a battery.
    - A smaller wall linked to a Bus Bar shows the bar as a machine display.
  - **Reactor display:** a wall at least 9 wide and 5 high linked to a Reactor Controller shows the whole reactor by itself. A smaller one shows the reactor as a machine display.
    - Four trend graphs cover the last two minutes: neutron power, pressure, coolant temperature and xenon. Each scales itself to what it has seen.
    - The centre panel shows the status, with trips in red, and every reading: neutron power, heat output, period, reactivity, fuel and coolant temperature, pressure, flow, rod position, xenon and boron.
    - Below that is a list of the latest events with their age: status changes, trips, and alarms raised and cleared.
    - Link a Steam Turbine to a wall that already shows a reactor to add its speed and shaft load. Linked to a wall of its own, a turbine gets its own display.
  - A linked wall smaller than 3 by 2 says so on its face and in goggles. An empty linker unlinks the wall, and it goes back to Display Link text.
  - Screens placed beside and above each other join into one wall. Each block gives three lines and about five characters.
  - It runs on rotation, like Create's Display Board. Each screen holds a cogwheel that meshes with its neighbours, so one turning cogwheel beside any screen drives the whole wall. Each screen costs 0.25 SU per RPM. A wall stays lit for up to 20 seconds after it stops turning, and a dark wall keeps its text for when power returns.
- **Meltdown Siren:** a brass horn on a steel body that stands on a floor or hangs on a wall. Craft it from a Steam Whistle, three Brass Sheets, two Steel Plates and an Electron Tube.
  - **Linking:** use a Plant Linker on a Reactor Controller, then on the siren. Used on the siren with nothing stored, the linker unlinks it.
  - **Warning:** once the reactor's hottest fuel passes the 1,100 °C trip temperature the siren starts to growl, and the note climbs as the fuel nears 1,800 °C, where it melts.
  - **Meltdown:** once fuel has melted or the vessel is breached it wails at full cry: it is driven for six and a half seconds, climbing and then holding its top note, and let coast for four and a half, an eleven second cycle.
  - **Winding:** it climbs to its top note in about five seconds and takes about seventeen to coast down to silence, falling fast at first and then slowly. It is heard 96 blocks away.
  - **Sound:** the note is a real siren's, two tones a minor third apart, and its pitch follows the speed of the rotor through two octaves.
  - **Silencing:** use the siren with an empty hand. A silenced warning stays quiet until the reactor is safe again, but a meltdown sets it off again and has to be silenced by itself.
  - **Redstone:** any redstone signal sounds it at full cry, linked or not, and that cannot be silenced by hand.
  - **Ponder:** its scene is under Reactor Accidents.
- **Control desk:** the Reactor Alloy Dashboard and its parts. The desk and part models and the part textures come from Create: Loconautics, used with its authors' permission; the desk has its own reactor alloy colours. Loconautics does not need to be installed.
  - **Reactor Alloy Dashboard:** a sloped desk. Desks placed side by side join into one run of up to 5, and each holds two parts, one in each half. Craft it from a Display Board, two Reactor Alloy Ingots and a Block of Industrial Iron.
  - **Parts:** Dashboard Button, Switch, Lever, Gauge and Alarm. Sneak-use a part on its target to bind it, then use it on the half of a desk you want it in. Used on any other surface it mounts there instead, as Loconautics' parts do: on a floor, a wall or a ceiling, up to four to a block in a two by two grid, or one in the middle of the face if you aim at the middle. They can sit under water. Mounted parts work the same as on a desk and come off when the block behind them goes. A part named in an anvil keeps that name as its label. The Plant Linker re-binds a part already on a desk.
  - **Button:** on a Reactor Controller it is SCRAM, reset, rods in, rods out, or raise or lower the Power Regulator's setpoint; on a Steam Turbine it is trip or reset; on a receiving Redstone Link it sends a full signal for one second. A redstone pulse into the desk presses its buttons.
  - **Rod lever:** a Lever bound to the Reactor Controller moves a rod bank: see Rod drives above. It starts in the middle, holding, when it is bound.
  - **Switch and Lever:** bind to a receiving Redstone Link. The switch holds it on or off. Hold use on the lever and move the mouse up or down to set any strength from 0 to 15; a bar beside the crosshair shows it, as Loconautics' lever does. Two controls on one link do not fight: the strongest wins.
  - **Gauge:** binds to any plant machine and shows one of its readings on a needle; using it shows the number.
  - **Alarm:** a caged lamp. It binds to a Reactor Controller and watches one alarm or all of them, or to a piece of electrical equipment, where it lights for trouble there: a breaker tripped by its protection, a disturbed or lost grid, a hot or worn transformer, a generator shut down by a fault, a battery holding up its bus or flat, an engine out of fuel, a Transfer Switch off its normal supply, a motor stopped by its supply, a dead bus, or anything burnt out. A new alarm flashes the lamp, sounds a klaxon and makes the desk give a redstone signal until you press the lamp; it then stays lit until the fault clears. A comparator counts the active alarms.
  - **Settings:** use a wrench on a part to open its settings. Pick the body (steel, red, dark, brass or reactor alloy), the cap, knob, lamp or needle colour, the style where there is one (large button, ball lever), the dial face of a gauge, the backplate, an inverted signal, a label, and what the part does: a button's action, a gauge's reading, the alarm it watches. A gauge opens its settings as soon as it is fitted.
    - **Indicator light:** a small bulb on a button, switch, lever or gauge that lights with the part's output or reading.
    - **Glass cover:** a hinged cover over a button or switch. Sneak-use the part to lift or shut it; while it is shut the part cannot be used by hand.
    - **Labels:** type one in the settings, name the part in an anvil, or use a named Name Tag on a fitted part. `&` colour codes work. A part taken off keeps its label as its name.
    - **Copying:** sneak and pick a part in creative to get one with the same look, cover, inversion, action and label. Using a bound spare part on a fitted one of the same kind re-binds it.
  - **Goggles:** look at a part to see its label, what it does, the block it is connected to and where, and its state. A missing target shows in red, and an inverted output is named. Nothing shows while a lever is held.
  - **Taking parts off:** sneak-use a wrench on a part, or break the desk while aiming at a part to take only that part.
  - **With Create: Loconautics installed:** its Gauge Button, Switch and Lever are hidden and their recipes switched off, because ours do the same job. Its dashboards and its Locomotive Gauge stay. Using one of our parts on an empty Loconautics dashboard turns that desk into ours in the same colour, and dye recolours our desks to any Loconautics colour. A desk recoloured this way drops the matching Loconautics dashboard. Without Loconautics those desks show in reactor alloy.
  - **Reactor Signal Interface:** for redstone instead of direct binding. Set it in the vessel wall and hold use on its face to pick its job from Create's value board.
    - Its face shows the job and the signal. A brass tab sits in one of six places along a track, in the order the board lists the jobs, and the red lens lights while the interface takes or gives a signal.
    - SCRAM, reset, rods in and rods out each act once on a redstone pulse. A rod pulse moves every bank 0.5%.
    - Alarm gives a redstone signal while any alarm is active: send it through a Redstone Link to a Steam Whistle on plant steam, a bell or note blocks.
    - Readings only is the setting it starts on, for Display Links.
    - Look at its face to see what it is set to.
  - **Rods by signal strength:** a signal at a Control Rod Drive or the Reactor Controller sets the rods coarsely, 0 for fully in and 15 for fully out.
  - **Turbine by signal:** a full signal at the Steam Turbine shuts its steam off and resets an overspeed trip. Weaker signals limit the governor.
- **Pumps and valves** are switched with Create's own parts: a clutch or gearshift and a lever on the pump shaft, a Valve Handle or powered shaft on a Reinforced Valve.
- **Primary pressure:** vessel coolant boils at 350 °C unless it is pressurized. Coolant hotter than its boiling point flashes to steam and is lost, a quarter of the excess each tick, and the boiling alarm is raised.
- **Pressurizer:** a vessel two blocks tall, so it needs the block above it free. Touching the reactor vessel, it raises the primary pressure with heaters, and every bar adds 2 °C to the boiling point (100 bar gives 548 °C, the 150 bar maximum 648 °C).
  - Set the pressure on its value box. The heaters need a lit Blaze Burner directly under the Pressurizer: a kindled burner raises 0.25 bar per tick and a seething one twice that. Pressure leaks 0.02 bar per tick, so holding it means keeping the burner fed.
  - It reads nothing until a gauge is fitted: use a Dashboard Gauge on one of its sides. The dial then shows the pressure, and goggles give the figure and the setpoint. A wrench takes the gauge off again, and breaking the Pressurizer drops it.
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
- **Total meltdown:** a core whose every fuel column has melted goes off as a nuclear explosion three seconds later.
  - **Crater:** a 15-block core leaves a bowl 40 blocks in radius and about 18 deep, lined with basalt, blackstone and magma, with a pool of hot Corium at the bottom. Bigger cores leave bigger craters, up to 64 blocks.
  - **Blast:** everything living inside the crater's radius dies, and out to nearly twice that it is burnt and thrown. Leaves, plants and timber there are stripped, tree trunks are left standing as Charred Logs, grass is scorched to dirt and fires start.
  - **Blast-proof blocks:** the explosion does not break Shielding Concrete, Encased Reinforced Pipe, Shielded Glass, Shielded Hatches, obsidian, crying obsidian, reinforced deepslate, Blocks of Netherite, ancient debris or Respawn Anchors. They also shelter what is behind them. The blast travels in straight lines from the core, and a block, a mob or a player with a blast-proof block on that line is not carved, burnt, hurt or thrown, so a sealed containment building keeps the explosion inside it and only its inside is gutted. Every opening lets the blast out: a doorway, a pipe or shaft through the wall, an ordinary block in place of a blast-proof one, or a floor that is not blast-proof, through which the blast digs out the ground below. A line that only grazes the edge of a blast-proof block counts as stopped. The Corium falls from the core until it lands on something that is still there, so a Shielding Concrete floor under the vessel catches it. The fallout is not held in: it falls over the country downwind whatever stands round the core. The list is the block tag `criticalmass:blast_proof`, which a data pack can add to.
  - **Cloud:** a flash whites out the view, then a fireball rises on a stem and rolls into a mushroom cloud that stands for over a minute. The blast is heard when its wave arrives, later the further away you are.
  - **Fallout:** the explosion leaves a plume of fallout that the wind carries one way, about 90 blocks wide and 300 long for a 40-block crater. It reaches its full length over a minute and a half.
    - **In the air:** the dose rate is up to 900 mSv/min near the crater and falls to nothing at the plume's edge. It halves every 5 minutes. A roof overhead cuts it to three tenths, and a full protective outfit blocks most of the rest, since most of it is dust and not direct radiation. A Survey Meter reads it.
    - **On the ground:** Fallout Ash settles across the plume for three minutes, kills the grass it lands on and glows faintly green. It is contamination, so it gives a dose until it is cleared, and it does not decay.
    - **Clearing it:** every layer cleared leaves one Fallout Ash, and its contamination goes with it.
      - Breaking a layer by hand clears it, and so does a piston.
      - Flowing water washes it off, so one bucket clears the ground it spreads over and carries the Fallout Ash to the edge of the flow.
      - One of Create's Mechanical Ploughs on a moving contraption sweeps up every layer it passes through and puts the Fallout Ash in the contraption's storage. A train or a cart with ploughs across its front opens a road through a plume and brings the ash home.
    - **Water Cannon:** a turret that washes ash and contamination off everything round it and keeps none of it.
      - It takes a shaft from below, and its head turns with the shaft: once round in 15 seconds at 16 RPM, in under 4 seconds at 64 RPM. It costs 2 SU per RPM.
      - Water goes in by pipe on any side, or from a bucket. It holds 4,000 mB and uses 10 mB for each block it washes, up to 3 blocks a tick.
      - It washes the Fallout Ash and puts out the fire on top of everything within 24 blocks, from 48 blocks below it to 12 above. The reach is set on its value board, from 4 to 24.
      - It also washes the contamination a leak, a breach or corium has left on the top of what it reaches, and in the block just above. Each pass takes half of what is there, for the same 10 mB, and a spot with less than 0.1 left is clean. Contamination under a roof or inside a building is out of its reach and still needs Decontamination Compound.
      - Ash it washes off leaves no Fallout Ash. With nothing left in reach it stops spraying and uses no water.
      - On a Sable body it washes the ground under where the body is now, so a vehicle with a tank, a pump and a cannon aboard clears a plume as it goes.
      - Goggles show its state and its water.
      - Recipe: a Fluid Pipe over a Fluid Pipe between two Brass Sheets, over a Cogwheel between two Steel Plates.
    - **Irradiated Creeper:** a Creeper caught in the open under the plume, or standing on Fallout Ash, turns into an Irradiated Creeper within about half a minute.
      - It has 26 health against a Creeper's 20, is a little larger and slower, and fallout does not sicken it.
      - It is a radiation source, so a Survey Meter ticks faster as one comes near.
      - It crackles like a Geiger counter when it starts to swell. When it goes off it does a Creeper's damage and also throws Fallout Ash over about five blocks round it, and anyone close takes a dose and is dusted with contamination. A charged one reaches twice as far. With the `mobGriefing` rule off it leaves no ash.
      - It drops gunpowder and 1 to 3 Fallout Ash. Its spawn egg is in the Fallout section of the tab.
      - Its model is `art/blockbench/irradiated_creeper.bbmodel`, which also holds its skin and its glow texture. `art/tools/irradiated_creeper.py` reads that file, writes the model's parts as `client/entity/IrradiatedCreeperParts.java` and copies the two textures into the mod. Run with `fresh` it builds the Blockbench file again and repaints both textures, which are generated art in the style of the Creepers Refreshed resource pack, with none of that pack's pixels. The animation in `client/entity/IrradiatedCreeperModel.java` finds parts by the group names in the Blockbench file.
    - **Fallout Ash:** the item is weakly radioactive, a full stack about as strong as four fresh assemblies. It has three uses.
      - Shielding Concrete: in a Mixer, 4 Fallout Ash take the place of the iron nuggets, with 2 gravel and 250 mB of water, for 2 blocks. That is the concrete to bury the crater's corium under.
      - Strontium-90: reprocessing spent fuel also gives it (see Reprocessing under the fuel chain). A heated Mixer leaches 1 from 8 Fallout Ash and 500 mB of water, and leaves 250 mB of Contaminated Coolant, which goes into waste canisters like decontamination runoff. Strontium-90 is radioactive too, half as strong as a fresh assembly.
      - Radioisotope Cell: 2 Strontium-90 in a column over a Copper Sheet, with 3 Reactor Alloy Sheets down each side. Sealed, it gives no dose. It goes in a Station Battery.
    - **Wildlife:** animals and monsters caught in the open in the thick of it sicken and die.
    - **Commands:** `/criticalmass fallout <position> [radius]` starts a plume by itself and `/criticalmass fallout clear` ends every plume. The ash stays.
  - **What it leaves inside:** the blast does not take everything with it.
    - **Core Debris:** pieces of the core are thrown outward until they meet something that survived, so a sealed building keeps them in along its walls and floor, and an opening lets some out. A 40-block explosion throws 20. Each glows faintly and gives 8 mSv/min at one block, more than a spent assembly, lying there or carried. Break one with a pickaxe to take it up. A Shielded Cask takes four.
    - **Wreckage:** about one in eight of the plant's blocks that the blast destroys, this mod's and Create's, falls where it stood as Wreckage, up to 240 pieces, so the vessel leaves a heap. Hold a Welding Torch on a piece for 2 seconds to cut it up: it gives back a Steel Plate, sometimes Steel Nuggets and now and then a Reactor Alloy Sheet. A pickaxe clears it and saves nothing.
    - **Contaminated shell:** a quarter of the contamination lies at the centre with the Corium. The rest lands on the surfaces that stopped the blast, in 96 patches: the inside of a building's walls, floor and roof, or the bowl of an open crater. It gives a dose until each patch is cleaned with Decontamination Compound, which needs a tank within 4 blocks for the runoff.
    - **All clear:** the explosion is kept as a site. Once someone is near, the site is read every 5 seconds at the places the debris and the contamination landed and over the Corium. When every one of them reads under 1 mSv/min, the site is cleared and those near it are told. Removing the sources does it, and so does burying them deep enough in Shielding Concrete.
    - **Charred wood:** an axe strips a Charred Log, and the logs make Charred Planks, four from each. There are Charred Wood, the stripped forms, stairs, a slab, a fence, a fence gate, a door, a trapdoor, a pressure plate, a button, a sign and a hanging sign, crafted as vanilla's wood is. A Mechanical Saw treats them as Create treats any wood. They do not catch fire. There are no boats.
    - **Milestones:** seven advancements follow the work, at the end of the accident branch of the mod's tree: picking up something a meltdown left, taking up cooled Corium, cutting Wreckage, cleaning a contaminated surface, bolting a cask shut on Core Debris, the all clear, and making Charred Planks.
    - **Commands:** `/criticalmass recovery` lists each site with its worst reading and where it is. `/criticalmass decontaminate <from> <to>` clears the contamination in a box.
  - **Saved with the world:** an explosion that has begun is kept when the world is saved, so stopping the server during the fuse or the blast does not call it off. It waits while its chunk is not loaded and carries on when someone comes near.
  - **How to get one:** not while the protection is on. The automatic trip and the fuel's own feedback stop a loss of cooling and a sudden loss of xenon and boron well short of melting.
  - **Protection bypass:** a Dashboard Switch bound to the Reactor Controller is a protection bypass. While it is on, the reactor no longer trips on fuel failure, high temperature, high flux, a short period or lost flow. A SCRAM by hand still works, and so does the trip for a broken vessel. An alarm lamp can watch for it, and the Plant Screen lists it. Fit the switch with a glass cover.
  - **A real one:** at 70% power on a small station of five fuel columns, with the bypass on, every rod withdrawn, the xenon and boron gone and the primary pump lost, four of the five fuel columns melted within two seconds. That stopped the reaction, decay heat melted the fifth half a minute later, and the core went off.
  - **Commands:** `/criticalmass meltdown` melts the nearest core to test it. `/criticalmass melt <controller>`, `/criticalmass detonate <position> [radius]` and `/criticalmass bypass <controller> <seconds>` do the parts on their own.
- **Config:** `resources.growChartrine` turns growing Chartrine off, so coolant on lava only makes tuff, and `resources.chartrineCoolant` sets how many mB land on lava for each block, 250 as it comes. `accidents.meltdown` turns melting off, `accidents.coriumDepth` limits burn-through, and `accidents.coriumContamination` scales the release. `accidents.nuclearExplosion` turns the explosion off, so a total meltdown only leaves corium, and `accidents.explosionRadius` sets the crater of a 15-block core. `accidents.blastShelter` turned off makes blast-proof blocks only survive the explosion, with no shelter behind them. `accidents.fallout` turns the fallout off and `accidents.falloutHalfLifeMinutes` sets how fast the airborne part fades.
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
  - **Blades:** the blades wear while steam passes, by how far the governor is open: wide open all the time they last 10 hours. Each overspeed trip costs another 2%. Worn blades give less, down to three quarters of the turbine's capacity, and worn out they give nothing. Goggles show their condition once it is below 100%.
  - **Changing blades:** stop the turbine, by closing its steam valve or tripping it, and use Turbine Blades on any part of it. Each one gives back a quarter. Reaching in while it still turns costs 3 hearts.
- **Power out:** the turbine's shaft is the plant's output. Put Create machinery straight on it and the governor follows whatever load is there. A Clutch on the shaft drops the load, and dropping all of it at once trips the turbine. A Generator on the shaft turns it into electricity for the grid: see Electricity below.
- **Valves:** a Reinforced Valve or Create's Fluid Valve fits inline in any loop. The plain valve leaks under pressure like a plain pipe. Turning the shaft forwards opens it and backwards closes it; a Valve Handle on the shaft is the usual way to work it. On Create's valve a handle left at its default 45 degrees needs two turns, so set it to 90 degrees or more for one. While it is closed nothing circulates in that loop.
- **Relief Valve:** fits inline, and grows a flanged stub on each side a pipe joins. It senses the loop's peak pressure, vents through its front from 8 bar down to 6 bar, and gives a redstone signal while venting. Vented water and heat are logged.
- **Rupture:** any steam buffer above 12 bar ruptures and loses its contents (logged).
- **Condenser:** steam side front/back, cooling water top/bottom. It condenses only while cooling water flows. Condensers set end to end along their steam side work as one row of up to 8: the steam runs through the whole row, and the cooling water goes in above or below one block and comes out of the opposite side of whichever block has the pipe there.
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
  - Goggles on the tower show whether it is cooling and how much heat it rejects. A tower without a fan shows a vapour plume while it works.
  - The air leaving the top is as hot as the water. Above 50 °C it scalds anything standing in the fan's air stream, or just above a tower with no fan, and it hurts more the hotter the water is.
- **Boiler Heating Plate:** a full block that goes under a Create Fluid Tank used as a boiler: a copper plate on a steel frame with a coil of tubes under it. Run hot coolant through it by the sockets on its two ends. Coolant at 120 °C heats the boiler like a kindled Blaze Burner and at 200 °C like a seething one, and the coil shows it: dull when cold, bright copper when heating, glowing at full heat. A redstone signal turns the plate off.
- **Feedwater:** the way back from the Condenser to the exchanger is the feedwater train, and it is built from parts that are already there.
  - **Feed tank:** a Create Fluid Tank in the loop after the Condenser holds the reserve of water.
  - **Level control:** a Threshold Switch on that tank reads how full it is. Lead its signal to the Clutch on a make-up Mechanical Pump set against the tank, and the tank is topped up whenever it runs low. The make-up water can come straight from the world: a second Mechanical Pump with its suction pipe ending over the middle of three water blocks in a row never runs dry. Keep that pipe a block clear of the loop's own pipes, or put a bracket on the loop's pipe where the two pass, because a pipe laid beside an unbracketed one joins the loop as a branch and stops it.
  - **Feed pump:** the Circulation Pump after the tank. It cavitates on boiling water, so the Condenser has to condense all of the exhaust before the pump: keep cooling water on it.
  - **Feedwater heater:** a Heat Exchanger that the loop crosses twice, with the exhaust on its way to the Condenser through one side and the feedwater after the pump through the other. It warms the feedwater back up towards boiling with heat the Condenser would have thrown away, so more of the reactor's heat makes steam.
  - **Alarm:** a Dashboard Alarm lamp bound to a Circulation Pump lights while the pump cavitates, stands still or leaks at its seals.
- **Fluid Tanks in a loop:** a Create Fluid Tank of any size can sit inline in a loop as a surge tank, for example for feedwater, and the water in it keeps its temperature.
  - It needs the loop's two pipes on it. With more pipes on it, the loop leaves by the first one it finds, checking north, east, south, then west, so set make-up pumps directly against the tank.
  - Water that a Create pump adds arrives cold, and water it takes out takes its share of the heat.
- **Chartrine:** a yellow-green stone of square crystal plates that holds uranium, made to sit beside Create's Crimsite, Asurine, Ochrum and Veridium.
  - **Veins:** it lies in layered veins in the Overworld, made by Create's own layered ore feature, with bands of tuff, granite and diorite between its own. A vein is tried once in 80 chunks, between height -30 and 70. A scan of 576 chunks found four, each of 2,500 to 4,750 blocks of Chartrine.
  - **Crushing:** Crushing Wheels turn a block into Crushed Raw Uranium 12 times in 100, a Clay Ball half the time and Glowstone Dust 5 times in 100. Every Chartrine block crushes the same.
  - **Building blocks:** Cut, Polished Cut, Cut Bricks and Small Bricks, each with stairs, a slab and a wall, and Layered Chartrine and a Chartrine Pillar, which join their faces to their neighbours as Create's do. A stonecutter turns any of them into any other.
  - **Growing it:** Contaminated Coolant that leaves an open Fluid Pipe end over lava turns the lava to stone. The lava may be at the pipe's mouth or in the block or two under it. The coolant leaves the pipe 1 mB a tick, and a block forms for about every 250 mB that lands on lava. Break the stone and let lava flow back in for the next.
  - **Which stone:** the chance of Chartrine is the share of the coolant's contamination that is on the reactor's record (see Coolant clean-up), and the rest of the time it is tuff. Coolant straight from a failed core always makes Chartrine, watered down by half it makes it half the time, and coolant with no record makes only tuff. Watering coolant down gains nothing.
  - **With no lava under it** the coolant dirties the floor at the pipe's mouth as a leak of it does, so a pit that has filled with stone contaminates its hall until the stone is broken.
  - **In a pipe:** lava and Contaminated Coolant that meet inside a pipe leave tuff, as lava and water leave cobblestone.
  - **Ponder:** the scene opens from Chartrine and from the Contaminated Coolant Bucket. `/criticalmass taint <tank>` marks the coolant in a Fluid Tank as the reactor would, to test a generator.
- **Enrichment:** a Centrifuge turns 2 Uranium Concentrate into 1 Enriched Uranium and 1 Depleted Uranium. It is an open steel frame with a brass drum that spins on the shaft.
  - It takes its shaft from above or below, so centrifuges stack on one shaft. It costs 8 SU per RPM.
  - A stack is a cascade. Each centrifuge in it gets more Enriched Uranium from the same Concentrate for every other stage in the stack, up to four stages: 1.25 from a batch in two stages, 1.5 in three and 1.75 in four, with that much less Depleted Uranium. Part items carry over to the next batch. Each stage still has its own contents, so feed and empty each one.
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
- **Spent Fuel Rack:** an open steel tub with four cells. Use an assembly on it to stand it in a cell and an empty hand to lift the last one out; funnels and Mechanical Arms can do both, and an arm can pick a rack or a Shielded Cask as a target the way it picks the Service Port. Fill it with a bucket or a pipe. The water shows at its real level, so a rack that is running dry can be seen: its fuel then heats up and wears. Racks set side by side read as one pool grid, and a comparator reads how full a rack is.
- **Shielded Cask:** dry storage for four cooled assemblies, waste canisters, Used Resin Cartridges or pieces of Core Debris. Its lid is bolted down on four studs: use a wrench on the cask to unbolt it, and the lid rises up the studs; use the wrench again to bolt it down, whatever is in it. A redstone signal also holds the lid up, for funnels and Mechanical Arms. It takes and gives items only with the lid up, and it shields far less then (half the dose gets out, against 1% when sealed). A cask keeps its contents when broken and placed again.
- **Reprocessing:** a cooled Spent Fuel Assembly can be taken apart for what is left in it, with Create's machines.
  - **Shearing:** pass it under a Mechanical Saw four times, on a belt or along a row of four saws facing up. It comes out as 4 Sheared Spent Fuel. A saw leaves an assembly alone while it is still hot, so it has to cool in a Spent Fuel Rack first.
  - **Dissolving:** a heated Mixer turns 4 Sheared Spent Fuel and 1,000 mB of water into 4 Uranium Concentrate, 1 Strontium-90 and 400 mB of Contaminated Coolant.
  - **What comes back:** the Uranium Concentrate goes to the Centrifuge again, and is a quarter of the uranium a new assembly takes. The Strontium-90 is half of a Radioisotope Cell, so two spent assemblies make one cell without any fallout. The Contaminated Coolant goes into waste canisters. The frame and cladding are lost.
  - **Dose:** the pieces of one assembly give what a cooled spent assembly gives, 5 mSv/min for the four, so shield the line and keep the pieces moving.
- **Damaged fuel:** once cooled (in the spent fuel rack), a damaged assembly becomes a Sealed Waste Canister that keeps its lifecycle record. It is a sequenced assembly: deploy an Open Waste Canister onto the assembly, deploy a Sealing Gasket, then press it shut. The deployer leaves hot assemblies alone, and the half-sealed canister is as radioactive as the bare assembly. Canisters go in a Shielded Cask.
- **Fire and despawn:** assemblies and canisters resist fire and never despawn.
- **Placing canisters:** a Sealed Waste Canister can be placed as a block. It stays a radiation source where it stands, and breaking it (with a pickaxe) drops the canister with its record intact.
- **Other recipes:** every block and item has a survival recipe, or comes from the world or the plant: the ores and Raw Uranium, the Centrifuge's Enriched and Depleted Uranium, spent and damaged fuel, a Used Resin Cartridge, and what an accident leaves behind. A game test checks that list. JEI shows the recipes under Create's categories.
  - **Buckets:** Primary Coolant, Contaminated Coolant, Boric Acid and Fuel Oil each have a bucket of 1,000 mB. A Spout fills one and an Item Drain empties one, and a Fluid Tank takes one by hand in creative mode, which is Create's rule for every bucket. Emptied onto the ground a bucket spills: the liquid is lost, the empty bucket comes back and no liquid block is left. A spill dirties the ground as a leak does, 200 of contamination for Contaminated Coolant and 50 for Primary Coolant. Aimed at a tank or a machine that holds liquid, the bucket is not spilled. A dispenser pours a bucket into a tank it faces and spills it otherwise. A bucket of Contaminated Coolant gives 3 mSv/min at one block, as a tank of as much does, and the coolant loses the reactor's record. Steam has no bucket.
  - **Survey Meter:** an Electron Tube over a glass pane between two Brass Sheets, with a Steel Plate under it.
  - **Dosimeter:** a Steel Plate, Nether Quartz and a Brass Nugget, in any order.
  - **Protective outfit:** each piece is the leather piece of the same kind with a Sealing Gasket and a block of wool, in any order.
  - **Area Radiation Monitor:** a redstone torch over an Electron Tube between two Brass Sheets, with a Copper Sheet between two Steel Plates under them.
  - **Demineralizer:** a Brass Sheet between two Steel Plates, a block of glass between two Fluid Pipes, and a Fluid Pipe between two Steel Plates.
  - **Resin Cartridge:** charcoal or coal between two clay balls, with an Iron Sheet over and under it.

**Electricity.** The plant has its own electrical system. It does not use Forge Energy. What the generators make goes down the wires as they make it, and only a Station Battery stores any. Values live in `content/power/Power.java`.
- **Generator:** a block with a shaft through it. Sections in a line on one shaft are one machine, up to 8 sections.
  - Each section is rated 8,192 SU, which is 32.8 MW (1 SU is 4 kW).
  - It makes 50 Hz at 64 RPM, so set the turbine's rated speed to 64. Its terminals are at 20 kV.
  - Its load on the shaft is its load setting times its rating, limited to nine tenths of what the shaft has to spare. The sections share it evenly. If the shaft sags under it, it sheds load at once and takes it up again, so a weak turbine lowers its output and does not stall.
  - Off the grid it carries whatever is wired to it: see Off the grid below.
  - It is built for indoors: see Weather below.
- **Voltages:** the plant works at three. Generators make 20 kV, the grid is 220 kV and motors take 6 kV. Transformers join them.
  - Everything is built for its own voltage and stands 10% more. Above that it is lost: see Faults below.
  - A voltage reaches as far as wires, closed breakers and bus bars carry it. A transformer starts a new one on its other side.
  - **Voltage drop:** a loaded wire loses voltage along it, 0.2% a block at its full current, and a transformer loses 5% at its full 100 MW. A motor at full load at the end of two long runs of Power Wire is near its limit, so carry power far at a high voltage and step it down where it is used. Goggles, gauges and screens show the voltage that is really there.
- **Wires:** there are three, and each has a highest voltage and a highest current. Use one on a terminal, then on another, to string it between them. Generators, Electric Motors, Transformers, Circuit Breakers, Bus Bars, Grid Connections and Insulators have terminals.
  - **Power Wire:** copper, for 6 kV and 1 kA, which is 10 MW: one motor at full load.
  - **Power Cable:** thick and black, for 20 kV and 4 kA. That is 139 MW at 20 kV, enough for four generator sections, or 42 MW at 6 kV to feed a motor bus.
  - **Transmission Line:** bare and silver, for 220 kV and 1 kA, which is 381 MW.
  - A wire may be used below its voltage. Its current limit stays the same, so it carries less power there.
  - A wire reaches 24 blocks and a terminal holds 4 wires.
  - Sneak-use on a terminal cuts its last wire and gives it back. Sneak-use in the air lets go of a wire that has been started.
  - Breaking a block drops its wires.
- **Transformer:** has a tall high-voltage bushing and a short low-voltage one. Its ratio is set on its value box: 220 / 20 kV for the generator's step-up, 220 / 6 kV to start the plant from the grid, and 20 / 6 kV to run the plant from its own generator.
  - It works both ways and carries 100 MW. Transformers wired side by side share what passes.
  - **Tap changer:** the same value box raises or lowers the low side in six steps of 1.25%, up to 7.5% either way. Raise it to make up for a long feeder or to ride through dips on the grid. Two transformers in a row both raised to the top give more than the 10% that equipment stands.
  - Above its rating its oil heats: at 130% it has about 20 seconds. Goggles show both voltages and what it carries, and a comparator reads its temperature. Left alone it explodes.
- **Insulator:** stands on any solid face. It is a place to join wires, turn a corner or carry a run across a hall.
- **Bus Bar:** a copper bar on a stand-off. Up to 8 in a line are one bar, and each block of it takes 4 wires, so it is where feeders meet.
  - Goggles show whether it is live, its voltage and frequency and the load on its network. A comparator reads 15 while it is live.
- **Electric Motor:** turns electricity back into rotation, with a shaft on its front and a terminal box on top.
  - Its speed is set on its value box, up to 256 RPM either way, and it gives up to 2,048 SU at any speed.
  - It draws what its shaft load takes, plus 5% in losses and 16 kW for running idle. Turning a generator with a motor always costs more than it makes.
  - It runs on 6 kV, between 47 and 53 Hz. On less than 5.4 kV it runs on for a second and a half and then stands still. A redstone signal stops it.
  - **Starting surge:** for 2 seconds after it starts it draws up to three times its load. A feeder near its limit glows at every start, a breaker set too close trips, and a battery or a small generator asked to start several motors at once is overloaded. Start them one at a time.
- **Circuit Breaker:** has two terminals on top, the line side ringed in brass and the load side in copper. Use it to close or open it.
  - It closes onto a dead side freely. With both sides live it closes only when they are at the same voltage and within 0.2 Hz and 20 degrees of each other, and says which one stopped it.
  - A redstone signal trips it. A comparator reads 15 while it is closed.
  - **Overcurrent trip:** its value box sets the current it trips at, from 0.1 to 6 kA, 4 kA as built. Above that it trips, sooner the further above: in about 2 seconds at one and a half times, at once at eight times. Set it at or under the limit of the wire it feeds and it opens before the wire burns.
  - **Lockout:** a breaker tripped by its protection locks out. Use it once to reset it, then again to close it. A redstone trip does not lock it.
  - It can break a short circuit of up to 1,500 MW. Asked to break more, it fails and is lost. One transformer gives about 800 MW of fault, so three side by side onto one bus are too many for a single breaker behind it.
  - Goggles show the current through it against its setting.
  - The red lamp is lit while it is closed and the green one while it is open.
- **Disconnect Switch:** a blade between two posts, for a break you can see. It has no protection and is worked dead: open the breaker in line with it first.
  - Opened while current flows, or closed with both sides live, it draws an arc, is lost, and hurts whoever worked it.
- **Earthing Switch:** ties its wires to earth. Close one on a dead line before working on it. If anyone then makes the line live, that is a short circuit, and the breaker they closed trips at once, so the line cannot become live under your hands.
- **Station Battery:** a cabinet of cells for the 6 kV bus. It holds the bus up when everything else has gone.
  - While the bus is live it charges, drawing 128 SU. From flat it is full in about 7 minutes.
  - When its wires have no grid and no running generator on them it supplies the bus by itself, at 6 kV and 50 Hz, up to 512 SU, which is 2 MW. Full, it lasts 5 minutes at that load and longer at less.
  - Batteries on the same wires share the load evenly, so two carry 1,024 SU.
  - Asked for more than 512 SU it shuts down for 5 seconds. As soon as a generator or the grid is back it stops supplying and charges again.
  - A Radioisotope Cell charges it with no supply at all. Use the cell on the battery to fit it, and a wrench to take it out; a lit green cell on the door shows it is there. It puts in 32 SU all the time, which fills a flat battery in 80 minutes or carries a load of up to 32 SU for good. A battery that has run flat on its cell stays off its bus until it is back to 2% charge, about a minute and a half.
  - Goggles show its state and charge, and a comparator reads the charge.
- **Diesel Engine:** a rotation source for a standby set. It turns its shaft at 64 RPM with 4,096 SU, which is the speed a Generator needs.
  - It runs while it has a redstone signal, or after it is used by hand, which calls it and releases it. It cranks for 3 seconds before it turns.
  - It burns Fuel Oil from its own 4,000 mB tank, filled by pipe: 0.4 mB a second idling and 4 mB a second at full load, so a full tank lasts about 17 minutes flat out. Out of fuel, it stops.
  - **Fuel Oil** is mixed in a heated Basin from coal or charcoal and 500 mB of water.
- **Transfer Switch:** moves a bus from its normal supply to a standby one by itself. It has three terminals on top: normal and standby at the two ends, under the lamps of the same side, and the load in the middle.
  - It sits on the normal supply while that has a grid or a running generator behind it, at between 47 and 53 Hz and at least nine tenths of its voltage.
  - A second after the normal supply is lost it lets go of it and gives a redstone signal. Lead that signal to a Diesel Engine, directly or along redstone dust, and the engine starts.
  - Once the standby supply is live it moves the load onto it. When the normal supply has been back for 5 seconds it moves the load back and drops the signal, and the engine stops.
  - It is never on both supplies at once, so the two need not be in step. The bus is dead for the time between them unless a Station Battery holds it.
  - Goggles show which supply it is on and the voltage of each.
- **Surge Arrester:** a post that leads a lightning surge to earth. Wire one to a line and it takes every strike on anything joined to that line at the same voltage, with a flash and no damage. Goggles count the surges it has taken.
- **Grid Connection:** the outside grid. Outside thunderstorms it is always live at 220 kV and 50 Hz, takes whatever is delivered to it and supplies whatever is drawn from it, so a cold plant can run its pump motors from the grid before it makes any power. Goggles show which way the power is going.
- **Synchronising:** a running generator that is not on the grid runs at 50 Hz times its RPM over 64, plus its speed trim.
  - Raise and lower change the trim by 0.02 Hz, up to 1 Hz either way.
  - A generator carrying load by itself sags: 2 Hz from no load to full load. Trim it back up, and trim a second generator down to it before closing that one on.
  - While the two frequencies differ the phase angle between them turns, one full turn a second for each Hz apart. Close the breaker as the angle passes zero.
  - Once on the grid the generator is held at 50 Hz and starts unloaded. Raise and lower then change its load by 2%, and the Load Setting value box on the generator sets it directly.
- **Off the grid:** a generator with no grid on its wires runs by itself at its own frequency and makes exactly what the motors on its wires draw. This is how a backup set works: a Diesel Engine or any rotation source at 64 RPM, a Generator, a Transformer down to 6 kV, and a Transfer Switch or a breaker onto the bus.
  - A second generator joins the same bus through a breaker the way one joins the grid: matched in frequency and phase. The first one leads and the others follow it, and they share the load by their ratings.
  - If the leader stops, the next one takes over the bus.
  - A generator asked for more than its shaft can give for a second trips the breaker nearest it. With no breaker to trip it shuts down for 5 seconds.
- **Trips:** the breaker nearest a generator trips when the generator is started against wires that are already live, when it stops while following the grid or another generator, when its shaft speed puts it more than 1.5 Hz from what it follows, when it is overloaded off the grid, and when it carries load off the grid below 47.5 Hz or above 52.5 Hz for 2 seconds.
- **Where the power goes:** every quarter of a second the plant works out how much each wire and transformer carries, from what each generator makes and each motor draws. Where there are two ways round, each takes its share by how much it can carry.
- **Faults:** a mistake costs equipment.
  - **Burnt-out equipment:** anything lost to a fault is left as a smoking wreck where it stood. Use Copper Sheets and Steel Plates on it, 4 of each for a Transformer or a Generator section and 2 of each for anything else, and it stands again with its settings, unwired. Broken, a wreck gives one Steel Plate.
  - **Too much current:** a wire above its current heats and glows orange. At one and a half times its limit it burns through in about 8 seconds, at twice in about 3. It cools again in 20 seconds if the load comes off. A burnt wire is lost and starts a fire at each end and under its middle.
  - **Too much voltage:** a wire on more than it is built for flashes over at once and is lost. A motor, a generator or a transformer winding on more than its voltage explodes, is lost and sets fire to what is round it. A transformer wired backwards does this to itself.
  - **Short circuit:** a live line tied to earth draws all the current its sources can give, limited mostly by the transformers in the way. A breaker between trips at once. With no breaker to clear it, the wires burn, the transformer feeding it overheats within a second and explodes, and the Earthing Switch is lost. A generator feeding a short circuit off the grid shuts itself down for 5 seconds.
  - **Live work:** stringing or cutting a wire on a live terminal draws an arc: 4 hearts at 6 kV, 8 at 20 kV, and death at 220 kV. Open a breaker and work on the dead side.
  - A breaker protects only what is behind it. It will not close two voltages together, but closing it onto dead equipment of the wrong voltage is the operator's mistake to make.
  - In the server config, `electrical.fires` turns the fires off, `electrical.liveWorkHurts` turns the arcs off, and `electrical.blastsBreakBlocks` lets a blast break the blocks round it.
- **Wear:** four kinds of equipment age. Goggles show their condition once it is below 100%.
  - A Generator section ages while it carries load, by how much it carries: 8 hours at its full rating, 13 at six tenths. Each fault that shuts the generator down costs another 2%. At the end of its life the section burns out.
  - An Electric Motor loses 0.4% with every start and 1% each time it rides through low voltage. At the end of its life it burns out.
  - A Transformer ages while its oil is hot. Five minutes close to exploding uses it up, and a milder overload does the same more slowly. Then it burns out, without a blast.
  - A Circuit Breaker's contacts wear each time it breaks a short circuit, by up to a third, and what it can break falls with them, down to a tenth of 1,500 MW. Asked to break more than it can, it fails.
  - **Servicing:** use a Copper Sheet on the machine, anywhere but its value box, to give back a quarter of its condition. It must be dead: on a live machine you get the arc. Open its breaker, open a Disconnect Switch and close an Earthing Switch first.
  - A worn machine keeps its condition when it is broken and placed again, and its item shows it.
- **Weather:** rain and thunder reach the plant's electrics.
  - **Rain:** Generators, Electric Motors, Station Batteries and Transfer Switches are indoor equipment. Live under open sky in the rain, one flashes over to earth every so often: the breaker nearest it trips, and with no breaker on its feeder it is lost. Put a roof over it. Transformers, breakers, switches, bus bars, insulators, arresters and wires stand the weather.
  - **Lightning:** in a thunderstorm, wires under open sky draw lightning, each about once in twelve minutes of storm. A natural bolt that lands beside a wire counts as well.
  - A vanilla Lightning Rod within 12 blocks takes the strike and nothing happens to the line. A Surge Arrester on the line takes the surge.
  - With neither, the struck wire flashes over and is lost, every breaker on that line trips, and the nearest transformer, generator or motor on it within 32 blocks explodes.
  - **The grid in a storm:** every 30 to 60 seconds of thunder the grid is disturbed. Half the time its voltage dips, to between 84% and 97% for 3 to 10 seconds. Four times in ten its frequency swings up to half a hertz either way for 10 to 20 seconds. One time in ten it is lost altogether for 10 to 40 seconds.
  - When the grid is lost the breaker nearest the Grid Connection trips and locks out, so the plant is left running by itself if it can. When the grid is back, reset the breaker and close it in step, as for synchronising.
  - The Grid Connection shows what the grid is doing through goggles, and gauges and Display Links read its voltage and frequency.
  - In the server config, `electrical.weather` turns all of this off.
  - `/criticalmass grid <position> dip|swing|outage <seconds>` disturbs a Grid Connection and `normal` ends it. `/criticalmass strike <position>` puts lightning on the wire nearest a position.
- **Power orders:** set Largest Power Order on the Grid Connection's value box, in steps of 5 MW up to 300 MW. Zero means no orders.
  - Thirty seconds after the last order the grid asks for between 40% and 100% of that figure.
  - Hold the delivered power within 5% of the order for 20 seconds to meet it. An order not met within 2 minutes is missed.
  - A comparator reads how far through the hold the plant is, and goggles show the order and the tally of orders met and missed.
- **Control room:** a Dashboard Button bound to a generator raises or lowers it, and one bound to a breaker closes, opens or resets it. Gauges and Display Links read a generator's output, frequency, phase angle and load setting, a breaker's phase angle across its contacts and the current through it, a Grid Connection's order, delivered power and frequency, a motor's speed and the power it draws, a bus's voltage, frequency and load, and a transformer's low-side voltage, what it carries and its oil temperature.
- **Commands:** `/criticalmass generator <generator> <fraction>` sets a generator's load setting. `/criticalmass breaker <breaker> close`, `open` or `reset` works a breaker with the same checks as by hand. `/criticalmass wire <from> <terminal> <to> <terminal> <wire, cable or line>` strings a wire; a breaker's terminals are 0 for line and 1 for load, a transformer's are 0 for high and 1 for low, and everything else has terminal 0.

**Effects and sounds.** The plant has its own effects and its own sounds. The sounds are synthesised from square, saw and triangle waves and noise at a low sample rate, to sit with the green screens and the analog control room.
- **Arcs:** a flashover, a short circuit, a disconnect switch worked live and a hand on a live terminal each draw a curved blue-white bolt with a soft glow, a flare at each end and thin forks, which writhes for its half second and comes with a crackle.
- **Electrical explosions:** equipment lost to a fault goes with a flash, a fireball, a ring across the ground, a spray of sparks, black smoke that hangs for seconds, a bang and a shake of the view that fades with distance.
- **Wires:** a wire near its limit spits sparks along its length and sizzles before it burns.
- **Switchgear:** breakers close, open and trip with their own heavy sounds, and a relay clicks when a breaker's current first passes its setting. Switches thud. Stringing and cutting wire each has a sound.
- **Pumps:** a cavitating Circulation Pump rattles like gravel in a pipe.
- **Machines:** a live transformer hums at mains pitch, louder with load and sharper as its oil heats, and smokes once it is overheating. A generator drones at a pitch that follows its speed, and a motor whirrs.
- **Steam:** a Relief Valve throws a jet from its mouth while it vents. A burst pipe and a ruptured vessel throw a cloud. A tripped turbine vents upward. Water on corium boils off in steam and embers.
- **Reactor:** fuel channels at power give off a blue glow with motes drifting up through it, seen through the viewports, brighter with power. Overheating channels shed embers. The Reactor Controller thrums, louder and higher with neutron power. Spent fuel in a flooded rack glows blue in its water.
- **Meltdown:** a fuel column that melts slumps to the vessel floor with a heavy thud and a hiss, under smoke and embers. When corium burns through the floor, the vessel gives way with a tearing crack and a long roar in a burst of embers and black smoke. Corium sinking into the ground slumps the same way.
- **Cooling tower:** a tower that draws by itself, with no fan on top, sends up a plume that grows with the heat it rejects.
- **Radiation:** a Survey Meter in hand ticks like a Geiger counter. It ticks about once every two or three seconds with nothing to read and faster with the dose rate or contamination it reads, up to a crackle of about 50 a second, and the clicks come at random as a real counter's do. What its holder wears makes no difference to it. A Dosimeter in hand clicks with the dose its wearer takes and is silent with none. Above a rate of 10 the view picks up static, heavier as the rate climbs.
- **Grid:** a power order arriving, met or missed each plays a short tune.
- **Water Cannon:** a jet of water arcs from the nozzle to the ground it is washing, with spray and a puff of ash where it lands and a hiss while it runs. A fire it puts out goes with a short hiss of its own.
- **Tools:** a Welding Torch at work throws blue-white sparks and orange embers from a flickering arc and crackles while it is held, and a finished cut or repair ends with a clank and a sizzle. Decontamination Compound scrubs with a wet fizz in a cloud of pale green foam.
- **Control desk:** Dashboard Switches and Levers click, a glass cover flips open and shut, picking a machine with a part or a Plant Linker clicks like a relay, and binding to it chirps. A gauge fitted to a Pressurizer and a cell fitted to a Station Battery seat with a clunk.
- **Nuclear explosion:** the flash, fireball, stem, rolling cap, ground surge and shock rings of a mushroom cloud, seen from up to a thousand blocks away. Its sound is the one sound that is not synthesised.
- **Fallout:** inside the plume the fog turns thick and green, glowing green ash drifts down on the wind, the edges of the view darken to green and pulse, and a hollow wind plays. All of it thins under a roof. From outside, a low green mist hangs over the whole zone, and the ash on the ground shimmers at night.

**Radiation.** On by default with gentle values; everything below is in the server config (`radiation.*`).
- **Dose rate:** in mSv/min. A player receives, from every source within 48 blocks, its strength times the shielding transmission divided by (1 + distance²).
- **Sources:** the steam loop while a Heat Exchanger's tubes leak, running or cooling reactors (from fission and decay heat), spent and damaged assemblies, Sheared Spent Fuel, waste canisters, Strontium-90 and (weakly) fresh fuel and Fallout Ash, wherever they are: placed canisters, racks (flooded racks let 10% through), casks (1%), the service port, dropped items, player inventories and any block that holds items. Positions are world-space, so sources on a moving Sable body are measured where the body is now.
- **Fuel in storage and in machines:** a chest, barrel or hopper, a Create Depot, Basin or Mechanical Saw, and any other block whose items a hopper or funnel could reach, give the full dose of what is in them, on the ground or on a Sable body. The box shields nothing, so only a rack under water or a Shielded Cask makes fuel safe to keep. A shulker box or bundle is as strong as the fuel packed in it, whether carried, dropped or stored. A double chest is counted once. A change shows on a meter within a second.
- **Fuel on the move:** whatever carries fuel gives its dose too. A Mechanical Arm is a source while it has fuel in its grip. So is a minecart with a chest or hopper, a boat with a chest, a donkey or llama with chests, an item frame, an armour stand, and a mob that has picked fuel up. A Create contraption is a source at each chest, barrel or vault on it, wherever that storage has moved to, and at each of its tanks that holds Contaminated Coolant, so a train of spent fuel gives a dose along the line. A contraption built on a Sable body counts where the body has carried it. None of them shields what it carries. A loot chest or loot cart that nobody has opened yet is left alone and counts for nothing.
- **Liquid waste:** Contaminated Coolant standing in a Fluid Tank, a Basin, a Spout or any other block that holds fluid gives a dose: each 1,000 mB is as strong as one Sealed Waste Canister, and the tank shields none of it. A tank of several blocks is one source at its middle. Water and clean Primary Coolant give nothing. This mod's own coolant blocks keep their own rules, so in the plant it is still only the plain pipes that give a dose.
- **Uranium in hand:** Raw Uranium, Crushed Uranium, Uranium Concentrate and the ore blocks as items give 0.01 mSv/min each, Enriched Uranium and Ceramic Fuel Pellets 0.02 and Depleted Uranium 0.005, carried or dropped. A whole inventory of Raw Uranium gives less than the body recovers. A Survey Meter reads what its holder carries.
- **Uranium ore:** ore in the ground gives a weak dose within 5 blocks: 0.2 mSv/min at the block, falling with distance and cut by rock as any source is. It is far less than the body recovers, so it harms no one, and a Survey Meter ticks faster near a vein, through a few blocks of stone.
- **Shielding:** attenuation per block is 3.0 for Shielding Concrete and Encased Reinforced Pipe, 2.5 for a closed Shielded Hatch, 1.5 for Shielded Glass, 1.0 for vessel casing, 0.7 for water and 0.4 for other solid blocks. An open hatch is 0.05.
- **Dose and effects:** dose is kept on the player, never on an item.
  - Body dose recovers 30 mSv per minute.
  - Effects start at 250 mSv (hunger), 500 (weakness), 1,000 (nausea, weakness II) and 2,000 (wither). Death clears the body dose but not the lifetime total.
  - A player in creative or spectator takes none of it: no dose, no contamination on skin or suit, no sickness and no grain on the screen. A dose already stored is kept as it is, without recovering, and counts again in survival. The Survey Meter still reads and ticks, and a Dosimeter reads a rate of zero.
- **Survey Meter:** hold it to read the dose rate in the action bar, to two decimal places below 10. Use it to switch to contamination (around you and on you). It clicks with whichever it is reading.
- **Dosimeter:** hold it for your dose. Use it for a summary, and sneak-use to reset its reading (the lifetime dose stays).
- **Area Radiation Monitor:** a fixed instrument for a hall. It hangs on a wall and reads the dose rate where it hangs, once a second, the way a Survey Meter held there would.
  - **Dial and sound:** its needle sweeps from 0.01 to 100 mSv/min, with a tenfold step to each quarter of the dial. It ticks like a Survey Meter, faster as the rate climbs.
  - **Alarm level:** set on its value box, from 0.1 to 10 mSv/min in tenths on the first row and from 1 to 100 in whole numbers on the second. It starts at 10.
  - **In alarm:** at or over the level its lamp lights and a horn sounds. It gives a redstone signal to the blocks around it and through the block it hangs on, so it can close a Shielded Hatch or sound a Meltdown Siren. It stays in alarm until the rate is a tenth under the level. Use it to silence the horn until the alarm clears.
  - **Comparator:** reads the rate against the alarm level, from 0 to 14 below it and 15 in alarm.
  - **Control room:** the Plant Linker remembers a monitor. A Plant Screen wall linked to one shows and graphs its rate, a Dashboard Gauge shows it on a needle with the alarm level at the middle of the scale, a Dashboard Alarm lamp lights while it alarms, and a Display Link gives the rate as text.
  - **Goggles:** its state, its rate and its alarm level.
- **Contamination:**
  - A breach leaks coolant out of each broken casing block and leaves contamination there that never fades.
  - Standing in it gives dose and contaminates you.
  - Failed fuel turns the vessel coolant into Contaminated Coolant. It keeps its contamination as it goes round the loop and shares it with clean coolant it mixes with, and a Demineralizer takes it out (see Coolant clean-up).
- **Protective gear:** each piece blocks a quarter of 90% of contamination uptake and collects that contamination itself. A full set cuts direct dose by only 5%: it is not shielding.
- **Decontamination Compound:** use it on a contaminated area (3×3×3) or in the air (yourself and your gear). Outdoors a Water Cannon does the ground for you.
  - Each use puts 250 mB of Contaminated Coolant runoff into a tank within 4 blocks. Without such a tank nothing is cleaned.
  - Solidify the runoff into Sealed Waste Canisters by sequenced assembly: spout 1,000 mB into an Open Waste Canister (3 Steel Plates in a bucket shape), deploy a Sealing Gasket, then press it shut.
- `/criticalmass radiation <x y z>` lists every source reaching that point and the total. `/criticalmass contaminate <pos> <amount>` adds contamination for testing.

**Chunk unloading.** An unloaded reactor freezes. It keeps its exact state and does nothing until it reloads, and a vessel that is only partly loaded waits instead of tripping.

Game tests in `com.criticalmass.gametest` cover vessel validation, viewports, core heat, rod banks and their in-core display, tripping, port rules, pump stress, the exchanger, turbine capacity, a 10,000-tick conservation run of the full two-loop plant, steam phase and pressure, turbine admission, an end-to-end plant scenario (blocked start, rod motion and delayed heating, the turbine driving a machine, SCRAM and cooled shutdown), the control desk (desks joining into runs of five, parts working the reactor and a Redstone Link, settings applied, clamped and saved, the linker re-binding a part, desks running a whole plant, Signal Interfaces working the reactor from redstone pulses, and with Create: Loconautics present its colours by dye and its dashboards taking our parts), the screen wall (power from a shaft shared across it, linked text, and the reactor display with its trends and events), meltdown (a flooded melt held in the vessel, a dry melt burning through, concrete stopping corium), start-up checks, the power ramp and temperature feedback, the built cooling tower (airflow by construction, fan, water and heat accounting through a Create Fluid Tank in the loop), a Create pump injecting coolant through a spare port, the pressurizer over a Blaze Burner and coolant flashing, the turbine governor and overspeed trip, the turbine carrying Create machines (governor following the load, half the load lost, a clutch dropping all of it), the reinforced valve and Create's Fluid Valve, pipes (coolant held in pipes, hot coolant in plain pipes, plain pipes leaking under pressure and radiating, any pipe bursting from overpressure, heat carried through Create pumps, a Reactor Alloy Ingot reinforcing a placed pipe and valve), condenser, cooling tower, radiator, boiler plate, relief and rupture, turbine-trip handling, and the fuel lifecycle (burnup, overheating damage, service port round trips and refusals, waste retention, rack cooling, cask pickup and reload), recipe loading, the centrifuge (minimum speed, batches, automation), the damaged-fuel sequence keeping its record, ore drops, assembling fuel holders onto a Sable body without duplication, and radiation (distance and shielding, casks and flooded racks, breach contamination, cleanup runoff, contaminated coolant, protective gear, dosimeter, the Survey Meter's clicks, uranium ore and uranium items), shearing cooled spent fuel and dissolving it into Strontium-90, a rod lever working its bank, desk buttons working the Power Regulator, the Meltdown Siren on redstone, on its supports and following a linked reactor from warning to meltdown, the Power Regulator holding power on bank D, pump cavitation, a Heat Exchanger crossed twice as a feedwater heater, a Threshold Switch reading a feed tank in a loop, cleared ash leaving Fallout Ash, the Water Cannon on the ground and on a Sable body, and washing contamination off open ground but not from under a roof, and the Radioisotope Cell (`./gradlew runGameTestServer`).
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
`"slot": n` selects a hotbar slot, `"use": [x, y, z, "face"]` right-clicks that block face with the held item, `"useItem": true` uses the held item in the air, and `"inventory": true` logs every inventory item with its components, and `"tooltip": true` logs the held item's tooltip lines. With JEI in the dev runtime, `"jei": "<item id>"` opens JEI's recipes for that item and `"closeScreen": true` closes it. `"clicks": true` logs how many Survey Meter clicks have played since the last count.

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
