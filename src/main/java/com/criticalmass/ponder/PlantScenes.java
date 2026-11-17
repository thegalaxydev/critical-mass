package com.criticalmass.ponder;

import com.criticalmass.content.control.DashboardBlockEntity;
import com.criticalmass.content.control.PlantAlarms;
import com.criticalmass.content.power.BreakerBlock;
import com.criticalmass.content.power.BreakerBlockEntity;
import com.criticalmass.content.power.TransferSwitchBlock;
import com.criticalmass.content.power.TransferSwitchBlockEntity;
import com.criticalmass.content.power.TransformerBlockEntity;
import com.criticalmass.content.power.WireLinks;
import com.criticalmass.content.power.WireType;
import com.criticalmass.content.thermal.Corium;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMItems;
import com.criticalmass.registry.CMParticles;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.phys.Vec3;

public final class PlantScenes {
    private PlantScenes() {
    }

    private static void wire(SceneBuilder scene, BlockPos a, int ta, BlockPos b, int tb, WireType type, int glowing) {
        scene.addInstruction(s -> WireLinks.sketch(s.getWorld(), a, ta, b, tb, type, glowing));
    }

    private static void unwire(SceneBuilder scene, BlockPos a, BlockPos b) {
        scene.addInstruction(s -> WireLinks.erase(s.getWorld(), a, b));
    }

    private static void say(CreateSceneBuilder scene, int ticks, Vec3 at, String text) {
        scene.overlay().showText(ticks).text(text).pointAt(at).placeNearTarget();
        scene.idle(ticks + 10);
    }

    private static void chapter(CreateSceneBuilder scene, int ticks, Vec3 at, String text) {
        scene.overlay().showText(ticks).attachKeyFrame().text(text).pointAt(at).placeNearTarget();
        scene.idle(ticks + 10);
    }

    private static void warn(CreateSceneBuilder scene, int ticks, Vec3 at, String text) {
        scene.overlay().showText(ticks).colored(PonderPalette.RED).text(text).pointAt(at).placeNearTarget();
        scene.idle(ticks + 10);
    }

