package com.criticalmass.content.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class Effects {
    public static final int ARC = 0;
    public static final int BLAST = 1;
    public static final int POP = 2;
    public static final int STEAM_JET = 3;
    public static final int STEAM_BURST = 4;
    public static final int OIL_FIRE = 5;
    public static final int NUKE = 6;
    private static final double REACH = 96.0;
    private static final double FAR_REACH = 1024.0;

    public interface Client {
        void loop(BlockPos pos, SoundEvent sound, float volume, float pitch);
    }

    public static Client client = (pos, sound, volume, pitch) -> {
    };

    private Effects() {
    }

    private static void send(Level level, int kind, Vec3 from, Vec3 to, float power) {
        if (level instanceof ServerLevel server) {
            PacketDistributor.sendToPlayersNear(server, null, from.x, from.y, from.z, REACH, new EffectPacket(kind, from, to, power));
        }
    }

    public static void arc(Level level, Vec3 from, Vec3 to, float power) {
        send(level, ARC, from, to, power);
    }

    public static void blast(Level level, Vec3 at, float power) {
        send(level, BLAST, at, at, power);
    }

    public static void pop(Level level, Vec3 at, float power) {
        send(level, POP, at, at, power);
    }

    public static void steamJet(Level level, Vec3 from, Vec3 direction, float power) {
        send(level, STEAM_JET, from, from.add(direction), power);
    }

    public static void steamBurst(Level level, Vec3 at, float power) {
        send(level, STEAM_BURST, at, at, power);
    }

    public static void oilFire(Level level, Vec3 at, float seconds) {
        send(level, OIL_FIRE, at, at, seconds);
    }

    public static void nuke(Level level, Vec3 at, float radius) {
        if (level instanceof ServerLevel server) {
            PacketDistributor.sendToPlayersNear(server, null, at.x, at.y, at.z, FAR_REACH, new EffectPacket(NUKE, at, at, radius));
        }
    }

    public static void sound(Level level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.BLOCKS, volume, pitch);
    }

    public static void loop(Level level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        if (level != null && level.isClientSide && volume > 0.01f) {
            client.loop(pos, sound, volume, pitch);
        }
    }
}
