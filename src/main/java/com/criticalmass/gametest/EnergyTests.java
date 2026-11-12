package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.kinetics.GeneratorBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMConfig;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class EnergyTests {
    private static final String EMPTY = "empty";
    private static final double STEAM = 40000;

    private EnergyTests() {
    }

    private static double fePerSu() {
        return CMConfig.get(CMConfig.FE_PER_SU) * CMConfig.get(CMConfig.GENERATOR_EFFICIENCY);
    }

    private static GeneratorBlockEntity generator(GameTestHelper h, BlockPos pos, Direction facing) {
        h.setBlock(pos, CMBlocks.GENERATOR.get().defaultBlockState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, facing));
        return h.getBlockEntity(pos);
    }

    private static SteamTurbineBlockEntity turbineSet(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, CMBlocks.STEAM_TURBINE.get().defaultBlockState().setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, Direction.Axis.X));
        SteamTurbineBlockEntity turbine = h.getBlockEntity(pos);
        h.onEachTick(() -> turbine.coolant().set(new FluidStack(Fluids.WATER, CMThermal.TURBINE_CAPACITY), CMThermal.TURBINE_CAPACITY * CMThermal.SATURATION_ENERGY + STEAM));
        return turbine;
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void generatorTurnsShaftPowerIntoForgeEnergy(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        GeneratorBlockEntity generator = generator(h, pos, Direction.NORTH);
        generator.setSetting(512);
        h.setBlock(pos.south(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.NORTH));
        IEnergyStorage side = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(pos), Direction.NORTH);
        IEnergyStorage shaft = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(pos), Direction.SOUTH);
        h.assertTrue(side != null && shaft == null, "Energy should be offered on every face except the shaft");
        int[] drawn = new int[1];
        h.onEachTick(() -> drawn[0] += side.extractEnergy(512, false));
        h.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    double expectedPower = 512 / fePerSu();
                    h.assertTrue(Math.abs(generator.outputRate() - 512) < 8, "Output should settle at the set load: " + generator.outputRate());
                    h.assertTrue(Math.abs(generator.shaftPower() - expectedPower) / expectedPower < 0.03, "Shaft load " + generator.shaftPower() + " expected " + expectedPower);
                    h.assertTrue(Math.abs(generator.generated() - generator.shaftWork() * fePerSu()) <= 2, "Energy made should match shaft work: " + generator.generated() + " vs " + generator.shaftWork() * fePerSu());
                    h.assertTrue(drawn[0] + generator.stored() == Math.round(generator.generated()), "Energy drawn plus stored should equal energy made");
                    h.setBlock(pos.above(), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(100)
                .thenExecute(() -> {
                    h.assertTrue(generator.breakerOpen() && generator.shaftPower() == 0 && generator.outputRate() < 1, "A full redstone signal should open the breaker: " + generator.shaftPower());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void fullBufferShedsTheLoad(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        GeneratorBlockEntity generator = generator(h, pos, Direction.NORTH);
        generator.setSetting(512);
        h.setBlock(pos.south(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.NORTH));
        h.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    h.assertTrue(generator.stored() >= generator.capacity() * 0.85 && generator.stored() <= generator.capacity(), "Buffer should fill and stop: " + generator.stored());
                    h.assertTrue(generator.shaftPower() < 16, "A full buffer should take almost no shaft power: " + generator.shaftPower());
                    h.assertTrue(generator.stored() == Math.round(generator.generated()), "Nothing should be lost with no consumer");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 500)
    public static void turbineGovernorFollowsShaftLoad(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        SteamTurbineBlockEntity turbine = turbineSet(h, pos);
        GeneratorBlockEntity generator = generator(h, pos.east(), Direction.EAST);
        generator.setSetting(0);
        IEnergyStorage out = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(pos.east()), Direction.UP);
        h.onEachTick(() -> out.extractEnergy(100000, false));
        double capacity = STEAM * CMThermal.TURBINE_EXTRACTION * CMThermal.SU_PER_HEAT;
        double[] idle = new double[1];
        h.startSequence()
                .thenIdle(150)
                .thenExecute(() -> {
                    idle[0] = turbine.heatRate();
                    double windage = Math.max(CMThermal.TURBINE_WINDAGE_SU, capacity * CMThermal.TURBINE_WINDAGE_SHARE);
                    h.assertTrue(Math.abs(turbine.stressCapacity() - capacity) / capacity < 0.03, "Capacity is what the steam could deliver: " + turbine.stressCapacity());
                    h.assertTrue(Math.abs(idle[0] - windage / CMThermal.SU_PER_HEAT) < 1.0, "An unloaded turbine should only cover its own losses: " + idle[0]);
                    generator.setSetting(240);
                })
                .thenIdle(200)
                .thenExecute(() -> {
                    double load = 240 / fePerSu();
                    double windage = Math.max(CMThermal.TURBINE_WINDAGE_SU, capacity * CMThermal.TURBINE_WINDAGE_SHARE);
                    double expected = (load + windage) / CMThermal.SU_PER_HEAT;
                    h.assertTrue(Math.abs(turbine.shaftLoad() - load) / load < 0.05, "Turbine should see the generator's load: " + turbine.shaftLoad() + " vs " + load);
                    h.assertTrue(Math.abs(turbine.heatRate() - expected) / expected < 0.05, "Steam use should follow the load: " + turbine.heatRate() + " vs " + expected);
                    h.assertTrue(turbine.heatRate() > idle[0] * 5, "Loaded turbine should use far more steam than idle");
                    h.assertTrue(Math.abs(generator.outputRate() - 240) < 8, "Generator output: " + generator.outputRate());
                    h.assertTrue(!turbine.overspeedTripped() && Math.abs(turbine.overspeed()) < 0.05, "Steady load should hold rated speed: " + turbine.overspeed());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void losingHalfTheLoadDoesNotTrip(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        SteamTurbineBlockEntity turbine = turbineSet(h, pos);
        GeneratorBlockEntity generator = generator(h, pos.east(), Direction.EAST);
        generator.setSetting(2048);
        IEnergyStorage out = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(pos.east()), Direction.UP);
        h.onEachTick(() -> out.extractEnergy(100000, false));
        double[] full = new double[1];
        h.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    full[0] = generator.shaftPower();
                    generator.setSetting((int) (full[0] * fePerSu() / 2));
                })
                .thenIdle(120)
                .thenExecute(() -> {
                    h.assertTrue(generator.shaftPower() < full[0] * 0.6, "Load should have halved: " + generator.shaftPower());
                    h.assertTrue(!turbine.overspeedTripped(), "Dropping half the load should be ridden out by the governor: " + turbine.overspeed());
                    h.assertTrue(turbine.admission() < 0.7, "Governor should have closed in: " + turbine.admission());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void suddenLoadLossTripsTheTurbine(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        SteamTurbineBlockEntity turbine = turbineSet(h, pos);
        GeneratorBlockEntity generator = generator(h, pos.east(), Direction.EAST);
        generator.setSetting(2048);
        IEnergyStorage out = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(pos.east()), Direction.UP);
        h.onEachTick(() -> out.extractEnergy(100000, false));
        double capacity = STEAM * CMThermal.TURBINE_EXTRACTION * CMThermal.SU_PER_HEAT;
        h.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    h.assertTrue(generator.shaftPower() > capacity * 0.9, "Generator should take nearly all the turbine can give: " + generator.shaftPower() + " of " + capacity);
                    h.assertTrue(!turbine.overspeedTripped(), "Turbine should carry a steady full load");
                    h.setBlock(pos.east().above(), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(turbine.overspeedTripped() && turbine.admission() == 0, "Losing the whole load at once should trip on overspeed: " + turbine.overspeed());
                    h.assertTrue(turbine.status().endsWith("overspeed_trip"), "Status should name the trip: " + turbine.status());
                    h.setBlock(pos.north(), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    h.assertTrue(!turbine.overspeedTripped(), "Closing the governor should reset the trip");
                    h.setBlock(pos.north(), Blocks.AIR);
                    h.setBlock(pos.east().above(), Blocks.AIR);
                })
                .thenIdle(200)
                .thenExecute(() -> {
                    h.assertTrue(!turbine.overspeedTripped() && generator.shaftPower() > capacity * 0.9, "Turbine should pick the load back up after the reset: " + generator.shaftPower());
                })
                .thenSucceed();
    }
}
