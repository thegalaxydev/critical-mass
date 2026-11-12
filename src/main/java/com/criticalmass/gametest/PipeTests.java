package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.fuel.SpentFuelRackBlockEntity;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class PipeTests {
    private static final String EMPTY = "empty";

    private PipeTests() {
    }

    private static double heatAt(double celsius) {
        return (celsius - CMThermal.AMBIENT) * CMThermal.HEAT_CAPACITY;
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void loopPipesHoldCoolant(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        TestVessels.loop(h, true);
        ReactorControllerBlockEntity controller = h.getBlockEntity(TestVessels.CONTROLLER);
        controller.coolant().set(CoolantLoopTests.coolant(6000), 6000 * heatAt(40));
        h.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(TestVessels.PUMP);
                    SteamTurbineBlockEntity turbine = h.getBlockEntity(TestVessels.TURBINE);
                    h.assertTrue(pump.flow() > 0, "Pump should circulate through Create pipes: " + pump.status());
                    int inPipes = TestVessels.pipeVolume(h, TestVessels.PIPES);
                    h.assertTrue(inPipes > 0, "Loop pipes should hold coolant");
                    int total = controller.coolant().amount() + turbine.coolant().amount() + inPipes;
                    h.assertTrue(total == 6000, "Coolant volume not conserved with pipes: " + total);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void hotCoolantRunsThroughCreatePipes(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        TestVessels.loop(h, true);
        ReactorControllerBlockEntity controller = h.getBlockEntity(TestVessels.CONTROLLER);
        controller.coolant().set(CoolantLoopTests.coolant(6000), 6000 * heatAt(160));
        h.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(TestVessels.PUMP);
                    for (BlockPos p : TestVessels.PIPES) {
                        h.assertBlockPresent(AllBlocks.FLUID_PIPE.get(), p);
                    }
                    h.assertTrue(pump.flow() > 0, "Hot coolant should keep circulating: " + pump.status());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void drainingHotCoolantSparesCreatePipes(GameTestHelper h) {
        BlockPos hot = new BlockPos(1, 1, 1);
        List<BlockPos> run = List.of(hot.east(), hot.east(2), hot.east(3));
        h.setBlock(hot, CMBlocks.SPENT_FUEL_RACK.get());
        TestVessels.pipes(h, run);
        SpentFuelRackBlockEntity tank = h.getBlockEntity(hot);
        tank.coolant().set(CoolantLoopTests.coolant(1000), 1000 * heatAt(200));
        IFluidHandler handler = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(hot), Direction.EAST);
        h.assertTrue(handler != null, "The rack should offer its water on its side");
        handler.drain(100, IFluidHandler.FluidAction.EXECUTE);
        h.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    for (BlockPos p : run) {
                        h.assertBlockPresent(AllBlocks.FLUID_PIPE.get(), p);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void coolantKeepsItsHeatThroughCreateTransfers(GameTestHelper h) {
        BlockPos from = new BlockPos(1, 1, 1);
        BlockPos to = new BlockPos(4, 1, 1);
        h.setBlock(from, CMBlocks.SPENT_FUEL_RACK.get());
        h.setBlock(to, CMBlocks.SPENT_FUEL_RACK.get());
        SpentFuelRackBlockEntity a = h.getBlockEntity(from);
        SpentFuelRackBlockEntity b = h.getBlockEntity(to);
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
    public static void oldPipeAndValveIdsBecomeCreateParts(GameTestHelper h) {
        h.assertTrue(BuiltInRegistries.BLOCK.get(CriticalMass.asResource("reinforced_pipe")) == AllBlocks.FLUID_PIPE.get(), "Old pipe blocks should load as Create pipes");
        h.assertTrue(BuiltInRegistries.ITEM.get(CriticalMass.asResource("reinforced_pipe")) == AllBlocks.FLUID_PIPE.asItem(), "Old pipe items should load as Create pipes");
        h.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.get(CriticalMass.asResource("reinforced_pipe")) == AllBlockEntityTypes.FLUID_PIPE.get(), "Old pipe data should load as Create pipes");
        for (String valve : List.of("reinforced_valve", "steam_bypass_valve")) {
            h.assertTrue(BuiltInRegistries.BLOCK.get(CriticalMass.asResource(valve)) == AllBlocks.FLUID_VALVE.get(), "Old valve blocks should load as Create valves: " + valve);
            h.assertTrue(BuiltInRegistries.ITEM.get(CriticalMass.asResource(valve)) == AllBlocks.FLUID_VALVE.asItem(), "Old valve items should load as Create valves: " + valve);
            h.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.get(CriticalMass.asResource(valve)) == AllBlockEntityTypes.FLUID_VALVE.get(), "Old valve data should load as Create valves: " + valve);
        }
        for (String tank : List.of("tower_basin", "feedwater_tank", "emergency_coolant_tank")) {
            h.assertTrue(BuiltInRegistries.BLOCK.get(CriticalMass.asResource(tank)) == AllBlocks.FLUID_TANK.get(), "Old tanks should load as Create Fluid Tanks: " + tank);
            h.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.get(CriticalMass.asResource(tank)) == AllBlockEntityTypes.FLUID_TANK.get(), "Old tank data should load as Create Fluid Tanks: " + tank);
        }
        h.assertTrue(BuiltInRegistries.BLOCK.get(CriticalMass.asResource("tower_distributor")) == AllBlocks.FLUID_PIPE.get(), "Old distributors should load as Create pipes");
        h.succeed();
    }
}
