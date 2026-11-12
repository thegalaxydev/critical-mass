package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.radiation.ContaminationData;
import com.criticalmass.content.radiation.Radiation;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.Corium;
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
}
