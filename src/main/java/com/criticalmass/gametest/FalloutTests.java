package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.radiation.ContaminationData;
import com.criticalmass.content.radiation.Fallout;
import com.criticalmass.content.radiation.Radiation;
import com.criticalmass.content.radiation.RadiationEvents;
import com.criticalmass.registry.CMBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class FalloutTests {
    private static final String LARGE = "empty_large";

    private FalloutTests() {
    }

    @GameTest(template = LARGE, timeoutTicks = 200, skyAccess = true)
    public static void falloutIrradiatesTheOpenAndShelterCutsIt(GameTestHelper h) {
        BlockPos heart = new BlockPos(5, 2, 8);
        BlockPos downwind = new BlockPos(8, 2, 8);
        BlockPos upwind = new BlockPos(1, 2, 8);
        Vec3 open = Vec3.atCenterOf(h.absolutePos(downwind));
        Vec3 behind = Vec3.atCenterOf(h.absolutePos(upwind));
        Vec3 east = Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(1, 0, 0))).subtract(Vec3.atLowerCornerOf(h.absolutePos(BlockPos.ZERO)));
        Fallout.clear(h.getLevel());
        Fallout.start(h.getLevel(), h.absolutePos(heart), 3, east.x, east.z);
        Cow cow = h.spawn(EntityType.COW, downwind.north());
        double[] first = new double[1];
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    first[0] = Fallout.rate(h.getLevel(), open);
                    h.assertTrue(first[0] > 100 && Fallout.rate(h.getLevel(), behind) == 0, "Fallout should lie downwind of the crater and not upwind: " + first[0] + " " + Fallout.rate(h.getLevel(), behind));
                    h.assertTrue(Radiation.ambientRate(h.getLevel(), open) >= first[0], "A survey should read the fallout: " + Radiation.ambientRate(h.getLevel(), open));
                    Player walker = h.makeMockPlayer(GameType.SURVIVAL);
                    walker.setPos(open.x, open.y - 1.6, open.z);
                    double dose = RadiationEvents.exposeOnce(walker, h.getLevel(), Radiation.blockSources(h.getLevel()));
                    h.assertTrue(dose > first[0] * 0.95 && RadiationEvents.data(walker).contamination > 0, "Someone in the open should take the dose and pick up contamination: " + dose);
                    h.setBlock(downwind.above(3), Blocks.STONE);
                })
                .thenIdle(45)
                .thenExecute(() -> {
                    double covered = Fallout.rate(h.getLevel(), open);
                    h.assertTrue(Math.abs(covered / first[0] - Fallout.SHELTER) < 0.05, "A roof should cut the fallout to " + Fallout.SHELTER + ": " + covered / first[0]);
                    h.assertTrue(cow.hasEffect(MobEffects.WITHER), "An animal in the open should sicken");
                    int plumes = Fallout.count(h.getLevel());
                    h.assertTrue(plumes == 1 && Fallout.clear(h.getLevel()) == 1 && Fallout.rate(h.getLevel(), open) == 0, "Cleared fallout should be gone: " + plumes + " plumes");
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void falloutAshIsContaminationThatLeavesWithIt(GameTestHelper h) {
        BlockPos ground = new BlockPos(8, 1, 8);
        BlockPos ash = ground.above();
        BlockPos second = new BlockPos(10, 2, 8);
        ContaminationData cells = ContaminationData.get(h.getLevel());
        h.setBlock(ground, Blocks.STONE);
        h.setBlock(second.below(), Blocks.STONE);
        h.setBlock(ash, CMBlocks.FALLOUT_ASH.get());
        h.setBlock(second, CMBlocks.FALLOUT_ASH.get());
        cells.add(h.absolutePos(ash), Fallout.ASH_CONTAMINATION);
        cells.add(h.absolutePos(second), Fallout.ASH_CONTAMINATION);
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertTrue(Radiation.fieldDose(h.getLevel(), Vec3.atCenterOf(h.absolutePos(ash.above()))) > 0, "Ash on the ground should give a dose");
                    h.setBlock(ash, Blocks.AIR);
                    h.setBlock(second.below(), Blocks.AIR);
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertTrue(cells.get(h.absolutePos(ash)) == 0, "Clearing ash should clear its contamination");
                    h.assertBlockPresent(Blocks.AIR, second);
                    h.assertTrue(cells.get(h.absolutePos(second)) == 0, "Ash that loses its footing should go, and its contamination with it");
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void ashIsSweptByAPloughAndWashedOffByWater(GameTestHelper h) {
        BlockPos first = new BlockPos(3, 2, 3);
        ContaminationData data = ContaminationData.get(h.getLevel());
        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 7; z++) {
                if (x == 1 || x == 7 || z == 1 || z == 7) {
                    h.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
                }
            }
        }
        for (int x = 2; x <= 5; x++) {
            BlockPos at = new BlockPos(x, 2, 3);
            h.setBlock(at, CMBlocks.FALLOUT_ASH.get());
            data.add(h.absolutePos(at), 1.5);
        }
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    h.assertTrue(new com.simibubi.create.content.contraptions.actors.plough.PloughMovementBehaviour().canBreak(h.getLevel(), h.absolutePos(first), h.getBlockState(first)),
                            "A Mechanical Plough should be able to sweep ash up");
                    h.assertTrue(data.get(h.absolutePos(first)) > 0, "The ash should be contamination");
                    h.setBlock(new BlockPos(6, 2, 3), Blocks.WATER);
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    for (int x = 2; x <= 5; x++) {
                        BlockPos at = new BlockPos(x, 2, 3);
                        h.assertTrue(!h.getBlockState(at).is(CMBlocks.FALLOUT_ASH.get()), "Flowing water should wash the ash off at " + at);
                        h.assertTrue(data.get(h.absolutePos(at)) == 0, "Washed ash should leave no contamination at " + at);
                    }
                })
                .thenSucceed();
    }
}
