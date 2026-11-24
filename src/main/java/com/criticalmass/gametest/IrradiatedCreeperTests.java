package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.radiation.ContaminationData;
import com.criticalmass.content.radiation.Fallout;
import com.criticalmass.content.radiation.IrradiatedCreeper;
import com.criticalmass.content.radiation.Radiation;
import com.criticalmass.content.radiation.RadiationEvents;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class IrradiatedCreeperTests {
    private static final String LARGE = "empty_large";

    private IrradiatedCreeperTests() {
    }

    private static void floor(GameTestHelper h) {
        for (int x = 1; x <= 14; x++) {
            for (int z = 1; z <= 14; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
    }

    private static int ash(GameTestHelper h) {
        int n = 0;
        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z++) {
                for (int y = 1; y <= 4; y++) {
                    if (h.getBlockState(new BlockPos(x, y, z)).is(CMBlocks.FALLOUT_ASH.get())) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void irradiatedCreeperIsASourceThatShrugsOffSickness(GameTestHelper h) {
        floor(h);
        BlockPos at = new BlockPos(8, 2, 8);
        Vec3 beside = Vec3.atCenterOf(h.absolutePos(at.east(2).above()));
        double before = Radiation.ambientRate(h.getLevel(), beside);
        IrradiatedCreeper creeper = h.spawnWithNoFreeWill(CMEntities.IRRADIATED_CREEPER.get(), at);
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    double rate = Radiation.ambientRate(h.getLevel(), beside) - before;
                    h.assertTrue(rate > 0.2 && rate < IrradiatedCreeper.DOSE, "A survey beside an Irradiated Creeper should read it: " + rate);
                    creeper.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0));
                    creeper.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
                    h.assertTrue(!creeper.hasEffect(MobEffects.WITHER) && !creeper.hasEffect(MobEffects.POISON), "Radiation sickness should not take hold of it");
                    h.assertTrue(creeper.getMaxHealth() == 26.0F, "It should be tougher than a Creeper: " + creeper.getMaxHealth());
                    creeper.discard();
                })
                .thenIdle(2)
                .thenExecute(() -> h.assertTrue(ash(h) == 0, "One that is only removed should leave no ash: " + ash(h)))
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void irradiatedCreeperBurstsIntoFallout(GameTestHelper h) {
        floor(h);
        BlockPos at = new BlockPos(8, 2, 8);
        IrradiatedCreeper creeper = h.spawnWithNoFreeWill(CMEntities.IRRADIATED_CREEPER.get(), at);
        Player near = h.makeMockPlayer(GameType.SURVIVAL);
        Vec3 stand = Vec3.atBottomCenterOf(h.absolutePos(at.east(3)));
        near.setPos(stand.x, stand.y, stand.z);
        h.getLevel().addFreshEntity(near);
        ContaminationData cells = ContaminationData.get(h.getLevel());
        double before = cells.total();
        h.startSequence()
                .thenIdle(2)
                .thenExecute(creeper::ignite)
                .thenWaitUntil(() -> h.assertTrue(!creeper.isAlive(), "It should go off"))
                .thenIdle(2)
                .thenExecute(() -> {
                    int laid = ash(h);
                    h.assertTrue(laid >= 6, "The burst should scatter Fallout Ash: " + laid);
                    h.assertTrue(cells.total() - before > laid * IrradiatedCreeper.BURST_CONTAMINATION * 0.9, "Its ash should be contamination: " + (cells.total() - before));
                    h.assertTrue(RadiationEvents.data(near).contamination > 0 && RadiationEvents.data(near).body > 0, "Someone beside it should be dosed and dusted");
                    near.discard();
                    for (int x = 0; x <= 15; x++) {
                        for (int z = 0; z <= 15; z++) {
                            for (int y = 1; y <= 4; y++) {
                                if (h.getBlockState(new BlockPos(x, y, z)).is(CMBlocks.FALLOUT_ASH.get())) {
                                    h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                                }
                            }
                        }
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void creepersMutateOnContaminatedGround(GameTestHelper h) {
        floor(h);
        BlockPos at = new BlockPos(8, 2, 8);
        ContaminationData cells = ContaminationData.get(h.getLevel());
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-1, 0, -1), at.offset(1, 0, 1))) {
            h.setBlock(p, CMBlocks.FALLOUT_ASH.get());
            cells.add(h.absolutePos(p), Fallout.ASH_CONTAMINATION);
        }
        Creeper clean = h.spawnWithNoFreeWill(EntityType.CREEPER, new BlockPos(2, 2, 2));
        Creeper exposed = h.spawnWithNoFreeWill(EntityType.CREEPER, at);
        AABB box = new AABB(h.absolutePos(new BlockPos(0, 1, 0))).expandTowards(16, 6, 16);
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    h.assertTrue(cells.near(exposed.position().add(0, 0.5, 0), 2) > IrradiatedCreeper.MUTATING_GROUND, "The ash should be enough to mutate on");
                    h.assertTrue(cells.near(clean.position().add(0, 0.5, 0), 2) < IrradiatedCreeper.MUTATING_GROUND, "Clean ground should not be");
                    IrradiatedCreeper made = IrradiatedCreeper.mutate(exposed);
                    h.assertTrue(made != null && !exposed.isAlive(), "A Creeper should turn into an Irradiated Creeper");
                    h.assertTrue(IrradiatedCreeper.mutate(made) == null, "One that has turned should not turn again");
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    h.assertTrue(h.getLevel().getEntitiesOfClass(IrradiatedCreeper.class, box).size() == 1 && clean.isAlive(), "Only the one on the ash should have turned");
                    for (BlockPos p : BlockPos.betweenClosed(at.offset(-1, 0, -1), at.offset(1, 0, 1))) {
                        h.setBlock(p, Blocks.AIR);
                    }
                })
                .thenSucceed();
    }
}
