package com.criticalmass.ponder;

import static com.criticalmass.ponder.PlantScenes.chapter;
import static com.criticalmass.ponder.PlantScenes.part;
import static com.criticalmass.ponder.PlantScenes.say;
import static com.criticalmass.ponder.PlantScenes.warn;

import com.criticalmass.content.control.CoreMap;
import com.criticalmass.content.control.PlantAlarms;
import com.criticalmass.content.control.ScreenBlockEntity;
import com.criticalmass.content.fuel.ChannelCapBlock;
import com.criticalmass.content.radiation.AreaMonitorBlock;
import com.criticalmass.content.radiation.AreaMonitorBlockEntity;
import com.criticalmass.content.thermal.DemineralizerBlock;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.Vec3;

public final class ServiceScenes {
    private static final double DOSE_SPAN = 2.0;

    private ServiceScenes() {
    }

    private static int fuel(int percent, int temperature, CoreMap.Cap cap) {
        return new CoreMap.Cell(CoreMap.Kind.FUEL, CoreMap.State.ACTIVE, cap, percent, temperature).pack();
    }

    private static int rod(int inserted) {
        return new CoreMap.Cell(CoreMap.Kind.CONTROL, CoreMap.State.EMPTY, CoreMap.Cap.NONE, inserted, 0).pack();
    }

    private static int water() {
        return new CoreMap.Cell(CoreMap.Kind.COOLANT, CoreMap.State.EMPTY, CoreMap.Cap.NONE, 0, 0).pack();
    }

    private static void rate(CreateSceneBuilder scene, Selection monitor, BlockPos at, double value, boolean alarm) {
        scene.world().modifyBlockEntityNBT(monitor, AreaMonitorBlockEntity.class, nbt -> nbt.putDouble("Rate", value));
        scene.world().modifyBlock(at, s -> s.setValue(AreaMonitorBlock.ALARM, alarm), false);
    }

    private static void resin(CreateSceneBuilder scene, BlockPos at, DemineralizerBlock.Resin shown) {
        scene.world().modifyBlock(at, s -> s.setValue(DemineralizerBlock.RESIN, shown), false);
    }

