package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.fuel.SpentFuelRackBlockEntity;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.ReinforcedValveBlock;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.CommonHooks;
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

    private static double heatAt(double celsius) {
        return (celsius - CMThermal.AMBIENT) * CMThermal.HEAT_CAPACITY;
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void loopPipesHoldCoolantAndColdCoolantSparesCreatePipes(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        TestVessels.loop(h, true);
        TestVessels.createPipes(h, List.of(CREATE_PIPE));
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
        TestVessels.createPipes(h, List.of(CREATE_PIPE));
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
        h.setBlock(hot, CMBlocks.SPENT_FUEL_RACK.get());
        h.setBlock(cold, CMBlocks.SPENT_FUEL_RACK.get());
        TestVessels.pipes(h, List.of(hotRun.get(0)));
        TestVessels.createPipes(h, List.of(hotRun.get(1), hotRun.get(2), coldRun.get(0), coldRun.get(1)));
        TestVessels.connect(h, hotRun.get(0));
        SpentFuelRackBlockEntity hotTank = h.getBlockEntity(hot);
        SpentFuelRackBlockEntity coldTank = h.getBlockEntity(cold);
        hotTank.coolant().set(CoolantLoopTests.coolant(1000), 1000 * heatAt(200));
        coldTank.coolant().set(CoolantLoopTests.coolant(1000), 1000 * heatAt(30));
        IFluidHandler hotHandler = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(hot), Direction.EAST);
        IFluidHandler coldHandler = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(cold), Direction.EAST);
        h.assertTrue(hotHandler != null && coldHandler != null, "Racks should offer their water on their sides");
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
    public static void reinforcedPipesJoinCreatePipes(GameTestHelper h) {
        BlockPos a = new BlockPos(2, 1, 2);
        BlockPos b = a.east();
        h.setBlock(a, CMBlocks.REINFORCED_PIPE.get());
        TestVessels.createPipes(h, List.of(b));
        TestVessels.connect(h, a);
        BlockState reinforced = h.getBlockState(a);
        BlockState create = h.getBlockState(b);
        h.assertTrue(FluidPipeBlock.isOpenAt(reinforced, Direction.EAST) && FluidPipeBlock.isOpenAt(create, Direction.WEST), "Reinforced and Create pipes should connect");
        h.succeed();
    }

    private static void apply(GameTestHelper h, Player player, BlockPos pos) {
        BlockPos abs = h.absolutePos(pos);
        CommonHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, abs, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void reactorAlloyReinforcesPipesAndValvesInPlace(GameTestHelper h) {
        BlockPos west = new BlockPos(2, 1, 2);
        BlockPos valve = west.east();
        BlockPos east = valve.east();
        h.setBlock(west.below(), Blocks.STONE);
        h.setBlock(valve, AllBlocks.FLUID_VALVE.getDefaultState()
                .setValue(DirectionalKineticBlock.FACING, Direction.UP).setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, false));
        TestVessels.createPipes(h, List.of(west, east));
        BlockState before = h.getBlockState(west);
        h.assertTrue(FluidPipeBlock.isOpenAt(before, Direction.EAST), "The pipe should run into the valve");
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CMItems.REACTOR_ALLOY_INGOT.get(), 3));
        apply(h, player, west);
        apply(h, player, valve);
        BlockState pipe = h.getBlockState(west);
        h.assertTrue(pipe.is(CMBlocks.REINFORCED_PIPE.get()), "Reactor alloy on a Create pipe should make a Reinforced Pipe: " + pipe);
        for (Direction d : Direction.values()) {
            h.assertTrue(FluidPipeBlock.isOpenAt(pipe, d) == FluidPipeBlock.isOpenAt(before, d), "The reinforced pipe should keep its connections: " + d);
        }
        BlockState reinforcedValve = h.getBlockState(valve);
        h.assertTrue(reinforcedValve.is(CMBlocks.REINFORCED_VALVE.get()) && ReinforcedValveBlock.getPipeAxis(reinforcedValve) == Direction.Axis.X,
                "Reactor alloy on a Fluid Valve should make a Reinforced Valve the same way round: " + reinforcedValve);
        h.assertBlockPresent(AllBlocks.FLUID_PIPE.get(), east);
        h.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "Each application should use one ingot: " + player.getItemInHand(InteractionHand.MAIN_HAND));
        apply(h, player, west);
        h.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "A pipe that is already reinforced should take no more alloy");
        for (String id : List.of("item_application/reinforced_pipe", "item_application/reinforced_valve")) {
            h.assertTrue(h.getLevel().getRecipeManager().byKey(CriticalMass.asResource(id)).isPresent(), "Deployers need the recipe: " + id);
        }
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void oldBlockIdsBecomeTheirReplacements(GameTestHelper h) {
        h.assertTrue(BuiltInRegistries.BLOCK.get(CriticalMass.asResource("steam_bypass_valve")) == CMBlocks.REINFORCED_VALVE.get(), "The old bypass valve should load as a Reinforced Valve");
        for (String tank : List.of("tower_basin", "feedwater_tank", "emergency_coolant_tank")) {
            h.assertTrue(BuiltInRegistries.BLOCK.get(CriticalMass.asResource(tank)) == AllBlocks.FLUID_TANK.get(), "Old tanks should load as Create Fluid Tanks: " + tank);
            h.assertTrue(BuiltInRegistries.ITEM.get(CriticalMass.asResource(tank)) == AllBlocks.FLUID_TANK.asItem(), "Old tank items should load as Create Fluid Tanks: " + tank);
            h.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.get(CriticalMass.asResource(tank)) == AllBlockEntityTypes.FLUID_TANK.get(), "Old tank data should load as Create Fluid Tanks: " + tank);
        }
        h.assertTrue(BuiltInRegistries.BLOCK.get(CriticalMass.asResource("tower_distributor")) == AllBlocks.FLUID_PIPE.get(), "Old distributors should load as Create pipes");
        h.succeed();
    }
}
