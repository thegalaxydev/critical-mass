package com.criticalmass.content.thermal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class PipeBurst {
    private PipeBurst() {
    }

    public static void burst(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!PipeCoolant.isPipe(state)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, x, y, z, 24, 0.3, 0.3, 0.3, 0.08);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 6, 0.2, 0.2, 0.2, 0.02);
        }
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.5f, 1.6f);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.8f);
        level.destroyBlock(pos, false);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(1.5))) {
            e.hurt(level.damageSources().hotFloor(), CMThermal.PIPE_BURST_DAMAGE);
        }
    }
}
