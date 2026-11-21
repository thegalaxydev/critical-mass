package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.fuel.SpentFuelRackBlockEntity;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.radiation.ContaminationData;
import com.criticalmass.content.radiation.Radiation;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.PipeCoolant;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.ReinforcedValveBlock;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.bracket.BracketBlock;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
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
    private static final String LARGE = "empty_large";
    private static final BlockPos CREATE_PIPE = new BlockPos(8, 5, 3);

    private PipeTests() {
    }

    private static double heatAt(double celsius) {
        return (celsius - CMThermal.AMBIENT) * CMThermal.HEAT_CAPACITY;
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
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

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void hotCoolantRunsThroughPlainPipesWithoutPressure(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        TestVessels.loop(h, true);
        TestVessels.createPipes(h, List.of(CREATE_PIPE));
        ReactorControllerBlockEntity controller = h.getBlockEntity(TestVessels.CONTROLLER);
        controller.coolant().set(CoolantLoopTests.coolant(6000), 6000 * heatAt(160));
        h.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(TestVessels.PUMP);
                    SteamTurbineBlockEntity turbine = h.getBlockEntity(TestVessels.TURBINE);
                    h.assertBlockPresent(AllBlocks.FLUID_PIPE.get(), CREATE_PIPE);
                    h.assertTrue(pump.flow() > 0, "Hot coolant should keep circulating through a plain pipe: " + pump.status());
                    int total = controller.coolant().amount() + turbine.coolant().amount() + TestVessels.pipeVolume(h, TestVessels.PIPES);
                    h.assertTrue(total == 6000, "An unpressurized plain pipe should not leak: " + total);
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void plainPipesLeakUnderPressureAndRadiate(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        TestVessels.loop(h, true);
        TestVessels.createPipes(h, List.of(CREATE_PIPE));
        ReactorControllerBlockEntity controller = h.getBlockEntity(TestVessels.CONTROLLER);
        controller.coolant().set(CoolantLoopTests.coolant(6000), 6000 * heatAt(40));
        h.onEachTick(() -> controller.notePressure(100));
        h.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(TestVessels.PUMP);
                    SteamTurbineBlockEntity turbine = h.getBlockEntity(TestVessels.TURBINE);
                    PipeCoolant plain = PipeCoolant.at(h.getLevel(), h.absolutePos(CREATE_PIPE));
                    long leaked = plain.sinks().ventedVolume;
                    h.assertTrue(pump.flow() > 0, "A leaking pipe should not stop the loop: " + pump.status());
                    h.assertTrue(leaked > 20, "A plain pipe on a loop at 100 bar should leak: " + leaked);
                    int total = controller.coolant().amount() + turbine.coolant().amount() + TestVessels.pipeVolume(h, TestVessels.PIPES);
                    h.assertTrue(total + leaked == 6000, "What is missing from the loop should be what the plain pipe leaked: " + total + " + " + leaked);
                    for (BlockPos p : TestVessels.PIPES) {
                        if (!p.equals(CREATE_PIPE)) {
                            h.assertTrue(PipeCoolant.at(h.getLevel(), h.absolutePos(p)).sinks().ventedVolume == 0, "Reinforced pipes should not leak");
                        }
                    }
                    Vec3 at = h.absolutePos(CREATE_PIPE).getCenter();
                    Vec3 sealed = h.absolutePos(TestVessels.PIPES.get(0)).getCenter();
                    List<Radiation.Source> sources = Radiation.blockSources(h.getLevel());
                    h.assertTrue(sources.stream().anyMatch(src -> src.pos().equals(at) && src.strength() > 0), "A plain pipe holding primary coolant should give off radiation");
                    h.assertTrue(sources.stream().noneMatch(src -> src.pos().equals(sealed)), "A reinforced pipe should not");
                    h.assertTrue(ContaminationData.get(h.getLevel()).get(h.absolutePos(CREATE_PIPE)) > 0, "Leaked primary coolant should contaminate the spot");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void drainingHotCoolantSparesPlainPipes(GameTestHelper h) {
        BlockPos hot = new BlockPos(1, 1, 1);
        List<BlockPos> run = List.of(hot.east(), hot.east(2), hot.east(3));
        h.setBlock(hot, CMBlocks.SPENT_FUEL_RACK.get());
        TestVessels.createPipes(h, run);
        SpentFuelRackBlockEntity rack = h.getBlockEntity(hot);
        rack.coolant().set(CoolantLoopTests.coolant(1000), 1000 * heatAt(200));
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

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void reinforcedPipesTakeBrackets(GameTestHelper h) {
        BlockPos mid = new BlockPos(2, 2, 2);
        List<BlockPos> run = List.of(mid.west(), mid, mid.east());
        for (BlockPos p : run) {
            h.setBlock(p, CMBlocks.REINFORCED_PIPE.get());
        }
        for (BlockPos p : run) {
            TestVessels.connect(h, p);
        }
        BlockPos abs = h.absolutePos(mid);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack brackets = new ItemStack(AllBlocks.METAL_BRACKET.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, brackets);
        UseOnContext click = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.DOWN, abs, false));
        brackets.useOn(click);
        BracketedBlockEntityBehaviour bracket = BlockEntityBehaviour.get(h.getLevel(), abs, BracketedBlockEntityBehaviour.TYPE);
        h.assertTrue(bracket != null && bracket.isBracketPresent(), "A Reinforced Pipe should take a bracket");
        h.assertTrue(bracket.getBracket().is(AllBlocks.METAL_BRACKET.get()) && bracket.getBracket().getValue(BracketBlock.FACING) == Direction.DOWN,
                "The bracket should hang on the face it was put on: " + bracket.getBracket());
        h.assertTrue(brackets.getCount() == 1, "The bracket should use one item: " + brackets);
        BlockState held = h.getBlockState(mid);
        h.assertTrue(FluidPipeBlock.isOpenAt(held, Direction.WEST) && FluidPipeBlock.isOpenAt(held, Direction.EAST), "The bracketed pipe should keep its run: " + held);
        h.setBlock(mid.above(), CMBlocks.REINFORCED_PIPE.get());
        TestVessels.connect(h, mid);
        TestVessels.connect(h, mid.above());
        h.assertTrue(!FluidPipeBlock.isOpenAt(h.getBlockState(mid), Direction.UP) && !FluidPipeBlock.isOpenAt(h.getBlockState(mid.above()), Direction.DOWN),
                "A bracketed pipe should not branch: " + h.getBlockState(mid) + " " + h.getBlockState(mid.above()));
        TestVessels.createPipes(h, List.of(mid.north(), mid.north().east()));
        TestVessels.connect(h, mid);
        h.assertTrue(!FluidPipeBlock.isOpenAt(h.getBlockState(mid), Direction.NORTH) && !FluidPipeBlock.isOpenAt(h.getBlockState(mid.north()), Direction.SOUTH),
                "A Create pipe beside a bracketed pipe should not join it: " + h.getBlockState(mid) + " " + h.getBlockState(mid.north()));
        h.setBlock(mid.north(), Blocks.AIR);
        h.setBlock(mid.north().east(), Blocks.AIR);
        h.assertTrue(CMBlocks.REINFORCED_PIPE.get().onWrenched(h.getBlockState(mid), click).consumesAction(), "A wrench should take the bracket off");
        h.assertTrue(!bracket.isBracketPresent(), "The bracket should be gone after the wrench");
        h.assertTrue(FluidPipeBlock.isOpenAt(h.getBlockState(mid), Direction.UP), "The freed pipe should join the pipe above it: " + h.getBlockState(mid));
        h.assertTrue(player.getInventory().countItem(AllBlocks.METAL_BRACKET.asItem()) == 2, "The wrench should give the bracket back");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void aWrenchLocksAReinforcedPipeStraight(GameTestHelper h) {
        BlockPos mid = new BlockPos(2, 1, 2);
        BlockPos glass = new BlockPos(0, 1, 0);
        List<BlockPos> run = List.of(mid.west(), mid, mid.east(), mid.south(), mid.south().west(), mid.south().east());
        for (BlockPos p : run) {
            h.setBlock(p, CMBlocks.REINFORCED_PIPE.get());
        }
        for (BlockPos p : run) {
            TestVessels.connect(h, p);
        }
        h.setBlock(glass, AllBlocks.GLASS_FLUID_PIPE.getDefaultState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.Z));
        BlockPos abs = h.absolutePos(mid);
        BlockState tee = h.getBlockState(mid);
        h.assertTrue(FluidPipeBlock.isOpenAt(tee, Direction.WEST) && FluidPipeBlock.isOpenAt(tee, Direction.EAST) && FluidPipeBlock.isOpenAt(tee, Direction.SOUTH), "The middle pipe should start as a tee: " + tee);
        PipeCoolant.at(h.getLevel(), abs).coolant().set(CoolantLoopTests.coolant(100), 500);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(com.simibubi.create.AllItems.WRENCH.get()));
        UseOnContext top = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs).add(0.4, 0.5, 0), Direction.UP, abs, false));
        UseOnContext end = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs).add(0.5, 0, 0), Direction.EAST, abs, false));
        h.assertTrue(CMBlocks.REINFORCED_PIPE.get().onWrenched(tee, top).consumesAction(), "A wrench on the side of a Reinforced Pipe should lock it");
        BlockState locked = h.getBlockState(mid);
        h.assertTrue(locked.is(CMBlocks.REINFORCED_STRAIGHT_PIPE.get()) && locked.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS) == Direction.Axis.X,
                "It should lock along the run nearest the click: " + locked);
        h.assertTrue(PipeCoolant.open(h.getLevel(), abs, locked, Direction.EAST) && PipeCoolant.open(h.getLevel(), abs, locked, Direction.WEST) && !PipeCoolant.open(h.getLevel(), abs, locked, Direction.SOUTH),
                "A locked pipe should be open only along its run");
        h.assertTrue(PipeCoolant.isPipe(locked) && PipeCoolant.isReinforced(locked), "A locked pipe should still be a Reinforced Pipe");
        h.assertTrue(PipeCoolant.at(h.getLevel(), abs).coolant().amount() == 100 && PipeCoolant.at(h.getLevel(), abs).coolant().heat() == 500, "Locking a pipe should keep the coolant in it");
        List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(locked, h.getLevel(), abs, h.getLevel().getBlockEntity(abs));
        h.assertTrue(drops.size() == 1 && drops.get(0).is(CMBlocks.REINFORCED_PIPE.get().asItem()), "A locked pipe should drop a Reinforced Pipe: " + drops);
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    BlockState beside = h.getBlockState(mid.south());
                    h.assertTrue(!FluidPipeBlock.isOpenAt(beside, Direction.NORTH) && FluidPipeBlock.isOpenAt(beside, Direction.WEST) && FluidPipeBlock.isOpenAt(beside, Direction.EAST), "A run beside a locked pipe should let go of it: " + beside);
                    h.assertTrue(FluidPipeBlock.isOpenAt(h.getBlockState(mid.east()), Direction.WEST), "The pipes along its run should stay joined: " + h.getBlockState(mid.east()));
                    h.assertTrue(CMBlocks.REINFORCED_STRAIGHT_PIPE.get().onWrenched(h.getBlockState(mid), top).consumesAction(), "A wrench on a locked pipe should free it");
                    BlockState freed = h.getBlockState(mid);
                    h.assertTrue(freed.is(CMBlocks.REINFORCED_PIPE.get()) && FluidPipeBlock.isOpenAt(freed, Direction.WEST) && FluidPipeBlock.isOpenAt(freed, Direction.EAST) && FluidPipeBlock.isOpenAt(freed, Direction.SOUTH),
                            "A freed pipe should join everything round it again: " + freed);
                    h.assertTrue(PipeCoolant.at(h.getLevel(), abs).coolant().amount() == 100, "Freeing a pipe should keep the coolant in it");
                    h.setBlock(mid.south(), Blocks.AIR);
                    TestVessels.connect(h, mid);
                    BlockState straight = h.getBlockState(mid);
                    h.assertTrue(!CMBlocks.REINFORCED_PIPE.get().onWrenched(straight, end).consumesAction() && h.getBlockState(mid).is(CMBlocks.REINFORCED_PIPE.get()), "A wrench on the end of a run should do nothing");
                    h.assertTrue(CMBlocks.REINFORCED_PIPE.get().onWrenched(straight, top).consumesAction() && h.getBlockState(mid).is(CMBlocks.REINFORCED_STRAIGHT_PIPE.get()), "A wrench on the side of a straight run should lock it");
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CMItems.REACTOR_ALLOY_INGOT.get(), 2));
                    apply(h, player, glass);
                    BlockState made = h.getBlockState(glass);
                    h.assertTrue(made.is(CMBlocks.REINFORCED_STRAIGHT_PIPE.get()) && made.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS) == Direction.Axis.Z && player.getMainHandItem().getCount() == 1,
                            "Reactor alloy on a Glass Fluid Pipe should make a locked Reinforced Pipe along the same run: " + made);
                })
                .thenSucceed();
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

    @GameTest(template = EMPTY)
    public static void circulationPumpTurnsToATouchingPipeWhenPlaced(GameTestHelper h) {
        BlockPos floor = new BlockPos(3, 1, 3);
        BlockPos spot = floor.above();
        h.setBlock(floor, Blocks.STONE);
        Player player = h.makeMockPlayer(GameType.CREATIVE);
        player.setYRot(0);
        player.setXRot(0);
        Direction look = Direction.orderedByNearest(player)[0];
        Direction side = look.getClockWise();
        BlockPos abs = h.absolutePos(floor);
        BlockPlaceContext click = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(CMBlocks.CIRCULATION_PUMP.get()), new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false));
        h.assertTrue(click.getClickedPos().equals(h.absolutePos(spot)), "The click should place the pump on the floor block");
        Direction alone = CMBlocks.CIRCULATION_PUMP.get().getStateForPlacement(click).getValue(DirectionalKineticBlock.FACING);
        h.assertTrue(alone == look.getOpposite(), "A pump with nothing beside it should face the player: " + alone);
        TestVessels.pipes(h, List.of(spot.relative(side)));
        Direction turned = CMBlocks.CIRCULATION_PUMP.get().getStateForPlacement(click).getValue(DirectionalKineticBlock.FACING);
        h.assertTrue(turned == side, "A pump placed beside a pipe should point into it: " + turned);
        player.setShiftKeyDown(true);
        Direction held = CMBlocks.CIRCULATION_PUMP.get().getStateForPlacement(click).getValue(DirectionalKineticBlock.FACING);
        h.assertTrue(held == look, "Sneaking should keep the facing the player chose: " + held);
        player.setShiftKeyDown(false);
        h.setBlock(spot.relative(side), Blocks.AIR);
        h.setBlock(spot.relative(side), AllBlocks.FLUID_TANK.getDefaultState());
        Direction tank = CMBlocks.CIRCULATION_PUMP.get().getStateForPlacement(click).getValue(DirectionalKineticBlock.FACING);
        h.assertTrue(tank == side, "A pump placed beside a Fluid Tank should point into it: " + tank);
        h.setBlock(spot.relative(side), Blocks.AIR);
        TestVessels.pipes(h, List.of(spot.relative(look)));
        Direction inLine = CMBlocks.CIRCULATION_PUMP.get().getStateForPlacement(click).getValue(DirectionalKineticBlock.FACING);
        h.assertTrue(inLine == look.getOpposite(), "A pipe already in line with the pump should leave it as placed: " + inLine);
        h.succeed();
    }
}
