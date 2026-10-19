package com.criticalmass.content.thermal;

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
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.0f, 1.4f);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 40, 0.6, 0.6, 0.6, 0.15);
        }
        return true;
    }

    public static void steamPuff(Level level, BlockPos pos, double dx, double dy, double dz) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5 + dx * 0.6, pos.getY() + 0.5 + dy * 0.6, pos.getZ() + 0.5 + dz * 0.6, 3, 0.1, 0.1, 0.1, 0.05);
        }
    }
}
