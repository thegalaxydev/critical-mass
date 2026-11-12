package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.kinetics.ControlRodDriveBlockEntity;
import com.criticalmass.content.thermal.ChannelBlocks;
import net.minecraft.world.level.block.state.BlockState;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolantBuffer;
import com.criticalmass.content.thermal.EmergencyTankBlockEntity;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.ReactorCore;
import com.criticalmass.content.thermal.WeldingTorchItem;
import com.criticalmass.content.thermal.VesselStructure;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMStress;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
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
            h.setBlock(pos.east(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.WEST));
            CreativeMotorBlockEntity motor = h.getBlockEntity(pos.east());
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
                .thenIdle(400)
                .thenExecute(() -> {
                    h.assertTrue(c.status().endsWith("running"), "Withdrawn rods with cooling should be Running: " + c.status());
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

    @GameTest(template = LARGE, timeoutTicks = 600)
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
                .thenIdle(200)
                .thenExecute(() -> {
                    h.assertTrue(a.insertion() == 0 && b.insertion() == 1, "Only bank A should withdraw: A " + a.insertion() + " B " + b.insertion());
                    h.assertTrue(a.travel() == 3 && b.travel() == 3, "Drives should learn the column height: " + a.travel());
                    for (int y = 2; y <= 4; y++) {
                        h.assertTrue(h.getBlockState(new BlockPos(3, y, 2)).getValue(ChannelBlocks.Control.ROD) == 0, "Withdrawn column should show no rod at y " + y);
                        h.assertTrue(h.getBlockState(new BlockPos(3, y, 4)).getValue(ChannelBlocks.Control.ROD) == ChannelBlocks.ROD_LEVELS, "Inserted column should show a full rod at y " + y);
                    }
                    h.assertTrue(c.isReacting(), "Reactor should run on bank A: " + c.status());
                    h.assertTrue(h.getBlockState(new BlockPos(3, 3, 3)).getValue(ChannelBlocks.Fuel.GLOW) == 0, "Fuel beside an inserted rod should stay dark");
                    BlockState side = h.getBlockState(new BlockPos(2, 3, 3));
                    h.assertTrue(side.getValue(ChannelBlocks.Fuel.GLOW) == 2 && side.getLightEmission() > 0, "Unrodded fuel should glow at full power");
                    c.setBankTarget(1, 0.5);
                })
                .thenIdle(120)
                .thenExecute(() -> {
                    h.assertTrue(Math.abs(b.insertion() - 0.5) < 1e-6 && Math.abs(c.bankInsertion(1) - 0.5) < 1e-6, "Bank B should stop half way: " + b.insertion());
                    h.assertTrue(h.getBlockState(new BlockPos(3, 4, 4)).getValue(ChannelBlocks.Control.ROD) == ChannelBlocks.ROD_LEVELS, "Top of a half-inserted column holds the rod");
                    h.assertTrue(h.getBlockState(new BlockPos(3, 3, 4)).getValue(ChannelBlocks.Control.ROD) == 2, "Rod tip should sit half way down the middle block");
                    h.assertTrue(h.getBlockState(new BlockPos(3, 2, 4)).getValue(ChannelBlocks.Control.ROD) == 0, "Bottom of a half-inserted column is empty");
                    h.assertTrue(h.getBlockState(new BlockPos(3, 3, 3)).getValue(ChannelBlocks.Fuel.GLOW) > 0, "Fuel should light up once its rod is half out");
                    CompoundTag saved = b.saveWithFullMetadata(h.getLevel().registryAccess());
                    ControlRodDriveBlockEntity copy = (ControlRodDriveBlockEntity) BlockEntity.loadStatic(h.absolutePos(posB), b.getBlockState(), saved, h.getLevel().registryAccess());
                    h.assertTrue(copy.bank() == 1 && Math.abs(copy.insertion() - 0.5) < 1e-6 && copy.travel() == 3, "Bank, position and travel should survive saving");
                    CompoundTag core = c.saveWithFullMetadata(h.getLevel().registryAccess());
                    ReactorControllerBlockEntity coreCopy = (ReactorControllerBlockEntity) BlockEntity.loadStatic(h.absolutePos(TestVessels.CONTROLLER), c.getBlockState(), core, h.getLevel().registryAccess());
                    h.assertTrue(coreCopy.bankTarget(0) == 0.0 && coreCopy.bankTarget(1) == 0.5, "Bank targets should survive saving");
                })
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
                .thenExecute(() -> h.setBlock(CASING, Blocks.AIR))
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
        for (int i = 0; i < 2400; i++) {
            core.tick(true, 0, 0, buffer);
        }
        double full = core.lastOutput();
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

    @GameTest(template = EMPTY, timeoutTicks = 300)
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
                .thenIdle(120)
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

    @GameTest(template = LARGE, timeoutTicks = 400)
    public static void rodDrivesSetReactorOutputAndScramOnTrip(GameTestHelper h) {
        ReactorControllerBlockEntity c = runningLoop(h, TestVessels.STANDARD);
        ControlRodDriveBlockEntity a = drive(h, DRIVE_A, 256);
        ControlRodDriveBlockEntity b = drive(h, DRIVE_B, 256);
        h.setBlock(DRIVE_A.west(), Blocks.REDSTONE_BLOCK);
        h.setBlock(DRIVE_B.west(), Blocks.REDSTONE_BLOCK);
        double side = CMThermal.FUEL_BASE_OUTPUT * 3 * (1 + CMThermal.FUEL_ADJACENCY_FACTOR);
        double center = CMThermal.FUEL_BASE_OUTPUT * 3 * (1 + 2 * CMThermal.FUEL_ADJACENCY_FACTOR);
        h.startSequence()
                .thenIdle(80)
                .thenExecute(() -> {
                    h.assertTrue(a.insertion() == 0 && b.insertion() == 0 && c.isReacting(), "Drives should withdraw and start the reactor: " + c.status());
                    h.assertTrue(Math.abs(c.core().lastOutput() - (2 * side + center)) < 1e-6, "Output with rods out: " + c.core().lastOutput());
                    h.setBlock(CASING, Blocks.AIR);
                })
                .thenIdle(CMThermal.ROD_FAILSAFE_TICKS + 5)
                .thenExecute(() -> h.assertTrue(a.insertion() == 1 && b.insertion() == 1, "Trip should release the drive latches and insert the rods"))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void emergencyTankInjectsWithoutErasingHeat(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        BlockPos tankPos = new BlockPos(0, 2, 2);
        h.setBlock(tankPos, CMBlocks.EMERGENCY_COOLANT_TANK.get());
        EmergencyTankBlockEntity tank = h.getBlockEntity(tankPos);
        tank.coolant().set(CoolantLoopTests.coolant(4000), 0);
        c.coolant().set(CoolantLoopTests.coolant(2000), 2000 * 200.0);
        double[] heat = new double[1];
        h.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    h.assertTrue(tank.coolant().amount() == 4000, "No injection without an emergency: " + tank.status());
                    c.core().setColumnHeat(2, 0, 200 * 3 * 400.0);
                    heat[0] = c.coolant().heat();
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    int injected = 4000 - tank.coolant().amount();
                    h.assertTrue(injected > 0 && c.coolant().amount() == 2000 + injected, "Tank should inject into the vessel: " + tank.status());
                    h.assertTrue(c.coolant().heat() == heat[0], "Injection must not change stored heat: " + c.coolant().heat() + " vs " + heat[0]);
                    h.assertTrue(c.coolant().temperature() < CMThermal.AMBIENT + 200, "Injected coolant should lower the temperature");
                    h.setBlock(tankPos.west(), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    int amount = tank.coolant().amount();
                    h.assertTrue(tank.status().endsWith("disarmed"), "Redstone should disarm the tank");
                    h.runAfterDelay(10, () -> h.assertTrue(tank.coolant().amount() == amount, "Disarmed tank injected"));
                })
                .thenIdle(12)
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
}