    public static void wires(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("wires", "Stringing wires between terminals");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos transformer = util.grid().at(1, 1, 5);
        BlockPos bus = util.grid().at(5, 1, 5);
        BlockPos insulator = util.grid().at(3, 2, 5);
        ItemStack cable = new ItemStack(CMItems.POWER_CABLE.get());
        scene.world().showSection(util.select().position(transformer), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(bus), Direction.DOWN);
        scene.idle(15);
        chapter(scene, 90, util.vector().topOf(transformer), "Generators, Electric Motors, Transformers, Circuit Breakers, Bus Bars, Grid Connections and Insulators have terminals");
        scene.overlay().showControls(util.vector().topOf(transformer), Pointing.DOWN, 30).rightClick().withItem(cable);
        scene.idle(35);
        scene.overlay().showControls(util.vector().topOf(bus), Pointing.DOWN, 30).rightClick().withItem(cable);
        scene.idle(10);
        wire(scene, transformer, TransformerBlockEntity.LOW, bus, 0, WireType.CABLE, 0);
        scene.idle(30);
        say(scene, 70, util.vector().centerOf(3, 2, 5), "Use a wire on one terminal and then on another to string it between them");
        say(scene, 70, util.vector().centerOf(3, 2, 5), "A wire reaches 24 blocks, and a terminal holds 4 wires");
        unwire(scene, transformer, bus);
        scene.world().showSection(util.select().fromTo(3, 1, 5, 3, 2, 5), Direction.DOWN);
        scene.idle(20);
        wire(scene, transformer, TransformerBlockEntity.LOW, insulator, 0, WireType.CABLE, 0);
        scene.idle(10);
        wire(scene, insulator, 0, bus, 0, WireType.CABLE, 0);
        scene.idle(10);
        chapter(scene, 80, util.vector().topOf(insulator), "An Insulator stands on any solid face. It is a place to join wires, turn a corner or carry a run across a hall");
        WireType[] kinds = {WireType.COPPER, WireType.CABLE, WireType.LINE};
        String[] lines = {
                "Power Wire is copper, for 6 kV and 1 kA: enough for one motor at full load",
                "Power Cable is thick and black, for 20 kV and 4 kA: it joins generators and feeds a motor bus",
                "Transmission Line is bare and silver, for 220 kV and 1 kA: it carries power to the grid"};
        for (int i = 0; i < 3; i++) {
            int z = 3 - i;
            BlockPos west = util.grid().at(1, 1, z);
            BlockPos east = util.grid().at(5, 1, z);
            scene.world().showSection(util.select().position(west).add(util.select().position(east)), Direction.DOWN);
            scene.idle(10);
            wire(scene, west, 0, east, 0, kinds[i], 0);
            scene.idle(10);
            if (i == 0) {
                chapter(scene, 70, util.vector().centerOf(3, 1, z), lines[i]);
            } else {
                say(scene, 70, util.vector().centerOf(3, 1, z), lines[i]);
            }
        }
        say(scene, 70, util.vector().centerOf(3, 1, 2), "A wire may be used below its voltage. Its current limit stays the same, so it carries less power there");
        BlockPos west = util.grid().at(1, 1, 3);
        BlockPos east = util.grid().at(5, 1, 3);
        for (int glow = 1; glow <= WireLinks.GLOW_STEPS; glow++) {
            wire(scene, west, 0, east, 0, WireType.COPPER, glow);
            scene.idle(8);
        }
        scene.overlay().showText(80).attachKeyFrame().colored(PonderPalette.RED).text("A wire above its current heats and glows orange. Left like that it burns through, and starts a fire at each end").pointAt(util.vector().centerOf(3, 1, 3)).placeNearTarget();
        scene.idle(90);
        unwire(scene, west, east);
        scene.effects().emitParticles(util.vector().centerOf(3, 1, 3), scene.effects().simpleParticleEmitter(ParticleTypes.LARGE_SMOKE, Vec3.ZERO), 4, 3);
        scene.idle(30);
        scene.overlay().showControls(util.vector().topOf(util.grid().at(1, 1, 2)), Pointing.DOWN, 40).rightClick().whileSneaking();
        scene.idle(20);
        unwire(scene, util.grid().at(1, 1, 2), util.grid().at(5, 1, 2));
        scene.idle(25);
        say(scene, 70, util.vector().topOf(util.grid().at(1, 1, 2)), "Sneak and use a terminal to cut its last wire and get it back");
        warn(scene, 90, util.vector().topOf(transformer), "Stringing or cutting a wire on a live terminal draws an arc. Open a breaker and work on the dead side");
        scene.markAsFinished();
    }