    public static void channelCap(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("channel_cap", "Refuelling one column at a time");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.75f);
        scene.setSceneOffsetY(-1);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos near = util.grid().at(2, 5, 5);
        BlockPos far = util.grid().at(4, 5, 7);
        BlockPos port = util.grid().at(3, 5, 6);
        Selection wall = util.select().fromTo(8, 1, 0, 8, 3, 4);
        Selection cog = util.select().position(8, 1, 5);
        Vec3 screen = util.vector().blockSurface(util.grid().at(8, 2, 2), Direction.WEST);
        ItemStack wrench = new ItemStack(AllItems.WRENCH.get());
        ItemStack linker = new ItemStack(CMItems.PLANT_LINKER.get());
        ItemStack assembly = new ItemStack(CMItems.FRESH_FUEL_ASSEMBLY.get());
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        ControlScenes.light(scene, wall, false);
        scene.world().showSection(util.select().fromTo(1, 1, 4, 5, 5, 8), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showOutline(PonderPalette.GREEN, "caps", util.select().position(near).add(util.select().position(far)), 110);
        chapter(scene, 110, util.vector().topOf(near), "A Fuel Channel Cap takes the place of a casing block in the vessel's top, over a fuel column. It is that one column's way in and out");
        say(scene, 90, util.vector().topOf(far), "Fit one over each column you mean to work on. A cap anywhere but over a fuel column is a vessel fault");
        scene.overlay().showControls(util.vector().topOf(near), Pointing.DOWN, 40).rightClick().withItem(wrench);
        scene.idle(15);
        scene.world().modifyBlock(near, s -> s.setValue(ChannelCapBlock.OPEN, true), false);
        scene.idle(25);
        chapter(scene, 110, util.vector().topOf(near), "Use a Wrench on a cap to unlock it, or hold it unlocked with a redstone signal. The reactor will not start while a cap is unlocked");
        scene.overlay().showControls(util.vector().topOf(near), Pointing.DOWN, 40).rightClick();
        scene.idle(45);
        say(scene, 110, util.vector().topOf(near), "With the reactor shut down, an empty hand lifts that column's assembly out, spent or not");
        scene.overlay().showControls(util.vector().topOf(near), Pointing.DOWN, 40).rightClick().withItem(assembly);
        scene.idle(45);
        say(scene, 110, util.vector().topOf(near), "An assembly used on the cap goes into its column. So part-burnt fuel can be moved from the middle of the core to the edge");
        say(scene, 100, util.vector().topOf(port), "The Service Port cannot do that. It gives up only the fuel that is spent");
        say(scene, 90, util.vector().topOf(near), "Funnels, Mechanical Arms and Deployers work a cap as hands do");
        warn(scene, 100, util.vector().topOf(near), "An unlocked cap shields a quarter as well as casing. Lock every cap again when the fuel is where you want it");
        scene.rotateCameraY(-35);
        scene.idle(20);
        scene.world().showSection(wall.copy().add(cog), Direction.WEST);
        scene.idle(20);
        scene.world().setKineticSpeed(wall.copy().add(cog), 32);
        ControlScenes.light(scene, wall, true);
        scene.overlay().showControls(util.vector().topOf(near), Pointing.DOWN, 30).rightClick().withItem(linker);
        scene.idle(35);
        scene.overlay().showControls(screen, Pointing.LEFT, 30).rightClick().withItem(linker);
        scene.idle(15);
        int[] cells = {fuel(38, 41, CoreMap.Cap.UNLOCKED), rod(100), fuel(100, 38, CoreMap.Cap.NONE), water(), fuel(72, 45, CoreMap.Cap.NONE), water(),
                fuel(12, 39, CoreMap.Cap.NONE), rod(100), fuel(100, 37, CoreMap.Cap.LOCKED)};
        CompoundTag map = new CoreMap.Snapshot(3, 3, cells, CoreMap.Handling.FREE).save();
        for (int z = 0; z <= 4; z++) {
            scene.world().modifyBlockEntity(util.grid().at(8, 3, z), ScreenBlockEntity.class, held -> {
                if (!held.isController()) {
                    return;
                }
                CompoundTag tag = held.saveWithoutMetadata(held.getLevel().registryAccess());
                tag.putString("Title", CMBlocks.FUEL_CHANNEL_CAP.get().getDescriptionId());
                tag.putString("Status", "");
                tag.putBoolean("Board", false);
                tag.putIntArray("Trends", new int[0]);
                tag.putIntArray("Rows", new int[0]);
                tag.put("Core", map);
                tag.putIntArray("Reactor", new int[]{near.getX(), near.getY(), near.getZ()});
                held.readClient(tag, held.getLevel().registryAccess());
            });
        }
        scene.effects().indicateSuccess(util.grid().at(8, 2, 2));
        scene.idle(25);
        chapter(scene, 120, screen, "Use the Plant Linker on a cap or the Service Port, then on a wall of Plant Screens at least 3 wide and 2 high. The wall draws the core from above, north at the top");
        say(scene, 120, screen, "A fuel column shows the fuel it has left and its temperature: green in use, amber when spent or nearly so, red when damaged or too hot. A mark in its corner is a cap");
        say(scene, 100, screen, "A ring is a control rod and how far it is in. Beside the map the wall says whether fuel can move, and counts the caps still unlocked");
        scene.markAsFinished();
    }

