package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.control.AnnunciatorBlockEntity;
import com.criticalmass.content.control.ControlButtonBlockEntity;
import com.criticalmass.content.control.GaugeBlockEntity;
import com.criticalmass.content.control.PlantChannel;
import com.criticalmass.content.kinetics.ControlRodDriveBlockEntity;
import com.criticalmass.content.kinetics.GeneratorBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class ControlTests {
    private static final String EMPTY = "empty";
    private static final String LARGE = "empty_large";

    private ControlTests() {
    }

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> T panel(GameTestHelper h, Block block, BlockPos pos) {
        h.setBlock(pos, block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        return h.getBlockEntity(pos);
    }

    private static ControlButtonBlockEntity button(GameTestHelper h, BlockPos pos, BlockPos target, ControlButtonBlockEntity.Function function) {
        ControlButtonBlockEntity b = panel(h, CMBlocks.CONTROL_BUTTON.get(), pos);
        b.setFunction(function);
        h.assertTrue(b.bind(h.absolutePos(target)), "Button should bind to " + target);
        return b;
    }

    private static ControlRodDriveBlockEntity drive(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, CMBlocks.CONTROL_ROD_DRIVE.get().defaultBlockState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.WEST));
        h.setBlock(pos.east(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.WEST));
        CreativeMotorBlockEntity motor = h.getBlockEntity(pos.east());
        motor.generatedSpeed.setValue(64);
        return h.getBlockEntity(pos);
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void gaugeReadsItsBoundTargetAndAlarms(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 6000 * (300 - CMThermal.AMBIENT) * CMThermal.HEAT_CAPACITY);
        GaugeBlockEntity gauge = panel(h, CMBlocks.GAUGE.get(), new BlockPos(9, 1, 9));
        GaugeBlockEntity loose = panel(h, CMBlocks.GAUGE.get(), new BlockPos(11, 1, 9));
        h.setBlock(new BlockPos(9, 1, 11), Blocks.DIRT);
        h.assertTrue(!gauge.bind(h.absolutePos(new BlockPos(9, 1, 11))), "A gauge should refuse a block that has nothing to read");
        h.assertTrue(gauge.bind(h.absolutePos(TestVessels.CONTROLLER)), "A gauge should bind to a reactor controller");
        gauge.setChannel(PlantChannel.COOLANT_TEMPERATURE);
        gauge.setSetpoint(50, false);
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    double expected = (300 - CMThermal.AMBIENT) / (PlantChannel.COOLANT_SCALE_MAX - CMThermal.AMBIENT);
                    h.assertTrue(gauge.valid() && Math.abs(gauge.value() - 300) < 1, "Gauge should read the vessel coolant: " + gauge.value());
                    h.assertTrue(gauge.comparator() == Math.round(15 * (float) expected), "Comparator should follow the scale: " + gauge.comparator());
                    h.assertTrue(gauge.alarm() && h.getBlockState(new BlockPos(9, 1, 9)).getSignal(h.getLevel(), h.absolutePos(new BlockPos(9, 1, 9)), Direction.UP) == 15, "High alarm should give a redstone signal");
                    h.assertTrue(!loose.valid() && loose.comparator() == 0 && !loose.alarm(), "An unbound gauge should read nothing");
                    gauge.setSetpoint(50, true);
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    h.assertTrue(!gauge.alarm(), "A low alarm should be quiet above its setpoint");
                    gauge.setChannel(PlantChannel.LEVEL);
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    h.assertTrue(Math.abs(gauge.value() - 100.0 * c.coolant().amount() / c.coolant().capacity()) < 0.5, "Level channel should read percent full: " + gauge.value());
                    gauge.setChannel(PlantChannel.SPEED);
                })
                .thenIdle(10)
                .thenExecute(() -> h.assertTrue(!gauge.valid(), "A reactor has no speed reading"))
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 900)
    public static void buttonsRunTheReactor(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        TestVessels.loop(h, true);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 0);
        ControlRodDriveBlockEntity a = drive(h, new BlockPos(3, 6, 2));
        ControlRodDriveBlockEntity b = drive(h, new BlockPos(3, 6, 4));
        ControlButtonBlockEntity out = button(h, new BlockPos(9, 1, 12), TestVessels.CONTROLLER, ControlButtonBlockEntity.Function.BANK_A_OUT);
        ControlButtonBlockEntity scram = button(h, new BlockPos(11, 1, 12), TestVessels.CONTROLLER, ControlButtonBlockEntity.Function.SCRAM);
        ControlButtonBlockEntity reset = button(h, new BlockPos(13, 1, 12), TestVessels.CONTROLLER, ControlButtonBlockEntity.Function.RESET);
        AnnunciatorBlockEntity alarms = panel(h, CMBlocks.ANNUNCIATOR.get(), new BlockPos(9, 3, 12));
        h.assertTrue(alarms.bind(h.absolutePos(TestVessels.CONTROLLER)), "Annunciator should bind to a controller");
        h.assertTrue(!alarms.bind(h.absolutePos(new BlockPos(3, 6, 2))), "Annunciator should only take a controller");
        ControlButtonBlockEntity ack = button(h, new BlockPos(11, 3, 12), new BlockPos(9, 3, 12), ControlButtonBlockEntity.Function.ACKNOWLEDGE);
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    for (int i = 0; i < 10; i++) {
                        out.press();
                    }
                    h.assertTrue(Math.abs(c.bankTarget(0) - 0.5) < 1e-9, "Ten presses should ask for half insertion: " + c.bankTarget(0));
                })
                .thenIdle(150)
                .thenExecute(() -> {
                    h.assertTrue(Math.abs(a.insertion() - 0.5) < 1e-6 && Math.abs(b.insertion() - 0.5) < 1e-6, "Both bank A drives should follow: " + a.insertion() + " " + b.insertion());
                    h.assertTrue(c.isReacting(), "Reactor should run on withdrawn rods: " + c.status());
                    h.assertTrue(alarms.activeCount() == 0 && !alarms.sounding(), "No alarms while running normally");
                    h.setBlock(new BlockPos(11, 2, 12), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(c.isTripped() && c.tripReason().equals("manual"), "A redstone pulse should press the SCRAM button: " + c.status());
                    h.assertTrue(a.insertion() == 1 && b.insertion() == 1, "SCRAM should drive the rods in: " + a.insertion());
                    h.assertTrue(alarms.active(AnnunciatorBlockEntity.Alarm.TRIP) && alarms.sounding(), "Trip alarm should sound");
                    BlockPos p = new BlockPos(9, 3, 12);
                    h.assertTrue(h.getBlockState(p).getSignal(h.getLevel(), h.absolutePos(p), Direction.UP) == 15, "Sounding alarm should give a redstone signal");
                    ack.press();
                    h.assertTrue(!alarms.sounding() && alarms.acknowledged(AnnunciatorBlockEntity.Alarm.TRIP), "Acknowledging should silence the alarm but keep it lit");
                    h.assertTrue(h.getBlockState(p).getSignal(h.getLevel(), h.absolutePos(p), Direction.UP) == 0, "Acknowledged alarm should drop the signal");
                    String result = reset.press();
                    h.assertTrue(result.endsWith("result.reset") && !c.isTripped(), "Reset should clear the trip: " + result);
                    h.assertTrue(c.bankTarget(0) == 1.0, "Reset should leave the rods commanded in");
                })
                .thenIdle(20)
                .thenExecute(() -> h.assertTrue(!alarms.active(AnnunciatorBlockEntity.Alarm.TRIP), "Trip alarm should clear after the reset"))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void buttonsTripTheTurbineAndOpenTheBreaker(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, CMBlocks.STEAM_TURBINE.get().defaultBlockState().setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, Direction.Axis.X));
        h.setBlock(pos.east(), CMBlocks.GENERATOR.get().defaultBlockState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
        SteamTurbineBlockEntity turbine = h.getBlockEntity(pos);
        GeneratorBlockEntity generator = h.getBlockEntity(pos.east());
        ControlButtonBlockEntity trip = button(h, new BlockPos(2, 1, 6), pos, ControlButtonBlockEntity.Function.TURBINE_TRIP);
        ControlButtonBlockEntity reset = button(h, new BlockPos(4, 1, 6), pos, ControlButtonBlockEntity.Function.TURBINE_RESET);
        ControlButtonBlockEntity open = button(h, new BlockPos(6, 1, 6), pos.east(), ControlButtonBlockEntity.Function.BREAKER_OPEN);
        ControlButtonBlockEntity wrong = button(h, new BlockPos(2, 3, 6), pos, ControlButtonBlockEntity.Function.SCRAM);
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    trip.press();
                    h.assertTrue(turbine.overspeedTripped(), "Trip button should trip the turbine");
                    reset.press();
                    h.assertTrue(!turbine.overspeedTripped(), "Reset button should clear the turbine trip");
                    open.press();
                    h.assertTrue(wrong.press().endsWith("wrong_target"), "A button should say when its target cannot do the job");
                })
                .thenIdle(15)
                .thenExecute(() -> h.assertTrue(generator.breakerOpen(), "Breaker button should open the generator breaker"))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void linkerBindsAPanelToAMachine(GameTestHelper h) {
        BlockPos tank = new BlockPos(2, 1, 2);
        BlockPos gaugePos = new BlockPos(5, 1, 2);
        h.setBlock(tank, CMBlocks.EMERGENCY_COOLANT_TANK.get());
        GaugeBlockEntity gauge = panel(h, CMBlocks.GAUGE.get(), gaugePos);
        Player player = h.makeMockPlayer(GameType.CREATIVE);
        ItemStack linker = new ItemStack(CMItems.PLANT_LINKER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, linker);
        BlockPos gaugeAbs = h.absolutePos(gaugePos);
        BlockPos tankAbs = h.absolutePos(tank);
        linker.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, linker, new BlockHitResult(Vec3.atCenterOf(gaugeAbs), Direction.NORTH, gaugeAbs, false)));
        h.assertTrue(gauge.target() == null, "An empty linker should bind nothing");
        linker.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, linker, new BlockHitResult(Vec3.atCenterOf(tankAbs), Direction.UP, tankAbs, false)));
        linker.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, linker, new BlockHitResult(Vec3.atCenterOf(gaugeAbs), Direction.NORTH, gaugeAbs, false)));
        h.assertTrue(tankAbs.equals(gauge.target()), "Linker should bind the gauge to the remembered tank: " + gauge.target());
        h.succeed();
    }
}
