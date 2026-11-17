package com.criticalmass.content.thermal;

import com.criticalmass.content.effects.Effects;
import com.criticalmass.registry.CMSounds;
import com.criticalmass.registry.CMParticles;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

public final class ThermalSafety {
    private ThermalSafety() {
    }

    public static boolean ruptureIfNeeded(Level level, BlockPos pos, CoolantBuffer buffer, ThermalSinks sinks) {
        if (!buffer.steamMedium() || buffer.pressure() < CMThermal.RUPTURE_PRESSURE) {
            return false;
        }
        sinks.vent(buffer.take(buffer.amount()));
        sinks.ruptures++;
        level.playSound(null, pos, CMSounds.PIPE_BURST.get(), SoundSource.BLOCKS, 2.0f, 0.9f);
        Effects.steamBurst(level, Vec3.atCenterOf(pos), 2.0f);
        return true;
    }

    public static void steamPuff(Level level, BlockPos pos, double dx, double dy, double dz) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(CMParticles.STEAM.get(), pos.getX() + 0.5 + dx * 0.6, pos.getY() + 0.5 + dy * 0.6, pos.getZ() + 0.5 + dz * 0.6, 0, dx + (level.random.nextDouble() - 0.5) * 0.3, dy + 0.2, dz + (level.random.nextDouble() - 0.5) * 0.3, 0.12);
        }
    }
}
