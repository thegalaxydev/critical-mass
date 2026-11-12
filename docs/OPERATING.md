# Operating a Critical Mass plant

This is the working manual: what each circuit does, the smallest plant that runs, how to wire a control room, and how to start, run, shut down and recover. Exact numbers are in the README.

## The three circuits

| Circuit | Carries | Route | Job |
| --- | --- | --- | --- |
| Primary | Primary coolant | Vessel outlet port, Circulation Pump, Heat Exchanger front and back, vessel inlet port | Takes heat out of the fuel. A Pressurizer on the vessel lets it run hotter than 350 °C |
| Steam | Water and steam | Heat Exchanger top and bottom, Steam Turbine, Condenser, Feedwater Tank, pump, back to the exchanger | Boils in the exchanger, turns the turbine, condenses and returns |
| Cooling | Water | Condenser cooling side, tower distributor, fill, basin, pump, back to the condenser | Throws the turbine's waste heat away |

The circuits never mix. Each one is a single pipe path with its own Circulation Pump. A closed valve, a missing pipe or a stopped pump stops that circuit only, and the plant feels it downstream: no primary flow trips the reactor, no steam flow starves the turbine, no cooling flow leaves the condenser hot and the steam loop pressure climbing to the relief valve.

Use Reinforced Pipe wherever the water is at or above 100 °C or is steam. Create's own pipes burst there.

## A minimal working plant

1. **Vessel.** A 5×5×5 casing shell. Inside, a 3×3 grid of channel columns, each 3 high: fuel, control and coolant channels. Every fuel column needs a coolant channel directly beside it, because a fuel column with none is not cooled at all and will overheat however much coolant flows. Every fuel column also needs a control channel directly beside it, because a rod only acts on the fuel columns it touches. Diagonal neighbours do not count. A layout that works is fuel, control, fuel in the first row, coolant, fuel, coolant in the second, and fuel, control, fuel in the third. Put the Reactor Controller, an outlet port and an inlet port in the walls and the Service Port in the top. The controller outlines anything wrong in red and names it. Through goggles it also lists, in orange, any fuel column with no coolant channel or no control channel beside it. These are warnings and do not stop the reactor.
2. **Fuel.** Put Fuel Assemblies in through the Service Port, one per fuel column.
3. **Rods.** A Control Rod Drive on the top face above each control column, with a shaft to its back.
4. **Primary loop.** Outlet port, pipe, Circulation Pump, Heat Exchanger (front and back), pipe, inlet port. Fill the vessel with primary coolant by bucket or through a port.
5. **Steam loop.** Heat Exchanger bottom to top, pipe to the Steam Turbine bottom, turbine top to the Condenser, Feedwater Tank, pump, back to the exchanger bottom. Fill the Feedwater Tank with water. Put a Relief Valve on the hot side.
6. **Cooling loop.** Condenser cooling side to a tower: Basin, two or more Packing blocks, Distributor on top, a Fan above that if you want more than natural draft. Basin outlet, pump, back to the condenser. Fill the basin with water.
   - **Size it for the core.** Nearly all of the core's heat ends up in the tower. Five fuel columns at full power make 750 HU/t, which takes four Packing blocks under a fan at 128 RPM. A tower that is too small shows as steam that never condenses and a relief valve that starts venting after a few minutes.
   - **Make-up water.** At that rate the tower evaporates about 370 mB a minute, enough to empty a full basin in about ten minutes. Pump water into a side of the basin with Create pipes. The basin sends loop water out of the first piped side it finds, checking north, east, south, then west, so put the make-up pipe on a later side than the loop outlet.
7. **Power.** A Generator on the turbine shaft. Everything else on that shaft shares what the generator leaves.

## Wiring a control room

Panels do nothing until they are bound. Use the Plant Linker on a machine, then on the panel.

| Panel | Bind to | Setting |
| --- | --- | --- |
| Gauge | Reactor Controller | Core temperature, with a high alarm at 70% |
| Gauge | Reactor Controller | Coolant temperature |
| Gauge | Reactor Controller | Rod bank A insertion |
| Gauge | Primary pump | Flow, with a low alarm |
| Gauge | Turbine | Speed |
| Gauge | Generator | Output |
| Gauge | Pressurizer | Pressure |
| Control Button | Reactor Controller | SCRAM, reset, bank A in, bank A out |
| Control Button | Turbine | Trip, reset |
| Control Button | Generator | Breaker open, breaker close |
| Annunciator | Reactor Controller | With an Alarm Siren beside it |

**Protective trips are yours to build.** A gauge in alarm gives a redstone signal, and a redstone pulse presses a button. Run redstone from the core temperature gauge to the SCRAM button and you have a high-temperature trip. Run it from the turbine speed gauge to the turbine trip button for a second overspeed guard. If you remove the wire, the trip is gone.

