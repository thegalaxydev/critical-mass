package com.criticalmass.gametest;

import com.criticalmass.content.fuel.CentrifugeBlockEntity;
import com.criticalmass.CriticalMass;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.minecraft.core.Direction;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.AllBlocks;
import com.criticalmass.content.fuel.FuelAssemblyItem;
import com.criticalmass.content.fuel.FuelState;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class ProductionTests {
    private static final String EMPTY = "empty";
    private static final List<String> CHAIN = List.of("milling/raw_uranium", "splashing/crushed_raw_uranium", "mixing/uranium_concentrate", "compacting/ceramic_fuel_pellet", "mixing/shielding_concrete_from_depleted_uranium", "mechanical_crafting/centrifuge",
            "mixing/reactor_alloy_ingot", "pressing/reactor_alloy_sheet", "pressing/fuel_cladding", "compacting/sealing_gasket", "mechanical_crafting/empty_fuel_assembly",
            "sequenced_assembly/fuel_assembly", "mixing/steel_ingot", "pressing/steel_plate", "crafting/reactor_service_port", "mechanical_crafting/reactor_controller",
            "crafting/open_waste_canister", "sequenced_assembly/sealed_waste_canister");

    private ProductionTests() {
    }

    @GameTest(template = EMPTY)
    public static void fuelChainRecipesLoad(GameTestHelper h) {
        RecipeManager rm = h.getLevel().getRecipeManager();
        for (String id : CHAIN) {
            h.assertTrue(rm.byKey(CriticalMass.asResource(id)).isPresent(), "Recipe should load: " + id);
        }
        h.assertTrue(rm.byKey(ResourceLocation.fromNamespaceAndPath("create", "crushing/raw_uranium")).isPresent(), "Create's raw uranium crushing should be active through c:raw_materials/uranium");
        long ours = rm.getRecipes().stream().filter(r -> r.id().getNamespace().equals(CriticalMass.MODID)).count();
        h.assertTrue(ours >= 45, "All addon recipes should load, found " + ours);
        h.assertTrue(SequencedAssemblyRecipe.getRecipe(h.getLevel(), new ItemStack(CMItems.EMPTY_FUEL_ASSEMBLY.get()), AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class).isPresent(),
                "An empty fuel assembly should start the sequenced assembly");
        h.assertTrue(SequencedAssemblyRecipe.getRecipe(h.getLevel(), new ItemStack(CMBlocks.OPEN_WASTE_CANISTER.get()), AllRecipeTypes.FILLING.getType(), FillingRecipe.class).isPresent(),
                "Spouting into an open waste canister should start its sequenced assembly");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 500)
    public static void centrifugeEnrichesOnlyAtSpeed(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, CMBlocks.CENTRIFUGE.get());
        h.setBlock(pos.below(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        CreativeMotorBlockEntity motor = h.getBlockEntity(pos.below());
        motor.generatedSpeed.setValue(64);
        CentrifugeBlockEntity centrifuge = h.getBlockEntity(pos);
        IItemHandler handler = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), Direction.NORTH);
        h.assertTrue(handler != null, "A centrifuge should take items from funnels and hoppers");
        ItemStack pellet = new ItemStack(CMItems.CERAMIC_FUEL_PELLET.get());
        h.assertTrue(handler.insertItem(CentrifugeBlockEntity.INPUT, pellet, false).getCount() == 1, "Only Uranium Concentrate goes in");
        h.assertTrue(handler.insertItem(CentrifugeBlockEntity.ENRICHED, new ItemStack(CMItems.URANIUM_CONCENTRATE.get()), false).getCount() == 1, "Nothing goes into the output slots");
        h.assertTrue(handler.insertItem(CentrifugeBlockEntity.INPUT, new ItemStack(CMItems.URANIUM_CONCENTRATE.get(), 5), false).isEmpty(), "Concentrate should go in");
        h.startSequence()
                .thenIdle(100)
                .thenExecute(() -> {
                    h.assertTrue(centrifuge.getSpeed() != 0 && centrifuge.status().endsWith("slow"), "Below its minimum speed a centrifuge should only say so: " + centrifuge.status());
                    h.assertTrue(centrifuge.held(CentrifugeBlockEntity.ENRICHED).isEmpty() && centrifuge.held(CentrifugeBlockEntity.INPUT).getCount() == 5, "Nothing should be enriched below the minimum speed");
                    motor.generatedSpeed.setValue(256);
                })
                .thenIdle(130)
                .thenExecute(() -> {
                    h.assertTrue(centrifuge.held(CentrifugeBlockEntity.ENRICHED).getCount() == 1 && centrifuge.held(CentrifugeBlockEntity.DEPLETED).getCount() == 1, "A batch at 256 RPM should take about five seconds: "
                            + centrifuge.held(CentrifugeBlockEntity.ENRICHED) + " " + centrifuge.status());
                    h.assertTrue(centrifuge.held(CentrifugeBlockEntity.INPUT).getCount() == 3, "A batch should use two concentrate");
                })
                .thenIdle(130)
                .thenExecute(() -> {
                    h.assertTrue(centrifuge.held(CentrifugeBlockEntity.ENRICHED).getCount() == 2 && centrifuge.held(CentrifugeBlockEntity.DEPLETED).getCount() == 2, "A second batch should follow");
                    h.assertTrue(centrifuge.held(CentrifugeBlockEntity.INPUT).getCount() == 1 && centrifuge.status().endsWith("idle"), "One concentrate is not enough for a batch: " + centrifuge.status());
                    h.assertTrue(handler.extractItem(CentrifugeBlockEntity.INPUT, 1, false).isEmpty(), "Automation should not pull the input back out");
                    h.assertTrue(handler.extractItem(CentrifugeBlockEntity.ENRICHED, 64, false).getCount() == 2 && handler.extractItem(CentrifugeBlockEntity.DEPLETED, 64, false).getCount() == 2,
                            "Automation should collect both products");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY)
    public static void uraniumOreDropsRawUranium(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        List<ItemStack> drops = Block.getDrops(CMBlocks.URANIUM_ORE.get().defaultBlockState(), h.getLevel(), pos, null, null, pick);
        h.assertTrue(drops.size() == 1 && drops.get(0).is(CMItems.RAW_URANIUM.get()), "Iron pickaxe mines raw uranium: " + drops);
        ItemStack silk = new ItemStack(Items.IRON_PICKAXE);
        silk.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        List<ItemStack> silkDrops = Block.getDrops(CMBlocks.DEEPSLATE_URANIUM_ORE.get().defaultBlockState(), h.getLevel(), pos, null, null, silk);
        h.assertTrue(silkDrops.size() == 1 && silkDrops.get(0).is(CMBlocks.DEEPSLATE_URANIUM_ORE.get().asItem()), "Silk touch keeps the ore: " + silkDrops);
        h.assertTrue(new ItemStack(CMItems.RAW_URANIUM.get()).is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "raw_materials/uranium"))), "Raw uranium carries c:raw_materials/uranium");
        h.succeed();
    }

    private static void motor(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.WEST));
        CreativeMotorBlockEntity motor = h.getBlockEntity(pos);
        motor.generatedSpeed.setValue(128);
    }

    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void damagedFuelSequenceKeepsItsRecord(GameTestHelper h) {
        FuelState hot = FuelState.fresh().withBurned(4200).withCondition(0, "overheat").discharged(5);
        FuelState cool = hot.withResidual(0.1);
        h.assertTrue(SequencedAssemblyRecipe.getRecipe(h.getLevel(), FuelAssemblyItem.stackFor(hot), AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class).isEmpty(),
                "Hot damaged fuel cannot start the canister sequence");
        h.assertTrue(SequencedAssemblyRecipe.getRecipe(h.getLevel(), FuelAssemblyItem.stackFor(cool), AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class).isPresent(),
                "Cooled damaged fuel starts the canister sequence");
        BlockPos depotA = new BlockPos(2, 1, 2);
        BlockPos deployer = new BlockPos(2, 3, 2);
        BlockPos depotB = new BlockPos(5, 1, 2);
        BlockPos press = new BlockPos(5, 3, 2);
        h.setBlock(depotA, AllBlocks.DEPOT.get());
        h.setBlock(depotB, AllBlocks.DEPOT.get());
        h.setBlock(deployer, AllBlocks.DEPLOYER.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN).setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
        h.setBlock(press, AllBlocks.MECHANICAL_PRESS.getDefaultState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
        motor(h, deployer.east());
        motor(h, press.east());
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    DepotBlockEntity a = h.getBlockEntity(depotA);
                    a.setHeldItem(FuelAssemblyItem.stackFor(cool));
                    hand(h, deployer).insertItem(0, new ItemStack(CMBlocks.OPEN_WASTE_CANISTER.get()), false);
                })
                .thenWaitUntil(() -> {
                    ItemStack held = ((DepotBlockEntity) h.getBlockEntity(depotA)).getHeldItem();
                    h.assertTrue(held.is(CMItems.INCOMPLETE_WASTE_CANISTER.get()), "Deploying the open canister: " + held);
                })
                .thenExecute(() -> {
                    ItemStack held = ((DepotBlockEntity) h.getBlockEntity(depotA)).getHeldItem();
                    FuelState s = held.get(CMComponents.FUEL_STATE.get());
                    h.assertTrue(s != null && s.id().equals(cool.id()), "The incomplete canister carries the assembly's record: " + s);
                    h.assertTrue(com.criticalmass.content.radiation.Radiation.itemStrength(held) > 0, "The open canister with fuel inside radiates");
                    hand(h, deployer).insertItem(0, new ItemStack(CMItems.SEALING_GASKET.get()), false);
                })
                .thenWaitUntil(() -> h.assertTrue(hand(h, deployer).getStackInSlot(0).isEmpty(), "Deploying the gasket"))
                .thenIdle(10)
                .thenExecute(() -> {
                    DepotBlockEntity a = h.getBlockEntity(depotA);
                    DepotBlockEntity b = h.getBlockEntity(depotB);
                    b.setHeldItem(a.getHeldItem());
                    a.setHeldItem(ItemStack.EMPTY);
                })
                .thenWaitUntil(() -> {
                    ItemStack held = ((DepotBlockEntity) h.getBlockEntity(depotB)).getHeldItem();
                    h.assertTrue(held.is(CMItems.SEALED_WASTE_CANISTER.get()), "Pressing seals it: " + held);
                })
                .thenExecute(() -> {
                    ItemStack done = ((DepotBlockEntity) h.getBlockEntity(depotB)).getHeldItem();
                    FuelState s = done.get(CMComponents.FUEL_STATE.get());
                    h.assertTrue(s != null && s.id().equals(cool.id()) && s.damageCause().equals("overheat") && s.burned() == 4200 && s.residual() == cool.residual(),
                            "The sealed canister keeps the assembly's record: " + s);
                    h.assertTrue(!done.has(com.simibubi.create.AllDataComponents.SEQUENCED_ASSEMBLY), "The finished canister is not left mid-sequence");
                })
                .thenSucceed();
    }

    private static IItemHandler hand(GameTestHelper h, BlockPos pos) {
        return h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), null);
    }

    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void stackedCentrifugesEnrichMoreFromTheSameFeed(GameTestHelper h) {
        BlockPos bottom = new BlockPos(2, 2, 2);
        h.setBlock(bottom.below(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        for (int i = 0; i < 5; i++) {
            h.setBlock(bottom.above(i), CMBlocks.CENTRIFUGE.get());
        }
        CreativeMotorBlockEntity motor = h.getBlockEntity(bottom.below());
        motor.generatedSpeed.setValue(256);
        CentrifugeBlockEntity centrifuge = h.getBlockEntity(bottom);
        centrifuge.insert(new ItemStack(CMItems.URANIUM_CONCENTRATE.get(), 8));
        h.startSequence()
                .thenIdle(470)
                .thenExecute(() -> {
                    CentrifugeBlockEntity top = h.getBlockEntity(bottom.above(4));
                    h.assertTrue(centrifuge.stages() == CentrifugeBlockEntity.CASCADE_MAX && top.stages() == CentrifugeBlockEntity.CASCADE_MAX, "A cascade should count four stages at most: " + centrifuge.stages());
                    h.assertTrue(top.getSpeed() != 0, "Every stage should turn on the one shaft");
                    h.assertTrue(centrifuge.held(CentrifugeBlockEntity.ENRICHED).getCount() == 7 && centrifuge.held(CentrifugeBlockEntity.DEPLETED).getCount() == 1,
                            "Four batches in a four stage cascade should give 7 enriched and 1 depleted: " + centrifuge.held(CentrifugeBlockEntity.ENRICHED) + " " + centrifuge.held(CentrifugeBlockEntity.DEPLETED));
                    h.assertTrue(centrifuge.held(CentrifugeBlockEntity.INPUT).isEmpty(), "All eight concentrate should be used");
                })
                .thenSucceed();
    }
}
