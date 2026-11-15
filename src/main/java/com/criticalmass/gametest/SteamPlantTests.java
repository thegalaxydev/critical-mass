package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.FacingBlock;
import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.BoilerPlateBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolantBuffer;
import com.criticalmass.content.thermal.HeatExchangerBlockEntity;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.ReliefValveBlockEntity;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class SteamPlantTests {
    private static final String LARGE = "empty_large";
    private static final BlockPos PLATE = new BlockPos(7, 3, 2);
    private static final BlockPos RELIEF = new BlockPos(6, 5, 6);
    private static final BlockPos CONDENSER = new BlockPos(8, 9, 6);

    private SteamPlantTests() {
    }

    private static FluidStack water(int amount) {
        return new FluidStack(Fluids.WATER, amount);
    }

    private static boolean close(double a, double b) {
        return Math.abs(a - b) <= 1e-7 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
    }

    private static void replace(GameTestHelper h, BlockPos pos, BlockState state) {
        h.setBlock(pos, state);
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            BlockState s = h.getBlockState(n);
            if (s.getBlock() instanceof FluidPipeBlock) {
                TestVessels.connect(h, n);
            }
        }
    }

    private static void fillSecondary(GameTestHelper h, double steamEnergyPerMb) {
        HeatExchangerBlockEntity exchanger = h.getBlockEntity(TestVessels.EXCHANGER);
        SteamTurbineBlockEntity turbine = h.getBlockEntity(TestVessels.PLANT_TURBINE);
        double perMb = CMThermal.SATURATION_ENERGY + steamEnergyPerMb;
        exchanger.secondary().set(water(1000), 1000 * perMb);
        turbine.coolant().set(water(1000), 1000 * perMb);
    }

    @GameTest(template = LARGE, timeoutTicks = 3300)
    public static void boilerPlateAndTurbineNeverShareHeat(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.COOLED);
        TestVessels.plant(h);
        replace(h, PLATE, CMBlocks.BOILER_HEATING_PLATE.get().defaultBlockState().setValue(HorizontalBlock.FACING, Direction.SOUTH));
        h.setBlock(PLATE.above(), AllBlocks.FLUID_TANK.getDefaultState());
        h.setBlock(PLATE.above(2), AllBlocks.STEAM_WHISTLE.getDefaultState());
        ReactorControllerBlockEntity controller = h.getBlockEntity(TestVessels.CONTROLLER);
        HeatExchangerBlockEntity exchanger = h.getBlockEntity(TestVessels.EXCHANGER);
        SteamTurbineBlockEntity turbine = h.getBlockEntity(TestVessels.PLANT_TURBINE);
        BoilerPlateBlockEntity plate = h.getBlockEntity(PLATE);
        FluidTankBlockEntity tank = h.getBlockEntity(PLATE.above());
        tank.getTankInventory().fill(water(1000), IFluidHandler.FluidAction.EXECUTE);
        tank.updateBoilerState();
        double preheat = 6000 * 300.0;
        controller.coolant().set(CoolantLoopTests.coolant(6000), preheat);
        fillSecondary(h, 0);
        h.setBlock(TestVessels.REDSTONE, Blocks.REDSTONE_BLOCK);
        String[] failure = new String[1];
        double[] baseline = {Double.NaN};
        h.onEachTick(() -> {
            FluidTankBlockEntity ctrl = tank.getControllerBE();
            if (ctrl != null && ctrl.boiler != null) {
                ctrl.boiler.waterSupply = 200;
            }
            if (failure[0] != null) {
                return;
            }
            double stored = controller.coolant().heat() + controller.core().storedHeat() + exchanger.primary().heat() + exchanger.secondary().heat()
                    + turbine.coolant().heat() + plate.coolant().heat() + TestVessels.pipeHeat(h, TestVessels.PRIMARY_PIPES) + TestVessels.pipeHeat(h, TestVessels.SECONDARY_PIPES);
            double outputs = turbine.heatExtracted() + plate.sinks().boilerHeat;
            if (Double.isNaN(baseline[0])) {
                baseline[0] = stored + outputs - controller.heatAdded();
            }
            if (!close(stored + outputs, baseline[0] + controller.heatAdded())) {
                failure[0] = "tick " + h.getTick() + ": stored " + stored + " + outputs " + outputs + " vs supplied " + (baseline[0] + controller.heatAdded());
            } else if (outputs > baseline[0] + controller.heatAdded() + 1e-6) {
                failure[0] = "tick " + h.getTick() + ": outputs exceed supplied heat";
            }
        });
        h.startSequence()
                .thenIdle(3000)
                .thenExecute(() -> {
                    h.assertTrue(failure[0] == null, "Heat counted twice or lost: " + failure[0]);
                    h.assertTrue(plate.sinks().boilerHeat > 0 && turbine.heatExtracted() > 0, "Both the boiler and the turbine should have drawn heat: boiler " + plate.sinks().boilerHeat + " turbine " + turbine.heatExtracted());
                    CriticalMass.LOGGER.info("boiler+turbine: supplied {} HU, boiler {} HU, turbine {} HU", preheat + controller.heatAdded(), plate.sinks().boilerHeat, turbine.heatExtracted());
                })
                .thenSucceed();
    }

    private static double maxSecondaryPressure(GameTestHelper h, List<BlockPos> nodes) {
        double p = 0;
        for (BlockPos pos : nodes) {
            Object be = h.getBlockEntity(pos);
            if (be instanceof HeatExchangerBlockEntity x) {
                p = Math.max(p, Math.max(x.secondary().pressure(), x.primary().steamMedium() ? x.primary().pressure() : 0));
            } else if (be instanceof SteamTurbineBlockEntity t) {
                p = Math.max(p, t.coolant().pressure());
            } else if (be instanceof ReliefValveBlockEntity v) {
                p = Math.max(p, v.coolant().pressure());
            }
        }
        return p;
    }

    @GameTest(template = LARGE, timeoutTicks = 3300)
    public static void trippedTurbineIsHandledByReliefWithoutRupture(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.COOLED);
        TestVessels.plant(h);
        replace(h, RELIEF, CMBlocks.RELIEF_VALVE.get().defaultBlockState().setValue(FacingBlock.FACING, Direction.EAST));
        ReactorControllerBlockEntity controller = h.getBlockEntity(TestVessels.CONTROLLER);
        controller.coolant().set(CoolantLoopTests.coolant(6000), 6000 * 200.0);
        fillSecondary(h, 0.7 * CMThermal.LATENT_HEAT);
        h.setBlock(TestVessels.REDSTONE, Blocks.REDSTONE_BLOCK);
        List<BlockPos> nodes = List.of(TestVessels.EXCHANGER, TestVessels.PLANT_TURBINE, RELIEF);
        double[] peak = new double[1];
        h.onEachTick(() -> peak[0] = Math.max(peak[0], maxSecondaryPressure(h, nodes)));
        h.startSequence()
                .thenIdle(100)
                .thenExecute(() -> h.setBlock(TestVessels.PLANT_SHAFT, Blocks.AIR))
                .thenIdle(3000)
                .thenExecute(() -> {
                    SteamTurbineBlockEntity turbine = h.getBlockEntity(TestVessels.PLANT_TURBINE);
                    ReliefValveBlockEntity relief = h.getBlockEntity(RELIEF);
                    HeatExchangerBlockEntity exchanger = h.getBlockEntity(TestVessels.EXCHANGER);
                    h.assertTrue(turbine.status().endsWith("disconnected"), "Turbine should report the lost load: " + turbine.status());
                    CirculationPumpBlockEntity pump2 = h.getBlockEntity(TestVessels.PUMP2);
                    h.assertTrue(relief.sinks().ventedVolume > 0, "Pressure should have risen to the relief valve: peak " + peak[0] + " relief " + relief.status() + " " + relief.coolant().amount() + "mB at " + relief.coolant().pressure() + " bar, pump2 " + pump2.status());
                    int ruptures = turbine.sinks().ruptures + relief.sinks().ruptures + exchanger.sinks().ruptures;
                    h.assertTrue(ruptures == 0 && peak[0] < CMThermal.RUPTURE_PRESSURE, "Relief valve should prevent rupture: peak " + peak[0] + " ruptures " + ruptures);
                    CriticalMass.LOGGER.info("turbine trip with relief: peak {} bar, vented {} mB / {} HU", peak[0], relief.sinks().ventedVolume, relief.sinks().ventedHeat);
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 2300)
    public static void trippedTurbineWithCondenserMarginStaysBelowRelief(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.COOLED);
        TestVessels.plant(h);
        replace(h, CONDENSER, CMBlocks.CONDENSER.get().defaultBlockState().setValue(HorizontalBlock.FACING, Direction.EAST));
        ReactorControllerBlockEntity controller = h.getBlockEntity(TestVessels.CONTROLLER);
        controller.coolant().set(CoolantLoopTests.coolant(6000), 6000 * 100.0);
        fillSecondary(h, 0.2 * CMThermal.LATENT_HEAT);
        h.setBlock(TestVessels.REDSTONE, Blocks.REDSTONE_BLOCK);
        HeatExchangerBlockEntity condenser = h.getBlockEntity(CONDENSER);
        double[] removed = new double[1];
        double[] peak = new double[1];
        boolean[] tripped = {false};
        List<BlockPos> nodes = List.of(TestVessels.EXCHANGER, TestVessels.PLANT_TURBINE, CONDENSER);
        h.onEachTick(() -> {
            CoolantBuffer cooling = condenser.secondary();
            removed[0] += cooling.heat();
            cooling.set(water(CMThermal.CONDENSER_CAPACITY), 0);
            condenser.onCoolantPassed(Direction.DOWN, 20);
            if (tripped[0]) {
                peak[0] = Math.max(peak[0], maxSecondaryPressure(h, nodes));
            }
        });
        h.startSequence()
                .thenIdle(100)
                .thenExecute(() -> {
                    h.setBlock(TestVessels.PLANT_SHAFT, Blocks.AIR);
                    tripped[0] = true;
                })
                .thenIdle(2000)
                .thenExecute(() -> {
                    SteamTurbineBlockEntity turbine = h.getBlockEntity(TestVessels.PLANT_TURBINE);
                    h.assertTrue(turbine.status().endsWith("disconnected"), "Turbine should report the lost load");
                    CirculationPumpBlockEntity pump2 = h.getBlockEntity(TestVessels.PUMP2);
                    h.assertTrue(removed[0] > 0, "Condenser should be carrying the heat away; pump2 " + pump2.status() + " flow " + pump2.flow());
                    h.assertTrue(peak[0] < CMThermal.RELIEF_PRESSURE, "After the trip, condenser margin must keep pressure below relief: peak " + peak[0]);
                    CriticalMass.LOGGER.info("turbine trip with condenser margin: peak {} bar, condenser removed {} HU", peak[0], removed[0]);
                })
                .thenSucceed();
    }
}
