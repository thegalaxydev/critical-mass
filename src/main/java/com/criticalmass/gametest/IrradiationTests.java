package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.radiation.Fallout;
import com.criticalmass.content.radiation.Irradiation;
import com.criticalmass.content.radiation.Radiation;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMItems;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class IrradiationTests {
    private static final String LARGE = "empty_large";

    private IrradiationTests() {
    }

    private static void floor(GameTestHelper h) {
        for (int x = 1; x <= 14; x++) {
            for (int z = 1; z <= 14; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
    }

    private static void sweep(GameTestHelper h) {
        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z++) {
                for (int y = 1; y <= 4; y++) {
                    if (h.getBlockState(new BlockPos(x, y, z)).is(CMBlocks.FALLOUT_ASH.get())) {
                        h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                    }
                }
            }
        }
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void irradiatedMobsGrowToughenAndTurnOnPlayers(GameTestHelper h) {
        floor(h);
        BlockPos at = new BlockPos(8, 2, 8);
        Vec3 beside = Vec3.atCenterOf(h.absolutePos(at.east(2).above()));
        double before = Radiation.ambientRate(h.getLevel(), beside);
        Cow cow = h.spawn(EntityType.COW, at);
        Zombie zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 4));
        cow.setNoAi(true);
        zombie.setNoAi(true);
        double zombieDamage = zombie.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    h.assertTrue(!Irradiation.is(cow) && !Irradiation.aggressive(cow), "A cow should start out ordinary");
                    h.assertTrue(Irradiation.apply(cow) && Irradiation.apply(zombie), "A cow and a zombie should both turn");
                    h.assertTrue(!Irradiation.apply(cow), "A mob should not turn twice");
                    h.assertTrue(Irradiation.is(cow) && cow.getScale() > 1.3F, "An irradiated cow should be bigger: " + cow.getScale());
                    h.assertTrue(cow.getMaxHealth() == 20.0F && cow.getHealth() == 20.0F, "It should have twice a cow's health and be healed: " + cow.getHealth() + " of " + cow.getMaxHealth());
                    h.assertTrue(Irradiation.aggressive(cow) && Irradiation.aggressive(zombie), "Both should hunt players");
                    h.assertTrue(zombie.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) > zombieDamage, "A zombie should hit harder");
                    h.assertTrue(cow.getName().getString().startsWith("Irradiated "), "It should be named for what it is: " + cow.getName().getString());
                    cow.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0));
                    h.assertTrue(!cow.hasEffect(MobEffects.WITHER), "Fallout sickness should not take hold of it");
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    double rate = Radiation.ambientRate(h.getLevel(), beside) - before;
                    h.assertTrue(rate > 0.2, "A survey beside an irradiated cow should read it: " + rate);
                    cow.discard();
                    zombie.discard();
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void petsAndGolemsAreNotIrradiated(GameTestHelper h) {
        floor(h);
        IronGolem golem = h.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 2, 4));
        Wolf pet = h.spawn(EntityType.WOLF, new BlockPos(8, 2, 8));
        Wolf wild = h.spawn(EntityType.WOLF, new BlockPos(11, 2, 11));
        golem.setNoAi(true);
        pet.setNoAi(true);
        wild.setNoAi(true);
        pet.setOwnerUUID(UUID.randomUUID());
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    h.assertTrue(!Irradiation.apply(golem) && !Irradiation.is(golem), "An Iron Golem should not turn");
                    h.assertTrue(!Irradiation.apply(pet) && !Irradiation.is(pet), "Someone's wolf should not turn");
                    h.assertTrue(Irradiation.apply(wild) && Irradiation.aggressive(wild), "A wild wolf should");
                    golem.discard();
                    pet.discard();
                    wild.discard();
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 200, skyAccess = true)
    public static void falloutTurnsTheMobsItHasSickened(GameTestHelper h) {
        floor(h);
        BlockPos heart = new BlockPos(5, 2, 8);
        Vec3 east = Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(1, 0, 0))).subtract(Vec3.atLowerCornerOf(h.absolutePos(BlockPos.ZERO)));
        Fallout.clear(h.getLevel(), h.absolutePos(heart));
        Pig weak = h.spawn(EntityType.PIG, new BlockPos(8, 2, 7));
        Pig hale = h.spawn(EntityType.PIG, new BlockPos(8, 2, 9));
        weak.setNoAi(true);
        hale.setNoAi(true);
        weak.setHealth(4.0F);
        Fallout.start(h.getLevel(), h.absolutePos(heart), 3, east.x, east.z);
        h.startSequence()
                .thenIdle(45)
                .thenExecute(() -> {
                    h.assertTrue(Irradiation.is(weak) && weak.getHealth() == weak.getMaxHealth(), "A mob the fallout has half killed should turn and be whole again: " + weak.getHealth());
                    h.assertTrue(!Irradiation.is(hale) && hale.hasEffect(MobEffects.WITHER), "A healthy one should only sicken at first");
                    Fallout.clear(h.getLevel(), h.absolutePos(heart));
                    weak.discard();
                    hale.discard();
                    sweep(h);
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void irradiatedMobsLeaveAshWhenTheyDie(GameTestHelper h) {
        floor(h);
        BlockPos at = new BlockPos(8, 2, 8);
        Chicken chicken = h.spawn(EntityType.CHICKEN, at);
        chicken.setNoAi(true);
        AABB box = new AABB(h.absolutePos(at)).inflate(3);
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    h.assertTrue(Irradiation.apply(chicken), "A chicken should turn");
                    chicken.kill();
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, box, item -> item.getItem().is(CMItems.FALLOUT_ASH.get())).size() == 1, "It should drop Fallout Ash");
                    h.assertBlockPresent(CMBlocks.FALLOUT_ASH.get(), at);
                    h.getLevel().getEntitiesOfClass(ItemEntity.class, box).forEach(ItemEntity::discard);
                    sweep(h);
                })
                .thenSucceed();
    }
}
