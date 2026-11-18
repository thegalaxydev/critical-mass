# Operating a Critical Mass plant

This is the working manual: what each circuit does, the smallest plant that runs, how to wire a control room, and how to start, run, shut down and recover. Exact numbers are in the README.

## The three circuits

| Circuit | Carries | Route | Job |
| --- | --- | --- | --- |
| Primary | Primary coolant | Vessel outlet port, Circulation Pump, Heat Exchanger front and back, vessel inlet port | Takes heat out of the fuel. A Pressurizer on the vessel lets it run hotter than 350 °C |
| Steam | Water and steam | Heat Exchanger top and bottom, Steam Turbine, Condenser, a Fluid Tank for feedwater, pump, back to the exchanger | Boils in the exchanger, turns the turbine, condenses and returns |
| Cooling | Water | Condenser cooling side, a pipe into the side of the top of the tower fill, the fill, a pipe out from under it, pump, back to the condenser | Throws the turbine's waste heat away |

The circuits never mix. Each one is a single pipe path with its own Circulation Pump. A closed valve, a missing pipe or a stopped pump stops that circuit only, and the plant feels it downstream: no primary flow trips the reactor, no steam flow starves the turbine, no cooling flow leaves the condenser hot and the steam loop pressure climbing to the relief valve.

Plumb every circuit with Create's fluid pipes and Fluid Valves, then reinforce the runs that matter: use a Reactor Alloy Ingot on each pipe and valve that will carry steam, pressurized coolant, or primary coolant near where you work. Plain pipes leak under pressure, and primary coolant in them gives off radiation. Any pipe bursts if its steam passes 12 bar, so put a Relief Valve in every loop that carries steam.

## A minimal working plant

1. **Vessel.** A 5×5×5 casing shell. Inside, a 3×3 grid of channel columns, each 3 high: fuel, control and coolant channels. Every fuel column needs a coolant channel directly beside it, because a fuel column with none is not cooled at all and will overheat however much coolant flows. Every fuel column also needs a control channel directly beside it, because a rod only acts on the fuel columns it touches. Diagonal neighbours do not count. A layout that works is fuel, control, fuel in the first row, coolant, fuel, coolant in the second, and fuel, control, fuel in the third. Put the Reactor Controller, an outlet port and an inlet port in the walls and the Service Port in the top. The controller outlines anything wrong in red and names it. Through goggles it also lists, in orange, any fuel column with no coolant channel or no control channel beside it. These are warnings and do not stop the reactor.
2. **Fuel.** Put a Valve Handle on the Service Port and turn the hatch open, then put Fuel Assemblies in through it, one per fuel column. A Mechanical Arm can load and unload the port for you.
3. **Rods.** A Control Rod Drive on the top face above each control column, turned by a Cogwheel beside it. Drives set side by side turn each other.
4. **Primary loop.** Outlet port, pipe, Circulation Pump, Heat Exchanger (front and back), pipe, inlet port. Fill the vessel with primary coolant by bucket or through a port.
5. **Steam loop.** Heat Exchanger bottom to top, pipe into the back of the Steam Turbine, a pipe from under or over the turbine to the Condenser, a Create Fluid Tank for feedwater, pump, back to the exchanger bottom. Fill the tank with water. Put a Relief Valve on the hot side. The pump has to be fed water, so it goes after the Condenser and the tank. For a feedwater heater, lead the turbine's exhaust through one side of a Heat Exchanger on its way to the Condenser and the pump's discharge through the other side. For level control, put a Threshold Switch on the tank and lead its signal to the Clutch of a make-up pump.
6. **Cooling loop.** Condenser cooling side to a tower: two or more Packing blocks standing on a pipe, the hot pipe coming into the side of the top block, and an Encased Fan on top if you want more than natural draft. The pipe under the fill runs to the pump and back to the condenser. The fill has a shaft through it, so a fan facing up can be driven from underneath; the cooled water then leaves by a pipe on the bottom block's side. Fill the loop with water through the make-up line, or with buckets on the fill.
   - **Size it for the core.** Nearly all of the core's heat ends up in the tower. Five fuel columns at full power make 750 HU/t, which takes four Packing blocks under a fan at 128 RPM. A tower that is too small shows as steam that never condenses and a relief valve that starts venting after a few minutes.
   - **Make-up water.** At that rate the tower evaporates about 370 mB a minute. Pipe water into the side of the bottom Packing block with a Mechanical Pump and leave it running: the fill only takes what it has room for.
