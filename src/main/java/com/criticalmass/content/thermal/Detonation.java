package com.criticalmass.content.thermal;

import com.criticalmass.content.effects.Effects;
import com.criticalmass.content.radiation.ContaminationData;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMConfig;
import com.criticalmass.registry.CMSounds;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class Detonation {
    public static final TagKey<Block> BLAST_PROOF = BlockTags.create(ResourceLocation.fromNamespaceAndPath("criticalmass", "blast_proof"));
    public static final int FUSE_TICKS = 60;
    public static final double DEPTH_SQUASH = 2.2;
    public static final double LINING = 2.5;
    public static final double REACH = 1.9;
    private static final int CARVE_BUDGET = 9000;
    private static final int SCORCH_COLUMNS = 700;
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final String NAME = "criticalmass_detonations";
    private final BlockPos centre;
    private final int radius;
    private final boolean scorch;
    private int fuse;
    private double front;
    private int ring;
    private boolean carved;

    private Detonation(BlockPos centre, int radius, int fuse, boolean scorch) {
        this.centre = centre;
        this.radius = radius;
        this.fuse = fuse;
        this.scorch = scorch;
    }

    public static int radiusFor(int moltenMass) {
        double scale = Mth.clamp(Math.cbrt(Math.max(1, moltenMass) / 15.0), 0.7, 1.6);
        return (int) Math.round(CMConfig.explosionRadius() * scale);
    }

    public static void arm(ServerLevel level, BlockPos centre, int radius, int fuse, boolean scorch) {
        Store store = Store.get(level);
        for (Detonation d : store.list) {
            if (d.centre.equals(centre)) {
                return;
            }
        }
        store.list.add(new Detonation(centre.immutable(), Math.max(2, radius), fuse, scorch));
        store.setDirty();
    }

    private static List<Detonation> held(Level level) {
        return level instanceof ServerLevel server ? Store.get(server).list : List.of();
    }

    public static net.minecraft.nbt.CompoundTag saved(ServerLevel level) {
        return Store.get(level).write(new net.minecraft.nbt.CompoundTag());
    }

    public static void restore(ServerLevel level, net.minecraft.nbt.CompoundTag tag) {
        Store store = Store.get(level);
        store.list.clear();
        store.read(tag);
        store.setDirty();
    }

    private static final class Store extends net.minecraft.world.level.saveddata.SavedData {
        private final List<Detonation> list = new ArrayList<>();

        static Store get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(new net.minecraft.world.level.saveddata.SavedData.Factory<>(Store::new, (tag, registries) -> {
                Store store = new Store();
                store.read(tag);
                return store;
            }, null), NAME);
        }

        void read(net.minecraft.nbt.CompoundTag tag) {
            net.minecraft.nbt.ListTag all = tag.getList("Detonations", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < all.size(); i++) {
                net.minecraft.nbt.CompoundTag one = all.getCompound(i);
                int[] at = one.getIntArray("Centre");
                if (at.length != 3) {
                    continue;
                }
                Detonation d = new Detonation(new BlockPos(at[0], at[1], at[2]), one.getInt("Radius"), one.getInt("Fuse"), one.getBoolean("Scorch"));
                d.front = one.getDouble("Front");
                d.ring = one.getInt("Ring");
                d.carved = one.getBoolean("Carved");
                list.add(d);
            }
        }

        net.minecraft.nbt.CompoundTag write(net.minecraft.nbt.CompoundTag tag) {
            net.minecraft.nbt.ListTag all = new net.minecraft.nbt.ListTag();
            for (Detonation d : list) {
                net.minecraft.nbt.CompoundTag one = new net.minecraft.nbt.CompoundTag();
                one.putIntArray("Centre", new int[] {d.centre.getX(), d.centre.getY(), d.centre.getZ()});
                one.putInt("Radius", d.radius);
                one.putInt("Fuse", d.fuse);
                one.putBoolean("Scorch", d.scorch);
                one.putDouble("Front", d.front);
                one.putInt("Ring", d.ring);
                one.putBoolean("Carved", d.carved);
                all.add(one);
            }
            tag.put("Detonations", all);
            return tag;
        }

        @Override
        public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            return write(tag);
        }
    }

    public static boolean armed(Level level, BlockPos near, double reach) {
        for (Detonation d : held(level)) {
            if (d.centre.closerThan(near, reach)) {
                return true;
            }
        }
        return false;
    }

    public static boolean cancel(Level level, BlockPos near, double reach) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        Store store = Store.get(server);
        boolean stopped = store.list.removeIf(d -> d.fuse > 0 && d.centre.closerThan(near, reach));
        if (stopped) {
            store.setDirty();
        }
        return stopped;
    }

    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Store store = Store.get(level);
        List<Detonation> list = store.list;
        if (list.isEmpty()) {
            return;
        }
        store.setDirty();
        Iterator<Detonation> all = new ArrayList<>(list).iterator();
        while (all.hasNext()) {
            Detonation d = all.next();
            if (d.step(level)) {
                list.remove(d);
            }
        }
    }

    public static boolean survives(BlockState state, Level level, BlockPos pos) {
        return state.is(BLAST_PROOF) || state.getDestroySpeed(level, pos) < 0;
    }

    private double measure(int dx, int dy, int dz) {
        double v = dy < 0 ? dy * DEPTH_SQUASH : dy;
        return Math.sqrt(dx * dx + v * v + dz * dz);
    }

    private boolean step(ServerLevel level) {
        if (!level.isLoaded(centre)) {
            return false;
        }
        Vec3 core = Vec3.atCenterOf(centre);
        if (fuse > 0) {
            if (fuse % 10 == 0) {
                float build = 1f - fuse / (float) FUSE_TICKS;
                Effects.pop(level, core, 1.5f + build * 3f);
                level.playSound(null, centre, CMSounds.REACTOR_THRUM.get(), SoundSource.BLOCKS, 2.0f + build * 2f, 1.2f + build * 0.8f);
            }
            if (--fuse > 0) {
                return false;
            }
            flash(level, core);
            return false;
        }
        if (!carved) {
            carve(level);
            return false;
        }
        if (!scorch) {
            return true;
        }
        return scorch(level);
    }

    private void flash(ServerLevel level, Vec3 core) {
        Effects.nuke(level, core, radius);
        if (scorch) {
            com.criticalmass.content.radiation.Fallout.start(level, centre, radius);
        }
        double reach = radius * REACH;
        for (Entity entity : level.getEntities((Entity) null, new AABB(centre).inflate(reach), e -> true)) {
            double distance = Math.sqrt(entity.distanceToSqr(core));
            if (distance > reach) {
                continue;
            }
            if (entity instanceof ItemEntity) {
                entity.discard();
                continue;
            }
            if (!(entity instanceof LivingEntity living) || (living instanceof Player player && (player.isCreative() || player.isSpectator()))) {
                continue;
            }
            double near = 1 - distance / reach;
            living.hurt(level.damageSources().explosion(null, null), distance <= radius ? 10000f : (float) (8 + near * 60));
            if (living.isAlive()) {
                Vec3 push = living.position().subtract(core).normalize().scale(1.5 + near * 3).add(0, 0.6, 0);
                living.setDeltaMovement(living.getDeltaMovement().add(push));
                living.hurtMarked = true;
                living.igniteForSeconds((float) (4 + near * 10));
            }
        }
        ContaminationData.get(level).add(centre, CMThermal.CORIUM_SPILL * radius * CMConfig.get(CMConfig.CORIUM_CONTAMINATION));
    }

    private void carve(ServerLevel level) {
        double outer = radius + LINING;
        double from = front;
        double shell = 4 * Math.PI * Math.max(1, from * from) * 0.75;
        double to = Math.min(outer, from + Mth.clamp(CARVE_BUDGET / shell, 0.35, 3.0));
        RandomSource random = level.random;
        int reach = (int) Math.ceil(to);
        int down = (int) Math.ceil(to / DEPTH_SQUASH);
        int floor = level.getMinBuildHeight();
        int roof = level.getMaxBuildHeight() - 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (dx * dx + dz * dz >= to * to || !level.hasChunk((centre.getX() + dx) >> 4, (centre.getZ() + dz) >> 4)) {
                    continue;
                }
                for (int dy = -down; dy <= reach; dy++) {
                    double m = measure(dx, dy, dz);
                    if (m < from || m >= to) {
                        continue;
                    }
                    int y = centre.getY() + dy;
                    if (y <= floor || y > roof) {
                        continue;
                    }
                    cursor.set(centre.getX() + dx, y, centre.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir() || survives(state, level, cursor)) {
                        continue;
                    }
                    if (level.getBlockState(cursor.above()).is(CMBlocks.FALLOUT_ASH.get())) {
                        level.setBlock(cursor.above(), Blocks.AIR.defaultBlockState(), QUIET);
                    }
                    if (m < radius) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), QUIET);
                    } else if (state.is(BlockTags.LOGS)) {
                        level.setBlock(cursor, charred(state), QUIET);
                    } else if (state.is(BlockTags.LEAVES) || state.ignitedByLava() || !state.getFluidState().isEmpty() || !state.isCollisionShapeFullBlock(level, cursor)) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    } else if (dy < 2) {
                        level.setBlock(cursor, slag(random), QUIET);
                    }
                }
            }
        }
        front = to;
        if (front >= outer) {
            carved = true;
            pool(level);
        }
    }

    private static BlockState charred(BlockState log) {
        BlockState out = CMBlocks.CHARRED_LOG.get().defaultBlockState();
        return log.hasProperty(RotatedPillarBlock.AXIS) ? out.setValue(RotatedPillarBlock.AXIS, log.getValue(RotatedPillarBlock.AXIS)) : out;
    }

    private static BlockState slag(RandomSource random) {
        int roll = random.nextInt(100);
        return (roll < 50 ? Blocks.SMOOTH_BASALT : roll < 80 ? Blocks.BLACKSTONE : roll < 92 ? Blocks.MAGMA_BLOCK : Blocks.TUFF).defaultBlockState();
    }

    private void pool(ServerLevel level) {
        int bottom = centre.getY() - (int) Math.floor((radius - 0.5) / DEPTH_SQUASH);
        if (bottom <= level.getMinBuildHeight() + 1) {
            return;
        }
        double heat = (900 - CMThermal.AMBIENT) * CMThermal.FUEL_HEAT_CAPACITY;
        for (Direction d : new Direction[] {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            BlockPos at = d == Direction.UP ? centre : centre.relative(d);
            if (survives(level.getBlockState(at), level, at)) {
                continue;
            }
            while (at.getY() > bottom && !survives(level.getBlockState(at.below()), level, at.below())) {
                at = at.below();
            }
            level.setBlock(at, CMBlocks.CORIUM.get().defaultBlockState().setValue(Corium.CoriumBlock.HOT, true), Block.UPDATE_ALL);
            if (level.getBlockEntity(at) instanceof Corium.Entity_ lump) {
                lump.fill(heat, 0, 1, CMConfig.coriumDepth());
            }
        }
    }

    private boolean scorch(ServerLevel level) {
        int reach = (int) Math.ceil(radius * REACH);
        int side = 2 * reach + 1;
        int total = side * side;
        RandomSource random = level.random;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int n = 0; n < SCORCH_COLUMNS && ring < total; n++, ring++) {
            int dx = ring % side - reach;
            int dz = ring / side - reach;
            double flat = Math.sqrt(dx * dx + dz * dz) / radius;
            if (flat < 0.9 || flat >= REACH) {
                continue;
            }
            double force = Mth.clamp((REACH - flat) / (REACH - 1.0), 0.0, 1.0);
            int x = centre.getX() + dx;
            int z = centre.getZ() + dz;
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            for (int y = top; y > top - 24 && y > level.getMinBuildHeight(); y--) {
                cursor.set(x, y, z);
                BlockState state = level.getBlockState(cursor);
                if (state.isAir()) {
                    continue;
                }
                if (!state.getFluidState().isEmpty()) {
                    break;
                }
                if (state.is(BlockTags.LOGS)) {
                    if (random.nextDouble() < 0.15 + force) {
                        level.setBlock(cursor, charred(state), Block.UPDATE_CLIENTS);
                    }
                    continue;
                }
                if (state.is(CMBlocks.CHARRED_LOG.get())) {
                    continue;
                }
                boolean frail = state.is(BlockTags.LEAVES) || state.canBeReplaced() || !state.isCollisionShapeFullBlock(level, cursor);
                if (frail) {
                    if (random.nextDouble() < Math.sqrt(force)) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                    continue;
                }
                if (state.ignitedByLava()) {
                    if (random.nextDouble() < force * 0.7) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                    continue;
                }
                if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM) || state.is(Blocks.SNOW_BLOCK)) {
                    if (random.nextDouble() < force) {
                        level.setBlock(cursor, (random.nextInt(3) == 0 ? Blocks.COARSE_DIRT : Blocks.DIRT).defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
                BlockPos above = cursor.above();
                if (random.nextDouble() < force * 0.1 && level.getBlockState(above).isAir()) {
                    level.setBlock(above, BaseFireBlock.getState(level, above), Block.UPDATE_ALL);
                }
                break;
            }
        }
        return ring >= total;
    }
}
