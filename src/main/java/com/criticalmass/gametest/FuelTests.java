package com.criticalmass.gametest;

import com.criticalmass.content.fuel.ServicePortArmType;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.Direction;
import com.criticalmass.CriticalMass;
import com.criticalmass.content.fuel.FuelAssemblyItem;
import com.criticalmass.content.fuel.FuelState;
import com.criticalmass.content.fuel.ServicePortBlockEntity;
import com.criticalmass.content.fuel.ShieldedCaskBlockEntity;
import com.criticalmass.content.fuel.SpentFuelRackBlock;
import com.criticalmass.content.fuel.SpentFuelRackBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.LoadedThermalBlocks;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.ReactorCore;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMConfig;
import com.criticalmass.registry.CMItems;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class FuelTests {
    private static final String EMPTY = "empty";
    private static final String LARGE = "empty_large";
    private static final BlockPos PORT_POWER = TestVessels.SERVICE.above();
    private static final long COLUMN_A = ReactorCore.key(2, -1);
    private static final long COLUMN_B = ReactorCore.key(2, 1);

    private FuelTests() {
    }

    private static boolean sameLifecycle(FuelState a, FuelState b) {
        return a.id().equals(b.id()) && a.remaining() == b.remaining() && a.condition() == b.condition() && a.burned() == b.burned()
                && a.contamination() == b.contamination() && a.damageCause().equals(b.damageCause());
    }

    private static boolean sameState(FuelState a, FuelState b) {
        return sameLifecycle(a, b) && a.residual() == b.residual() && a.dischargeResidual() == b.dischargeResidual();
    }

    private static ItemStack assembly(FuelState s) {
        return FuelAssemblyItem.stackFor(s);
    }

    private static FuelState state(ItemStack stack) {
        return stack.get(CMComponents.FUEL_STATE.get());
    }

    private static ReactorControllerBlockEntity loop(GameTestHelper h, boolean fuel) {
        TestVessels.standardVessel(h, TestVessels.COOLED, fuel);
        TestVessels.loop(h, true);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 0);
        return c;
    }

    private static List<ItemEntity> items(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(BlockPos.ZERO)).expandTowards(16, 16, 16));
    }

    @GameTest(template = LARGE, timeoutTicks = 900)
    public static void burnupFollowsProducedHeat(GameTestHelper h) {
        ReactorControllerBlockEntity c = loop(h, true);
        double start = 0.0002;
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    for (Map.Entry<Long, FuelState> e : new ArrayList<>(c.core().assemblies().entrySet())) {
                        c.core().setAssembly(e.getKey(), e.getValue().withRemaining(start));
                    }
                    h.setBlock(TestVessels.REDSTONE, Blocks.REDSTONE_BLOCK);
                    TestVessels.atPower(c, 0.3);
                })
                .thenIdle(30)
                .thenExecute(() -> {
                    for (FuelState a : c.core().assemblies().values()) {
                        h.assertTrue(a.remaining() < start && a.remaining() > 0 && a.burned() > 0, "Burning fuel should lose remaining fuel and gain burn history: " + a);
                    }
                })
                .thenIdle(300)
                .thenExecute(() -> {
                    double burned = 0;
                    double expected = start * CMConfig.fuelLifetime() * CMThermal.FUEL_BASE_OUTPUT * 3;
                    for (FuelState a : c.core().assemblies().values()) {
                        h.assertTrue(a.spent() && a.form() == FuelAssemblyItem.Form.SPENT, "Burnt-out fuel should be spent: " + a);
                        h.assertTrue(Math.abs(a.burned() - expected) < expected * 0.02, "Burn history " + a.burned() + " should match the fuel burnt, " + expected);
                        h.assertTrue(a.condition() < 1 && a.condition() > 0.99, "Normal burn should wear the assembly only slightly: " + a.condition());
                        burned += a.burned();
                    }
                    h.assertTrue(burned <= c.heatAdded() + 1e-6, "Burn history " + burned + " cannot exceed the heat produced " + c.heatAdded());
                    h.assertTrue(c.core().activeAssemblies() == 0 && c.core().lastFission() == 0, "Spent core should produce only decay heat");
                    h.assertTrue(!c.status().endsWith("running"), "Spent core should not report Running: " + c.status());
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void overheatingAcceleratesConditionLoss(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.COOLED);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> c.core().setColumnHeat(2, -1, (1600 - CMThermal.AMBIENT) * CMThermal.FUEL_HEAT_CAPACITY * 3))
                .thenIdle(40)
                .thenExecute(() -> {
                    FuelState hot = c.core().assembly(COLUMN_A);
                    FuelState cool = c.core().assembly(COLUMN_B);
                    h.assertTrue(hot.condition() < 0.99, "Overheated assembly should lose condition: " + hot.condition());
                    h.assertTrue(cool.condition() == 1, "Cool assembly should keep its condition: " + cool.condition());
                    c.core().setAssembly(COLUMN_A, hot.withCondition(0.0001, "overheat"));
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    ServicePortBlockEntity port = h.getBlockEntity(TestVessels.SERVICE);
                    FuelState hot = c.core().assembly(COLUMN_A) != null ? c.core().assembly(COLUMN_A) : state(port.output());
                    h.assertTrue(hot != null && port.output().is(CMItems.DAMAGED_FUEL_ASSEMBLY.get()), "Failed assembly should be discharged to the port as damaged fuel");
                    h.assertTrue(hot.damaged() && hot.damageCause().equals("overheat") && hot.form() == FuelAssemblyItem.Form.DAMAGED, "Overheated assembly should fail: " + hot);
                    h.assertTrue(c.core().activeAssemblies() == 1, "Failed assembly should stop counting as active fuel");
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 700)
    public static void fuelFailureTripsTheReactor(GameTestHelper h) {
        ReactorControllerBlockEntity c = loop(h, true);
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> h.setBlock(TestVessels.REDSTONE, Blocks.REDSTONE_BLOCK))
                .thenIdle(300)
                .thenExecute(() -> {
                    h.assertTrue(c.isReacting() && !c.isTripped(), "Reactor should be running before the failure: " + c.status());
                    c.core().setAssembly(COLUMN_A, c.core().assembly(COLUMN_A).withCondition(1e-7, "overheat"));
                    c.core().setColumnHeat(2, -1, (900 - CMThermal.AMBIENT) * CMThermal.FUEL_HEAT_CAPACITY * 3);
                })
                .thenIdle(3)
                .thenExecute(() -> h.assertTrue(c.isTripped() && c.tripReason().equals("fuel_failure"), "Fuel failure should trip: " + c.tripReason()))
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void servicePortHatchTurnsOpenAndShut(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.COOLED, false);
        ServicePortBlockEntity port = h.getBlockEntity(TestVessels.SERVICE);
        port.setLid(false);
        ItemStack fuel = FuelAssemblyItem.stackFor(FuelState.fresh());
        h.assertTrue("criticalmass.service.closed".equals(port.refuseReason(fuel)), "A shut hatch should refuse fuel: " + port.refuseReason(fuel));
        h.assertTrue(port.insert(fuel, true).getCount() == fuel.getCount(), "A shut hatch should take nothing");
        BlockPos above = TestVessels.SERVICE.above();
        h.setBlock(above, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        CreativeMotorBlockEntity motor = h.getBlockEntity(above);
        motor.generatedSpeed.setValue(64);
        boolean[] first = new boolean[1];
        h.startSequence()
                .thenIdle(30)
                .thenExecute(() -> {
                    first[0] = port.open();
                    motor.generatedSpeed.setValue(-64);
                })
                .thenIdle(30)
                .thenExecute(() -> {
                    h.assertTrue(first[0] != port.open(), "Turning one way should open the hatch and the other way shut it: " + first[0] + " then " + port.open());
                    motor.generatedSpeed.setValue(first[0] ? 64 : -64);
                })
                .thenIdle(30)
                .thenExecute(() -> {
                    h.assertTrue(port.open(), "Turning it back should open the hatch again");
                    h.assertTrue(port.refuseReason(fuel) == null, "An open hatch on a cold vessel should take fuel: " + port.refuseReason(fuel));
                    BlockPos at = h.absolutePos(TestVessels.SERVICE);
                    h.assertTrue(ArmInteractionPointType.getPrimaryType(h.getLevel(), at, h.getLevel().getBlockState(at)) instanceof ServicePortArmType, "A Mechanical Arm should be able to pick the port as a target");
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void servicePortRoundTripKeepsState(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.COOLED, false);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        ServicePortBlockEntity port = h.getBlockEntity(TestVessels.SERVICE);
        FuelState a = FuelState.fresh().withRemaining(0.7).withBurned(1234).withCondition(0.9, "wear");
        FuelState b = FuelState.fresh().withRemaining(0.4).withBurned(77);
        List<FuelState> originals = List.of(a, b);
        boolean[] done = new boolean[1];
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertTrue(port.refuseReason(assembly(a)) == null, "Cold vessel with free channels should accept fuel: " + port.refuseReason(assembly(a)));
                    h.assertTrue(port.handler().insertItem(0, assembly(a), true).isEmpty(), "Simulated insert should accept");
                    h.assertTrue(c.core().assemblies().isEmpty(), "Simulated insert must not load fuel");
                    h.assertTrue(port.handler().insertItem(0, assembly(a), false).isEmpty(), "Insert A");
                    h.assertTrue(port.handler().insertItem(0, assembly(b), false).isEmpty(), "Insert B");
                    ItemStack third = assembly(FuelState.fresh());
                    h.assertTrue(port.handler().insertItem(0, third, false) == third, "Third assembly has no free channel");
                    h.assertTrue("criticalmass.service.no_channel".equals(port.refuseReason(third)), "Reason should be no free channel: " + port.refuseReason(third));
                    h.assertTrue(port.handler().insertItem(0, assembly(FuelAssemblyItem.defaultState(FuelAssemblyItem.Form.DAMAGED)), false).getCount() == 1, "Damaged fuel is refused");
                    h.assertTrue(port.handler().insertItem(0, new ItemStack(CMItems.SPENT_FUEL_ASSEMBLY.get()), false).getCount() == 1, "Spent fuel is refused");
                })
                .thenExecute(() -> {
                    List<FuelState> loaded = new ArrayList<>(c.core().assemblies().values());
                    for (int i = 0; i < 2; i++) {
                        h.assertTrue(sameState(loaded.get(i), originals.get(i)), "Loaded state should equal the inserted item: " + loaded.get(i));
                    }
                })
                .thenExecute(() -> cycle(h, c, port, originals, 3, done))
                .thenWaitUntil(() -> h.assertTrue(done[0], "Insert and extract cycles should finish"))
                .thenSucceed();
    }

    private static void cycle(GameTestHelper h, ReactorControllerBlockEntity c, ServicePortBlockEntity port, List<FuelState> originals, int rounds, boolean[] done) {
        List<FuelState> out = new ArrayList<>();
        h.setBlock(PORT_POWER, Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(4, () -> step(h, c, port, originals, rounds, out, done));
    }

    private static void step(GameTestHelper h, ReactorControllerBlockEntity c, ServicePortBlockEntity port, List<FuelState> originals, int rounds, List<FuelState> out, boolean[] done) {
        ItemStack taken = port.handler().extractItem(1, 1, false);
        if (!taken.isEmpty()) {
            out.add(state(taken));
        }
        if (out.size() < originals.size()) {
            h.runAfterDelay(2, () -> step(h, c, port, originals, rounds, out, done));
            return;
        }
        for (FuelState o : originals) {
            boolean found = false;
            for (FuelState s : out) {
                found |= sameState(s, o);
            }
            h.assertTrue(found, "Extracted assemblies should match what went in, round " + rounds + ": " + out);
        }
        h.assertTrue(c.core().assemblies().isEmpty(), "Every assembly should be out");
        h.setBlock(PORT_POWER, Blocks.AIR);
        if (rounds <= 1) {
            done[0] = true;
            return;
        }
        h.runAfterDelay(2, () -> {
            for (FuelState o : originals) {
                h.assertTrue(port.handler().insertItem(0, assembly(o), false).isEmpty(), "Reinsert round " + rounds);
            }
            h.runAfterDelay(2, () -> cycle(h, c, port, originals, rounds - 1, done));
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 700)
    public static void portRefusesWhileRunning(GameTestHelper h) {
        ReactorControllerBlockEntity c = loop(h, false);
        ServicePortBlockEntity port = h.getBlockEntity(TestVessels.SERVICE);
        FuelState spent = FuelState.fresh().withRemaining(0);
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    c.loadFuel(2, -1, FuelState.fresh());
                    h.setBlock(TestVessels.REDSTONE, Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(300)
                .thenExecute(() -> {
                    h.assertTrue(c.isReacting(), "Reactor should be running: " + c.status());
                    c.loadFuel(2, -1, spent);
                    ItemStack fresh = assembly(FuelState.fresh());
                    h.assertTrue("criticalmass.service.running".equals(port.refuseReason(fresh)), "Insertion should be refused while running: " + port.refuseReason(fresh));
                    h.assertTrue(port.handler().insertItem(0, fresh, false) == fresh, "Funnel insertion should bounce while running");
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    h.assertTrue(port.output().isEmpty() && c.core().assemblies().size() == 1, "Spent fuel is not extracted while running");
                    h.setBlock(TestVessels.REDSTONE, Blocks.AIR);
                })
                .thenIdle(10)
                .thenExecute(() -> h.assertTrue(!port.output().isEmpty() && sameLifecycle(state(port.output()), spent), "Spent fuel should come out once the reactor stops"))
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void fullOutputBlocksRefuelingAndKeepsWaste(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.COOLED, false);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        ServicePortBlockEntity port = h.getBlockEntity(TestVessels.SERVICE);
        FuelState w1 = FuelState.fresh().withRemaining(0);
        FuelState w2 = FuelState.fresh().withCondition(0, "overheat");
        Set<UUID> waste = Set.of(w1.id(), w2.id());
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    c.loadFuel(2, -1, w1);
                    c.loadFuel(2, 1, w2);
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertTrue(!port.output().isEmpty(), "Waste should move into the port output");
                    ItemStack fresh = assembly(FuelState.fresh());
                    h.assertTrue("criticalmass.service.output_full".equals(port.refuseReason(fresh)), "Full output should block refueling: " + port.refuseReason(fresh));
                    h.assertTrue(port.status().equals("criticalmass.service.output_full"), "Port status should show why: " + port.status());
                    h.assertTrue(port.handler().insertItem(0, fresh, false) == fresh, "Funnel refuel should bounce while output is full");
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    Set<UUID> seen = new HashSet<>();
                    for (FuelState s : c.core().assemblies().values()) {
                        seen.add(s.id());
                    }
                    seen.add(state(port.output()).id());
                    h.assertTrue(seen.equals(waste), "No waste may vanish while the output waits: " + seen);
                    ItemStack first = port.handler().extractItem(1, 1, false);
                    h.assertTrue(waste.contains(state(first).id()), "Extracted waste keeps its lifecycle ID");
                    h.assertTrue(first.is(state(first).form().item()), "Extracted item form follows its state");
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    ItemStack second = port.handler().extractItem(1, 1, false);
                    h.assertTrue(!second.isEmpty() && waste.contains(state(second).id()), "Second waste assembly should follow");
                    h.assertTrue(second.is(CMItems.DAMAGED_FUEL_ASSEMBLY.get()) || second.is(CMItems.SPENT_FUEL_ASSEMBLY.get()), "Waste leaves as spent or damaged");
                    ItemStack fresh = assembly(FuelState.fresh());
                    h.assertTrue(port.handler().insertItem(0, fresh, false).isEmpty(), "Refueling resumes once the output is clear");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void rackCoolsAndDryRackHeats(GameTestHelper h) {
        BlockPos wetPos = new BlockPos(1, 1, 1);
        BlockPos dryPos = new BlockPos(4, 1, 1);
        BlockPos copyPos = new BlockPos(1, 1, 5);
        h.setBlock(wetPos, CMBlocks.SPENT_FUEL_RACK.get());
        h.setBlock(dryPos, CMBlocks.SPENT_FUEL_RACK.get());
        SpentFuelRackBlockEntity wet = h.getBlockEntity(wetPos);
        SpentFuelRackBlockEntity dry = h.getBlockEntity(dryPos);
        wet.coolant().set(new FluidStack(Fluids.WATER, CMThermal.RACK_CAPACITY), 0);
        FuelState hot = FuelState.fresh().withRemaining(0).withBurned(5000).withCondition(0.8, "wear").discharged(10);
        FuelState hot2 = FuelState.fresh().withRemaining(0).discharged(10);
        HolderLookup.Provider reg = h.getLevel().registryAccess();
        h.assertTrue(wet.handler().insertItem(0, assembly(hot), false).isEmpty(), "Rack accepts spent fuel");
        h.assertTrue(dry.handler().insertItem(0, assembly(hot2), false).isEmpty(), "Dry rack accepts spent fuel");
        h.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    FuelState w = state(wet.slots().get(0));
                    FuelState d = state(dry.slots().get(0));
                    h.assertTrue(w.residual() < 10 && w.coolingProgress() > 0.05, "Racked assembly should cool: " + w);
                    h.assertTrue(sameLifecycle(w, hot) && w.dischargeResidual() == hot.dischargeResidual(), "Only cooling changes while racked: " + w);
                    h.assertTrue(Math.abs(w.residual() - d.residual()) < 1e-9, "Decay follows the same physics wet or dry");
                    h.assertTrue(wet.rackTemperature() < 25, "Flooded rack stays cool: " + wet.rackTemperature());
                    h.assertTrue(dry.rackTemperature() > 60, "Dry rack heats up: " + dry.rackTemperature());
                    CompoundTag tag = wet.saveWithoutMetadata(reg);
                    h.setBlock(copyPos, CMBlocks.SPENT_FUEL_RACK.get());
                    SpentFuelRackBlockEntity copy = h.getBlockEntity(copyPos);
                    copy.loadWithComponents(tag, reg);
                    h.assertTrue(sameState(state(copy.slots().get(0)), w), "Cooling progress persists through save and load");
                    h.assertTrue(copy.coolant().amount() == wet.coolant().amount(), "Rack water persists");
                    copy.slots().set(0, ItemStack.EMPTY);
                    dry.slots().set(0, assembly(d.withCondition(0.0001, "wear")));
                    dry.setDryHeat((CMThermal.RACK_DRY_DAMAGE_TEMPERATURE + 300 - CMThermal.AMBIENT) * CMThermal.RACK_DRY_HEAT_CAPACITY);
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    ItemStack s = dry.slots().get(0);
                    FuelState d = state(s);
                    h.assertTrue(d.damaged() && d.damageCause().equals("dry_storage") && s.is(CMItems.DAMAGED_FUEL_ASSEMBLY.get()), "Overheated dry rack damages fuel: " + d);
                    h.assertTrue(d.id().equals(hot2.id()), "Damage keeps the lifecycle ID");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void rackBlockStateShowsAssemblyCount(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, CMBlocks.SPENT_FUEL_RACK.get());
        SpentFuelRackBlockEntity rack = h.getBlockEntity(pos);
        h.assertTrue(h.getBlockState(pos).getValue(SpentFuelRackBlock.FILL) == 0, "Empty rack shows no assemblies");
        for (int i = 1; i <= 4; i++) {
            rack.insert(assembly(FuelState.fresh().withRemaining(0).discharged(10)));
            h.assertTrue(h.getBlockState(pos).getValue(SpentFuelRackBlock.FILL) == i, "Rack shows " + i + " assemblies");
        }
        rack.takeLast();
        h.assertTrue(h.getBlockState(pos).getValue(SpentFuelRackBlock.FILL) == 3, "Taking one out updates the rack");
        rack.handler().extractItem(0, 1, false);
        h.assertTrue(h.getBlockState(pos).getValue(SpentFuelRackBlock.FILL) == 2, "Extracting through the item handler updates the rack");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void caskPreservesContentsOnPickupAndPlace(GameTestHelper h) {
        BlockPos caskPos = new BlockPos(2, 1, 2);
        BlockPos placed = new BlockPos(5, 1, 5);
        h.setBlock(caskPos, CMBlocks.SHIELDED_CASK.get());
        ShieldedCaskBlockEntity cask = h.getBlockEntity(caskPos);
        ItemStack cooled = assembly(FuelState.fresh().withRemaining(0).withBurned(9000).discharged(0.2));
        ItemStack fresh = assembly(FuelState.fresh().withRemaining(0.5));
        ItemStack canister = new ItemStack(CMItems.SEALED_WASTE_CANISTER.get());
        ItemStack hotSpent = assembly(FuelState.fresh().withRemaining(0).discharged(8));
        ItemStack damaged = assembly(FuelState.fresh().withCondition(0, "overheat"));
        h.assertTrue("criticalmass.cask.too_hot".equals(ShieldedCaskBlockEntity.refuseReason(hotSpent)), "Hot spent fuel is refused");
        h.assertTrue("criticalmass.cask.damaged".equals(ShieldedCaskBlockEntity.refuseReason(damaged)), "Damaged fuel is refused");
        h.assertTrue(!cask.insert(cooled.copy()) && cask.handler().insertItem(0, cooled.copy(), false).getCount() == 1, "A shut cask takes nothing by hand or by funnel");
        cask.setLid(true);
        h.assertTrue(cask.handler().insertItem(0, hotSpent, false).getCount() == 1, "Funnel cannot insert hot fuel");
        h.assertTrue(cask.insert(cooled) && cask.insert(fresh) && cask.insert(canister), "Cask accepts cooled fuel, fresh fuel and canisters");
        List<ItemStack> before = new ArrayList<>();
        for (ItemStack s : cask.slots()) {
            before.add(s.copy());
        }
        cask.setLid(false);
        h.assertTrue(cask.takeLast().isEmpty() && cask.handler().extractItem(0, 1, false).isEmpty(), "A shut cask gives nothing up");
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    h.getLevel().destroyBlock(h.absolutePos(caskPos), true);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    List<ItemEntity> drops = items(h);
                    h.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(CMBlocks.SHIELDED_CASK.get().asItem()), "Only the cask itself should drop: " + drops.size());
                    ItemStack item = drops.get(0).getItem();
                    ItemContainerContents contents = item.get(DataComponents.CONTAINER);
                    h.assertTrue(contents != null, "Cask item should carry its contents");
                    h.setBlock(placed, CMBlocks.SHIELDED_CASK.get());
                    ShieldedCaskBlockEntity again = h.getBlockEntity(placed);
                    again.applyComponentsFromItemStack(item);
                    for (int i = 0; i < before.size(); i++) {
                        h.assertTrue(ItemStack.matches(before.get(i), again.slots().get(i)), "Slot " + i + " should be identical after pickup and place");
                    }
                    drops.get(0).discard();
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void wasteIsNeverDeletedWhenBlocksBreak(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.COOLED);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        ServicePortBlockEntity port = h.getBlockEntity(TestVessels.SERVICE);
        Set<UUID> ids = new HashSet<>();
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    FuelState spent = c.core().assembly(COLUMN_A).withRemaining(0);
                    c.core().setAssembly(COLUMN_A, spent);
                    for (FuelState s : c.core().assemblies().values()) {
                        ids.add(s.id());
                    }
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertTrue(!port.output().isEmpty(), "Spent assembly should wait in the port");
                    h.getLevel().destroyBlock(h.absolutePos(TestVessels.SERVICE), true);
                    h.getLevel().destroyBlock(h.absolutePos(TestVessels.CONTROLLER), true);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    Set<UUID> found = new HashSet<>();
                    for (ItemEntity e : items(h)) {
                        FuelState s = state(e.getItem());
                        if (s != null) {
                            found.add(s.id());
                        }
                        e.discard();
                    }
                    h.assertTrue(found.equals(ids), "Broken port and controller should drop every assembly: " + found + " vs " + ids);
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void stateSurvivesReloadEverywhere(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.COOLED);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        HolderLookup.Provider reg = h.getLevel().registryAccess();
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    c.core().setAssembly(COLUMN_A, c.core().assembly(COLUMN_A).withRemaining(0.31).withBurned(42).withCondition(0.66, "wear"));
                    List<FuelState> before = new ArrayList<>(c.core().assemblies().values());
                    c.loadWithComponents(c.saveWithoutMetadata(reg), reg);
                    List<FuelState> after = new ArrayList<>(c.core().assemblies().values());
                    h.assertTrue(before.size() == after.size(), "Channel assemblies survive reload");
                    for (int i = 0; i < before.size(); i++) {
                        h.assertTrue(sameState(before.get(i), after.get(i)), "Channel state survives reload: " + after.get(i));
                    }
                    ItemStack stack = assembly(FuelState.fresh().withRemaining(0).withCondition(0.5, "wear").discharged(3));
                    ItemStack parsed = ItemStack.parseOptional(reg, (CompoundTag) stack.save(reg));
                    h.assertTrue(ItemStack.matches(stack, parsed), "Inventory item state survives reload");
                    ItemEntity entity = new ItemEntity(h.getLevel(), 0, 0, 0, stack.copy());
                    CompoundTag tag = new CompoundTag();
                    entity.saveWithoutId(tag);
                    ItemEntity loaded = new ItemEntity(EntityType.ITEM, h.getLevel());
                    loaded.load(tag);
                    h.assertTrue(ItemStack.matches(stack, loaded.getItem()), "Dropped item state survives reload");
                    h.assertTrue(stack.getMaxStackSize() == 1 && new ItemStack(CMItems.SEALED_WASTE_CANISTER.get()).getMaxStackSize() == 1, "Stateful items never stack");
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void assemblingABodyDoesNotDuplicateFuel(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.COOLED);
        BlockPos rackPos = new BlockPos(9, 1, 3);
        BlockPos caskPos = new BlockPos(9, 1, 5);
        h.setBlock(rackPos, CMBlocks.SPENT_FUEL_RACK.get());
        h.setBlock(caskPos, CMBlocks.SHIELDED_CASK.get());
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        Set<UUID> ids = new HashSet<>();
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    c.core().setAssembly(COLUMN_A, c.core().assembly(COLUMN_A).withRemaining(0));
                    FuelState r = FuelState.fresh().withRemaining(0);
                    FuelState k = FuelState.fresh().withRemaining(0);
                    SpentFuelRackBlockEntity rack = h.getBlockEntity(rackPos);
                    ShieldedCaskBlockEntity cask = h.getBlockEntity(caskPos);
                    rack.insert(assembly(r));
                    cask.setLid(true);
                    cask.insert(assembly(k));
                    ids.add(r.id());
                    ids.add(k.id());
                    for (FuelState s : c.core().assemblies().values()) {
                        ids.add(s.id());
                    }
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    ServicePortBlockEntity port = h.getBlockEntity(TestVessels.SERVICE);
                    h.assertTrue(!port.output().isEmpty(), "The spent assembly should wait in the port before assembly");
                    List<BlockPos> blocks = new ArrayList<>();
                    for (BlockPos p : BlockPos.betweenClosed(h.absolutePos(new BlockPos(0, 1, 0)), h.absolutePos(new BlockPos(10, 6, 8)))) {
                        if (!h.getLevel().getBlockState(p).isAir()) {
                            blocks.add(p.immutable());
                        }
                    }
                    SubLevelAssemblyHelper.assembleBlocks(h.getLevel(), h.absolutePos(TestVessels.CONTROLLER), blocks,
                            new BoundingBox3i(h.absolutePos(new BlockPos(0, 1, 0)), h.absolutePos(new BlockPos(10, 6, 8))));
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    h.assertTrue(items(h).stream().noneMatch(e -> state(e.getItem()) != null), "Assembly must not drop fuel: " + items(h).size() + " items");
                    Set<UUID> moved = new HashSet<>();
                    for (BlockEntity be : LoadedThermalBlocks.snapshot()) {
                        if (be.getLevel() != h.getLevel() || SableCompanion.INSTANCE.getContaining(be) == null) {
                            continue;
                        }
                        List<ItemStack> held = new ArrayList<>();
                        if (be instanceof ReactorControllerBlockEntity rc) {
                            for (FuelState s : rc.core().assemblies().values()) {
                                moved.add(s.id());
                            }
                        } else if (be instanceof ServicePortBlockEntity p) {
                            held.add(p.output());
                        } else if (be instanceof SpentFuelRackBlockEntity r) {
                            held.addAll(r.slots());
                        } else if (be instanceof ShieldedCaskBlockEntity k) {
                            held.addAll(k.slots());
                        }
                        for (ItemStack st : held) {
                            if (state(st) != null && ids.contains(state(st).id())) {
                                moved.add(state(st).id());
                            }
                        }
                    }
                    h.assertTrue(moved.equals(ids), "Every assembly should be on the body exactly once: " + moved.size() + " of " + ids.size());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY)
    public static void caskLidIsUnboltedWithAWrenchOrHeldUpByRedstone(GameTestHelper h) {
        BlockPos caskPos = new BlockPos(2, 1, 2);
        h.setBlock(caskPos, CMBlocks.SHIELDED_CASK.get());
        ShieldedCaskBlockEntity cask = h.getBlockEntity(caskPos);
        h.assertTrue(cask.sealed() && !cask.open(), "A cask should start shut");
        cask.toggle();
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(cask.open(), "Unbolting should raise the lid: " + cask.lid.getValue());
                    cask.toggle();
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(cask.sealed(), "Bolting it again should seat the lid: " + cask.lid.getValue());
                    h.setBlock(caskPos.north(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(cask.open(), "A redstone signal should hold the lid up: " + cask.lid.getValue());
                    h.setBlock(caskPos.north(), net.minecraft.world.level.block.Blocks.AIR);
                })
                .thenIdle(20)
                .thenExecute(() -> h.assertTrue(cask.sealed(), "The lid should come down when the signal goes: " + cask.lid.getValue()))
                .thenSucceed();
    }
}