    public static void breaker(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("breaker", "Protecting a feeder with a Circuit Breaker");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos bus = util.grid().at(1, 1, 3);
        BlockPos breaker = util.grid().at(3, 1, 3);
        BlockPos motor = util.grid().at(5, 1, 3);
        Selection drive = util.select().fromTo(5, 1, 3, 6, 1, 3);
        scene.world().setKineticSpeed(drive, 0);
        scene.world().showSection(util.select().position(bus), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(breaker), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(drive, Direction.DOWN);
        scene.idle(15);
        wire(scene, bus, 0, breaker, BreakerBlockEntity.LINE, WireType.CABLE, 0);
        scene.idle(10);
        wire(scene, breaker, BreakerBlockEntity.LOAD, motor, 0, WireType.COPPER, 0);
        scene.idle(20);
        chapter(scene, 90, util.vector().topOf(breaker), "A Circuit Breaker has two terminals on top. The line side is ringed in brass and the load side in copper");
        scene.overlay().showControls(util.vector().blockSurface(breaker, Direction.NORTH), Pointing.RIGHT, 30).rightClick();
        scene.idle(15);
        scene.world().modifyBlock(breaker, s -> s.setValue(BreakerBlock.CLOSED, true), false);
        scene.world().setKineticSpeed(drive, 32);
        scene.effects().indicateSuccess(breaker);
        scene.idle(30);
        say(scene, 80, util.vector().blockSurface(breaker, Direction.NORTH), "Use it to close or open it. The red lamp is lit while it is closed and the green one while it is open");
        chapter(scene, 90, util.vector().blockSurface(breaker, Direction.NORTH), "Its value box sets the current it trips at, from 0.1 to 6 kA. Set it at or under the limit of the wire it feeds");
        for (int glow = 1; glow <= 3; glow++) {
            wire(scene, breaker, BreakerBlockEntity.LOAD, motor, 0, WireType.COPPER, glow);
            scene.idle(8);
        }
        scene.world().modifyBlock(breaker, s -> s.setValue(BreakerBlock.CLOSED, false), false);
        scene.world().setKineticSpeed(drive, 0);
        scene.effects().indicateRedstone(breaker);
        scene.idle(10);
        wire(scene, breaker, BreakerBlockEntity.LOAD, motor, 0, WireType.COPPER, 0);
        scene.overlay().showText(90).attachKeyFrame().colored(PonderPalette.RED).text("Above that current it trips, sooner the further above, and opens before the wire burns").pointAt(util.vector().centerOf(4, 1, 3)).placeNearTarget();
        scene.idle(100);
        say(scene, 80, util.vector().topOf(breaker), "A breaker tripped by its protection locks out. Use it once to reset it, and again to close it");
        scene.overlay().showControls(util.vector().blockSurface(breaker, Direction.NORTH), Pointing.RIGHT, 20).rightClick();
        scene.idle(30);
        scene.overlay().showControls(util.vector().blockSurface(breaker, Direction.NORTH), Pointing.RIGHT, 20).rightClick();
        scene.idle(15);
        scene.world().modifyBlock(breaker, s -> s.setValue(BreakerBlock.CLOSED, true), false);
        scene.world().setKineticSpeed(drive, 32);
        scene.idle(30);
        say(scene, 80, util.vector().topOf(breaker), "A redstone signal trips it without locking it, and a comparator reads 15 while it is closed");
        warn(scene, 90, util.vector().topOf(breaker), "A breaker protects only what is behind it, and can break a short circuit of up to 1,500 MW. Asked for more, it fails");
        scene.markAsFinished();
    }

    public static void synchronising(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("synchronising", "Putting a Generator on the grid");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.8f);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos generator = util.grid().at(1, 1, 4);
        BlockPos breaker = util.grid().at(3, 1, 4);
        BlockPos transformer = util.grid().at(5, 1, 4);
        BlockPos grid = util.grid().at(7, 1, 4);
        Selection set = util.select().fromTo(0, 1, 4, 1, 1, 4);
        scene.world().setKineticSpeed(set, 0);
        scene.world().showSection(util.select().fromTo(5, 1, 4, 7, 1, 4), Direction.DOWN);
        scene.idle(10);
        wire(scene, transformer, TransformerBlockEntity.HIGH, grid, 0, WireType.LINE, 0);
        scene.idle(15);
        chapter(scene, 80, util.vector().topOf(grid), "The Grid Connection is live at 220 kV and 50 Hz. A Transformer brings it down to the generator's 20 kV");
        scene.world().showSection(util.select().position(breaker), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(set, Direction.DOWN);
        scene.idle(10);
        wire(scene, breaker, BreakerBlockEntity.LOAD, transformer, TransformerBlockEntity.LOW, WireType.CABLE, 0);
        scene.idle(5);
        wire(scene, generator, 0, breaker, BreakerBlockEntity.LINE, WireType.CABLE, 0);
        scene.idle(15);
        scene.world().setKineticSpeed(set, 64);
        scene.effects().rotationSpeedIndicator(util.grid().at(0, 1, 4));
        scene.idle(20);
        chapter(scene, 80, util.vector().topOf(generator), "A Generator makes 50 Hz at 64 RPM. Off the grid its frequency follows its speed, and its speed trim moves it up to 1 Hz either way");
        say(scene, 100, util.vector().topOf(breaker), "With both sides live the breaker closes only when they are at the same voltage, within 0.2 Hz and within 20 degrees of each other");
        scene.overlay().showControls(util.vector().blockSurface(breaker, Direction.NORTH), Pointing.RIGHT, 30).rightClick();
        scene.idle(15);
        scene.effects().indicateRedstone(breaker);
        scene.idle(25);
        warn(scene, 70, util.vector().topOf(breaker), "Closed out of step it refuses, and says which of the three stopped it");
        chapter(scene, 100, util.vector().topOf(breaker), "While the two frequencies differ the angle between them turns, one full turn a second for each hertz apart. A gauge bound to the breaker shows it");
        say(scene, 90, util.vector().topOf(generator), "Raise and lower trim the generator by 0.02 Hz. Trim until the angle turns slowly");
        scene.overlay().showControls(util.vector().blockSurface(breaker, Direction.NORTH), Pointing.RIGHT, 30).rightClick();
        scene.idle(15);
        scene.world().modifyBlock(breaker, s -> s.setValue(BreakerBlock.CLOSED, true), false);
        scene.effects().indicateSuccess(breaker);
        scene.idle(25);
        say(scene, 70, util.vector().topOf(breaker), "Close the breaker as the angle passes zero");
        chapter(scene, 100, util.vector().topOf(generator), "On the grid the generator is held at 50 Hz and starts unloaded. Raise and lower now change its load, and its Load Setting value box sets it directly");
        warn(scene, 100, util.vector().topOf(generator), "It takes at most nine tenths of what its shaft has to spare, so a weak turbine lowers the output and does not stall");
        scene.markAsFinished();
    }

    public static void standby(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("standby", "Standby power for the station bus");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.8f);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos engine = util.grid().at(1, 1, 5);
        BlockPos generator = util.grid().at(2, 1, 5);
        BlockPos transformer = util.grid().at(4, 1, 5);
        BlockPos transfer = util.grid().at(6, 1, 5);
        BlockPos battery = util.grid().at(1, 1, 2);
        BlockPos supply = util.grid().at(7, 1, 2);
        BlockPos busWest = util.grid().at(3, 1, 2);
        BlockPos busEast = util.grid().at(5, 1, 2);
        Selection set = util.select().fromTo(1, 1, 5, 2, 1, 5);
        Selection dust = util.select().fromTo(1, 1, 4, 6, 1, 4);
        scene.world().setKineticSpeed(set, 0);
        scene.world().modifyBlocks(dust, s -> s.hasProperty(RedStoneWireBlock.POWER) ? s.setValue(RedStoneWireBlock.POWER, 0) : s, false);
        scene.world().modifyBlock(transfer, s -> s.setValue(TransferSwitchBlock.POSITION, TransferSwitchBlock.Position.NORMAL), false);
        scene.world().modifyBlock(supply, s -> s.setValue(BreakerBlock.CLOSED, true), false);
        scene.world().showSection(util.select().fromTo(3, 1, 2, 5, 1, 2).add(util.select().position(supply)), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(transfer), Direction.DOWN);
        scene.idle(10);
        wire(scene, supply, BreakerBlockEntity.LOAD, transfer, TransferSwitchBlockEntity.NORMAL, WireType.CABLE, 0);
        scene.idle(5);
        wire(scene, transfer, TransferSwitchBlockEntity.LOAD, busEast, 0, WireType.CABLE, 0);
        scene.idle(15);
        chapter(scene, 100, util.vector().topOf(transfer), "A Transfer Switch has three terminals: the normal supply and the standby one at its two ends, and the load in the middle");
        scene.world().showSection(set.add(util.select().position(transformer)), Direction.DOWN);
        scene.idle(10);
        wire(scene, generator, 0, transformer, TransformerBlockEntity.HIGH, WireType.CABLE, 0);
        scene.idle(5);
        wire(scene, transformer, TransformerBlockEntity.LOW, transfer, TransferSwitchBlockEntity.STANDBY, WireType.CABLE, 0);
        scene.idle(15);
        say(scene, 100, util.vector().topOf(engine), "A standby set is a Diesel Engine, a Generator and a Transformer down to 6 kV. The engine turns at 64 RPM, which is what a Generator needs");
        scene.world().showSection(dust, Direction.DOWN);
        scene.idle(15);
        scene.world().showSection(util.select().position(battery), Direction.DOWN);
        scene.idle(10);
        wire(scene, battery, 0, busWest, 0, WireType.CABLE, 0);
        scene.idle(15);
        chapter(scene, 90, util.vector().topOf(battery), "A Station Battery charges while the bus is live. With nothing else on its wires it holds the bus by itself, up to 2 MW");
        scene.world().modifyBlock(supply, s -> s.setValue(BreakerBlock.CLOSED, false), false);
        scene.effects().indicateRedstone(supply);
        scene.idle(20);
        scene.overlay().showText(80).attachKeyFrame().colored(PonderPalette.RED).text("When the normal supply is lost, the battery carries the bus at once").pointAt(util.vector().topOf(supply)).placeNearTarget();
        scene.idle(90);
        scene.world().modifyBlock(transfer, s -> s.setValue(TransferSwitchBlock.POSITION, TransferSwitchBlock.Position.OFF), false);
        scene.world().toggleRedstonePower(dust);
        scene.effects().indicateRedstone(transfer);
        scene.idle(20);
        say(scene, 90, util.vector().topOf(transfer), "A second later the switch lets go of the dead supply and gives a redstone signal. Lead it to the engine");
        scene.world().setKineticSpeed(set, 64);
        scene.effects().rotationSpeedIndicator(engine);
        scene.idle(20);
        say(scene, 70, util.vector().topOf(engine), "The engine cranks for 3 seconds and runs");
        scene.world().modifyBlock(transfer, s -> s.setValue(TransferSwitchBlock.POSITION, TransferSwitchBlock.Position.STANDBY), false);
        scene.effects().indicateSuccess(transfer);
        scene.idle(20);
        say(scene, 90, util.vector().topOf(transfer), "Once the standby supply is live the switch moves the load onto it, and the battery charges again");
        scene.world().modifyBlock(supply, s -> s.setValue(BreakerBlock.CLOSED, true), false);
        scene.idle(30);
        scene.world().modifyBlock(transfer, s -> s.setValue(TransferSwitchBlock.POSITION, TransferSwitchBlock.Position.NORMAL), false);
        scene.world().toggleRedstonePower(dust);
        scene.world().setKineticSpeed(set, 0);
        scene.idle(20);
        chapter(scene, 100, util.vector().topOf(transfer), "When the normal supply has been back for 5 seconds the switch moves the load back and drops the signal, and the engine stops");
        say(scene, 90, util.vector().topOf(transfer), "It is never on both supplies at once, so the two need not be in step");
        say(scene, 100, util.vector().topOf(battery), "Batteries on the same wires share the load. Asked for more than they can give, they shut down for 5 seconds");
        warn(scene, 100, util.vector().topOf(engine), "The engine burns Fuel Oil from its own tank. A full tank lasts about 17 minutes flat out");
        scene.markAsFinished();
    }