7. **Power.** The turbine shaft is the output. Put Create machinery on it, through a Clutch if you want to be able to drop the load. To sell power instead, set the turbine's rated speed to 64 and put Generator sections on the shaft. Run Power Cable from the generator to a Circuit Breaker, and from the breaker to the low side of a Transformer set to 220 / 20 kV. Run Transmission Line from its high side to a Grid Connection. With the generator at 50 Hz and the phase angle near zero, close the breaker, then raise the load in steps. For the plant's own pumps and fans, feed a Bus Bar at 6 kV from a second Transformer: 220 / 6 kV off the grid to start a cold plant, or 20 / 6 kV off the generator. Wire each Electric Motor to the bus with Power Wire, one motor to a wire. Give a backup generator its own breaker onto the same bus for when the grid is lost. Open the breaker before working on any wire: live terminals arc. To be sure of it, close an Earthing Switch on the dead side: anyone who closes the breaker then trips it at once. Set each feeder breaker's overcurrent trip to the limit of the wire it feeds, 1 kA for Power Wire.

## Wiring a control room

Readings go on a wall of Plant Screens. Screens placed beside and above each other join into one wall. They run on rotation, like a Display Board: each screen holds a cogwheel that meshes with its neighbours, so one turning cogwheel beside any screen drives the whole wall.

Build the wall at least 9 wide and 5 high, then use the Plant Linker on the Reactor Controller and on any screen. Smaller walls, from 3 wide and 2 high, show any other machine the same way: link one to the generator, the transformer, a bus or a pump and it graphs that machine's readings. The wall becomes the reactor display: trend graphs of neutron power, pressure, coolant temperature and xenon on the sides, and in the middle the status, every reading and a list of the latest events. Link the turbine the same way to add its speed and shaft load. If the wall is too small it tells you so.

A wall you have not linked shows Display Link text instead, three green lines to a block. Put a Create Display Link on a machine, point it at any screen of the wall, and pick what to show. Create's Display Board, Nixie Tubes and signs take the same links.

| Display Link on | Shows |
| --- | --- |
| Reactor Controller or a Reactor Signal Interface | Status summary: status, hottest fuel, coolant temperature, pressure, flow and heat output |
| Reactor Controller or a Reactor Signal Interface | Active alarms |
| Turbine | Speed |
| Primary pump | Flow |

The controller has one free face, so put Reactor Signal Interfaces in the vessel wall when you want more links to the same reactor. A Display Board only flips while it turns at 30 RPM or more; a cogwheel beside it drives it.

The control desk is a Reactor Alloy Dashboard with Dashboard parts on it. Sneak-use a part on its target, then use it on a desk. A wrench on a fitted part opens its settings.

| Part | Bind it to | What it does |
| --- | --- | --- |
| Dashboard Button | Reactor Controller | SCRAM, reset, rods in, rods out, or raise or lower the Power Regulator's setpoint, chosen in its settings |
| Dashboard Button | Steam Turbine | Trip or reset |
| Dashboard Button, Switch, Lever | A receiving Redstone Link | A one second pulse, on or off, or any strength from 0 to 15 |
| Dashboard Gauge | Any plant machine | One reading of that machine on a needle, chosen in its settings |
| Dashboard Alarm | Reactor Controller, or electrical equipment | Lights while an alarm is active, or while the equipment is in trouble. A new alarm flashes and sounds until you press it |
| Meltdown Siren | Reactor Controller, with the Plant Linker | Growls once the hottest fuel passes 1,100 °C, higher as it nears melting, and wails once fuel has melted or the vessel is breached. Use it to silence it. A redstone signal sounds it too |

For redstone instead of direct binding, set Reactor Signal Interfaces in the vessel wall and use a wrench on each to choose its job. Use one with an empty hand to see what it is set to.

