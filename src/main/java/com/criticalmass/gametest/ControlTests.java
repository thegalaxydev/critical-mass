package com.criticalmass.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import com.criticalmass.CriticalMass;
import com.criticalmass.content.control.PlantChannel;
import com.criticalmass.content.control.PlantDisplaySources;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.kinetics.ControlRodDriveBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlock;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class ControlTests {
    private static final String EMPTY = "empty";
    private static final String LARGE = "empty_large";

    private ControlTests() {
    }

    private static ControlRodDriveBlockEntity drive(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, CMBlocks.CONTROL_ROD_DRIVE.get().defaultBlockState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.WEST));
        h.setBlock(pos.east(), AllBlocks.COGWHEEL.getDefaultState());
        h.setBlock(pos.east().above(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        CreativeMotorBlockEntity motor = h.getBlockEntity(pos.east().above());
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

    private static ReactorControllerBlock.Display face(GameTestHelper h) {
        return h.getBlockState(TestVessels.CONTROLLER).getValue(ReactorControllerBlock.STATUS);
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
    public static void plantStartsRunsDrivesAMachineAndShutsDown(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        TestVessels.plant(h);
        h.setBlock(TestVessels.MOTOR, Blocks.AIR);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        com.criticalmass.content.thermal.HeatExchangerBlockEntity exchanger = h.getBlockEntity(TestVessels.EXCHANGER);
        SteamTurbineBlockEntity turbine = h.getBlockEntity(TestVessels.PLANT_TURBINE);
        c.coolant().set(CoolantLoopTests.coolant(6000), 6000 * 230.0);
        exchanger.secondary().set(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), 1000 * CMThermal.SATURATION_ENERGY);
        turbine.coolant().set(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), 1000 * CMThermal.SATURATION_ENERGY);
        h.setBlock(TestVessels.PLANT_TURBINE.west(2), AllBlocks.MECHANICAL_PRESS.getDefaultState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.WEST));
        ControlRodDriveBlockEntity a = drive(h, new BlockPos(3, 6, 2));
        ControlRodDriveBlockEntity b = drive(h, new BlockPos(3, 6, 4));
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
                    h.assertTrue(face(h) == ReactorControllerBlock.Display.RUNNING, "The controller's face should show a running reactor: " + face(h));
                    h.assertTrue(c.core().lastOutput() > 0.05 * c.core().ratedOutput() && c.core().neutronPower() < CMThermal.HIGH_FLUX_TRIP, "The core should be making heat: " + c.core().lastOutput());
                    h.assertTrue(Math.abs(c.core().reactivity()) < 2e-3, "A core holding power should sit near critical: " + c.core().reactivity());
                    h.assertTrue(c.core().maxTemperature() > c.coolant().temperature() + 5, "Fuel at power should run hotter than its coolant: " + c.core().maxTemperature() + " over " + c.coolant().temperature());
                    h.assertTrue(exchanger.exchanged() > 0 && turbine.heatExtracted() > 0, "Heat should cross the exchanger and reach the turbine");
                    h.assertTrue(turbine.getSpeed() != 0 && turbine.shaftLoad() > 0, "The turbine should be turning the press on its shaft: " + turbine.shaftLoad());
                    h.assertTrue(turbine.shaftLoad() <= turbine.heatRate() * CMThermal.SU_PER_HEAT + 1e-6, "Shaft work cannot exceed the steam energy the turbine takes: " + turbine.shaftLoad() + " vs " + turbine.heatRate() * CMThermal.SU_PER_HEAT);
                    c.scram();
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(!c.isReacting() && c.isTripped() && a.insertion() == 1 && b.insertion() == 1, "SCRAM should stop the reaction and insert the rods");
                    h.assertTrue(face(h) == ReactorControllerBlock.Display.TRIPPED, "The controller's face should show the trip: " + face(h));
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
    public static void gogglesGiveAStateAndAFewReadings(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 6000 * (300 - CMThermal.AMBIENT) * CMThermal.HEAT_CAPACITY);
        h.runAfterDelay(20, () -> {
            List<Component> lines = new ArrayList<>();
            c.addToGoggleTooltip(lines, false);
            List<String> text = lines.stream().map(line -> line.getString().trim()).toList();
            h.assertTrue(text.size() <= 8, "A sound reactor should read in eight lines or fewer: " + text);
            h.assertTrue(text.get(0).endsWith(":") && text.contains("Fuel Temperature:") && text.get(text.size() - 1).endsWith("at 300 \u00b0C"),
                    "The readout should be a title, then labels over values, ending with the coolant: " + text);
            h.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void controllerFaceIsDarkWithoutAVesselAndLitOnStandby(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        BlockPos lone = new BlockPos(7, 2, 7);
        h.setBlock(lone, CMBlocks.REACTOR_CONTROLLER.get());
        h.runAfterDelay(40, () -> {
            h.assertTrue(face(h) == ReactorControllerBlock.Display.STANDBY, "A sound reactor that is shut down should show standby: " + face(h));
            ReactorControllerBlock.Display alone = h.getBlockState(lone).getValue(ReactorControllerBlock.STATUS);
            h.assertTrue(alone == ReactorControllerBlock.Display.OFF, "A controller with no vessel should stay dark: " + alone);
            h.succeed();
        });
    }
}
