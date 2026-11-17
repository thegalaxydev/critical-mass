package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.radiation.ContaminationData;
import com.criticalmass.content.radiation.Radiation;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.Corium;
import com.criticalmass.content.thermal.Detonation;
import com.criticalmass.content.thermal.ReactorCore;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.registry.CMBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class MeltdownTests {
    private static final String EMPTY = "empty";
    private static final String LARGE = "empty_large";
    private static final BlockPos FLOOR = new BlockPos(3, 1, 3);
    private static final double MOLTEN = (2000 - CMThermal.AMBIENT) * CMThermal.FUEL_HEAT_CAPACITY * 3;

    private MeltdownTests() {
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void floodedMeltStaysInTheVessel(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 0);
        double[] start = new double[1];
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> c.core().setColumnHeat(2, 0, MOLTEN))
                .thenIdle(3)
                .thenExecute(() -> {
                    for (int y = 2; y <= 4; y++) {
                        h.assertBlockPresent(CMBlocks.RUINED_CHANNEL.get(), new BlockPos(3, y, 3));
                    }
                    h.assertTrue(c.core().coriumMass() == 3 && c.core().assemblies().size() == 4, "The melted assembly should become corium: mass " + c.core().coriumMass());
                    h.assertTrue(CMThermal.isContaminated(c.coolant().fluid()), "Melted fuel should contaminate the coolant");
                    h.assertTrue(!c.isReacting(), "A reactor with a ruined channel should not run: " + c.status());
                    start[0] = c.core().coriumHeat();
                })
                .thenIdle(200)
                .thenExecute(() -> {
                    h.assertTrue(c.core().coriumHeat() < start[0] * 0.3, "Coolant should quench the corium: " + c.core().coriumHeat() + " of " + start[0]);
                    double floor = c.integrity(h.absolutePos(FLOOR));
                    h.assertTrue(floor > 0 && floor < 1, "The floor should be scarred but hold: " + floor);
                    h.assertBlockPresent(CMBlocks.REACTOR_VESSEL_CASING.get(), FLOOR);
                    h.assertTrue(c.flashedVolume() > 0 || c.coolant().temperature() > CMThermal.AMBIENT + 20, "The corium's heat should end up in the coolant");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void aCoreMeltedInEveryColumnIsArmedToGoOff(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 0);
        BlockPos heart = h.absolutePos(new BlockPos(3, 3, 3));
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    c.core().setColumnHeat(2, 0, MOLTEN);
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    h.assertTrue(!Detonation.armed(h.getLevel(), heart, 4), "One melted column of five should not arm the core");
                    for (ReactorCore.FuelColumn f : c.core().columns()) {
                        c.core().setColumnHeat(f.dx(), f.dz(), MOLTEN);
                    }
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    h.assertTrue(c.core().coriumMass() == 15 && c.core().assemblies().isEmpty(), "Every assembly should be corium: mass " + c.core().coriumMass());
                    h.assertTrue(Detonation.armed(h.getLevel(), heart, 4), "A core melted in every column should be armed");
                    h.assertTrue(Detonation.radiusFor(15) == 40 && Detonation.radiusFor(120) == 64, "The crater should grow with the molten mass: " + Detonation.radiusFor(120));
                    h.assertTrue(Detonation.cancel(h.getLevel(), heart, 4) && !Detonation.armed(h.getLevel(), heart, 4), "The fuse should be cancellable while it burns");
                })
                .thenIdle(80)
                .thenExecute(() -> h.assertBlockPresent(CMBlocks.REACTOR_CONTROLLER.get(), TestVessels.CONTROLLER))
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void aDetonationCarvesACraterLinesItAndCharsLogs(GameTestHelper h) {
        BlockPos centre = new BlockPos(8, 4, 8);
        for (int x = 2; x <= 14; x++) {
            for (int z = 2; z <= 14; z++) {
                for (int y = 1; y <= 5; y++) {
                    h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        BlockPos log = new BlockPos(8, 4, 13);
        BlockPos leaves = new BlockPos(13, 4, 8);
        BlockPos planks = new BlockPos(3, 4, 8);
        h.setBlock(log, Blocks.OAK_LOG);
        h.setBlock(leaves, Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        h.setBlock(planks, Blocks.OAK_PLANKS);
        Detonation.arm(h.getLevel(), h.absolutePos(centre), 4, 1, false);
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertBlockPresent(Blocks.AIR, centre);
                    h.assertBlockPresent(Blocks.AIR, centre.east(3));
                    h.assertBlockPresent(Blocks.AIR, centre.above(3));
                    h.assertBlockPresent(CMBlocks.CHARRED_LOG.get(), log);
                    h.assertBlockPresent(Blocks.AIR, leaves);
                    h.assertBlockPresent(Blocks.AIR, planks);
                    net.minecraft.world.level.block.state.BlockState lining = h.getBlockState(new BlockPos(8, 4, 3));
                    h.assertTrue(lining.is(Blocks.SMOOTH_BASALT) || lining.is(Blocks.BLACKSTONE) || lining.is(Blocks.MAGMA_BLOCK) || lining.is(Blocks.TUFF), "The crater wall should be slag: " + lining);
                    h.assertBlockPresent(Blocks.STONE, new BlockPos(2, 1, 2));
                    h.assertBlockPresent(CMBlocks.CORIUM.get(), new BlockPos(8, 3, 8));
                    h.assertTrue(!Detonation.armed(h.getLevel(), h.absolutePos(centre), 2), "A finished detonation should be gone");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void dryMeltBurnsThroughUnlessConcreteStopsIt(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(FluidStack.EMPTY, 0);
        BlockPos under = FLOOR.below();
        h.setBlock(under, CMBlocks.SHIELDING_CONCRETE.get());
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    c.core().setColumnHeat(2, 0, MOLTEN);
                    c.setIntegrity(h.absolutePos(FLOOR), 0.2);
                })
                .thenIdle(120)
                .thenExecute(() -> {
                    h.assertBlockPresent(CMBlocks.CORIUM.get(), FLOOR);
                    Corium.Entity_ lump = h.getBlockEntity(FLOOR);
                    h.assertTrue(lump.mass() == 3 && lump.temperature() > CMThermal.CORIUM_SINK_TEMPERATURE && h.getBlockState(FLOOR).getValue(Corium.CoriumBlock.HOT), "Corium should leave the vessel hot: " + lump.temperature());
                    h.assertTrue(c.core().coriumMass() == 0 && c.problemPositions().contains(h.absolutePos(FLOOR)), "The vessel should be holed and emptied of corium: " + c.status());
                    h.assertTrue(ContaminationData.get(h.getLevel()).get(h.absolutePos(FLOOR)) > 0, "The breach should contaminate the ground");
                    Vec3 near = Vec3.atCenterOf(h.absolutePos(FLOOR.east(4)));
                    h.assertTrue(Radiation.ambientRate(h.getLevel(), near) > 0, "Corium should be a radiation source");
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertBlockPresent(CMBlocks.SHIELDING_CONCRETE.get(), under);
                    h.assertBlockPresent(CMBlocks.CORIUM.get(), FLOOR);
                    h.setBlock(under.below(), CMBlocks.SHIELDING_CONCRETE.get());
                    h.setBlock(under, Blocks.STONE);
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertBlockPresent(CMBlocks.CORIUM.get(), under);
                    h.assertBlockNotPresent(CMBlocks.CORIUM.get(), FLOOR);
                    Corium.Entity_ lump = h.getBlockEntity(under);
                    h.assertTrue(lump.depth() == 1 && lump.mass() == 3, "Corium should have melted down one block: depth " + lump.depth());
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void anArmedDetonationIsKeptWhenTheWorldIsSaved(GameTestHelper h) {
        net.minecraft.core.BlockPos at = h.absolutePos(new net.minecraft.core.BlockPos(2, 3, 2));
        com.criticalmass.content.thermal.Detonation.arm(h.getLevel(), at, 3, 4000, false);
        net.minecraft.nbt.CompoundTag kept = com.criticalmass.content.thermal.Detonation.saved(h.getLevel());
        h.assertTrue(com.criticalmass.content.thermal.Detonation.cancel(h.getLevel(), at, 1) && !com.criticalmass.content.thermal.Detonation.armed(h.getLevel(), at, 1), "The fuse should be stopped");
        com.criticalmass.content.thermal.Detonation.restore(h.getLevel(), kept);
        h.assertTrue(com.criticalmass.content.thermal.Detonation.armed(h.getLevel(), at, 1), "A saved detonation should be armed again when the world loads");
        h.assertTrue(com.criticalmass.content.thermal.Detonation.cancel(h.getLevel(), at, 1), "It should still be on its fuse");
        h.succeed();
    }
}
