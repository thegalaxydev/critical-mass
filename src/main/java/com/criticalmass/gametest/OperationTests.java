package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import java.util.List;
import com.criticalmass.content.CoolantPortBlock;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.kinetics.ControlRodDriveBlockEntity;
import com.criticalmass.content.thermal.ChannelBlocks;
import net.minecraft.world.level.block.state.BlockState;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolantBuffer;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.ReactorCore;
import com.criticalmass.content.thermal.WeldingTorchItem;
import com.criticalmass.content.thermal.VesselStructure;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMStress;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class OperationTests {
    private static final String EMPTY = "empty";
    private static final String LARGE = "empty_large";
    private static final BlockPos DRIVE_A = new BlockPos(3, 6, 2);
    private static final BlockPos DRIVE_B = new BlockPos(3, 6, 4);
    private static final BlockPos CASING = new BlockPos(5, 2, 2);

    private OperationTests() {
    }

    private static ControlRodDriveBlockEntity drive(GameTestHelper h, BlockPos pos, int rpm) {
        h.setBlock(pos, CMBlocks.CONTROL_ROD_DRIVE.get().defaultBlockState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.WEST));
        if (rpm != 0) {
            h.setBlock(pos.east(), AllBlocks.COGWHEEL.getDefaultState());
            h.setBlock(pos.east().above(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
            CreativeMotorBlockEntity motor = h.getBlockEntity(pos.east().above());
            motor.generatedSpeed.setValue(rpm);
        }
        return h.getBlockEntity(pos);
    }

    private static ReactorControllerBlockEntity runningLoop(GameTestHelper h, char[][] layout) {
        TestVessels.standardVessel(h, layout);
        TestVessels.loop(h, true);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 0);
        return c;
    }

    @GameTest(template = LARGE, timeoutTicks = 900)
    public static void stateMachineWalksThroughStates(GameTestHelper h) {
        ReactorControllerBlockEntity c = runningLoop(h, TestVessels.COOLED);
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    h.assertTrue(c.status().endsWith("cold"), "Fresh reactor should be Cold: " + c.status());
                    h.setBlock(TestVessels.REDSTONE, Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(c.status().endsWith("source_range") || c.status().endsWith("starting"), "Armed without power should read source range: " + c.status());
                    TestVessels.atPower(c, 0.5);
                })
                .thenIdle(400)
                .thenExecute(() -> {
                    h.assertTrue(c.status().endsWith("running"), "Withdrawn rods with cooling should be Running: " + c.status());
                    for (ReactorCore.FuelColumn f : c.core().columns()) {
                        c.core().setColumnDecay(f.dx(), f.dz(), 5.0);
                    }
                    h.setBlock(CASING, Blocks.AIR);
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    h.assertTrue(c.status().endsWith("invalid") && c.isTripped() && c.tripReason().equals("invalid"), "Broken vessel should trip: " + c.status() + " " + c.tripReason());
                    h.setBlock(CASING, CMBlocks.REACTOR_VESSEL_CASING.get());
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    h.assertTrue(c.status().endsWith("tripped"), "Repaired vessel stays Tripped until reset: " + c.status());
                    h.setBlock(TestVessels.REDSTONE, Blocks.AIR);
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    h.assertTrue(!c.isTripped() && c.status().endsWith("cooling_down"), "Reset reactor with residual heat should be Cooling Down: " + c.status() + " residual " + c.core().residualPower());
                    for (ReactorCore.FuelColumn f : c.core().columns()) {
                        c.core().setColumnDecay(f.dx(), f.dz(), 0);
                        c.core().setColumnHeat(f.dx(), f.dz(), 0);
                    }
                    c.coolant().set(CoolantLoopTests.coolant(c.coolant().amount()), 0);
                })
                .thenIdle(3)
                .thenExecute(() -> h.assertTrue(c.status().endsWith("cold"), "Cool, unheated core should be Cold: " + c.status()))
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 900)
    public static void aRodLeverMovesItsBankWhileHeldAndHoldsItInTheMiddle(GameTestHelper h) {
        ReactorControllerBlockEntity c = runningLoop(h, TestVessels.STANDARD);
        ControlRodDriveBlockEntity a = drive(h, DRIVE_A, 64);
        ControlRodDriveBlockEntity b = drive(h, DRIVE_B, 64);
        b.setBank(1);
        BlockPos deskPos = new BlockPos(10, 1, 10);
        h.setBlock(deskPos, CMBlocks.REACTOR_ALLOY_DASHBOARD.get());
        com.criticalmass.content.control.DashboardBlockEntity desk = h.getBlockEntity(deskPos);
        com.criticalmass.content.control.DashPart lever = new com.criticalmass.content.control.DashPart(com.criticalmass.content.control.DashPart.Type.LEVER);
        h.assertTrue(desk.install(0, lever) && desk.bind(0, h.absolutePos(TestVessels.CONTROLLER)), "A lever should bind to a Reactor Controller");
        h.assertTrue(lever.modes(h.getLevel()) == CMThermal.ROD_BANKS + 1 && lever.rodBank() == 0 && desk.modeKey(lever).endsWith("rods_a"), "A rod lever should offer each bank and all of them: " + lever.modes(h.getLevel()));
        int top = com.criticalmass.content.control.DashPart.MAX_LEVEL;
        double[] seen = new double[1];
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(a.insertion() == 1 && b.insertion() == 1, "A lever at the bottom should leave the rods in");
                    desk.setLever(0, top);
                })
                .thenIdle(100)
                .thenExecute(() -> {
                    h.assertTrue(a.insertion() < 0.84 && a.insertion() > 0.76 && b.insertion() == 1, "A lever pushed right up should withdraw its own bank at 4% a second: A " + a.insertion() + " B " + b.insertion());
                    seen[0] = a.insertion();
                    desk.setLever(0, top / 2);
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(Math.abs(a.insertion() - seen[0]) < 0.012 && Math.abs(c.bankTarget(0) - a.insertion()) < 1e-9, "A lever in the middle should hold the rods where they are: " + a.insertion() + " from " + seen[0]);
                    seen[0] = a.insertion();
                    desk.setLever(0, top / 2 + 2);
                })
                .thenIdle(70)
                .thenExecute(() -> {
                    double moved = seen[0] - a.insertion();
                    h.assertTrue(Math.abs(moved - 70 * CMThermal.LEVER_STEP / CMThermal.LEVER_REACH) < 0.006, "One notch above the middle should creep the rods out: " + moved);
                    for (int i = 0; i < CMThermal.ROD_BANKS; i++) {
                        desk.cycle(0);
                    }
                    h.assertTrue(lever.rodBank() == -1 && desk.modeKey(lever).endsWith("rods_all"), "The last setting should be all banks");
                    c.setBankTarget(1, 0.5);
                    desk.setLever(0, 0);
                })
                .thenIdle(200)
                .thenExecute(() -> h.assertTrue(a.insertion() > 1 - 1e-9 && b.insertion() > 1 - 1e-9 && c.bankTarget(0) == 1 && c.bankTarget(1) == 1, "Pulled right down on all banks it should drive every rod in: A " + a.insertion() + " B " + b.insertion()))
                .thenSucceed();
    }

    private static com.criticalmass.content.control.DashPart deskButton(GameTestHelper h, com.criticalmass.content.control.DashboardBlockEntity desk, int slot, com.criticalmass.content.control.DashPart.Press press) {
        com.criticalmass.content.control.DashPart part = new com.criticalmass.content.control.DashPart(com.criticalmass.content.control.DashPart.Type.BUTTON);
        h.assertTrue(desk.install(slot, part) && desk.bind(slot, h.absolutePos(TestVessels.CONTROLLER)), "A button should bind to a Reactor Controller");
        for (int i = 0; i < 8 && part.press(h.getLevel()) != press; i++) {
            desk.cycle(slot);
        }
        h.assertTrue(part.press(h.getLevel()) == press && desk.modeKey(part).endsWith(press.key), "A button on the controller should offer " + press.key);
        return part;
    }

    @GameTest(template = LARGE, timeoutTicks = 400)
    public static void deskButtonsWorkTheRegulatorAndABoundRodLeverStartsAtHold(GameTestHelper h) {
        ReactorControllerBlockEntity c = runningLoop(h, TestVessels.STANDARD);
        ControlRodDriveBlockEntity d = drive(h, DRIVE_B, 64);
        d.setBank(CMThermal.REGULATING_BANK);
        BlockPos buttons = new BlockPos(10, 1, 10);
        BlockPos others = new BlockPos(12, 1, 10);
        h.setBlock(buttons, CMBlocks.REACTOR_ALLOY_DASHBOARD.get());
        h.setBlock(others, CMBlocks.REACTOR_ALLOY_DASHBOARD.get());
        com.criticalmass.content.control.DashboardBlockEntity desk = h.getBlockEntity(buttons);
        com.criticalmass.content.control.DashboardBlockEntity side = h.getBlockEntity(others);
        deskButton(h, desk, 0, com.criticalmass.content.control.DashPart.Press.REGULATOR_RAISE);
        deskButton(h, desk, 1, com.criticalmass.content.control.DashPart.Press.REGULATOR_LOWER);
        com.criticalmass.content.control.DashPart lever = new com.criticalmass.content.control.DashPart(com.criticalmass.content.control.DashPart.Type.LEVER);
        h.assertTrue(lever.level == 0 && side.install(0, lever) && side.bind(0, h.absolutePos(TestVessels.CONTROLLER)), "A lever should bind to a Reactor Controller");
        h.assertTrue(lever.level == com.criticalmass.content.control.DashPart.MAX_LEVEL / 2 && lever.rodShift(0) == 0, "A lever bound to a controller should start in the middle, holding: " + lever.level);
        com.criticalmass.content.control.PlantChannel setpoint = com.criticalmass.content.control.PlantChannel.SETPOINT;
        BlockPos controller = h.absolutePos(TestVessels.CONTROLLER);
        h.startSequence()
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(desk.use(1, 0.5).endsWith("regulator_off") && c.regulatorSetpoint() == 0, "LOWER should leave a regulator that is off alone");
                    TestVessels.atPower(c, 0.2);
                    h.assertTrue(desk.use(0, 0.5).endsWith("regulator_on") && c.regulatorSetpoint() == 20, "RAISE should switch the regulator on at the present power: " + c.regulatorSetpoint());
                    h.assertTrue(desk.use(0, 0.5).endsWith("regulator_raised") && c.regulatorSetpoint() == 25, "RAISE should then move the setpoint up a notch: " + c.regulatorSetpoint());
                    com.criticalmass.content.control.PlantChannel.Reading shown = setpoint.read(h.getLevel(), controller);
                    h.assertTrue(shown != null && shown.value() == 25 && shown.max() == 100, "The setpoint channel should read the regulator: " + shown);
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    h.assertTrue(c.regulatorSetpoint() == 25 && !c.isTripped(), "The regulator should stay on: " + c.regulatorSetpoint() + " " + c.tripReason());
                    for (int want = 20; want >= 5; want -= 5) {
                        h.assertTrue(desk.use(1, 0.5).endsWith("regulator_lowered") && c.regulatorSetpoint() == want, "LOWER should move the setpoint down a notch: " + c.regulatorSetpoint());
                    }
                    h.assertTrue(desk.use(1, 0.5).endsWith("regulator_off") && c.regulatorSetpoint() == 0, "LOWER below the last notch should switch it off: " + c.regulatorSetpoint());
                    c.scram();
                    h.assertTrue(desk.use(0, 0.5).endsWith("regulator_tripped") && c.regulatorSetpoint() == 0, "RAISE should be refused while the reactor is tripped");
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 2600)
    public static void theRegulatorHoldsPowerWithBankDAndATripSwitchesItOff(GameTestHelper h) {
        ReactorControllerBlockEntity c = runningLoop(h, TestVessels.STANDARD);
        ControlRodDriveBlockEntity a = drive(h, DRIVE_A, 64);
        ControlRodDriveBlockEntity d = drive(h, DRIVE_B, 64);
        d.setBank(CMThermal.REGULATING_BANK);
        c.holdBoron(5000);
        c.setBankTarget(0, 0.0);
        c.setBankTarget(CMThermal.REGULATING_BANK, 0.5);
        h.startSequence()
                .thenIdle(540)
                .thenExecute(() -> {
                    h.assertTrue(a.insertion() == 0 && Math.abs(d.insertion() - 0.5) < 1e-6, "The banks should be set up: A " + a.insertion() + " D " + d.insertion());
                    TestVessels.atPower(c, 0.2);
                    double xenon = (CMThermal.XENON_DECAY + CMThermal.XENON_BURNOUT) * 0.2 / (CMThermal.XENON_DECAY + CMThermal.XENON_BURNOUT * 0.2);
                    double defect = (CMThermal.FUEL_COEFFICIENT + CMThermal.MODERATOR_COEFFICIENT) * (c.coolant().temperature() - CMThermal.AMBIENT) + CMThermal.XENON_WORTH * xenon;
                    c.holdBoron((c.core().excessReactivity() - CMThermal.ROD_WORTH * 3 / 5 / 2 - defect) / CMThermal.BORON_WORTH);
                    c.setRegulator(30);
                })
                .thenIdle(700)
                .thenExecute(() -> {
                    h.assertTrue(!c.isTripped() && c.regulatorSetpoint() == 30, "The regulator should not trip the reactor: " + c.tripReason());
                    h.assertTrue(Math.abs(c.core().neutronPower() - 0.3) < 0.02, "The regulator should bring power to its setpoint and hold it: " + c.core().neutronPower() + " with bank D at " + d.insertion());
                    h.assertTrue(a.insertion() == 0, "It should leave the other banks alone: " + a.insertion());
                    c.setRegulator(45);
                })
                .thenIdle(700)
                .thenExecute(() -> {
                    h.assertTrue(!c.isTripped() && Math.abs(c.core().neutronPower() - 0.45) < 0.03, "It should follow a new setpoint: " + c.core().neutronPower() + " with bank D at " + d.insertion() + " " + c.tripReason());
                    c.scram();
                })
                .thenIdle(5)
                .thenExecute(() -> h.assertTrue(c.isTripped() && c.regulatorSetpoint() == 0, "A trip should switch the regulator off: " + c.regulatorSetpoint()))
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 1000)
    public static void rodBanksMoveSeparatelyAndShowInTheCore(GameTestHelper h) {
        ReactorControllerBlockEntity c = runningLoop(h, TestVessels.STANDARD);
        BlockPos posA = new BlockPos(3, 6, 2);
        BlockPos posB = new BlockPos(3, 6, 4);
        ControlRodDriveBlockEntity a = drive(h, posA, 64);
        ControlRodDriveBlockEntity b = drive(h, posB, 64);
        b.setBank(1);
        c.setBankTarget(0, 0.0);
        c.setBankTarget(1, 1.0);
        h.startSequence()
                .thenIdle(520)
                .thenExecute(() -> {
                    h.assertTrue(a.insertion() == 0 && b.insertion() == 1, "Only bank A should withdraw: A " + a.insertion() + " B " + b.insertion());
                    h.assertTrue(a.travel() == 3 && b.travel() == 3, "Drives should learn the column height: " + a.travel());
                    for (int y = 2; y <= 4; y++) {
                        h.assertTrue(h.getBlockState(new BlockPos(3, y, 2)).getValue(ChannelBlocks.Control.ROD) == 0, "Withdrawn column should show no rod at y " + y);
                        h.assertTrue(h.getBlockState(new BlockPos(3, y, 4)).getValue(ChannelBlocks.Control.ROD) == ChannelBlocks.ROD_LEVELS, "Inserted column should show a full rod at y " + y);
                    }
                    h.assertTrue(c.isReacting(), "Reactor should run on bank A: " + c.status());
                    h.assertTrue(Math.abs(c.core().rodReactivity() + CMThermal.ROD_WORTH * 3 / 5) < 1e-9, "Bank B alone should hold its three fuel columns down: " + c.core().rodReactivity());
                    c.setBankTarget(1, 0.5);
                })
                .thenIdle(270)
                .thenExecute(() -> {
                    h.assertTrue(Math.abs(b.insertion() - 0.5) < 1e-6 && Math.abs(c.bankInsertion(1) - 0.5) < 1e-6, "Bank B should stop half way: " + b.insertion());
                    h.assertTrue(h.getBlockState(new BlockPos(3, 4, 4)).getValue(ChannelBlocks.Control.ROD) == ChannelBlocks.ROD_LEVELS, "Top of a half-inserted column holds the rod");
                    h.assertTrue(h.getBlockState(new BlockPos(3, 3, 4)).getValue(ChannelBlocks.Control.ROD) == 2, "Rod tip should sit half way down the middle block");
                    h.assertTrue(h.getBlockState(new BlockPos(3, 2, 4)).getValue(ChannelBlocks.Control.ROD) == 0, "Bottom of a half-inserted column is empty");
                    h.assertTrue(Math.abs(c.core().rodReactivity() + CMThermal.ROD_WORTH * 3 / 5 / 2) < 1e-6, "A half-inserted bank should be worth half: " + c.core().rodReactivity());
                    CompoundTag saved = b.saveWithFullMetadata(h.getLevel().registryAccess());
                    ControlRodDriveBlockEntity copy = (ControlRodDriveBlockEntity) BlockEntity.loadStatic(h.absolutePos(posB), b.getBlockState(), saved, h.getLevel().registryAccess());
                    h.assertTrue(copy.bank() == 1 && Math.abs(copy.insertion() - 0.5) < 1e-6 && copy.travel() == 3, "Bank, position and travel should survive saving");
                    CompoundTag core = c.saveWithFullMetadata(h.getLevel().registryAccess());
                    ReactorControllerBlockEntity coreCopy = (ReactorControllerBlockEntity) BlockEntity.loadStatic(h.absolutePos(TestVessels.CONTROLLER), c.getBlockState(), core, h.getLevel().registryAccess());
                    h.assertTrue(coreCopy.bankTarget(0) == 0.0 && coreCopy.bankTarget(1) == 0.5, "Bank targets should survive saving");
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 900)
    public static void startNeedsFlowAndCoolant(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 0);
        ControlRodDriveBlockEntity d = drive(h, new BlockPos(3, 6, 2), 64);
        c.setBankTarget(0, 0.0);
        boolean[] flow = {false};
        h.onEachTick(() -> {
            if (flow[0]) {
                c.noteFlow(32);
            }
        });
        h.startSequence()
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(!c.isReacting() && c.status().endsWith("blocked_flow"), "No flow should block the start: " + c.status());
                    h.assertTrue(d.insertion() == 1 && d.withdrawalBlocked() && d.requested() == 0, "Rods should not withdraw while the start is blocked: " + d.insertion());
                    c.coolant().set(CoolantLoopTests.coolant(2000), 0);
                    flow[0] = true;
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(!c.isReacting() && c.status().endsWith("blocked_coolant"), "A vessel under half full should block the start: " + c.status());
                    h.assertTrue(d.insertion() == 1, "Rods should still be held in");
                    c.coolant().set(CoolantLoopTests.coolant(6000), 0);
                })
                .thenIdle(520)
                .thenExecute(() -> {
                    h.assertTrue(c.isReacting() && d.insertion() == 0, "With flow and coolant the rods should withdraw and the reactor start: " + c.status() + " " + d.insertion());
                    flow[0] = false;
                })
                .thenIdle(60)
                .thenExecute(() -> h.assertTrue(c.isTripped() && c.tripReason().equals("low_flow"), "Losing flow at power should still trip: " + c.status()))
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 900)
    public static void aBypassedReactorRunsOnWithoutFlowUntilTheBypassIsLetGo(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 0);
        drive(h, new BlockPos(3, 6, 2), 64);
        c.setBankTarget(0, 0.0);
        boolean[] flow = {true};
        boolean[] bypass = {false};
        int lamp = 1 << com.criticalmass.content.control.PlantAlarms.Alarm.BYPASS.ordinal();
        h.onEachTick(() -> {
            if (flow[0]) {
                c.noteFlow(32);
            }
            if (bypass[0]) {
                c.holdBypass(5);
            }
        });
        h.startSequence()
                .thenIdle(560)
                .thenExecute(() -> {
                    h.assertTrue(c.isReacting() && !c.bypassed(), "The reactor should be running with its protection on: " + c.status());
                    h.assertTrue((com.criticalmass.content.control.PlantAlarms.evaluate(c) & lamp) == 0, "The bypass lamp should be dark");
                    bypass[0] = true;
                    flow[0] = false;
                })
                .thenIdle(CMThermal.LOW_FLOW_TRIP_TICKS + 15)
                .thenExecute(() -> {
                    h.assertTrue(c.bypassed() && !c.isTripped() && c.isReacting(), "A bypassed reactor should run on without flow: " + c.status() + " " + c.tripReason());
                    h.assertTrue((com.criticalmass.content.control.PlantAlarms.evaluate(c) & lamp) != 0, "The bypass lamp should be lit");
                    c.scram();
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    h.assertTrue(c.isTripped() && c.tripReason().equals("manual"), "A SCRAM by hand should still work through the bypass: " + c.tripReason());
                    bypass[0] = false;
                })
                .thenIdle(10)
                .thenExecute(() -> h.assertTrue(!c.bypassed(), "The bypass should lapse once nothing holds it"))
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void viewportCountsAsVesselWall(GameTestHelper h) {
        ReactorControllerBlockEntity c = runningLoop(h, TestVessels.STANDARD);
        h.setBlock(new BlockPos(5, 3, 3), CMBlocks.REACTOR_VIEWPORT.get());
        h.setBlock(TestVessels.REDSTONE, Blocks.REDSTONE_BLOCK);
        h.startSequence()
                .thenIdle(120)
                .thenExecute(() -> {
                    h.assertTrue(c.problemPositions().isEmpty() && c.missing().isEmpty(), "A viewport in the wall should keep the vessel valid: " + c.problemReasons());
                    h.assertTrue(c.isReacting(), "Reactor should run with a viewport: " + c.status());
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void tripStateAndResidualHeatSurviveSaveAndLoad(GameTestHelper h) {
        ReactorControllerBlockEntity c = runningLoop(h, TestVessels.COOLED);
        h.setBlock(TestVessels.REDSTONE, Blocks.REDSTONE_BLOCK);
        h.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    for (ReactorCore.FuelColumn f : c.core().columns()) {
                        c.core().setColumnDecay(f.dx(), f.dz(), 5.0);
                    }
                    h.setBlock(CASING, Blocks.AIR);
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    CompoundTag saved = c.saveWithFullMetadata(h.getLevel().registryAccess());
                    BlockEntity loaded = BlockEntity.loadStatic(h.absolutePos(TestVessels.CONTROLLER), c.getBlockState(), saved, h.getLevel().registryAccess());
                    ReactorControllerBlockEntity copy = (ReactorControllerBlockEntity) loaded;
                    h.assertTrue(copy.isTripped() && copy.tripReason().equals("invalid"), "Trip state lost across save/load");
                    h.assertTrue(c.core().residualPower() > 0 && Math.abs(copy.core().residualPower() - c.core().residualPower()) < 1e-9, "Residual heat lost across save/load");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void residualHeatDecaysAndNeedsCooling(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        VesselStructure s = VesselStructure.scan(h.getLevel(), h.absolutePos(TestVessels.CONTROLLER));
        ReactorCore core = new ReactorCore();
        core.rebuild(s, h.absolutePos(TestVessels.CONTROLLER));
        core.fillFresh();
        CoolantBuffer buffer = new CoolantBuffer(1000, () -> { });
        double full = core.ratedOutput();
        for (ReactorCore.FuelColumn f : core.columns()) {
            core.setColumnDecay(f.dx(), f.dz(), CMThermal.DECAY_FRACTION * full / core.fuelColumns());
        }
        core.tick(false, 1, 0, buffer);
        double first = core.lastOutput();
        double heat = core.storedHeat();
        for (int i = 0; i < 600; i++) {
            core.tick(false, 1, 0, buffer);
        }
        double later = core.lastOutput();
        h.assertTrue(first > 0 && first < full * CMThermal.DECAY_FRACTION * 1.01, "Residual heat right after shutdown should be a few percent of full power: " + first + " of " + full);
        h.assertTrue(later > 0 && later < first, "Residual heat should decay: " + first + " then " + later);
        h.assertTrue(core.storedHeat() > heat, "Without cooling, residual heat keeps warming the core");
        h.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void lostFlowStopsPumpsAndTripsReactor(GameTestHelper h) {
        ReactorControllerBlockEntity c = runningLoop(h, TestVessels.COOLED);
        h.setBlock(TestVessels.REDSTONE, Blocks.REDSTONE_BLOCK);
        h.startSequence()
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(c.isReacting(), "Reactor should be running: " + c.status());
                    h.setBlock(TestVessels.MOTOR, Blocks.AIR);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(TestVessels.PUMP);
                    h.assertTrue(pump.flow() == 0, "Pump should stop as soon as the network stops");
                })
                .thenIdle(CMThermal.LOW_FLOW_TRIP_TICKS + 5)
                .thenExecute(() -> h.assertTrue(c.isTripped() && c.tripReason().equals("low_flow"), "Reactor should trip on lost flow: " + c.status() + " " + c.tripReason()))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 700)
    public static void rodDriveRateIsCappedAndHoldingIsFree(GameTestHelper h) {
        ControlRodDriveBlockEntity d = drive(h, new BlockPos(2, 1, 2), 256);
        h.setBlock(new BlockPos(1, 1, 2), Blocks.REDSTONE_BLOCK);
        double[] last = {1.0};
        double[] fastest = {0};
        float[] movingStress = {0};
        h.onEachTick(() -> {
            fastest[0] = Math.max(fastest[0], last[0] - d.insertion());
            last[0] = d.insertion();
            if (d.moving()) {
                movingStress[0] = d.calculateStressApplied();
            }
        });
        h.startSequence()
                .thenIdle(520)
                .thenExecute(() -> {
                    h.assertTrue(d.insertion() == 0 && !d.moving(), "Drive should reach full withdrawal: " + d.insertion());
                    h.assertTrue(fastest[0] <= CMThermal.ROD_MAX_RATE + 1e-9 && fastest[0] > 0, "Insertion changed too fast: " + fastest[0]);
                    h.assertTrue(movingStress[0] == (float) CMStress.CONTROL_ROD_DRIVE_IMPACT, "Moving drive should load the network");
                    h.assertTrue(d.calculateStressApplied() == 0, "Holding position should cost no stress");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void lostShaftPowerInsertsRods(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        ControlRodDriveBlockEntity d = drive(h, pos, 64);
        h.setBlock(new BlockPos(1, 1, 2), Blocks.REDSTONE_BLOCK);
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    d.setInsertion(0);
                    h.setBlock(pos.east(), Blocks.AIR);
                })
                .thenIdle(CMThermal.ROD_FAILSAFE_TICKS + 3)
                .thenExecute(() -> h.assertTrue(d.insertion() == 1.0, "Rods should be fully in within the fail-safe time: " + d.insertion()))
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 900)
    public static void rodDrivesSetReactorOutputAndScramOnTrip(GameTestHelper h) {
        ReactorControllerBlockEntity c = runningLoop(h, TestVessels.STANDARD);
        ControlRodDriveBlockEntity a = drive(h, DRIVE_A, 256);
        ControlRodDriveBlockEntity b = drive(h, DRIVE_B, 256);
        h.setBlock(DRIVE_A.west(), Blocks.REDSTONE_BLOCK);
        h.setBlock(DRIVE_B.west(), Blocks.REDSTONE_BLOCK);
        double[] before = new double[1];
        h.startSequence()
                .thenIdle(520)
                .thenExecute(() -> {
                    h.assertTrue(a.insertion() == 0 && b.insertion() == 0 && c.isReacting(), "Drives should withdraw and arm the reactor: " + c.status());
                    TestVessels.atPower(c, 0.5);
                })
                .thenIdle(100)
                .thenExecute(() -> {
                    before[0] = c.core().neutronPower();
                    h.assertTrue(!c.isTripped() && before[0] > 0.05 && c.core().lastOutput() > 0, "The core should be at power: " + before[0] + " " + c.tripReason());
                    h.assertTrue(h.getBlockState(new BlockPos(3, 3, 3)).getValue(ChannelBlocks.Fuel.GLOW) > 0, "Fuel at power should glow");
                    h.setBlock(CASING, Blocks.AIR);
                })
                .thenIdle(CMThermal.ROD_FAILSAFE_TICKS + 5)
                .thenExecute(() -> {
                    h.assertTrue(a.insertion() == 1 && b.insertion() == 1, "Trip should release the drive latches and insert the rods");
                    h.assertTrue(c.core().neutronPower() < before[0] * 0.2 && c.core().reactivity() < -0.05, "Dropped rods should collapse the power: " + c.core().neutronPower() + " from " + before[0]);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void breachLeaksAndRepairWaitsForResidualHeat(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 6000 * 100.0);
        BlockPos abs = h.absolutePos(CASING);
        h.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    c.setIntegrity(abs, 0.01);
                    h.assertTrue(!c.isBreached(), "No breach before integrity reaches zero");
                    c.setIntegrity(abs, 0);
                    c.core().setColumnDecay(2, 0, 100);
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(c.status().endsWith("breached"), "Zero integrity should breach the vessel: " + c.status());
                    h.assertTrue(c.coolant().amount() < 6000 && c.sinks().ventedVolume == 6000 - c.coolant().amount(), "Breach should leak coolant and log it");
                    h.assertTrue(!WeldingTorchItem.allowed(h.getLevel(), abs) && !WeldingTorchItem.repairAt(h.getLevel(), abs), "Repair must be refused while residual heat is high");
                    for (ReactorCore.FuelColumn f : c.core().columns()) {
                        c.core().setColumnDecay(f.dx(), f.dz(), 0);
                    }
                    h.assertTrue(WeldingTorchItem.repairAt(h.getLevel(), abs), "Repair should be allowed once residual heat is low");
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    h.assertTrue(!c.isBreached() && c.integrity(abs) == CMThermal.REPAIR_AMOUNT, "Repair should close the breach");
                    h.assertTrue(!c.status().endsWith("breached") && !c.status().endsWith("damaged"), "Repaired vessel should be usable again: " + c.status());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void fuelWithoutCoolantOrRodBesideItIsFlagged(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.UNSOUND);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        h.startSequence()
                .thenIdle(60)
                .thenExecute(() -> {
                    BlockPos centre = h.absolutePos(new BlockPos(3, 2, 3));
                    BlockPos west = h.absolutePos(new BlockPos(2, 2, 3));
                    BlockPos east = h.absolutePos(new BlockPos(4, 2, 3));
                    h.assertTrue(c.warningPositions().size() == 3, "Three fuel columns should be flagged: " + c.warningPositions() + " " + c.warningReasons());
                    for (int i = 0; i < c.warningPositions().size(); i++) {
                        BlockPos p = c.warningPositions().get(i);
                        String r = c.warningReasons().get(i);
                        boolean right = p.equals(centre) ? r.endsWith("no_coolant") : (p.equals(west) || p.equals(east)) && r.endsWith("no_control");
                        h.assertTrue(right, "Wrong warning at " + p + ": " + r);
                    }
                    h.assertTrue(c.problemPositions().isEmpty(), "A layout warning should not make the vessel invalid: " + c.problemReasons());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rodDrivesTurnByTheirCogwheels(GameTestHelper h) {
        BlockPos first = new BlockPos(5, 2, 3);
        ControlRodDriveBlockEntity near = drive(h, first, 32);
        h.setBlock(first.west(), CMBlocks.CONTROL_ROD_DRIVE.get().defaultBlockState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
        ControlRodDriveBlockEntity far = h.getBlockEntity(first.west());
        h.setBlock(first.west(2), AllBlocks.COGWHEEL.getDefaultState());
        h.setBlock(first.north(), AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Z));
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    KineticBlockEntity beyond = h.getBlockEntity(first.west(2));
                    KineticBlockEntity beside = h.getBlockEntity(first.north());
                    h.assertTrue(Math.abs(near.getSpeed()) == 32, "A cogwheel beside a drive should turn it: " + near.getSpeed());
                    h.assertTrue(far.getSpeed() == -near.getSpeed(), "Two drives side by side should mesh and turn opposite ways: " + near.getSpeed() + " and " + far.getSpeed());
                    h.assertTrue(beyond.getSpeed() == near.getSpeed(), "Rotation should carry on through the row to the next cogwheel: " + beyond.getSpeed());
                    h.assertTrue(beside.getSpeed() == 0, "A drive takes no shaft: " + beside.getSpeed());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void fuelBesideCoolantAndRodIsNotFlagged(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        h.startSequence()
                .thenIdle(60)
                .thenExecute(() -> h.assertTrue(c.warningPositions().isEmpty() && c.problemPositions().isEmpty(), "A sound core should have no warnings: " + c.warningReasons()))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void createPumpInjectsCoolantThroughASparePort(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(4000), 0);
        BlockPos port = new BlockPos(5, 2, 3);
        BlockPos pump = port.east();
        BlockPos tankPos = pump.east();
        BlockPos motorPos = tankPos.above();
        h.setBlock(port, TestVessels.port(Direction.EAST, CoolantPortBlock.Mode.INLET));
        h.setBlock(tankPos, AllBlocks.FLUID_TANK.getDefaultState());
        h.setBlock(pump, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.WEST));
        h.setBlock(pump.above(), AllBlocks.COGWHEEL.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
        FluidTankBlockEntity tank = h.getBlockEntity(tankPos);
        tank.getTankInventory().setFluid(CoolantLoopTests.coolant(3000));
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    c.revalidate();
                    h.assertTrue(c.problemPositions().isEmpty(), "A vessel may have a second inlet port: " + c.problemReasons());
                    h.assertTrue(c.coolant().amount() == 4000, "A stopped pump should inject nothing: " + c.coolant().amount());
                    h.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.WEST));
                    CreativeMotorBlockEntity motor = h.getBlockEntity(motorPos);
                    motor.generatedSpeed.setValue(64);
                })
                .thenIdle(200)
                .thenExecute(() -> {
                    int vessel = c.coolant().amount();
                    int left = tank.getTankInventory().getFluidAmount();
                    h.assertTrue(vessel > 5000, "A turning Mechanical Pump should inject coolant from the tank through the port: " + vessel);
                    h.assertTrue(vessel + left == 7000, "What leaves the tank should arrive in the vessel: " + vessel + " + " + left);
                })
                .thenSucceed();
    }
}
