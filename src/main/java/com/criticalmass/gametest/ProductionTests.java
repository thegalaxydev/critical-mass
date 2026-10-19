package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.fuel.FuelAssemblyBenchBlockEntity;
import com.criticalmass.content.fuel.FuelAssemblyBenchBlockEntity.Outcome;
import com.criticalmass.content.fuel.FuelAssemblyItem;
import com.criticalmass.content.fuel.FuelState;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
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
    private static final List<String> CHAIN = List.of("milling/raw_uranium", "splashing/crushed_raw_uranium", "mixing/uranium_concentrate", "compacting/ceramic_fuel_pellet",
            "mixing/reactor_alloy_ingot", "pressing/reactor_alloy_sheet", "pressing/fuel_cladding", "compacting/sealing_gasket", "mechanical_crafting/empty_fuel_assembly",
            "sequenced_assembly/fuel_assembly", "compacting/shielding_plate", "crafting/reactor_service_port", "mechanical_crafting/reactor_controller");

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
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void benchAssemblesFuelByHandAndResumes(GameTestHelper h) {
        BlockPos a = new BlockPos(1, 1, 1);
        BlockPos b = new BlockPos(4, 1, 1);
        h.setBlock(a, CMBlocks.FUEL_ASSEMBLY_BENCH.get());
        h.setBlock(b, CMBlocks.FUEL_ASSEMBLY_BENCH.get());
        FuelAssemblyBenchBlockEntity bench = h.getBlockEntity(a);
        FuelAssemblyBenchBlockEntity other = h.getBlockEntity(b);
        HolderLookup.Provider reg = h.getLevel().registryAccess();
        ItemStack cladding = new ItemStack(CMItems.FUEL_CLADDING.get());
        ItemStack pellet = new ItemStack(CMItems.CERAMIC_FUEL_PELLET.get());
        bench.setHeld(new ItemStack(CMItems.EMPTY_FUEL_ASSEMBLY.get()));
        h.assertTrue(bench.work(pellet, false) == Outcome.WRONG, "Pellets before cladding are refused");
        h.assertTrue(bench.work(ItemStack.EMPTY, true) == Outcome.WRONG, "Pressing before loading is refused");
        FuelAssemblyBenchBlockEntity current = bench;
        for (int loop = 0; loop < 4; loop++) {
            h.assertTrue(current.work(cladding, false) == Outcome.CONSUME, "Cladding, loop " + loop);
            h.assertTrue(current.work(pellet, false) == Outcome.CONSUME, "First pellet, loop " + loop);
            if (loop == 1) {
                CompoundTag tag = current.saveWithoutMetadata(reg);
                other.loadWithComponents(tag, reg);
                bench.setHeld(ItemStack.EMPTY);
                current = other;
                h.assertTrue(current.held().is(CMItems.INCOMPLETE_FUEL_ASSEMBLY.get()), "Reloaded bench keeps the incomplete assembly");
            }
            h.assertTrue(current.work(pellet, false) == Outcome.CONSUME, "Second pellet, loop " + loop);
            h.assertTrue(current.work(ItemStack.EMPTY, true) == Outcome.KEEP, "Press, loop " + loop);
        }
        ItemStack done = current.held();
        FuelState s = done.get(CMComponents.FUEL_STATE.get());
        h.assertTrue(done.is(CMItems.FRESH_FUEL_ASSEMBLY.get()), "Sequence should finish a fresh assembly: " + done);
        h.assertTrue(s != null && s.remaining() == 1 && s.condition() == 1 && s.burned() == 0, "Fresh assembly starts full with a new lifecycle ID: " + s);
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void damagedFuelIsSealedIntoACanister(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, CMBlocks.FUEL_ASSEMBLY_BENCH.get());
        FuelAssemblyBenchBlockEntity bench = h.getBlockEntity(pos);
        FuelState hot = FuelState.fresh().withBurned(5000).withCondition(0, "overheat").discharged(5);
        FuelState cool = hot.withResidual(0.1);
        ItemStack plate = new ItemStack(CMItems.SHIELDING_PLATE.get());
        ItemStack gasket = new ItemStack(CMItems.SEALING_GASKET.get());
        bench.setHeld(FuelAssemblyItem.stackFor(hot));
        h.assertTrue(bench.work(plate, false) == Outcome.TOO_HOT, "Hot damaged fuel cannot be recovered yet");
        bench.setHeld(FuelAssemblyItem.stackFor(cool));
        h.assertTrue(bench.work(gasket, false) == Outcome.WRONG, "Gasket comes last");
        h.assertTrue(bench.work(plate, false) == Outcome.CONSUME && bench.work(plate, false) == Outcome.CONSUME, "Two shielding plates");
        h.assertTrue(bench.held().is(CMItems.DAMAGED_FUEL_ASSEMBLY.get()) && bench.held().get(CMComponents.RECOVERY_STEP.get()) == 2, "Recovery progress is kept on the item");
        h.assertTrue(bench.work(gasket, false) == Outcome.CONSUME, "Gasket seals it");
        ItemStack canister = bench.held();
        FuelState recorded = canister.get(CMComponents.FUEL_STATE.get());
        h.assertTrue(canister.is(CMItems.SEALED_WASTE_CANISTER.get()), "Damaged fuel leaves as a canister: " + canister);
        h.assertTrue(recorded != null && recorded.id().equals(cool.id()) && recorded.damageCause().equals("overheat") && recorded.burned() == 5000, "Canister records the lifecycle data: " + recorded);
        h.assertTrue(canister.has(DataComponents.FIRE_RESISTANT) && canister.getEntityLifespan(h.getLevel()) == Integer.MAX_VALUE, "Canisters resist fire and never despawn");
        h.assertTrue(FuelAssemblyItem.stackFor(cool).has(DataComponents.FIRE_RESISTANT), "Assemblies resist fire");
        h.succeed();
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
}
