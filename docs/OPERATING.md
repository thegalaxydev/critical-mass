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
5. **Steam loop.** Heat Exchanger bottom to top, pipe into the back of the Steam Turbine, a pipe from under or over the turbine to the Condenser, a Create Fluid Tank for feedwater, pump, back to the exchanger bottom. Fill the tank with water. Put a Relief Valve on the hot side.
6. **Cooling loop.** Condenser cooling side to a tower: two or more Packing blocks standing on a pipe, the hot pipe coming into the side of the top block, and an Encased Fan on top if you want more than natural draft. The pipe under the fill runs to the pump and back to the condenser. The fill has a shaft through it, so a fan facing up can be driven from underneath; the cooled water then leaves by a pipe on the bottom block's side. Fill the loop with water through the make-up line, or with buckets on the fill.
   - **Size it for the core.** Nearly all of the core's heat ends up in the tower. Five fuel columns at full power make 750 HU/t, which takes four Packing blocks under a fan at 128 RPM. A tower that is too small shows as steam that never condenses and a relief valve that starts venting after a few minutes.
   - **Make-up water.** At that rate the tower evaporates about 370 mB a minute. Pipe water into the side of the bottom Packing block with a Mechanical Pump and leave it running: the fill only takes what it has room for.
7. **Power.** The turbine shaft is the output. Put Create machinery on it, through a Clutch if you want to be able to drop the load. To sell power instead, set the turbine's rated speed to 64 and put Generator sections on the shaft. Run Power Cable from the generator to a Circuit Breaker, and from the breaker to the low side of a Transformer set to 220 / 20 kV. Run Transmission Line from its high side to a Grid Connection. With the generator at 50 Hz and the phase angle near zero, close the breaker, then raise the load in steps. For the plant's own pumps and fans, feed a Bus Bar at 6 kV from a second Transformer: 220 / 6 kV off the grid to start a cold plant, or 20 / 6 kV off the generator. Wire each Electric Motor to the bus with Power Wire, one motor to a wire. Give a backup generator its own breaker onto the same bus for when the grid is lost. Open the breaker before working on any wire: live terminals arc.

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
| Dashboard Button | Reactor Controller | SCRAM, reset, rods in or rods out, chosen in its settings |
| Dashboard Button | Steam Turbine | Trip or reset |
| Dashboard Button, Switch, Lever | A receiving Redstone Link | A one second pulse, on or off, or any strength from 0 to 15 |
| Dashboard Gauge | Any plant machine | One reading of that machine on a needle, chosen in its settings |
| Dashboard Alarm | Reactor Controller | Lights while an alarm is active. A new alarm flashes and sounds until you press it |

For redstone instead of direct binding, set Reactor Signal Interfaces in the vessel wall and use a wrench on each to choose its job. Use one with an empty hand to see what it is set to.

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
| Turbine overspeed trip | Load lost too fast | Close the governor with a full redstone signal or the reset button, then reload in steps |
| Relief valve venting | Steam not being used or condensed | Check the condenser, the cooling loop and the tower fan |
| Tower fill says covered | No fan and no open sky | Add a fan or open the top |
| Tower fan turning but airflow stays at 25% | The fan is blowing down into the fill | Reverse its rotation so it blows up and out |
| A pipe burst | Steam passed 12 bar | Replace it, fit a Relief Valve in that loop, and find out why the steam was not being used or condensed |
| A loop keeps losing water, or the pipes give a dose | Plain pipes or valves under pressure, or holding primary coolant | Reinforce them with a Reactor Alloy Ingot |
| Ruined Fuel Channel | Fuel melted | Keep the vessel flooded so the corium cools inside. Then replace the ruined channels and any failed casing |
| Corium outside the vessel | The floor failed | Stay off it. Flood it with water, wait for it to go dark, mine it and store it shielded. Decontaminate the area |

## Expanding safely

- **Bigger core:** fuel columns next to each other give more power each, and need coolant channels beside every one of them. Check the hottest fuel column, not the average.
- **More rods:** put drives in separate banks so you can move a few at a time.
- **More exchange area:** stack Heat Exchangers into a taller steam generator and set Condensers end to end. Each block adds its own area, so the two loops run closer in temperature.
- **More heat rejection first:** add fill blocks and a fan before raising power. A tower that cannot keep up shows as a hot condenser and a venting relief valve.
- **More pressure:** a hotter primary loop makes more steam but depends on the Blaze Burner under the Pressurizer. Keep it fed.
- **Containment:** Shielding Concrete under and around the vessel stops corium and cuts dose.