**The protection can be bypassed.** Bind a Dashboard Switch to the Reactor Controller and it becomes a protection bypass: while it is on, none of the automatic trips act and only a SCRAM by hand stops the reactor. Give it a glass cover and an alarm lamp of its own. With the trips off, a core run without cooling melts in seconds, and a core melted in every column explodes.

**Protective trips are yours to build.** A comparator on the Reactor Controller reads the hottest fuel column from 0 when cold to 15 at the 1,100 °C trip, and a redstone pulse into a Signal Interface set to SCRAM trips the reactor. Compare that signal against one you set and feed the result into the interface, and you have an earlier high-temperature trip. An interface set to alarm gives a redstone signal while any alarm is active, so wiring it to the SCRAM interface trips on any alarm. If you remove the wire, the trip is gone.

**Pumps and valves** use Create's parts: a clutch and lever on a pump shaft, a Valve Handle on a Reinforced Valve.

## Start-up

A cold core with its rods in is thousands of pcm below critical. Starting it means taking that away slowly and watching the period.

1. Bring outside rotation to all three pumps. A cold plant makes none of its own.
2. Check the alarm lamps are dark and the vessel is at least half full.
3. Start the primary pump. The controller will not let rods move until coolant is flowing.
4. Start the steam and cooling pumps and the tower fan.
5. Press the rods out button on the desk a few times, then wait and read the reactivity. Each press moves the rods 0.5%. Near the ends of the stroke a press is worth a few pcm, near the middle up to 120.
6. As reactivity nears zero, slow down. Neutron power climbs on its own as the core approaches critical. Once reactivity is positive, stop pressing and let the period do the work. A period between 10 and 30 seconds is a comfortable climb. Under 3 seconds trips the reactor.
7. Power climbs through many decades before it makes any heat. When neutron power reaches a few percent the fuel warms, reactivity falls back toward zero and the power levels off by itself.
8. Raise power in small steps from there. Each step raises power until the hotter fuel and coolant cancel it.
9. When the steam loop boils, the turbine comes up to its rated speed. Put the load on its shaft a machine or two at a time.
10. Once the turbine shaft is driving the plant's own pumps and fans, the outside supply can be disconnected.

Never hold a lever to pull the rods. Continuous withdrawal goes straight through critical and trips on a short period, or worse.

## Running

- **Holding power:** a steady core sits at zero reactivity. Xenon builds over the first half hour at power and costs up to 2,500 pcm, so power sags unless you keep feeding the rods out. Fuel burn-up does the same more slowly.
- **More power:** withdraw rods, or draw more steam. Drawing steam cools the coolant, and cooler coolant raises reactor power by itself.
- **Less power:** insert rods. After a power cut xenon rises for several minutes before it falls, so expect to pull rods again later.
- **Cold water is reactivity:** anything that cools the primary coolant quickly, such as starting a big steam demand, adds reactivity fast. Do it in steps.
- **Hotter and more efficient:** raise the Pressurizer setpoint, then let the coolant temperature rise. Never let coolant pass its boiling point; the boiling alarm lamp means you are losing coolant.
- **Load changes:** change the shaft load in steps. Losing the whole load at once trips the turbine on overspeed; losing half of it does not.
- **Water:** the tower evaporates water, 1 mB for every 2,000 HU it rejects. Keep the tower's make-up line running.
- **Tower fan:** a fan that is too fast cools the steam loop below boiling at part power and the turbine stops. Slow it until the loop boils again.

## Shutdown

1. Take the load off the turbine shaft, a machine at a time or with a Clutch once it is light.
2. Insert the rods with the IN button, or press SCRAM. Power falls to a few percent at once and then fades over about 20 seconds as the delayed neutrons die away.
3. **Keep the pumps and tower running.** Decay heat starts at 7% of the power you were making and halves every minute. Stop cooling too soon and the fuel heats up with the rods fully in.
4. When the controller reads Cold, the pumps can stop.
5. If you plan to restart, do it within a couple of minutes or wait out the xenon peak.

## Faults and recovery

