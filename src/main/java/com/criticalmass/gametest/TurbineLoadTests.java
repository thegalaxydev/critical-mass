package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class TurbineLoadTests {
    private static final String EMPTY = "empty_large";
    private static final BlockPos TURBINE = new BlockPos(1, 1, 2);
    private static final BlockPos SHAFT = TURBINE.east(2);
    private static final BlockPos CLUTCH = SHAFT.east();

    private TurbineLoadTests() {
    }

    private static double pressLoad() {
        return BlockStressValues.getImpact(AllBlocks.MECHANICAL_PRESS.get()) * CMThermal.TURBINE_RPM;
    }

    private static double capacity(double steam) {
        return steam * CMThermal.TURBINE_EXTRACTION * CMThermal.SU_PER_HEAT;
    }

    private static SteamTurbineBlockEntity turbine(GameTestHelper h, double steam) {
        h.setBlock(TURBINE, CMBlocks.STEAM_TURBINE.get().defaultBlockState().setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, Direction.Axis.X));
        SteamTurbineBlockEntity turbine = h.getBlockEntity(TURBINE);
        h.onEachTick(() -> turbine.coolant().set(new FluidStack(Fluids.WATER, CMThermal.TURBINE_CAPACITY), CMThermal.TURBINE_CAPACITY * CMThermal.SATURATION_ENERGY + steam));
        return turbine;
    }

    private static void clutch(GameTestHelper h) {
        h.setBlock(SHAFT, AllBlocks.SHAFT.getDefaultState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        h.setBlock(CLUTCH, AllBlocks.CLUTCH.getDefaultState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
    }

    private static void presses(GameTestHelper h, BlockPos first, int count) {
        for (int i = 0; i < count; i++) {
            h.setBlock(first.east(i), AllBlocks.MECHANICAL_PRESS.getDefaultState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
        }
    }

    @GameTest(template = EMPTY, timeoutTicks = 500)
    public static void turbineGovernorFollowsShaftLoad(GameTestHelper h) {
        double capacity = capacity(10000);
        SteamTurbineBlockEntity turbine = turbine(h, 10000);
        clutch(h);
        h.setBlock(CLUTCH.above(), Blocks.REDSTONE_BLOCK);
        presses(h, CLUTCH.east(), 2);
        double[] idle = new double[1];
        h.startSequence()
                .thenIdle(150)
                .thenExecute(() -> {
                    idle[0] = turbine.heatRate();
                    double windage = Math.max(CMThermal.TURBINE_WINDAGE_SU, capacity * CMThermal.TURBINE_WINDAGE_SHARE);
                    h.assertTrue(Math.abs(turbine.stressCapacity() - capacity) / capacity < 0.03, "Capacity is what the steam could deliver: " + turbine.stressCapacity());
                    h.assertTrue(Math.abs(idle[0] - windage / CMThermal.SU_PER_HEAT) < 1.0, "An unloaded turbine should only cover its own losses: " + idle[0]);
                    h.setBlock(CLUTCH.above(), Blocks.AIR);
                })
                .thenIdle(200)
                .thenExecute(() -> {
                    double load = 2 * pressLoad();
                    double windage = Math.max(CMThermal.TURBINE_WINDAGE_SU, capacity * CMThermal.TURBINE_WINDAGE_SHARE);
                    double expected = (load + windage) / CMThermal.SU_PER_HEAT;
                    h.assertTrue(Math.abs(turbine.shaftLoad() - load) / load < 0.05, "Turbine should see the load of the machines on its shaft: " + turbine.shaftLoad() + " vs " + load);
                    h.assertTrue(Math.abs(turbine.heatRate() - expected) / expected < 0.05, "Steam use should follow the load: " + turbine.heatRate() + " vs " + expected);
                    h.assertTrue(turbine.heatRate() > idle[0] * 5, "Loaded turbine should use far more steam than idle");
                    h.assertTrue(turbine.getSpeed() != 0, "The loaded shaft should keep turning");
                    h.assertTrue(!turbine.overspeedTripped() && Math.abs(turbine.overspeed()) < 0.05, "Steady load should hold rated speed: " + turbine.overspeed());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void losingHalfTheLoadDoesNotTrip(GameTestHelper h) {
        SteamTurbineBlockEntity turbine = turbine(h, 7000);
        presses(h, SHAFT, 4);
        double[] full = new double[1];
        h.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    full[0] = turbine.shaftLoad();
                    h.assertTrue(full[0] > capacity(7000) * 0.85, "Four presses should take most of what the turbine can give: " + full[0]);
                    h.setBlock(SHAFT.east(2), Blocks.AIR);
                    h.setBlock(SHAFT.east(3), Blocks.AIR);
                })
                .thenIdle(120)
                .thenExecute(() -> {
                    h.assertTrue(turbine.shaftLoad() < full[0] * 0.6, "Load should have halved: " + turbine.shaftLoad());
                    h.assertTrue(!turbine.overspeedTripped(), "Dropping half the load should be ridden out by the governor: " + turbine.overspeed());
                    h.assertTrue(turbine.admission() < 0.7, "Governor should have closed in: " + turbine.admission());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void suddenLoadLossTripsTheTurbine(GameTestHelper h) {
        double capacity = capacity(7000);
        SteamTurbineBlockEntity turbine = turbine(h, 7000);
        clutch(h);
        presses(h, CLUTCH.east(), 4);
        h.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    h.assertTrue(turbine.shaftLoad() > capacity * 0.85, "The presses should take nearly all the turbine can give: " + turbine.shaftLoad() + " of " + capacity);
                    h.assertTrue(!turbine.overspeedTripped(), "Turbine should carry a steady full load");
                    h.setBlock(CLUTCH.above(), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(turbine.overspeedTripped() && turbine.admission() == 0, "A clutch dropping the whole load at once should trip on overspeed: " + turbine.overspeed());
                    h.assertTrue(turbine.status().endsWith("overspeed_trip"), "Status should name the trip: " + turbine.status());
                    h.setBlock(TURBINE.north(2), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    h.assertTrue(!turbine.overspeedTripped(), "Closing the governor should reset the trip");
                    h.setBlock(TURBINE.north(2), Blocks.AIR);
                    h.setBlock(CLUTCH.above(), Blocks.AIR);
                })
                .thenIdle(200)
                .thenExecute(() -> {
                    h.assertTrue(!turbine.overspeedTripped() && turbine.shaftLoad() > capacity * 0.85, "Turbine should pick the load back up after the reset: " + turbine.shaftLoad());
                })
                .thenSucceed();
    }
}
