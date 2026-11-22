# The Fuel Works

A factory that keeps a reactor fuelled and supplied. Raw stone and metal come in at one end of the main hall and finished Fuel Assemblies leave at the other, with no hands on the line. A lower wing on the south side makes what a running plant uses up: Primary Coolant, Boric Acid, Fuel Oil, Shielding Concrete, Resin Cartridges and Open Waste Canisters. It is built by `src/inspect/scenes/make_factory.py`, the way `make_facility.py` builds the generating station, and every drive in it is an Electric Motor on the works' own substation.

## Building it

`src/inspect/scenes/make_factory.py <world>/datapacks/criticalmass_factory` writes a datapack. In that world, stand on open flat ground with about 80 blocks clear to the east and 70 to the south and run:

```
/reload
/function criticalmass_factory:factory/build
```

It clears the site, builds the hall, the wing, the tank farm, the yard and the substation, strings the wires and closes the breakers, one feeder at a time. The line starts by itself and the first Fuel Assemblies reach the Fuel Store in about five minutes.

`make_factory.py scene <minutes> <views>` writes `factory.json` for the inspection harness instead, which builds the works, runs it for that long, reads the stores and tanks each minute and photographs the views named, or all of them.

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

The Chartrine also gives about 66 Clay Balls and 7 Glowstone Dust, and the Centrifuges give 8 Depleted Uranium. The wing uses the clay and the Depleted Uranium, and what it makes is:

| Product | Made from | Where that comes from |
| --- | --- | --- |
| 1,000 mB Primary Coolant | 1 Lapis Lazuli and 1,000 mB water | Lapis bin |
| 1,000 mB Boric Acid | 1 Borax and 1,000 mB water, heated | Scoria crushed in the wing, about four to a Borax |
| 500 mB Fuel Oil | 1 coal and 500 mB water, heated | Coal bin |
| 4 Shielding Concrete | 1 Depleted Uranium, 2 gravel and 250 mB water | The Centrifuges, and a gravel bin |
| 1 Resin Cartridge | 2 Iron Sheets, 2 Clay Balls and 1 coal | An iron bin, the mill's clay, and a coal bin |
| 1 Open Waste Canister | 3 Steel Plates | Steel Ingots from the melt shop |

## What comes in from outside

Two things cannot be made in a factory, so they have to arrive:

- **Chartrine.** It can be grown only from coolant that a fuel failure has contaminated, and a failure costs more uranium than its coolant gives back. It has to be mined.
- **Zinc.** Create has no renewable source of it.

Iron, Nether Quartz, charcoal, Dried Kelp, Iron Nuggets, Lapis Lazuli, Scoria, gravel and coal arrive too. Create can farm or make most of them, and a farm for each can replace its bin later without changing the line. In the built works each of the thirteen bins is a Creative Crate standing in for the mine, the farm or the railhead, with a sign on it saying what it holds. Process heat is from creative Blaze Burners and the water from Creative Fluid Tanks, kept the same way. Glowstone, and the clay the wing has no room for, leave through two more Creative Crates, which take whatever is put in them.

## The line

It runs west to east down one hall, in two rows either side of a gallery. The south row makes the pellets and the north row makes the metal parts, and they meet at the assembly loop in the east end.

