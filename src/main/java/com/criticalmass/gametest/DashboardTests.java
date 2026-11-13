package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.control.DashPart;
import com.criticalmass.content.control.DashboardBlock;
import com.criticalmass.content.control.DashboardBlockEntity;
import com.criticalmass.content.control.PlantAlarms;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMComponents;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import net.minecraft.core.BlockPos;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class DashboardTests {
    private static final String EMPTY = "empty";

    private DashboardTests() {
    }

    private static DashboardBlockEntity desk(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, CMBlocks.DASHBOARD.get());
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
        DashboardBlock block = CMBlocks.DASHBOARD.get();
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
    public static void dyeRecoloursAJoinedRunAndTheDropKeepsIt(GameTestHelper h) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0);
        player.setXRot(0);
        DashboardBlock block = CMBlocks.DASHBOARD.get();
        for (int x = 1; x <= 3; x++) {
            BlockPos floor = new BlockPos(x, 1, 3);
            h.setBlock(floor, Blocks.STONE);
            BlockPos abs = h.absolutePos(floor);
            BlockState state = block.getStateForPlacement(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(block),
                    new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
            h.setBlock(floor.above(), state);
        }
        BlockPos alone = new BlockPos(5, 1, 3);
        h.setBlock(alone, block);
        DashboardBlockEntity fitted = h.getBlockEntity(new BlockPos(1, 2, 3));
        fitted.install(0, new DashPart(DashPart.Type.SWITCH));
        BlockPos middle = h.absolutePos(new BlockPos(2, 2, 3));
        ItemStack dye = new ItemStack(Items.RED_DYE, 2);
        h.getLevel().getBlockState(middle).useItemOn(dye, h.getLevel(), player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(middle), Direction.UP, middle, false));
        for (int x = 1; x <= 3; x++) {
            h.assertTrue(h.getBlockState(new BlockPos(x, 2, 3)).getValue(DashboardBlock.COLOR) == DyeColor.RED, "One dye should recolour the whole run, desk " + x);
        }
        h.assertTrue(dye.getCount() == 1, "One dye should be used for the run: " + dye.getCount() + " left");
        h.assertTrue(h.getBlockState(alone).getValue(DashboardBlock.COLOR) == DashboardBlock.DEFAULT_COLOR, "A desk outside the run should keep its colour");
        DashboardBlockEntity kept = h.getBlockEntity(new BlockPos(1, 2, 3));
        h.assertTrue(kept.part(0) != null && kept.part(0).type == DashPart.Type.SWITCH, "Dyeing should not disturb the parts on a desk");
        h.assertTrue(h.getBlockState(new BlockPos(2, 2, 3)).getValue(DashboardBlock.JOIN_CW) && h.getBlockState(new BlockPos(2, 2, 3)).getValue(DashboardBlock.JOIN_CCW),
                "Dyeing should not break the run apart");
        List<ItemStack> dyed = Block.getDrops(h.getBlockState(new BlockPos(3, 2, 3)), h.getLevel(), h.absolutePos(new BlockPos(3, 2, 3)), null);
        BlockItemStateProperties stored = dyed.isEmpty() ? null : dyed.get(0).get(DataComponents.BLOCK_STATE);
        h.assertTrue(stored != null && "red".equals(stored.properties().get("color")), "A dyed desk should drop as a dyed desk");
        List<ItemStack> plain = Block.getDrops(h.getBlockState(alone), h.getLevel(), h.absolutePos(alone), null);
        h.assertTrue(plain.size() == 1 && !plain.get(0).has(DataComponents.BLOCK_STATE), "A desk in the standard colour should drop as a plain desk");
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
}
