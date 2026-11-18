package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.control.MeltdownSirenBlock;
import com.criticalmass.content.control.MeltdownSirenBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.registry.CMBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class SirenTests {
    private static final String EMPTY = "empty";
    private static final String LARGE = "empty_large";
    private static final BlockPos SIREN = new BlockPos(2, 2, 2);

    private SirenTests() {
    }

    private static MeltdownSirenBlockEntity standing(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos.below(), Blocks.STONE);
        h.setBlock(pos, CMBlocks.MELTDOWN_SIREN.get().defaultBlockState());
        return h.getBlockEntity(pos);
    }

    private static double heat(double temperature) {
        return (temperature - CMThermal.AMBIENT) * CMThermal.FUEL_HEAT_CAPACITY * 3;
    }

    @GameTest(template = EMPTY, timeoutTicks = 900)
    public static void aSirenWindsUpOnRedstoneAndCoastsDown(GameTestHelper h) {
        MeltdownSirenBlockEntity siren = standing(h, SIREN);
        float[] seen = new float[1];
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    h.assertTrue(siren.demand() == 0 && siren.spin() == 0 && siren.status().equals(MeltdownSirenBlockEntity.UNLINKED), "A siren with no link and no signal should be still: " + siren.status());
                    h.setBlock(SIREN.east(), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(20)
                .thenExecute(() -> h.assertTrue(siren.demand() == 1 && siren.status().equals(MeltdownSirenBlockEntity.POWERED) && siren.spin() > 0.3f && siren.spin() < 0.5f,
                        "A redstone signal should start it winding up: " + siren.spin()))
                .thenIdle(80)
                .thenExecute(() -> {
                    h.assertTrue(siren.spin() >= 0.55f && siren.spin() <= 1, "At full cry it should wail between its low note and full speed: " + siren.spin());
                    seen[0] = siren.spin();
                    h.setBlock(SIREN.east(), Blocks.AIR);
                })
                .thenIdle(40)
                .thenExecute(() -> h.assertTrue(siren.demand() == 0 && siren.spin() > 0.2f && siren.spin() < seen[0], "With the signal gone it should coast down slowly: " + siren.spin() + " from " + seen[0]))
                .thenIdle(500)
                .thenExecute(() -> h.assertTrue(siren.spin() == 0, "It should come to rest: " + siren.spin()))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void aSirenStandsOnAFloorOrHangsOnAWallAndFallsWithItsSupport(GameTestHelper h) {
        standing(h, SIREN);
        BlockPos wall = new BlockPos(4, 2, 2);
        BlockPos hung = wall.east();
        h.setBlock(wall, Blocks.STONE);
        BlockState mounted = CMBlocks.MELTDOWN_SIREN.get().defaultBlockState().setValue(MeltdownSirenBlock.WALL, true).setValue(MeltdownSirenBlock.FACING, Direction.EAST);
        h.assertTrue(mounted.canSurvive(h.getLevel(), h.absolutePos(hung)), "A siren should hang on a wall behind it");
        h.assertTrue(!mounted.setValue(MeltdownSirenBlock.FACING, Direction.NORTH).canSurvive(h.getLevel(), h.absolutePos(hung)), "It should not hang with no wall behind it");
        h.setBlock(hung, mounted);
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertBlockPresent(CMBlocks.MELTDOWN_SIREN.get(), hung);
                    h.assertBlockPresent(CMBlocks.MELTDOWN_SIREN.get(), SIREN);
                    h.setBlock(wall, Blocks.AIR);
                    h.setBlock(SIREN.below(), Blocks.AIR);
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertBlockPresent(Blocks.AIR, hung);
                    h.assertBlockPresent(Blocks.AIR, SIREN);
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void aLinkedSirenWarnsPastTheTripTemperatureAndWailsOnAMeltdown(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        BlockPos at = new BlockPos(10, 2, 10);
        MeltdownSirenBlockEntity siren = standing(h, at);
        h.assertTrue(!siren.link(h.absolutePos(at.below())), "A siren should refuse a block that is not a Reactor Controller");
        h.assertTrue(siren.link(h.absolutePos(TestVessels.CONTROLLER)), "A siren should link to a Reactor Controller");
        double warm = (CMThermal.TRIP_TEMPERATURE + CMThermal.MELT_TEMPERATURE) / 2;
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    h.assertTrue(siren.demand() == 0 && siren.status().equals(MeltdownSirenBlockEntity.SILENT) && !siren.canSilence(), "A linked siren should be silent over a cold core: " + siren.status());
                    c.core().setColumnHeat(2, 0, heat(warm));
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    h.assertTrue(siren.status().equals(MeltdownSirenBlockEntity.WARNING) && siren.demand() > MeltdownSirenBlockEntity.WARN_LOW && siren.demand() < MeltdownSirenBlockEntity.WARN_HIGH && siren.spin() > 0,
                            "Fuel halfway from the trip to melting should make it growl: " + siren.status() + " " + siren.demand() + " at " + c.core().maxTemperature());
                    h.assertTrue(siren.canSilence(), "A sounding siren should be able to be silenced");
                    siren.silence();
                    c.core().setColumnHeat(2, 0, heat(warm));
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    h.assertTrue(siren.demand() == 0 && siren.status().equals(MeltdownSirenBlockEntity.SILENCED), "Used, it should go quiet while the warning lasts: " + siren.status() + " at " + c.core().maxTemperature());
                    c.core().setColumnHeat(2, 0, heat(CMThermal.MELT_TEMPERATURE + 200));
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertTrue(c.core().coriumMass() > 0, "The column should have melted: " + c.core().maxTemperature());
                    h.assertTrue(siren.demand() == 1 && siren.status().equals(MeltdownSirenBlockEntity.MELTDOWN), "A meltdown should set it wailing even after the warning was silenced: " + siren.status());
                    siren.silence();
                })
                .thenIdle(3)
                .thenExecute(() -> h.assertTrue(siren.demand() == 0 && siren.status().equals(MeltdownSirenBlockEntity.SILENCED), "Used again, it should go quiet: " + siren.status()))
                .thenSucceed();
    }
}
