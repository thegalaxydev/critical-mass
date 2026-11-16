package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.power.BreakerBlock;
import com.criticalmass.content.power.BreakerBlockEntity;
import com.criticalmass.content.power.GeneratorBlockEntity;
import com.criticalmass.content.power.GridTieBlockEntity;
import com.criticalmass.content.power.Power;
import com.criticalmass.content.power.WireLinks;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
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

    private PowerTests() {
    }

    private static void wire(GameTestHelper h, BlockPos a, int ta, BlockPos b, int tb) {
        String problem = WireLinks.connect(h.getLevel(), h.absolutePos(a), ta, h.absolutePos(b), tb);
        h.assertTrue(problem.isEmpty(), "A wire should string between two terminals in reach: " + problem);
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
        wire(h, BREAKER, BreakerBlockEntity.LINE, GRID, 0);
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
                    h.assertTrue(WireLinks.connect(h.getLevel(), h.absolutePos(GENERATOR), 0, h.absolutePos(POST), 0).endsWith("wire.already")
                            && WireLinks.connect(h.getLevel(), h.absolutePos(GENERATOR), 0, h.absolutePos(GENERATOR), 0).endsWith("wire.same"), "A wire should not double up or loop onto its own end");
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
}
