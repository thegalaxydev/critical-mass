# The Fuel Works

A factory that keeps a reactor fuelled. Raw stone and metal come in at one end and finished Fuel Assemblies leave at the other, with no hands on the line. It is built by `src/inspect/scenes/make_factory.py`, the way `make_facility.py` builds the generating station, and every drive in it is an Electric Motor on the works' own substation.

## Building it

`src/inspect/scenes/make_factory.py <world>/datapacks/criticalmass_factory` writes a datapack. In that world, stand on open flat ground with about 80 blocks clear to the east and 50 to the south and run:

```
/reload
/function criticalmass_factory:factory/build
```

It clears the site, builds the hall, the yard and the substation, strings the wires and closes the breakers. The line starts by itself and the first Fuel Assemblies reach the Fuel Store in about five minutes.

`make_factory.py scene <minutes> <views>` writes `factory.json` for the inspection harness instead, which builds the works, runs it for that long, reads the stores each minute and photographs the views named, or all of them.

## What it makes

One Fuel Assembly takes:

| Part | Made from | Raw material |
| --- | --- | --- |
| 8 Ceramic Fuel Pellets | 8 Enriched Uranium, from 16 Uranium Concentrate in single Centrifuges | 16 Crushed Raw Uranium washed, from about 133 Chartrine |
| 4 Fuel Cladding | 2 Reactor Alloy Sheets | |
| 1 Empty Fuel Assembly | 4 Reactor Alloy Sheets and 1 Sealing Gasket | |
| 6 Reactor Alloy Sheets in all | 6 Reactor Alloy Ingots, from 6 Steel Ingots, 3 Zinc Ingots and 3 Nether Quartz | 6 Iron Ingots and 6 charcoal for the steel |
| 1 Sealing Gasket | 1 Dried Kelp and half an Iron Nugget | |

The reference station has 25 fuel columns and an assembly lasts 100 minutes at full power, so it burns one assembly every 4 minutes. That is the rate the works is sized for, and it makes them faster than that: see the figures at the end.

The Chartrine also gives about 66 Clay Balls and 7 Glowstone Dust, which leave by the dispatch crates beside the mill, and the Centrifuges give 8 Depleted Uranium, which is kept in its own store. That much Depleted Uranium is 32 Shielding Concrete with 16 gravel.

## What comes in from outside

Two things cannot be made in a factory, so they have to arrive:

- **Chartrine.** It can be grown only from coolant that a fuel failure has contaminated, and a failure costs more uranium than its coolant gives back. It has to be mined.
- **Zinc.** Create has no renewable source of it.

Iron, Nether Quartz, charcoal, Dried Kelp and Iron Nuggets arrive too. Create can farm all of them, and a farm for each can replace its bin later without changing the line. In the built works each of the seven bins is a Creative Crate standing in for the mine, the farm or the railhead, with a sign on it saying what it holds. Process heat is from creative Blaze Burners and the wash water from a Creative Fluid Tank, kept the same way. Clay and glowstone leave through two more Creative Crates, which take whatever is put in them.

## The line

It runs west to east down one hall, in two rows either side of a gallery. The south row makes the pellets and the north row makes the metal parts, and they meet at the assembly loop in the east end.

1. **Mill.** Chartrine drops sixteen at a time between two Crushing Wheels. A belt carries what falls out past two filtered funnels, which take the clay and the glowstone off it, to a Basin under a Mechanical Mixer, where a pump adds water and the Crushed Raw Uranium is washed into Uranium Concentrate.
2. **Enrichment.** The concentrate passes a row of four Centrifuges on one line of Encased Chain Drives. Each takes what it needs off the belt and puts Enriched and Depleted Uranium on a second belt. A filtered funnel takes the Depleted Uranium to its store.
3. **Pelleting.** The Enriched Uranium drops into a heated Basin under a Mechanical Press. The Ceramic Fuel Pellets ride a belt east and then north across the aisle, under the gallery, to the pellet store beside the frame shop.
4. **Melt shop.** Iron and charcoal pass a pair of Brass Tunnels one of each at a time into a superheated mixer for steel, and the steel waits in a store. Two steel, one zinc and one quartz pass a second set of tunnels into a heated mixer for the alloy. The tunnels are set to synchronise, so they pass a set only when every part of it is waiting, and no mixer fills with the wrong thing.
5. **Rolling.** A press rolls the ingots into sheets, which go into the sheet store. One funnel feeds sheets from the store under a second press, which makes Fuel Cladding. Another feeds them to the frame shop. Each takes only what its end of the line uses.
6. **Frame shop.** A press compacts kelp and iron into gaskets, two at a time. Each pair waits at a Brass Tunnel until sixteen pellets are waiting at the tunnel beside it, and the two are let through together: that is the pellets for exactly the two frames those gaskets will make. A filtered funnel takes the pellets off into a small store and the gaskets go on to a Depot. Two Mechanical Arms set four sheets and a gasket into five Mechanical Crafters, which make the Empty Fuel Assembly and drop it on a belt.
7. **Assembly hall.** The frame goes round a loop of belt under a Deployer with cladding, two with pellets and a press. Each set of sixteen pellets rides a short belt onto the same loop, where a filtered funnel takes it off to a Depot inside the loop, and a Mechanical Arm there keeps the two pellet Deployers stocked. A second arm stocks the cladding. After four laps the frame is a Fuel Assembly, and a filtered funnel takes it off the loop.
8. **Fuel store.** Finished assemblies wait in a vault, with an Area Radiation Monitor on the wall behind it.

