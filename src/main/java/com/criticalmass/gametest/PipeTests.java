package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.EmergencyTankBlockEntity;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class PipeTests {
    private static final String EMPTY = "empty";
    private static final BlockPos CREATE_PIPE = new BlockPos(6, 6, 3);

    private PipeTests() {
    }

    private static void createPipe(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, AllBlocks.FLUID_PIPE.getDefaultState());
        FluidPipeBlock block = (FluidPipeBlock) AllBlocks.FLUID_PIPE.get();
        h.setBlock(pos, block.updateBlockState(h.getBlockState(pos), Direction.NORTH, null, h.getLevel(), h.absolutePos(pos)));
    }

    private static double heatAt(double celsius) {
        return (celsius - CMThermal.AMBIENT) * CMThermal.HEAT_CAPACITY;
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void loopPipesHoldCoolantAndColdCoolantSparesCreatePipes(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        TestVessels.loop(h, true);
        createPipe(h, CREATE_PIPE);
        ReactorControllerBlockEntity controller = h.getBlockEntity(TestVessels.CONTROLLER);
        controller.coolant().set(CoolantLoopTests.coolant(6000), 6000 * heatAt(40));
        h.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(TestVessels.PUMP);
                    SteamTurbineBlockEntity turbine = h.getBlockEntity(TestVessels.TURBINE);
                    h.assertTrue(pump.flow() > 0, "Pump should circulate through a Create pipe: " + pump.status());
                    h.assertBlockPresent(AllBlocks.FLUID_PIPE.get(), CREATE_PIPE);
                    int inPipes = TestVessels.pipeVolume(h, TestVessels.PIPES);
                    h.assertTrue(inPipes > 0, "Loop pipes should hold coolant");
                    int total = controller.coolant().amount() + turbine.coolant().amount() + inPipes;
                    h.assertTrue(total == 6000, "Coolant volume not conserved with pipes: " + total);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void hotCoolantBurstsCreatePipesInTheLoop(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        TestVessels.loop(h, true);
        createPipe(h, CREATE_PIPE);
        ReactorControllerBlockEntity controller = h.getBlockEntity(TestVessels.CONTROLLER);
        controller.coolant().set(CoolantLoopTests.coolant(6000), 6000 * heatAt(160));
        h.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(TestVessels.PUMP);
                    h.assertBlockNotPresent(AllBlocks.FLUID_PIPE.get(), CREATE_PIPE);
                    h.assertTrue(pump.flow() == 0, "The burst pipe should break the loop: " + pump.status());
                    for (BlockPos p : TestVessels.PIPES) {
                        if (!p.equals(CREATE_PIPE)) {
                            h.assertBlockPresent(CMBlocks.REINFORCED_PIPE.get(), p);
                        }
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void drainingHotCoolantBurstsTheNearestCreatePipe(GameTestHelper h) {
        BlockPos hot = new BlockPos(1, 1, 1);
        BlockPos cold = new BlockPos(1, 1, 4);
        List<BlockPos> hotRun = List.of(hot.east(), hot.east(2), hot.east(3));
        List<BlockPos> coldRun = List.of(cold.east(), cold.east(2));
        h.setBlock(hot, CMBlocks.EMERGENCY_COOLANT_TANK.get());
        h.setBlock(cold, CMBlocks.EMERGENCY_COOLANT_TANK.get());
        h.setBlock(hotRun.get(0), CMBlocks.REINFORCED_PIPE.get());
        TestVessels.pipes(h, List.of(hotRun.get(0)));
        createPipe(h, hotRun.get(1));
        createPipe(h, hotRun.get(2));
        createPipe(h, coldRun.get(0));
        createPipe(h, coldRun.get(1));
        EmergencyTankBlockEntity hotTank = h.getBlockEntity(hot);
        EmergencyTankBlockEntity coldTank = h.getBlockEntity(cold);
        hotTank.coolant().set(CoolantLoopTests.coolant(1000), 1000 * heatAt(200));
        coldTank.coolant().set(CoolantLoopTests.coolant(1000), 1000 * heatAt(30));
        IFluidHandler hotHandler = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(hot), Direction.EAST);
        IFluidHandler coldHandler = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(cold), Direction.EAST);
        h.assertTrue(hotHandler != null && coldHandler != null, "Tanks should offer coolant on their sides");
        hotHandler.drain(100, IFluidHandler.FluidAction.EXECUTE);
        coldHandler.drain(100, IFluidHandler.FluidAction.EXECUTE);
        h.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    h.assertBlockPresent(CMBlocks.REINFORCED_PIPE.get(), hotRun.get(0));
                    h.assertBlockNotPresent(AllBlocks.FLUID_PIPE.get(), hotRun.get(1));
                    h.assertBlockPresent(AllBlocks.FLUID_PIPE.get(), hotRun.get(2));
                    h.assertBlockPresent(AllBlocks.FLUID_PIPE.get(), coldRun.get(0));
                    h.assertBlockPresent(AllBlocks.FLUID_PIPE.get(), coldRun.get(1));
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void coolantKeepsItsHeatThroughCreateTransfers(GameTestHelper h) {
        BlockPos from = new BlockPos(1, 1, 1);
        BlockPos to = new BlockPos(4, 1, 1);
        h.setBlock(from, CMBlocks.EMERGENCY_COOLANT_TANK.get());
        h.setBlock(to, CMBlocks.EMERGENCY_COOLANT_TANK.get());
        EmergencyTankBlockEntity a = h.getBlockEntity(from);
        EmergencyTankBlockEntity b = h.getBlockEntity(to);
        double perMb = heatAt(80);
        a.coolant().set(CoolantLoopTests.coolant(1000), 1000 * perMb);
        IFluidHandler source = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(from), Direction.UP);
        IFluidHandler target = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(to), Direction.UP);
        int filled = target.fill(source.drain(250, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(filled == 250, "Transfer should move 250 mB: " + filled);
        h.assertTrue(Math.abs(b.coolant().heat() - 250 * perMb) < 1e-6, "Heat should travel with the coolant: " + b.coolant().heat());
        h.assertTrue(Math.abs(a.coolant().heat() + b.coolant().heat() - 1000 * perMb) < 1e-6, "Heat should be conserved");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void reinforcedPipesJoinCreatePipes(GameTestHelper h) {
        BlockPos a = new BlockPos(2, 1, 2);
        BlockPos b = a.east();
        h.setBlock(a, CMBlocks.REINFORCED_PIPE.get());
        createPipe(h, b);
        TestVessels.pipes(h, List.of(a));
        BlockState reinforced = h.getBlockState(a);
        BlockState create = h.getBlockState(b);
        h.assertTrue(FluidPipeBlock.isOpenAt(reinforced, Direction.EAST) && FluidPipeBlock.isOpenAt(create, Direction.WEST), "Reinforced and Create pipes should connect");
        h.succeed();
    }
}