| Sign | Cause | Action |
| --- | --- | --- |
| Start blocked | No primary flow, or the vessel under half full | Start the pump, open the valve, fix the pipe, add coolant |
| Trip: low flow | Primary pump stopped for 2 seconds | Restore flow, then reset. Release your emergency injection line if the core is hot |
| Trip: short period | Reactivity added too fast | Reset, then withdraw in smaller steps and wait between them |
| Trip: high flux | Neutron power passed 118% | Find what cooled the coolant or moved the rods, then restart in steps |
| Power sags with the rods untouched | Xenon building, or fuel burning out | Withdraw rods a little at a time |
| Cannot reach critical after a shutdown | Xenon peak | Wait for it to decay, about an hour at worst |
| Hot window, casing cracking | A fuel column without enough cooling | Insert rods, raise flow, add coolant channels next to that column. Weld the casing once it is cool |
| Boiling window | Coolant above its boiling point | Lower power, or restore the Pressurizer's power and setpoint. Refill the vessel |
| Feed pump cavitating, flow down | Steam is reaching the pump: the Condenser is not condensing all of the exhaust | Start or speed up the cooling water pump, check the cooling tower has water and airflow, lower the load until the pump runs quiet |
| Feed tank running low | The Relief Valve has vented, or a plain pipe is leaking under steam pressure | Top up the tank, or let a Threshold Switch and a make-up pump do it. Reinforce the hot pipes |
| Turbine overspeed trip | Load lost too fast | Close the governor with a full redstone signal or the reset button, then reload in steps |
| Relief valve venting | Steam not being used or condensed | Check the condenser, the cooling loop and the tower fan |
| Tower fill says covered | No fan and no open sky | Add a fan or open the top |
| Tower fan turning but airflow stays at 25% | The fan is blowing down into the fill | Reverse its rotation so it blows up and out |
| A pipe burst | Steam passed 12 bar | Replace it, fit a Relief Valve in that loop, and find out why the steam was not being used or condensed |
| A loop keeps losing water, or the pipes give a dose | Plain pipes or valves under pressure, or holding primary coolant | Reinforce them with a Reactor Alloy Ingot |
| Ruined Fuel Channel | Fuel melted | Keep the vessel flooded so the corium cools inside. Then replace the ruined channels and any failed casing |
| Corium outside the vessel | The floor failed | Stay off it. Flood it with water, wait for it to go dark, mine it and store it shielded. Decontaminate the area |
| BYPASS lamp lit | A switch bound to the Reactor Controller is on, so the automatic trips are off | Switch it off unless you mean it. Until then only a SCRAM by hand will stop the reactor |
| Green fog and falling ash | You are in the fallout of a nuclear explosion | Get under a roof, which cuts the dose to three tenths, or walk out across the wind. Wear the protective outfit. Read the rate on a Survey Meter. Break the Fallout Ash to clean the ground |
| Contamination left outdoors by a leak, a breach or corium | It never fades by itself | Set a Water Cannon turning within 24 blocks with water on it. Each pass takes half, so let it turn until it stops spraying. Under a roof use Decontamination Compound |
| Ash still on the ground after the plume has gone | It is contamination and does not decay | Sweep it with ploughs or wash it into a stream to keep the Fallout Ash, or set a Water Cannon turning with a pipe of water on it to clear everything within 24 blocks. Mix the ash into Shielding Concrete or leach it for Strontium-90 |
| Every fuel column melted | Total meltdown | Run. Three seconds later the core goes off and leaves a crater 40 blocks in radius, with fire and blast to nearly twice that. Shielding Concrete, Shielded Glass, Shielded Hatches and obsidian are left standing and shelter whatever is behind them in a straight line from the core, so get behind a containment wall and away from its doors |

## Expanding safely

- **Bigger core:** fuel columns next to each other give more power each, and need coolant channels beside every one of them. Check the hottest fuel column, not the average.
- **More rods:** put drives in separate banks so you can move a few at a time.
- **Rod lever:** bind a Dashboard Lever to the controller and pick its bank in its settings. Middle holds, up withdraws, down inserts, and the further from the middle the faster.
- **Power Regulator:** put at least one drive in bank D, bring the reactor critical by hand with the other banks, then set a power on the controller's value box. The controller works bank D to hold it. Set it back to Off before moving bank D yourself.
- **Regulator from the desk:** two Dashboard Buttons bound to the controller, one set to raise and one to lower. Raise switches the regulator on at the power the reactor is at, and after that each press moves the setpoint 5%. Lower takes it down 5% and switches it off below the last step. A Gauge on the controller set to Regulator setpoint shows it.
- **More exchange area:** stack Heat Exchangers into a taller steam generator and set Condensers end to end. Each block adds its own area, so the two loops run closer in temperature.
- **More heat rejection first:** add fill blocks and a fan before raising power. A tower that cannot keep up shows as a hot condenser and a venting relief valve.
- **More pressure:** a hotter primary loop makes more steam but depends on the Blaze Burner under the Pressurizer. Keep it fed.
- **Containment:** Shielding Concrete under and around the vessel stops corium and cuts dose. It also outlasts a nuclear explosion and shelters what is behind it, so a containment sealed on every side, under the vessel too, keeps the blast in. Each doorway and each pipe through the wall lets a jet of it out.

