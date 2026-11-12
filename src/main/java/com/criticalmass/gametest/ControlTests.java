package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.control.AnnunciatorBlockEntity;
import com.criticalmass.content.control.ControlButtonBlockEntity;
import com.criticalmass.content.control.PlantDisplaySources;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import com.criticalmass.content.control.PlantChannel;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
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
    public static void displayLinkShowsPlantReadings(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 6000 * (300 - CMThermal.AMBIENT) * CMThermal.HEAT_CAPACITY);
        BlockPos linkPos = TestVessels.CONTROLLER.west();
        BlockPos signPos = new BlockPos(0, 2, 0);
        h.setBlock(signPos.below(), Blocks.STONE);
        h.setBlock(signPos, Blocks.OAK_SIGN);
        h.setBlock(linkPos, AllBlocks.DISPLAY_LINK.getDefaultState().setValue(DisplayLinkBlock.FACING, Direction.WEST));
        DisplayLinkBlockEntity link = h.getBlockEntity(linkPos);
        SignBlockEntity sign = h.getBlockEntity(signPos);
        link.target(h.absolutePos(signPos));
        h.assertTrue(DisplaySource.getAll(h.getLevel(), h.absolutePos(TestVessels.CONTROLLER)).contains(PlantDisplaySources.STATUS.get()), "The controller should offer its status as a display source");
        link.activeSource = PlantDisplaySources.CHANNELS.get(PlantChannel.COOLANT_TEMPERATURE).get();
        link.updateGatheredData();
        String reading = sign.getFrontText().getMessage(0, false).getString();
        h.assertTrue(reading.equals("300 °C"), "The sign should show the coolant temperature: " + reading);
        link.activeSource = PlantDisplaySources.STATUS.get();
        link.updateGatheredData();
        String first = sign.getFrontText().getMessage(0, false).getString();
        String second = sign.getFrontText().getMessage(1, false).getString();
        h.assertTrue(!first.isEmpty() && second.endsWith("°C"), "The summary should lead with the status and then the core temperature: " + first + " / " + second);
        link.activeSource = PlantDisplaySources.CHANNELS.get(PlantChannel.SPEED).get();
        link.updateGatheredData();
        h.assertTrue(link.activeSource == null, "A reactor has no speed reading to offer");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void controllerComparatorFollowsTheHottestFuel(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        double halfway = (CMThermal.AMBIENT + CMThermal.TRIP_TEMPERATURE) / 2;
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    h.assertTrue(c.comparator() == 0, "A cold core should give no comparator signal: " + c.comparator());
                    c.core().setColumnHeat(2, 0, (halfway - CMThermal.AMBIENT) * CMThermal.FUEL_HEAT_CAPACITY * 3);
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    int signal = h.getBlockState(TestVessels.CONTROLLER).getAnalogOutputSignal(h.getLevel(), h.absolutePos(TestVessels.CONTROLLER));
                    h.assertTrue(signal == 8 && c.comparator() == 8, "Fuel halfway to the trip temperature should read 8: " + signal + " at " + c.core().maxTemperature());
                })
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
                .thenIdle(270)
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

    private static double cooledColumn(ReactorControllerBlockEntity c) {
        for (com.criticalmass.content.thermal.ReactorCore.FuelColumn f : c.core().columns()) {
            if (f.dx() == 2 && f.dz() == 0) {
                return f.temperature();
            }
        }
        return Double.NaN;
    }

    @GameTest(template = LARGE, timeoutTicks = 3000)
    public static void plantStartsRunsGeneratesAndShutsDown(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        TestVessels.plant(h);
        h.setBlock(TestVessels.MOTOR, Blocks.AIR);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        com.criticalmass.content.thermal.HeatExchangerBlockEntity exchanger = h.getBlockEntity(TestVessels.EXCHANGER);
        SteamTurbineBlockEntity turbine = h.getBlockEntity(TestVessels.PLANT_TURBINE);
        c.coolant().set(CoolantLoopTests.coolant(6000), 6000 * 230.0);
        exchanger.secondary().set(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), 1000 * CMThermal.SATURATION_ENERGY);
        turbine.coolant().set(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), 1000 * CMThermal.SATURATION_ENERGY);
        BlockPos generatorPos = TestVessels.PLANT_TURBINE.west();
        h.setBlock(generatorPos, CMBlocks.GENERATOR.get().defaultBlockState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.WEST));
        GeneratorBlockEntity generator = h.getBlockEntity(generatorPos);
        generator.setSetting(64);
        net.neoforged.neoforge.energy.IEnergyStorage out = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, h.absolutePos(generatorPos), Direction.UP);
        h.onEachTick(() -> out.extractEnergy(100000, false));
        ControlRodDriveBlockEntity a = drive(h, new BlockPos(3, 6, 2));
        ControlRodDriveBlockEntity b = drive(h, new BlockPos(3, 6, 4));
        ControlButtonBlockEntity scram = button(h, new BlockPos(12, 1, 12), TestVessels.CONTROLLER, ControlButtonBlockEntity.Function.SCRAM);
        c.setBankTarget(0, 0.0);
        double[] seen = new double[4];
        h.startSequence()
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(!c.isReacting() && c.status().endsWith("blocked_flow") && a.insertion() == 1, "A plant with no primary flow should refuse to start: " + c.status());
                    h.setBlock(TestVessels.MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
                })
                .thenIdle(30)
                .thenExecute(() -> {
                    h.assertTrue(a.insertion() < 1 && a.insertion() > 0, "Rods should be on their way out, not there yet: " + a.insertion());
                    seen[0] = c.core().lastOutput();
                })
                .thenIdle(520)
                .thenExecute(() -> {
                    h.assertTrue(c.isReacting() && a.insertion() == 0 && b.insertion() == 0, "Rods should be out with the reactor armed: " + c.status());
                    h.assertTrue(c.core().lastOutput() == 0 && c.core().neutronPower() < 1e-4, "A borated core should still be at source level: " + c.core().neutronPower());
                    TestVessels.atPower(c, 0.5);
                })
                .thenIdle(900)
                .thenExecute(() -> {
                    h.assertTrue(c.isReacting() && !c.isTripped(), "The reactor should hold power without tripping: " + c.status() + " " + c.tripReason());
                    h.assertTrue(c.core().lastOutput() > 0.05 * c.core().ratedOutput() && c.core().neutronPower() < CMThermal.HIGH_FLUX_TRIP, "The core should be making heat: " + c.core().lastOutput());
                    h.assertTrue(Math.abs(c.core().reactivity()) < 2e-3, "A core holding power should sit near critical: " + c.core().reactivity());
                    h.assertTrue(c.core().maxTemperature() > c.coolant().temperature() + 5, "Fuel at power should run hotter than its coolant: " + c.core().maxTemperature() + " over " + c.coolant().temperature());
                    h.assertTrue(exchanger.exchanged() > 0 && turbine.heatExtracted() > 0, "Heat should cross the exchanger and reach the turbine");
                    h.assertTrue(generator.generated() > 0 && generator.outputRate() > 1, "The generator should be making Forge Energy: " + generator.outputRate());
                    double perSu = com.criticalmass.registry.CMConfig.get(com.criticalmass.registry.CMConfig.FE_PER_SU) * com.criticalmass.registry.CMConfig.get(com.criticalmass.registry.CMConfig.GENERATOR_EFFICIENCY);
                    h.assertTrue(Math.abs(generator.generated() - generator.shaftWork() * perSu) <= 2, "Energy made should match shaft work");
                    h.assertTrue(generator.shaftWork() <= turbine.heatExtracted() * CMThermal.SU_PER_HEAT + 1e-6, "Shaft work cannot exceed the steam energy the turbine took: " + generator.shaftWork() + " vs " + turbine.heatExtracted() * CMThermal.SU_PER_HEAT);
                    scram.press();
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(!c.isReacting() && c.isTripped() && a.insertion() == 1 && b.insertion() == 1, "SCRAM should stop the reaction and insert the rods");
                    h.assertTrue(c.core().neutronPower() < 0.1 && c.core().reactivity() < -0.05 && c.core().residualPower() > 0, "Fission should collapse while decay heat remains: " + c.core().neutronPower());
                    CirculationPumpBlockEntity pump = h.getBlockEntity(TestVessels.PUMP);
                    h.assertTrue(pump.flow() > 0, "Cooling should keep running after the trip: " + pump.status());
                    seen[2] = cooledColumn(c);
                    seen[3] = c.core().residualPower();
                })
                .thenIdle(600)
                .thenExecute(() -> {
                    h.assertTrue(cooledColumn(c) < seen[2], "With cooling kept up the cooled fuel should come down: " + seen[2] + " to " + cooledColumn(c));
                    h.assertTrue(c.core().residualPower() < seen[3], "Decay heat should fade");
                    h.assertTrue(c.core().coriumMass() == 0 && c.damagedBlocks() == 0, "A cooled shutdown should leave the core undamaged");
                })
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
        BlockPos buttonPos = new BlockPos(5, 1, 2);
        h.setBlock(tank, CMBlocks.EMERGENCY_COOLANT_TANK.get());
        ControlButtonBlockEntity bound = panel(h, CMBlocks.CONTROL_BUTTON.get(), buttonPos);
        Player player = h.makeMockPlayer(GameType.CREATIVE);
        ItemStack linker = new ItemStack(CMItems.PLANT_LINKER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, linker);
        BlockPos buttonAbs = h.absolutePos(buttonPos);
        BlockPos tankAbs = h.absolutePos(tank);
        linker.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, linker, new BlockHitResult(Vec3.atCenterOf(buttonAbs), Direction.NORTH, buttonAbs, false)));
        h.assertTrue(bound.target() == null, "An empty linker should bind nothing");
        linker.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, linker, new BlockHitResult(Vec3.atCenterOf(tankAbs), Direction.UP, tankAbs, false)));
        linker.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, linker, new BlockHitResult(Vec3.atCenterOf(buttonAbs), Direction.NORTH, buttonAbs, false)));
        h.assertTrue(tankAbs.equals(bound.target()), "Linker should bind the button to the remembered tank: " + bound.target());
        h.succeed();
    }
}
