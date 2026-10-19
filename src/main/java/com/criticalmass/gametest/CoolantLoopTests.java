package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMFluids;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class CoolantLoopTests {
    private static final String EMPTY = "empty";

    private CoolantLoopTests() {
    }

    static FluidStack coolant(int amount) {
        return new FluidStack(CMFluids.PRIMARY_COOLANT.source.get(), amount);
    }

    static boolean close(double a, double b) {
        return Math.abs(a - b) <= 1e-6 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
    }

    private static double ledger(ReactorControllerBlockEntity c, SteamTurbineBlockEntity t) {
        return c.coolant().heat() + c.core().storedHeat() + t.coolant().heat() + t.heatExtracted();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void vesselHeatSurvivesSaveAndLoad(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity be = h.getBlockEntity(TestVessels.CONTROLLER);
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    be.coolant().set(coolant(4321), 98765.4321);
                    be.core().setColumnHeat(1, 0, 5555.5);
                    double core = be.core().storedHeat();
                    h.assertTrue(core == 5555.5, "Column heat was not set: " + core);
                    CompoundTag saved = be.saveWithFullMetadata(h.getLevel().registryAccess());
                    BlockEntity loaded = BlockEntity.loadStatic(h.absolutePos(TestVessels.CONTROLLER), be.getBlockState(), saved, h.getLevel().registryAccess());
                    h.assertTrue(loaded instanceof ReactorControllerBlockEntity, "Saved controller did not load back as a controller");
                    ReactorControllerBlockEntity copy = (ReactorControllerBlockEntity) loaded;
                    h.assertTrue(copy.coolant().amount() == 4321 && copy.coolant().heat() == 98765.4321, "Coolant changed across save/load");
                    h.assertTrue(copy.core().storedHeat() == core, "Core heat changed across save/load: " + copy.core().storedHeat());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 700)
    public static void reactorLoopConservesCoolantAndHeat(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        TestVessels.loop(h, true);
        ReactorControllerBlockEntity controller = h.getBlockEntity(TestVessels.CONTROLLER);
        controller.coolant().set(coolant(6000), 0);
        h.setBlock(TestVessels.REDSTONE, Blocks.REDSTONE_BLOCK);
        int[] volumes = new int[2];
        h.startSequence()
                .thenIdle(300)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(TestVessels.PUMP);
                    SteamTurbineBlockEntity turbine = h.getBlockEntity(TestVessels.TURBINE);
                    h.assertTrue(controller.isReacting(), "Reactor should run: " + controller.status() + " problems " + controller.problemReasons() + " missing " + controller.missing());
                    h.assertTrue(pump.flow() == CMThermal.pumpFlow(pump.getSpeed()) && pump.flow() > 0, "Pump flow " + pump.flow() + " status " + pump.status());
                    h.assertTrue(controller.core().lastExtracted() > 0, "Coolant channels extracted nothing");
                    int total = controller.coolant().amount() + turbine.coolant().amount();
                    h.assertTrue(total == 6000, "Coolant volume not conserved: " + total);
                    h.assertTrue(close(ledger(controller, turbine), controller.heatAdded()), "Heat not conserved: " + ledger(controller, turbine) + " vs " + controller.heatAdded());
                    h.assertTrue(turbine.heatExtracted() > 0, "Turbine extracted no heat");
                    h.setBlock(TestVessels.MOTOR, Blocks.AIR);
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(TestVessels.PUMP);
                    SteamTurbineBlockEntity turbine = h.getBlockEntity(TestVessels.TURBINE);
                    h.assertTrue(pump.getSpeed() == 0 && pump.flow() == 0, "Pump kept flowing without rotation");
                    h.assertTrue(controller.core().lastExtracted() == 0, "Coolant channels extracted heat without flow");
                    volumes[0] = controller.coolant().amount();
                    volumes[1] = turbine.coolant().amount();
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    SteamTurbineBlockEntity turbine = h.getBlockEntity(TestVessels.TURBINE);
                    h.assertTrue(controller.coolant().amount() == volumes[0] && turbine.coolant().amount() == volumes[1], "Coolant moved with the shaft stopped");
                    h.assertTrue(close(ledger(controller, turbine), controller.heatAdded()), "Heat not conserved after stopping");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void invalidVesselTripsAndLatches(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity controller = h.getBlockEntity(TestVessels.CONTROLLER);
        controller.coolant().set(coolant(6000), 0);
        h.setBlock(TestVessels.REDSTONE, Blocks.REDSTONE_BLOCK);
        BlockPos casing = new BlockPos(5, 2, 2);
        double[] mark = new double[1];
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> h.assertTrue(controller.isReacting(), "Reactor should start: " + controller.status()))
                .thenExecute(() -> h.setBlock(casing, Blocks.AIR))
                .thenIdle(3)
                .thenExecute(() -> {
                    h.assertTrue(!controller.isReacting() && controller.isTripped(), "Breaking the vessel should trip the reactor: " + controller.status());
                    h.assertTrue(controller.problemPositions().contains(h.absolutePos(casing)), "Missing casing not reported at its position");
                    mark[0] = controller.heatAdded();
                    h.setBlock(casing, CMBlocks.REACTOR_VESSEL_CASING.get());
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertTrue(controller.isTripped() && !controller.isReacting(), "Trip should latch until the signal is removed");
                    h.assertTrue(controller.heatAdded() == mark[0], "Tripped reactor produced heat");
                    h.setBlock(TestVessels.REDSTONE, Blocks.AIR);
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    h.assertTrue(!controller.isTripped(), "Trip should clear with the signal off");
                    h.setBlock(TestVessels.REDSTONE, Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(3)
                .thenExecute(() -> h.assertTrue(controller.isReacting(), "Reactor should restart: " + controller.status()))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void unpoweredReactorGivesNoCapacity(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        TestVessels.loop(h, true);
        ReactorControllerBlockEntity controller = h.getBlockEntity(TestVessels.CONTROLLER);
        controller.coolant().set(coolant(6000), 0);
        h.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(TestVessels.PUMP);
                    SteamTurbineBlockEntity turbine = h.getBlockEntity(TestVessels.TURBINE);
                    h.assertTrue(pump.flow() > 0, "Pump should circulate: " + pump.status());
                    h.assertTrue(!controller.isReacting() && controller.heatAdded() == 0, "Rods fully inserted should give no reaction heat");
                    h.assertTrue(turbine.stressCapacity() == 0 && turbine.getGeneratedSpeed() == 0, "Turbine produced capacity with no delivered heat");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void turbineCapacityScalesWithHeatNotSpeed(GameTestHelper h) {
        BlockPos a = new BlockPos(2, 2, 2);
        BlockPos b = new BlockPos(5, 2, 5);
        BlockState turbine = CMBlocks.STEAM_TURBINE.get().defaultBlockState().setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, Direction.Axis.X);
        h.setBlock(a, turbine);
        h.setBlock(b, turbine);
        SteamTurbineBlockEntity low = h.getBlockEntity(a);
        SteamTurbineBlockEntity high = h.getBlockEntity(b);
        double base = 20000;
        h.onEachTick(() -> {
            low.coolant().set(coolant(CMThermal.TURBINE_CAPACITY), base);
            high.coolant().set(coolant(CMThermal.TURBINE_CAPACITY), base * 2);
        });
        h.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    double expectedLow = base * CMThermal.TURBINE_EXTRACTION * CMThermal.SU_PER_HEAT;
                    h.assertTrue(Math.abs(low.stressCapacity() - expectedLow) / expectedLow < 0.03, "Capacity " + low.stressCapacity() + " expected about " + expectedLow);
                    double ratio = high.stressCapacity() / low.stressCapacity();
                    h.assertTrue(Math.abs(ratio - 2.0) < 0.06, "Doubling delivered heat should double capacity, ratio " + ratio);
                    h.assertTrue(low.getGeneratedSpeed() == CMThermal.TURBINE_RPM && high.getGeneratedSpeed() == CMThermal.TURBINE_RPM, "Turbine speed should be fixed");
                })
                .thenSucceed();
    }
}