## The reference plant

`src/inspect/scenes/make_facility.py <world>/datapacks/criticalmass_facility` writes a datapack that builds a whole generating station. In that world, stand on open flat ground with about 110 blocks clear to the south-east and run:

```
/reload
/function criticalmass_facility:facility/build
```

It clears and paves a 107 by 95 block site and builds the station on it, bound, wired and labelled. The station is left shut down on grid power: the grid breaker, the station supply breaker and the three plant feeder breakers are closed, the 6 kV station bus is live, and Electric Motors turn the pumps and the rod drives. The rods are in and the generator breakers are open, ready to start from the control room with the RODS OUT button or the rod levers. A fourth feeder, for the reprocessing hall, is left open until you want the hall.

It is a two-loop plant. One vessel feeds two primary loops, each with its own pump and steam generator, and each steam generator feeds its own turbine, generator, condenser and cooling tower. Everything with an A is on the north side and everything with a B on the south.

| Building | What is in it |
| --- | --- |
| Reactor building | A round Shielding Concrete building under a dome, 35 blocks across. The vessel stands in the middle, nine blocks square and nine high, with 25 fuel columns, 12 control rods and 12 coolant channels. A grated platform runs round its head, reached by stairs on the west side, with the twelve rod drives on top turned by one train of Cogwheels, and a polar crane above. The drives are in three banks: A is the west row, B the east row, and D, the bank the Power Regulator works, the row down the middle. A gantry on the platform carries the refuelling machine: two Mechanical Arms hanging over the vessel head, turned by an Electric Motor on the reactor feeder. One lifts spent fuel out of the Service Port into two flooded transfer racks beside it, and the other loads fresh assemblies stood on the Depot. Each loop leaves the vessel high on its own side, runs east to its steam generator, a stack of eight Heat Exchangers, and comes back low through its coolant pump and an isolation valve. The Pressurizer and the emergency injection line, held off by a lever, are on the vessel's west side. An airlock door leads south to the access corridor |
| Turbine hall | Two machine sets side by side on the deck. Steam comes through the wall at deck level, past a Relief Valve and the main steam stop valve, into the back of each turbine, which is set to 64 RPM. The shaft runs east through a Stressometer and a Speedometer to the Generator. Power Cable runs from each generator to its breaker and over a post to the 20 kV unit bus, and out through a bushing high in the east wall. Each exhaust drops through the deck to a row of eight Condensers in the basement, with the feedwater tank and the feed pump beside it. On its way down the exhaust passes through a feedwater heater, a Heat Exchanger that the feed line crosses on its way back to the steam generator, which gives the feedwater about 500 HU/t at 90% power. Beside each feedwater tank a Mechanical Pump stands ready to top it up from a tank of make-up water: a Threshold Switch on the feedwater tank holds the pump's Clutch open while the tank has more than 12,000 mB and lets it in below 10,000 mB. The two circulating water pumps stand by the north wall. Stairs by the north wall join the basement and the deck, and a crane spans the hall |
| Switchgear room | A caged room in the east end of the basement. The station supply breaker feeds the 6 kV station bus through the Transfer Switch next door. Two Station Batteries stand at the west end of the bus. Four feeder breakers leave the bus: one for the two coolant pumps and the rod drive motor, one for train A's feed, circulating and make-up pumps, one for train B's, and one for the reprocessing hall. Power Wire runs from each along insulators under the deck, through the north wall to the make-up pumps and through a penetration into the reactor building. The hall's feeder, in Power Cable because its motors draw more than a Power Wire carries, runs west under the deck, through the service building and across the yard |
| Standby diesel room | A second caged room across the passage: a Diesel Engine with a full tank of Fuel Oil, a Generator, a 20 / 6 kV Transformer and the Transfer Switch, with a line of redstone dust behind them carrying the switch's start signal to the engine. A door at the end of the passage leads out to the switchyard |
| Switchyard | Outside the east wall. The main Transformer, 220 / 20 kV, the grid breaker and the Grid Connection stand in a line, joined by Transmission Line. The unit Transformer, 20 / 6 kV, takes the plant's own supply off the 20 kV side and sends it back through the wall. A Surge Arrester stands on the 220 kV line and three Lightning Rods on the gantry over it |
| Cooling towers | Two concrete shells north of the turbine hall, each over a water basin. Inside each, three columns of twelve Packing blocks work by natural draught with no fan, and the make-up pump stands beside the last column under a small roof. A mast with a Lightning Rod covers each feeder that crosses the yard |
| Control room | Upstairs in the service building, behind a Shielded Glass window onto the turbine deck. A nine by five wall of Plant Screens shows the reactor, with a three by three wall for each turbine and each generator beside it. On the east wall a wall linked to the station bus draws the plant's electrics as a one-line diagram, and another shows the Grid Connection. One bench of 25 Reactor Alloy Dashboards runs the width of the room. From the west: alarm lamps for the station supply and the batteries, the controls for set A, the reactor, the controls for set B, grid power and its alarm, and open and close for the grid breaker and the station supply breaker. Each machine set has its synchroscope and frequency, generator breaker open and close, turbine trip and reset, turbine speed and power, generator lower and raise, and generator power and feed flow. The reactor has the two loop flows, pressure and coolant temperature, heat output and the alarm, a rod lever for all banks and one for bank D, rods in and out, lower and raise for the Power Regulator, its setpoint and neutron power, SCRAM and reset, and the trip bypass switch with the vessel level. A desk of four Dashboards against the west wall has open and close for the four feeder breakers, and a Meltdown Siren linked to the reactor hangs over it. The relay room is behind the screens |
| Service building | A second Meltdown Siren hangs high on the front wall, over the yard. Inside: entrance lobby, change room with Protective Suits, health physics, the stairs up, and the way through to the access corridor |
| Fuel building | Spent fuel pool with racks, dry casks, a cascade of three Centrifuges, and a waste vault under Shielded Hatches. A door in the south wall leads to the reprocessing hall |
| Reprocessing hall | South of the fuel building. A gallery runs along a Shielding Concrete wall with Shielded Glass windows, and the line stands behind it in the hot cell. From the east: a feed port, an Andesite Funnel set in the wall; four Mechanical Saws in a row on one line of Encased Chain Drives; a Depot; a Mechanical Arm that carries the pieces to the dissolver, a Basin over a Blaze Burner under a Mechanical Mixer, fed with water by a Mechanical Pump from a tank behind it. The dissolver pours into a second Basin set in the wall, where you take the Uranium Concentrate and Strontium-90 out from the gallery, and a pump moves the Contaminated Coolant on to the liquid waste tank beside it. West of the tank, on the gallery side of the wall, stands the canning bench: a Mechanical Belt under a Spout fed from the waste tank, a Deployer holding Sealing Gaskets and a Mechanical Press, with a Depot at the far end. An Open Waste Canister put on the east end of the belt is filled with 1,000 mB, gasketed, pressed shut and left on the Depot as a Sealed Waste Canister. Five Electric Motors on the hall's own feeder turn the saws, the mixer and arm, the dissolver's pumps, the bench and the pump that feeds its Spout. A Shielded Cask stands in the gallery and a door at the east end leads into the cell. Fuel on the line gives its dose from inside the machines: with four assemblies in work the gallery reads under 0.2 mSv/min behind the wall, up to 0.5 at the feed port and up to 1.1 in front of the product port, the two openings in it. The liquid waste gives a dose too. A Shielding Concrete pier on the gallery side covers the waste tank, which reads 0.5 through it when full, and the bench's full Spout reads about 1.1 to whoever works at it, so can the waste as it comes and do not stand at the bench |

