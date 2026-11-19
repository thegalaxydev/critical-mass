package com.criticalmass.ponder;

import static com.criticalmass.ponder.PlantScenes.chapter;
import static com.criticalmass.ponder.PlantScenes.say;
import static com.criticalmass.ponder.PlantScenes.warn;

import com.criticalmass.content.fuel.SpentFuelRackBlock;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMFluids;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingBehaviour;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

public final class FuelScenes {
    private FuelScenes() {
    }

    private static void hold(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos deployer, ItemStack stack) {
        scene.world().modifyBlockEntityNBT(util.select().position(deployer), DeployerBlockEntity.class,
                nbt -> nbt.put("HeldItem", stack.saveOptional(scene.world().getHolderLookupProvider())));
    }

    private static void deploy(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos deployer, ItemStack stack, BlockPos depot, ItemStack result) {
        hold(scene, util, deployer, stack);
        scene.idle(10);
        scene.world().moveDeployer(deployer, 1, 20);
        scene.idle(21);
        hold(scene, util, deployer, ItemStack.EMPTY);
        scene.world().removeItemsFromBelt(depot);
        scene.world().createItemOnBeltLike(depot, Direction.UP, result);
        scene.world().moveDeployer(deployer, -1, 20);
        scene.idle(25);
    }

    private static void press(CreateSceneBuilder scene, BlockPos press, BlockPos depot, Vec3 at, ItemStack before, ItemStack after) {
        scene.world().modifyBlockEntity(press, MechanicalPressBlockEntity.class, p -> p.getPressingBehaviour().start(PressingBehaviour.Mode.BELT));
        scene.idle(15);
        scene.world().modifyBlockEntity(press, MechanicalPressBlockEntity.class, p -> p.getPressingBehaviour().makePressingParticleEffect(at.add(0, 0.5, 0), before));
        scene.world().removeItemsFromBelt(depot);
        scene.world().createItemOnBeltLike(depot, Direction.UP, after);
        scene.idle(25);
    }

    public static void enrichment(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("enrichment", "Enriching uranium in a Centrifuge");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos low = util.grid().at(3, 1, 3);
        Selection stack = util.select().fromTo(3, 1, 3, 3, 4, 3);
        Vec3 front = util.vector().blockSurface(low, Direction.NORTH);
        ItemStack concentrate = new ItemStack(CMItems.URANIUM_CONCENTRATE.get());
        ItemStack enriched = new ItemStack(CMItems.ENRICHED_URANIUM.get());
        ItemStack depleted = new ItemStack(CMItems.DEPLETED_URANIUM.get());
        scene.world().setKineticSpeed(stack, 0);
        scene.world().showSection(util.select().position(low), Direction.DOWN);
        scene.idle(10);
        ElementLink<WorldSectionElement> drive = scene.world().showIndependentSection(util.select().position(3, 4, 3), Direction.DOWN);
        scene.world().moveSection(drive, util.vector().of(0, -2, 0), 0);
        scene.idle(20);
        chapter(scene, 100, front, "A Centrifuge is an open steel frame with a brass drum that spins on the shaft. It takes its shaft from above or below");
        scene.world().setKineticSpeed(stack, 64);
        scene.effects().rotationSpeedIndicator(low.above());
        scene.idle(25);
        warn(scene, 80, util.vector().topOf(low), "It only works at 96 RPM or more, and costs 8 SU for each RPM");
        scene.world().setKineticSpeed(stack, 128);
        scene.effects().rotationSpeedIndicator(low.above());
        scene.idle(25);
        scene.overlay().showControls(front, Pointing.RIGHT, 40).rightClick().withItem(concentrate);
        scene.idle(20);
        chapter(scene, 110, front, "Put Uranium Concentrate in by hand, funnel or hopper. A batch takes 2 and makes 1 Enriched Uranium and 1 Depleted Uranium");
        say(scene, 90, front, "A batch takes 10 seconds at 128 RPM and 5 seconds at 256 RPM");
        scene.overlay().showControls(front, Pointing.RIGHT, 25).rightClick().withItem(enriched);
        scene.idle(30);
        scene.overlay().showControls(front, Pointing.RIGHT, 25).rightClick().withItem(depleted);
        scene.idle(15);
        say(scene, 90, front, "Take the products out with an empty hand, a funnel or a hopper");
        scene.world().moveSection(drive, util.vector().of(0, 2, 0), 15);
        scene.idle(20);
        scene.world().showSection(util.select().position(3, 2, 3), Direction.DOWN);
        scene.idle(8);
        scene.world().showSection(util.select().position(3, 3, 3), Direction.DOWN);
        scene.idle(20);
        chapter(scene, 120, util.vector().blockSurface(util.grid().at(3, 2, 3), Direction.NORTH),
                "Centrifuges stacked on one shaft are a cascade. Each one gets more Enriched Uranium from the same Concentrate for every other stage in the stack");
        say(scene, 120, util.vector().blockSurface(util.grid().at(3, 3, 3), Direction.NORTH),
                "Two stages give 1.25 from a batch, three give 1.5 and four give 1.75, with that much less Depleted Uranium. A fifth stage adds nothing");
        say(scene, 90, front, "Each stage still has its own contents, so feed and empty every one");
        say(scene, 110, util.vector().topOf(util.grid().at(3, 3, 3)), "Enriched Uranium goes on to become fuel pellets. Depleted Uranium mixes with gravel and water into Shielding Concrete");
        scene.markAsFinished();
    }