    public static void repair(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("repair", "Repairing burnt-out equipment");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos transformer = util.grid().at(3, 1, 3);
        BlockPos west = util.grid().at(1, 1, 3);
        BlockPos east = util.grid().at(5, 1, 3);
        scene.world().showSection(util.select().fromTo(1, 1, 3, 5, 1, 3), Direction.DOWN);
        scene.idle(15);
        wire(scene, west, 0, transformer, TransformerBlockEntity.HIGH, WireType.CABLE, 0);
        wire(scene, transformer, TransformerBlockEntity.LOW, east, 0, WireType.COPPER, 0);
        scene.idle(20);
        chapter(scene, 80, util.vector().topOf(transformer), "A mistake costs equipment: too much voltage, a short circuit with no breaker to clear it, or lightning");
        scene.addInstruction(s -> {
            WireLinks.erase(s.getWorld(), west, transformer);
            WireLinks.erase(s.getWorld(), transformer, east);
        });
        scene.effects().emitParticles(util.vector().centerOf(transformer), scene.effects().simpleParticleEmitter(ParticleTypes.EXPLOSION, Vec3.ZERO), 1, 2);
        scene.world().setBlock(transformer, CMBlocks.BURNT_EQUIPMENT.get().defaultBlockState(), true);
        scene.idle(10);
        scene.effects().emitParticles(util.vector().topOf(transformer), scene.effects().simpleParticleEmitter(ParticleTypes.LARGE_SMOKE, new Vec3(0, 0.05, 0)), 2, 60);
        scene.overlay().showText(90).attachKeyFrame().colored(PonderPalette.RED).text("Anything lost to a fault is left as a smoking wreck where it stood, with its wires gone").pointAt(util.vector().topOf(transformer)).placeNearTarget();
        scene.idle(100);
        scene.overlay().showControls(util.vector().topOf(transformer), Pointing.DOWN, 40).rightClick().withItem(new ItemStack(AllItems.COPPER_SHEET.get()));
        scene.idle(50);
        scene.overlay().showControls(util.vector().topOf(transformer), Pointing.DOWN, 40).rightClick().withItem(new ItemStack(CMItems.STEEL_PLATE.get()));
        scene.idle(50);
        say(scene, 100, util.vector().topOf(transformer), "Use Copper Sheets and Steel Plates on it: 4 of each for a Transformer or a Generator section, and 2 of each for anything else");
        scene.world().restoreBlocks(util.select().position(transformer));
        scene.effects().indicateSuccess(transformer);
        scene.idle(20);
        chapter(scene, 80, util.vector().topOf(transformer), "It stands again with its settings, unwired. String its wires once more");
        wire(scene, west, 0, transformer, TransformerBlockEntity.HIGH, WireType.CABLE, 0);
        scene.idle(10);
        wire(scene, transformer, TransformerBlockEntity.LOW, east, 0, WireType.COPPER, 0);
        scene.idle(20);
        say(scene, 70, util.vector().topOf(transformer), "Broken instead, a wreck gives one Steel Plate");
        scene.overlay().showControls(util.vector().blockSurface(transformer, Direction.NORTH), Pointing.RIGHT, 40).rightClick().withItem(new ItemStack(AllItems.COPPER_SHEET.get()));
        scene.idle(20);
        chapter(scene, 100, util.vector().blockSurface(transformer, Direction.NORTH), "Motors, Transformers and breakers also wear. A Copper Sheet used on one, away from its value box, gives back a quarter of its condition");
        warn(scene, 100, util.vector().topOf(transformer), "It must be dead for that. Open its breaker, open a Disconnect Switch and close an Earthing Switch first");
        scene.markAsFinished();
    }