The core makes 8,750 HU/t at 100% neutron power. No fuel column touches another, and each has rods and coolant channels on its sides.

To skip the start-up, run:

```
/function criticalmass_facility:facility/run
```

That brings the station up in three steps, each matched to what the steam side can take at that point, and takes about a minute:

1. The reactor is set to 30% power with matching xenon and the rods where they hold it. The turbines run up on the first steam.
2. Twenty seconds later it goes to 60%. Fifteen seconds after that both generator breakers close and the generators take 30% load.
3. It goes to 90%, and fifteen seconds later the generators go to 60% load.

From there the power sags slowly as xenon and burn-up build. Hold it with the rod buttons or a rod lever, or press REG RAISE once and let the Power Regulator hold it with bank D.

At 90% the reactor makes 7,875 HU/t and the primary coolant sits near 180 °C. Each steam generator takes half. A third of the heat leaves each turbine as power and the towers reject the rest, with the circulating water going out near 85 °C and coming back near 50 °C. Each generator is set to 60% of its rating, 19.7 MW, which is close to what its turbine can give at that power. The plant's own motors use about 3.7 MW, so the grid gets about 36 MW. That is 9% of what the station makes, where a real plant uses about 5%. Before the generators are on, the Grid Connection shows the 3.7 MW drawn from the grid. The reprocessing hall takes another 3 MW or so while its feeder is closed.

