# Operating a Critical Mass plant

This is the working manual: what each circuit does, the smallest plant that runs, how to wire a control room, and how to start, run, shut down and recover. Exact numbers are in the README.

## The three circuits

| Circuit | Carries | Route | Job |
| --- | --- | --- | --- |
| Primary | Primary coolant | Vessel outlet port, Circulation Pump, Heat Exchanger front and back, vessel inlet port | Takes heat out of the fuel. A Pressurizer on the vessel lets it run hotter than 350 °C |
| Steam | Water and steam | Heat Exchanger top and bottom, Steam Turbine, Condenser, Feedwater Tank, pump, back to the exchanger | Boils in the exchanger, turns the turbine, condenses and returns |
| Cooling | Water | Condenser cooling side, tower distributor, fill, basin, pump, back to the condenser | Throws the turbine's waste heat away |

The circuits never mix. Each one is a single pipe path with its own Circulation Pump. A closed valve, a missing pipe or a stopped pump stops that circuit only, and the plant feels it downstream: no primary flow trips the reactor, no steam flow starves the turbine, no cooling flow leaves the condenser hot and the steam loop pressure climbing to the relief valve.

Every circuit is plumbed with Create's own fluid pipes and Fluid Valves, hot or cold. A pipe only bursts when the steam in it passes 12 bar, so put a Relief Valve in any loop that carries steam.

## A minimal working plant

1. **Vessel.** A 5×5×5 casing shell. Inside, a 3×3 grid of channel columns, each 3 high: fuel, control and coolant channels. Every fuel column needs a coolant channel directly beside it, because a fuel column with none is not cooled at all and will overheat however much coolant flows. Every fuel column also needs a control channel directly beside it, because a rod only acts on the fuel columns it touches. Diagonal neighbours do not count. A layout that works is fuel, control, fuel in the first row, coolant, fuel, coolant in the second, and fuel, control, fuel in the third. Put the Reactor Controller, an outlet port and an inlet port in the walls and the Service Port in the top. The controller outlines anything wrong in red and names it. Through goggles it also lists, in orange, any fuel column with no coolant channel or no control channel beside it. These are warnings and do not stop the reactor.
2. **Fuel.** Put Fuel Assemblies in through the Service Port, one per fuel column.
3. **Rods.** A Control Rod Drive on the top face above each control column, with a shaft to its back.
4. **Primary loop.** Outlet port, pipe, Circulation Pump, Heat Exchanger (front and back), pipe, inlet port. Fill the vessel with primary coolant by bucket or through a port.
5. **Steam loop.** Heat Exchanger bottom to top, pipe to the Steam Turbine bottom, turbine top to the Condenser, Feedwater Tank, pump, back to the exchanger bottom. Fill the Feedwater Tank with water. Put a Relief Valve on the hot side.
6. **Cooling loop.** Condenser cooling side to a tower: Basin, two or more Packing blocks, Distributor on top, a Fan above that if you want more than natural draft. Basin outlet, pump, back to the condenser. Fill the basin with water.
   - **Size it for the core.** Nearly all of the core's heat ends up in the tower. Five fuel columns at full power make 750 HU/t, which takes four Packing blocks under a fan at 128 RPM. A tower that is too small shows as steam that never condenses and a relief valve that starts venting after a few minutes.
   - **Make-up water.** At that rate the tower evaporates about 370 mB a minute, enough to empty a full basin in about ten minutes. Pump water into a side of the basin with Create pipes. The basin sends loop water out of the first piped side it finds, checking north, east, south, then west, so put the make-up pipe on a later side than the loop outlet.
7. **Power.** The turbine shaft is the output. Put Create machinery on it, through a Clutch if you want to be able to drop the load.

## Wiring a control room

Readings go on a wall of Plant Screens. Screens placed beside and above each other join into one wall. They run on rotation, like a Display Board: turn a shaft into the back of any one screen and it shares with the rest.

Build the wall at least 9 wide and 5 high, then use the Plant Linker on the Reactor Controller and on any screen. The wall becomes the reactor display: trend graphs of neutron power, pressure, coolant temperature and xenon on the sides, and in the middle the status, every reading and a list of the latest events. Link the turbine the same way to add its speed and shaft load. If the wall is too small it tells you so.

A wall you have not linked shows Display Link text instead, three green lines to a block. Put a Create Display Link on a machine, point it at any screen of the wall, and pick what to show. Create's Display Board, Nixie Tubes and signs take the same links.

| Display Link on | Shows |
| --- | --- |
| Reactor Controller or a Reactor Signal Interface | Status summary: status, hottest fuel, coolant temperature, pressure, flow and heat output |
| Reactor Controller or a Reactor Signal Interface | Active alarms |
| Turbine | Speed |
| Primary pump | Flow |

The controller has one free face, so put Reactor Signal Interfaces in the vessel wall when you want more links to the same reactor. A Display Board only flips while it turns at 30 RPM or more; a cogwheel beside it drives it.

Controls are separate panels that you place and link yourself. Each is a thin wall panel, as deep as a Plant Screen, with its controls on the front. Mount them side by side or above each other to make a board, flush with the screens if you like. Use the Plant Linker on a machine and then on the panel that controls it. Then look at a control and use it.

