package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.control.PlantAlarms;
import com.criticalmass.content.control.PlantChannel;
import com.criticalmass.content.control.PlantDisplaySources;
import com.criticalmass.content.control.ScreenBlock;
import com.criticalmass.content.control.ScreenBlockEntity;
import com.criticalmass.content.power.Power;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class ScreenTests {
    private static final String LARGE = "empty_large";

    private ScreenTests() {
    }

    @GameTest(template = LARGE, timeoutTicks = 700)
    public static void screenWallTurnsFromACogwheelAndShowsLinkedText(GameTestHelper h) {
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
                    h.setBlock(corner.west(), AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Direction.Axis.Z));
                    h.setBlock(corner.west().south(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.NORTH));
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    for (ScreenBlockEntity s : screens) {
                        h.assertTrue(s.powered() && s.getSpeed() != 0, "A cogwheel meshed with one screen should turn and power the whole wall");
                    }
                    h.assertTrue(screens.get(0).getSpeed() == -screens.get(1).getSpeed(), "Neighbouring screens should mesh like cogwheels and turn opposite ways");
                    h.setBlock(corner.west().south(), Blocks.AIR);
                })
                .thenIdle(ScreenBlockEntity.CAPACITY + 20)
                .thenExecute(() -> {
                    for (ScreenBlockEntity s : screens) {
                        h.assertTrue(!s.powered(), "The wall should go dark a while after the cogwheel stops");
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
        h.setBlock(turbinePos, TestVessels.turbine(Direction.EAST));
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

    private static ScreenBlockEntity wall(GameTestHelper h, BlockPos corner, int width, int height, List<ScreenBlockEntity> all) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                BlockPos p = corner.offset(x, y, 0);
                h.setBlock(p, CMBlocks.PLANT_SCREEN.get().defaultBlockState().setValue(ScreenBlock.FACING, Direction.NORTH));
                all.add(h.getBlockEntity(p));
            }
        }
        return h.getBlockEntity(corner.offset(width - 1, height - 1, 0));
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void aSmallScreenWallShowsWhateverMachineItIsLinkedTo(GameTestHelper h) {
        BlockPos driverPos = new BlockPos(2, 2, 2);
        BlockPos generatorPos = driverPos.east();
        BlockPos busPos = new BlockPos(2, 2, 5);
        h.setBlock(driverPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.EAST));
        h.setBlock(generatorPos, CMBlocks.GENERATOR.get().defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
        h.setBlock(busPos, CMBlocks.BUS_BAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        CreativeMotorBlockEntity driver = h.getBlockEntity(driverPos);
        driver.generatedSpeed.setValue((int) Power.SYNC_RPM);
        List<ScreenBlockEntity> all = new ArrayList<>();
        ScreenBlockEntity main = wall(h, new BlockPos(6, 2, 8), ScreenBlockEntity.MIN_WIDTH, ScreenBlockEntity.MIN_HEIGHT, all);
        ScreenBlockEntity narrow = wall(h, new BlockPos(11, 2, 8), 2, 1, all);
        h.onEachTick(() -> all.forEach(ScreenBlockEntity::charge));
        h.startSequence()
                .thenIdle(15)
                .thenExecute(() -> {
                    h.assertTrue(main.width() == ScreenBlockEntity.MIN_WIDTH && main.height() == ScreenBlockEntity.MIN_HEIGHT && main.bigEnough(), "Three by two should be enough for a display");
                    h.assertTrue(main.link(h.absolutePos(generatorPos)) && narrow.link(h.absolutePos(generatorPos)), "A wall should link to a generator");
                    h.assertTrue(narrow.linked() && !narrow.bigEnough(), "Two by one should link and report that it is too small");
                })
                .thenIdle(70)
                .thenExecute(() -> {
                    h.assertTrue(main.samples() >= 3 && !main.board() && !main.fullBoard(), "A wall linked to a generator should sample it as a page, not the reactor board: " + main.samples());
                    h.assertTrue(main.title().equals(CMBlocks.GENERATOR.get().getDescriptionId()) && main.status().endsWith("generator.running"), "The page should carry the machine's name and state: " + main.status());
                    h.assertTrue(main.trends().equals(List.of(PlantChannel.OUTPUT, PlantChannel.DEMAND, PlantChannel.FREQUENCY, PlantChannel.SYNC)), "The page should graph what a gauge could read there: " + main.trends());
                    h.assertTrue(main.rows().equals(main.trends()) && main.values().size() == 4 && main.values().get(2).startsWith("50"), "It should list the same readings: " + main.values());
                    CompoundTag sent = main.getUpdateTag(h.getLevel().registryAccess());
                    h.assertTrue(sent.getFloat("Low2") == 45f && sent.getFloat("Scale2") == 55f, "Frequency should be graphed on its own window, not from zero: " + sent.getFloat("Low2") + " to " + sent.getFloat("Scale2"));
                    int middle = sent.getByteArray("Trend2")[0] & 255;
                    h.assertTrue(middle > 120 && middle < 135, "50 Hz should plot in the middle of 45 to 55: " + middle);
                    h.assertTrue(main.link(h.absolutePos(busPos)), "The same wall should take another machine");
                    h.assertTrue(main.samples() == 0, "A new machine should start the graphs again");
                })
                .thenIdle(50)
                .thenExecute(() -> {
                    h.assertTrue(main.title().equals(CMBlocks.BUS_BAR.get().getDescriptionId()) && main.status().endsWith("bus.dead"), "The page should follow the new machine: " + main.title() + " " + main.status());
                    h.assertTrue(main.trends().equals(List.of(PlantChannel.DEMAND, PlantChannel.FREQUENCY, PlantChannel.VOLTAGE)) && main.samples() >= 2, "A bus should graph its load, frequency and voltage: " + main.trends());
                    main.unlink();
                    h.assertTrue(!main.linked() && main.trends().isEmpty() && main.title().isEmpty(), "Unlinking should clear the page");
                })
                .thenSucceed();
    }
}