Things to try from here:
- Open a generator breaker from the control room, trim the generator with RAISE and LOWER, and close it again as the synchroscope passes zero.
- Let the regulator take the reactor. At power, press REG RAISE on the reactor desk: the setpoint gauge jumps to the power the reactor is at and bank D starts to move by itself. Press it again and watch the power follow up 5%. Push ROD LEVER D off the middle and the two fight, so press REG LOWER until the gauge reads zero first.
- Reprocess a spent assembly. Close FUEL FEEDER on the desk by the west wall, take a cooled Spent Fuel Assembly to the hall and use it on the feed port. It takes about a minute to pass the four saws, the arm drops the four pieces in the dissolver, and the Uranium Concentrate and Strontium-90 come out in the basin in the wall. Each assembly leaves 400 mB of Contaminated Coolant in the waste tank. Once it holds 1,000 mB, put an Open Waste Canister on the east end of the canning bench's belt and take the Sealed Waste Canister off the Depot at the other end; the Deployer starts with 64 gaskets. Open the feeder again when you are done: the saws take power whether they are cutting or not.
- Drain a feedwater tank below 10,000 mB with a bucket or a pump and watch the make-up pump's Clutch let in and fill it again.
- Refuel it. With the reactor shut down and cool, turn the Service Port's hatch open with its Valve Handle. The unloading arm lifts each spent assembly into the transfer racks, eight at a time, and the loading arm takes fresh ones off the Depot as fast as you stand them there. Carry the spent fuel on to the pool in the fuel building by hand.
- Open the reactor feeder. Both coolant pumps stop, the rod drives lose power and drop the rods, and the reactor trips on loss of flow.
- Open the grid breaker in the switchyard with the generators on. They carry the plant by themselves, off the grid. Closed again, they go back on the grid unloaded.
- Open the station supply breaker. The two Station Batteries hold the bus between them, so the pumps and rod drives keep running. One battery could not: the motors draw more than the 2 MW it gives. A second later the Transfer Switch lets go of the dead supply and starts the Diesel Engine, and three seconds after that the standby set takes the bus and the batteries charge again. Close the breaker and the switch goes back five seconds later and stops the engine.
- Run `/weather thunder` and stay a while. The grid dips, swings and now and then goes altogether. When it goes, the grid breaker trips, the generators carry the plant if they are on, and otherwise the batteries and the Diesel Engine do. Reset the grid breaker and close it again once the grid is back. Every motor is under a roof, so rain alone trips nothing.
- Break the Lightning Rods and the Surge Arrester in the switchyard and run `/criticalmass strike` on the line between the grid breaker and the main Transformer.
- Do the same with the batteries' wires cut, or the engine's tank empty. The bus goes dead, the pumps stop, the rod drives lose power and drop the rods, and the reactor trips on loss of flow.