**Pumps and valves** use Create's parts: a clutch and lever on a pump shaft, a Valve Handle on a Reinforced Valve.

## Start-up

1. Bring outside rotation to all three pumps. A cold plant makes none of its own.
2. Check the annunciator is clear and the vessel is at least half full.
3. Start the primary pump. The controller will not let rods move until coolant is flowing.
4. Start the steam and cooling pumps and the tower fan.
5. Withdraw bank A a step at a time with the bank out button. Watch the rod stand up out of its drive and the fuel start to glow through a viewport. Power follows over about 3 seconds; temperatures follow over minutes.
6. When the steam loop boils, the turbine comes up to its rated speed. Close the generator breaker and raise its load in steps.
7. Once the generator is feeding the Pressurizer and the plant's own shafts, the outside supply can be disconnected.

## Running

- **More power:** withdraw rods, or draw more steam. Drawing steam cools the coolant, and cooler coolant raises reactor power by itself.
- **Hotter and more efficient:** raise the Pressurizer setpoint, then let the coolant temperature rise. Never let coolant pass its boiling point; the annunciator's boiling window means you are losing coolant.
- **Load changes:** change generator load in steps. Losing the whole load at once trips the turbine on overspeed; losing half of it does not.
- **Water:** the tower evaporates water, 1 mB for every 2,000 HU it rejects. Keep the basin topped up, or pipe make-up water into it.

## Shutdown

1. Lower the generator load and open the breaker.
2. Insert the rods with the bank in buttons, or press SCRAM.
3. **Keep the pumps and tower running.** Decay heat starts at 7% of full power and halves every minute. Stop cooling too soon and the fuel heats up with the rods fully in.
4. When the controller reads Cold, the pumps can stop.

## Faults and recovery

| Sign | Cause | Action |
| --- | --- | --- |
| Start blocked | No primary flow, or the vessel under half full | Start the pump, open the valve, fix the pipe, add coolant |
| Trip: low flow | Primary pump stopped for 2 seconds | Restore flow, then reset. The Emergency Coolant Tank injects by itself |
| Hot window, casing cracking | A fuel column without enough cooling | Insert rods, raise flow, add coolant channels next to that column. Weld the casing once it is cool |
| Boiling window | Coolant above its boiling point | Lower power, or restore the Pressurizer's power and setpoint. Refill the vessel |
| Turbine overspeed trip | Load lost too fast | Close the governor with a full redstone signal or the reset button, then reload in steps |
| Relief valve venting | Steam not being used or condensed | Check the condenser, the cooling loop and the tower fan |
| Tower distributor says covered | No fan and no open sky | Add a fan or open the top |
| A pipe burst | Hot water or steam in a Create pipe | Replace that run with Reinforced Pipe |
| Ruined Fuel Channel | Fuel melted | Keep the vessel flooded so the corium cools inside. Then replace the ruined channels and any failed casing |
| Corium outside the vessel | The floor failed | Stay off it. Flood it with water, wait for it to go dark, mine it and store it shielded. Decontaminate the area |

## Expanding safely

- **Bigger core:** fuel columns next to each other give more power each, and need coolant channels beside every one of them. Check the hottest fuel column, not the average.
- **More rods:** put drives in separate banks so you can move a few at a time.
- **More heat rejection first:** add fill blocks and a fan before raising power. A tower that cannot keep up shows as a hot condenser and a venting relief valve.
- **More pressure:** a hotter primary loop makes more steam but depends on the Pressurizer's electricity. Give it a second supply.
- **Containment:** Shielding Concrete under and around the vessel stops corium and cuts dose.

## The reference plant

`src/inspect/scenes/make_reactor_datapack.py <world>/datapacks/criticalmass_reactor_demo` writes a datapack. In that world, stand on open flat ground and run:

```
/reload
/function criticalmass_demo:reactor/build
```

It clears a 24×16 area south-east of you and builds a complete, bound and running plant: vessel with viewports and two rod drives, pressurizer, all three loops with a real tower, turbine and generator, a control wall, and a shielded fuel bay. Creative motors stand in for outside power.

The core is five fuel columns, each beside a coolant channel and a control rod, making 750 HU/t with the rods fully out. The tower has four Packing blocks under a fan at 128 RPM, and a creative tank pumps make-up water into the basin. At full power the plant settles within about four minutes: fuel near 30 °C, the tower rejecting about 615 HU/t, the generator at 64 FE/t, and no alarms. To operate it: the levers by the rod drives withdraw the rods, the lever on the controller's west side is its redstone signal, and the control wall's buttons SCRAM, reset and move bank A. Once you press a bank button or reset, the bank target takes over from the levers.
