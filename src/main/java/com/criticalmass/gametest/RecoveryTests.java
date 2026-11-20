package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.fuel.ShieldedCaskBlockEntity;
import com.criticalmass.content.radiation.ContaminationData;
import com.criticalmass.content.radiation.Radiation;
import com.criticalmass.content.thermal.Aftermath;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.Detonation;
import com.criticalmass.content.thermal.WeldingTorchItem;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMConfig;
import com.criticalmass.registry.CMItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class RecoveryTests {
    private static final String EMPTY = "empty";
    private static final String LARGE = "empty_large";

    private RecoveryTests() {
    }

    private static List<BlockPos> find(GameTestHelper h, net.minecraft.world.level.block.Block block, int x0, int y0, int z0, int x1, int y1, int z1) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) {
            if (h.getBlockState(p).is(block)) {
                out.add(p.immutable());
            }
        }
        return out;
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void anExplosionLeavesDebrisWreckageAndAContaminatedShell(GameTestHelper h) {
        BlockPos centre = new BlockPos(8, 5, 8);
        for (BlockPos p : BlockPos.betweenClosed(3, 1, 3, 13, 9, 13)) {
            boolean wall = p.getX() == 3 || p.getX() == 13 || p.getZ() == 3 || p.getZ() == 13 || p.getY() == 1 || p.getY() == 9;
            h.setBlock(p, wall ? CMBlocks.SHIELDING_CONCRETE.get() : p.getY() >= 4 && p.getY() <= 6 && p.distManhattan(centre) > 1 ? CMBlocks.STEEL_BLOCK.get() : Blocks.AIR);
        }
        ContaminationData soil = ContaminationData.get(h.getLevel());
        for (BlockPos p : BlockPos.betweenClosed(3, 1, 3, 13, 9, 13)) {
            soil.clear(h.absolutePos(p));
        }
        double spill = CMThermal.CORIUM_SPILL * 8 * CMConfig.get(CMConfig.CORIUM_CONTAMINATION);
        h.assertTrue(Aftermath.leavesWreckage(CMBlocks.STEEL_BLOCK.get().defaultBlockState()) && !Aftermath.leavesWreckage(Blocks.STONE.defaultBlockState())
                && !Aftermath.leavesWreckage(CMBlocks.URANIUM_ORE.get().defaultBlockState()), "Plant blocks should leave wreckage, and stone and ore should not");
        h.assertTrue(ShieldedCaskBlockEntity.refuseReason(new ItemStack(CMBlocks.CORE_DEBRIS.get())) == null, "A Shielded Cask should take Core Debris");
        h.assertTrue(Radiation.itemStrength(new ItemStack(CMBlocks.CORE_DEBRIS.get(), 2)) == 2 * Radiation.DEBRIS_DOSE, "Core Debris should give its dose as an item");
        Detonation.arm(h.getLevel(), h.absolutePos(centre), 8, 1, false, true);
        h.startSequence()
                .thenIdle(80)
                .thenExecute(() -> {
                    List<BlockPos> debris = find(h, CMBlocks.CORE_DEBRIS.get(), 0, 0, 0, 15, 15, 15);
                    List<BlockPos> wreckage = find(h, CMBlocks.WRECKAGE.get(), 0, 0, 0, 15, 15, 15);
                    h.assertTrue(debris.size() >= 3 && debris.size() <= Aftermath.debrisFor(8), "The blast should throw Core Debris about: " + debris.size());
                    h.assertTrue(!wreckage.isEmpty(), "Some of the plant should be left as Wreckage");
                    for (BlockPos p : debris) {
                        h.assertTrue(p.getX() > 3 && p.getX() < 13 && p.getZ() > 3 && p.getZ() < 13 && p.getY() > 1 && p.getY() < 9, "A sealed building should keep its debris in: " + p);
                        h.assertTrue(!h.getBlockState(p.below()).canBeReplaced(), "Debris should come to rest on something: " + p);
                    }
                    for (BlockPos p : wreckage) {
                        h.assertTrue(p.getX() > 3 && p.getX() < 13 && p.getZ() > 3 && p.getZ() < 13 && !h.getBlockState(p.below()).canBeReplaced(), "Wreckage should fall inside and come to rest: " + p);
                    }
                    h.assertBlockPresent(CMBlocks.SHIELDING_CONCRETE.get(), new BlockPos(3, 5, 8));
                    h.assertTrue(find(h, CMBlocks.STEEL_BLOCK.get(), 4, 2, 4, 12, 8, 12).isEmpty(), "The blast should still clear the plant inside");
                    double inside = 0;
                    for (BlockPos p : BlockPos.betweenClosed(3, 1, 3, 13, 9, 13)) {
                        inside += soil.get(h.absolutePos(p));
                    }
                    h.assertTrue(Math.abs(inside - spill) < 1e-6, "The blast should leave the same contamination in all, and all of it inside: " + inside);
                    Vec3 heart = Vec3.atCenterOf(h.absolutePos(centre));
                    h.assertTrue(soil.get(h.absolutePos(centre)) < spill * 0.3, "Only a quarter of it should lie at the centre: " + soil.get(h.absolutePos(centre)));
                    double walls = 0;
                    for (BlockPos p : BlockPos.betweenClosed(4, 2, 4, 12, 8, 12)) {
                        if (p.getX() == 4 || p.getX() == 12 || p.getZ() == 4 || p.getZ() == 12 || p.getY() == 2 || p.getY() == 8) {
                            walls += soil.get(h.absolutePos(p));
                        }
                    }
                    h.assertTrue(walls > spill * 0.6, "Most of it should lie on the inside of the shell: " + walls + " of " + spill);
                    List<Radiation.Source> sources = Radiation.blockSources(h.getLevel());
                    BlockPos piece = h.absolutePos(debris.get(0));
                    h.assertTrue(sources.stream().anyMatch(s -> s.pos().distanceTo(Vec3.atCenterOf(piece)) < 0.1 && s.strength() == Radiation.DEBRIS_DOSE), "Core Debris should be a radiation source where it lies");
                    Aftermath.Site site = Aftermath.Sites.get(h.getLevel()).all().stream().filter(s -> s.centre().equals(h.absolutePos(centre))).findFirst().orElse(null);
                    h.assertTrue(site != null && site.radius() == 8 && site.points().size() > debris.size() && !site.cleared(), "The explosion should be recorded as a site to recover");
                    h.assertTrue(!Aftermath.clear(h.getLevel(), site), "A site with debris and contamination in it should not read clear");
                    for (BlockPos p : debris) {
                        h.setBlock(p, Blocks.AIR);
                    }
                    for (BlockPos p : find(h, CMBlocks.CORIUM.get(), 0, 0, 0, 15, 15, 15)) {
                        h.setBlock(p, Blocks.AIR);
                    }
                    h.assertTrue(!Aftermath.clear(h.getLevel(), site), "Contamination alone should keep the site from reading clear");
                    for (BlockPos p : BlockPos.betweenClosed(3, 1, 3, 13, 9, 13)) {
                        soil.clear(h.absolutePos(p));
                    }
                    double[] reading = new double[1];
                    Aftermath.worst(h.getLevel(), site, reading);
                    h.assertTrue(Aftermath.clear(h.getLevel(), site), "With the debris, the corium and the contamination gone the site should read clear: " + reading[0] + " at " + heart);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void aWeldingTorchCutsWreckageIntoScrap(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 1, 2);
        h.setBlock(at.below(), Blocks.STONE);
        h.setBlock(at, CMBlocks.WRECKAGE.get());
        WeldingTorchItem.cut(h.getLevel(), h.absolutePos(at), null);
        h.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    h.assertBlockPresent(Blocks.AIR, at);
                    List<ItemEntity> dropped = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(at)).inflate(2));
                    h.assertTrue(dropped.stream().anyMatch(e -> e.getItem().is(CMItems.STEEL_PLATE.get())), "Cutting Wreckage should give back a Steel Plate");
                    dropped.forEach(ItemEntity::discard);
                })
                .thenSucceed();
    }
}