    public static void areaMonitor(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("area_monitor", "Watching a hall with an Area Radiation Monitor");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9f);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos at = util.grid().at(3, 2, 4);
        BlockPos lamp = util.grid().at(4, 2, 4);
        BlockPos source = util.grid().at(3, 1, 2);
        BlockPos shield = util.grid().at(3, 1, 3);
        Selection monitor = util.select().position(at);
        Selection desk = util.select().position(5, 1, 1);
        Vec3 face = util.vector().blockSurface(at, Direction.NORTH).add(0, 0, 0.25);
        rate(scene, monitor, at, 0, false);
        scene.world().modifyBlock(lamp, s -> s.setValue(RedstoneLampBlock.LIT, false), false);
        ControlScenes.reading(scene, desk, 0, 0, "0.00 mSv/min");
        part(scene, desk, 1, p -> p.putInt("Alarms", 0));
        scene.world().showSection(util.select().fromTo(1, 1, 5, 5, 3, 5), Direction.DOWN);
        scene.idle(15);
        scene.world().showSection(monitor, Direction.SOUTH);
        scene.idle(20);
        chapter(scene, 110, face, "An Area Radiation Monitor hangs on a wall and reads the dose rate where it hangs, the same figure a Survey Meter held there gives");
        scene.world().showSection(util.select().position(source), Direction.DOWN);
        scene.idle(15);
        for (int step = 1; step <= 9; step++) {
            rate(scene, monitor, at, step * 0.2, false);
            scene.idle(5);
        }
        say(scene, 110, face, "Its needle sweeps from 0.01 to 100 mSv/min, and it ticks like a Survey Meter, faster as the rate climbs");
        scene.overlay().showControls(face, Pointing.DOWN, 40).rightClick();
        scene.idle(45);
        chapter(scene, 120, face, "Set its alarm level on its value box. The first row is tenths up to 10 mSv/min, the second whole numbers up to 100");
        rate(scene, monitor, at, 1.8, true);
        scene.world().showSection(util.select().position(lamp), Direction.DOWN);
        scene.world().modifyBlock(lamp, s -> s.setValue(RedstoneLampBlock.LIT, true), false);
        scene.effects().indicateRedstone(at);
        scene.idle(25);
        chapter(scene, 120, face, "At or over that level its lamp lights and its horn sounds. It gives a redstone signal to the blocks around it and through the block it hangs on");
        say(scene, 100, face, "It stays in alarm until the rate is a tenth under the level. Use it to silence the horn until the alarm clears");
        say(scene, 80, face, "A comparator reads the rate against the alarm level");
        scene.world().showSection(desk, Direction.DOWN);
        scene.idle(15);
        ControlScenes.reading(scene, desk, 0, 1.8 / DOSE_SPAN, "1.80 mSv/min");
        part(scene, desk, 1, p -> p.putInt("Alarms", 1 << PlantAlarms.Alarm.EQUIPMENT.ordinal()));
        scene.overlay().showControls(face, Pointing.RIGHT, 30).rightClick().withItem(new ItemStack(CMItems.PLANT_LINKER.get()));
        scene.idle(35);
        chapter(scene, 120, util.vector().topOf(5, 1, 1), "The Plant Linker remembers a monitor. A Dashboard Gauge shows its rate with the alarm level at the middle of the scale, and a Dashboard Alarm lamp lights while it alarms");
        say(scene, 100, util.vector().topOf(5, 1, 1), "A wall of Plant Screens linked to it graphs the rate, and a Display Link gives it as text");
        scene.world().showSection(util.select().position(shield), Direction.DOWN);
        scene.idle(15);
        for (int step = 1; step <= 8; step++) {
            rate(scene, monitor, at, 1.8 - step * 0.21, step < 2);
            ControlScenes.reading(scene, desk, 0, (1.8 - step * 0.21) / DOSE_SPAN, String.format(java.util.Locale.ROOT, "%.2f mSv/min", 1.8 - step * 0.21));
            scene.idle(5);
        }
        scene.world().modifyBlock(lamp, s -> s.setValue(RedstoneLampBlock.LIT, false), false);
        part(scene, desk, 1, p -> p.putInt("Alarms", 0));
        scene.idle(15);
        say(scene, 110, util.vector().topOf(shield), "Shielding between the source and the monitor brings its reading down, as it does for you");
        scene.markAsFinished();
    }

    public static void cleanup(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("cleanup", "Cleaning the coolant with a Demineralizer");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.75f);
        scene.setSceneOffsetY(-1);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos controller = util.grid().at(5, 3, 3);
        BlockPos at = util.grid().at(1, 2, 4);
        BlockPos letdown = util.grid().at(4, 2, 4);
        BlockPos charging = util.grid().at(4, 2, 6);
        BlockPos cask = util.grid().at(1, 1, 1);
        Selection line = util.select().fromTo(1, 1, 4, 3, 3, 6);
        Selection drives = util.select().fromTo(2, 2, 4, 2, 3, 4).add(util.select().fromTo(1, 2, 5, 1, 3, 5));
        Vec3 window = util.vector().blockSurface(at, Direction.NORTH);
        ItemStack cartridge = new ItemStack(CMItems.RESIN_CARTRIDGE.get());
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        resin(scene, at, DemineralizerBlock.Resin.NONE);
        scene.world().showSection(util.select().fromTo(4, 1, 3, 8, 5, 7), Direction.DOWN);
        scene.idle(20);
        chapter(scene, 120, util.vector().blockSurface(controller, Direction.NORTH), "When a fuel assembly fails, the vessel's coolant becomes Contaminated Coolant. It keeps that as it goes round the loop and shares it with what it mixes with");
        scene.world().showSection(line, Direction.EAST);
        scene.idle(20);
        chapter(scene, 110, window, "A Demineralizer takes the contamination out of coolant and keeps it in a Resin Cartridge");
        scene.overlay().showOutline(PonderPalette.BLUE, "letdown", util.select().position(letdown), 120);
        scene.overlay().showOutline(PonderPalette.GREEN, "charging", util.select().position(charging), 120);
        say(scene, 120, util.vector().blockSurface(letdown, Direction.WEST), "One Mechanical Pump draws coolant off through a port and into it. A second returns what it has cleaned through a spare inlet port");
        say(scene, 80, util.vector().topOf(at), "Pipes join every face but the top");
        scene.overlay().showControls(util.vector().topOf(at), Pointing.DOWN, 40).rightClick().withItem(cartridge);
        scene.idle(15);
        resin(scene, at, DemineralizerBlock.Resin.FRESH);
        scene.idle(30);
        chapter(scene, 110, window, "Use a Resin Cartridge on it to fit one. Contaminated coolant waits for resin. Clean coolant passes with or without it");
        scene.world().setKineticSpeed(drives, 32);
        scene.effects().rotationSpeedIndicator(util.grid().at(2, 3, 4));
        scene.effects().rotationSpeedIndicator(util.grid().at(1, 3, 5));
        scene.idle(40);
        resin(scene, at, DemineralizerBlock.Resin.USED);
        scene.idle(20);
        chapter(scene, 110, window, "It cleans 10 mB a tick, and the coolant's heat and its boron pass with it");
        say(scene, 120, util.vector().blockSurface(controller, Direction.NORTH), "Each pass takes out what goes through, so the loop gets cleaner as it turns over. At one part in a hundred it is Primary Coolant again");
        say(scene, 100, window, "The window shows the resin bed: amber when fresh, part brown in use and brown when spent");
        resin(scene, at, DemineralizerBlock.Resin.SPENT);
        scene.world().setKineticSpeed(drives, 0);
        scene.idle(20);
        chapter(scene, 120, window, "A cartridge takes up what 4,000 mB of wholly contaminated coolant holds. Then it is spent, and the line stops until it is changed");
        scene.overlay().showControls(util.vector().topOf(at), Pointing.DOWN, 40).rightClick();
        scene.idle(15);
        resin(scene, at, DemineralizerBlock.Resin.NONE);
        scene.idle(30);
        scene.world().showSection(util.select().position(cask), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showControls(util.vector().topOf(cask), Pointing.DOWN, 50).withItem(new ItemStack(CMItems.USED_RESIN_CARTRIDGE.get()));
        warn(scene, 120, util.vector().topOf(cask), "A Used Resin Cartridge is radioactive, 12 mSv/min at one block when spent. Lift it out with an empty hand and put it in a Shielded Cask");
        say(scene, 100, util.vector().topOf(at), "Funnels and Mechanical Arms fit cartridges and take the spent ones. A comparator reads the resin left");
        scene.markAsFinished();
    }

    public static void tubeLeak(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("tube_leak", "Heat Exchanger tubes that wear and leak");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.8f);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos lower = util.grid().at(4, 2, 4);
        BlockPos upper = util.grid().at(4, 3, 4);
        BlockPos at = util.grid().at(2, 2, 6);
        Selection exchangers = util.select().fromTo(4, 2, 4, 4, 3, 4);
        Selection monitor = util.select().position(at);
        Selection desk = util.select().position(6, 1, 1);
        Vec3 tubes = util.vector().blockSurface(lower, Direction.NORTH);
        Vec3 face = util.vector().blockSurface(at, Direction.NORTH).add(0, 0, 0.25);
        rate(scene, monitor, at, 0, false);
        part(scene, desk, 0, p -> p.putInt("Alarms", 0));
        ControlScenes.reading(scene, desk, 1, 0, "0.00 mSv/min");
        scene.world().showSection(util.select().fromTo(2, 1, 4, 6, 4, 4), Direction.DOWN);
        scene.idle(20);
        chapter(scene, 120, tubes, "A Heat Exchanger's tubes wear while heat crosses them, faster the further apart the two sides are in temperature");
        say(scene, 120, util.vector().blockSurface(upper, Direction.NORTH), "At 150 °C apart a block is worn out in 10 hours of running, and at 50 °C apart in 30. A taller stack spreads the heat and lasts longer");
        warn(scene, 100, tubes, "Hot coolant flowing through with the steam side dry wears them out in half an hour");
        scene.overlay().showOutline(PonderPalette.RED, "leak", exchangers, 130);
        chapter(scene, 120, tubes, "From three quarters worn the tubes leak. Primary coolant crosses into the steam side, up to 4 mB a tick when they are worn out");
        say(scene, 110, tubes, "The primary loop loses coolant and the steam loop gains it as water. Goggles on the exchanger show its condition and the leak");
        scene.world().showSection(util.select().fromTo(1, 1, 7, 3, 3, 7), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(monitor, Direction.SOUTH);
        scene.idle(15);
        for (int step = 1; step <= 6; step++) {
            rate(scene, monitor, at, step * 0.05, step >= 4);
            scene.idle(6);
        }
        scene.effects().indicateRedstone(at);
        chapter(scene, 130, face, "The leak carries activity round the steam loop, and every block and pipe it is in gives a dose. An Area Radiation Monitor in the turbine hall, set low, catches it");
        say(scene, 100, face, "That activity halves every minute, so the dose is there only while the tubes leak");
        scene.world().showSection(desk, Direction.DOWN);
        scene.idle(15);
        part(scene, desk, 0, p -> p.putInt("Alarms", 1 << PlantAlarms.Alarm.EQUIPMENT.ordinal()));
        ControlScenes.reading(scene, desk, 1, 0.75, "0.30 mSv/min");
        scene.idle(15);
        say(scene, 100, util.vector().topOf(6, 1, 1), "A Dashboard Alarm lamp bound to the exchanger lights while its tubes leak");
        scene.overlay().showControls(util.vector().topOf(upper), Pointing.DOWN, 50).rightClick().withItem(new ItemStack(CMItems.REACTOR_ALLOY_SHEET.get()));
        scene.idle(20);
        chapter(scene, 130, tubes, "To plug the tubes, stop both loops and let the exchanger cool to 60 °C, then use a Reactor Alloy Sheet on it. Each sheet takes a quarter off its wear");
        for (int step = 5; step >= 0; step--) {
            rate(scene, monitor, at, step * 0.05, false);
            scene.idle(5);
        }
        part(scene, desk, 0, p -> p.putInt("Alarms", 0));
        ControlScenes.reading(scene, desk, 1, 0, "0.00 mSv/min");
        scene.effects().indicateSuccess(lower);
        scene.idle(15);
        warn(scene, 100, tubes, "Used on an exchanger that is hot or flowing, it burns you and takes no sheet");
        scene.markAsFinished();
    }

    public static void chartrine(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("chartrine", "Growing Chartrine from Contaminated Coolant");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos tank = util.grid().at(1, 2, 3);
        BlockPos pump = util.grid().at(2, 2, 3);
        BlockPos cell = util.grid().at(4, 1, 3);
        BlockPos mouth = util.grid().at(4, 2, 3);
        BlockPos drill = util.grid().at(5, 1, 3);
        Selection line = util.select().fromTo(1, 1, 3, 3, 3, 3);
        Selection pit = util.select().fromTo(3, 1, 2, 4, 1, 5).add(util.select().position(5, 1, 4));
        Selection miner = util.select().fromTo(5, 1, 3, 6, 1, 3);
        Vec3 drip = util.vector().centerOf(mouth);
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        scene.world().showSection(pit, Direction.DOWN);
        scene.idle(20);
        chapter(scene, 110, util.vector().topOf(cell), "Chartrine lies in layered veins underground. It can also be grown, from lava and the Contaminated Coolant of a failed core");
        scene.world().showSection(line, Direction.DOWN);
        scene.idle(20);
        say(scene, 120, drip, "Leave a Fluid Pipe open a block above the lava and pump Contaminated Coolant to it. It leaves the pipe 1 mB a tick");
        scene.world().setKineticSpeed(util.select().fromTo(2, 2, 3, 2, 3, 3), 32);
        scene.effects().rotationSpeedIndicator(pump);
        scene.idle(40);
        scene.world().setBlock(cell, CMBlocks.CHARTRINE.get().defaultBlockState(), true);
        scene.idle(20);
        chapter(scene, 110, util.vector().topOf(cell), "About every 250 mB that lands on the lava turns it to stone");
        say(scene, 130, util.vector().blockSurface(tank, Direction.NORTH), "How contaminated the coolant is sets the stone. Straight from the core it is always Chartrine. Watered down by half it is Chartrine half the time and tuff the rest");
        scene.world().showSection(miner, Direction.WEST);
        scene.idle(15);
        scene.world().setKineticSpeed(miner, 32);
        scene.idle(25);
        scene.world().destroyBlock(cell);
        scene.idle(15);
        scene.world().setBlock(cell, net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState(), false);
        say(scene, 100, util.vector().blockSurface(drill, Direction.NORTH), "Break the stone and let the lava flow back in for the next block");
        warn(scene, 140, util.vector().blockSurface(tank, Direction.NORTH), "Only contamination the reactor made counts, and only while a record of it is kept. A Fluid Tank keeps it. Coolant carried in a bucket stays dangerous but makes only tuff");
        scene.world().setBlock(cell, net.minecraft.world.level.block.Blocks.TUFF.defaultBlockState(), true);
        scene.idle(30);
        warn(scene, 110, drip, "While stone fills the pit the coolant has no lava to land on. It dirties the floor instead, as any leak does");
        say(scene, 100, util.vector().topOf(cell), "Crushing Wheels turn Chartrine into Crushed Raw Uranium 12 times in 100");
        scene.markAsFinished();
    }

    public static void pumpSeals(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("pump_seals", "Pump seals and their leaks");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos pump = util.grid().at(3, 2, 3);
        BlockPos monitor = util.grid().at(3, 2, 4);
        Selection drive = util.select().fromTo(3, 1, 3, 3, 2, 3);
        Selection run = util.select().fromTo(1, 2, 3, 5, 2, 3);
        Selection wall = util.select().fromTo(2, 1, 5, 4, 3, 5).add(util.select().position(monitor));
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        rate(scene, util.select().position(monitor), monitor, 0.02, false);
        scene.world().showSection(run.add(drive), Direction.DOWN);
        scene.idle(20);
        scene.world().setKineticSpeed(drive, 64);
        scene.effects().rotationSpeedIndicator(pump);
        chapter(scene, 120, util.vector().topOf(pump), "A Circulation Pump's seals wear while it moves coolant. They are worn out in 10 hours of running, and ten times as fast while the pump cavitates");
        scene.overlay().showOutline(PonderPalette.RED, "leak", util.select().position(pump), 110);
        warn(scene, 110, util.vector().blockSurface(pump, Direction.NORTH), "From three quarters worn they leak coolant out beside the pump, up to 1 mB a tick, whether or not it is turning");
        scene.world().showSection(wall, Direction.NORTH);
        scene.idle(20);
        rate(scene, util.select().position(monitor), monitor, 3.4, true);
        chapter(scene, 120, util.vector().blockSurface(monitor, Direction.NORTH), "Water is only lost. Primary coolant also contaminates the floor at the pump, so an Area Radiation Monitor in the pump room catches the leak");
        say(scene, 100, util.vector().topOf(pump), "Goggles show the seals' condition and the leak. A Dashboard Alarm lamp bound to the pump lights while it leaks");
        scene.world().setKineticSpeed(drive, 0);
        scene.idle(15);
        scene.overlay().showControls(util.vector().topOf(pump), Pointing.DOWN, 50).rightClick().withItem(new ItemStack(CMItems.SEALING_GASKET.get()));
        chapter(scene, 120, util.vector().topOf(pump), "Stop the pump and use a Sealing Gasket on it. Each one takes a quarter off the wear. A pump that is turning hurts the hand and takes no gasket");
        say(scene, 100, util.vector().blockSurface(monitor, Direction.NORTH), "The floor stays contaminated until it is cleaned with Decontamination Compound");
        rate(scene, util.select().position(monitor), monitor, 0.02, false);
        say(scene, 80, util.vector().topOf(pump), "A pump keeps its wear when it is broken and placed again");
        scene.markAsFinished();
    }

    public static void throughWall(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("through_wall", "Taking a pipe through a containment wall");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos hole = util.grid().at(3, 2, 3);
        Selection wall = util.select().fromTo(3, 1, 1, 3, 3, 5).substract(util.select().position(hole));
        Selection run = util.select().fromTo(1, 2, 3, 5, 2, 3);
        Selection side = util.select().fromTo(4, 2, 2, 4, 2, 2);
        scene.world().showSection(wall, Direction.DOWN);
        scene.idle(15);
        scene.world().showSection(run, Direction.DOWN);
        scene.idle(20);
        warn(scene, 110, util.vector().blockSurface(hole, Direction.WEST), "A pipe through a containment wall is a hole in it. Radiation gets out there, and so does the blast");
        scene.world().showSection(side, Direction.SOUTH);
        scene.idle(15);
        scene.overlay().showControls(util.vector().topOf(util.grid().at(4, 2, 3)), Pointing.DOWN, 40).rightClick().withItem(AllItems.WRENCH.asStack());
        scene.idle(15);
        scene.world().setBlock(util.grid().at(4, 2, 3), CMBlocks.REINFORCED_STRAIGHT_PIPE.get().defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X), false);
        chapter(scene, 120, util.vector().topOf(util.grid().at(4, 2, 3)), "Use a Wrench on the side of a Reinforced Pipe to lock it straight. A locked pipe joins nothing beside its run");
        scene.overlay().showControls(util.vector().topOf(hole), Pointing.DOWN, 40).rightClick().withItem(new ItemStack(CMBlocks.SHIELDING_CONCRETE.get()));
        scene.idle(15);
        scene.world().setBlock(hole, CMBlocks.ENCASED_REINFORCED_PIPE.get().defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X), true);
        chapter(scene, 120, util.vector().topOf(hole), "Use Shielding Concrete on a straight or locked Reinforced Pipe to encase it. It becomes a full block with the pipe running through it");
        say(scene, 120, util.vector().blockSurface(hole, Direction.WEST), "It shields like Shielding Concrete, stops corium and stands through a nuclear explosion, and coolant still passes along its one way");
        say(scene, 100, util.vector().topOf(hole), "A Wrench takes the casing off again, leaves a locked pipe and gives the concrete back. A bend or a tee cannot be encased");
        scene.markAsFinished();
    }

    public static void holdingTank(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("holding_tank", "Holding coolant in a Fluid Tank");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos tank = util.grid().at(3, 2, 3);
        BlockPos desk = util.grid().at(3, 1, 1);
        Selection stand = util.select().fromTo(3, 1, 3, 3, 2, 3);
        Selection in = util.select().fromTo(1, 2, 3, 2, 3, 3);
        Selection out = util.select().fromTo(4, 2, 3, 5, 3, 3);
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        scene.world().showSection(stand, Direction.DOWN);
        scene.idle(20);
        chapter(scene, 120, util.vector().blockSurface(tank, Direction.NORTH), "A Create Fluid Tank keeps the figures of the coolant or Boric Acid in it: its heat, its boron and its contamination");
        scene.world().showSection(in, Direction.EAST);
        scene.world().showSection(out, Direction.WEST);
        scene.idle(20);
        scene.world().setKineticSpeed(in.add(out), 32);
        say(scene, 120, util.vector().topOf(tank), "The figures go with the coolant through any pipe or pump, between tanks and the plant's machines, so a holding tank and a borated make-up tank work");
        say(scene, 110, util.vector().blockSurface(tank, Direction.NORTH), "Mixing shares them out. Coolant at one part contaminated in a hundred or less is Primary Coolant again");
        scene.world().showSection(util.select().position(desk), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showControls(util.vector().blockSurface(tank, Direction.NORTH), Pointing.RIGHT, 40).rightClick().withItem(new ItemStack(CMItems.PLANT_LINKER.get()));
        chapter(scene, 120, util.vector().topOf(desk), "Use the Plant Linker on a tank to remember it for a Dashboard Gauge, a Plant Screen wall or a Display Link. They read its temperature, level, boron and contamination");
        warn(scene, 140, util.vector().topOf(tank), "A bucket, a Basin and an Item Drain keep no figures. Coolant from them comes cold and without boron, and Contaminated Coolant from them counts as wholly contaminated, with nothing on the reactor's record");
        scene.markAsFinished();
    }
}