Because gaskets are let out only against pellets, the frame shop can never make frames faster than the pellet line can fill them. Without that the loop fills with frames waiting for pellets until no belt in it can move. A stop anywhere backs the line up to that point and no further: the stores fill, the tunnels stop passing sets, and it all starts again when the stop is cleared.

Five things about Create decided the layout, and are worth knowing before changing it:

- A Brass Tunnel forgets its mode when it joins another on being placed by a command, so the builder sets the mode afterwards.
- A belt that ends at a Basin throws items over it as often as into it. Every belt here ends under a funnel instead.
- A short feeder belt has to end under its tunnel with a solid block after it, or half of what it carries falls off the end.
- A Basin pours onto any belt or Depot that touches it, and picks one if there are two. Each Basin here has only one it can reach.
- A Mechanical Crafter drops what it makes whether or not there is room for it, so what has to be held back is an ingredient, not the product.

## Power

The works has its own substation in a fenced yard on the south side: a Grid Connection, a Surge Arrester, a Circuit Breaker and a 220 / 6 kV Transformer. The whole works draws about 59 MW, which is more than one Power Cable carries at 6 kV, so two cables in parallel run from the Transformer through bushings in the wall to a bus of four Bus Bars. Four feeder breakers stand in front of the bus, one each for the mill, enrichment, the melt shop and assembly, so a bay can be shut down by itself.

From each feeder a Power Cable climbs to the roof and runs from insulator to insulator over its bay. Power Wire drops from the insulators to the 44 Electric Motors, two to an insulator. The two motors on the Centrifuge row take about 8.6 MW each and are fed with Power Cable. The builder lays this out by itself and checks that no run is longer than a wire reaches and no terminal has more than four wires.

## How it looks

A long hall in the station's materials: light grey concrete with a cyan band, a dark plinth and pilasters, industrial iron windows along both sides and a raised roof light down the middle on girders. A grated gallery with railings runs the length of the hall over the aisle, reached by stairs at the west end, and looks down on both rows and on the loop. The east end is glazed. Each bay has its sign, every bin and store is labelled, and the loop has a hazard border on the floor.

Outside, the west end has two receiving doors under canopies, an apron, and stockpiles of Chartrine, raw iron and coal. The melt shop's stack stands against the north wall. The substation yard is on the south side under a gantry with Lightning Rods.

## What it does in a run

In one run of eight minutes from a cold start, read from the stores each minute:

- The Fuel Store was empty at four minutes, held 4 Fuel Assemblies at five and 7 at eight. Once the line is full that is about one a minute, four times what the reference station burns. Assemblies do not stack, so the store holds 20.
- The Depleted Uranium store gained about 12 a minute, and the sheet store about 15 sheets a minute more than the line used. A small vault holds 1,280 of either, so the Depleted Uranium wants emptying every hour and a half or so. When a store is full its part of the line stops and waits.
- The Transformer carried 58.9 MW and the bus stood at 5.74 kV. The two Crushing Wheels took about 4.3 MW each and each Centrifuge motor 8.6 MW.

The speeds, the store sizes and the split into four feeders are choices made for this build, not rules of the mod.

## Not in this build

Coolant, Boric Acid and Fuel Oil mixing, Resin Cartridges, waste canisters and Shielding Concrete are a second hall. Carrying the assemblies to the reactor's Service Port stays with the station's fuel building. The works stands by itself on its own Grid Connection: it is not yet set down beside the reference station and wired to it.
