package com.criticalmass.ponder;

import static com.criticalmass.ponder.PlantScenes.chapter;
import static com.criticalmass.ponder.PlantScenes.say;
import static com.criticalmass.ponder.PlantScenes.warn;

import com.criticalmass.registry.CMFluids;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public final class BoronScenes {
    private static final double GAUGE_SPAN = 2500;

    private BoronScenes() {
    }

    private static void level(CreateSceneBuilder scene, BlockPos tank, Fluid fluid, int amount) {
        scene.world().modifyBlockEntity(tank, FluidTankBlockEntity.class, be -> be.getTankInventory().setFluid(amount <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, amount)));
    }

    private static void ppm(CreateSceneBuilder scene, Selection desk, int value) {
        ControlScenes.reading(scene, desk, 0, value / GAUGE_SPAN, value + " ppm");
    }

    public static void boron(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("boron", "Borating and diluting the coolant");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.75f);
        scene.setSceneOffsetY(-1);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos controller = util.grid().at(5, 3, 3);
        BlockPos acidTank = util.grid().at(0, 2, 4);
        BlockPos acidPort = util.grid().at(4, 2, 4);
        BlockPos cleanTank = util.grid().at(0, 2, 6);
        BlockPos holdTank = util.grid().at(7, 2, 0);
        BlockPos holdPort = util.grid().at(7, 2, 3);
        Selection desk = util.select().position(2, 1, 1);
        Selection acidLine = util.select().fromTo(0, 1, 4, 3, 3, 4);
        Selection acidDrive = util.select().fromTo(1, 3, 4, 2, 3, 4).add(util.select().position(2, 2, 4));
        Selection cleanLine = util.select().fromTo(0, 1, 6, 3, 3, 6);
        Selection cleanDrive = util.select().fromTo(1, 3, 6, 2, 3, 6).add(util.select().position(2, 2, 6));
        Selection holdLine = util.select().fromTo(7, 1, 0, 7, 3, 2);
        Selection holdDrive = util.select().fromTo(7, 3, 0, 7, 3, 1).add(util.select().position(7, 2, 1));
        Fluid acid = CMFluids.BORIC_ACID.source.get();
        Fluid coolant = CMFluids.PRIMARY_COOLANT.source.get();
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        level(scene, acidTank, acid, 4000);
        level(scene, cleanTank, coolant, 4000);
        level(scene, holdTank, coolant, 0);
        ppm(scene, desk, 0);
        scene.world().showSection(util.select().fromTo(4, 1, 3, 8, 5, 7), Direction.DOWN);
        scene.idle(20);
        chapter(scene, 120, util.vector().blockSurface(controller, Direction.NORTH), "Boron dissolved in the primary coolant holds the reaction down, 7 pcm for each ppm. It is the slow control beside the rods");
        scene.world().showSection(desk, Direction.DOWN);
        scene.idle(15);
        say(scene, 90, util.vector().topOf(2, 1, 1), "A Dashboard Gauge bound to the controller reads it, and so does a Plant Screen");
        scene.world().showSection(acidLine, Direction.EAST);
        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(acidTank), Pointing.DOWN, 60).withItem(new ItemStack(CMItems.BORAX.get()));
        chapter(scene, 120, util.vector().blockSurface(acidTank, Direction.NORTH), "Crushed Scoria gives Borax, and Borax mixed with water in a heated basin gives Boric Acid. Keep it in a Fluid Tank");
        scene.overlay().showOutline(PonderPalette.GREEN, "port", util.select().position(acidPort), 110);
        say(scene, 110, util.vector().blockSurface(acidPort, Direction.WEST), "Run a Mechanical Pump from the tank to a spare inlet port on the vessel");
        scene.world().setKineticSpeed(acidDrive, 32);
        scene.effects().rotationSpeedIndicator(util.grid().at(2, 3, 4));
        scene.idle(10);
        for (int step = 1; step <= 10; step++) {
            level(scene, acidTank, acid, 4000 - step * 100);
            ppm(scene, desk, step * 100);
            scene.idle(5);
        }
        scene.world().setKineticSpeed(acidDrive, 0);
        scene.effects().indicateSuccess(util.grid().at(2, 1, 1));
        chapter(scene, 130, util.vector().blockSurface(acidPort, Direction.WEST), "The acid joins the coolant and its boron goes with it. 1,000 mB of acid into 6,000 mB of coolant leaves 1,000 ppm");
        scene.world().showSection(cleanLine, Direction.EAST);
        scene.idle(20);
        scene.world().setKineticSpeed(cleanDrive, 32);
        scene.effects().rotationSpeedIndicator(util.grid().at(2, 3, 6));
        scene.idle(10);
        for (int step = 1; step <= 5; step++) {
            level(scene, cleanTank, coolant, 4000 - step * 200);
            ppm(scene, desk, 1000 - step * 25);
            scene.idle(6);
        }
        scene.world().setKineticSpeed(cleanDrive, 0);
        chapter(scene, 120, util.vector().blockSurface(cleanTank, Direction.NORTH), "Clean Primary Coolant pumped in the same way thins it out again. Another 1,000 mB brings it down to 875 ppm");
        warn(scene, 120, util.vector().blockSurface(controller, Direction.NORTH), "Diluting a critical reactor adds reactivity as surely as pulling rods. Do it a little at a time and read the reactivity");
        scene.world().showSection(holdLine, Direction.SOUTH);
        scene.idle(20);
        scene.overlay().showOutline(PonderPalette.BLUE, "letdown", util.select().position(holdPort), 110);
        scene.world().setKineticSpeed(holdDrive, 32);
        scene.effects().rotationSpeedIndicator(util.grid().at(7, 3, 1));
        scene.idle(10);
        for (int step = 1; step <= 8; step++) {
            level(scene, holdTank, coolant, step * 250);
            scene.idle(5);
        }
        scene.world().setKineticSpeed(holdDrive, 0);
        chapter(scene, 130, util.vector().blockSurface(holdPort, Direction.NORTH), "A full vessel takes no more, so draw coolant off first. What is let down takes its share of the boron, and the reading stays where it was");
        say(scene, 110, util.vector().blockSurface(holdTank, Direction.WEST), "A Fluid Tank keeps the boron of the coolant in it. Pumped back in, it brings the same figure with it");
        say(scene, 120, util.vector().blockSurface(controller, Direction.NORTH), "Boron goes round the loop with the coolant. A leak takes it out, and coolant that boils off leaves it behind");
        scene.markAsFinished();
    }
}
