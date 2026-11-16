package com.criticalmass.content.power;

import com.criticalmass.registry.CMConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.Vec3;

public final class Hazard {
    private Hazard() {
    }

    public static void sparks(Level level, Vec3 at, int count) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, count, 0.25, 0.25, 0.25, 0.4);
            server.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, count / 3, 0.2, 0.2, 0.2, 0.02);
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 1.5f, 1.4f);
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
        sparks(level, at, 30);
        level.explode(null, at.x, at.y, at.z, power, false, CMConfig.ELECTRICAL_BLASTS_BREAK_BLOCKS.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
    }

    public static void destroy(Level level, BlockPos pos, float power) {
        if (level.isClientSide || level.getBlockState(pos).isAir()) {
            return;
        }
        level.removeBlock(pos, false);
        blast(level, Vec3.atCenterOf(pos), power);
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
            blast(level, middle, 2.0f);
        } else {
            sparks(level, middle, 12);
        }
        sparks(level, from, 6);
        sparks(level, to, 6);
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
        sparks(player.level(), at, 16);
        float harm = kilovolts <= Power.MOTOR_KV * Power.OVERVOLTAGE ? 8f : kilovolts <= Power.GENERATOR_KV * Power.OVERVOLTAGE ? 16f : 40f;
        player.hurt(player.level().damageSources().lightningBolt(), harm);
    }
}
