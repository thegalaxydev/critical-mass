package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.power.BatteryBlockEntity;
import com.criticalmass.content.power.BreakerBlock;
import com.criticalmass.content.power.BreakerBlockEntity;
import com.criticalmass.content.power.BusBarBlockEntity;
import com.criticalmass.content.power.DieselEngineBlockEntity;
import com.criticalmass.content.power.DisconnectBlockEntity;
import com.criticalmass.content.power.EarthingBlockEntity;
import com.criticalmass.content.power.GeneratorBlockEntity;
import com.criticalmass.content.power.GridTieBlockEntity;
import com.criticalmass.content.power.MotorBlockEntity;
import com.criticalmass.content.power.Power;
import com.criticalmass.content.power.SurgeArresterBlockEntity;
import com.criticalmass.content.power.PowerNetwork;
import com.criticalmass.content.power.TransferSwitchBlock;
import com.criticalmass.content.power.TransferSwitchBlockEntity;
import com.criticalmass.content.power.TransformerBlockEntity;
import com.criticalmass.content.power.WireLinks;
import com.criticalmass.content.power.WireType;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMFluids;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class PowerTests {
    private static final String EMPTY = "empty";
    private static final BlockPos MOTOR = new BlockPos(1, 2, 2);
    private static final BlockPos GENERATOR = new BlockPos(2, 2, 2);
    private static final BlockPos BREAKER = new BlockPos(2, 4, 2);
    private static final BlockPos GRID = new BlockPos(2, 6, 2);
    private static final BlockPos POST = new BlockPos(5, 3, 4);
    private static final BlockPos STEP_UP = new BlockPos(5, 5, 2);
    private static final int MAIN = 0;
    private static final int STARTUP = 1;
    private static final int UNIT = 2;

    private PowerTests() {
    }

    private static void wire(GameTestHelper h, WireType type, BlockPos a, int ta, BlockPos b, int tb) {
        String problem = WireLinks.connect(h.getLevel(), h.absolutePos(a), ta, h.absolutePos(b), tb, type);
        h.assertTrue(problem.isEmpty(), "A wire should string between two terminals in reach: " + problem);
    }

    private static void wire(GameTestHelper h, BlockPos a, int ta, BlockPos b, int tb) {
        wire(h, WireType.CABLE, a, ta, b, tb);
    }

    private static TransformerBlockEntity transformer(GameTestHelper h, BlockPos pos, int ratio) {
        h.setBlock(pos, CMBlocks.TRANSFORMER.get());
        TransformerBlockEntity t = h.getBlockEntity(pos);
        t.setRatio(ratio);
        return t;
    }

    private static void line(GameTestHelper h, int sections, int rpm) {
        h.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.EAST));
        for (int i = 0; i < sections; i++) {
            h.setBlock(GENERATOR.east(i), CMBlocks.GENERATOR.get().defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
        }
        h.setBlock(BREAKER, CMBlocks.CIRCUIT_BREAKER.get());
        h.setBlock(GRID, CMBlocks.GRID_CONNECTION.get());
        h.setBlock(POST.below(), Blocks.STONE);
        h.setBlock(POST, CMBlocks.INSULATOR.get());
        wire(h, GENERATOR.east(sections - 1), 0, POST, 0);
        wire(h, POST, 0, BREAKER, BreakerBlockEntity.LOAD);
        transformer(h, STEP_UP, MAIN);
        wire(h, BREAKER, BreakerBlockEntity.LINE, STEP_UP, TransformerBlockEntity.LOW);
        wire(h, WireType.LINE, STEP_UP, TransformerBlockEntity.HIGH, GRID, 0);
        CreativeMotorBlockEntity motor = h.getBlockEntity(MOTOR);
        motor.generatedSpeed.setValue(rpm);
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void generatorSynchronisesThroughABreakerAndCarriesLoad(GameTestHelper h) {
        line(h, 1, (int) Power.SYNC_RPM);
        GeneratorBlockEntity g = h.getBlockEntity(GENERATOR);
        BreakerBlockEntity breaker = h.getBlockEntity(BREAKER);
        GridTieBlockEntity grid = h.getBlockEntity(GRID);
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(g.energised() && !g.synced() && Math.abs(g.frequency() - Power.GRID_HZ) < 1e-6, "A generator at its rated speed should run at 50 Hz, unsynchronised: " + g.frequency());
                    h.assertTrue(WireLinks.connect(h.getLevel(), h.absolutePos(GENERATOR), 0, h.absolutePos(POST), 0, WireType.CABLE).endsWith("wire.already")
                            && WireLinks.connect(h.getLevel(), h.absolutePos(GENERATOR), 0, h.absolutePos(GENERATOR), 0, WireType.CABLE).endsWith("wire.same"), "A wire should not double up or loop onto its own end");
                    g.setPhase(90);
                    h.assertTrue(breaker.close().endsWith("refused.phase") && !breaker.closed(), "A breaker should not close across two sides that are out of phase");
                    for (int i = 0; i < 15; i++) {
                        g.raise();
                    }
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    g.setPhase(0);
                    h.assertTrue(breaker.close().endsWith("refused.frequency"), "A breaker should not close across two sides at different frequencies: " + g.frequency());
                    for (int i = 0; i < 10; i++) {
                        g.lower();
                    }
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    g.setPhase(0);
                    h.assertTrue(breaker.close().endsWith("breaker.closed") && breaker.closed(), "In step and in phase the breaker should close");
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(g.synced() && g.frequency() == Power.GRID_HZ && g.outputSu() == 0, "Once on the grid the generator should be synchronised and unloaded: " + g.status());
                    g.raise();
                    h.assertTrue(Math.abs(g.load() - Power.LOAD_STEP) < 1e-9 && g.trim() == 0, "On the grid, raise should add load, not speed");
                    g.setLoad(0.5);
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    double half = Power.SECTION_RATED_SU / 2;
                    h.assertTrue(Math.abs(g.outputSu() - half) < half * 0.05, "Half load on one section should put half its rating on the shaft: " + g.outputSu());
                    h.assertTrue(g.calculateStressApplied() > 0 && !g.isOverStressed(), "The generator should load the shaft without stalling it");
                    h.assertTrue(Math.abs(grid.deliveredMw() - Power.megawatts(half)) < 1.0, "The grid should receive what the generator makes: " + grid.deliveredMw());
                    breaker.open();
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(!g.synced() && g.outputSu() == 0 && g.calculateStressApplied() == 0, "Opening the breaker should take the load off the shaft: " + g.outputSu());
                    g.setPhase(0);
                    breaker.close();
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(g.synced(), "The generator should go back on the grid");
                    h.setBlock(POST, Blocks.AIR);
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(!g.synced() && g.wires().isEmpty() && breaker.wires().count(BreakerBlockEntity.LOAD) == 0, "Breaking an insulator should take its wires down and cut the generator off");
                    h.assertTrue(breaker.wires().count(BreakerBlockEntity.LINE) == 1, "The wire on the other terminal should stay");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void startingAGeneratorAgainstALiveLineTripsTheBreaker(GameTestHelper h) {
        line(h, 1, 0);
        GeneratorBlockEntity g = h.getBlockEntity(GENERATOR);
        BreakerBlockEntity breaker = h.getBlockEntity(BREAKER);
        CreativeMotorBlockEntity motor = h.getBlockEntity(MOTOR);
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    h.assertTrue(!g.energised(), "A generator with a still shaft should be dead");
                    h.assertTrue(breaker.close().endsWith("breaker.closed"), "A breaker should close onto a dead side");
                    motor.generatedSpeed.setValue((int) Power.SYNC_RPM);
                })
                .thenIdle(30)
                .thenExecute(() -> {
                    h.assertTrue(!breaker.closed() && breaker.tripReason().endsWith("not_synchronised"), "Starting a machine against a live line should trip the breaker: " + breaker.tripReason());
                    h.assertTrue(g.energised() && !g.synced(), "The generator should be left running by itself");
                    h.assertTrue(breaker.locked() && breaker.close().endsWith("refused.locked"), "A breaker tripped by its protection should lock out until it is reset");
                    breaker.reset();
                    h.setBlock(BREAKER.north(), Blocks.REDSTONE_BLOCK);
                    g.setPhase(0);
                    breaker.close();
                })
                .thenIdle(10)
                .thenExecute(() -> h.assertTrue(h.getBlockState(BREAKER).getValue(BreakerBlock.CLOSED), "A breaker closed by hand stays closed until a signal changes"))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 700)
    public static void sectionsMakeOneMachineAndPowerOrdersAreMetByHoldingLoad(GameTestHelper h) {
        line(h, 2, (int) Power.SYNC_RPM);
        GeneratorBlockEntity g = h.getBlockEntity(GENERATOR);
        GeneratorBlockEntity second = h.getBlockEntity(GENERATOR.east());
        BreakerBlockEntity breaker = h.getBlockEntity(BREAKER);
        GridTieBlockEntity grid = h.getBlockEntity(GRID);
        double target = Power.megawatts(Power.SECTION_RATED_SU);
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(second.master() == g && g.sectionCount() == 2 && g.ratedSu() == 2 * Power.SECTION_RATED_SU, "Two sections in line should be one machine of twice the rating: " + g.sectionCount());
                    g.setPhase(0);
                    breaker.close();
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    grid.placeOrder(target);
                    g.setLoad(0.5);
                })
                .thenIdle(100)
                .thenExecute(() -> {
                    h.assertTrue(grid.ordering() && grid.heldTicks() > 0, "An order should count the time its demand is met: " + grid.deliveredMw() + " for " + grid.orderMw());
                    h.assertTrue(Math.abs(second.calculateStressApplied() - g.calculateStressApplied()) < 1e-3 && g.calculateStressApplied() > 0, "The sections should share the load evenly");
                })
                .thenIdle(Power.ORDER_HOLD_TICKS)
                .thenExecute(() -> h.assertTrue(grid.completed() == 1 && grid.missed() == 0 && !grid.ordering(), "Holding the ordered power for the hold time should complete the order: " + grid.completed()))
                .thenSucceed();
    }

    private static MotorBlockEntity motor(GameTestHelper h, BlockPos pos, int rpm, int presses) {
        h.setBlock(pos, CMBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(DirectionalKineticBlock.FACING, Direction.EAST));
        for (int i = 1; i <= presses; i++) {
            press(h, pos.east(i));
        }
        MotorBlockEntity motor = h.getBlockEntity(pos);
        motor.setSpeed(rpm);
        return motor;
    }

    private static void press(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, AllBlocks.MECHANICAL_PRESS.getDefaultState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
    }

    private static double pressLoad(int presses, int rpm) {
        return presses * BlockStressValues.getImpact(AllBlocks.MECHANICAL_PRESS.get()) * rpm;
    }

    private static double drawFor(double shaftLoad) {
        return shaftLoad / Power.MOTOR_EFFICIENCY + Power.MOTOR_RATED_SU * Power.MOTOR_IDLE;
    }

    private static boolean near(double value, double target) {
        return Math.abs(value - target) <= Math.max(4.0, target * 0.05);
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void aMotorRunsOnGridPowerAndTheGridCountsWhatItDraws(GameTestHelper h) {
        BlockPos gridPos = new BlockPos(1, 2, 1);
        BlockPos stepDown = new BlockPos(3, 2, 1);
        BlockPos motorPos = new BlockPos(1, 2, 3);
        h.setBlock(gridPos, CMBlocks.GRID_CONNECTION.get());
        TransformerBlockEntity t = transformer(h, stepDown, STARTUP);
        wire(h, WireType.LINE, gridPos, 0, stepDown, TransformerBlockEntity.HIGH);
        MotorBlockEntity motor = motor(h, motorPos, 16, 2);
        GridTieBlockEntity grid = h.getBlockEntity(gridPos);
        double draw = drawFor(pressLoad(2, 16));
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(!motor.live() && motor.getSpeed() == 0 && motor.status().endsWith("motor.dead"), "A motor with no wire should stand still: " + motor.status());
                    h.assertTrue(Math.abs(t.lowSide() - Power.MOTOR_KV) < 0.01 && Math.abs(t.highSide() - Power.GRID_KV) < 0.01, "A 220 / 6 kV transformer on the grid should give 6 kV: " + t.lowSide());
                    wire(h, WireType.COPPER, stepDown, TransformerBlockEntity.LOW, motorPos, 0);
                })
                .thenIdle(50)
                .thenExecute(() -> {
                    h.assertTrue(motor.live() && Math.abs(motor.getSpeed()) == 16, "A motor on 6 kV should turn at its set speed: " + motor.status());
                    h.assertTrue(near(motor.demandSu(), draw), "A motor should draw what its shaft load takes, plus its losses: " + motor.demandSu() + " for " + draw);
                    h.assertTrue(near(-Power.su(grid.deliveredMw()), draw), "The grid should count what the plant draws from it: " + grid.deliveredMw());
                    h.assertTrue(near(Power.su(t.throughMw()), draw), "The transformer should carry what the motor draws: " + t.throughMw());
                    h.setBlock(motorPos.north(), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(30)
                .thenExecute(() -> {
                    h.assertTrue(!motor.live() && motor.getSpeed() == 0 && motor.status().endsWith("motor.held"), "A redstone signal should stop a motor: " + motor.status());
                    h.assertTrue(motor.demandSu() < 2, "A stopped motor should draw nothing: " + motor.demandSu());
                    h.setBlock(motorPos.north(), Blocks.AIR);
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(motor.live(), "A motor should start again when the signal goes");
                    t.setRatio(MAIN);
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(h.getBlockState(motorPos).isAir() || !h.getBlockState(motorPos).is(CMBlocks.ELECTRIC_MOTOR.get()), "A 6 kV motor given 20 kV should burn out");
                    h.assertTrue(t.wires().count(TransformerBlockEntity.LOW) == 0, "A copper wire given 20 kV should flash over and be lost");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void aGeneratorAloneCarriesItsBusAndItsBreakerTripsOnOverload(GameTestHelper h) {
        BlockPos gridPos = new BlockPos(0, 2, 0);
        BlockPos startup = new BlockPos(2, 2, 0);
        BlockPos driverPos = new BlockPos(0, 2, 2);
        BlockPos generatorPos = new BlockPos(1, 2, 2);
        BlockPos breakerPos = new BlockPos(1, 2, 5);
        BlockPos unit = new BlockPos(3, 2, 6);
        BlockPos busPos = new BlockPos(4, 2, 4);
        BlockPos loadPos = new BlockPos(2, 4, 2);
        h.setBlock(gridPos, CMBlocks.GRID_CONNECTION.get());
        transformer(h, startup, STARTUP);
        MotorBlockEntity driver = motor(h, driverPos, (int) Power.SYNC_RPM, 0);
        h.setBlock(generatorPos, CMBlocks.GENERATOR.get().defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
        h.setBlock(breakerPos, CMBlocks.CIRCUIT_BREAKER.get());
        transformer(h, unit, UNIT);
        h.setBlock(busPos, CMBlocks.BUS_BAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        h.setBlock(busPos.east(), CMBlocks.BUS_BAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        MotorBlockEntity load = motor(h, loadPos, (int) Power.SYNC_RPM, 1);
        wire(h, WireType.LINE, gridPos, 0, startup, TransformerBlockEntity.HIGH);
        wire(h, WireType.COPPER, startup, TransformerBlockEntity.LOW, driverPos, 0);
        wire(h, generatorPos, 0, breakerPos, BreakerBlockEntity.LOAD);
        wire(h, breakerPos, BreakerBlockEntity.LINE, unit, TransformerBlockEntity.HIGH);
        wire(h, unit, TransformerBlockEntity.LOW, busPos, 0);
        wire(h, WireType.COPPER, busPos.east(), 0, loadPos, 0);
        GeneratorBlockEntity g = h.getBlockEntity(generatorPos);
        BreakerBlockEntity breaker = h.getBlockEntity(breakerPos);
        BusBarBlockEntity bus = h.getBlockEntity(busPos);
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(driver.live() && g.online() && !g.synced() && Math.abs(g.frequency() - Power.GRID_HZ) < 1e-6, "A generator turned by a motor should run by itself at 50 Hz: " + g.status());
                    h.assertTrue(!bus.live() && !load.live(), "A bus behind an open breaker should be dead");
                    h.assertTrue(breaker.close().endsWith("breaker.closed"), "A breaker should close onto a dead bus");
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    double draw = drawFor(pressLoad(1, (int) Power.SYNC_RPM));
                    h.assertTrue(bus.live() && Math.abs(bus.kilovolts() - Power.MOTOR_KV) < 0.01, "A 20 / 6 kV transformer should put 6 kV on the bus: " + bus.kilovolts());
                    h.assertTrue(load.live() && Math.abs(load.getSpeed()) == Power.SYNC_RPM, "Two bars in a line should be one bus and feed the motor wired to the far one: " + load.status());
                    h.assertTrue(near(load.demandSu(), draw) && near(g.outputSu(), draw), "A generator alone should make what its bus draws: " + g.outputSu() + " for " + load.demandSu());
                    h.assertTrue(near(bus.loadSu(), draw) && g.status().endsWith("generator.supplying"), "The bus should read its load: " + bus.loadSu());
                    h.assertTrue(driver.demandSu() > g.outputSu(), "Turning a generator with a motor should cost more than it makes");
                    press(h, loadPos.east(2));
                    press(h, loadPos.east(3));
                    press(h, loadPos.east(4));
                })
                .thenIdle(80)
                .thenExecute(() -> {
                    h.assertTrue(!breaker.closed() && breaker.tripReason().endsWith("trip.overload"), "More load than the shaft can carry should trip the generator's breaker: " + breaker.tripReason());
                    h.assertTrue(!bus.live() && !load.live() && g.online(), "The trip should leave the bus dead and the generator running");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void twoGeneratorsShareABusOnceSynchronised(GameTestHelper h) {
        BlockPos[] drivers = {new BlockPos(0, 2, 0), new BlockPos(0, 2, 6)};
        BlockPos[] breakers = {new BlockPos(3, 2, 0), new BlockPos(3, 2, 6)};
        BlockPos busPos = new BlockPos(5, 2, 3);
        BlockPos unit = new BlockPos(6, 2, 5);
        BlockPos loadPos = new BlockPos(3, 4, 3);
        h.setBlock(busPos, CMBlocks.BUS_BAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
        transformer(h, unit, UNIT);
        for (int i = 0; i < 2; i++) {
            h.setBlock(drivers[i], AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.EAST));
            h.setBlock(drivers[i].east(), CMBlocks.GENERATOR.get().defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
            h.setBlock(breakers[i], CMBlocks.CIRCUIT_BREAKER.get());
            wire(h, drivers[i].east(), 0, breakers[i], BreakerBlockEntity.LOAD);
            wire(h, breakers[i], BreakerBlockEntity.LINE, busPos, 0);
            CreativeMotorBlockEntity driver = h.getBlockEntity(drivers[i]);
            driver.generatedSpeed.setValue((int) Power.SYNC_RPM);
        }
        MotorBlockEntity load = motor(h, loadPos, (int) Power.SYNC_RPM, 2);
        wire(h, busPos, 0, unit, TransformerBlockEntity.HIGH);
        wire(h, WireType.COPPER, unit, TransformerBlockEntity.LOW, loadPos, 0);
        GeneratorBlockEntity[] g = {h.getBlockEntity(drivers[0].east()), h.getBlockEntity(drivers[1].east())};
        BreakerBlockEntity[] b = {h.getBlockEntity(breakers[0]), h.getBlockEntity(breakers[1])};
        double draw = drawFor(pressLoad(2, (int) Power.SYNC_RPM));
        int[] lead = new int[1];
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> h.assertTrue(b[0].close().endsWith("breaker.closed"), "The first generator should close onto the dead bus"))
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(g[0].frequency() < Power.GRID_HZ - Power.SYNC_HZ, "A generator carrying load by itself should run below 50 Hz: " + g[0].frequency());
                    h.assertTrue(b[1].close().endsWith("refused.frequency"), "A second generator faster than the loaded bus should be refused");
                    for (int i = (int) Math.round((Power.GRID_HZ - g[0].frequency()) / Power.TRIM_STEP); i > 0; i--) {
                        g[1].lower();
                    }
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    g[1].setPhase(90);
                    h.assertTrue(b[1].close().endsWith("refused.phase"), "A second generator out of phase with the bus should be refused");
                    g[1].setPhase(g[0].phase());
                    h.assertTrue(b[1].close().endsWith("breaker.closed"), "In phase with the bus the second generator should close on");
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(g[0].online() && g[1].online() && g[0].synced() != g[1].synced(), "One generator should lead the bus and the other follow it: " + g[0].status() + ", " + g[1].status());
                    h.assertTrue(near(g[0].outputSu(), draw / 2) && near(g[1].outputSu(), draw / 2), "Equal generators should share the bus evenly: " + g[0].outputSu() + " and " + g[1].outputSu());
                    lead[0] = g[0].synced() ? 1 : 0;
                    CreativeMotorBlockEntity driver = h.getBlockEntity(drivers[lead[0]]);
                    driver.generatedSpeed.setValue(0);
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    GeneratorBlockEntity left = g[1 - lead[0]];
                    h.assertTrue(load.live() && left.online() && !left.synced() && near(left.outputSu(), draw), "When the leading generator stops the other should take the whole bus: " + left.outputSu());
                    CreativeMotorBlockEntity driver = h.getBlockEntity(drivers[lead[0]]);
                    driver.generatedSpeed.setValue((int) Power.SYNC_RPM);
                })
                .thenIdle(40)
                .thenExecute(() -> h.assertTrue(!b[lead[0]].closed() && b[lead[0]].tripReason().endsWith("not_synchronised"), "Starting it again onto the live bus should trip its breaker: " + b[lead[0]].tripReason()))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void equipmentOnTheWrongVoltageIsLost(GameTestHelper h) {
        BlockPos gridPos = new BlockPos(3, 2, 3);
        BlockPos motorPos = new BlockPos(1, 2, 5);
        BlockPos generatorPos = new BlockPos(5, 2, 5);
        BlockPos breakerPos = new BlockPos(5, 2, 1);
        BlockPos aloneDriver = new BlockPos(0, 4, 1);
        BlockPos post = new BlockPos(1, 3, 1);
        h.setBlock(gridPos, CMBlocks.GRID_CONNECTION.get());
        motor(h, motorPos, 16, 0);
        h.setBlock(generatorPos, CMBlocks.GENERATOR.get().defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
        h.setBlock(breakerPos, CMBlocks.CIRCUIT_BREAKER.get());
        h.setBlock(aloneDriver, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.EAST));
        h.setBlock(aloneDriver.east(), CMBlocks.GENERATOR.get().defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
        h.setBlock(post.below(), Blocks.STONE);
        h.setBlock(post, CMBlocks.INSULATOR.get());
        CreativeMotorBlockEntity driver = h.getBlockEntity(aloneDriver);
        driver.generatedSpeed.setValue((int) Power.SYNC_RPM);
        wire(h, WireType.LINE, gridPos, 0, breakerPos, BreakerBlockEntity.LINE);
        wire(h, aloneDriver.east(), 0, breakerPos, BreakerBlockEntity.LOAD);
        GridTieBlockEntity grid = h.getBlockEntity(gridPos);
        BreakerBlockEntity breaker = h.getBlockEntity(breakerPos);
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(breaker.close().endsWith("refused.voltage") && !breaker.closed(), "A breaker should not close 20 kV onto 220 kV");
                    wire(h, WireType.COPPER, gridPos, 0, post, 0);
                })
                .thenIdle(12)
                .thenExecute(() -> {
                    h.assertTrue(grid.wires().count(0) == 1, "A copper wire on 220 kV should flash over and be lost: " + grid.wires().count(0));
                    wire(h, WireType.LINE, gridPos, 0, motorPos, 0);
                })
                .thenIdle(12)
                .thenExecute(() -> {
                    h.assertTrue(!h.getBlockState(motorPos).is(CMBlocks.ELECTRIC_MOTOR.get()), "A 6 kV motor wired to 220 kV should be lost");
                    wire(h, WireType.LINE, gridPos, 0, generatorPos, 0);
                })
                .thenIdle(12)
                .thenExecute(() -> {
                    h.assertTrue(!h.getBlockState(generatorPos).is(CMBlocks.GENERATOR.get()), "A 20 kV generator wired to 220 kV should be lost");
                    h.assertTrue(h.getBlockState(aloneDriver.east()).is(CMBlocks.GENERATOR.get()), "The generator behind the open breaker should be safe");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void anOverloadedWireHeatsAndBurnsThrough(GameTestHelper h) {
        BlockPos gridPos = new BlockPos(0, 2, 0);
        BlockPos startup = new BlockPos(2, 2, 0);
        BlockPos busPos = new BlockPos(5, 2, 0);
        BlockPos[] motors = {new BlockPos(0, 2, 3), new BlockPos(0, 4, 3)};
        h.setBlock(gridPos, CMBlocks.GRID_CONNECTION.get());
        TransformerBlockEntity t = transformer(h, startup, STARTUP);
        h.setBlock(busPos, CMBlocks.BUS_BAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        wire(h, WireType.LINE, gridPos, 0, startup, TransformerBlockEntity.HIGH);
        wire(h, WireType.COPPER, startup, TransformerBlockEntity.LOW, busPos, 0);
        MotorBlockEntity[] m = new MotorBlockEntity[2];
        for (int i = 0; i < 2; i++) {
            m[i] = motor(h, motors[i], (int) Power.SYNC_RPM, 4);
            wire(h, busPos, 0, motors[i], 0);
        }
        BusBarBlockEntity bus = h.getBlockEntity(busPos);
        h.startSequence()
                .thenIdle(70)
                .thenExecute(() -> {
                    h.assertTrue(m[0].live() && m[1].live() && bus.live(), "Both motors should start on the bus");
                    WireLinks.Link feed = t.wires().find(h.absolutePos(busPos), TransformerBlockEntity.LOW, 0);
                    WireLinks.Link back = bus.wires().find(h.absolutePos(startup), 0, TransformerBlockEntity.LOW);
                    h.assertTrue(feed != null && back != null, "The feed should still be up after three seconds");
                    double warmth = Math.max(t.wires().heat(feed), bus.wires().heat(back));
                    h.assertTrue(warmth > 0.15 && warmth < 1, "A copper wire carrying two motors should be heating: " + warmth);
                })
                .thenIdle(200)
                .thenExecute(() -> {
                    h.assertTrue(t.wires().count(TransformerBlockEntity.LOW) == 0 && bus.wires().count(0) == 2, "The overloaded wire should burn through and the others stay");
                    h.assertTrue(!bus.live() && !m[0].live() && !m[1].live(), "Losing the feed should leave the bus dead");
                })
                .thenSucceed();
    }

    private static GeneratorBlockEntity bigUnit(GameTestHelper h, BlockPos breakerPos) {
        BlockPos driverPos = new BlockPos(0, 2, 2);
        h.setBlock(driverPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.EAST));
        for (int i = 1; i <= 4; i++) {
            h.setBlock(driverPos.east(i), CMBlocks.GENERATOR.get().defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
        }
        h.setBlock(breakerPos, CMBlocks.CIRCUIT_BREAKER.get());
        wire(h, driverPos.east(), 0, breakerPos, BreakerBlockEntity.LOAD);
        CreativeMotorBlockEntity driver = h.getBlockEntity(driverPos);
        driver.generatedSpeed.setValue((int) Power.SYNC_RPM);
        return h.getBlockEntity(driverPos.east());
    }

    @GameTest(template = EMPTY, timeoutTicks = 800)
    public static void anOverloadedTransformerOverheatsAndExplodes(GameTestHelper h) {
        BlockPos breakerPos = new BlockPos(1, 4, 4);
        BlockPos stepUp = new BlockPos(4, 4, 4);
        BlockPos gridPos = new BlockPos(6, 4, 4);
        GeneratorBlockEntity g = bigUnit(h, breakerPos);
        TransformerBlockEntity t = transformer(h, stepUp, MAIN);
        h.setBlock(gridPos, CMBlocks.GRID_CONNECTION.get());
        wire(h, breakerPos, BreakerBlockEntity.LINE, stepUp, TransformerBlockEntity.LOW);
        wire(h, WireType.LINE, stepUp, TransformerBlockEntity.HIGH, gridPos, 0);
        BreakerBlockEntity breaker = h.getBlockEntity(breakerPos);
        double full = Power.megawatts(4 * Power.SECTION_RATED_SU);
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    g.setPhase(0);
                    h.assertTrue(breaker.close().endsWith("breaker.closed"), "The unit should synchronise through its step-up transformer");
                })
                .thenIdle(10)
                .thenExecute(() -> g.setLoad(1.0))
                .thenIdle(170)
                .thenExecute(() -> {
                    h.assertTrue(Math.abs(t.throughMw() - full) < full * 0.05, "The transformer should carry the whole unit: " + t.throughMw() + " of " + full);
                    h.assertTrue(t.heat() > 0.2 && t.heat() < 1 && t.comparator() > 0, "A transformer above its rating should heat up: " + t.heat());
                })
                .thenIdle(400)
                .thenExecute(() -> {
                    h.assertTrue(!h.getBlockState(stepUp).is(CMBlocks.TRANSFORMER.get()), "A transformer left overloaded should explode");
                    h.assertTrue(!g.synced() && g.outputSu() == 0, "Losing the transformer should take the unit off the grid: " + g.status());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void transformersSideBySideShareTheLoad(GameTestHelper h) {
        BlockPos breakerPos = new BlockPos(1, 4, 4);
        BlockPos busPos = new BlockPos(3, 4, 4);
        BlockPos[] stepUps = {new BlockPos(5, 4, 2), new BlockPos(5, 4, 6)};
        BlockPos gridPos = new BlockPos(7, 4, 4);
        GeneratorBlockEntity g = bigUnit(h, breakerPos);
        h.setBlock(busPos, CMBlocks.BUS_BAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        h.setBlock(gridPos, CMBlocks.GRID_CONNECTION.get());
        wire(h, breakerPos, BreakerBlockEntity.LINE, busPos, 0);
        TransformerBlockEntity[] t = new TransformerBlockEntity[2];
        for (int i = 0; i < 2; i++) {
            t[i] = transformer(h, stepUps[i], MAIN);
            wire(h, busPos, 0, stepUps[i], TransformerBlockEntity.LOW);
            wire(h, WireType.LINE, stepUps[i], TransformerBlockEntity.HIGH, gridPos, 0);
        }
        BreakerBlockEntity breaker = h.getBlockEntity(breakerPos);
        GridTieBlockEntity grid = h.getBlockEntity(gridPos);
        double full = Power.megawatts(4 * Power.SECTION_RATED_SU);
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    g.setPhase(0);
                    h.assertTrue(breaker.close().endsWith("breaker.closed"), "The unit should synchronise through two transformers side by side");
                })
                .thenIdle(10)
                .thenExecute(() -> g.setLoad(1.0))
                .thenIdle(200)
                .thenExecute(() -> {
                    h.assertTrue(Math.abs(t[0].throughMw() - full / 2) < full * 0.05 && Math.abs(t[1].throughMw() - full / 2) < full * 0.05,
                            "Two transformers side by side should each carry half: " + t[0].throughMw() + " and " + t[1].throughMw());
                    h.assertTrue(t[0].heat() == 0 && t[1].heat() == 0 && Math.abs(grid.deliveredMw() - full) < full * 0.05, "Neither should heat, and the grid should get the whole unit: " + grid.deliveredMw());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void aGeneratorShedsLoadWhenItsShaftSagsAndStaysOnTheGrid(GameTestHelper h) {
        BlockPos gridPos = new BlockPos(0, 2, 0);
        BlockPos startup = new BlockPos(2, 2, 0);
        BlockPos driverPos = new BlockPos(0, 2, 3);
        BlockPos generatorPos = new BlockPos(1, 2, 3);
        BlockPos breakerPos = new BlockPos(3, 4, 5);
        BlockPos stepUp = new BlockPos(5, 2, 0);
        h.setBlock(gridPos, CMBlocks.GRID_CONNECTION.get());
        transformer(h, startup, STARTUP);
        transformer(h, stepUp, MAIN);
        motor(h, driverPos, (int) Power.SYNC_RPM, 0);
        h.setBlock(generatorPos, CMBlocks.GENERATOR.get().defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
        h.setBlock(breakerPos, CMBlocks.CIRCUIT_BREAKER.get());
        wire(h, WireType.LINE, gridPos, 0, startup, TransformerBlockEntity.HIGH);
        wire(h, WireType.COPPER, startup, TransformerBlockEntity.LOW, driverPos, 0);
        wire(h, WireType.LINE, gridPos, 0, stepUp, TransformerBlockEntity.HIGH);
        wire(h, stepUp, TransformerBlockEntity.LOW, breakerPos, BreakerBlockEntity.LINE);
        wire(h, generatorPos, 0, breakerPos, BreakerBlockEntity.LOAD);
        GeneratorBlockEntity g = h.getBlockEntity(generatorPos);
        BreakerBlockEntity breaker = h.getBlockEntity(breakerPos);
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    g.setPhase(0);
                    h.assertTrue(breaker.close().endsWith("breaker.closed"), "The generator should go on the grid");
                })
                .thenIdle(10)
                .thenExecute(() -> g.setLoad(1.0))
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(g.synced() && near(g.outputSu(), Power.MOTOR_RATED_SU * Power.SHAFT_SHARE), "Asked for more than its shaft has, a generator should take nine tenths of it: " + g.outputSu());
                    press(h, generatorPos.east());
                    press(h, generatorPos.east(2));
                })
                .thenIdle(80)
                .thenExecute(() -> {
                    double left = (Power.MOTOR_RATED_SU - pressLoad(2, (int) Power.SYNC_RPM)) * Power.SHAFT_SHARE;
                    h.assertTrue(breaker.closed() && g.synced(), "A sagging shaft should not trip the generator off the grid: " + breaker.tripReason());
                    h.assertTrue(near(g.outputSu(), left) && !g.isOverStressed(), "It should shed load to what the shaft has left: " + g.outputSu() + " for " + left);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void aBreakerSetBelowItsWireTripsBeforeTheWireBurns(GameTestHelper h) {
        BlockPos gridPos = new BlockPos(0, 2, 0);
        BlockPos startup = new BlockPos(2, 2, 0);
        BlockPos breakerPos = new BlockPos(4, 4, 0);
        BlockPos busPos = new BlockPos(6, 2, 0);
        BlockPos[] motors = {new BlockPos(0, 2, 3), new BlockPos(0, 4, 3)};
        h.setBlock(gridPos, CMBlocks.GRID_CONNECTION.get());
        transformer(h, startup, STARTUP);
        h.setBlock(breakerPos, CMBlocks.CIRCUIT_BREAKER.get());
        h.setBlock(busPos, CMBlocks.BUS_BAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        wire(h, WireType.LINE, gridPos, 0, startup, TransformerBlockEntity.HIGH);
        wire(h, startup, TransformerBlockEntity.LOW, breakerPos, BreakerBlockEntity.LINE);
        wire(h, WireType.COPPER, breakerPos, BreakerBlockEntity.LOAD, busPos, 0);
        for (BlockPos motorPos : motors) {
            motor(h, motorPos, (int) Power.SYNC_RPM, 4);
            wire(h, busPos, 0, motorPos, 0);
        }
        BreakerBlockEntity breaker = h.getBlockEntity(breakerPos);
        BusBarBlockEntity bus = h.getBlockEntity(busPos);
        breaker.setPickupKiloamps(1.0);
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> h.assertTrue(breaker.close().endsWith("breaker.closed"), "The feeder breaker should close onto the dead bus"))
                .thenIdle(120)
                .thenExecute(() -> {
                    h.assertTrue(!breaker.closed() && breaker.tripReason().endsWith("trip.overcurrent") && breaker.locked(), "A breaker set to its wire's limit should trip on two motors' current: " + breaker.tripReason());
                    h.assertTrue(bus.wires().count(0) == 3 && !bus.live(), "It should open before the copper wire burns, leaving the bus dead and the wire whole");
                    h.assertTrue(breaker.close().endsWith("refused.locked"), "A tripped breaker should not close until it is reset");
                    h.assertTrue(breaker.reset().endsWith("breaker.reset") && breaker.close().endsWith("breaker.closed"), "Reset, it should close again");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void anEarthedLiveLineTripsItsBreakerAndUnprotectedGearIsLost(GameTestHelper h) {
        BlockPos gridPos = new BlockPos(0, 2, 0);
        BlockPos stepDown = new BlockPos(2, 2, 0);
        BlockPos breakerPos = new BlockPos(4, 2, 0);
        BlockPos busPos = new BlockPos(6, 2, 0);
        BlockPos safeEarth = new BlockPos(6, 2, 3);
        BlockPos bareEarth = new BlockPos(2, 2, 3);
        h.setBlock(gridPos, CMBlocks.GRID_CONNECTION.get());
        transformer(h, stepDown, MAIN);
        h.setBlock(breakerPos, CMBlocks.CIRCUIT_BREAKER.get());
        h.setBlock(busPos, CMBlocks.BUS_BAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        h.setBlock(safeEarth, CMBlocks.EARTHING_SWITCH.get());
        h.setBlock(bareEarth, CMBlocks.EARTHING_SWITCH.get());
        wire(h, WireType.LINE, gridPos, 0, stepDown, TransformerBlockEntity.HIGH);
        wire(h, stepDown, TransformerBlockEntity.LOW, breakerPos, BreakerBlockEntity.LINE);
        wire(h, breakerPos, BreakerBlockEntity.LOAD, busPos, 0);
        wire(h, busPos, 0, safeEarth, 0);
        wire(h, stepDown, TransformerBlockEntity.LOW, bareEarth, 0);
        BreakerBlockEntity breaker = h.getBlockEntity(breakerPos);
        BusBarBlockEntity bus = h.getBlockEntity(busPos);
        EarthingBlockEntity earth = h.getBlockEntity(safeEarth);
        EarthingBlockEntity bare = h.getBlockEntity(bareEarth);
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> h.assertTrue(breaker.close().endsWith("breaker.closed"), "The breaker should close onto the dead bus"))
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(bus.live() && Math.abs(bus.kilovolts() - Power.GENERATOR_KV) < 0.01, "The bus should be live at 20 kV: " + bus.kilovolts());
                    earth.toggle();
                })
                .thenIdle(30)
                .thenExecute(() -> {
                    h.assertTrue(!breaker.closed() && breaker.locked(), "Earthing a live bus is a short circuit and should trip its breaker at once: " + breaker.tripReason());
                    h.assertTrue(h.getBlockState(safeEarth).is(CMBlocks.EARTHING_SWITCH.get()) && h.getBlockState(stepDown).is(CMBlocks.TRANSFORMER.get()) && !bus.live(),
                            "A breaker that clears the fault should save the switch and the transformer");
                    h.assertTrue(breaker.reset().endsWith("breaker.reset") && breaker.close().endsWith("breaker.closed"), "A breaker should close onto an earthed line");
                })
                .thenIdle(30)
                .thenExecute(() -> {
                    h.assertTrue(!breaker.closed(), "Closing onto an earthed line should trip the breaker again and hurt nothing");
                    h.assertTrue(h.getBlockState(stepDown).is(CMBlocks.TRANSFORMER.get()), "The transformer should still stand");
                    bare.toggle();
                })
                .thenIdle(40)
                .thenExecute(() -> h.assertTrue(!h.getBlockState(stepDown).is(CMBlocks.TRANSFORMER.get()) || !h.getBlockState(bareEarth).is(CMBlocks.EARTHING_SWITCH.get()),
                        "A short circuit with no breaker to clear it should cost equipment"))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void aDisconnectSwitchIsWorkedDeadOrItArcs(GameTestHelper h) {
        BlockPos gridPos = new BlockPos(0, 2, 0);
        BlockPos startup = new BlockPos(2, 2, 0);
        BlockPos breakerPos = new BlockPos(4, 2, 0);
        BlockPos switchPos = new BlockPos(6, 2, 0);
        BlockPos motorPos = new BlockPos(1, 2, 4);
        h.setBlock(gridPos, CMBlocks.GRID_CONNECTION.get());
        transformer(h, startup, STARTUP);
        h.setBlock(breakerPos, CMBlocks.CIRCUIT_BREAKER.get());
        h.setBlock(switchPos, CMBlocks.DISCONNECT_SWITCH.get());
        MotorBlockEntity motor = motor(h, motorPos, 16, 2);
        wire(h, WireType.LINE, gridPos, 0, startup, TransformerBlockEntity.HIGH);
        wire(h, startup, TransformerBlockEntity.LOW, breakerPos, BreakerBlockEntity.LINE);
        wire(h, breakerPos, BreakerBlockEntity.LOAD, switchPos, 0);
        wire(h, WireType.COPPER, switchPos, 1, motorPos, 0);
        BreakerBlockEntity breaker = h.getBlockEntity(breakerPos);
        DisconnectBlockEntity isolator = h.getBlockEntity(switchPos);
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    h.assertTrue(isolator.operate(null).endsWith("breaker.closed") && isolator.closed(), "A disconnect switch should close with both sides dead");
                    breaker.close();
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(motor.live() && isolator.current() > Power.ISOLATOR_BREAK_KA && breaker.current() > Power.ISOLATOR_BREAK_KA, "The motor's current should pass the switch and the breaker: " + isolator.current());
                    breaker.open();
                })
                .thenIdle(30)
                .thenExecute(() -> {
                    h.assertTrue(isolator.operate(null).endsWith("breaker.open") && h.getBlockState(switchPos).is(CMBlocks.DISCONNECT_SWITCH.get()), "With the breaker open the switch should open quietly");
                    h.assertTrue(isolator.operate(null).endsWith("breaker.closed"), "And close again dead");
                    breaker.close();
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(motor.live(), "The motor should be running again through the switch");
                    h.assertTrue(isolator.operate(null).endsWith("isolator.arc"), "Opening a disconnect switch under load should draw an arc");
                })
                .thenIdle(5)
                .thenExecute(() -> h.assertTrue(!h.getBlockState(switchPos).is(CMBlocks.DISCONNECT_SWITCH.get()) && !motor.live(), "The arc should cost the switch and leave the motor dead"))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void aBreakerAskedToBreakTooLargeAFaultFails(GameTestHelper h) {
        BlockPos gridPos = new BlockPos(0, 2, 3);
        BlockPos busPos = new BlockPos(4, 2, 3);
        BlockPos breakerPos = new BlockPos(6, 2, 3);
        BlockPos earthPos = new BlockPos(6, 2, 6);
        h.setBlock(gridPos, CMBlocks.GRID_CONNECTION.get());
        h.setBlock(busPos, CMBlocks.BUS_BAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
        h.setBlock(breakerPos, CMBlocks.CIRCUIT_BREAKER.get());
        h.setBlock(earthPos, CMBlocks.EARTHING_SWITCH.get());
        for (int z : new int[]{1, 3, 5}) {
            BlockPos unit = new BlockPos(2, 2, z);
            transformer(h, unit, MAIN);
            wire(h, WireType.LINE, gridPos, 0, unit, TransformerBlockEntity.HIGH);
            wire(h, unit, TransformerBlockEntity.LOW, busPos, 0);
        }
        wire(h, busPos, 0, breakerPos, BreakerBlockEntity.LINE);
        wire(h, breakerPos, BreakerBlockEntity.LOAD, earthPos, 0);
        BreakerBlockEntity breaker = h.getBlockEntity(breakerPos);
        EarthingBlockEntity earth = h.getBlockEntity(earthPos);
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    h.assertTrue(breaker.close().endsWith("breaker.closed"), "The feeder should close");
                    earth.toggle();
                })
                .thenIdle(20)
                .thenExecute(() -> h.assertTrue(!h.getBlockState(breakerPos).is(CMBlocks.CIRCUIT_BREAKER.get()), "Three transformers side by side give more fault than one breaker can break, and it should fail"))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void aBatteryChargesFromItsBusAndHoldsItUpWhenTheSupplyGoes(GameTestHelper h) {
        BlockPos gridPos = new BlockPos(0, 2, 0);
        BlockPos startup = new BlockPos(2, 2, 0);
        BlockPos breakerPos = new BlockPos(4, 2, 0);
        BlockPos busPos = new BlockPos(6, 2, 0);
        BlockPos batteryPos = new BlockPos(6, 2, 3);
        BlockPos motorPos = new BlockPos(1, 2, 5);
        h.setBlock(gridPos, CMBlocks.GRID_CONNECTION.get());
        transformer(h, startup, STARTUP);
        h.setBlock(breakerPos, CMBlocks.CIRCUIT_BREAKER.get());
        h.setBlock(busPos, CMBlocks.BUS_BAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        h.setBlock(batteryPos, CMBlocks.STATION_BATTERY.get());
        MotorBlockEntity motor = motor(h, motorPos, 16, 1);
        wire(h, WireType.LINE, gridPos, 0, startup, TransformerBlockEntity.HIGH);
        wire(h, startup, TransformerBlockEntity.LOW, breakerPos, BreakerBlockEntity.LINE);
        wire(h, breakerPos, BreakerBlockEntity.LOAD, busPos, 0);
        wire(h, busPos, 0, batteryPos, 0);
        wire(h, WireType.COPPER, busPos, 0, motorPos, 0);
        BreakerBlockEntity breaker = h.getBlockEntity(breakerPos);
        BusBarBlockEntity bus = h.getBlockEntity(busPos);
        BatteryBlockEntity battery = h.getBlockEntity(batteryPos);
        double draw = drawFor(pressLoad(1, 16));
        double[] held = new double[1];
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> breaker.close())
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(!battery.online() && battery.status().endsWith("battery.charging") && battery.charge() > 0, "A battery on a live bus should charge: " + battery.status());
                    h.assertTrue(near(bus.loadSu(), draw + Power.BATTERY_CHARGE_SU), "Charging should show as load on the bus: " + bus.loadSu());
                    battery.setCharge(0.5);
                    breaker.open();
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(battery.online() && battery.status().endsWith("battery.supplying") && near(battery.outputSu(), draw), "With the supply gone the battery should carry the bus: " + battery.outputSu());
                    h.assertTrue(bus.live() && Math.abs(bus.kilovolts() - Power.MOTOR_KV) < 0.01 && motor.live(), "The bus should stay at 6 kV and the motor keep turning");
                    h.assertTrue(battery.charge() < 0.5, "It should be running down: " + battery.charge());
                    held[0] = battery.charge();
                    battery.setCharge(0.0001);
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(!battery.online() && battery.status().endsWith("battery.flat") && !bus.live() && !motor.live(), "A flat battery should let the bus go dead: " + battery.status());
                    breaker.close();
                })
                .thenIdle(30)
                .thenExecute(() -> h.assertTrue(bus.live() && battery.status().endsWith("battery.charging") && motor.live(), "The supply returning should start the motor and charge the battery again"))
                .thenSucceed();
    }

    private static DieselEngineBlockEntity engine(GameTestHelper h, BlockPos pos, int fuel) {
        h.setBlock(pos, CMBlocks.DIESEL_ENGINE.get().defaultBlockState().setValue(DirectionalKineticBlock.FACING, Direction.EAST));
        DieselEngineBlockEntity engine = h.getBlockEntity(pos);
        if (fuel > 0) {
            engine.tank().fill(new FluidStack(CMFluids.FUEL_OIL.source.get(), fuel), IFluidHandler.FluidAction.EXECUTE);
        }
        return engine;
    }

    @GameTest(template = EMPTY, timeoutTicks = 500)
    public static void aDieselEngineStartsOnASignalBurnsFuelAndStopsWhenItRunsOut(GameTestHelper h) {
        BlockPos enginePos = new BlockPos(1, 2, 2);
        BlockPos generatorPos = enginePos.east();
        DieselEngineBlockEntity engine = engine(h, enginePos, 40);
        h.setBlock(generatorPos, CMBlocks.GENERATOR.get().defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
        GeneratorBlockEntity g = h.getBlockEntity(generatorPos);
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(!engine.running() && engine.getSpeed() == 0 && !g.energised(), "An engine that is not called should stand still");
                    h.setBlock(enginePos.above(), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(Power.DIESEL_START_TICKS / 2)
                .thenExecute(() -> h.assertTrue(!engine.running() && engine.status().endsWith("engine.starting"), "An engine should take time to start: " + engine.status()))
                .thenIdle(Power.DIESEL_START_TICKS + 100)
                .thenExecute(() -> {
                    h.assertTrue(engine.running() && Math.abs(engine.getSpeed()) == Power.SYNC_RPM, "Once started it should turn at 64 RPM: " + engine.getSpeed());
                    h.assertTrue(g.online() && Math.abs(g.frequency() - Power.GRID_HZ) < 1e-6, "A generator on its shaft should make 50 Hz");
                    h.assertTrue(engine.tank().getFluidAmount() < 40, "It should be burning fuel: " + engine.tank().getFluidAmount());
                    h.setBlock(enginePos.above(), Blocks.AIR);
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(!engine.running() && engine.getSpeed() == 0, "Without its signal the engine should stop");
                    engine.tank().drain(engine.tank().getFluidAmount() - 1, IFluidHandler.FluidAction.EXECUTE);
                    h.setBlock(enginePos.above(), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(Power.DIESEL_START_TICKS + 120)
                .thenExecute(() -> h.assertTrue(!engine.running() && engine.status().endsWith("engine.no_fuel") && !g.energised(), "An engine out of fuel should stop and say so: " + engine.status()))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 700)
    public static void aTransferSwitchStartsTheStandbySetAndMovesTheBusOver(GameTestHelper h) {
        BlockPos gridPos = new BlockPos(0, 2, 0);
        BlockPos startup = new BlockPos(2, 2, 0);
        BlockPos switchPos = new BlockPos(4, 2, 3);
        BlockPos enginePos = new BlockPos(5, 2, 3);
        BlockPos generatorPos = enginePos.east();
        BlockPos unit = new BlockPos(6, 2, 6);
        BlockPos busPos = new BlockPos(2, 2, 5);
        BlockPos motorPos = new BlockPos(1, 4, 3);
        h.setBlock(gridPos, CMBlocks.GRID_CONNECTION.get());
        transformer(h, startup, STARTUP);
        h.setBlock(switchPos, CMBlocks.TRANSFER_SWITCH.get());
        DieselEngineBlockEntity engine = engine(h, enginePos, 2000);
        h.setBlock(generatorPos, CMBlocks.GENERATOR.get().defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
        transformer(h, unit, UNIT);
        h.setBlock(busPos, CMBlocks.BUS_BAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        MotorBlockEntity motor = motor(h, motorPos, 16, 1);
        wire(h, WireType.LINE, gridPos, 0, startup, TransformerBlockEntity.HIGH);
        wire(h, startup, TransformerBlockEntity.LOW, switchPos, TransferSwitchBlockEntity.NORMAL);
        wire(h, generatorPos, 0, unit, TransformerBlockEntity.HIGH);
        wire(h, unit, TransformerBlockEntity.LOW, switchPos, TransferSwitchBlockEntity.STANDBY);
        wire(h, switchPos, TransferSwitchBlockEntity.LOAD, busPos, 0);
        wire(h, WireType.COPPER, busPos, 0, motorPos, 0);
        TransferSwitchBlockEntity transfer = h.getBlockEntity(switchPos);
        BusBarBlockEntity bus = h.getBlockEntity(busPos);
        GeneratorBlockEntity g = h.getBlockEntity(generatorPos);
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(transfer.position() == TransferSwitchBlock.Position.NORMAL && !transfer.calling() && !engine.running(), "With the normal supply live the switch should sit on it and leave the engine alone: " + transfer.position());
                    h.assertTrue(bus.live() && motor.live(), "The bus should be fed through the switch");
                    WireLinks.cutAll(h.getLevel(), h.absolutePos(gridPos));
                })
                .thenIdle(40)
                .thenExecute(() -> h.assertTrue(transfer.calling() && transfer.position() == TransferSwitchBlock.Position.OFF && !bus.live() && engine.status().endsWith("engine.starting"),
                        "Losing the normal supply should open the switch and call the engine: " + transfer.position() + " " + engine.status()))
                .thenIdle(Power.DIESEL_START_TICKS + 60)
                .thenExecute(() -> {
                    h.assertTrue(engine.running() && g.online() && transfer.position() == TransferSwitchBlock.Position.STANDBY, "Once the set is up the switch should move to it: " + transfer.position());
                    h.assertTrue(bus.live() && motor.live() && near(g.outputSu(), motor.demandSu()), "The standby set should carry the bus: " + g.outputSu());
                    wire(h, WireType.LINE, gridPos, 0, startup, TransformerBlockEntity.HIGH);
                })
                .thenIdle(Power.TRANSFER_RETURN_TICKS + 80)
                .thenExecute(() -> {
                    h.assertTrue(transfer.position() == TransferSwitchBlock.Position.NORMAL && !transfer.calling(), "With the normal supply back for five seconds the switch should return to it: " + transfer.position());
                    h.assertTrue(bus.live() && motor.live() && !engine.running(), "The bus should be back on the grid and the engine stopped: " + engine.status());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void aLoadedGeneratorOffTheGridSagsAndTripsOnUnderFrequency(GameTestHelper h) {
        BlockPos driverPos = new BlockPos(0, 2, 0);
        BlockPos generatorPos = driverPos.east();
        BlockPos breakerPos = new BlockPos(3, 2, 0);
        BlockPos unit = new BlockPos(5, 2, 2);
        BlockPos loadPos = new BlockPos(2, 4, 4);
        h.setBlock(driverPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.EAST));
        h.setBlock(generatorPos, CMBlocks.GENERATOR.get().defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
        h.setBlock(breakerPos, CMBlocks.CIRCUIT_BREAKER.get());
        transformer(h, unit, UNIT);
        MotorBlockEntity load = motor(h, loadPos, (int) Power.SYNC_RPM, 2);
        wire(h, generatorPos, 0, breakerPos, BreakerBlockEntity.LOAD);
        wire(h, breakerPos, BreakerBlockEntity.LINE, unit, TransformerBlockEntity.HIGH);
        wire(h, WireType.COPPER, unit, TransformerBlockEntity.LOW, loadPos, 0);
        CreativeMotorBlockEntity driver = h.getBlockEntity(driverPos);
        driver.generatedSpeed.setValue((int) Power.SYNC_RPM);
        GeneratorBlockEntity g = h.getBlockEntity(generatorPos);
        BreakerBlockEntity breaker = h.getBlockEntity(breakerPos);
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> h.assertTrue(breaker.close().endsWith("breaker.closed"), "The generator should close onto the dead line"))
                .thenIdle(60)
                .thenExecute(() -> {
                    double sag = Power.DROOP_HZ * g.outputSu() / g.ratedSu();
                    h.assertTrue(load.live() && sag > 0.2 && Math.abs(g.frequency() - (Power.GRID_HZ - sag)) < 0.03, "Its frequency should sag with its load: " + g.frequency() + " for " + g.outputSu());
                    driver.generatedSpeed.setValue(61);
                })
                .thenIdle(100)
                .thenExecute(() -> h.assertTrue(!breaker.closed() && breaker.tripReason().endsWith("trip.frequency"), "Held under 47.5 Hz it should trip its breaker: " + g.frequency() + " " + breaker.tripReason()))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void losingTheGridTripsTheBreakerNearestItAndItComesBackDead(GameTestHelper h) {
        BlockPos gridPos = new BlockPos(0, 2, 0);
        BlockPos breakerPos = new BlockPos(2, 2, 0);
        BlockPos startup = new BlockPos(4, 2, 0);
        BlockPos busPos = new BlockPos(6, 2, 2);
        BlockPos motorPos = new BlockPos(1, 2, 5);
        h.setBlock(gridPos, CMBlocks.GRID_CONNECTION.get());
        h.setBlock(breakerPos, CMBlocks.CIRCUIT_BREAKER.get());
        transformer(h, startup, STARTUP);
        h.setBlock(busPos, CMBlocks.BUS_BAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        MotorBlockEntity motor = motor(h, motorPos, 16, 1);
        wire(h, WireType.LINE, gridPos, 0, breakerPos, BreakerBlockEntity.LINE);
        wire(h, WireType.LINE, breakerPos, BreakerBlockEntity.LOAD, startup, TransformerBlockEntity.HIGH);
        wire(h, startup, TransformerBlockEntity.LOW, busPos, 0);
        wire(h, WireType.COPPER, busPos, 0, motorPos, 0);
        GridTieBlockEntity tie = h.getBlockEntity(gridPos);
        BreakerBlockEntity breaker = h.getBlockEntity(breakerPos);
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> breaker.close())
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(motor.live(), "The motor should run from the grid");
                    tie.disturb(GridTieBlockEntity.Event.OUTAGE, 60, 0);
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(!tie.live() && !breaker.closed() && breaker.tripReason().endsWith("trip.grid_lost"), "Losing the grid should trip the breaker nearest it: " + breaker.tripReason());
                    h.assertTrue(!breaker.line().live() && !motor.live(), "A lost grid should be dead on its side of the breaker");
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(tie.live() && breaker.line().live() && breaker.locked(), "The grid should come back live against the tripped breaker");
                    breaker.reset();
                    h.assertTrue(breaker.close().endsWith("breaker.closed"), "Reset, the breaker should close onto the dead plant");
                })
                .thenIdle(30)
                .thenExecute(() -> h.assertTrue(motor.live(), "The motor should run again"))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 500)
    public static void aVoltageDipStopsMotorsUnlessTheTransformerIsTappedUp(GameTestHelper h) {
        BlockPos gridPos = new BlockPos(0, 2, 0);
        BlockPos startup = new BlockPos(2, 2, 0);
        BlockPos busPos = new BlockPos(4, 2, 2);
        BlockPos motorPos = new BlockPos(1, 2, 5);
        h.setBlock(gridPos, CMBlocks.GRID_CONNECTION.get());
        TransformerBlockEntity t = transformer(h, startup, STARTUP);
        h.setBlock(busPos, CMBlocks.BUS_BAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        MotorBlockEntity motor = motor(h, motorPos, 16, 1);
        wire(h, WireType.LINE, gridPos, 0, startup, TransformerBlockEntity.HIGH);
        wire(h, startup, TransformerBlockEntity.LOW, busPos, 0);
        wire(h, WireType.COPPER, busPos, 0, motorPos, 0);
        GridTieBlockEntity tie = h.getBlockEntity(gridPos);
        BusBarBlockEntity bus = h.getBlockEntity(busPos);
        double raised = Power.MOTOR_KV * (1 + Power.TAP_STEPS * Power.TAP_STEP);
        t.setTap(Power.TAP_STEPS);
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(motor.live() && Math.abs(bus.kilovolts() - raised) < 0.03, "Tapped up six steps the low side should stand 7.5% high: " + bus.kilovolts());
                    tie.disturb(GridTieBlockEntity.Event.DIP, 400, 0.85);
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(Math.abs(bus.kilovolts() - raised * 0.85) < 0.03 && motor.live(), "Tapped up, the motor should run through the dip: " + bus.kilovolts() + " " + motor.status());
                    t.setTap(0);
                })
                .thenIdle(20)
                .thenExecute(() -> h.assertTrue(bus.kilovolts() < Power.MOTOR_KV * Power.UNDERVOLTAGE && motor.live(), "A motor should ride through the first second and a half of low voltage: " + bus.kilovolts()))
                .thenIdle(45)
                .thenExecute(() -> {
                    h.assertTrue(!motor.live() && motor.status().endsWith("motor.voltage"), "Left on low voltage the motor should stop: " + motor.status());
                    tie.disturb(GridTieBlockEntity.Event.NORMAL, 0, 0);
                })
                .thenIdle(40)
                .thenExecute(() -> h.assertTrue(motor.live() && Math.abs(bus.kilovolts() - Power.MOTOR_KV) < 0.03, "With the grid back to normal the motor should run again: " + bus.kilovolts()))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void aFrequencySwingOnTheGridCarriesToEverythingOnIt(GameTestHelper h) {
        BlockPos gridPos = new BlockPos(0, 2, 0);
        BlockPos startup = new BlockPos(2, 2, 0);
        BlockPos busPos = new BlockPos(4, 2, 2);
        h.setBlock(gridPos, CMBlocks.GRID_CONNECTION.get());
        transformer(h, startup, STARTUP);
        h.setBlock(busPos, CMBlocks.BUS_BAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        wire(h, WireType.LINE, gridPos, 0, startup, TransformerBlockEntity.HIGH);
        wire(h, startup, TransformerBlockEntity.LOW, busPos, 0);
        GridTieBlockEntity tie = h.getBlockEntity(gridPos);
        BusBarBlockEntity bus = h.getBlockEntity(busPos);
        h.startSequence()
                .thenIdle(30)
                .thenExecute(() -> {
                    h.assertTrue(Math.abs(bus.frequency() - Power.GRID_HZ) < 1e-6, "A quiet grid should hold 50 Hz");
                    tie.disturb(GridTieBlockEntity.Event.SWING, 300, 0.4);
                })
                .thenIdle(120)
                .thenExecute(() -> {
                    h.assertTrue(Math.abs(bus.frequency() - (Power.GRID_HZ + 0.4)) < 0.01, "The bus should follow the grid's frequency: " + bus.frequency());
                    tie.disturb(GridTieBlockEntity.Event.NORMAL, 0, 0);
                })
                .thenIdle(120)
                .thenExecute(() -> h.assertTrue(Math.abs(bus.frequency() - Power.GRID_HZ) < 0.01, "The grid should settle back to 50 Hz: " + bus.frequency()))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void aTransferSwitchLeavesASupplyThatSagsAndTheBatteryHoldsTheBus(GameTestHelper h) {
        BlockPos gridPos = new BlockPos(0, 2, 0);
        BlockPos startup = new BlockPos(2, 2, 0);
        BlockPos switchPos = new BlockPos(4, 2, 3);
        BlockPos busPos = new BlockPos(2, 2, 5);
        BlockPos batteryPos = new BlockPos(6, 2, 5);
        BlockPos motorPos = new BlockPos(1, 4, 3);
        h.setBlock(gridPos, CMBlocks.GRID_CONNECTION.get());
        transformer(h, startup, STARTUP);
        h.setBlock(switchPos, CMBlocks.TRANSFER_SWITCH.get());
        h.setBlock(busPos, CMBlocks.BUS_BAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        h.setBlock(batteryPos, CMBlocks.STATION_BATTERY.get());
        MotorBlockEntity motor = motor(h, motorPos, 16, 1);
        wire(h, WireType.LINE, gridPos, 0, startup, TransformerBlockEntity.HIGH);
        wire(h, startup, TransformerBlockEntity.LOW, switchPos, TransferSwitchBlockEntity.NORMAL);
        wire(h, switchPos, TransferSwitchBlockEntity.LOAD, busPos, 0);
        wire(h, busPos, 0, batteryPos, 0);
        wire(h, WireType.COPPER, busPos, 0, motorPos, 0);
        GridTieBlockEntity tie = h.getBlockEntity(gridPos);
        TransferSwitchBlockEntity transfer = h.getBlockEntity(switchPos);
        BusBarBlockEntity bus = h.getBlockEntity(busPos);
        BatteryBlockEntity battery = h.getBlockEntity(batteryPos);
        battery.setCharge(0.5);
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(transfer.position() == TransferSwitchBlock.Position.NORMAL && motor.live(), "The switch should sit on a healthy supply: " + transfer.position());
                    tie.disturb(GridTieBlockEntity.Event.DIP, 400, 0.85);
                })
                .thenIdle(70)
                .thenExecute(() -> {
                    h.assertTrue(transfer.position() == TransferSwitchBlock.Position.OFF && transfer.calling(), "A supply under nine tenths of its voltage should be left: " + transfer.position());
                    h.assertTrue(battery.online() && motor.live() && Math.abs(bus.kilovolts() - Power.MOTOR_KV) < 0.03, "The battery should hold the bus at 6 kV meanwhile: " + bus.kilovolts());
                    tie.disturb(GridTieBlockEntity.Event.NORMAL, 0, 0);
                })
                .thenIdle(Power.TRANSFER_RETURN_TICKS + 80)
                .thenExecute(() -> h.assertTrue(transfer.position() == TransferSwitchBlock.Position.NORMAL && !battery.online() && motor.live(), "With the supply healthy again the switch should return: " + transfer.position()))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void aLoadedWireLosesVoltageAlongIt(GameTestHelper h) {
        BlockPos gridPos = new BlockPos(0, 2, 0);
        BlockPos startup = new BlockPos(2, 2, 0);
        BlockPos busPos = new BlockPos(4, 2, 0);
        BlockPos motorPos = new BlockPos(1, 2, 6);
        h.setBlock(gridPos, CMBlocks.GRID_CONNECTION.get());
        transformer(h, startup, STARTUP);
        h.setBlock(busPos, CMBlocks.BUS_BAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        MotorBlockEntity motor = motor(h, motorPos, (int) Power.SYNC_RPM, 2);
        wire(h, WireType.LINE, gridPos, 0, startup, TransformerBlockEntity.HIGH);
        wire(h, startup, TransformerBlockEntity.LOW, busPos, 0);
        wire(h, WireType.COPPER, busPos, 0, motorPos, 0);
        BusBarBlockEntity bus = h.getBlockEntity(busPos);
        h.startSequence()
                .thenIdle(80)
                .thenExecute(() -> {
                    double megawatts = Power.megawatts(motor.demandSu());
                    double length = Math.sqrt(h.absolutePos(busPos).distSqr(h.absolutePos(motorPos)));
                    double lost = Power.MOTOR_KV * Power.WIRE_DROP * length * Power.kiloamps(megawatts, Power.MOTOR_KV) / WireType.COPPER.kiloamps;
                    double end = PowerNetwork.kilovolts(h.getLevel(), h.absolutePos(motorPos), 0);
                    h.assertTrue(motor.live() && megawatts > 4, "The motor should be drawing over 4 MW: " + megawatts);
                    h.assertTrue(bus.kilovolts() < Power.MOTOR_KV - 0.005 && bus.kilovolts() > Power.MOTOR_KV * 0.99, "A loaded transformer should give a little under its voltage: " + bus.kilovolts());
                    h.assertTrue(lost > 0.02 && Math.abs(bus.kilovolts() - end - lost) < lost * 0.2, "The wire should lose " + lost + " kV to the motor: " + bus.kilovolts() + " to " + end);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void lightningOnAWireIsTakenByAnArresterOrARodAndOtherwiseCostsEquipment(GameTestHelper h) {
        BlockPos gridPos = new BlockPos(0, 2, 0);
        BlockPos breakerPos = new BlockPos(2, 2, 0);
        BlockPos main = new BlockPos(6, 2, 0);
        BlockPos arresterPos = new BlockPos(5, 2, 3);
        BlockPos rodPos = new BlockPos(4, 2, 2);
        h.setBlock(gridPos, CMBlocks.GRID_CONNECTION.get());
        h.setBlock(breakerPos, CMBlocks.CIRCUIT_BREAKER.get());
        transformer(h, main, MAIN);
        h.setBlock(arresterPos, CMBlocks.SURGE_ARRESTER.get());
        wire(h, WireType.LINE, gridPos, 0, breakerPos, BreakerBlockEntity.LINE);
        wire(h, WireType.LINE, breakerPos, BreakerBlockEntity.LOAD, main, TransformerBlockEntity.HIGH);
        wire(h, WireType.LINE, arresterPos, 0, main, TransformerBlockEntity.HIGH);
        BreakerBlockEntity breaker = h.getBlockEntity(breakerPos);
        SurgeArresterBlockEntity arrester = h.getBlockEntity(arresterPos);
        Vec3 over = Vec3.atCenterOf(h.absolutePos(new BlockPos(4, 3, 0)));
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> breaker.close())
                .thenIdle(30)
                .thenExecute(() -> {
                    h.assertTrue(PowerNetwork.strikeNear(h.getLevel(), over, 4.0, false), "A strike beside a wire should find it");
                    h.assertTrue(arrester.surges() == 1 && breaker.closed() && h.getBlockState(main).is(CMBlocks.TRANSFORMER.get()), "An arrester on the line should take the surge and nothing be lost");
                    WireLinks.cutAll(h.getLevel(), h.absolutePos(arresterPos));
                    h.setBlock(rodPos, Blocks.LIGHTNING_ROD);
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    h.assertTrue(PowerNetwork.strikeNear(h.getLevel(), over, 4.0, false), "A strike beside a wire should find it");
                    h.assertTrue(arrester.surges() == 1 && breaker.closed() && h.getBlockState(main).is(CMBlocks.TRANSFORMER.get()), "A lightning rod close by should take the strike off the wire");
                    h.setBlock(rodPos, Blocks.AIR);
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    h.assertTrue(PowerNetwork.strikeNear(h.getLevel(), over, 4.0, false), "A strike beside a wire should find it");
                    h.assertTrue(!breaker.closed() && breaker.tripReason().endsWith("trip.surge"), "With nothing to take it the surge should trip the breaker on that line: " + breaker.tripReason());
                    h.assertTrue(h.getBlockState(main).isAir() && breaker.wires().all().size() == 1, "It should cost the struck wire and the nearest transformer");
                })
                .thenSucceed();
    }
}
