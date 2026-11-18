package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.fuel.FuelAssemblyItem;
import com.criticalmass.content.fuel.FuelState;
import com.criticalmass.content.fuel.SealedWasteCanisterBlockEntity;
import com.criticalmass.content.fuel.WasteCanisterItem;
import com.criticalmass.content.fuel.ShieldedCaskBlockEntity;
import com.criticalmass.content.fuel.SpentFuelRackBlockEntity;
import com.criticalmass.content.radiation.ContaminationData;
import com.criticalmass.content.radiation.DecontaminationCompoundItem;
import com.criticalmass.content.radiation.Radiation;
import com.criticalmass.content.radiation.RadiationData;
import com.criticalmass.content.radiation.RadiationEvents;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolantBuffer;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.ReactorCore;
import com.criticalmass.registry.CMAttachments;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMConfig;
import com.criticalmass.registry.CMFluids;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.AllBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class RadiationTests {
    private static final String EMPTY = "empty";
    private static final String LARGE = "empty_large";

    private RadiationTests() {
    }

    private static List<Radiation.Source> local(GameTestHelper h) {
        Vec3 origin = Vec3.atLowerCornerOf(h.absolutePos(BlockPos.ZERO));
        return Radiation.blockSources(h.getLevel()).stream().filter(s -> s.pos().distanceTo(origin.add(8, 8, 8)) < 14).toList();
    }

    private static ItemStack spent(double residual) {
        return FuelAssemblyItem.stackFor(FuelState.fresh().withRemaining(0).discharged(residual));
    }

    @GameTest(template = LARGE)
    public static void doseFallsWithDistanceAndShielding(GameTestHelper h) {
        Vec3 src = Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 8)));
        List<Radiation.Source> one = List.of(new Radiation.Source(src, 100, null, null, null));
        double r2 = Radiation.fromSources(h.getLevel(), src.add(2, 0, 0), one, null);
        double r4 = Radiation.fromSources(h.getLevel(), src.add(4, 0, 0), one, null);
        double r8 = Radiation.fromSources(h.getLevel(), src.add(8, 0, 0), one, null);
        h.assertTrue(r2 > r4 && r4 > r8 && Math.abs(r4 - 100 / 17.0) < 1e-6, "Dose should fall with distance: " + r2 + " " + r4 + " " + r8);
        BlockPos wall = new BlockPos(5, 2, 8);
        Vec3 to = src.add(6, 0, 0);
        BlockState hatch = CMBlocks.SHIELDED_HATCH.get().defaultBlockState();
        Block[] materials = {Blocks.AIR, CMBlocks.SHIELDED_GLASS.get(), CMBlocks.REACTOR_VESSEL_CASING.get(), CMBlocks.SHIELDING_CONCRETE.get()};
        double[] t = new double[materials.length];
        for (int i = 0; i < materials.length; i++) {
            h.setBlock(wall, materials[i]);
            t[i] = Radiation.transmission(h.getLevel(), src, to, null);
        }
        h.setBlock(wall, hatch.setValue(TrapDoorBlock.OPEN, true));
        double open = Radiation.transmission(h.getLevel(), src, to, null);
        h.setBlock(wall, hatch.setValue(TrapDoorBlock.OPEN, false));
        double closed = Radiation.transmission(h.getLevel(), src, to, null);
        h.assertTrue(t[0] == 1, "Air does not shield: " + t[0]);
        h.assertTrue(Math.abs(t[1] - Math.exp(-CMConfig.get(CMConfig.SHIELD_GLASS))) < 1e-6, "One block of glass: " + t[1]);
        h.assertTrue(t[1] > t[3] && t[2] > t[3], "Glass and casing are weaker than concrete: " + t[1] + " " + t[2] + " " + t[3]);
        h.assertTrue(open > 0.9 && closed < 0.1, "An open hatch shields almost nothing, a closed one shields: " + open + " " + closed);
        h.succeed();
    }

    private static double strengthAt(GameTestHelper h, List<Radiation.Source> sources, BlockPos... blocks) {
        double sum = 0;
        for (Radiation.Source s : sources) {
            for (BlockPos b : blocks) {
                if (s.pos().distanceTo(Vec3.atCenterOf(h.absolutePos(b))) < 0.1) {
                    sum += s.strength();
                }
            }
        }
        return sum;
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void fuelInAChestOrACreateMachineGivesADose(GameTestHelper h) {
        BlockPos left = new BlockPos(3, 2, 3);
        BlockPos right = new BlockPos(4, 2, 3);
        BlockPos depot = new BlockPos(8, 2, 3);
        BlockPos basin = new BlockPos(12, 2, 3);
        BlockPos barrel = new BlockPos(3, 2, 12);
        BlockPos rack = new BlockPos(8, 2, 12);
        BlockPos empty = new BlockPos(12, 2, 12);
        BlockPos wall = new BlockPos(8, 2, 5);
        BlockState chest = Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.NORTH);
        h.setBlock(left, chest.setValue(net.minecraft.world.level.block.ChestBlock.TYPE, net.minecraft.world.level.block.state.properties.ChestType.LEFT));
        h.setBlock(right, chest.setValue(net.minecraft.world.level.block.ChestBlock.TYPE, net.minecraft.world.level.block.state.properties.ChestType.RIGHT));
        h.setBlock(depot, AllBlocks.DEPOT.get());
        h.setBlock(basin, AllBlocks.BASIN.get());
        h.setBlock(barrel, Blocks.BARREL);
        h.setBlock(rack, CMBlocks.SPENT_FUEL_RACK.get());
        h.setBlock(empty, Blocks.BARREL);
        h.setBlock(wall, CMBlocks.SHIELDING_CONCRETE.get());
        net.minecraft.world.level.block.entity.ChestBlockEntity half = h.getBlockEntity(left);
        half.setItem(5, spent(0));
        for (BlockPos machine : List.of(depot, basin)) {
            ItemStack left0 = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(machine), null).insertItem(0, spent(0), false);
            h.assertTrue(left0.isEmpty(), "The machine at " + machine + " should take a fuel assembly");
        }
        ItemStack box = new ItemStack(Blocks.SHULKER_BOX);
        box.set(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(List.of(spent(0))));
        net.minecraft.world.level.block.entity.BarrelBlockEntity cask = h.getBlockEntity(barrel);
        cask.setItem(0, box);
        net.minecraft.world.level.block.entity.BarrelBlockEntity clean = h.getBlockEntity(empty);
        clean.setItem(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 64));
        SpentFuelRackBlockEntity held = h.getBlockEntity(rack);
        held.insert(spent(0));
        double one = Radiation.itemStrength(spent(0));
        Vec3 middle = Vec3.atCenterOf(h.absolutePos(new BlockPos(8, 2, 8)));
        Vec3 open = Vec3.atCenterOf(h.absolutePos(depot)).add(0, 3, 0);
        Vec3 behind = Vec3.atCenterOf(h.absolutePos(depot)).add(0, 0, 3);
        h.assertTrue(one > 0 && Radiation.itemStrength(box) == one && Radiation.itemStrength(new ItemStack(CMBlocks.SHIELDED_CASK.get())) == 0, "A box of fuel should be as strong as the fuel in it: " + Radiation.itemStrength(box));
        h.startSequence()
                .thenIdle(com.criticalmass.content.radiation.ContainerSources.KEPT_TICKS + 5)
                .thenExecute(() -> {
                    List<Radiation.Source> sources = Radiation.sourcesNear(h.getLevel(), middle, local(h));
                    h.assertTrue(Math.abs(strengthAt(h, sources, left, right) - one) < 1e-6, "Fuel in a double chest should be one source, counted once: " + strengthAt(h, sources, left, right));
                    h.assertTrue(Math.abs(strengthAt(h, sources, depot) - one) < 1e-6, "Fuel on a Depot should be a source: " + strengthAt(h, sources, depot));
                    h.assertTrue(Math.abs(strengthAt(h, sources, basin) - one) < 1e-6, "Fuel in a Basin should be a source: " + strengthAt(h, sources, basin));
                    h.assertTrue(Math.abs(strengthAt(h, sources, barrel) - one) < 1e-6, "Fuel in a shulker box in a barrel should be a source: " + strengthAt(h, sources, barrel));
                    h.assertTrue(Math.abs(strengthAt(h, sources, rack) - one) < 1e-6, "A rack should still be counted once: " + strengthAt(h, sources, rack));
                    h.assertTrue(strengthAt(h, sources, empty) == 0, "A barrel of iron should not be a source");
                    List<Radiation.Source> only = sources.stream().filter(s -> s.pos().distanceTo(Vec3.atCenterOf(h.absolutePos(depot))) < 0.1).toList();
                    double clear = Radiation.fromSources(h.getLevel(), open, only, null);
                    double shielded = Radiation.fromSources(h.getLevel(), behind, only, null);
                    h.assertTrue(Math.abs(clear - one / 10) < 1e-6, "The Depot should not shield its own fuel: " + clear);
                    h.assertTrue(shielded < clear * 0.1 && shielded > 0, "Shielding Concrete should cut the dose from fuel on a Depot: " + shielded + " vs " + clear);
                    h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(depot), null).extractItem(0, 64, false);
                })
                .thenIdle(com.criticalmass.content.radiation.ContainerSources.KEPT_TICKS + 5)
                .thenExecute(() -> {
                    List<Radiation.Source> sources = Radiation.sourcesNear(h.getLevel(), middle, local(h));
                    h.assertTrue(strengthAt(h, sources, depot) == 0, "An emptied Depot should stop being a source: " + strengthAt(h, sources, depot));
                    h.assertTrue(Math.abs(strengthAt(h, sources, basin) - one) < 1e-6, "The Basin should still be one");
                })
                .thenSucceed();
    }

    private static void pour(GameTestHelper h, BlockPos pos, net.minecraft.world.level.material.Fluid fluid, int amount) {
        int taken = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(pos), null).fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(taken == amount, "The block at " + pos + " should take " + amount + " mB: " + taken);
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void contaminatedCoolantInATankGivesADose(GameTestHelper h) {
        BlockPos low = new BlockPos(3, 2, 3);
        BlockPos high = low.above();
        BlockPos basin = new BlockPos(8, 2, 3);
        BlockPos water = new BlockPos(12, 2, 3);
        BlockPos plain = new BlockPos(3, 2, 12);
        BlockPos own = new BlockPos(8, 2, 12);
        for (BlockPos tank : List.of(low, high, water, plain)) {
            h.setBlock(tank, AllBlocks.FLUID_TANK.get());
        }
        h.setBlock(basin, AllBlocks.BASIN.get());
        h.setBlock(own, CMBlocks.SPENT_FUEL_RACK.get());
        double bucket = Radiation.LIQUID_WASTE_DOSE;
        Vec3 middle = Vec3.atCenterOf(h.absolutePos(new BlockPos(8, 2, 8)));
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    com.simibubi.create.content.fluids.tank.FluidTankBlockEntity tall = h.getBlockEntity(low);
                    h.assertTrue(tall.isController() && tall.getHeight() == 2, "The two tanks should have joined: " + tall.getHeight());
                    pour(h, high, CMFluids.CONTAMINATED_COOLANT.source.get(), 6000);
                    pour(h, basin, CMFluids.CONTAMINATED_COOLANT.source.get(), 500);
                    pour(h, water, Fluids.WATER, 4000);
                    pour(h, plain, CMFluids.PRIMARY_COOLANT.source.get(), 4000);
                    SpentFuelRackBlockEntity rack = h.getBlockEntity(own);
                    rack.coolant().set(new FluidStack(CMFluids.CONTAMINATED_COOLANT.source.get(), 1000), 0);
                })
                .thenIdle(com.criticalmass.content.radiation.ContainerSources.KEPT_TICKS + 5)
                .thenExecute(() -> {
                    List<Radiation.Source> sources = Radiation.sourcesNear(h.getLevel(), middle, List.of());
                    Vec3 centre = Vec3.atCenterOf(h.absolutePos(low)).add(0, 0.5, 0);
                    double tank = sources.stream().filter(s -> s.pos().distanceTo(centre) < 1.2).mapToDouble(Radiation.Source::strength).sum();
                    h.assertTrue(Math.abs(tank - 6 * bucket) < 1e-6, "A tank two blocks high should be one source for all its Contaminated Coolant: " + tank);
                    h.assertTrue(sources.stream().anyMatch(s -> s.pos().distanceTo(centre) < 0.01), "The source should sit in the middle of the tank");
                    h.assertTrue(Math.abs(strengthAt(h, sources, basin) - bucket / 2) < 1e-6, "Half a bucket in a Basin should be a source: " + strengthAt(h, sources, basin));
                    h.assertTrue(strengthAt(h, sources, water) == 0 && strengthAt(h, sources, plain) == 0, "Water and clean Primary Coolant in a tank should give no dose");
                    h.assertTrue(strengthAt(h, sources, own) == 0, "This mod's own blocks keep their own rules");
                    double beside = Radiation.fromSources(h.getLevel(), centre.add(2, 0, 0), sources.stream().filter(s -> s.pos().distanceTo(centre) < 0.01).toList(), null);
                    h.assertTrue(Math.abs(beside - 6 * bucket / 5) < 1e-6, "The tank should not shield its own liquid: " + beside);
                    h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(low), null).drain(6000, IFluidHandler.FluidAction.EXECUTE);
                })
                .thenIdle(com.criticalmass.content.radiation.ContainerSources.KEPT_TICKS + 5)
                .thenExecute(() -> {
                    List<Radiation.Source> sources = Radiation.sourcesNear(h.getLevel(), middle, List.of());
                    h.assertTrue(strengthAt(h, sources, low, high) == 0 && sources.stream().noneMatch(s -> s.pos().distanceTo(Vec3.atCenterOf(h.absolutePos(low)).add(0, 0.5, 0)) < 0.01), "A drained tank should stop being a source");
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE)
    public static void caskAndFloodedRackCutTheDose(GameTestHelper h) {
        BlockPos rackPos = new BlockPos(2, 1, 2);
        BlockPos wetPos = new BlockPos(2, 1, 14);
        BlockPos caskPos = new BlockPos(14, 1, 14);
        h.setBlock(rackPos, CMBlocks.SPENT_FUEL_RACK.get());
        h.setBlock(wetPos, CMBlocks.SPENT_FUEL_RACK.get());
        h.setBlock(caskPos, CMBlocks.SHIELDED_CASK.get());
        SpentFuelRackBlockEntity rack = h.getBlockEntity(rackPos);
        SpentFuelRackBlockEntity wet = h.getBlockEntity(wetPos);
        ShieldedCaskBlockEntity cask = h.getBlockEntity(caskPos);
        wet.coolant().set(new FluidStack(Fluids.WATER, CMThermal.RACK_CAPACITY), 0);
        cask.setLid(true);
        for (int i = 0; i < 4; i++) {
            rack.insert(spent(0.4));
            wet.insert(spent(0.4));
            cask.insert(spent(0.4));
        }
        cask.setLid(false);
        h.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    List<Radiation.Source> sources = local(h);
                    double dry = Radiation.fromSources(h.getLevel(), Vec3.atCenterOf(h.absolutePos(rackPos)).add(1.5, 0, 0), sources, null);
                    double flooded = Radiation.fromSources(h.getLevel(), Vec3.atCenterOf(h.absolutePos(wetPos)).add(1.5, 0, 0), sources, null);
                    double sealed = Radiation.fromSources(h.getLevel(), Vec3.atCenterOf(h.absolutePos(caskPos)).add(1.5, 0, 0), sources, null);
                    h.assertTrue(dry > 10, "A dry rack of spent fuel is a strong source: " + dry);
                    h.assertTrue(flooded < dry * 0.2, "Water cuts the rack dose: " + flooded + " vs " + dry);
                    h.assertTrue(sealed < 1, "A sealed cask stays below 1 mSv/min next to it: " + sealed);
                    cask.setLid(true);
                    double lifted = Radiation.fromSources(h.getLevel(), Vec3.atCenterOf(h.absolutePos(caskPos)).add(1.5, 0, 0), local(h), null);
                    h.assertTrue(lifted > sealed * 10, "A cask with its lid up shields far less: " + lifted + " vs " + sealed);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY)
    public static void contaminationPersistsAndCleanupNeedsRunoff(GameTestHelper h) {
        HolderLookup.Provider reg = h.getLevel().registryAccess();
        ContaminationData data = ContaminationData.get(h.getLevel());
        BlockPos spot = h.absolutePos(new BlockPos(2, 2, 2));
        data.add(spot, 40);
        data.add(spot.east(), 10);
        ContaminationData copy = ContaminationData.load(data.save(new CompoundTag(), reg), reg);
        h.assertTrue(copy.get(spot) == 40 && copy.get(spot.east()) == 10, "Contamination survives save and load");
        h.assertTrue(!DecontaminationCompoundItem.cleanArea(h.getLevel(), spot), "Cleanup is refused without a runoff tank");
        h.assertTrue(data.get(spot) == 40, "Refused cleanup leaves contamination in place");
        BlockPos tank = new BlockPos(5, 1, 2);
        h.setBlock(tank, AllBlocks.FLUID_TANK.getDefaultState());
        h.assertTrue(DecontaminationCompoundItem.cleanArea(h.getLevel(), spot), "Cleanup works with a tank nearby");
        IFluidHandler handler = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(tank), null);
        FluidStack runoff = handler.getFluidInTank(0);
        h.assertTrue(runoff.getAmount() == DecontaminationCompoundItem.RUNOFF && CMThermal.isContaminated(runoff), "Cleanup produces 250 mB of contaminated runoff: " + runoff);
        h.assertTrue(data.get(spot) == 0 && data.get(spot.east()) == 0, "Cleanup removes the contamination");
        h.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void breachSpillsContamination(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.COOLED);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 0);
        BlockPos hole = new BlockPos(5, 2, 2);
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> c.setIntegrity(h.absolutePos(hole), 0))
                .thenIdle(20)
                .thenExecute(() -> {
                    double near = ContaminationData.get(h.getLevel()).near(Vec3.atCenterOf(h.absolutePos(hole.east())), 1);
                    h.assertTrue(c.isBreached() && near > 0, "A breach leaves contamination outside the hole: " + near);
                    h.assertTrue(c.sinks().ventedVolume > 0, "The leak is logged");
                    for (BlockPos p : BlockPos.betweenClosed(h.absolutePos(hole).offset(-1, -1, -1), h.absolutePos(hole).offset(1, 1, 1))) {
                        ContaminationData.get(h.getLevel()).clear(p);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void claddingFailureContaminatesCoolant(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.COOLED);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 0);
        long column = ReactorCore.key(2, -1);
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    c.core().setAssembly(column, c.core().assembly(column).withCondition(1e-7, "overheat"));
                    c.core().setColumnHeat(2, -1, (900 - CMThermal.AMBIENT) * CMThermal.FUEL_HEAT_CAPACITY * 3);
                })
                .thenIdle(4)
                .thenExecute(() -> {
                    h.assertTrue(CMThermal.isContaminated(c.coolant().fluid()) && c.coolant().amount() == 6000, "Cladding failure contaminates the vessel coolant: " + c.coolant().fluid());
                    CoolantBuffer clean = new CoolantBuffer(2000, () -> { });
                    clean.set(CoolantLoopTests.coolant(1000), 0);
                    clean.give(c.coolant().take(100));
                    h.assertTrue(CMThermal.isContaminated(clean.fluid()) && clean.amount() == 1100, "Contaminated coolant carries its contamination into clean coolant");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY)
    public static void gearCutsContaminationNotDose(GameTestHelper h) {
        Player bare = h.makeMockPlayer(GameType.SURVIVAL);
        Player suited = h.makeMockPlayer(GameType.SURVIVAL);
        Vec3 a = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1, 1, 1)));
        Vec3 b = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(6, 1, 6)));
        bare.setPos(a);
        suited.setPos(b);
        suited.setItemSlot(EquipmentSlot.HEAD, new ItemStack(CMItems.PROTECTIVE_HOOD.get()));
        suited.setItemSlot(EquipmentSlot.CHEST, new ItemStack(CMItems.PROTECTIVE_SUIT.get()));
        suited.setItemSlot(EquipmentSlot.LEGS, new ItemStack(CMItems.PROTECTIVE_LEGGINGS.get()));
        suited.setItemSlot(EquipmentSlot.FEET, new ItemStack(CMItems.PROTECTIVE_BOOTS.get()));
        bare.getInventory().add(spent(5));
        suited.getInventory().add(spent(5));
        ContaminationData data = ContaminationData.get(h.getLevel());
        data.add(BlockPos.containing(a), 50);
        data.add(BlockPos.containing(b), 50);
        double rBare = RadiationEvents.exposeOnce(bare, h.getLevel(), List.of());
        double rSuit = RadiationEvents.exposeOnce(suited, h.getLevel(), List.of());
        RadiationData dBare = RadiationEvents.data(bare);
        RadiationData dSuit = RadiationEvents.data(suited);
        double full = CMConfig.get(CMConfig.GEAR_CONTAMINATION_PROTECTION);
        h.assertTrue(dBare.contamination > 0 && Math.abs(dSuit.contamination - dBare.contamination * (1 - full)) < 1e-9, "Full set blocks its share of contamination: " + dSuit.contamination + " vs " + dBare.contamination);
        h.assertTrue(suited.getItemBySlot(EquipmentSlot.CHEST).getOrDefault(CMComponents.CONTAMINATION.get(), 0.0) > 0, "The suit itself picks up contamination");
        double directBare = rBare - dBare.contamination * Radiation.SKIN_DOSE;
        double directSuit = rSuit - dSuit.contamination * Radiation.SKIN_DOSE - RadiationEvents.gearContamination(suited) * Radiation.GEAR_DOSE;
        h.assertTrue(Math.abs(directSuit / directBare - (1 - CMConfig.get(CMConfig.GEAR_DOSE_PROTECTION))) < 1e-6, "Gear barely reduces direct dose: " + directSuit + " vs " + directBare);
        data.clear(BlockPos.containing(a));
        data.clear(BlockPos.containing(b));
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void aSurveyMeterClicksWithWhatItReads(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 1, 3)));
        p.setPos(at);
        h.assertTrue(RadiationEvents.clickRate(p, h.getLevel()) == 0, "Empty hands should not click");
        h.assertTrue(RadiationEvents.clicks(0) == RadiationEvents.CLICK_BACKGROUND && RadiationEvents.clicks(10) > RadiationEvents.clicks(1) && RadiationEvents.clicks(1) > RadiationEvents.clicks(0)
                && RadiationEvents.clicks(1e9) == RadiationEvents.CLICK_MAX, "Clicks should start at background, rise with the reading and stop at the limit");
        ItemStack meter = new ItemStack(CMItems.SURVEY_METER.get());
        p.setItemInHand(InteractionHand.OFF_HAND, meter);
        double quiet = RadiationEvents.clickRate(p, h.getLevel());
        h.assertTrue(quiet >= RadiationEvents.CLICK_BACKGROUND && quiet == RadiationEvents.clicks(Radiation.ambientRate(h.getLevel(), p.getEyePosition())), "A held meter should tick at background or above: " + quiet);
        ContaminationData data = ContaminationData.get(h.getLevel());
        data.add(BlockPos.containing(at), 50);
        double near = RadiationEvents.clickRate(p, h.getLevel());
        h.assertTrue(near > quiet + 5 && near == RadiationEvents.clicks(Radiation.ambientRate(h.getLevel(), p.getEyePosition())), "The meter should click with the rate it reads: " + near + " from " + quiet);
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(CMItems.PROTECTIVE_HOOD.get()));
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(CMItems.PROTECTIVE_SUIT.get()));
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(CMItems.PROTECTIVE_LEGGINGS.get()));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(CMItems.PROTECTIVE_BOOTS.get()));
        h.assertTrue(RadiationEvents.clickRate(p, h.getLevel()) == near, "The outfit should not quieten the meter");
        meter.set(CMComponents.METER_MODE.get(), 1);
        double ground = RadiationEvents.clickRate(p, h.getLevel());
        h.assertTrue(ground == RadiationEvents.clicks(data.near(p.position().add(0, 0.5, 0), 2)) && ground > RadiationEvents.CLICK_BACKGROUND + 5, "On contamination the meter should click with the contamination: " + ground);
        p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CMItems.DOSIMETER.get()));
        h.assertTrue(RadiationEvents.clickRate(p, h.getLevel()) == 0, "A dosimeter should be silent while its wearer takes no dose");
        double dose = RadiationEvents.exposeOnce(p, h.getLevel(), List.of());
        h.assertTrue(dose > RadiationEvents.DOSIMETER_FLOOR && RadiationEvents.clickRate(p, h.getLevel()) == RadiationEvents.clicks(dose), "A dosimeter should click with the dose its wearer takes: " + dose);
        data.clear(BlockPos.containing(at));
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void uraniumOreGivesAWeakDoseThatRockCuts(GameTestHelper h) {
        Vec3 at = h.absolutePos(new BlockPos(4, 2, 2)).getCenter();
        BlockPos vein = new BlockPos(2, 2, 2);
        h.setBlock(vein, Blocks.STONE);
        double rock = Radiation.ambientRate(h.getLevel(), at);
        h.assertTrue(Radiation.oreDose(h.getLevel(), at) == 0, "Plain rock should give nothing");
        h.setBlock(vein, CMBlocks.URANIUM_ORE.get());
        double open = Radiation.oreDose(h.getLevel(), at);
        h.assertTrue(Math.abs(open - Radiation.ORE_DOSE / 5) < 1e-9, "Ore two blocks off should give a fifth of its strength: " + open);
        h.assertTrue(Math.abs(Radiation.ambientRate(h.getLevel(), at) - rock - open) < 1e-9, "A survey should read the ore: " + (Radiation.ambientRate(h.getLevel(), at) - rock));
        h.setBlock(new BlockPos(3, 2, 2), Blocks.STONE);
        double behind = Radiation.oreDose(h.getLevel(), at);
        h.assertTrue(Math.abs(behind / open - Math.exp(-CMConfig.get(CMConfig.SHIELD_SOLID))) < 1e-6, "A block of rock should cut it as it cuts any source: " + behind / open);
        h.setBlock(new BlockPos(6, 2, 2), CMBlocks.DEEPSLATE_URANIUM_ORE.get());
        double both = Radiation.oreDose(h.getLevel(), at);
        h.assertTrue(Math.abs(both - behind - Radiation.ORE_DOSE / 5) < 1e-9, "Deepslate ore should give the same: " + (both - behind));
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.setPos(at.x, at.y - p.getEyeHeight(), at.z);
        double dose = RadiationEvents.exposeOnce(p, h.getLevel(), List.of());
        h.assertTrue(dose >= both - 1e-9 && both < CMConfig.get(CMConfig.RECOVERY_PER_MINUTE) / 100, "Someone beside ore should take its dose, far less than the body recovers: " + dose);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CMItems.SURVEY_METER.get()));
        h.assertTrue(RadiationEvents.clickRate(p, h.getLevel()) > RadiationEvents.CLICK_BACKGROUND + 1, "A Survey Meter should tick faster beside ore: " + RadiationEvents.clickRate(p, h.getLevel()));
        h.setBlock(vein, Blocks.AIR);
        h.setBlock(new BlockPos(6, 2, 2), Blocks.AIR);
        h.assertTrue(Radiation.oreDose(h.getLevel(), at) == 0, "With the ore gone its dose should be gone");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void uraniumInHandReadsOnASurveyMeter(GameTestHelper h) {
        double raw = Radiation.itemStrength(new ItemStack(CMItems.RAW_URANIUM.get()));
        h.assertTrue(raw == Radiation.URANIUM_DOSE && Radiation.itemStrength(new ItemStack(CMItems.RAW_URANIUM.get(), 64)) == 64 * raw, "Raw Uranium should give a weak dose that grows with the stack: " + raw);
        for (ItemStack same : List.of(new ItemStack(CMItems.URANIUM_CONCENTRATE.get()), new ItemStack(com.simibubi.create.AllItems.CRUSHED_URANIUM.get()), new ItemStack(CMBlocks.URANIUM_ORE.get()), new ItemStack(CMBlocks.DEEPSLATE_URANIUM_ORE.get()))) {
            h.assertTrue(Radiation.itemStrength(same) == raw, same.getItem() + " should give what Raw Uranium gives");
        }
        double enriched = Radiation.itemStrength(new ItemStack(CMItems.ENRICHED_URANIUM.get()));
        double depleted = Radiation.itemStrength(new ItemStack(CMItems.DEPLETED_URANIUM.get()));
        h.assertTrue(enriched > raw && depleted > 0 && depleted < raw && Radiation.itemStrength(new ItemStack(CMItems.CERAMIC_FUEL_PELLET.get())) == enriched, "Enriched should give more than raw and depleted less: " + enriched + ", " + depleted);
        h.assertTrue(8 * enriched < Radiation.FRESH_DOSE, "The pellets in an assembly should give less than the assembly");
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        Vec3 feet = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 1, 3)));
        p.setPos(feet);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CMItems.SURVEY_METER.get()));
        double empty = Radiation.ambientRate(h.getLevel(), p.getEyePosition());
        double quiet = RadiationEvents.clickRate(p, h.getLevel());
        net.minecraft.world.entity.item.ItemEntity dropped = new net.minecraft.world.entity.item.ItemEntity(h.getLevel(), feet.x + 1, feet.y, feet.z, new ItemStack(CMItems.RAW_URANIUM.get(), 64), 0, 0, 0);
        dropped.setNoGravity(true);
        h.getLevel().addFreshEntity(dropped);
        double lying = Radiation.ambientRate(h.getLevel(), p.getEyePosition()) - empty;
        h.assertTrue(lying > 0.1 && RadiationEvents.clickRate(p, h.getLevel()) > quiet + 1, "A meter should read a dropped stack of Raw Uranium: " + lying);
        dropped.discard();
        for (int slot = 9; slot < 36; slot++) {
            p.getInventory().setItem(slot, new ItemStack(CMItems.RAW_URANIUM.get(), 64));
        }
        double dose = RadiationEvents.exposeOnce(p, h.getLevel(), List.of());
        h.assertTrue(dose >= 27 * 64 * raw - 1e-9 && dose < CMConfig.get(CMConfig.RECOVERY_PER_MINUTE) && RadiationEvents.data(p).body == 0, "27 stacks of Raw Uranium should give a dose, and less than the body recovers: " + dose);
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void aCreativePlayerTakesNoDoseContaminationOrSickness(GameTestHelper h) {
        Vec3 feet = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 1, 3)));
        Player builder = h.makeMockPlayer(GameType.CREATIVE);
        Player worker = h.makeMockPlayer(GameType.SURVIVAL);
        for (Player p : List.of(builder, worker)) {
            p.setPos(feet);
            for (int slot = 9; slot < 18; slot++) {
                p.getInventory().setItem(slot, new ItemStack(CMItems.SPENT_FUEL_ASSEMBLY.get()));
            }
            RadiationData d = RadiationEvents.data(p);
            d.body = RadiationEvents.WITHER_DOSE + 500;
            d.lastRate = 7;
            p.setData(com.criticalmass.registry.CMAttachments.RADIATION.get(), d);
        }
        ContaminationData.get(h.getLevel()).add(h.absolutePos(new BlockPos(3, 1, 3)), 50);
        h.assertTrue(RadiationEvents.takesDose(worker) && !RadiationEvents.takesDose(builder), "Only a player who is not in creative should take a dose");
        double taken = RadiationEvents.second(worker, h.getLevel(), List.of());
        double spared = RadiationEvents.second(builder, h.getLevel(), List.of());
        RadiationData hurt = RadiationEvents.data(worker);
        RadiationData safe = RadiationEvents.data(builder);
        h.assertTrue(taken > 1 && hurt.lastRate == taken && hurt.contamination > 0 && worker.hasEffect(net.minecraft.world.effect.MobEffects.WITHER) && worker.hasEffect(net.minecraft.world.effect.MobEffects.CONFUSION),
                "Carrying spent fuel over contaminated ground in survival should give a dose, contamination and sickness: " + taken + " " + hurt.contamination);
        h.assertTrue(spared == 0 && safe.lastRate == 0 && safe.contamination == 0 && safe.total == 0 && safe.body == RadiationEvents.WITHER_DOSE + 500,
                "The same in creative should give no dose and no contamination, and leave the stored dose alone: " + spared + " " + safe.contamination + " " + safe.body);
        h.assertTrue(builder.getActiveEffects().isEmpty(), "A creative player should get no sickness even with a dose stored: " + builder.getActiveEffects());
        builder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CMItems.SURVEY_METER.get()));
        h.assertTrue(RadiationEvents.clickRate(builder, h.getLevel()) > 0, "A Survey Meter should still tick in a creative player's hand");
        ContaminationData.get(h.getLevel()).wash(h.absolutePos(new BlockPos(3, 1, 3)), 0, 1);
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void dosimeterDoseLivesOnThePlayer(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 1, 3))));
        p.getInventory().add(spent(10));
        RadiationEvents.exposeOnce(p, h.getLevel(), List.of());
        RadiationEvents.exposeOnce(p, h.getLevel(), List.of());
        RadiationData d = RadiationEvents.data(p);
        h.assertTrue(d.total > 0 && d.dosimeter() == d.total, "Exposure accumulates: " + d.total);
        RadiationData copy = new RadiationData();
        copy.deserializeNBT(h.getLevel().registryAccess(), d.serializeNBT(h.getLevel().registryAccess()));
        h.assertTrue(copy.total == d.total && copy.body == d.body, "Dose survives save and load");
        ItemStack dosimeter = new ItemStack(CMItems.DOSIMETER.get());
        h.assertTrue(dosimeter.getComponentsPatch().isEmpty() && dosimeter.copy().getComponentsPatch().isEmpty(), "The dosimeter carries no dose that could be copied");
        double before = d.total;
        d.baseline = d.total;
        p.setData(CMAttachments.RADIATION.get(), d);
        h.assertTrue(RadiationEvents.data(p).dosimeter() == 0 && RadiationEvents.data(p).total == before, "Resetting the reading keeps the lifetime dose");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(CriticalMass.MODID, "sequenced_assembly/sealed_waste_canister")).isPresent(), "Solidification recipe loads");
        h.assertTrue(new FluidStack(CMFluids.CONTAMINATED_COOLANT.source.get(), 1).getFluid() != null, "Contaminated coolant exists");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void placedCanisterKeepsItsRecordAndRadiates(GameTestHelper h) {
        FuelState state = FuelState.fresh().withRemaining(0).discharged(0.3);
        BlockPos floor = new BlockPos(2, 0, 2);
        BlockPos at = floor.above();
        h.setBlock(floor, Blocks.STONE);
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = WasteCanisterItem.sealing(state);
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = h.absolutePos(floor);
        stack.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
        h.assertBlockPresent(CMBlocks.SEALED_WASTE_CANISTER.get(), at);
        h.assertTrue(stack.isEmpty(), "Placing uses up the canister item");
        SealedWasteCanisterBlockEntity be = h.getBlockEntity(at);
        h.assertTrue(state.equals(be.state()), "The placed canister keeps its record: " + be.state());
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    double expected = Radiation.CANISTER_DOSE + Radiation.RESIDUAL_DOSE * 0.3;
                    Vec3 centre = Vec3.atCenterOf(h.absolutePos(at));
                    h.assertTrue(local(h).stream().anyMatch(s -> s.pos().distanceTo(centre) < 0.01 && Math.abs(s.strength() - expected) < 1e-9), "A placed canister is a radiation source of " + expected);
                    List<ItemStack> drops = Block.getDrops(h.getBlockState(at), h.getLevel(), h.absolutePos(at), h.getBlockEntity(at));
                    h.assertTrue(drops.size() == 1 && drops.get(0).is(CMItems.SEALED_WASTE_CANISTER.get()) && state.equals(drops.get(0).get(CMComponents.FUEL_STATE.get())), "Breaking it drops the canister with its record: " + drops);
                    h.setBlock(at, Blocks.AIR);
                    h.assertTrue(local(h).stream().noneMatch(s -> s.pos().distanceTo(centre) < 0.01), "A removed canister stops radiating");
                })
                .thenSucceed();
    }
}
