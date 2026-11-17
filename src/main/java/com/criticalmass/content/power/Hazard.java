package com.criticalmass.content.power;

import com.criticalmass.content.effects.Effects;
import com.criticalmass.registry.CMConfig;
import com.criticalmass.registry.CMSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Hazard {
    private Hazard() {
    }

    public static void sparks(Level level, Vec3 at, int count) {
        if (level.isClientSide) {
            return;
        }
        Effects.pop(level, at, Math.min(1.5f, count / 10f));
        Effects.sound(level, at, CMSounds.ARC_ZAP.get(), 0.5f + Math.min(0.8f, count / 20f), 0.9f + level.random.nextFloat() * 0.3f);
    }

    public static void fire(Level level, BlockPos pos) {
        if (!CMConfig.ELECTRICAL_FIRES.get()) {
            return;
        }
        ignite(level, pos);
        for (Direction side : Direction.values()) {
            ignite(level, pos.relative(side));
        }
    }

    private static void ignite(Level level, BlockPos pos) {
        if (level.getBlockState(pos).isAir() && BaseFireBlock.canBePlacedAt(level, pos, Direction.UP)) {
            level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
        }
    }

    public static void blast(Level level, Vec3 at, float power) {
        if (level.isClientSide) {
            return;
        }
        Effects.blast(level, at, power / 2f);
        Effects.sound(level, at, CMSounds.ARC_BLAST.get(), 2.0f + power, 0.85f + level.random.nextFloat() * 0.2f);
        if (CMConfig.ELECTRICAL_BLASTS_BREAK_BLOCKS.get()) {
            level.explode(null, at.x, at.y, at.z, power, false, Level.ExplosionInteraction.BLOCK);
            return;
        }
        double reach = power * 2.0;
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(reach))) {
            double distance = Math.sqrt(near.distanceToSqr(at));
            if (distance >= reach) {
                continue;
            }
            double share = 1 - distance / reach;
            near.hurt(level.damageSources().explosion(null, null), (float) (share * power * 5));
            Vec3 push = near.position().add(0, near.getBbHeight() / 2, 0).subtract(at).normalize().scale(share * 0.9);
            near.push(push.x, Math.max(0.15, push.y) * 0.8, push.z);
            near.hurtMarked = true;
        }
    }

    public static void destroy(Level level, BlockPos pos, float power) {
        if (level.isClientSide || level.getBlockState(pos).isAir()) {
            return;
        }
        level.removeBlock(pos, false);
        Vec3 at = Vec3.atCenterOf(pos);
        blast(level, at, power);
        Effects.oilFire(level, at, 3f + power * 1.5f);
        fire(level, pos);
    }

    public static void burnWire(Level level, BlockPos owner, WireLinks.Link link, boolean flashover) {
        Vec3 from = PowerNetwork.terminal(level, owner, link.mine());
        Vec3 to = PowerNetwork.terminal(level, link.other(), link.theirs());
        if (!WireLinks.cut(level, owner, link)) {
            return;
        }
        Vec3 middle = from.add(to).scale(0.5);
        if (flashover) {
            Effects.arc(level, from, to, 1.6f);
            blast(level, middle, 2.0f);
        } else {
            Effects.arc(level, from, middle, 0.7f);
            Effects.arc(level, to, middle, 0.7f);
            Effects.pop(level, middle, 1.2f);
            Effects.sound(level, middle, CMSounds.WIRE_SIZZLE.get(), 1.4f, 0.9f + level.random.nextFloat() * 0.2f);
        }
        if (CMConfig.ELECTRICAL_FIRES.get()) {
            beside(level, owner);
            beside(level, link.other());
            below(level, BlockPos.containing(middle));
        }
    }

    private static void beside(Level level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            BlockPos at = pos.relative(side);
            if (level.getBlockState(at).isAir() && BaseFireBlock.canBePlacedAt(level, at, Direction.UP)) {
                level.setBlockAndUpdate(at, BaseFireBlock.getState(level, at));
                return;
            }
        }
    }

    private static void below(Level level, BlockPos pos) {
        BlockPos at = pos;
        for (int i = 0; i < 12 && level.getBlockState(at).isAir() && level.getBlockState(at.below()).isAir(); i++) {
            at = at.below();
        }
        ignite(level, at);
    }

    public static void shock(Player player, Vec3 at, double kilovolts) {
        if (kilovolts <= 0 || !CMConfig.LIVE_WORK_HURTS.get() || player.level().isClientSide) {
            return;
        }
        Level level = player.level();
        boolean low = kilovolts <= Power.MOTOR_KV * Power.OVERVOLTAGE;
        boolean middle = kilovolts <= Power.GENERATOR_KV * Power.OVERVOLTAGE;
        Effects.arc(level, at, player.position().add(0, player.getBbHeight() * 0.6, 0), low ? 0.8f : middle ? 1.3f : 2.0f);
        Effects.sound(level, at, CMSounds.ARC_ZAP.get(), low ? 1.0f : 1.6f, low ? 1.1f : 0.85f);
        player.hurt(level.damageSources().lightningBolt(), low ? 8f : middle ? 16f : 40f);
    }
}
