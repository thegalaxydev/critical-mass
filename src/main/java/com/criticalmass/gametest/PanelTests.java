package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.control.PanelBlock;
import com.criticalmass.content.control.PanelBlockEntity;
import com.criticalmass.content.control.PanelBlockEntity.Action;
import com.criticalmass.content.control.PanelBlockEntity.Control;
import com.criticalmass.content.control.PanelBlockEntity.Kind;
import com.criticalmass.content.control.PlantAlarms;
import com.criticalmass.content.control.PlantDisplaySources;
import com.criticalmass.content.control.ScreenBlock;
import com.criticalmass.content.control.ScreenBlockEntity;
import com.criticalmass.content.kinetics.ControlRodDriveBlockEntity;
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
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class PanelTests {
    private static final String EMPTY = "empty";
    private static final String LARGE = "empty_large";

    private PanelTests() {
    }

    private static List<Block> panels() {
        return List.of(CMBlocks.SCRAM_PANEL.get(), CMBlocks.ROD_PANEL.get(), CMBlocks.TURBINE_PANEL.get(),
                CMBlocks.ALARM_PANEL.get(), CMBlocks.LEVER_PANEL.get(), CMBlocks.DIAL_PANEL.get());
    }

    private static PanelBlockEntity panel(GameTestHelper h, Block block, BlockPos pos, Direction facing) {
        h.setBlock(pos, block.defaultBlockState().setValue(PanelBlock.FACING, facing));
        return h.getBlockEntity(pos);
    }

    private static int signal(GameTestHelper h, BlockPos pos) {
        BlockPos abs = h.absolutePos(pos);
        return h.getLevel().getBlockState(abs).getSignal(h.getLevel(), abs, Direction.UP);
    }

    private static ControlRodDriveBlockEntity drive(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, CMBlocks.CONTROL_ROD_DRIVE.get().defaultBlockState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.WEST));
        h.setBlock(pos.east(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.WEST));
        CreativeMotorBlockEntity motor = h.getBlockEntity(pos.east());
        motor.generatedSpeed.setValue(64);
        return h.getBlockEntity(pos);
    }

    private static void use(GameTestHelper h, Player player, PanelBlockEntity panel, Control control, boolean sneak) {
        Vec3 target = panel.position(control);
        Vec3 eye = target.add(Vec3.atLowerCornerOf(panel.facing().getNormal()).scale(0.7)).add(0, 0.8, 0);
        player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, target);
        player.setShiftKeyDown(sneak);
        BlockPos pos = panel.getBlockPos();
        h.getLevel().getBlockState(pos).useWithoutItem(h.getLevel(), player, new BlockHitResult(target, Direction.UP, pos, false));
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void panelControlsAreHitWhereTheyStand(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        Control bare = new Control(Kind.LAMP, Action.NONE, 0.7, 0.7, 1f, 0, "", 0, 0, -1);
        for (Block block : panels()) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                PanelBlockEntity panel = panel(h, block, pos, facing);
                Vec3 front = Vec3.atLowerCornerOf(facing.getNormal());
                List<Control> controls = panel.type().controls;
                for (int i = 0; i < controls.size(); i++) {
                    Control c = controls.get(i);
                    Vec3 target = panel.position(c);
                    h.assertTrue(new AABB(panel.getBlockPos()).inflate(0.01).contains(target), c + " should sit on its own panel when facing " + facing);
                    Vec3 eye = target.add(front.scale(0.7)).add(0, 0.8, 0).add(front.z * 0.15, 0, front.x * 0.15);
                    int picked = panel.pick(eye, target.subtract(eye).normalize());
                    h.assertTrue(picked == (c.kind() == Kind.LAMP ? -1 : i), "Looking at control " + i + " of " + panel.type() + " facing " + facing + " picked " + picked);
                }
                Vec3 corner = panel.position(bare);
                Vec3 eye = corner.add(front.scale(0.7)).add(0, 0.8, 0);
                h.assertTrue(panel.pick(eye, corner.subtract(eye).normalize()) == -1, "The bare corner of a panel should pick nothing");
                h.assertTrue(panel.pick(eye, new Vec3(0, 1, 0)) == -1, "Looking away from the panel should pick nothing");
            }
        }
        h.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 900)
    public static void panelsRunTheLinkedPlant(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        TestVessels.loop(h, true);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 0);
        ControlRodDriveBlockEntity a = drive(h, new BlockPos(3, 6, 2));
        ControlRodDriveBlockEntity b = drive(h, new BlockPos(3, 6, 4));
        BlockPos turbinePos = new BlockPos(9, 1, 2);
        h.setBlock(turbinePos, CMBlocks.STEAM_TURBINE.get().defaultBlockState().setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, Direction.Axis.X));
        SteamTurbineBlockEntity turbine = h.getBlockEntity(turbinePos);
        BlockPos controller = h.absolutePos(TestVessels.CONTROLLER);
        BlockPos scramPos = new BlockPos(8, 1, 10);
        BlockPos leverPos = scramPos.east();
        BlockPos alarmPos = new BlockPos(11, 1, 10);
        BlockPos tripPos = new BlockPos(8, 1, 7);
        PanelBlockEntity scram = panel(h, CMBlocks.SCRAM_PANEL.get(), scramPos, Direction.NORTH);
        PanelBlockEntity lever = panel(h, CMBlocks.LEVER_PANEL.get(), leverPos, Direction.NORTH);
        PanelBlockEntity rods = panel(h, CMBlocks.ROD_PANEL.get(), new BlockPos(10, 1, 10), Direction.NORTH);
        PanelBlockEntity alarms = panel(h, CMBlocks.ALARM_PANEL.get(), alarmPos, Direction.NORTH);
        PanelBlockEntity trip = panel(h, CMBlocks.TURBINE_PANEL.get(), tripPos, Direction.NORTH);
        h.assertTrue(scram.press(Action.SCRAM).endsWith("result.unbound") && trip.press(Action.TURBINE_TRIP).endsWith("result.unbound"),
                "An unlinked panel should say so");
        h.assertTrue(!scram.bind(h.absolutePos(turbinePos)) && !trip.bind(controller) && !lever.bind(controller), "A panel should only link to the machine it controls");
        h.assertTrue(scram.bind(controller) && rods.bind(controller) && alarms.bind(controller), "Reactor panels should link to the controller");
        h.assertTrue(trip.bind(h.absolutePos(turbinePos)), "The turbine panel should link to the turbine");
        h.startSequence()
                .thenIdle(12)
                .thenExecute(() -> {
                    for (int i = 0; i < 101; i++) {
                        rods.press(Action.ROD_OUT);
                    }
                    rods.press(Action.ROD_IN);
                    for (int bank = 0; bank < CMThermal.ROD_BANKS; bank++) {
                        h.assertTrue(Math.abs(c.bankTarget(bank) - 0.5) < 1e-9, "Rod steps should move every bank: " + c.bankTarget(bank));
                    }
                    h.assertTrue(rods.pressed(Action.ROD_IN), "A pressed button should show as pressed");
                    trip.press(Action.TURBINE_TRIP);
                    h.assertTrue(turbine.overspeedTripped(), "The trip button should trip the turbine");
                    trip.press(Action.TURBINE_RESET);
                    h.assertTrue(!turbine.overspeedTripped(), "The reset button should clear the turbine trip");
                    lever.press(Action.TOGGLE);
                    h.assertTrue(signal(h, leverPos) == 15, "A lever panel that is on should give a full signal");
                })
                .thenIdle(270)
                .thenExecute(() -> {
                    h.assertTrue(!rods.pressed(Action.ROD_IN), "A button should spring back");
                    h.assertTrue(Math.abs(a.insertion() - 0.5) < 1e-6 && Math.abs(b.insertion() - 0.5) < 1e-6, "Both drives should follow: " + a.insertion() + " " + b.insertion());
                    h.assertTrue(c.isReacting() && !c.isTripped(), "A lever panel beside the SCRAM panel should not press it: " + c.status());
                    h.assertTrue(!alarms.sounding() && alarms.activeAlarms() == 0 && signal(h, alarmPos) == 0, "No alarms while running normally");
                    h.setBlock(scramPos.north(), Blocks.REDSTONE_BLOCK);
                    h.setBlock(tripPos.north(), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(c.isTripped() && c.tripReason().equals("manual"), "Redstone into the SCRAM panel should trip the reactor: " + c.status());
                    h.assertTrue(turbine.overspeedTripped(), "Redstone into the turbine panel should trip the turbine");
                    h.assertTrue(a.insertion() == 1 && b.insertion() == 1, "SCRAM should drive the rods in: " + a.insertion());
                    h.assertTrue(alarms.alarm(PlantAlarms.Alarm.TRIP) && alarms.sounding(), "The trip lamp should light and sound");
                    h.assertTrue(signal(h, alarmPos) == 15, "A sounding alarm should give a redstone signal");
                    BlockPos alarmAbs = h.absolutePos(alarmPos);
                    h.assertTrue(h.getLevel().getBlockState(alarmAbs).getAnalogOutputSignal(h.getLevel(), alarmAbs) == 1, "A comparator should count one active alarm");
                    alarms.press(Action.ACK);
                    h.assertTrue(!alarms.sounding() && alarms.acknowledged(PlantAlarms.Alarm.TRIP) && signal(h, alarmPos) == 0, "Acknowledging should silence the alarm and keep the lamp lit");
                    String result = scram.press(Action.REACTOR_RESET);
                    h.assertTrue(result.endsWith("result.reset") && !c.isTripped(), "Reset should clear the trip: " + result);
                    h.assertTrue(c.bankTarget(0) == 1.0, "Reset should leave the rods commanded in");
                })
                .thenIdle(25)
                .thenExecute(() -> {
                    h.assertTrue(!c.isTripped() && !alarms.alarm(PlantAlarms.Alarm.TRIP), "A held signal should not press SCRAM again, and the trip lamp should clear");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void leverAndDialPanelsGiveRedstone(GameTestHelper h) {
        BlockPos leverPos = new BlockPos(2, 1, 3);
        BlockPos dialPos = new BlockPos(4, 1, 3);
        PanelBlockEntity lever = panel(h, CMBlocks.LEVER_PANEL.get(), leverPos, Direction.NORTH);
        PanelBlockEntity dial = panel(h, CMBlocks.DIAL_PANEL.get(), dialPos, Direction.EAST);
        h.setBlock(leverPos.west(), Blocks.REDSTONE_LAMP);
        Player player = h.makeMockPlayer(GameType.CREATIVE);
        h.assertTrue(signal(h, leverPos) == 0 && signal(h, dialPos) == 0, "Panels should start switched off");
        use(h, player, lever, lever.type().controls.get(0), false);
        h.assertTrue(lever.on() && signal(h, leverPos) == 15, "Using the lever should switch it on at full strength");
        use(h, player, dial, dial.type().controls.get(0), false);
        h.assertTrue(dial.on() && signal(h, dialPos) == 15, "Using the dial should switch it on at its setting: " + signal(h, dialPos));
        dial.setStrength(7);
        h.assertTrue(signal(h, dialPos) == 7, "The dial should give the strength it is set to: " + signal(h, dialPos));
        use(h, player, dial, dial.type().controls.get(0), true);
        h.assertTrue(dial.on() && dial.strength() == 8 && signal(h, dialPos) == 8, "Sneak-using the dial should raise its setting by one: " + dial.strength());
        dial.setStrength(15);
        use(h, player, dial, dial.type().controls.get(0), true);
        h.assertTrue(dial.strength() == 1, "The setting should wrap from 15 back to 1: " + dial.strength());
        use(h, player, dial, dial.type().controls.get(0), false);
        h.assertTrue(!dial.on() && signal(h, dialPos) == 0, "Using the dial again should switch it off");
        BlockPos floor = h.absolutePos(new BlockPos(2, 0, 5));
        h.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        ItemStack named = new ItemStack(CMBlocks.LEVER_PANEL.get());
        named.set(DataComponents.CUSTOM_NAME, Component.literal("PUMP 1"));
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, named);
        InteractionResult placed = ((BlockItem) named.getItem()).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, named, new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
        h.assertTrue(placed.consumesAction() && h.getLevel().getBlockEntity(floor.above()) instanceof PanelBlockEntity p && p.label().equals("PUMP 1") && p.facing() == player.getDirection().getOpposite(),
                "A named panel should face its builder and show its name");
        h.getLevel().destroyBlock(floor.above(), true);
        List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(floor.above()).inflate(2));
        h.assertTrue(drops.size() == 1 && drops.get(0).getItem().getHoverName().getString().equals("PUMP 1"), "A broken panel should keep its name");
        h.startSequence()
                .thenIdle(4)
                .thenExecute(() -> h.assertTrue(h.getBlockState(leverPos.west()).getValue(RedstoneLampBlock.LIT), "A lever panel should light a lamp beside it"))
                .thenExecute(() -> lever.press(Action.TOGGLE))
                .thenIdle(8)
                .thenExecute(() -> h.assertTrue(!h.getBlockState(leverPos.west()).getValue(RedstoneLampBlock.LIT), "Switching the lever off should drop the signal"))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void linkerLinksAPanelToItsMachine(GameTestHelper h) {
        BlockPos turbinePos = new BlockPos(2, 1, 2);
        BlockPos tankPos = new BlockPos(4, 1, 2);
        h.setBlock(turbinePos, CMBlocks.STEAM_TURBINE.get().defaultBlockState().setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, Direction.Axis.X));
        h.setBlock(tankPos, CMBlocks.EMERGENCY_COOLANT_TANK.get());
        PanelBlockEntity trip = panel(h, CMBlocks.TURBINE_PANEL.get(), new BlockPos(3, 1, 5), Direction.NORTH);
        Player player = h.makeMockPlayer(GameType.CREATIVE);
        ItemStack linker = new ItemStack(CMItems.PLANT_LINKER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, linker);
        BlockPos panelAbs = trip.getBlockPos();
        BlockPos turbineAbs = h.absolutePos(turbinePos);
        BlockPos tankAbs = h.absolutePos(tankPos);
        linker.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, linker, new BlockHitResult(Vec3.atCenterOf(panelAbs), Direction.UP, panelAbs, false)));
        h.assertTrue(trip.target() == null, "An empty linker should link nothing");
        linker.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, linker, new BlockHitResult(Vec3.atCenterOf(tankAbs), Direction.UP, tankAbs, false)));
        linker.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, linker, new BlockHitResult(Vec3.atCenterOf(panelAbs), Direction.UP, panelAbs, false)));
        h.assertTrue(trip.target() == null, "A turbine panel should refuse a machine that is not a turbine");
        linker.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, linker, new BlockHitResult(Vec3.atCenterOf(turbineAbs), Direction.UP, turbineAbs, false)));
        linker.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, linker, new BlockHitResult(Vec3.atCenterOf(panelAbs), Direction.UP, panelAbs, false)));
        h.assertTrue(turbineAbs.equals(trip.target()), "The linker should link the panel to the remembered turbine: " + trip.target());
        h.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 700)
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
                    h.setBlock(corner.south(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.NORTH));
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    for (ScreenBlockEntity s : screens) {
                        h.assertTrue(s.powered(), "A shaft turning behind one screen should power the whole wall");
                    }
                    h.assertTrue(((ScreenBlockEntity) h.getBlockEntity(corner)).getSpeed() != 0, "The screen should be turned through its back");
                    h.setBlock(corner.south(), Blocks.AIR);
                })
                .thenIdle(ScreenBlockEntity.CAPACITY + 20)
                .thenExecute(() -> {
                    for (ScreenBlockEntity s : screens) {
                        h.assertTrue(!s.powered(), "The wall should go dark a while after the shaft stops");
                    }
                    h.assertTrue(!main.lines().isEmpty(), "A dark wall should keep its text for when power returns");
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void linkedScreenWallShowsTheReactorDisplay(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 6000 * (300 - CMThermal.AMBIENT) * CMThermal.HEAT_CAPACITY);
        BlockPos turbinePos = new BlockPos(8, 1, 2);
        h.setBlock(turbinePos, CMBlocks.STEAM_TURBINE.get().defaultBlockState().setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, Direction.Axis.X));
        List<ScreenBlockEntity> wall = new ArrayList<>();
        BlockPos corner = new BlockPos(4, 1, 13);
        for (int y = 0; y < ScreenBlockEntity.BOARD_HEIGHT; y++) {
            for (int x = 0; x < ScreenBlockEntity.BOARD_WIDTH; x++) {
                BlockPos p = corner.offset(x, y, 0);
                h.setBlock(p, CMBlocks.PLANT_SCREEN.get().defaultBlockState().setValue(ScreenBlock.FACING, Direction.NORTH));
                wall.add(h.getBlockEntity(p));
            }
        }
        BlockPos smallPos = new BlockPos(9, 1, 8);
        h.setBlock(smallPos, CMBlocks.PLANT_SCREEN.get().defaultBlockState().setValue(ScreenBlock.FACING, Direction.NORTH));
        ScreenBlockEntity small = h.getBlockEntity(smallPos);
        ScreenBlockEntity main = h.getBlockEntity(corner.offset(ScreenBlockEntity.BOARD_WIDTH - 1, ScreenBlockEntity.BOARD_HEIGHT - 1, 0));
        h.onEachTick(() -> {
            for (ScreenBlockEntity s : wall) {
                s.charge();
            }
            small.charge();
        });
        Player player = h.makeMockPlayer(GameType.CREATIVE);
        ItemStack linker = new ItemStack(CMItems.PLANT_LINKER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, linker);
        BlockPos controller = h.absolutePos(TestVessels.CONTROLLER);
        BlockPos turbineAbs = h.absolutePos(turbinePos);
        BlockPos screenAbs = h.absolutePos(corner);
        BlockPos smallAbs = h.absolutePos(smallPos);
        BlockPos stone = h.absolutePos(new BlockPos(8, 1, 5));
        h.getLevel().setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
        h.startSequence()
                .thenIdle(15)
                .thenExecute(() -> {
                    h.assertTrue(main.isController() && main.width() == ScreenBlockEntity.BOARD_WIDTH && main.height() == ScreenBlockEntity.BOARD_HEIGHT && main.bigEnough(), "The wall should measure nine by five");
                    h.assertTrue(!main.linked() && !main.link(stone), "A wall should only link to plant machines");
                    linker.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, linker, new BlockHitResult(Vec3.atCenterOf(controller), Direction.UP, controller, false)));
                    linker.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, linker, new BlockHitResult(Vec3.atCenterOf(screenAbs), Direction.NORTH, screenAbs, false)));
                    h.assertTrue(main.linked() && controller.equals(main.reactor()), "Linking any screen should link the whole wall: " + main.reactor());
                    linker.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, linker, new BlockHitResult(Vec3.atCenterOf(smallAbs), Direction.NORTH, smallAbs, false)));
                    h.assertTrue(small.linked() && !small.bigEnough(), "A small wall should link and report that it is too small");
                    linker.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, linker, new BlockHitResult(Vec3.atCenterOf(turbineAbs), Direction.UP, turbineAbs, false)));
                    linker.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, linker, new BlockHitResult(Vec3.atCenterOf(screenAbs), Direction.NORTH, screenAbs, false)));
                    h.assertTrue(main.hasTurbine() && controller.equals(main.reactor()), "A turbine should join the display without replacing the reactor");
                })
                .thenIdle(70)
                .thenExecute(() -> {
                    h.assertTrue(main.samples() >= 3, "A powered, linked wall should sample once a second: " + main.samples());
                    h.assertTrue(main.values().size() == ScreenBlockEntity.ROWS.size() && main.status().endsWith(".short"), "The wall should hold a status and every reading: " + main.status() + " " + main.values());
                    h.assertTrue(Math.abs(main.latest(ScreenBlockEntity.Trend.COOLANT) - 300) < 15, "The coolant trend should follow the coolant temperature: " + main.latest(ScreenBlockEntity.Trend.COOLANT));
                    h.assertTrue(!main.speed().isEmpty() && !main.output().isEmpty(), "A linked turbine should show its speed and shaft load");
                    CompoundTag sent = main.getUpdateTag(h.getLevel().registryAccess());
                    h.assertTrue(sent.getByteArray("Trend2").length == main.samples() && sent.getFloat("Scale2") == 500f, "The trend should be sent scaled to a round maximum: " + sent.getFloat("Scale2"));
                    h.assertTrue((sent.getByteArray("Trend2")[0] & 255) > 130 && (sent.getByteArray("Trend2")[0] & 255) < 175, "300 on a scale of 500 should plot at three fifths");
                    h.assertTrue(main.events().isEmpty(), "A steady plant should log nothing: " + main.events());
                    c.coolant().set(CoolantLoopTests.coolant(1000), 1000 * (300 - CMThermal.AMBIENT) * CMThermal.HEAT_CAPACITY);
                })
                .thenIdle(45)
                .thenExecute(() -> {
                    h.assertTrue(!main.events().isEmpty() && main.events().get(0).key().equals(PlantAlarms.Alarm.LOW_COOLANT.langKey()), "A new alarm should head the event list: " + main.events());
                    c.coolant().set(CoolantLoopTests.coolant(6000), 6000 * (300 - CMThermal.AMBIENT) * CMThermal.HEAT_CAPACITY);
                })
                .thenIdle(45)
                .thenExecute(() -> {
                    h.assertTrue(main.events().size() >= 2 && main.events().get(0).about().equals(PlantAlarms.Alarm.LOW_COOLANT.langKey()), "A cleared alarm should be logged too: " + main.events());
                    ItemStack empty = new ItemStack(CMItems.PLANT_LINKER.get());
                    player.setItemInHand(InteractionHand.MAIN_HAND, empty);
                    empty.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, empty, new BlockHitResult(Vec3.atCenterOf(screenAbs), Direction.NORTH, screenAbs, false)));
                    h.assertTrue(!main.linked() && main.samples() == 0, "An empty linker should unlink the wall");
                })
                .thenSucceed();
    }
}
