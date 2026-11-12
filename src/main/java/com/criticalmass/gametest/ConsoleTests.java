package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.control.ConsoleBlock;
import com.criticalmass.content.control.ConsoleBlockEntity;
import com.criticalmass.content.control.ConsoleBlockEntity.Control;
import com.criticalmass.content.control.PlantDisplaySources;
import com.criticalmass.content.control.ScreenBlock;
import com.criticalmass.content.control.ScreenBlockEntity;
import com.criticalmass.content.kinetics.GeneratorBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class ConsoleTests {
    private static final String LARGE = "empty_large";
    private static final BlockPos MASTER = new BlockPos(10, 1, 9);

    private ConsoleTests() {
    }

    private static ConsoleBlockEntity console(GameTestHelper h, BlockPos pos, Direction facing) {
        h.setBlock(pos, CMBlocks.CONTROL_CONSOLE.get().defaultBlockState().setValue(ConsoleBlock.FACING, facing));
        return h.getBlockEntity(pos);
    }

    private static int count(GameTestHelper h, BlockPos master, Direction facing) {
        int found = 0;
        for (BlockPos p : ConsoleBlock.positions(h.absolutePos(master), facing)) {
            if (h.getLevel().getBlockState(p).getBlock() instanceof ConsoleBlock) {
                found++;
            }
        }
        return found;
    }

    private static int signal(GameTestHelper h, ConsoleBlockEntity console, int column, ConsoleBlock.Part part) {
        BlockPos p = ConsoleBlock.partPos(console.getBlockPos(), console.facing(), column, part);
        return h.getLevel().getBlockState(p).getSignal(h.getLevel(), p, Direction.UP);
    }

    @GameTest(template = LARGE, timeoutTicks = 40)
    public static void consoleNeedsItsWholeSpaceAndLeavesInOnePiece(GameTestHelper h) {
        BlockPos floor = h.absolutePos(MASTER.below());
        BlockPos master = h.absolutePos(MASTER);
        h.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        BlockPos blocker = ConsoleBlock.partPos(master, player.getDirection().getOpposite(), 0, ConsoleBlock.Part.BACK_TOP);
        ItemStack stack = new ItemStack(CMItems.CONTROL_CONSOLE.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
        h.getLevel().setBlockAndUpdate(blocker, Blocks.STONE.defaultBlockState());
        InteractionResult refused = CMItems.CONTROL_CONSOLE.get().place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit));
        h.assertTrue(refused == InteractionResult.FAIL && stack.getCount() == 2, "A blocked space should refuse the console: " + refused);
        h.assertTrue(!(h.getLevel().getBlockState(master).getBlock() instanceof ConsoleBlock), "A refused console should leave no parts behind");
        h.getLevel().setBlockAndUpdate(blocker, Blocks.AIR.defaultBlockState());
        InteractionResult placed = CMItems.CONTROL_CONSOLE.get().place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit));
        h.assertTrue(placed.consumesAction() && stack.getCount() == 1, "A clear space should take the console: " + placed);
        BlockState state = h.getLevel().getBlockState(master);
        h.assertTrue(state.getBlock() instanceof ConsoleBlock && ConsoleBlock.isMaster(state), "The clicked block should be the middle of the console");
        Direction facing = state.getValue(ConsoleBlock.FACING);
        h.assertTrue(facing == player.getDirection().getOpposite(), "The console should face the player who placed it: " + facing);
        h.assertTrue(count(h, MASTER, facing) == 20, "The console should fill five by two by two blocks: " + count(h, MASTER, facing));
        int entities = 0;
        for (BlockPos p : ConsoleBlock.positions(master, facing)) {
            h.assertTrue(master.equals(ConsoleBlock.masterPos(p, h.getLevel().getBlockState(p))), "Every part should know its middle block");
            if (h.getLevel().getBlockEntity(p) != null) {
                entities++;
            }
        }
        h.assertTrue(entities == 1, "Only the middle block should carry the console state: " + entities);
        h.getLevel().destroyBlock(ConsoleBlock.partPos(master, facing, 4, ConsoleBlock.Part.BACK_TOP), true);
        h.assertTrue(count(h, MASTER, facing) == 0, "Breaking any part should remove the whole console: " + count(h, MASTER, facing));
        h.assertItemEntityCountIs(CMItems.CONTROL_CONSOLE.get(), MASTER, 5, 1);
        h.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 40)
    public static void consoleControlsAreHitWhereTheyStand(GameTestHelper h) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            ConsoleBlockEntity console = console(h, MASTER, facing);
            h.assertTrue(count(h, MASTER, facing) == 20, "A console facing " + facing + " should place all its parts");
            Vec3 front = Vec3.atLowerCornerOf(facing.getNormal());
            for (Control c : Control.values()) {
                Vec3 target = console.position(c);
                BlockPos under = BlockPos.containing(target.x, target.y - 0.01, target.z);
                BlockState below = h.getLevel().getBlockState(under);
                h.assertTrue(below.getBlock() instanceof ConsoleBlock && console.getBlockPos().equals(ConsoleBlock.masterPos(under, below)), c + " should sit on its own console when facing " + facing);
                Vec3 eye = target.add(front.scale(0.8)).add(0, 0.9, 0).add(front.z * 0.2, 0, front.x * 0.2);
                Control picked = console.pick(eye, target.subtract(eye).normalize());
                h.assertTrue(picked == (c.kind == ConsoleBlockEntity.Kind.LAMP ? null : c), "Looking at " + c + " facing " + facing + " picked " + picked);
            }
            Vec3 between = console.position(Control.SCRAM).add(console.position(Control.REACTOR_RESET)).scale(0.5);
            Vec3 eye = between.add(front.scale(0.8)).add(0, 0.9, 0);
            h.assertTrue(console.pick(eye, between.subtract(eye).normalize()) == null, "The bare panel between controls should pick nothing");
            h.assertTrue(console.pick(eye, new Vec3(0, 1, 0)) == null, "Looking away from the panel should pick nothing");
            h.setBlock(MASTER, Blocks.AIR);
            h.assertTrue(count(h, MASTER, facing) == 0, "Removing the middle block should remove the console facing " + facing);
        }
        h.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 40)
    public static void wrenchTurnsTheWholeConsoleAndKeepsItsState(GameTestHelper h) {
        ConsoleBlockEntity console = console(h, MASTER, Direction.NORTH);
        BlockPos master = h.absolutePos(MASTER);
        BlockPos machine = new BlockPos(3, 1, 3);
        h.setBlock(machine, CMBlocks.GENERATOR.get().defaultBlockState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
        h.assertTrue(console.bind(h.absolutePos(machine)), "The console should bind the generator");
        console.press(Control.SWITCH_2);
        console.press(Control.DIAL_4);
        ConsoleBlock block = CMBlocks.CONTROL_CONSOLE.get();
        BlockPos blocker = ConsoleBlock.partPos(master, Direction.EAST, 0, ConsoleBlock.Part.FRONT_TOP);
        h.getLevel().setBlockAndUpdate(blocker, Blocks.STONE.defaultBlockState());
        h.assertTrue(!block.turn(h.getLevel(), master) && count(h, MASTER, Direction.NORTH) == 20, "A blocked turn should leave the console as it stands");
        h.getLevel().setBlockAndUpdate(blocker, Blocks.AIR.defaultBlockState());
        Direction facing = Direction.NORTH;
        for (int i = 0; i < 4; i++) {
            h.assertTrue(block.turn(h.getLevel(), master), "A clear turn should succeed from " + facing);
            facing = facing.getClockWise();
            ConsoleBlockEntity turned = h.getBlockEntity(MASTER);
            h.assertTrue(turned.facing() == facing && count(h, MASTER, facing) == 20, "The console should now face " + facing);
            int total = 0;
            for (BlockPos p : BlockPos.betweenClosed(master.offset(-3, 0, -3), master.offset(3, 1, 3))) {
                if (h.getLevel().getBlockState(p).getBlock() instanceof ConsoleBlock) {
                    total++;
                }
            }
            h.assertTrue(total == 20, "A turn should leave no parts behind: " + total);
            h.assertTrue(turned.on(Control.SWITCH_2) && turned.on(Control.DIAL_4) && !turned.on(Control.SWITCH_1), "A turned console should keep its switches");
            h.assertTrue(turned.press(Control.BREAKER).contains("breaker"), "A turned console should keep its bindings");
            h.assertTrue(signal(h, turned, 1, ConsoleBlock.Part.BACK_BOTTOM) == 15 && signal(h, turned, 3, ConsoleBlock.Part.BACK_TOP) == 15, "Outputs should move with the console");
        }
        h.assertItemEntityCountIs(CMItems.CONTROL_CONSOLE.get(), MASTER, 6, 0);
        h.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void consoleRunsTheBoundPlant(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 0);
        for (BlockPos drive : List.of(new BlockPos(3, 6, 2), new BlockPos(3, 6, 4))) {
            h.setBlock(drive, CMBlocks.CONTROL_ROD_DRIVE.get().defaultBlockState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.WEST));
            h.setBlock(drive.east(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.WEST));
        }
        BlockPos turbinePos = new BlockPos(8, 1, 2);
        h.setBlock(turbinePos, CMBlocks.STEAM_TURBINE.get().defaultBlockState().setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, Direction.Axis.X));
        h.setBlock(turbinePos.east(), CMBlocks.GENERATOR.get().defaultBlockState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
        SteamTurbineBlockEntity turbine = h.getBlockEntity(turbinePos);
        GeneratorBlockEntity generator = h.getBlockEntity(turbinePos.east());
        ConsoleBlockEntity console = console(h, MASTER, Direction.NORTH);
        h.assertTrue(console.press(Control.SCRAM).endsWith("needs.reactor"), "An unbound console should ask for a reactor");
        h.assertTrue(console.press(Control.TURBINE_TRIP).endsWith("needs.turbine"), "An unbound console should ask for a turbine");
        h.assertTrue(console.press(Control.BREAKER).endsWith("needs.generator"), "An unbound console should ask for a generator");
        h.assertTrue(!console.bind(h.absolutePos(MASTER.below())), "A console should only bind to plant machines");
        h.assertTrue(console.bind(h.absolutePos(TestVessels.CONTROLLER)) && console.bind(h.absolutePos(turbinePos)) && console.bind(h.absolutePos(turbinePos.east())), "A console should bind a reactor, a turbine and a generator");
        h.startSequence()
                .thenIdle(12)
                .thenExecute(() -> {
                    h.assertTrue(console.on(Control.BREAKER), "The breaker switch should follow a closed breaker");
                    for (int i = 0; i < 100; i++) {
                        console.press(Control.ROD_OUT);
                    }
                    for (int bank = 0; bank < CMThermal.ROD_BANKS; bank++) {
                        h.assertTrue(Math.abs(c.bankTarget(bank) - 0.5) < 1e-9, "A hundred steps out should ask every bank for half insertion: " + c.bankTarget(bank));
                    }
                    console.press(Control.ROD_IN);
                    h.assertTrue(Math.abs(c.bankTarget(0) - 0.505) < 1e-9, "One step in should push the rods back by one step: " + c.bankTarget(0));
                    h.assertTrue(console.pressed(Control.ROD_IN), "A pressed button should show as pressed");
                    console.press(Control.SWITCH_1);
                    console.press(Control.DIAL_2);
                    h.assertTrue(console.on(Control.SWITCH_1) && console.on(Control.DIAL_2) && !console.on(Control.SWITCH_2), "Switches and dials should latch");
                    h.assertTrue(signal(h, console, 0, ConsoleBlock.Part.BACK_BOTTOM) == 15 && signal(h, console, 1, ConsoleBlock.Part.BACK_BOTTOM) == 0, "Switch 1 should power the first lower back block");
                    h.assertTrue(signal(h, console, 1, ConsoleBlock.Part.BACK_TOP) == 15 && signal(h, console, 0, ConsoleBlock.Part.BACK_TOP) == 0, "Dial 2 should power the second upper back block");
                    h.assertTrue(signal(h, console, 0, ConsoleBlock.Part.FRONT_BOTTOM) == 0, "Front blocks should give no signal");
                    console.press(Control.SWITCH_1);
                    h.assertTrue(signal(h, console, 0, ConsoleBlock.Part.BACK_BOTTOM) == 0, "Switching off should drop the signal");
                    h.assertTrue(signal(h, console, 4, ConsoleBlock.Part.BACK_BOTTOM) == 0 && signal(h, console, 4, ConsoleBlock.Part.BACK_TOP) == 0, "No alarm or trip signal while all is well");
                    console.press(Control.TURBINE_TRIP);
                    h.assertTrue(turbine.overspeedTripped(), "The trip button should trip the turbine");
                    console.press(Control.TURBINE_RESET);
                    h.assertTrue(!turbine.overspeedTripped(), "The reset button should clear the turbine trip");
                    h.assertTrue(console.press(Control.BREAKER).endsWith("breaker_open") && !console.on(Control.BREAKER), "The breaker switch should open the breaker");
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(!console.pressed(Control.ROD_IN), "A button should spring back");
                    h.assertTrue(generator.breakerOpen() && !console.on(Control.BREAKER), "The generator breaker should be open");
                    console.press(Control.SCRAM);
                    h.assertTrue(c.isTripped() && c.tripReason().equals("manual"), "SCRAM should trip the reactor: " + c.status());
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(console.alarm(Control.LAMP_TRIP) && console.sounding(), "The trip lamp should light and sound");
                    h.assertTrue(signal(h, console, 4, ConsoleBlock.Part.BACK_BOTTOM) == 15 && signal(h, console, 4, ConsoleBlock.Part.BACK_TOP) == 15, "A trip should power the alarm and trip outputs");
                    console.press(Control.ACK);
                    h.assertTrue(!console.sounding() && console.acknowledgedAlarm(Control.LAMP_TRIP), "Acknowledging should silence the alarm and keep the lamp lit");
                    h.assertTrue(signal(h, console, 4, ConsoleBlock.Part.BACK_BOTTOM) == 0 && signal(h, console, 4, ConsoleBlock.Part.BACK_TOP) == 15, "Acknowledging should drop the alarm output only");
                    String result = console.press(Control.REACTOR_RESET);
                    h.assertTrue(result.endsWith("result.reset") && !c.isTripped(), "Reset should clear the trip: " + result);
                    h.assertTrue(console.press(Control.BREAKER).endsWith("breaker_close"), "The breaker switch should close the breaker again");
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(!console.alarm(Control.LAMP_TRIP) && signal(h, console, 4, ConsoleBlock.Part.BACK_TOP) == 0, "The trip lamp and output should clear after the reset");
                    h.assertTrue(!generator.breakerOpen() && console.on(Control.BREAKER), "The generator breaker should be closed again");
                    Player player = h.makeMockPlayer(GameType.CREATIVE);
                    Vec3 target = console.position(Control.SWITCH_3);
                    Vec3 eye = target.add(0, 0.9, -0.8);
                    player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
                    player.lookAt(EntityAnchorArgument.Anchor.EYES, target);
                    BlockPos part = ConsoleBlock.partPos(console.getBlockPos(), Direction.NORTH, 3, ConsoleBlock.Part.FRONT_TOP);
                    h.getLevel().getBlockState(part).useWithoutItem(h.getLevel(), player, new BlockHitResult(target, Direction.UP, part, false));
                    h.assertTrue(console.on(Control.SWITCH_3), "Using the console while looking at switch 3 should throw it");
                    h.assertTrue(signal(h, console, 2, ConsoleBlock.Part.BACK_BOTTOM) == 15, "Switch 3 should power the third lower back block");
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void screenWallSharesPowerAndShowsLinkedText(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        List<ScreenBlockEntity> screens = new ArrayList<>();
        BlockPos corner = new BlockPos(8, 1, 10);
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) {
                BlockPos p = corner.offset(x, y, 0);
                h.setBlock(p, CMBlocks.PLANT_SCREEN.get().defaultBlockState().setValue(ScreenBlock.FACING, Direction.NORTH));
                screens.add(h.getBlockEntity(p));
            }
        }
        ScreenBlockEntity main = h.getBlockEntity(corner.offset(2, 1, 0));
        BlockPos linkPos = TestVessels.CONTROLLER.west();
        h.setBlock(linkPos, AllBlocks.DISPLAY_LINK.getDefaultState().setValue(DisplayLinkBlock.FACING, Direction.WEST));
        DisplayLinkBlockEntity link = h.getBlockEntity(linkPos);
        h.startSequence()
                .thenIdle(15)
                .thenExecute(() -> {
                    for (ScreenBlockEntity s : screens) {
                        h.assertTrue(s.isController() == (s == main) && !s.powered(), "Only the top left screen should lead the wall, and none should be powered yet");
                    }
                    h.assertTrue(main.width() == 3 && main.height() == 2, "The wall should measure three by two: " + main.width() + " by " + main.height());
                    h.assertTrue(DisplayTarget.get(h.getLevel(), h.absolutePos(corner)) == PlantDisplaySources.SCREEN.get(), "A screen should be a display target");
                    link.target(h.absolutePos(corner));
                    link.activeSource = PlantDisplaySources.STATUS.get();
                    link.updateGatheredData();
                    h.assertTrue(main.lines().size() >= 2 && !main.lines().get(0).isEmpty() && main.lines().get(1).endsWith("°C"), "The wall should take the reactor summary: " + main.lines());
                    h.assertTrue(main.lines().size() <= 6, "A three by two wall holds six lines: " + main.lines().size());
                    main.setLine(6, "overflow");
                    h.assertTrue(main.lines().size() <= 6, "Lines past the bottom of the wall should be dropped");
                    IEnergyStorage input = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(corner), Direction.DOWN);
                    h.assertTrue(input != null && input.receiveEnergy(1000, false) == ScreenBlockEntity.CAPACITY, "A screen should take energy up to its buffer");
                    h.assertTrue(input.extractEnergy(100, false) == 0, "A screen should not give energy back");
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    for (ScreenBlockEntity s : screens) {
                        h.assertTrue(s.powered(), "Power fed into one screen should reach the whole wall");
                    }
                })
                .thenIdle(120)
                .thenExecute(() -> {
                    for (ScreenBlockEntity s : screens) {
                        h.assertTrue(!s.powered(), "The wall should go dark when the power runs out");
                    }
                    h.assertTrue(!main.lines().isEmpty(), "A dark wall should keep its text for when power returns");
                })
                .thenSucceed();
    }
}