| Panel | Link to | Controls |
| --- | --- | --- |
| SCRAM Panel | Reactor Controller | SCRAM, reset |
| Rod Control Panel | Reactor Controller | Rods in, rods out |
| Alarm Panel | Reactor Controller | Six alarm lamps and acknowledge. Put an Alarm Siren beside it |
| Turbine Panel | Steam Turbine | Trip, reset |
| Lever Panel | Nothing | A redstone signal you switch on and off |
| Dial Panel | Nothing | A redstone signal of the strength you set. Sneak-use raises it |

Name a panel in an anvil and the name shows on its face, which is how you tell one lever from the next.

**Protective trips are yours to build.** A comparator on the Reactor Controller reads the hottest fuel column from 0 when cold to 15 at the 1,100 °C trip, and a redstone pulse into the SCRAM Panel presses SCRAM. Compare that signal against one you set and feed the result into the SCRAM Panel, and you have an earlier high-temperature trip. The Alarm Panel gives a redstone signal on any new alarm. Panels ignore each other's signals, so run it through dust or a repeater on its way to the SCRAM Panel to trip on any alarm. If you remove the wire, the trip is gone.

**Pumps and valves** use Create's parts: a clutch and lever on a pump shaft, a Valve Handle on a Fluid Valve.

## Start-up

A cold core with its rods in is thousands of pcm below critical. Starting it means taking that away slowly and watching the period.

1. Bring outside rotation to all three pumps. A cold plant makes none of its own.
2. Check the alarm lamps are dark and the vessel is at least half full.
3. Start the primary pump. The controller will not let rods move until coolant is flowing.
4. Start the steam and cooling pumps and the tower fan.
5. Press OUT on the Rod Control Panel a few times, then wait and read the reactivity. Each press moves the rods 0.5%. Near the ends of the stroke a press is worth a few pcm, near the middle up to 120.
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
- **Water:** the tower evaporates water, 1 mB for every 2,000 HU it rejects. Keep the basin topped up, or pipe make-up water into it.
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
| Trip: low flow | Primary pump stopped for 2 seconds | Restore flow, then reset. The Emergency Coolant Tank injects by itself |
| Trip: short period | Reactivity added too fast | Reset, then withdraw in smaller steps and wait between them |
| Trip: high flux | Neutron power passed 118% | Find what cooled the coolant or moved the rods, then restart in steps |
| Power sags with the rods untouched | Xenon building, or fuel burning out | Withdraw rods a little at a time |
| Cannot reach critical after a shutdown | Xenon peak | Wait for it to decay, about an hour at worst |
| Hot window, casing cracking | A fuel column without enough cooling | Insert rods, raise flow, add coolant channels next to that column. Weld the casing once it is cool |
| Boiling window | Coolant above its boiling point | Lower power, or restore the Pressurizer's power and setpoint. Refill the vessel |
| Turbine overspeed trip | Load lost too fast | Close the governor with a full redstone signal or the reset button, then reload in steps |
| Relief valve venting | Steam not being used or condensed | Check the condenser, the cooling loop and the tower fan |
| Tower distributor says covered | No fan and no open sky | Add a fan or open the top |
| A pipe burst | Steam pressure passed 12 bar | Replace the pipe, fit a Relief Valve in that loop, and find out why the steam was not being used or condensed |
| Ruined Fuel Channel | Fuel melted | Keep the vessel flooded so the corium cools inside. Then replace the ruined channels and any failed casing |
| Corium outside the vessel | The floor failed | Stay off it. Flood it with water, wait for it to go dark, mine it and store it shielded. Decontaminate the area |

## Expanding safely

- **Bigger core:** fuel columns next to each other give more power each, and need coolant channels beside every one of them. Check the hottest fuel column, not the average.
- **More rods:** put drives in separate banks so you can move a few at a time.
- **More heat rejection first:** add fill blocks and a fan before raising power. A tower that cannot keep up shows as a hot condenser and a venting relief valve.
- **More pressure:** a hotter primary loop makes more steam but depends on the Blaze Burner under the Pressurizer. Keep it fed.
- **Containment:** Shielding Concrete under and around the vessel stops corium and cuts dose.

## The reference plant

`src/inspect/scenes/make_reactor_datapack.py <world>/datapacks/criticalmass_reactor_demo` writes a datapack. In that world, stand on open flat ground and run:

```
/reload
/function criticalmass_demo:reactor/build
```

It clears a 24×16 area south-east of you and builds a complete, bound plant: vessel with viewports and two rod drives, pressurizer, all three loops with a real tower, a turbine with a Mechanical Press on its shaft, a control room, and a shielded fuel bay. Creative motors stand in for outside power. The plant is left shut down with its pumps running and its rods in, ready for you to start with the OUT button on the Rod Control Panel.

The core is five fuel columns, each beside a coolant channel and a control rod, making 750 HU/t at 100% neutron power. With no boron it goes critical from cold with the rods between 55% and 60% inserted. The tower has four Packing blocks under a fan, and a creative tank pumps make-up water into the basin. The control room is a nine by five wall of Plant Screens linked to the reactor and the turbine, so it shows the reactor display. A creative motor turns a shaft through the wall behind the screens to power them. Under the screens runs a row of six panels: SCRAM, rods, alarms and turbine, each linked to its machine, plus a spare lever and dial. An Alarm Siren sits under the Alarm Panel.

To skip the start-up, run:

```
/function criticalmass_demo:reactor/run
```

That puts the reactor at 70% power with matching xenon and the rods where they hold it. From there the power sags slowly as xenon and burn-up build, and the Rod Control Panel holds it.