1. **Mill.** Chartrine drops sixteen at a time between two Crushing Wheels. A belt carries what falls out past three filtered funnels to a Basin under a Mechanical Mixer, where a pump adds water and the Crushed Raw Uranium is washed into Uranium Concentrate. The first funnel takes clay into the Clay Store, the second takes the glowstone, and the third takes the clay that passes the first when the store is full.
2. **Enrichment.** The concentrate passes a row of four Centrifuges on one line of Encased Chain Drives. Each takes what it needs off the belt and puts Enriched and Depleted Uranium on a second belt. A filtered funnel takes the Depleted Uranium to its store.
3. **Pelleting.** The Enriched Uranium drops into a heated Basin under a Mechanical Press. The Ceramic Fuel Pellets ride a belt east and then north across the aisle, under the gallery, to the pellet store beside the frame shop.
4. **Melt shop.** Iron and charcoal pass a pair of Brass Tunnels one of each at a time into a superheated mixer for steel, and the steel waits in a store. Two steel, one zinc and one quartz pass a second set of tunnels into a heated mixer for the alloy. The tunnels are set to synchronise, so they pass a set only when every part of it is waiting, and no mixer fills with the wrong thing.
5. **Rolling.** A press rolls the ingots into sheets, which go into the sheet store. One funnel feeds sheets from the store under a second press, which makes Fuel Cladding. Another feeds them to the frame shop. Each takes only what its end of the line uses.
6. **Frame shop.** A press compacts kelp and iron into gaskets, two at a time. Each pair waits at a Brass Tunnel until sixteen pellets are waiting at the tunnel beside it, and the two are let through together: that is the pellets for exactly the two frames those gaskets will make. A filtered funnel takes the pellets off into a small store and the gaskets go on to a Depot. Two Mechanical Arms set four sheets and a gasket into five Mechanical Crafters, which make the Empty Fuel Assembly and drop it on a belt.
7. **Assembly hall.** The frame goes round a loop of belt under a Deployer with cladding, two with pellets and a press. Each set of sixteen pellets rides a short belt onto the same loop, where a filtered funnel takes it off to a Depot inside the loop, and a Mechanical Arm there keeps the two pellet Deployers stocked. A second arm stocks the cladding. After four laps the frame is a Fuel Assembly, and a filtered funnel takes it off the loop.
8. **Fuel store.** Finished assemblies wait in a vault, with an Area Radiation Monitor on the wall behind it.

Because gaskets are let out only against pellets, the frame shop can never make frames faster than the pellet line can fill them. Without that the loop fills with frames waiting for pellets until no belt in it can move. A stop anywhere backs the line up to that point and no further: the stores fill, the tunnels stop passing sets, and it all starts again when the stop is cleared.

## The chemistry wing

The wing is a lower hall against the south wall of the main one, with an aisle down the middle. Three things reach it from the main hall through openings in the shared wall: clay on a belt from the Clay Store, Depleted Uranium on a belt from its store, and steel on a conveyor set in the floor, which starts under the Steel Store, crosses the main aisle and runs under the enrichment row.

North of the wing's aisle are three shops that make solid things:

1. **Cartridge shop.** A press rolls iron into Iron Sheets, the clay belt ends at a Depot, and a coal bin stands over a third Depot. Three Mechanical Arms round one cogwheel set two sheets, two clay and a coal into five Mechanical Crafters, and the Resin Cartridge drops onto a belt to its store.
2. **Canister shop.** A press on the floor conveyor makes the steel into Steel Plates before it leaves the main hall. One arm takes plates from the end of the conveyor and sets three into four crafters, whose fourth slot is covered, and the Open Waste Canister drops onto a belt to its store. The arm is geared slow, at 12 RPM, so that the shop takes about four Steel Ingots a minute and leaves the rest for the alloy.
3. **Concrete plant.** The Depleted Uranium belt and a gravel feeder pass a pair of synchronised tunnels, one uranium and two gravel at a time, into a Basin with a water pump. The Shielding Concrete rides a belt to its store. This takes all the Depleted Uranium the Centrifuges make, so its store in the main hall stays empty until the concrete store is full.

South of the aisle are three mixing bays that make liquids. Each has a bin, a belt, a Basin under a Mechanical Mixer with a pump bringing water in on one side, and a second pump taking the product out on the other, through the south wall to a Fluid Tank in the tank farm.

4. **Coolant.** Lapis Lazuli and water, mixed cold, make Primary Coolant.
5. **Boron.** Scoria drops sixteen at a time between two Crushing Wheels, and the Borax that falls out is mixed with water over a Blaze Burner into Boric Acid.
6. **Fuel Oil.** Coal and water over a Blaze Burner make Fuel Oil for the Diesel Engines.

The tank farm stands outside the south wall inside a low bund: three Fluid Tanks three wide and six high, of 432 buckets each. When a tank is full its mixer stops and the bin waits.

## Worth knowing before changing it

Eight things about Create decided the layout:

- A Brass Tunnel forgets its mode when it joins another on being placed by a command, so the builder sets the mode afterwards.
- A belt that ends at a Basin throws items over it as often as into it. Every belt here ends under a funnel instead.
- A short feeder belt has to end under its tunnel with a solid block after it, or half of what it carries falls off the end.
- A Basin pours onto any belt or Depot that touches it, and picks one if there are two. Each Basin here has only one it can reach.
- A Mechanical Crafter drops what it makes whether or not there is room for it, so what has to be held back is an ingredient, not the product.
- A pump on a Basin draws out the water going in as readily as the product. Each transfer line starts with a Smart Fluid Pipe filtered to the product.
- A group of Mechanical Crafters waits until every one of them holds something. The empty corner of the canister's pattern has a Crafter Slot Cover.
- A belt runs the way its shaft turns it, whichever way it was laid. A belt laid south and driven from its east side needs the opposite speed to one driven from its west side.

