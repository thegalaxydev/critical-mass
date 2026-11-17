package com.criticalmass.content.power;

import com.criticalmass.registry.CMConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.jetbrains.annotations.Nullable;

public final class Storms {
    public static final String BOLT_TAG = "criticalmass_strike";
    private static final Map<Level, int[]> QUIET = new WeakHashMap<>();
    private static final Map<Level, List<Vec3>> PENDING = new WeakHashMap<>();

    private Storms() {
    }

    public static boolean enabled() {
        return CMConfig.ELECTRICAL_WEATHER.get();
    }

    static void grid(Level level, Collection<BlockPos> ties) {
        if (ties.isEmpty() || !enabled() || !level.isThundering()) {
            return;
        }
        int[] quiet;
        synchronized (QUIET) {
            quiet = QUIET.computeIfAbsent(level, k -> new int[] {Power.STORM_QUIET_STEPS});
        }
        if (quiet[0]-- > 0) {
            return;
        }
        RandomSource random = level.random;
        quiet[0] = Power.STORM_QUIET_STEPS + random.nextInt(Power.STORM_QUIET_STEPS);
        int roll = random.nextInt(10);
        GridTieBlockEntity.Event kind = roll < 5 ? GridTieBlockEntity.Event.DIP : roll < 9 ? GridTieBlockEntity.Event.SWING : GridTieBlockEntity.Event.OUTAGE;
        int ticks = switch (kind) {
            case DIP -> 60 + random.nextInt(140);
            case SWING -> 200 + random.nextInt(200);
            default -> 200 + random.nextInt(600);
        };
        double amount = switch (kind) {
            case DIP -> Power.DIP_FLOOR + random.nextDouble() * (Power.DIP_CEILING - Power.DIP_FLOOR);
            case SWING -> (random.nextBoolean() ? 1 : -1) * (0.2 + random.nextDouble() * (Power.SWING_HZ - 0.2));
            default -> 0;
        };
        for (BlockPos pos : ties) {
            if (level.getBlockEntity(pos) instanceof GridTieBlockEntity tie && tie.event() == GridTieBlockEntity.Event.NORMAL) {
                tie.disturb(kind, ticks, amount);
            }
        }
    }

    static void bolt(Level level, Vec3 at) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(at);
        bolt.setVisualOnly(true);
        bolt.addTag(BOLT_TAG);
        server.addFreshEntity(bolt);
    }

    @Nullable
    static BlockPos rod(Level level, Vec3 at) {
        BlockPos centre = BlockPos.containing(at);
        BlockPos best = null;
        double nearest = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -Power.ROD_REACH; dx <= Power.ROD_REACH; dx++) {
            for (int dz = -Power.ROD_REACH; dz <= Power.ROD_REACH; dz++) {
                for (int dy = -4; dy <= 24; dy++) {
                    cursor.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    if (level.getBlockState(cursor).is(Blocks.LIGHTNING_ROD)) {
                        double distance = dx * dx + dz * dz;
                        if (distance < nearest) {
                            nearest = distance;
                            best = cursor.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        Level level = event.getLevel();
        if (level.isClientSide() || !(event.getEntity() instanceof LightningBolt bolt) || bolt.getTags().contains(BOLT_TAG) || !enabled()) {
            return;
        }
        BlockPos at = bolt.blockPosition();
        if (level.getBlockState(at).is(Blocks.LIGHTNING_ROD) || level.getBlockState(at.below()).is(Blocks.LIGHTNING_ROD)) {
            return;
        }
        synchronized (PENDING) {
            PENDING.computeIfAbsent(level, k -> new ArrayList<>()).add(bolt.position());
        }
    }

    static List<Vec3> pending(Level level) {
        synchronized (PENDING) {
            List<Vec3> waiting = PENDING.remove(level);
            return waiting == null ? List.of() : waiting;
        }
    }
}