    public static void bypass(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("bypass", "Bypassing the reactor's protection");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos controller = util.grid().at(3, 2, 5);
        BlockPos desk = util.grid().at(3, 1, 2);
        Selection deskOnly = util.select().position(desk);
        scene.world().showSection(util.select().fromTo(2, 1, 5, 4, 3, 5), Direction.DOWN);
        scene.idle(20);
        chapter(scene, 110, util.vector().blockSurface(controller, Direction.NORTH), "The Reactor Controller trips the reactor by itself on fuel failure, high temperature, high flux, a short period and lost flow");
        scene.world().showSection(deskOnly, Direction.DOWN);
        scene.idle(20);
        scene.overlay().showControls(util.vector().blockSurface(controller, Direction.NORTH), Pointing.RIGHT, 30).rightClick().withItem(new ItemStack(CMItems.PLANT_LINKER.get()));
        scene.idle(35);
        scene.overlay().showControls(util.vector().topOf(desk), Pointing.DOWN, 30).rightClick().withItem(new ItemStack(CMItems.PLANT_LINKER.get()));
        scene.idle(40);
        chapter(scene, 90, util.vector().topOf(desk), "A Dashboard Switch bound to the controller with the Plant Linker is a protection bypass");
        scene.overlay().showControls(util.vector().topOf(desk), Pointing.DOWN, 30).rightClick();
        scene.idle(15);
        scene.world().modifyBlockEntityNBT(deskOnly, DashboardBlockEntity.class, nbt -> {
            nbt.getCompound("Part0").putBoolean("On", true);
            nbt.getCompound("Part1").putInt("Alarms", 1 << PlantAlarms.Alarm.BYPASS.ordinal());
        }, true);
        scene.effects().indicateRedstone(desk);
        scene.idle(25);
        scene.overlay().showText(100).attachKeyFrame().colored(PonderPalette.RED).text("While it is on, none of those trips act. The reactor keeps running through what would have stopped it").pointAt(util.vector().blockSurface(controller, Direction.NORTH)).placeNearTarget();
        scene.idle(110);
        say(scene, 90, util.vector().blockSurface(controller, Direction.NORTH), "A SCRAM by hand still works, and so does the trip for a broken vessel");
        say(scene, 90, util.vector().topOf(desk), "An alarm lamp bound to the controller lights for it, and a Plant Screen lists BYPASS");
        say(scene, 80, util.vector().topOf(desk), "Fit the switch with a glass cover, so it is not flipped by a stray hand");
        scene.world().modifyBlockEntityNBT(deskOnly, DashboardBlockEntity.class, nbt -> {
            nbt.getCompound("Part0").putBoolean("On", false);
            nbt.getCompound("Part1").putInt("Alarms", 0);
        }, true);
        scene.idle(20);
        warn(scene, 110, util.vector().blockSurface(controller, Direction.NORTH), "With the protection off a loss of cooling can go all the way to a meltdown. With it on, it cannot");
        scene.markAsFinished();
    }