## Power

The works has its own substation in a fenced yard on the south side: a Grid Connection, a Surge Arrester, a Circuit Breaker and a 220 / 6 kV Transformer. The whole works draws about 84 MW, which is more than two Power Cables carry at 6 kV, so three cables in parallel run from the Transformer through bushings in the wall to a bus of six Bus Bars. Six feeder breakers stand in front of the bus, one each for the mill, enrichment, the melt shop, assembly, the wing's mixing bays and the wing's shops, so a bay can be shut down by itself.

From each feeder a Power Cable climbs to the roof and runs from insulator to insulator over its bay. The two wing feeders pass through bushings in the shared wall first. Power Wire drops from the insulators to the 75 Electric Motors, two to an insulator. The two motors on the Centrifuge row take about 8.6 MW each and are fed with Power Cable. The builder lays this out by itself and checks that no run is longer than a wire reaches and no terminal has more than four wires.

Three things about the electrical system shaped it:

- **The feeders are closed one at a time, three seconds apart.** A motor draws up to three times its running current for two seconds as it starts. All 75 starting together burnt out the Transformer.
- **The wing has two feeders, not one.** On one feeder its 27 motors hung on a single run of cable 65 blocks long. The motors at the far end kept dropping out and starting again, as they do when their voltage falls below nine tenths of 6 kV, and each start pulled the voltage down for the rest, until the feeder cable burnt. With two feeders no motor is more than 29 blocks of cable from its riser.
- **The Transformer's tap is four steps up,** which is 5%, so that the bus stands at 5.94 kV under the full load.

## How it looks

A long hall in the station's materials: light grey concrete with a cyan band, a dark plinth and pilasters, industrial iron windows along both sides and a raised roof light down the middle on girders. A grated gallery with railings runs the length of the hall over the aisle, reached by stairs at the west end, and looks down on both rows and on the loop. The east end is glazed. Each bay has its sign, every bin and store is labelled, and the loop has a hazard border on the floor.

The wing is two blocks lower than the hall, in the same materials, with a strip of roof glazing over its aisle. A doorway beside the receiving doors joins the two, and the hall's south windows look down into the wing. It has its own door on the west side under a canopy. The three copper tanks stand in the open against its south wall, each with its sign on the bund and with lamps along it.

Outside, the west end has two receiving doors under canopies, an apron, and stockpiles of Chartrine, raw iron and coal. The melt shop's stack stands against the north wall. The substation yard is on the south side, east of the wing, under a gantry with Lightning Rods.

## What it does in a run

In one run of eight minutes from a cold start, read from the stores and tanks each minute:

- The Fuel Store was empty at four minutes, held 2 Fuel Assemblies at five and 8 at eight. Once the line is full that is about one a minute, four times what the reference station burns. Assemblies do not stack, so the store holds 20.
- The tanks held 63 buckets of Primary Coolant, 62 of Boric Acid and 73 of Fuel Oil. That is about 8 buckets a minute each, so a tank fills in a little under an hour.
- The concrete store held 312 Shielding Concrete and gained about 50 a minute at the end, which is all the Depleted Uranium the Centrifuges make. A small vault holds 1,280, so it wants emptying every half hour or so.
- The cartridge store held 79 Resin Cartridges, about 10 a minute, and the canister store 10 Open Waste Canisters, a little over one a minute.
- The Clay Store was full at six minutes. The sheet store held 49 sheets.
- The Transformer carried 84.3 MW with 6.01 kV at its terminals. The main hall takes about 62 MW of that, the mixing bays 10.4 MW and the shops 11.5 MW. The two Crushing Wheels in the mill took about 4.3 MW each and each Centrifuge motor 8.6 MW.

Built from the datapack in a new world, the works ran the same way.

The speeds, the store and tank sizes and the split into six feeders are choices made for this build, not rules of the mod.

## Not in this build

Nothing yet carries what the works makes to the station: the assemblies wait in the Fuel Store, the liquids in the tank farm and the rest in their stores. Sealing the canisters is the station's work, at its canning bench. The works stands by itself on its own Grid Connection: it is not yet set down beside the reference station and wired to it.
