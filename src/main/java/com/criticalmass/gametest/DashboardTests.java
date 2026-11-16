package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.control.DashPart;
import com.criticalmass.content.control.DashboardBlock;
import com.criticalmass.content.control.DashboardBlockEntity;
import com.criticalmass.content.control.LoconauticsCompat;
import com.criticalmass.content.control.PartPanelBlock;
import com.criticalmass.content.control.PlantAlarms;
import com.criticalmass.content.control.SignalInterfaceBlock;
import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.content.kinetics.ControlRodDriveBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class DashboardTests {
    private static final String EMPTY = "empty";
    private static final String LARGE = "empty_large";

    private DashboardTests() {
    }

    private static DashboardBlockEntity desk(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, CMBlocks.REACTOR_ALLOY_DASHBOARD.get());
        return h.getBlockEntity(pos);
    }

    private static DashPart fit(GameTestHelper h, DashboardBlockEntity desk, int slot, DashPart.Type type, BlockPos target) {
        DashPart part = new DashPart(type);
        h.assertTrue(desk.install(slot, part), "An empty half should take a part");
        if (target != null) {
            h.assertTrue(desk.bind(slot, h.absolutePos(target)), "The " + type.key + " should bind to its target");
        }
        return part;
    }

    @GameTest(template = EMPTY)
    public static void dashboardsJoinIntoRunsOfFive(GameTestHelper h) {
        Player player = h.makeMockPlayer(GameType.CREATIVE);
        player.setYRot(0);
        player.setXRot(0);
        DashboardBlock block = CMBlocks.REACTOR_ALLOY_DASHBOARD.get();
        for (int x = 1; x <= 6; x++) {
            BlockPos floor = new BlockPos(x, 1, 3);
            h.setBlock(floor, Blocks.STONE);
            BlockPos abs = h.absolutePos(floor);
            BlockState state = block.getStateForPlacement(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(block),
                    new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
            h.setBlock(floor.above(), state);
        }
        Direction facing = h.getBlockState(new BlockPos(1, 2, 3)).getValue(DashboardBlock.FACING);
        Direction along = facing.getClockWise();
        int joins = 0;
        for (int x = 1; x <= 6; x++) {
            BlockState state = h.getBlockState(new BlockPos(x, 2, 3));
            h.assertTrue(state.getValue(DashboardBlock.FACING) == facing, "Every desk should face the player");
            if (state.getValue(DashboardBlock.JOIN_CW)) {
                joins++;
                BlockState next = h.getLevel().getBlockState(h.absolutePos(new BlockPos(x, 2, 3)).relative(along));
                h.assertTrue(next.is(block) && next.getValue(DashboardBlock.JOIN_CCW), "A join should be shared by both desks at " + x);
            }
        }
        h.assertTrue(joins == 4, "Six desks in a row should make one run of five and one desk on its own: " + joins + " joins");
        h.setBlock(new BlockPos(3, 2, 3), Blocks.AIR);
        h.assertTrue(!h.getBlockState(new BlockPos(2, 2, 3)).getValue(DashboardBlock.JOIN_CW) || !h.getBlockState(new BlockPos(2, 2, 3)).getValue(DashboardBlock.JOIN_CCW),
                "Removing a desk should open the run on both sides");
        h.assertTrue(!h.getBlockState(new BlockPos(4, 2, 3)).getValue(DashboardBlock.JOIN_CW) || !h.getBlockState(new BlockPos(4, 2, 3)).getValue(DashboardBlock.JOIN_CCW),
                "Removing a desk should open the run on both sides");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void dyeOnlyRecoloursWhenLoconauticsSuppliesTheColours(GameTestHelper h) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0);
        player.setXRot(0);
        DashboardBlock block = CMBlocks.REACTOR_ALLOY_DASHBOARD.get();
        for (int x = 1; x <= 3; x++) {
            BlockPos floor = new BlockPos(x, 1, 3);
            h.setBlock(floor, Blocks.STONE);
            BlockPos abs = h.absolutePos(floor);
            BlockState state = block.getStateForPlacement(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(block),
                    new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
            h.setBlock(floor.above(), state);
        }
        DashboardBlockEntity fitted = h.getBlockEntity(new BlockPos(1, 2, 3));
        fitted.install(0, new DashPart(DashPart.Type.SWITCH));
        BlockPos middle = h.absolutePos(new BlockPos(2, 2, 3));
        ItemStack dye = new ItemStack(Items.RED_DYE, 2);
        h.getLevel().getBlockState(middle).useItemOn(dye, h.getLevel(), player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(middle), Direction.UP, middle, false));
        boolean colours = LoconauticsCompat.loaded();
        DashboardBlock.Skin expected = colours ? DashboardBlock.Skin.RED : DashboardBlock.Skin.ALLOY;
        for (int x = 1; x <= 3; x++) {
            h.assertTrue(h.getBlockState(new BlockPos(x, 2, 3)).getValue(DashboardBlock.SKIN) == expected, "Desk " + x + " should be " + expected.key + " after dyeing");
        }
        h.assertTrue(dye.getCount() == (colours ? 1 : 2), "Dye should only be used when it recolours the run: " + dye.getCount() + " left");
        DashboardBlockEntity kept = h.getBlockEntity(new BlockPos(1, 2, 3));
        h.assertTrue(kept.part(0) != null && kept.part(0).type == DashPart.Type.SWITCH, "Dyeing should not disturb the parts on a desk");
        h.assertTrue(h.getBlockState(new BlockPos(2, 2, 3)).getValue(DashboardBlock.JOIN_CW) && h.getBlockState(new BlockPos(2, 2, 3)).getValue(DashboardBlock.JOIN_CCW),
                "Dyeing should not break the run apart");
        List<ItemStack> drops = Block.getDrops(h.getBlockState(new BlockPos(3, 2, 3)), h.getLevel(), h.absolutePos(new BlockPos(3, 2, 3)), null);
        h.assertTrue(drops.size() == 1 && drops.get(0).is(block.asItem()) != colours, "A recoloured desk should drop as that Loconautics dashboard, an alloy desk as itself: " + drops);
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void partsGoOntoALoconauticsDashboardByAdoptingIt(GameTestHelper h) {
        Block theirs = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(LoconauticsCompat.MOD, "blue_dashboard"));
        if (theirs == Blocks.AIR) {
            h.succeed();
            return;
        }
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, theirs);
        BlockPos abs = h.absolutePos(pos);
        h.assertTrue(LoconauticsCompat.theirDesk(h.getLevel().getBlockState(abs)), "A Loconautics dashboard should be recognised");
        Direction facing = LoconauticsCompat.facing(h.getLevel().getBlockState(abs));
        ItemStack button = new ItemStack(CMItems.DASHBOARD_PARTS.get(DashPart.Type.BUTTON).get());
        player.setItemInHand(InteractionHand.MAIN_HAND, button);
        button.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, button, new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.4, 0), Direction.UP, abs, false)));
        BlockState now = h.getLevel().getBlockState(abs);
        h.assertTrue(now.is(CMBlocks.REACTOR_ALLOY_DASHBOARD.get()) && now.getValue(DashboardBlock.SKIN) == DashboardBlock.Skin.BLUE && now.getValue(DashboardBlock.FACING) == facing,
                "Fitting one of our parts should turn the desk into ours in the same colour and facing: " + now);
        DashboardBlockEntity desk = h.getBlockEntity(pos);
        h.assertTrue(desk.part(0) != null || desk.part(1) != null, "The part should be fitted");
        h.assertTrue(button.isEmpty(), "The part should be used up");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void dashboardPartsWorkTheReactor(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        DashboardBlockEntity left = desk(h, new BlockPos(6, 1, 7));
        DashboardBlockEntity right = desk(h, new BlockPos(7, 1, 7));
        DashPart scram = fit(h, left, 0, DashPart.Type.BUTTON, TestVessels.CONTROLLER);
        DashPart gauge = fit(h, left, 1, DashPart.Type.GAUGE, TestVessels.CONTROLLER);
        DashPart alarm = fit(h, right, 0, DashPart.Type.ALARM, TestVessels.CONTROLLER);
        h.assertTrue(!right.bind(0, new BlockPos(0, 0, 0)), "An alarm should refuse a block that is not a reactor");
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(!gauge.readout.isEmpty() && gauge.fraction >= 0 && gauge.fraction <= 1, "A bound gauge should read its machine: " + gauge.readout);
                    String first = left.modeKey(gauge);
                    left.cycle(1);
                    h.assertTrue(!left.modeKey(gauge).equals(first), "A wrench should step the gauge to its next reading: " + first);
                    h.assertTrue((alarm.alarms & 1 << PlantAlarms.Alarm.LOW_COOLANT.ordinal()) != 0 && alarm.sounding(), "An empty vessel should raise the alarm: " + alarm.alarms);
                    h.assertTrue(right.signal() == 15 && right.activeAlarms() >= 1, "A sounding alarm should give a redstone signal");
                    right.use(0, 0.5);
                    h.assertTrue(!alarm.sounding() && alarm.alarming() && right.signal() == 0, "Pressing the alarm should acknowledge it and stop the signal");
                    h.assertTrue(!c.isTripped(), "Nothing should have tripped the reactor yet");
                    left.use(0, 0.5);
                    h.assertTrue(c.isTripped(), "A button bound to the reactor should SCRAM it");
                    h.assertTrue(scram.pressed(h.getLevel().getGameTime()), "The button should stay down for a moment");
                    ItemStack taken = left.remove(0);
                    h.assertTrue(left.part(0) == null && h.absolutePos(TestVessels.CONTROLLER).equals(taken.get(CMComponents.LINK_TARGET.get())), "A removed part should keep its binding");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void dashboardControlsDriveARedstoneLink(GameTestHelper h) {
        BlockPos linkPos = new BlockPos(3, 2, 3);
        h.setBlock(linkPos.below(), Blocks.STONE);
        h.setBlock(linkPos, AllBlocks.REDSTONE_LINK.getDefaultState().setValue(RedstoneLinkBlock.FACING, Direction.UP).setValue(RedstoneLinkBlock.RECEIVER, true));
        DashboardBlockEntity desk = desk(h, new BlockPos(5, 1, 5));
        DashPart lever = fit(h, desk, 0, DashPart.Type.LEVER, linkPos);
        DashPart toggle = fit(h, desk, 1, DashPart.Type.SWITCH, linkPos);
        DashboardBlockEntity other = desk(h, new BlockPos(1, 1, 5));
        h.assertTrue(other.install(0, new DashPart(DashPart.Type.SWITCH)) && !other.bind(0, h.absolutePos(new BlockPos(5, 1, 5))), "A switch should refuse a block that is not a receiving Redstone Link");
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    h.assertTrue(!h.getBlockState(linkPos).getValue(RedstoneLinkBlock.POWERED), "Nothing is switched on yet");
                    desk.use(0, 0.6);
                    h.assertTrue(lever.level == 9, "A lever should take its strength from where it is clicked: " + lever.level);
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    h.assertTrue(h.getBlockState(linkPos).getValue(RedstoneLinkBlock.POWERED), "The lever should power the link it is bound to");
                    h.assertTrue(h.getLevel().getSignal(h.absolutePos(linkPos), Direction.UP) == 9 || h.getLevel().getBestNeighborSignal(h.absolutePos(linkPos.below())) == 9,
                            "The link should give the lever's strength");
                    desk.use(1, 0.5);
                    h.assertTrue(toggle.on, "The switch should turn on");
                    desk.use(0, 0);
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    h.assertTrue(h.getBlockState(linkPos).getValue(RedstoneLinkBlock.POWERED), "The strongest control on a link should win");
                    desk.use(1, 0.5);
                })
                .thenIdle(10)
                .thenExecute(() -> h.assertTrue(!h.getBlockState(linkPos).getValue(RedstoneLinkBlock.POWERED), "With every control off the link should drop out"))
                .thenSucceed();
    }

    private static ControlRodDriveBlockEntity drive(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, CMBlocks.CONTROL_ROD_DRIVE.get().defaultBlockState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.WEST));
        h.setBlock(pos.east(), AllBlocks.COGWHEEL.getDefaultState());
        h.setBlock(pos.east().above(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        CreativeMotorBlockEntity motor = h.getBlockEntity(pos.east().above());
        motor.generatedSpeed.setValue(64);
        return h.getBlockEntity(pos);
    }

    private static DashPart button(GameTestHelper h, DashboardBlockEntity desk, int slot, BlockPos target, DashPart.Press press) {
        DashPart part = fit(h, desk, slot, DashPart.Type.BUTTON, target);
        for (int i = 0; i < 8 && part.press(h.getLevel()) != press; i++) {
            desk.cycle(slot);
        }
        h.assertTrue(part.press(h.getLevel()) == press, "A button should be able to be set to " + press.key);
        return part;
    }

    private static int signal(GameTestHelper h, BlockPos pos, Direction side) {
        BlockPos abs = h.absolutePos(pos);
        return h.getLevel().getBlockState(abs).getSignal(h.getLevel(), abs, side);
    }

    @GameTest(template = EMPTY)
    public static void gogglesReadOnlyTheHalfBeingLookedAt(GameTestHelper h) {
        DashboardBlockEntity desk = desk(h, new BlockPos(2, 1, 2));
        fit(h, desk, 0, DashPart.Type.ALARM, null);
        List<Component> fitted = new ArrayList<>();
        List<Component> empty = new ArrayList<>();
        List<Component> whole = new ArrayList<>();
        desk.readout(fitted, 0);
        desk.readout(empty, 1);
        desk.readout(whole, -1);
        h.assertTrue(fitted.size() == 3, "Looking at the alarm should name it and say it is not connected");
        h.assertTrue(empty.size() == 2 && !empty.get(1).getString().equals(fitted.get(1).getString()), "Looking at the empty half must not show the part on the other half");
        h.assertTrue(whole.size() == fitted.size(), "With no half in view every fitted part is listed");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void settingsChangeAPartAndSurviveSaving(GameTestHelper h) {
        BlockPos linkPos = new BlockPos(3, 2, 3);
        h.setBlock(linkPos.below(), Blocks.STONE);
        h.setBlock(linkPos, AllBlocks.REDSTONE_LINK.getDefaultState().setValue(RedstoneLinkBlock.FACING, Direction.UP).setValue(RedstoneLinkBlock.RECEIVER, true));
        DashboardBlockEntity desk = desk(h, new BlockPos(5, 1, 5));
        DashPart toggle = fit(h, desk, 0, DashPart.Type.SWITCH, linkPos);
        DashPart alarm = fit(h, desk, 1, DashPart.Type.ALARM, null);
        h.assertTrue(toggle.body == DashPart.ALLOY_BODY && toggle.plate(), "A new part should start with the reactor alloy body and a backplate");
        h.assertTrue(desk.edit(0, 1, 12, 0, 0, 0, 0, true, "FEED PUMP"), "A fitted part should take new settings");
        h.assertTrue(toggle.body == 1 && toggle.color == 12 && !toggle.plate() && toggle.inverted && toggle.label.equals("FEED PUMP"), "The settings should be applied");
        h.assertTrue(desk.edit(1, 99, 99, 0, 0, 0, 0, false, "x".repeat(80)) && alarm.body < DashPart.BODIES && alarm.color < DashPart.COLORS && alarm.label.length() == DashPart.LABEL_LENGTH,
                "Out of range settings should be brought into range");
        h.assertTrue(toggle.signal(0) == DashPart.MAX_LEVEL, "An inverted switch that is off should send a full signal");
        DashPart loaded = DashPart.load(toggle.save());
        h.assertTrue(loaded.body == 1 && loaded.color == 12 && loaded.inverted && loaded.label.equals("FEED PUMP") && !loaded.plate(), "Settings should survive saving and loading");
        h.assertTrue(!desk.edit(0, 0, 0, 0, 0, 0, 0, false, "") || desk.part(0) == toggle, "Editing keeps the same part");
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    desk.edit(0, 1, 12, 0, 0, 0, 0, true, "FEED PUMP");
                })
                .thenIdle(10)
                .thenExecute(() -> h.assertTrue(h.getBlockState(linkPos).getValue(RedstoneLinkBlock.POWERED), "An inverted switch that is off should power its link"))
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 900)
    public static void dashboardsRunTheLinkedPlant(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        TestVessels.loop(h, true);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 0);
        ControlRodDriveBlockEntity a = drive(h, new BlockPos(3, 6, 2));
        ControlRodDriveBlockEntity b = drive(h, new BlockPos(3, 6, 4));
        BlockPos turbinePos = new BlockPos(9, 1, 2);
        h.setBlock(turbinePos, TestVessels.turbine(Direction.EAST));
        SteamTurbineBlockEntity turbine = h.getBlockEntity(turbinePos);
        BlockPos scramPos = new BlockPos(8, 1, 10);
        BlockPos alarmPos = new BlockPos(12, 1, 10);
        BlockPos tripPos = new BlockPos(8, 1, 7);
        DashboardBlockEntity scramDesk = desk(h, scramPos);
        DashboardBlockEntity rodDesk = desk(h, new BlockPos(10, 1, 10));
        DashboardBlockEntity alarmDesk = desk(h, alarmPos);
        DashboardBlockEntity tripDesk = desk(h, tripPos);
        DashboardBlockEntity resetDesk = desk(h, new BlockPos(11, 1, 7));
        h.assertTrue(scramDesk.install(0, new DashPart(DashPart.Type.BUTTON)) && scramDesk.use(0, 0.5).endsWith("dashboard.unbound"), "An unbound button should say so");
        scramDesk.remove(0);
        h.assertTrue(scramDesk.install(0, new DashPart(DashPart.Type.BUTTON)) && !scramDesk.bind(0, h.absolutePos(scramPos.below())), "A button should refuse a block it cannot work");
        scramDesk.remove(0);
        button(h, scramDesk, 0, TestVessels.CONTROLLER, DashPart.Press.SCRAM);
        fit(h, scramDesk, 1, DashPart.Type.SWITCH, null);
        DashPart in = button(h, rodDesk, 0, TestVessels.CONTROLLER, DashPart.Press.ROD_IN);
        button(h, rodDesk, 1, TestVessels.CONTROLLER, DashPart.Press.ROD_OUT);
        DashPart alarm = fit(h, alarmDesk, 0, DashPart.Type.ALARM, TestVessels.CONTROLLER);
        button(h, tripDesk, 0, turbinePos, DashPart.Press.TURBINE_TRIP);
        button(h, resetDesk, 0, turbinePos, DashPart.Press.TURBINE_RESET);
        button(h, resetDesk, 1, TestVessels.CONTROLLER, DashPart.Press.REACTOR_RESET);
        int tripBit = 1 << PlantAlarms.Alarm.TRIP.ordinal();
        h.startSequence()
                .thenIdle(12)
                .thenExecute(() -> {
                    for (int i = 0; i < 101; i++) {
                        rodDesk.use(1, 0.5);
                    }
                    rodDesk.use(0, 0.5);
                    for (int bank = 0; bank < CMThermal.ROD_BANKS; bank++) {
                        h.assertTrue(Math.abs(c.bankTarget(bank) - 0.5) < 1e-9, "Rod steps should move every bank: " + c.bankTarget(bank));
                    }
                    h.assertTrue(in.pressed(h.getLevel().getGameTime()), "A pressed button should show as pressed");
                    tripDesk.use(0, 0.5);
                    h.assertTrue(turbine.overspeedTripped(), "The trip button should trip the turbine");
                    resetDesk.use(0, 0.5);
                    h.assertTrue(!turbine.overspeedTripped(), "The reset button should clear the turbine trip");
                    scramDesk.use(1, 0.5);
                })
                .thenIdle(270)
                .thenExecute(() -> {
                    h.assertTrue(!in.pressed(h.getLevel().getGameTime()), "A button should spring back");
                    h.assertTrue(Math.abs(a.insertion() - 0.5) < 1e-6 && Math.abs(b.insertion() - 0.5) < 1e-6, "Both drives should follow: " + a.insertion() + " " + b.insertion());
                    h.assertTrue(c.isReacting() && !c.isTripped(), "A switch beside the SCRAM button should not press it: " + c.status());
                    h.assertTrue(!alarm.sounding() && alarmDesk.activeAlarms() == 0 && signal(h, alarmPos, Direction.UP) == 0, "No alarms while running normally: " + alarm.alarms);
                    h.setBlock(scramPos.north(), Blocks.REDSTONE_BLOCK);
                    h.setBlock(tripPos.north(), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(c.isTripped() && c.tripReason().equals("manual"), "Redstone into the SCRAM desk should trip the reactor: " + c.status());
                    h.assertTrue(turbine.overspeedTripped(), "Redstone into the turbine desk should trip the turbine");
                    h.assertTrue(a.insertion() == 1 && b.insertion() == 1, "SCRAM should drive the rods in: " + a.insertion());
                    h.assertTrue((alarm.alarms & tripBit) != 0 && alarm.sounding(), "The trip alarm should light and sound: " + alarm.alarms);
                    h.assertTrue(signal(h, alarmPos, Direction.UP) == 15, "A sounding alarm should give a redstone signal");
                    BlockPos alarmAbs = h.absolutePos(alarmPos);
                    h.assertTrue(h.getLevel().getBlockState(alarmAbs).getAnalogOutputSignal(h.getLevel(), alarmAbs) == 1, "A comparator should count one active alarm");
                    alarmDesk.use(0, 0.5);
                    h.assertTrue(!alarm.sounding() && (alarm.acknowledged & tripBit) != 0 && signal(h, alarmPos, Direction.UP) == 0, "Acknowledging should silence the alarm and keep it lit");
                    String result = resetDesk.use(1, 0.5);
                    h.assertTrue(result.endsWith("result.reset") && !c.isTripped(), "Reset should clear the trip: " + result);
                    h.assertTrue(c.bankTarget(0) == 1.0, "Reset should leave the rods commanded in");
                })
                .thenIdle(25)
                .thenExecute(() -> h.assertTrue(!c.isTripped() && (alarm.alarms & tripBit) == 0, "A held signal should not press SCRAM again, and the trip alarm should clear"))
                .thenSucceed();
    }

    private static BlockPos port(GameTestHelper h, BlockPos pos, SignalInterfaceBlock.Function function) {
        h.setBlock(pos, CMBlocks.REACTOR_SIGNAL_INTERFACE.get().defaultBlockState().setValue(HorizontalBlock.FACING, Direction.EAST).setValue(SignalInterfaceBlock.FUNCTION, function));
        return pos;
    }

    private static void pulse(GameTestHelper h, BlockPos port) {
        h.setBlock(port.east(), Blocks.REDSTONE_BLOCK);
        h.setBlock(port.east(), Blocks.AIR);
    }

    @GameTest(template = LARGE, timeoutTicks = 900)
    public static void signalInterfacesWorkTheReactorByRedstone(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        TestVessels.loop(h, true);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 0);
        ControlRodDriveBlockEntity a = drive(h, new BlockPos(3, 6, 2));
        ControlRodDriveBlockEntity b = drive(h, new BlockPos(3, 6, 4));
        BlockPos scram = port(h, new BlockPos(5, 2, 2), SignalInterfaceBlock.Function.SCRAM);
        BlockPos reset = port(h, new BlockPos(5, 2, 4), SignalInterfaceBlock.Function.RESET);
        BlockPos rodsIn = port(h, new BlockPos(5, 4, 2), SignalInterfaceBlock.Function.RODS_IN);
        BlockPos rodsOut = port(h, new BlockPos(5, 4, 4), SignalInterfaceBlock.Function.RODS_OUT);
        BlockPos alarm = port(h, new BlockPos(5, 3, 3), SignalInterfaceBlock.Function.ALARM);
        int tripBit = 1 << PlantAlarms.Alarm.TRIP.ordinal();
        h.startSequence()
                .thenIdle(12)
                .thenExecute(() -> {
                    h.assertTrue(SignalInterfaceBlock.reactor(h.getLevel(), h.absolutePos(scram)) == c, "An interface in the vessel wall should find its reactor");
                    for (int i = 0; i < 101; i++) {
                        pulse(h, rodsOut);
                    }
                    pulse(h, rodsIn);
                    for (int bank = 0; bank < CMThermal.ROD_BANKS; bank++) {
                        h.assertTrue(Math.abs(c.bankTarget(bank) - 0.5) < 1e-9, "Each pulse should step every bank 0.5%: " + c.bankTarget(bank));
                    }
                    h.setBlock(rodsOut.east(), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(270)
                .thenExecute(() -> {
                    h.assertTrue(Math.abs(c.bankTarget(0) - 0.495) < 1e-9, "A held signal should step the rods once, not keep stepping: " + c.bankTarget(0));
                    h.assertTrue(Math.abs(a.insertion() - 0.495) < 1e-6 && Math.abs(b.insertion() - 0.495) < 1e-6, "Both drives should follow: " + a.insertion() + " " + b.insertion());
                    h.assertTrue(c.isReacting() && !c.isTripped(), "The reactor should be running: " + c.status());
                    h.assertTrue(signal(h, alarm, Direction.EAST) == 0, "The alarm interface should be quiet while nothing is wrong");
                    pulse(h, scram);
                    h.assertTrue(c.isTripped() && c.tripReason().equals("manual"), "A pulse into the SCRAM interface should trip the reactor: " + c.status());
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(a.insertion() == 1 && b.insertion() == 1, "SCRAM should drive the rods in: " + a.insertion());
                    h.assertTrue((PlantAlarms.evaluate(c) & tripBit) != 0 && signal(h, alarm, Direction.EAST) == 15, "The alarm interface should give a signal while the trip alarm is active");
                    pulse(h, reset);
                    h.assertTrue(!c.isTripped() && c.bankTarget(0) == 1.0, "A pulse into the reset interface should clear the trip and leave the rods commanded in");
                })
                .thenIdle(25)
                .thenExecute(() -> h.assertTrue(signal(h, alarm, Direction.EAST) == 0, "The alarm interface should drop its signal once the alarm clears"))
                .thenSucceed();
    }

    private static InteractionResult mount(GameTestHelper h, Player player, DashPart.Type type, BlockPos support, Direction face, double x, double y, double z) {
        ItemStack stack = new ItemStack(CMItems.DASHBOARD_PARTS.get(type).get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos at = h.absolutePos(support);
        return stack.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, stack, new BlockHitResult(new Vec3(at.getX() + x, at.getY() + y, at.getZ() + z), face, at, false)));
    }

    @GameTest(template = EMPTY)
    public static void partsMountOnAnySurfaceUpToFourToABlock(GameTestHelper h) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0);
        BlockPos floor = new BlockPos(1, 1, 1);
        h.setBlock(floor, Blocks.STONE);
        mount(h, player, DashPart.Type.BUTTON, floor, Direction.UP, 0.2, 1, 0.2);
        BlockPos onFloor = floor.above();
        h.assertTrue(h.getBlockState(onFloor).is(CMBlocks.PART_PANEL.get()), "A part used on a floor should mount there: " + h.getBlockState(onFloor));
        DashboardBlockEntity panel = h.getBlockEntity(onFloor);
        h.assertTrue(!panel.centered() && panel.slots() == 4 && !panel.empty(), "A part placed off centre should take one of four places");
        mount(h, player, DashPart.Type.SWITCH, floor, Direction.UP, 0.8, 1, 0.8);
        mount(h, player, DashPart.Type.LEVER, floor, Direction.UP, 0.8, 1, 0.8);
        mount(h, player, DashPart.Type.GAUGE, floor, Direction.UP, 0.8, 1, 0.8);
        int fitted = 0;
        for (int slot = 0; slot < panel.slots(); slot++) {
            fitted += panel.part(slot) == null ? 0 : 1;
        }
        h.assertTrue(fitted == 4, "Four parts should share one block: " + fitted);
        h.assertTrue(mount(h, player, DashPart.Type.ALARM, floor, Direction.UP, 0.5, 1, 0.5) == InteractionResult.FAIL, "A fifth part should be refused");

        BlockPos pad = new BlockPos(4, 1, 1);
        h.setBlock(pad, Blocks.STONE);
        mount(h, player, DashPart.Type.GAUGE, pad, Direction.UP, 0.5, 1, 0.5);
        DashboardBlockEntity single = h.getBlockEntity(pad.above());
        h.assertTrue(single.centered() && single.part(0) != null, "A part placed on the middle of a face should sit centred");
        Vec3 middle = Vec3.atLowerCornerOf(h.absolutePos(pad.above())).add(0.5, 0.05, 0.5);
        single.remove(single.slotAt(middle));
        h.assertTrue(single.empty(), "Taking the only part should leave nothing behind");

        BlockPos wall = new BlockPos(1, 2, 4);
        h.setBlock(wall, Blocks.STONE);
        mount(h, player, DashPart.Type.LEVER, wall, Direction.NORTH, 0.3, 0.3, 0);
        BlockPos onWall = wall.north();
        BlockState hung = h.getBlockState(onWall);
        h.assertTrue(hung.is(CMBlocks.PART_PANEL.get()) && hung.getValue(PartPanelBlock.FACE) == AttachFace.WALL && hung.getValue(PartPanelBlock.FACING) == Direction.NORTH,
                "A part used on a wall should hang on it facing out: " + hung);
        var bounds = hung.getShape(h.getLevel(), h.absolutePos(onWall)).bounds();
        h.assertTrue(Math.abs(bounds.maxZ - 1) < 1e-6 && bounds.minZ > 0.75 && bounds.getXsize() < 0.5 && bounds.getYsize() < 0.5, "A wall part's outline should be one small box against the wall: " + bounds);

        for (AttachFace face : AttachFace.values()) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                BlockState state = CMBlocks.PART_PANEL.get().defaultBlockState().setValue(PartPanelBlock.FACE, face).setValue(PartPanelBlock.FACING, facing);
                for (int slot = 0; slot < PartPanelBlock.SLOTS; slot++) {
                    Vec3 local = new Vec3(PartPanelBlock.cellX(slot, false) + 0.25, 0.05, PartPanelBlock.cellZ(slot, false) + 0.25);
                    Vec3 in = PartPanelBlock.toBlock(state, local);
                    h.assertTrue(PartPanelBlock.slotAt(state, BlockPos.ZERO, in, false) == slot && PartPanelBlock.toLocal(state, in).distanceTo(local) < 1e-6,
                            "Each place should be found again from a point in it: " + state + " " + slot);
                    h.assertTrue(in.x > 0 && in.x < 1 && in.y > -1e-6 && in.y < 1 + 1e-6 && in.z > 0 && in.z < 1, "Parts should stay inside their block: " + state + " " + in);
                }
            }
        }

        h.setBlock(wall, Blocks.AIR);
        h.runAfterDelay(5, () -> {
            h.assertTrue(h.getBlockState(onWall).isAir(), "Parts should come off when the block behind them goes");
            h.succeed();
        });
    }
}
