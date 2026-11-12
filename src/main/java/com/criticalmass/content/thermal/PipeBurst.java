package com.criticalmass.content.thermal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

public final class PipeBurst {
    private static final int SEARCH_LIMIT = 64;
    private static final List<Pending> PENDING = new ArrayList<>();

    private PipeBurst() {
    }

    private record Pending(Level level, BlockPos start, Direction from) {
    }

    public static void queueFrom(Level level, BlockPos origin, Direction side) {
        if (!level.isClientSide) {
            PENDING.add(new Pending(level, origin.relative(side), side));
        }
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        List<Pending> due = new ArrayList<>();
        PENDING.removeIf(p -> {
            if (p.level() == event.getLevel()) {
                due.add(p);
                return true;
            }
            return false;
        });
        for (Pending p : due) {
            BlockPos weak = weakestPipe(p.level(), p.start(), p.from());
            if (weak != null) {
                burst(p.level(), weak);
            }
        }
    }

    @Nullable
    public static BlockPos weakestPipe(Level level, BlockPos start, Direction from) {
        BlockState first = level.getBlockState(start);
        if (!PipeCoolant.open(level, start, first, from.getOpposite())) {
            return null;
        }
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && seen.size() <= SEARCH_LIMIT) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (PipeCoolant.isPipe(state) && !PipeCoolant.isReinforced(state)) {
                return pos;
            }
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                if (seen.contains(next) || !level.isLoaded(next) || !PipeCoolant.open(level, pos, state, d)) {
                    continue;
                }
                BlockState ns = level.getBlockState(next);
                if (PipeCoolant.open(level, next, ns, d.getOpposite())) {
                    seen.add(next);
                    queue.add(next);
                }
            }
        }
        return null;
    }

    public static void burst(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!PipeCoolant.isPipe(state) || PipeCoolant.isReinforced(state)) {
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