## The reference plant

`src/inspect/scenes/make_facility.py <world>/datapacks/criticalmass_facility` writes a datapack that builds a whole generating station. In that world, stand on open flat ground with about 80 blocks clear to the south-east and run:

```
/reload
/function criticalmass_facility:facility/build
```

It clears and paves a 75 by 71 block site and builds the station on it, bound, wired and labelled. The station is left shut down on grid power: the grid breaker and the station supply breaker are closed, the 6 kV station bus is live, and Electric Motors turn the pumps, the tower fan and the rod drives. The rods are in and the generator breaker is open, ready to start from the control room with the RODS OUT button.

| Building | What is in it |
| --- | --- |
| Containment | A round Shielding Concrete building under a dome. The vessel stands in the middle with a service gantry round its head, two rod drives on top turned by one train of Cogwheels, and a polar crane above. The primary loop runs round the east side: pump, a steam generator of three stacked Heat Exchangers, and an isolation valve. The Pressurizer stands at the vessel's corner and the emergency injection line on its west side, held off by a lever. An airlock door leads south to the access corridor |
| Turbine hall | Steam comes through the wall at deck level, past a Relief Valve and the main steam stop valve, into the back of the turbine, which is set to 64 RPM. The shaft runs east through a Stressometer and a Speedometer to the Generator. Power Cable runs from the generator to the generator breaker and the 20 kV unit bus on the deck, and out through a bushing in the east wall. The exhaust drops through the deck to a row of two Condensers in the basement, with the feedwater tank and the feed pump beside it |
| Switchyard | Outside the east wall. The main Transformer, 220 / 20 kV, the grid breaker and the Grid Connection stand in a line, joined by Transmission Line. The unit Transformer, 20 / 6 kV, takes the plant's own supply off the 20 kV side and sends it back through the wall |
| Station service | In the turbine hall basement. The station supply breaker feeds the 6 kV station bus. Power Wire runs from the bus along insulators under the deck to the feed and circulating pump motors, through the north wall to the tower fan and make-up pump, and through a containment penetration to the primary pump and the rod drive motor. A standby generator with its own transformer and breaker stands in the south-east corner, turning and ready, with its breaker open |
| Cooling tower | Four Packing blocks under an Encased Fan, inside a concrete shell with a water basin. The make-up line stands beside the fill. The circulating water pump is in the turbine hall basement |
| Control room | Upstairs, behind a Shielded Glass window onto the turbine deck. A nine by five wall of Plant Screens linked to the reactor and the turbine, with a three by three wall each side for the generator and the Grid Connection. Eleven Reactor Alloy Dashboards: the synchroscope and frequency, generator breaker open and close, turbine trip and reset, turbine speed and power, pressure and coolant temperature, heat output and the alarm, rods in and out, SCRAM and reset, primary and feed flow, generator lower and raise, and generator and grid power |
| Service building | Entrance lobby, change room with Protective Suits, and the way through to the access corridor |
| Fuel building | Spent fuel pool with racks, dry casks, a cascade of three Centrifuges, and a waste vault under Shielded Hatches |

The core is five fuel columns, each beside a coolant channel and a control rod, making 750 HU/t at 100% neutron power. With no boron it goes critical from cold with the rods between 55% and 60% inserted.

To skip the start-up, run:

```
/function criticalmass_facility:facility/run
```

That puts the reactor at 70% power with matching xenon and the rods where they hold it. From there the power sags slowly as xenon and burn-up build, and the rod buttons hold it. Thirty-five seconds later, once the turbine is up to speed, the generator breaker closes and the generator takes load.

The station is small. The generator is set to 10% of its rating, 3.3 MW, which is about what its turbine can give, and the plant's own motors use about 1.8 MW of that, so the grid gets the rest. Before the generator is on, the Grid Connection shows that 1.8 MW drawn from the grid.

Things to try from here:
- Open the generator breaker from the control room, trim the generator with RAISE and LOWER, and close it again as the synchroscope passes zero.
- Open the grid breaker in the switchyard with the generator on. The generator carries the plant by itself, off the grid. Closed again, it goes back on the grid unloaded.
- Open the station supply breaker. The station bus goes dead, the pumps stop, the rod drives lose power and drop the rods, and the reactor trips on loss of flow. Close the standby breaker to bring the bus back.
