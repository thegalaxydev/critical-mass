package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.power.BreakerBlockEntity;
import com.criticalmass.content.power.MotorBlockEntity;
import com.criticalmass.content.power.TransformerBlockEntity;
import com.criticalmass.content.power.WireLinks;
import com.criticalmass.content.power.WireType;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class WeatherTests {
    private static final String RAIN = "rain";

    private WeatherTests() {
    }

    @BeforeBatch(batch = RAIN)
    public static void rain(ServerLevel level) {
        level.setWeatherParameters(0, 12000, true, false);
    }

    @AfterBatch(batch = RAIN)
    public static void clear(ServerLevel level) {
        level.setWeatherParameters(12000, 0, false, false);
    }

    private static void wire(GameTestHelper h, WireType type, BlockPos a, int ta, BlockPos b, int tb) {
        String problem = WireLinks.connect(h.getLevel(), h.absolutePos(a), ta, h.absolutePos(b), tb, type);
        h.assertTrue(problem.isEmpty(), "A wire should string between two terminals in reach: " + problem);
    }

    private static MotorBlockEntity motor(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, CMBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(DirectionalKineticBlock.FACING, Direction.EAST));
        MotorBlockEntity motor = h.getBlockEntity(pos);
        motor.setSpeed(16);
        return motor;
    }

    @GameTest(template = "empty", batch = RAIN, timeoutTicks = 2400, skyAccess = true)
    public static void rainOnAMotorLeftInTheOpenTripsItsFeederAndARoofKeepsItDry(GameTestHelper h) {
        BlockPos gridPos = new BlockPos(0, 2, 0);
        BlockPos startup = new BlockPos(2, 2, 0);
        BlockPos openBreaker = new BlockPos(4, 2, 0);
        BlockPos roofedBreaker = new BlockPos(4, 2, 3);
        BlockPos openMotor = new BlockPos(1, 2, 5);
        BlockPos roofedMotor = new BlockPos(5, 2, 5);
        h.setBlock(gridPos, CMBlocks.GRID_CONNECTION.get());
        h.setBlock(startup, CMBlocks.TRANSFORMER.get());
        TransformerBlockEntity t = h.getBlockEntity(startup);
        t.setRatio(1);
        h.setBlock(openBreaker, CMBlocks.CIRCUIT_BREAKER.get());
        h.setBlock(roofedBreaker, CMBlocks.CIRCUIT_BREAKER.get());
        MotorBlockEntity open = motor(h, openMotor);
        MotorBlockEntity roofed = motor(h, roofedMotor);
        h.setBlock(roofedMotor.above(2), Blocks.SMOOTH_STONE);
        wire(h, WireType.LINE, gridPos, 0, startup, TransformerBlockEntity.HIGH);
        wire(h, WireType.CABLE, startup, TransformerBlockEntity.LOW, openBreaker, BreakerBlockEntity.LINE);
        wire(h, WireType.CABLE, startup, TransformerBlockEntity.LOW, roofedBreaker, BreakerBlockEntity.LINE);
        wire(h, WireType.COPPER, openBreaker, BreakerBlockEntity.LOAD, openMotor, 0);
        wire(h, WireType.COPPER, roofedBreaker, BreakerBlockEntity.LOAD, roofedMotor, 0);
        BreakerBlockEntity a = h.getBlockEntity(openBreaker);
        BreakerBlockEntity b = h.getBlockEntity(roofedBreaker);
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    a.close();
                    b.close();
                })
                .thenIdle(40)
                .thenExecute(() -> h.assertTrue(h.getLevel().isRaining() && roofed.live(), "It should be raining with both motors started"))
                .thenWaitUntil(() -> h.assertTrue(!a.closed() && a.tripReason().endsWith("trip.wet"), "Rain on a live motor should trip its feeder"))
                .thenIdle(20)
                .thenExecute(() -> h.assertTrue(b.closed() && roofed.live() && !open.live(), "A motor under a roof should stay dry and running"))
                .thenSucceed();
    }
}