    public static void meltdown(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("meltdown", "How a core melts");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9f);
        scene.setSceneOffsetY(-1);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos fuel = util.grid().at(2, 3, 2);
        BlockPos other = util.grid().at(3, 3, 3);
        BlockPos floor = util.grid().at(2, 2, 2);
        BlockPos under = util.grid().at(2, 1, 2);
        scene.world().showSection(util.select().fromTo(2, 1, 2, 4, 2, 4), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(2, 3, 2, 4, 4, 4), Direction.DOWN);
        scene.idle(20);
        chapter(scene, 90, util.vector().centerOf(2, 4, 2), "A core is fuel columns with coolant channels and control rods beside them, inside a vessel of casing");
        say(scene, 100, util.vector().centerOf(2, 4, 2), "Above 800 °C fuel wears fast and the casing beside it cracks. The reactor trips at 1,100 °C, but decay heat keeps heating fuel that has no cooling");
        scene.world().setBlocks(util.select().fromTo(2, 3, 2, 2, 4, 2), CMBlocks.RUINED_CHANNEL.get().defaultBlockState(), true);
        scene.effects().emitParticles(util.vector().centerOf(2, 4, 2), scene.effects().simpleParticleEmitter(ParticleTypes.LAVA, Vec3.ZERO), 3, 10);
        scene.idle(20);
        scene.overlay().showText(100).attachKeyFrame().colored(PonderPalette.RED).text("At 1,800 °C a fuel column melts. Its channel blocks become Ruined Fuel Channels and the molten fuel pools on the vessel floor as corium").pointAt(util.vector().centerOf(fuel)).placeNearTarget();
        scene.idle(110);
        say(scene, 100, util.vector().topOf(floor), "Coolant in the vessel takes heat out of the corium, so keep the vessel flooded. An emergency injection line helps");
        for (int i = 0; i < 6; i++) {
            scene.world().incrementBlockBreakingProgress(floor);
            scene.idle(8);
        }
        scene.overlay().showText(90).attachKeyFrame().colored(PonderPalette.RED).text("Above 1,000 °C the corium eats the floor casing under it").pointAt(util.vector().centerOf(floor)).placeNearTarget();
        scene.idle(60);
        scene.world().destroyBlock(floor);
        scene.world().setBlock(under, CMBlocks.CORIUM.get().defaultBlockState().setValue(Corium.CoriumBlock.HOT, true), false);
        scene.world().showSection(util.select().position(under), Direction.DOWN);
        scene.idle(40);
        say(scene, 100, util.vector().centerOf(under), "When that block fails the corium leaves the vessel as hot Corium. It glows, burns, is a strong radiation source and contaminates the ground");
        say(scene, 100, util.vector().topOf(util.grid().at(2, 0, 2)), "It melts down through up to 3 solid blocks. Shielding Concrete and obsidian stop it, and water beside it cools it six times faster");
        scene.world().modifyBlock(under, s -> s.setValue(Corium.CoriumBlock.HOT, false), false);
        scene.idle(20);
        chapter(scene, 100, util.vector().centerOf(under), "Cooled, it is dark slag that can be mined. The slag and the ruined channels stay radioactive, as blocks and as items");
        scene.world().setBlocks(util.select().fromTo(3, 3, 3, 3, 4, 3), CMBlocks.RUINED_CHANNEL.get().defaultBlockState(), true);
        scene.effects().emitParticles(util.vector().centerOf(other), scene.effects().simpleParticleEmitter(ParticleTypes.LAVA, Vec3.ZERO), 3, 10);
        scene.idle(30);
        scene.overlay().showText(100).attachKeyFrame().colored(PonderPalette.RED).text("A core whose every fuel column has melted goes off as a nuclear explosion three seconds later").pointAt(util.vector().centerOf(3, 3, 3)).placeNearTarget();
        scene.idle(70);
        scene.effects().emitParticles(util.vector().centerOf(3, 3, 3), scene.effects().simpleParticleEmitter(ParticleTypes.EXPLOSION_EMITTER, Vec3.ZERO), 1, 3);
        scene.world().setBlocks(util.select().fromTo(2, 1, 2, 4, 4, 4), Blocks.AIR.defaultBlockState(), false);
        scene.idle(50);
        warn(scene, 100, util.vector().centerOf(3, 1, 3), "The trips and the fuel's own feedback stop every accident short of this. It takes a protection bypass to get here");
        scene.markAsFinished();
    }

    public static void fallout(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("fallout", "Fallout after a nuclear explosion");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.8f);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);
        chapter(scene, 110, util.vector().centerOf(4, 3, 4), "A nuclear explosion leaves a plume of fallout that the wind carries one way from the crater, hundreds of blocks long");
        Vec3 drift = new Vec3(0.03, -0.08, 0.01);
        for (double[] at : new double[][] {{1.5, 1.5}, {6.5, 1.5}, {3.5, 4.5}, {1.5, 7.5}, {7.5, 3.5}, {4.5, 7.5}}) {
            scene.effects().emitParticles(util.vector().of(at[0], 5.5, at[1]), scene.effects().particleEmitterWithinBlockSpace(CMParticles.FALLOUT.get(), drift), 2, 400);
        }
        scene.idle(20);
        say(scene, 100, util.vector().centerOf(4, 3, 4), "In the plume the air itself gives a dose, up to 900 mSv a minute near the crater. It halves every 5 minutes");
        for (int ring = 0; ring < 9; ring++) {
            for (int x = 0; x < 9; x++) {
                int z = (x * 5 + ring * 2) % 9;
                boolean roofed = x >= 5 && x <= 7 && z >= 5 && z <= 7;
                boolean taken = (x == 1 && z == 6);
                if (roofed || taken) {
                    continue;
                }
                BlockPos at = util.grid().at(x, 1, z);
                scene.world().setBlock(at, CMBlocks.FALLOUT_ASH.get().defaultBlockState(), false);
                scene.world().setBlock(at.below(), Blocks.DIRT.defaultBlockState(), false);
            }
            scene.idle(6);
        }
        scene.overlay().showText(100).attachKeyFrame().text("Fallout Ash settles across the plume for three minutes and kills the grass it lands on").pointAt(util.vector().topOf(util.grid().at(2, 0, 2))).placeNearTarget();
        scene.idle(110);
        say(scene, 100, util.vector().topOf(util.grid().at(2, 0, 2)), "The ash is contamination. It gives a dose until it is broken or washed off, and it does not decay");
        chapter(scene, 100, util.vector().centerOf(6, 1, 6), "A roof overhead cuts the dose from the air to three tenths");
        say(scene, 100, util.vector().centerOf(6, 1, 6), "A full protective outfit blocks most of the rest, since most of it is dust. A Survey Meter reads what is left");
        scene.world().destroyBlock(util.grid().at(4, 1, 6));
        scene.idle(8);
        scene.world().destroyBlock(util.grid().at(4, 1, 5));
        scene.idle(8);
        scene.world().destroyBlock(util.grid().at(4, 1, 7));
        scene.idle(20);
        say(scene, 90, util.vector().centerOf(4, 1, 6), "Break the ash to clear a way through");
        warn(scene, 100, util.vector().centerOf(1, 2, 6), "Animals and monsters caught in the open in the thick of it sicken and die");
        scene.markAsFinished();
    }
}