    public static void fuelAssembly(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("fuel_assembly", "Making a Fuel Assembly");
        scene.configureBasePlate(0, 0, 7);
        scene.setSceneOffsetY(-1);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos basin = util.grid().at(1, 2, 2);
        BlockPos compactor = util.grid().at(1, 4, 2);
        BlockPos depot = util.grid().at(4, 1, 3);
        BlockPos deployer = util.grid().at(4, 3, 3);
        BlockPos press = util.grid().at(3, 3, 3);
        Vec3 onDepot = util.vector().topOf(depot);
        ItemStack enriched = new ItemStack(CMItems.ENRICHED_URANIUM.get());
        ItemStack pellet = new ItemStack(CMItems.CERAMIC_FUEL_PELLET.get());
        ItemStack cladding = new ItemStack(CMItems.FUEL_CLADDING.get());
        ItemStack frame = new ItemStack(CMItems.EMPTY_FUEL_ASSEMBLY.get());
        ItemStack partly = new ItemStack(CMItems.INCOMPLETE_FUEL_ASSEMBLY.get());
        ItemStack finished = new ItemStack(CMItems.FRESH_FUEL_ASSEMBLY.get());
        scene.world().setKineticSpeed(util.select().everywhere(), 32);
        scene.world().showSection(util.select().fromTo(1, 1, 2, 1, 4, 2), Direction.DOWN);
        scene.idle(20);
        ElementLink<EntityElement> dropped = scene.world().createItemEntity(util.vector().topOf(basin).add(0, 0.6, 0), util.vector().of(0, 0, 0), enriched);
        scene.overlay().showControls(util.vector().topOf(basin).add(0, 0.6, 0), Pointing.DOWN, 30).withItem(enriched);
        scene.idle(15);
        scene.world().modifyEntity(dropped, Entity::discard);
        scene.world().modifyBlockEntity(compactor, MechanicalPressBlockEntity.class, p -> p.getPressingBehaviour().start(PressingBehaviour.Mode.BASIN));
        scene.idle(40);
        scene.overlay().showControls(util.vector().blockSurface(basin, Direction.NORTH), Pointing.RIGHT, 40).withItem(pellet);
        scene.idle(10);
        chapter(scene, 100, util.vector().blockSurface(basin, Direction.NORTH), "A press over a heated Basin compacts each Enriched Uranium into a Ceramic Fuel Pellet");
        say(scene, 90, util.vector().blockSurface(basin, Direction.NORTH), "Reactor Alloy Sheets press into Fuel Cladding, two from each sheet");
        scene.world().showSection(util.select().position(depot), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showControls(onDepot, Pointing.DOWN, 40).rightClick().withItem(frame);
        scene.idle(10);
        scene.world().createItemOnBeltLike(depot, Direction.NORTH, frame);
        scene.idle(20);
        chapter(scene, 110, onDepot, "A Mechanical Crafter makes the Empty Fuel Assembly from 4 Reactor Alloy Sheets and a Sealing Gasket. Filling it is a sequenced assembly, on a Depot or a belt");
        scene.world().showSection(util.select().position(deployer), Direction.DOWN);
        scene.idle(15);
        deploy(scene, util, deployer, cladding, depot, partly);
        chapter(scene, 70, util.vector().blockSurface(deployer, Direction.NORTH), "A Deployer adds Fuel Cladding");
        deploy(scene, util, deployer, pellet, depot, partly);
        deploy(scene, util, deployer, pellet, depot, partly);
        say(scene, 80, util.vector().blockSurface(deployer, Direction.NORTH), "Deployers add two Ceramic Fuel Pellets, one after the other");
        scene.world().hideSection(util.select().position(deployer), Direction.UP);
        scene.idle(15);
        ElementLink<WorldSectionElement> closer = scene.world().showIndependentSection(util.select().position(press), Direction.DOWN);
        scene.world().moveSection(closer, util.vector().of(1, 0, 0), 0);
        scene.idle(15);
        press(scene, press, depot, util.vector().centerOf(depot), partly, partly);
        say(scene, 80, util.vector().blockSurface(deployer, Direction.NORTH), "A Mechanical Press closes that layer");
        press(scene, press, depot, util.vector().centerOf(depot), partly, finished);
        scene.effects().indicateSuccess(depot);
        chapter(scene, 110, onDepot, "That is one of four layers. After four rounds it comes off as a Fuel Assembly, with 8 pellets and 4 cladding in it");
        say(scene, 110, onDepot, "The item's tooltip names the next step. Its progress stays on the item, so an interrupted assembly carries on later");
        say(scene, 90, onDepot, "A Fuel Assembly goes into the reactor through the Reactor Service Port");
        scene.markAsFinished();
    }

    public static void reprocessing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("reprocessing", "Reprocessing spent fuel");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.8f);
        scene.setSceneOffsetY(-2);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos rack = util.grid().at(1, 1, 6);
        BlockPos basin = util.grid().at(7, 2, 4);
        BlockPos mixer = util.grid().at(7, 4, 4);
        Selection saws = util.select().fromTo(1, 1, 4, 5, 1, 4);
        ItemStack spent = new ItemStack(CMItems.SPENT_FUEL_ASSEMBLY.get());
        ItemStack cut = new ItemStack(CMItems.INCOMPLETE_SHEARED_FUEL.get());
        ItemStack pieces = new ItemStack(CMItems.SHEARED_FUEL.get(), 4);
        ItemStack concentrate = new ItemStack(CMItems.URANIUM_CONCENTRATE.get(), 4);
        ItemStack strontium = new ItemStack(CMItems.STRONTIUM_90.get());
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        scene.world().modifyBlock(rack, s -> s.setValue(SpentFuelRackBlock.FILL, 1), false);
        scene.world().showSection(util.select().position(rack), Direction.DOWN);
        scene.idle(20);
        chapter(scene, 110, util.vector().topOf(rack), "A Spent Fuel Assembly still holds uranium. It has to cool in a flooded Spent Fuel Rack before it can be taken apart");
        scene.world().showSection(saws, Direction.DOWN);
        scene.world().setKineticSpeed(saws, -32);
        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(util.grid().at(2, 1, 4)), Pointing.DOWN, 30).withItem(spent);
        scene.idle(15);
        for (int x = 2; x <= 5; x++) {
            scene.world().createItemOnBeltLike(util.grid().at(x, 1, 4), Direction.WEST, x == 2 ? spent : cut);
            scene.idle(28);
            if (x == 2) {
                scene.overlay().showText(110).attachKeyFrame().text("Shear it by passing it over a Mechanical Saw four times: along a row of four saws, or four times round on a belt")
                        .pointAt(util.vector().topOf(util.grid().at(3, 1, 4))).placeNearTarget();
            }
        }
        ElementLink<EntityElement> out = scene.world().createItemEntity(util.vector().topOf(util.grid().at(5, 1, 4)).add(0.6, -0.1, 0), util.vector().of(0.05, 0.18, 0), pieces);
        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(util.grid().at(6, 0, 4)).add(0, 0.4, 0), Pointing.DOWN, 40).withItem(pieces);
        scene.idle(10);
        say(scene, 80, util.vector().topOf(util.grid().at(5, 1, 4)), "It comes off the fourth cut as 4 Sheared Spent Fuel");
        warn(scene, 80, util.vector().topOf(util.grid().at(3, 1, 4)), "A saw leaves an assembly alone while it is still hot");
        scene.world().modifyEntity(out, Entity::discard);
        scene.world().showSection(util.select().fromTo(7, 1, 4, 7, 4, 4), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().position(mixer), 32);
        scene.idle(20);
        scene.world().modifyBlockEntity(basin, BasinBlockEntity.class, b -> b.inputTank.getPrimaryHandler().setFluid(new FluidStack(Fluids.WATER, 1000)));
        scene.world().createItemOnBeltLike(basin, Direction.UP, pieces);
        scene.overlay().showControls(util.vector().topOf(basin), Pointing.DOWN, 30).withItem(pieces);
        scene.idle(20);
        scene.world().modifyBlockEntity(mixer, MechanicalMixerBlockEntity.class, MechanicalMixerBlockEntity::startProcessingBasin);
        chapter(scene, 110, util.vector().blockSurface(basin, Direction.NORTH), "A Mechanical Mixer over a heated Basin dissolves 4 Sheared Spent Fuel in 1,000 mB of water");
        scene.world().modifyBlockEntity(basin, BasinBlockEntity.class, b -> {
            b.inputInventory.clearContent();
            b.inputTank.getPrimaryHandler().setFluid(new FluidStack(CMFluids.CONTAMINATED_COOLANT.source.get(), 400));
        });
        scene.overlay().showControls(util.vector().blockSurface(basin, Direction.NORTH), Pointing.RIGHT, 40).withItem(concentrate);
        scene.idle(45);
        scene.overlay().showControls(util.vector().blockSurface(basin, Direction.NORTH), Pointing.RIGHT, 40).withItem(strontium);
        scene.idle(15);
        say(scene, 110, util.vector().blockSurface(basin, Direction.NORTH), "It gives back 4 Uranium Concentrate and 1 Strontium-90, and leaves 400 mB of Contaminated Coolant");
        say(scene, 110, util.vector().blockSurface(basin, Direction.NORTH), "The concentrate goes to the Centrifuge again. It is a quarter of the uranium a new assembly takes");
        say(scene, 80, util.vector().blockSurface(basin, Direction.NORTH), "Two Strontium-90 make one Radioisotope Cell");
        warn(scene, 120, util.vector().topOf(util.grid().at(3, 1, 4)), "The pieces give the dose the whole assembly gave, and so does every machine that holds them. Put the line behind Shielding Concrete");
        warn(scene, 110, util.vector().blockSurface(basin, Direction.NORTH), "Contaminated Coolant gives a dose wherever it stands. Pipe it away and seal it in waste canisters");
        scene.markAsFinished();
    }

    public static void wasteCanister(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("waste_canister", "Sealing waste in canisters");
        scene.configureBasePlate(0, 0, 7);
        scene.setSceneOffsetY(-1);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos depot = util.grid().at(3, 1, 3);
        BlockPos spout = util.grid().at(3, 3, 3);
        BlockPos deployer = util.grid().at(2, 3, 3);
        BlockPos press = util.grid().at(4, 3, 3);
        BlockPos canister = util.grid().at(1, 1, 5);
        BlockPos cask = util.grid().at(5, 1, 5);
        Vec3 onDepot = util.vector().topOf(depot);
        Vec3 above = util.vector().blockSurface(spout, Direction.NORTH);
        ItemStack open = new ItemStack(CMBlocks.OPEN_WASTE_CANISTER.get());
        ItemStack partly = new ItemStack(CMItems.INCOMPLETE_WASTE_CANISTER.get());
        ItemStack sealed = new ItemStack(CMItems.SEALED_WASTE_CANISTER.get());
        ItemStack gasket = new ItemStack(CMItems.SEALING_GASKET.get());
        ItemStack damaged = new ItemStack(CMItems.DAMAGED_FUEL_ASSEMBLY.get());
        scene.world().setKineticSpeed(util.select().everywhere(), 32);
        scene.world().showSection(util.select().position(depot), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showControls(onDepot, Pointing.DOWN, 40).rightClick().withItem(open);
        scene.idle(10);
        scene.world().createItemOnBeltLike(depot, Direction.NORTH, open);
        scene.idle(20);
        chapter(scene, 110, onDepot, "Liquid waste is sealed into an Open Waste Canister by a sequenced assembly, on a Depot or a belt");
        scene.world().showSection(util.select().position(spout), Direction.DOWN);
        scene.world().modifyBlockEntity(spout, SpoutBlockEntity.class,
                s -> s.getBehaviour(SmartFluidTankBehaviour.TYPE).getPrimaryHandler().setFluid(new FluidStack(CMFluids.CONTAMINATED_COOLANT.source.get(), 1000)));
        scene.idle(20);
        scene.world().modifyBlockEntityNBT(util.select().position(spout), SpoutBlockEntity.class, nbt -> nbt.putInt("ProcessingTicks", 20));
        scene.idle(20);
        scene.world().removeItemsFromBelt(depot);
        scene.world().createItemOnBeltLike(depot, Direction.UP, partly);
        scene.world().modifyBlockEntityNBT(util.select().position(spout), SpoutBlockEntity.class, nbt -> nbt.putBoolean("Splash", true));
        scene.idle(20);
        chapter(scene, 90, above, "A Spout fills it with 1,000 mB of Contaminated Coolant");
        scene.world().hideSection(util.select().position(spout), Direction.UP);
        scene.idle(15);
        ElementLink<WorldSectionElement> fitter = scene.world().showIndependentSection(util.select().position(deployer), Direction.DOWN);
        scene.world().moveSection(fitter, util.vector().of(1, 0, 0), 0);
        scene.idle(15);
        hold(scene, util, deployer, gasket);
        scene.idle(10);
        scene.world().moveDeployer(deployer, 1, 20);
        scene.idle(21);
        hold(scene, util, deployer, ItemStack.EMPTY);
        scene.world().moveDeployer(deployer, -1, 20);
        scene.idle(25);
        say(scene, 80, above, "A Deployer fits a Sealing Gasket");
        scene.world().hideIndependentSection(fitter, Direction.UP);
        scene.idle(15);
        ElementLink<WorldSectionElement> closer = scene.world().showIndependentSection(util.select().position(press), Direction.DOWN);
        scene.world().moveSection(closer, util.vector().of(-1, 0, 0), 0);
        scene.idle(15);
        press(scene, press, depot, util.vector().centerOf(depot), partly, sealed);
        scene.effects().indicateSuccess(depot);
        say(scene, 90, above, "A Mechanical Press shuts it, and it comes off as a Sealed Waste Canister");
        scene.world().removeItemsFromBelt(depot);
        scene.world().createItemOnBeltLike(depot, Direction.NORTH, damaged);
        scene.overlay().showControls(onDepot, Pointing.DOWN, 40).withItem(damaged);
        scene.idle(20);
        chapter(scene, 130, onDepot, "A Damaged Fuel Assembly is canned the same way once it has cooled in a rack. A Deployer lowers an Open Waste Canister over it, another fits the gasket and a press shuts it");
        warn(scene, 100, onDepot, "A half-sealed canister is as radioactive as what is in it. The Deployer leaves a hot assembly alone");
        scene.world().hideIndependentSection(closer, Direction.UP);
        scene.world().removeItemsFromBelt(depot);
        scene.world().showSection(util.select().position(canister), Direction.DOWN);
        scene.idle(20);
        chapter(scene, 120, util.vector().topOf(canister), "A Sealed Waste Canister can be placed as a block. It is a radiation source where it stands, and a pickaxe takes it up again with its record kept");
        scene.world().showSection(util.select().position(cask), Direction.DOWN);
        scene.idle(20);
        say(scene, 110, util.vector().topOf(cask), "Canisters belong in a Shielded Cask, four to a cask. Bolted down it lets 1%% of their dose out");
        scene.markAsFinished();
    }
}
