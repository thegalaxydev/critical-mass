package com.criticalmass.ponder;

import com.criticalmass.content.control.DashPart;
import com.criticalmass.content.control.DashboardBlockEntity;
import com.criticalmass.content.control.PlantAlarms;
import com.criticalmass.content.fuel.ServicePortBlockEntity;
import com.criticalmass.content.fuel.ShieldedCaskBlockEntity;
import com.criticalmass.content.fuel.SpentFuelRackBlock;
import com.criticalmass.content.fuel.SpentFuelRackBlockEntity;
import com.criticalmass.content.kinetics.ControlRodDriveBlockEntity;
import com.criticalmass.content.power.BreakerBlock;
import com.criticalmass.content.power.BreakerBlockEntity;
import com.criticalmass.content.power.TransferSwitchBlock;
import com.criticalmass.content.power.TransferSwitchBlockEntity;
import com.criticalmass.content.power.TransformerBlockEntity;
import com.criticalmass.content.power.WireLinks;
import com.criticalmass.content.power.WireType;
import com.criticalmass.content.thermal.ChannelBlocks;
import com.criticalmass.content.thermal.Corium;
import com.criticalmass.content.thermal.PressurizerBlockEntity;
import com.criticalmass.content.thermal.ReactorControllerBlock;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMItems;
import com.criticalmass.registry.CMParticles;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

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

    private static void glow(CreateSceneBuilder scene, Selection fuel, int level) {
        scene.world().modifyBlocks(fuel, s -> s.hasProperty(ChannelBlocks.Fuel.GLOW) ? s.setValue(ChannelBlocks.Fuel.GLOW, level) : s, false);
    }

    private static void status(CreateSceneBuilder scene, BlockPos controller, ReactorControllerBlock.Display shown) {
        scene.world().modifyBlock(controller, s -> s.setValue(ReactorControllerBlock.STATUS, shown), false);
    }

    private static void rod(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos drive, int x, int z, double insertion) {
        scene.world().modifyBlockEntityNBT(util.select().position(drive), ControlRodDriveBlockEntity.class, nbt -> {
            nbt.putInt("Travel", 3);
            nbt.putDouble("Insertion", insertion);
        });
        for (int i = 0; i < 3; i++) {
            int level = (int) Math.round(4 * Math.max(0, Math.min(1, insertion * 3 - i)));
            scene.world().modifyBlock(util.grid().at(x, 4 - i, z), s -> s.setValue(ChannelBlocks.Control.ROD, level), false);
        }
    }

    public static void vessel(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("reactor_vessel", "Building a reactor vessel");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.8f);
        scene.setSceneOffsetY(-1.5f);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos controller = util.grid().at(1, 3, 3);
        Selection fuel = util.select().fromTo(3, 2, 2, 3, 4, 2).add(util.select().fromTo(2, 2, 3, 2, 4, 3)).add(util.select().fromTo(4, 2, 3, 4, 4, 3)).add(util.select().fromTo(3, 2, 4, 3, 4, 4));
        Selection coolant = util.select().fromTo(2, 2, 2, 2, 4, 2).add(util.select().fromTo(4, 2, 2, 4, 4, 2)).add(util.select().fromTo(2, 2, 4, 2, 4, 4)).add(util.select().fromTo(4, 2, 4, 4, 4, 4));
        status(scene, controller, ReactorControllerBlock.Display.STANDBY);
        glow(scene, fuel, 0);
        scene.world().showSection(util.select().fromTo(1, 1, 1, 5, 1, 5), Direction.DOWN);
        scene.idle(20);
        chapter(scene, 90, util.vector().topOf(util.grid().at(3, 1, 3)), "A reactor vessel is a box of Reactor Vessel Casing, odd on every side, from 5 by 5 by 5 up to 11 by 11 by 13");
        scene.world().showSection(fuel, Direction.DOWN);
        scene.idle(20);
        chapter(scene, 90, util.vector().centerOf(3, 4, 2), "Inside stand columns of channels, one kind to a column. Fuel Channels hold the fuel assemblies");
        scene.world().showSection(coolant, Direction.DOWN);
        scene.idle(20);
        say(scene, 70, util.vector().centerOf(2, 4, 2), "Coolant Channels carry the heat away");
        scene.world().showSection(util.select().fromTo(3, 2, 3, 3, 4, 3), Direction.DOWN);
        scene.idle(20);
        say(scene, 100, util.vector().centerOf(3, 4, 3), "Control Channels take the control rods. Give every fuel column a coolant column and a control column beside it");
        scene.world().showSection(util.select().fromTo(1, 2, 5, 5, 4, 5).add(util.select().fromTo(5, 2, 1, 5, 4, 4)), Direction.DOWN);
        scene.idle(15);
        scene.world().showSection(util.select().fromTo(1, 2, 1, 1, 4, 4), Direction.EAST);
        scene.idle(20);
        chapter(scene, 90, util.vector().blockSurface(controller, Direction.WEST), "Exactly one Reactor Controller sits in a side wall, facing out");
        scene.world().showSection(util.select().fromTo(2, 2, 1, 4, 4, 1), Direction.SOUTH);
        scene.idle(20);
        say(scene, 100, util.vector().blockSurface(util.grid().at(4, 3, 1), Direction.NORTH), "A Primary Coolant Port set to outlet and one set to inlet go in the walls, facing out");
        scene.overlay().showControls(util.vector().blockSurface(util.grid().at(2, 3, 1), Direction.NORTH), Pointing.RIGHT, 40).rightClick().withItem(new ItemStack(AllItems.WRENCH.get()));
        scene.idle(10);
        say(scene, 70, util.vector().blockSurface(util.grid().at(2, 3, 1), Direction.NORTH), "A wrench switches a port between the two");
        scene.world().showSection(util.select().fromTo(1, 5, 1, 5, 5, 5), Direction.DOWN);
        scene.idle(20);
        chapter(scene, 90, util.vector().topOf(util.grid().at(3, 5, 3)), "A Reactor Service Port goes in the top, facing up. Fuel goes in and comes out through it");
        scene.overlay().showOutline(PonderPalette.RED, "edges", util.select().fromTo(1, 5, 1, 5, 5, 1).add(util.select().fromTo(1, 1, 1, 1, 5, 1)).add(util.select().fromTo(1, 5, 1, 1, 5, 5)), 90);
        warn(scene, 90, util.vector().centerOf(1, 5, 1), "Only casing may sit on the edges and corners");
        status(scene, controller, ReactorControllerBlock.Display.RUNNING);
        glow(scene, fuel, 1);
        scene.idle(15);
        glow(scene, fuel, 2);
        chapter(scene, 100, util.vector().blockSurface(util.grid().at(3, 3, 1), Direction.NORTH), "A Reactor Viewport takes the place of any casing block. Through it the fuel is dark when idle and glows blue at power");
        say(scene, 100, util.vector().blockSurface(controller, Direction.WEST), "The controller's face shows the state: amber when shut down, green when running and red when tripped");
        scene.overlay().showControls(util.vector().blockSurface(controller, Direction.WEST), Pointing.LEFT, 60).withItem(new ItemStack(AllItems.GOGGLES.get()));
        scene.idle(10);
        say(scene, 100, util.vector().blockSurface(controller, Direction.WEST), "With goggles on, the controller lists what is wrong or missing, and the blocks at fault are outlined in red");
        scene.markAsFinished();
    }

    public static void rods(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("rods", "Control rods and their drives");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.7f);
        scene.setSceneOffsetY(-2);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos controller = util.grid().at(1, 3, 3);
        BlockPos drive = util.grid().at(3, 6, 2);
        BlockPos cog = util.grid().at(2, 6, 2);
        Selection fuel = util.select().fromTo(4, 2, 2, 4, 4, 2);
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        status(scene, controller, ReactorControllerBlock.Display.STANDBY);
        glow(scene, fuel, 0);
        rod(scene, util, drive, 3, 2, 1);
        scene.world().showSection(util.select().fromTo(1, 1, 1, 5, 5, 5).substract(util.select().fromTo(2, 2, 1, 4, 4, 1)), Direction.DOWN);
        scene.idle(25);
        chapter(scene, 100, util.vector().centerOf(3, 3, 2), "This vessel is shown with a wall open. A control rod fully in its column holds down the fuel columns beside it");
        scene.world().showSection(util.select().position(drive), Direction.DOWN);
        scene.idle(20);
        chapter(scene, 90, util.vector().centerOf(drive), "A Control Rod Drive stands on the vessel top over a control column and moves that column's rod");
        scene.world().showSection(util.select().position(cog), Direction.EAST);
        scene.idle(15);
        scene.world().setKineticSpeed(util.select().position(drive), 32);
        scene.world().setKineticSpeed(util.select().position(cog), -32);
        scene.effects().rotationSpeedIndicator(cog);
        scene.idle(20);
        say(scene, 100, util.vector().centerOf(cog), "It is turned by the cogwheel round its middle. Mesh a Cogwheel beside it, or set drives side by side and they turn each other");
        scene.effects().indicateRedstone(drive);
        scene.overlay().showText(110).attachKeyFrame().text("A redstone signal withdraws the rod, faster with shaft speed. The rod stands up out of the drive as it leaves the core").pointAt(util.vector().topOf(drive)).placeNearTarget();
        for (int step = 1; step <= 12; step++) {
            rod(scene, util, drive, 3, 2, 1 - step / 12.0);
            scene.idle(6);
            if (step == 6) {
                glow(scene, fuel, 1);
                status(scene, controller, ReactorControllerBlock.Display.RUNNING);
            }
        }
        glow(scene, fuel, 2);
        scene.idle(50);
        say(scene, 90, util.vector().centerOf(4, 3, 2), "With the rods out the chain reaction grows, and the fuel glows blue");
        say(scene, 100, util.vector().blockSurface(controller, Direction.WEST), "Rods will not withdraw until coolant is flowing through the vessel and the vessel is at least half full");
        scene.overlay().showControls(util.vector().blockSurface(drive, Direction.NORTH), Pointing.RIGHT, 50).rightClick();
        scene.idle(10);
        chapter(scene, 110, util.vector().blockSurface(drive, Direction.NORTH), "Hold use on the drive's upper case to put it in a bank, A to D. A bank target set from the control desk moves every drive in the bank together");
        scene.world().setKineticSpeed(util.select().position(drive).add(util.select().position(cog)), 0);
        scene.overlay().showText(100).attachKeyFrame().colored(PonderPalette.RED).text("Losing shaft power, overstress or a trip lets go of the drive's latch, and the rod drops fully in within 2 seconds").pointAt(util.vector().topOf(drive)).placeNearTarget();
        for (int step = 1; step <= 8; step++) {
            rod(scene, util, drive, 3, 2, step / 8.0);
            scene.idle(3);
        }
        status(scene, controller, ReactorControllerBlock.Display.TRIPPED);
        glow(scene, fuel, 1);
        scene.idle(90);
        warn(scene, 100, util.vector().centerOf(4, 3, 2), "Fuel still gives decay heat after a trip: 7%% of its power at first, halving every minute. Keep the pumps running");
        scene.markAsFinished();
    }

    public static void primaryLoop(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("primary_loop", "The primary coolant loop");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.65f);
        scene.setSceneOffsetY(-1.5f);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos controller = util.grid().at(6, 3, 4);
        BlockPos outlet = util.grid().at(4, 2, 6);
        BlockPos inlet = util.grid().at(5, 2, 4);
        BlockPos pump = util.grid().at(2, 2, 6);
        BlockPos exchanger = util.grid().at(1, 2, 4);
        BlockPos pressurizer = util.grid().at(7, 2, 3);
        Selection drive = util.select().fromTo(2, 1, 6, 2, 2, 6);
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        status(scene, controller, ReactorControllerBlock.Display.STANDBY);
        scene.world().showSection(util.select().fromTo(4, 1, 4, 8, 5, 8), Direction.DOWN);
        scene.idle(25);
        chapter(scene, 110, util.vector().blockSurface(outlet, Direction.WEST), "The primary loop carries the core's heat away: out of the outlet port, through the machines on the way and back in at the inlet port");
        scene.world().showSection(util.select().position(3, 2, 6).add(drive), Direction.EAST);
        scene.idle(20);
        scene.world().setKineticSpeed(drive, 64);
        scene.effects().rotationSpeedIndicator(pump);
        scene.idle(10);
        chapter(scene, 110, util.vector().topOf(pump), "A Circulation Pump pushes coolant out of its front: 2 mB a tick for each RPM, up to 256 mB a tick. It stops when its shaft stops");
        scene.world().showSection(util.select().fromTo(1, 2, 5, 1, 2, 6), Direction.EAST);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(1, 1, 4, 1, 4, 4), Direction.DOWN);
        scene.idle(20);
        chapter(scene, 110, util.vector().blockSurface(exchanger, Direction.WEST), "A Heat Exchanger takes the primary through its front and back, and the secondary through its top and bottom. The two never mix");
        say(scene, 100, util.vector().blockSurface(exchanger.above(), Direction.WEST), "Heat crosses only while both sides are flowing. Exchangers stacked on one another work as one column of up to 8");
        scene.world().showSection(util.select().fromTo(1, 2, 2, 1, 2, 3), Direction.SOUTH);
        scene.idle(8);
        scene.world().showSection(util.select().fromTo(2, 2, 2, 5, 2, 2), Direction.SOUTH);
        scene.idle(8);
        scene.world().showSection(util.select().position(5, 2, 3), Direction.SOUTH);
        scene.idle(20);
        say(scene, 80, util.vector().blockSurface(inlet, Direction.NORTH), "A loop must be one path, with no branches");
        scene.overlay().showControls(util.vector().topOf(util.grid().at(3, 2, 2)), Pointing.DOWN, 50).rightClick().withItem(new ItemStack(CMItems.REACTOR_ALLOY_INGOT.get()));
        scene.idle(10);
        chapter(scene, 100, util.vector().topOf(util.grid().at(3, 2, 2)), "Plumb the loop with Create's Fluid Pipes, then use a Reactor Alloy Ingot on each one to reinforce it");
        warn(scene, 100, util.vector().topOf(util.grid().at(3, 2, 2)), "A plain pipe leaks under pressure, and gives off radiation while it holds primary coolant");
        scene.world().showSection(util.select().fromTo(7, 1, 3, 7, 3, 3), Direction.DOWN);
        scene.idle(25);
        chapter(scene, 120, util.vector().blockSurface(pressurizer, Direction.WEST), "Vessel coolant boils at 350 °C and is lost as steam. A Pressurizer touching the vessel raises the pressure, and every bar adds 2 °C to the boiling point");
        say(scene, 100, util.vector().centerOf(7, 1, 3), "Its heaters need a lit Blaze Burner directly underneath. Its value box sets the pressure, up to 150 bar");
        scene.overlay().showControls(util.vector().blockSurface(pressurizer, Direction.WEST), Pointing.LEFT, 40).rightClick().withItem(new ItemStack(CMItems.DASHBOARD_PARTS.get(DashPart.Type.GAUGE).get()));
        scene.idle(20);
        for (int bar = 1; bar <= 100; bar += 11) {
            double shown = bar;
            scene.world().modifyBlockEntityNBT(util.select().position(pressurizer), PressurizerBlockEntity.class, nbt -> {
                nbt.putString("GaugeFace", "west");
                nbt.putDouble("Pressure", shown);
            });
            scene.idle(4);
        }
        say(scene, 90, util.vector().blockSurface(pressurizer, Direction.WEST), "Use a Dashboard Gauge on one of its sides to read the pressure");
        warn(scene, 100, util.vector().centerOf(7, 1, 3), "With the burner out the pressure falls back and hot coolant boils away, so a hot plant depends on that burner");
        status(scene, controller, ReactorControllerBlock.Display.RUNNING);
        scene.idle(10);
        say(scene, 110, util.vector().blockSurface(controller, Direction.NORTH), "The reactor will not start until coolant flows through the vessel, and it trips when the flow has stopped for 2 seconds");
        scene.markAsFinished();
    }

    public static void steam(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("steam", "The steam loop, the turbine and the condenser");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.65f);
        scene.setSceneOffsetY(-2);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos exchanger = util.grid().at(7, 3, 4);
        BlockPos relief = util.grid().at(7, 5, 4);
        BlockPos turbine = util.grid().at(4, 5, 4);
        BlockPos condenser = util.grid().at(1, 2, 4);
        BlockPos pump = util.grid().at(5, 2, 2);
        Selection train = util.select().fromTo(1, 5, 4, 4, 5, 4);
        Selection feed = util.select().fromTo(5, 1, 2, 5, 2, 2);
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        scene.world().showSection(util.select().fromTo(6, 3, 4, 8, 3, 4), Direction.DOWN);
        scene.idle(20);
        chapter(scene, 110, util.vector().blockSurface(exchanger, Direction.NORTH), "The secondary loop starts in the Heat Exchanger. Its water boils from 100 °C, and the steam runs from 1 bar up to 10 bar when it has all boiled");
        scene.world().showSection(util.select().fromTo(7, 4, 4, 7, 5, 4).add(util.select().position(6, 5, 4)), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(3, 4, 3, 5, 6, 5).add(util.select().fromTo(1, 5, 4, 2, 5, 4)), Direction.DOWN);
        scene.idle(25);
        chapter(scene, 120, util.vector().topOf(util.grid().at(4, 6, 4)), "A Steam Turbine fills a three-block cube. Steam goes in at the back, the exhaust leaves through the bottom or the top, and the shaft comes out of the front");
        scene.world().setKineticSpeed(train, 16);
        scene.effects().rotationSpeedIndicator(util.grid().at(2, 5, 4));
        scene.idle(20);
        say(scene, 110, util.vector().centerOf(2, 5, 4), "Its shaft is the plant's output: 4 SU for each HU/t of steam it takes. Its value box sets the speed, from 1 to 256 RPM");
        say(scene, 100, util.vector().blockSurface(turbine.north(), Direction.NORTH), "A governor follows the load on the shaft, so the turbine takes only the steam its load needs");
        scene.world().setKineticSpeed(train, 0);
        scene.effects().indicateRedstone(util.grid().at(3, 5, 4));
        warn(scene, 110, util.vector().centerOf(2, 5, 4), "Lose the whole load at once and the rotor overspeeds and trips. A full redstone signal, taken away again, resets it");
        scene.world().setKineticSpeed(train, 16);
        scene.world().showSection(util.select().fromTo(4, 2, 4, 4, 3, 4).add(util.select().fromTo(2, 2, 4, 3, 2, 4)), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(1, 1, 4, 1, 3, 4), Direction.DOWN);
        scene.idle(25);
        chapter(scene, 120, util.vector().blockSurface(condenser, Direction.NORTH), "A Condenser turns the exhaust back into water. Steam passes through it end to end and cooling water top to bottom, and it condenses only while the cooling water flows");
        scene.world().showSection(util.select().fromTo(0, 2, 2, 0, 2, 4), Direction.EAST);
        scene.idle(8);
        scene.world().showSection(util.select().fromTo(1, 2, 2, 4, 2, 2).add(feed), Direction.SOUTH);
        scene.idle(8);
        scene.world().showSection(util.select().fromTo(6, 2, 2, 7, 2, 2).add(util.select().fromTo(7, 2, 3, 7, 2, 4)), Direction.SOUTH);
        scene.idle(15);
        scene.world().setKineticSpeed(feed, 64);
        scene.effects().rotationSpeedIndicator(pump);
        scene.idle(10);
        say(scene, 100, util.vector().topOf(pump), "A Circulation Pump sends the water back into the bottom of the exchanger, and the loop is closed");
        Vec3 mouth = util.vector().blockSurface(relief, Direction.NORTH);
        scene.effects().emitParticles(mouth, scene.effects().simpleParticleEmitter(CMParticles.STEAM.get(), new Vec3(0, 0.02, -0.12)), 3, 100);
        scene.effects().indicateRedstone(relief);
        chapter(scene, 110, mouth, "A Relief Valve fits inline. It vents through its front from 8 bar down to 6 bar, and gives a redstone signal while it vents");
        warn(scene, 100, util.vector().centerOf(6, 5, 4), "Any pipe bursts when the steam in it passes 12 bar. Put a Relief Valve in every steam loop");
        scene.world().setKineticSpeed(train, 0);
        scene.overlay().showControls(util.vector().blockSurface(turbine.north(), Direction.NORTH), Pointing.RIGHT, 50).rightClick().withItem(new ItemStack(CMItems.TURBINE_BLADE.get()));
        scene.idle(10);
        chapter(scene, 110, util.vector().blockSurface(turbine.north(), Direction.NORTH), "The blades wear while steam passes. Stop the turbine and use Turbine Blades on it: each one gives back a quarter");
        scene.world().setKineticSpeed(train, 16);
        scene.markAsFinished();
    }

    public static void cooling(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("cooling", "Rejecting heat: the cooling tower and the radiator");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.75f);
        scene.setSceneOffsetY(-1.5f);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos top = util.grid().at(3, 5, 3);
        BlockPos bottom = util.grid().at(3, 2, 3);
        BlockPos fan = util.grid().at(3, 6, 3);
        BlockPos radiator = util.grid().at(5, 2, 1);
        Selection blower = util.select().fromTo(3, 6, 3, 3, 7, 3);
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        scene.world().showSection(util.select().fromTo(3, 1, 0, 3, 1, 3), Direction.DOWN);
        scene.idle(20);
        chapter(scene, 90, util.vector().centerOf(3, 1, 1), "A cooling tower is built from the bottom. First comes a pipe to carry the cooled water away");
        for (int y = 2; y <= 5; y++) {
            scene.world().showSection(util.select().position(3, y, 3), Direction.DOWN);
            scene.idle(6);
        }
        scene.idle(15);
        say(scene, 100, util.vector().blockSurface(util.grid().at(3, 4, 3), Direction.WEST), "Then up to 12 blocks of Cooling Tower Packing standing on that pipe. Stacked, they join into one glazed column");
        scene.world().showSection(util.select().fromTo(3, 5, 0, 3, 5, 2), Direction.SOUTH);
        scene.idle(20);
        say(scene, 100, util.vector().centerOf(3, 5, 1), "The hot water from the condenser goes into the side of the top block. It falls through the fill and leaves underneath");
        say(scene, 90, util.vector().blockSurface(util.grid().at(3, 3, 3), Direction.WEST), "The water inside shows how full the whole stack is, and the stack reads as one tank");
        scene.effects().emitParticles(util.vector().topOf(top).add(0, 0.3, 0), scene.effects().simpleParticleEmitter(CMParticles.STEAM.get(), new Vec3(0, 0.06, 0)), 1, 110);
        chapter(scene, 110, util.vector().topOf(top), "Open to the sky it draws by itself: 25%% of full airflow, and 5%% more for each extra block, up to 75%%");
        scene.world().showSection(blower, Direction.DOWN);
        scene.idle(15);
        scene.world().setKineticSpeed(blower, -64);
        scene.effects().rotationSpeedIndicator(fan.above());
        scene.idle(15);
        chapter(scene, 120, util.vector().blockSurface(fan, Direction.WEST), "An Encased Fan on top adds 100%% at 64 RPM and needs no open sky. It has to blow the air up and out of the tower: reverse it if it blows down into the fill");
        say(scene, 110, util.vector().blockSurface(fan, Direction.WEST), "A fan facing down is driven from above. One facing up is driven from below, through the vertical shaft that every fill block carries");
        say(scene, 100, util.vector().blockSurface(bottom, Direction.WEST), "Rejected heat evaporates water, 1 mB for every 2,000 HU. Pump make-up water into the side of the bottom block");
        warn(scene, 100, util.vector().topOf(fan.above()), "The air leaving the top is as hot as the water. Above 50 °C it scalds anyone standing in it");
        scene.world().showSection(util.select().fromTo(5, 1, 1, 5, 3, 1), Direction.DOWN);
        scene.idle(20);
        chapter(scene, 110, util.vector().blockSurface(radiator, Direction.WEST), "A Radiator loses no water. Its pipes go on top and bottom, and it rejects heat through each of its four sides that is open to the air");
        scene.markAsFinished();
    }

    public static void refuelling(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("refuelling", "Refuelling and storing spent fuel");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos port = util.grid().at(4, 2, 4);
        BlockPos handle = util.grid().at(4, 3, 4);
        BlockPos rack = util.grid().at(1, 1, 4);
        BlockPos cask = util.grid().at(4, 1, 1);
        Selection turned = util.select().fromTo(4, 2, 4, 4, 3, 4);
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        scene.world().modifyBlock(rack, s -> s.setValue(SpentFuelRackBlock.FILL, 0), false);
        scene.world().showSection(util.select().fromTo(3, 1, 3, 5, 2, 5), Direction.DOWN);
        scene.idle(20);
        chapter(scene, 100, util.vector().topOf(port), "Fuel goes in and comes out through the Reactor Service Port in the vessel top. Its hatch is shut until it is turned open");
        scene.world().showSection(util.select().position(handle), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(handle), Pointing.DOWN, 30).rightClick();
        scene.idle(10);
        scene.world().setKineticSpeed(turned, 32);
        scene.world().modifyBlockEntity(port, ServicePortBlockEntity.class, be -> be.onSpeedChanged(0));
        scene.idle(30);
        scene.world().setKineticSpeed(turned, 0);
        say(scene, 110, util.vector().topOf(handle), "Put a Valve Handle on top and use it to open the hatch. Sneak and use it to shut it again. A shaft from above does the same, by its direction");
        scene.world().hideSection(util.select().position(handle), Direction.UP);
        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(port), Pointing.DOWN, 50).rightClick().withItem(new ItemStack(CMItems.FRESH_FUEL_ASSEMBLY.get()));
        scene.idle(10);
        say(scene, 110, util.vector().topOf(port), "Use a Fuel Assembly on the open port to load a fuel column, and an empty hand to take one out. Funnels, hoppers and Mechanical Arms do both");
        scene.world().showSection(util.select().position(rack), Direction.DOWN);
        scene.idle(20);
        chapter(scene, 90, util.vector().topOf(rack), "A Spent Fuel Rack has four cells. Use an assembly on it to stand it in a cell");
        for (int held = 1; held <= 3; held++) {
            int cells = held;
            scene.overlay().showControls(util.vector().topOf(rack), Pointing.DOWN, 15).rightClick().withItem(new ItemStack(CMItems.SPENT_FUEL_ASSEMBLY.get()));
            scene.idle(10);
            scene.world().modifyBlock(rack, s -> s.setValue(SpentFuelRackBlock.FILL, cells), false);
            scene.idle(12);
        }
        scene.overlay().showControls(util.vector().topOf(rack), Pointing.DOWN, 40).rightClick().withItem(new ItemStack(Items.WATER_BUCKET));
        for (int amount = 500; amount <= 4000; amount += 500) {
            int water = amount;
            scene.world().modifyBlockEntityNBT(util.select().position(rack), SpentFuelRackBlockEntity.class, nbt -> {
                CompoundTag fluid = new CompoundTag();
                fluid.putString("id", "minecraft:water");
                fluid.putInt("amount", water);
                CompoundTag buffer = new CompoundTag();
                buffer.put("Fluid", fluid);
                buffer.putDouble("Heat", 0);
                nbt.put("Buffer", buffer);
            });
            scene.idle(5);
        }
        say(scene, 110, util.vector().blockSurface(rack, Direction.WEST), "Spent fuel is hot and radioactive. Fill the rack with water by bucket or pipe: a flooded rack lets only a tenth of the dose out");
        warn(scene, 100, util.vector().blockSurface(rack, Direction.WEST), "A rack that runs dry lets its fuel heat up and wear. The water shows at its real level");
        scene.world().showSection(util.select().position(cask), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(cask), Pointing.DOWN, 40).rightClick().withItem(new ItemStack(AllItems.WRENCH.get()));
        scene.idle(10);
        scene.world().modifyBlockEntity(cask, ShieldedCaskBlockEntity.class, ShieldedCaskBlockEntity::toggle);
        scene.idle(20);
        chapter(scene, 110, util.vector().topOf(cask), "Cooled assemblies and waste canisters go into a Shielded Cask. Use a wrench to unbolt its lid, and again to bolt it down");
        say(scene, 100, util.vector().topOf(cask), "It takes and gives items only with the lid up, and a redstone signal holds the lid up for funnels and Mechanical Arms");
        scene.world().modifyBlockEntity(cask, ShieldedCaskBlockEntity.class, ShieldedCaskBlockEntity::toggle);
        scene.idle(25);
        warn(scene, 100, util.vector().topOf(cask), "Bolted down it lets 1%% of the dose out. With the lid up it lets out half");
        scene.markAsFinished();
    }

    private static void level(CreateSceneBuilder scene, BlockPos tank, int amount) {
        scene.world().modifyBlockEntity(tank, FluidTankBlockEntity.class, be -> be.getTankInventory().setFluid(new FluidStack(Fluids.WATER, amount)));
    }

    public static void feedwater(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("feedwater", "The feedwater train");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.75f);
        scene.setSceneOffsetY(-1);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos heater = util.grid().at(6, 2, 3);
        BlockPos condenser = util.grid().at(4, 2, 3);
        BlockPos tank = util.grid().at(2, 2, 3);
        BlockPos reader = util.grid().at(2, 2, 2);
        BlockPos pump = util.grid().at(3, 2, 5);
        BlockPos makeup = util.grid().at(1, 2, 3);
        Selection feed = util.select().fromTo(3, 1, 5, 3, 2, 5);
        Selection topUp = util.select().fromTo(1, 2, 3, 1, 3, 3);
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        level(scene, tank, 6000);
        scene.world().showSection(util.select().fromTo(7, 2, 3, 8, 2, 3).add(util.select().position(5, 2, 3)), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(4, 1, 3, 4, 3, 3).add(util.select().position(3, 2, 3)), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(tank), Direction.DOWN);
        scene.idle(20);
        chapter(scene, 110, util.vector().blockSurface(tank, Direction.NORTH), "Condensate from the Condenser collects in a Create Fluid Tank set in the loop. That tank is the plant's reserve of feedwater");
        say(scene, 90, util.vector().blockSurface(tank, Direction.NORTH), "It needs the loop's two pipes on it, and the water in it keeps its temperature");
        scene.world().showSection(util.select().position(reader), Direction.SOUTH);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(0, 2, 3, 1, 2, 3).add(util.select().fromTo(0, 3, 3, 1, 3, 3)), Direction.EAST);
        scene.idle(20);
        scene.overlay().showText(140).attachKeyFrame().text("A Threshold Switch on the tank reads how full it is. Lead its signal to the Clutch on a make-up pump, and the tank is topped up whenever it runs low").pointAt(util.vector().blockSurface(reader, Direction.NORTH)).placeNearTarget();
        for (int amount = 5500; amount >= 1500; amount -= 500) {
            level(scene, tank, amount);
            scene.idle(4);
        }
        scene.effects().indicateRedstone(reader);
        scene.world().setKineticSpeed(topUp, 32);
        scene.effects().rotationSpeedIndicator(makeup.above());
        scene.idle(15);
        for (int amount = 2000; amount <= 7000; amount += 500) {
            level(scene, tank, amount);
            scene.idle(5);
        }
        scene.effects().indicateSuccess(reader);
        scene.world().setKineticSpeed(topUp, 0);
        scene.idle(40);
        scene.world().showSection(util.select().fromTo(2, 2, 4, 2, 2, 5).add(feed), Direction.NORTH);
        scene.idle(10);
        scene.world().setKineticSpeed(feed, 32);
        scene.effects().rotationSpeedIndicator(pump);
        scene.idle(15);
        chapter(scene, 90, util.vector().topOf(pump), "The feed pump is a Circulation Pump, and it has to be fed water");
        scene.effects().emitParticles(util.vector().blockSurface(pump, Direction.WEST), scene.effects().simpleParticleEmitter(CMParticles.STEAM.get(), new Vec3(-0.03, 0.04, 0)), 2, 90);
        warn(scene, 120, util.vector().topOf(pump), "If what reaches it is boiling it cavitates: it loses the share of its flow that is steam, down to a tenth. Keep cooling water on the Condenser");
        say(scene, 90, util.vector().topOf(pump), "An alarm lamp bound to a pump lights while it cavitates or stands still");
        scene.world().showSection(util.select().fromTo(4, 2, 5, 6, 2, 5).add(util.select().fromTo(6, 1, 3, 6, 1, 5)), Direction.NORTH);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(6, 2, 3, 6, 4, 3), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showOutline(PonderPalette.GREEN, "heater", util.select().position(heater), 220);
        chapter(scene, 130, util.vector().blockSurface(heater, Direction.NORTH), "A Heat Exchanger that the same loop crosses twice is a feedwater heater. The steam on its way to the Condenser passes through one side, and the feedwater through the other");
        say(scene, 120, util.vector().centerOf(6, 3, 3), "It warms the feedwater back up towards boiling, so less of the reactor's heat goes on heating water and more of it makes steam");
        scene.markAsFinished();
    }

    private static Vec3 arc(Vec3 from, Vec3 to, double along) {
        Vec3 flat = from.lerp(to, along);
        return flat.add(0, 4 * 1.2 * along * (1 - along), 0);
    }

    private static void jet(CreateSceneBuilder scene, Vec3 from, Vec3 to) {
        for (int i = 1; i <= 5; i++) {
            scene.effects().emitParticles(arc(from, to, i / 6.0), scene.effects().simpleParticleEmitter(ParticleTypes.SPLASH, Vec3.ZERO), 2, 1);
        }
        scene.effects().emitParticles(to.add(0, 0.2, 0), scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(0, 0.02, 0)), 1, 1);
    }

    public static void waterCannon(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("water_cannon", "Washing down with a Water Cannon");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.8f);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos cannon = util.grid().at(4, 2, 4);
        Selection drive = util.select().fromTo(4, 1, 4, 4, 1, 8);
        Selection supply = util.select().fromTo(4, 2, 0, 4, 2, 3);
        Selection head = util.select().position(cannon);
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        scene.world().showSection(util.select().fromTo(0, 1, 0, 3, 1, 8).add(util.select().fromTo(5, 1, 0, 8, 1, 8)), Direction.DOWN);
        scene.world().showSection(drive, Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(head, Direction.DOWN);
        scene.idle(20);
        chapter(scene, 90, util.vector().topOf(cannon), "A Water Cannon is a turret. A shaft from below turns its head");
        scene.world().showSection(supply, Direction.SOUTH);
        scene.idle(20);
        say(scene, 90, util.vector().centerOf(4, 2, 2), "Water goes in by pipe on any side, or from a bucket. It holds 4,000 mB");
        for (int ring = 0; ring < 9; ring++) {
            for (int x = 0; x < 9; x++) {
                int z = (x * 5 + ring * 2) % 9;
                if (x != 4) {
                    scene.world().setBlock(util.grid().at(x, 1, z), CMBlocks.FALLOUT_ASH.get().defaultBlockState(), false);
                }
            }
            scene.idle(4);
        }
        scene.idle(20);
        say(scene, 80, util.vector().topOf(util.grid().at(2, 0, 2)), "After an explosion the ground is covered in Fallout Ash");
        scene.world().setKineticSpeed(drive, 32);
        scene.world().setKineticSpeed(head, 32);
        scene.world().setKineticSpeed(supply, 32);
        scene.effects().rotationSpeedIndicator(util.grid().at(4, 1, 6));
        scene.overlay().showText(140).attachKeyFrame().text("While it turns it washes the ash off the top of everything within its reach, and puts out fire there").pointAt(util.vector().topOf(cannon)).placeNearTarget();
        Vec3 nozzle = util.vector().centerOf(cannon).add(0, 0.8, 0);
        double perTick = 32 * 0.075;
        for (int step = 0; step < 150; step++) {
            double low = step * perTick;
            double aim = Math.toRadians(low);
            jet(scene, nozzle, util.vector().of(4.5 - Math.sin(aim) * 3.2, 1.1, 4.5 - Math.cos(aim) * 3.2));
            for (int x = 0; x < 9; x++) {
                for (int z = 0; z < 9; z++) {
                    double bearing = (Math.toDegrees(Math.atan2(4 - x, 4 - z)) + 360) % 360;
                    if (x != 4 && bearing >= low && bearing < low + perTick) {
                        scene.world().setBlock(util.grid().at(x, 1, z), Blocks.AIR.defaultBlockState(), false);
                    }
                }
            }
            scene.idle(1);
        }
        scene.idle(20);
        say(scene, 90, util.vector().blockSurface(cannon, Direction.NORTH), "Its value box sets the reach, from 4 to 24 blocks. Each block washed takes 10 mB of water");
        chapter(scene, 100, util.vector().topOf(util.grid().at(2, 0, 2)), "Washed ash is gone and leaves nothing to collect. Clear ash by hand, with flowing water or with a Mechanical Plough to keep it");
        say(scene, 90, util.vector().topOf(cannon), "With nothing left in reach it stops by itself and keeps its water");
        say(scene, 100, util.vector().topOf(cannon), "On a moving Sable body it washes the ground under where the body is now");
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
        say(scene, 110, util.vector().centerOf(4, 1, 6), "Break the ash to clear a way through. Flowing water washes it off, and a Mechanical Plough on a moving contraption sweeps it up");
        say(scene, 110, util.vector().centerOf(4, 1, 6), "Ash cleared these ways is left as Fallout Ash. A Water Cannon washes it off everything within 24 blocks and leaves nothing");
        warn(scene, 100, util.vector().centerOf(1, 2, 6), "Animals and monsters caught in the open in the thick of it sicken and die");
        scene.markAsFinished();
    }
}
