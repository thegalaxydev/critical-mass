package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.control.PlantAlarms;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.HeatExchangerBlockEntity;
import com.criticalmass.content.thermal.PipeCoolant;
import com.criticalmass.content.thermal.TankCoolant;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class FeedwaterTests {
    private static final String EMPTY = "empty";
    private static final BlockPos PUMP = new BlockPos(2, 2, 1);
    private static final BlockPos TANK = new BlockPos(4, 2, 2);
    private static final List<BlockPos> RING = List.of(new BlockPos(3, 2, 1), new BlockPos(4, 2, 1), new BlockPos(4, 2, 2), new BlockPos(4, 2, 3), new BlockPos(3, 2, 3),
            new BlockPos(2, 2, 3), new BlockPos(1, 2, 3), new BlockPos(1, 2, 2), new BlockPos(1, 2, 1));
    private static final BlockPos HEATER = new BlockPos(3, 4, 3);
    private static final BlockPos HEATER_PUMP = new BlockPos(3, 2, 4);
    private static final List<BlockPos> FEED_SIDE = List.of(new BlockPos(3, 2, 3), new BlockPos(3, 3, 3));
    private static final List<BlockPos> BETWEEN = List.of(new BlockPos(3, 5, 3), new BlockPos(3, 6, 3), new BlockPos(3, 6, 2), new BlockPos(3, 6, 1), new BlockPos(3, 5, 1),
            new BlockPos(3, 4, 1), new BlockPos(3, 4, 2));
    private static final List<BlockPos> RETURN_SIDE = List.of(new BlockPos(3, 4, 4), new BlockPos(3, 4, 5), new BlockPos(3, 3, 5), new BlockPos(3, 2, 5));

    private FeedwaterTests() {
    }

    private static CirculationPumpBlockEntity pump(GameTestHelper h, BlockPos pos, Direction facing, boolean alongFirst) {
        h.setBlock(pos, CMBlocks.CIRCULATION_PUMP.get().defaultBlockState().setValue(DirectionalKineticBlock.FACING, facing).setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, alongFirst));
        h.setBlock(pos.below(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        return h.getBlockEntity(pos);
    }

    private static void fill(GameTestHelper h, List<BlockPos> pipes, double energy) {
        for (BlockPos p : pipes) {
            PipeCoolant.at(h.getLevel(), h.absolutePos(p)).coolant().set(new FluidStack(Fluids.WATER, CMThermal.PIPE_CAPACITY), CMThermal.PIPE_CAPACITY * energy);
        }
    }

    private static double boiled(double share) {
        return CMThermal.SATURATION_ENERGY + CMThermal.LATENT_HEAT * share;
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void aPumpFedBoilingWaterCavitatesAndLosesFlow(GameTestHelper h) {
        CirculationPumpBlockEntity pump = pump(h, PUMP, Direction.EAST, true);
        TestVessels.pipes(h, RING);
        fill(h, RING, 40);
        int[] full = new int[1];
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    full[0] = pump.flow();
                    h.assertTrue(full[0] > 0 && pump.status().endsWith("running") && !pump.cavitating() && !PlantAlarms.trouble(pump), "A pump fed water should run at full flow: " + pump.status() + " " + full[0]);
                    fill(h, RING, boiled(0.5));
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(pump.cavitating() && pump.status().equals(CirculationPumpBlockEntity.CAVITATING) && PlantAlarms.trouble(pump), "A pump fed boiling water should cavitate: " + pump.status());
                    h.assertTrue(Math.abs(pump.flow() - full[0] * 0.5) <= 2, "Half steam at the intake should halve the flow: " + pump.flow() + " of " + full[0]);
                    fill(h, RING, boiled(1));
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(pump.cavitating() && Math.abs(pump.flow() - full[0] * CMThermal.CAVITATION_FLOOR) <= 1, "All steam should leave a tenth of the flow: " + pump.flow());
                    h.assertTrue(TestVessels.pipeVolume(h, RING) == RING.size() * CMThermal.PIPE_CAPACITY, "Cavitation should lose no water");
                    fill(h, RING, 40);
                })
                .thenIdle(20)
                .thenExecute(() -> h.assertTrue(!pump.cavitating() && pump.flow() == full[0], "With water at the intake again the flow should come back: " + pump.flow()))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void aLoopCrossingOneHeatExchangerOnBothSidesHeatsItsOwnFeedwater(GameTestHelper h) {
        h.setBlock(HEATER, CMBlocks.HEAT_EXCHANGER.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        CirculationPumpBlockEntity pump = pump(h, HEATER_PUMP, Direction.NORTH, false);
        List<BlockPos> pipes = new ArrayList<>(FEED_SIDE);
        pipes.addAll(BETWEEN);
        pipes.addAll(RETURN_SIDE);
        TestVessels.pipes(h, pipes);
        HeatExchangerBlockEntity heater = h.getBlockEntity(HEATER);
        fill(h, FEED_SIDE, 20);
        fill(h, RETURN_SIDE, 20);
        heater.secondary().set(new FluidStack(Fluids.WATER, 500), 500 * 20.0);
        heater.primary().set(new FluidStack(Fluids.WATER, 500), 500 * boiled(0.5));
        fill(h, BETWEEN, boiled(0.5));
        double before = heater.secondary().heat() + heater.primary().heat() + 100 * (FEED_SIDE.size() + RETURN_SIDE.size()) * 20.0 + 100 * BETWEEN.size() * boiled(0.5);
        h.startSequence()
                .thenIdle(30)
                .thenExecute(() -> {
                    h.assertTrue(pump.flow() > 0, "One loop should be allowed through both sides of a Heat Exchanger: " + pump.status());
                    h.assertTrue(heater.exchanged() > 1000, "The steam on its way to the condenser should heat the feedwater: " + heater.exchanged());
                    double after = heater.secondary().heat() + heater.primary().heat();
                    int water = heater.secondary().amount() + heater.primary().amount() + TestVessels.pipeVolume(h, pipes);
                    for (BlockPos p : pipes) {
                        after += PipeCoolant.at(h.getLevel(), h.absolutePos(p)).coolant().heat();
                    }
                    h.assertTrue(water == 1000 + 100 * pipes.size(), "The loop should keep its water: " + water);
                    h.assertTrue(Math.abs(after - before) < 1e-3 * before, "The heater should move heat and lose none: " + after + " from " + before);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void aThresholdSwitchReadsAFeedTankThatIsPartOfALoop(GameTestHelper h) {
        CirculationPumpBlockEntity pump = pump(h, PUMP, Direction.EAST, true);
        h.setBlock(TANK, AllBlocks.FLUID_TANK.getDefaultState());
        List<BlockPos> pipes = new ArrayList<>(RING);
        pipes.remove(TANK);
        TestVessels.pipes(h, pipes);
        fill(h, pipes, 40);
        BlockPos reader = TANK.east();
        h.setBlock(reader, AllBlocks.THRESHOLD_SWITCH.getDefaultState().setValue(HorizontalDirectionalBlock.FACING, Direction.WEST).setValue(DirectedDirectionalBlock.TARGET, AttachFace.WALL));
        FluidTankBlockEntity tank = h.getBlockEntity(TANK);
        tank.getTankInventory().setFluid(new FluidStack(Fluids.WATER, 7000));
        ThresholdSwitchBlockEntity gauge = h.getBlockEntity(reader);
        h.startSequence()
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(pump.flow() > 0, "The loop should run through the tank: " + pump.status());
                    gauge.updateCurrentLevel();
                    int held = TankCoolant.at(h.getLevel(), h.absolutePos(TANK)).coolant().amount();
                    h.assertTrue(gauge.getMaxLevel() > 0 && Math.abs(gauge.getStockLevel() - held) <= 64 && gauge.getStockLevel() > gauge.getMaxLevel() * 0.8, "A Threshold Switch should read a nearly full feed tank: " + gauge.getStockLevel() + " of " + gauge.getMaxLevel() + ", tank " + held);
                    tank.getTankInventory().setFluid(new FluidStack(Fluids.WATER, 1000));
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    gauge.updateCurrentLevel();
                    h.assertTrue(pump.flow() > 0 && gauge.getStockLevel() < gauge.getMaxLevel() * 0.2, "It should read the tank run low while the loop still runs: " + gauge.getStockLevel() + " of " + gauge.getMaxLevel());
                })
                .thenSucceed();
    }
}
