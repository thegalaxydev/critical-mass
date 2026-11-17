package com.criticalmass.content.thermal;

import com.criticalmass.content.effects.Effects;
import com.criticalmass.registry.CMSounds;
import net.minecraft.world.phys.Vec3;
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
        Effects.steamBurst(level, new Vec3(x, y, z), 1.0f);
        level.playSound(null, pos, CMSounds.PIPE_BURST.get(), SoundSource.BLOCKS, 1.5f, 1.0f + level.random.nextFloat() * 0.15f);
        level.destroyBlock(pos, false);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(1.5))) {
            e.hurt(level.damageSources().hotFloor(), CMThermal.PIPE_BURST_DAMAGE);
        }
    }
}
