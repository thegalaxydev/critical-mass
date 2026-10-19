package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.CoolantPortBlock;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.HeatExchangerBlockEntity;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.registry.CMStress;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class PlantTests {
    private static final String LARGE = "empty_large";

    private PlantTests() {
    }

    private static FluidStack water(int amount) {
        return new FluidStack(Fluids.WATER, amount);
    }

    private static boolean close(double a, double b) {
        return Math.abs(a - b) <= 1e-7 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
    }

    @GameTest(template = LARGE)
    public static void createPipesDoNotConnectToPorts(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        BlockPos pipe = TestVessels.OUTLET.south();
        BlockPos portAbs = h.absolutePos(TestVessels.OUTLET);
        boolean port = FluidPipeBlock.canConnectTo(h.getLevel(), portAbs, h.getLevel().getBlockState(portAbs), Direction.NORTH);
        BlockPos tank = new BlockPos(10, 1, 10);
        h.setBlock(tank, AllBlocks.FLUID_TANK.getDefaultState());
        BlockPos tankAbs = h.absolutePos(tank);
        boolean control = FluidPipeBlock.canConnectTo(h.getLevel(), tankAbs, h.getLevel().getBlockState(tankAbs), Direction.NORTH);
        h.assertTrue(!port, "A Create fluid pipe would connect to a primary coolant port at " + pipe);
        h.assertTrue(control, "Control failed: a Create fluid pipe should connect to a fluid tank");
        h.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void portModesSetFlowDirection(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        TestVessels.loop(h, true);
        ReactorControllerBlockEntity controller = h.getBlockEntity(TestVessels.CONTROLLER);
        controller.coolant().set(CoolantLoopTests.coolant(6000), 0);
        h.setBlock(TestVessels.OUTLET, TestVessels.port(Direction.SOUTH, CoolantPortBlock.Mode.INLET));
        h.setBlock(TestVessels.INLET, TestVessels.port(Direction.NORTH, CoolantPortBlock.Mode.OUTLET));
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(TestVessels.PUMP);
                    h.assertTrue(pump.flow() == 0 && pump.status().startsWith("criticalmass.circuit.needs_"), "Reversed ports should stop the pump: " + pump.status());
                    h.setBlock(TestVessels.OUTLET, TestVessels.port(Direction.SOUTH, CoolantPortBlock.Mode.OUTLET));
                    h.setBlock(TestVessels.INLET, TestVessels.port(Direction.NORTH, CoolantPortBlock.Mode.INLET));
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(TestVessels.PUMP);
                    h.assertTrue(pump.flow() > 0, "Correct ports should circulate: " + pump.status());
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void pumpStressFollowsFlow(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        TestVessels.loop(h, true);
        ReactorControllerBlockEntity controller = h.getBlockEntity(TestVessels.CONTROLLER);
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(TestVessels.PUMP);
                    float idle = pump.calculateStressApplied();
                    h.assertTrue(pump.flow() == 0 && Math.abs(idle - CMStress.CIRCULATION_PUMP_IMPACT * CMThermal.PUMP_IDLE_STRESS) < 1e-4, "Idle pump stress " + idle);
                    controller.coolant().set(CoolantLoopTests.coolant(6000), 0);
                })
                .thenIdle(30)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(TestVessels.PUMP);
                    float running = pump.calculateStressApplied();
                    h.assertTrue(pump.flow() > 0 && Math.abs(running - CMStress.CIRCULATION_PUMP_IMPACT) < 1e-4, "Running pump stress " + running + " flow " + pump.flow());
                    CreativeMotorBlockEntity motor = h.getBlockEntity(TestVessels.MOTOR);
                    motor.generatedSpeed.setValue(256);
                })
                .thenIdle(30)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(TestVessels.PUMP);
                    float total = pump.calculateStressApplied() * Math.abs(pump.getSpeed());
                    float capped = (float) CMStress.CIRCULATION_PUMP_IMPACT * CMThermal.pumpCapRpm();
                    h.assertTrue(Math.abs(pump.getSpeed()) == 256 && pump.flow() == CMThermal.PUMP_MAX_FLOW, "Pump should run at 256 RPM with capped flow: " + pump.getSpeed() + " " + pump.flow());
                    h.assertTrue(Math.abs(total - capped) < 1e-2, "Stress above the flow cap should stop rising: " + total + " vs " + capped);
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 400)
    public static void exchangerKeepsCircuitsSeparate(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        TestVessels.plant(h);
        ReactorControllerBlockEntity controller = h.getBlockEntity(TestVessels.CONTROLLER);
        HeatExchangerBlockEntity exchanger = h.getBlockEntity(TestVessels.EXCHANGER);
        SteamTurbineBlockEntity turbine = h.getBlockEntity(TestVessels.PLANT_TURBINE);
        controller.coolant().set(CoolantLoopTests.coolant(6000), 300000);
        exchanger.secondary().set(water(1000), 0);
        turbine.coolant().set(water(1000), 0);
        h.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    h.assertTrue(exchanger.primary().fluid().is(CoolantLoopTests.coolant(1).getFluid()), "Primary side should hold primary coolant");
                    h.assertTrue(exchanger.secondary().fluid().is(Fluids.WATER) && turbine.coolant().fluid().is(Fluids.WATER), "Water left the secondary circuit or coolant entered it");
                    h.assertTrue(controller.coolant().fluid().is(CoolantLoopTests.coolant(1).getFluid()), "Water reached the vessel");
                    h.assertTrue(controller.coolant().amount() + exchanger.primary().amount() == 6000, "Primary volume changed");
                    h.assertTrue(exchanger.secondary().amount() + turbine.coolant().amount() == 2000, "Secondary volume changed");
                    h.assertTrue(exchanger.exchanged() > 0, "No heat crossed the exchanger");
                    h.assertTrue(turbine.heatExtracted() > 0, "Turbine received no heat");
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 10300)
    public static void plantConservesHeatAndFluidFor10000Ticks(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.COOLED);
        TestVessels.plant(h);
        ReactorControllerBlockEntity controller = h.getBlockEntity(TestVessels.CONTROLLER);
        HeatExchangerBlockEntity exchanger = h.getBlockEntity(TestVessels.EXCHANGER);
        SteamTurbineBlockEntity turbine = h.getBlockEntity(TestVessels.PLANT_TURBINE);
        controller.coolant().set(CoolantLoopTests.coolant(6000), 0);
        exchanger.secondary().set(water(1000), 0);
        turbine.coolant().set(water(1000), 0);
        h.setBlock(TestVessels.REDSTONE, Blocks.REDSTONE_BLOCK);
        int[] checks = new int[1];
        String[] failure = new String[1];
        h.onEachTick(() -> {
            if (failure[0] != null) {
                return;
            }
            int primary = controller.coolant().amount() + exchanger.primary().amount();
            int secondary = exchanger.secondary().amount() + turbine.coolant().amount();
            double stored = controller.coolant().heat() + controller.core().storedHeat() + exchanger.primary().heat() + exchanger.secondary().heat() + turbine.coolant().heat();
            double accounted = stored + turbine.heatExtracted();
            if (primary != 6000 || secondary != 2000) {
                failure[0] = "tick " + h.getTick() + ": volume primary " + primary + " secondary " + secondary;
            } else if (!close(accounted, controller.heatAdded())) {
                failure[0] = "tick " + h.getTick() + ": heat " + accounted + " vs added " + controller.heatAdded();
            }
            checks[0]++;
        });
        h.startSequence()
                .thenIdle(10000)
                .thenExecute(() -> {
                    h.assertTrue(failure[0] == null, "Ledger broke at " + failure[0]);
                    h.assertTrue(checks[0] >= 10000, "Only " + checks[0] + " ticks checked");
                    h.assertTrue(controller.isReacting(), "Reactor stopped: " + controller.status());
                    h.assertTrue(turbine.stressCapacity() > 0, "Turbine produced no capacity through the exchanger");
                    h.assertTrue(controller.core().hotSpots() == 0 && controller.damagedBlocks() == 0, "Fully cooled core should not overheat or crack");
                    CriticalMass.LOGGER.info("plant after {} ticks: added {} HU, turbine extracted {} HU, exchanger moved {} HU, capacity {} SU",
                            checks[0], controller.heatAdded(), turbine.heatExtracted(), exchanger.exchanged(), turbine.stressCapacity());
                })
                .thenSucceed();
    }
}
