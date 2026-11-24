package com.criticalmass.content.radiation;

import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class Fallout extends SavedData {
    public static final double PEAK_RATE = 900.0;
    public static final double SHINE = 0.3;
    public static final double SHELTER = 0.3;
    public static final double UPTAKE = 0.0005;
    public static final double WIDTH = 2.2;
    public static final double LENGTH = 5.0;
    public static final double SHIFT = 2.5;
    public static final double EARLY_REACH = 0.35;
    public static final int SPREAD_TICKS = 1800;
    public static final int ASH_DELAY = 200;
    public static final int ASH_TICKS = 3600;
    public static final int ASH_PER_TICK = 30;
    public static final double ASH_CONTAMINATION = 1.5;
    public static final double SPENT = 0.01;
    public static final double SEEN_FROM = 320.0;
    public static final double SICKENS = 0.2;
    public static final int SICKEN_TICKS = 40;
    private static final String NAME = "criticalmass_fallout";
    private final List<Plume> plumes = new ArrayList<>();

    public static final class Plume {
        final double x;
        final double y;
        final double z;
        final double radius;
        final double windX;
        final double windZ;
        final long born;

        Plume(double x, double y, double z, double radius, double windX, double windZ, long born) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.windX = windX;
            this.windZ = windZ;
            this.born = born;
        }

        public Vec3 centre() {
            return new Vec3(x, y, z);
        }

        public double radius() {
            return radius;
        }

        public double strength(Level level) {
            return strength(level.getGameTime());
        }

        double reach(long now) {
            return EARLY_REACH + (1 - EARLY_REACH) * Math.min(1.0, (now - born) / (double) SPREAD_TICKS);
        }

        double strength(long now) {
            return Math.pow(0.5, (now - born) / (CMConfig.falloutHalfLife() * 1200.0));
        }

        double share(double px, double pz, long now) {
            double dx = px - x;
            double dz = pz - z;
            double reach = reach(now);
            double along = (dx * windX + dz * windZ - SHIFT * radius * reach) / (LENGTH * radius * reach);
            double across = (dz * windX - dx * windZ) / (WIDTH * radius * reach);
            return Math.max(0.0, 1.0 - Math.sqrt(along * along + across * across));
        }
    }

    public static Fallout get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Fallout::new, Fallout::load, null), NAME);
    }

    public static void start(ServerLevel level, BlockPos centre, double radius) {
        if (!CMConfig.fallout()) {
            return;
        }
        double angle = level.random.nextDouble() * Math.PI * 2;
        start(level, centre, radius, Math.cos(angle), Math.sin(angle));
    }

    public static void start(ServerLevel level, BlockPos centre, double radius, double windX, double windZ) {
        Fallout data = get(level);
        data.plumes.add(new Plume(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, radius, windX, windZ, level.getGameTime()));
        data.setDirty();
    }

    public static int clear(ServerLevel level) {
        Fallout data = get(level);
        int n = data.plumes.size();
        data.plumes.clear();
        data.setDirty();
        return n;
    }

    public static int clear(ServerLevel level, BlockPos centre) {
        Fallout data = get(level);
        int before = data.plumes.size();
        data.plumes.removeIf(p -> Mth.floor(p.x) == centre.getX() && Mth.floor(p.y) == centre.getY() && Mth.floor(p.z) == centre.getZ());
        data.setDirty();
        return before - data.plumes.size();
    }

    public static int count(ServerLevel level) {
        return get(level).plumes.size();
    }

    public static double intensity(ServerLevel level, Vec3 at) {
        long now = level.getGameTime();
        double sum = 0;
        for (Plume p : get(level).plumes) {
            sum += p.share(at.x, at.z, now) * p.strength(now);
        }
        return Math.min(1.5, sum);
    }

    public static boolean sheltered(Level level, Vec3 at) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(at.x), Mth.floor(at.z)) > at.y;
    }

    public static double rate(ServerLevel level, Vec3 at) {
        double intensity = intensity(level, at);
        return intensity <= 0 ? 0 : intensity * PEAK_RATE * (sheltered(level, at) ? SHELTER : 1.0);
    }

    public static Vec3 wind(ServerLevel level, Vec3 at) {
        long now = level.getGameTime();
        Plume best = null;
        double most = 0;
        for (Plume p : get(level).plumes) {
            double here = p.share(at.x, at.z, now) * p.strength(now);
            if (here > most) {
                most = here;
                best = p;
            }
        }
        return best == null ? Vec3.ZERO : new Vec3(best.windX, 0, best.windZ);
    }

    @org.jetbrains.annotations.Nullable
    public static Plume nearest(ServerLevel level, Vec3 at) {
        Plume best = null;
        double nearest = SEEN_FROM * SEEN_FROM;
        for (Plume p : get(level).plumes) {
            double distance = (p.x - at.x) * (p.x - at.x) + (p.z - at.z) * (p.z - at.z);
            if (distance < nearest) {
                nearest = distance;
                best = p;
            }
        }
        return best;
    }

    private static void sicken(ServerLevel level, Plume p, long now) {
        double span = (SHIFT + LENGTH) * p.radius * p.reach(now);
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(p.x - span, p.y - 48, p.z - span, p.x + span, p.y + 96, p.z + span);
        for (net.minecraft.world.entity.LivingEntity entity : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box, e -> !(e instanceof net.minecraft.world.entity.player.Player))) {
            if (p.share(entity.getX(), entity.getZ(), now) * p.strength(now) > SICKENS && !sheltered(level, entity.getEyePosition())) {
                if (Irradiation.is(entity) || CMConfig.mutations() && entity.getHealth() <= entity.getMaxHealth() * Irradiation.TURNS_BELOW && Irradiation.apply(entity)) {
                    continue;
                }
                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, SICKEN_TICKS * 3, 0));
            }
        }
    }

    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Irradiation.groundTick(level);
        Fallout data = get(level);
        if (data.plumes.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        if (data.plumes.removeIf(p -> p.strength(now) < SPENT)) {
            data.setDirty();
        }
        for (Plume p : data.plumes) {
            if (now - p.born >= ASH_DELAY && now - p.born < ASH_DELAY + ASH_TICKS) {
                settle(level, p, now);
            }
            if (now % SICKEN_TICKS == 0 && CMConfig.effects()) {
                sicken(level, p, now);
            }
        }
    }

    private static void settle(ServerLevel level, Plume p, long now) {
        RandomSource random = level.random;
        double reach = p.reach(now);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < ASH_PER_TICK; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double out = Math.sqrt(random.nextDouble());
            double along = SHIFT * p.radius * reach + Math.cos(angle) * out * LENGTH * p.radius * reach;
            double across = Math.sin(angle) * out * WIDTH * p.radius * reach;
            int x = Mth.floor(p.x + along * p.windX - across * p.windZ);
            int z = Mth.floor(p.z + along * p.windZ + across * p.windX);
            if (!level.hasChunk(x >> 4, z >> 4) || random.nextDouble() > 0.25 + p.share(x + 0.5, z + 0.5, now)) {
                continue;
            }
            cursor.set(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
            dust(level, cursor, ASH_CONTAMINATION);
        }
    }

    public static boolean dust(ServerLevel level, BlockPos.MutableBlockPos cursor, double contamination) {
        BlockState here = level.getBlockState(cursor);
        if (!here.isAir() && !(here.canBeReplaced() && here.getFluidState().isEmpty())) {
            return false;
        }
        BlockPos under = cursor.below();
        BlockState ground = level.getBlockState(under);
        if (!ground.isCollisionShapeFullBlock(level, under) || ground.is(CMBlocks.FALLOUT_ASH.get()) || ground.is(Blocks.MAGMA_BLOCK) || ground.is(CMBlocks.CORIUM.get()) || !ground.getFluidState().isEmpty() || !ground.isFaceSturdy(level, under, Direction.UP)) {
            return false;
        }
        level.setBlock(cursor, CMBlocks.FALLOUT_ASH.get().defaultBlockState(), Block.UPDATE_ALL);
        ContaminationData.get(level).add(cursor.immutable(), contamination);
        if (ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.PODZOL) || ground.is(Blocks.MYCELIUM)) {
            level.setBlock(under, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    public static Fallout load(CompoundTag tag, HolderLookup.Provider registries) {
        Fallout data = new Fallout();
        for (Tag t : tag.getList("Plumes", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            data.plumes.add(new Plume(c.getDouble("X"), c.getDouble("Y"), c.getDouble("Z"), c.getDouble("Radius"), c.getDouble("WindX"), c.getDouble("WindZ"), c.getLong("Born")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Plume p : plumes) {
            CompoundTag c = new CompoundTag();
            c.putDouble("X", p.x);
            c.putDouble("Y", p.y);
            c.putDouble("Z", p.z);
            c.putDouble("Radius", p.radius);
            c.putDouble("WindX", p.windX);
            c.putDouble("WindZ", p.windZ);
            c.putLong("Born", p.born);
            list.add(c);
        }
        tag.put("Plumes", list);
        return tag;
    }
}
