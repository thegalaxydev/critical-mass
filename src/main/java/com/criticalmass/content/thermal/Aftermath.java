package com.criticalmass.content.thermal;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.radiation.ContaminationData;
import com.criticalmass.content.radiation.Fallout;
import com.criticalmass.content.radiation.Radiation;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMConfig;
import com.criticalmass.registry.CMTriggers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

public final class Aftermath {
    public static final int CONTAMINATION_SPOTS = 96;
    public static final double SHELL_SHARE = 0.75;
    public static final double WRECK_CHANCE = 0.12;
    public static final int WRECK_LIMIT = 240;
    public static final double CLEAR_RATE = 1.0;
    public static final int CHECK_TICKS = 100;
    public static final int NEAR = 24;
    private static final String NAME = "criticalmass_recovery";

    private Aftermath() {
    }

    public static int debrisFor(int radius) {
        return Mth.clamp(radius / 2, 6, 32);
    }

    public static boolean leavesWreckage(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty() || state.is(Tags.Blocks.ORES)) {
            return false;
        }
        String owner = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace();
        if (!owner.equals(CriticalMass.MODID) && !owner.equals("create")) {
            return false;
        }
        return !state.is(CMBlocks.CORE_DEBRIS.get()) && !state.is(CMBlocks.WRECKAGE.get()) && !state.is(CMBlocks.FALLOUT_ASH.get()) && !state.is(CMBlocks.CORIUM.get())
                && !state.is(CMBlocks.CHARRED_LOG.get());
    }

    private static BlockPos thrown(ServerLevel level, BlockPos centre, int radius, RandomSource random) {
        double y = random.nextDouble() * 2 - 1;
        double turn = random.nextDouble() * Mth.TWO_PI;
        double flat = Math.sqrt(1 - y * y);
        double x = Math.cos(turn) * flat;
        double z = Math.sin(turn) * flat;
        BlockPos last = centre;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (double t = 1; t < radius * Detonation.DEPTH_SQUASH; t += 0.5) {
            int dx = Mth.floor(x * t + 0.5);
            int dy = Mth.floor(y * t + 0.5);
            int dz = Mth.floor(z * t + 0.5);
            double measured = Math.sqrt(dx * dx + dz * dz + (dy < 0 ? dy * Detonation.DEPTH_SQUASH : dy) * (dy < 0 ? dy * Detonation.DEPTH_SQUASH : dy));
            cursor.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
            if (measured >= radius || !level.isLoaded(cursor) || cursor.getY() <= level.getMinBuildHeight() || cursor.getY() >= level.getMaxBuildHeight()) {
                break;
            }
            BlockState state = level.getBlockState(cursor);
            if (Detonation.survives(state, level, cursor) || !state.canBeReplaced()) {
                break;
            }
            last = cursor.immutable();
        }
        return last;
    }

    private static BlockPos settled(ServerLevel level, BlockPos from) {
        BlockPos.MutableBlockPos cursor = from.mutable();
        while (cursor.getY() > level.getMinBuildHeight() + 1 && level.getBlockState(cursor.below()).canBeReplaced()) {
            cursor.move(0, -1, 0);
        }
        return cursor.immutable();
    }

    public static void settle(ServerLevel level, BlockPos centre, int radius, List<BlockPos> wrecks) {
        RandomSource random = level.random;
        List<BlockPos> points = new ArrayList<>();
        ContaminationData soil = ContaminationData.get(level);
        double each = CMThermal.CORIUM_SPILL * radius * CMConfig.get(CMConfig.CORIUM_CONTAMINATION) * SHELL_SHARE / CONTAMINATION_SPOTS;
        for (int i = 0; i < CONTAMINATION_SPOTS; i++) {
            BlockPos at = thrown(level, centre, radius, random);
            if (each > 0) {
                soil.add(at, each);
            }
            if (i % 3 == 0) {
                points.add(at);
            }
        }
        for (int i = 0; i < debrisFor(radius); i++) {
            BlockPos at = settled(level, thrown(level, centre, radius, random));
            if (level.getBlockState(at).canBeReplaced() && level.getBlockState(at).getFluidState().isEmpty()) {
                level.setBlock(at, CMBlocks.CORE_DEBRIS.get().defaultBlockState(), Block.UPDATE_ALL);
                points.add(at);
            }
        }
        for (BlockPos wreck : wrecks) {
            if (!level.getBlockState(wreck).canBeReplaced()) {
                continue;
            }
            BlockPos at = settled(level, wreck);
            if (level.getBlockState(at).getFluidState().isEmpty()) {
                level.setBlock(at, CMBlocks.WRECKAGE.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        points.add(settled(level, centre).above());
        Sites.get(level).add(new Site(centre, radius, points, false));
    }

    public static double rateAt(ServerLevel level, BlockPos point, List<Radiation.Source> blocks) {
        Vec3 at = Vec3.atCenterOf(point);
        return Radiation.fromSources(level, at, Radiation.sourcesNear(level, at, blocks), null) + Radiation.fieldDose(level, at) + Fallout.rate(level, at);
    }

    @Nullable
    public static BlockPos worst(ServerLevel level, Site site, double[] reading) {
        List<Radiation.Source> blocks = Radiation.blockSources(level);
        BlockPos where = null;
        reading[0] = 0;
        for (BlockPos point : site.points()) {
            double rate = rateAt(level, point, blocks);
            if (rate > reading[0]) {
                reading[0] = rate;
                where = point;
            }
        }
        return where;
    }

    public static boolean clear(ServerLevel level, Site site) {
        List<Radiation.Source> blocks = Radiation.blockSources(level);
        for (BlockPos point : site.points()) {
            if (rateAt(level, point, blocks) >= CLEAR_RATE) {
                return false;
            }
        }
        return true;
    }

    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % CHECK_TICKS != 0) {
            return;
        }
        Sites sites = Sites.get(level);
        for (int i = 0; i < sites.list.size(); i++) {
            Site site = sites.list.get(i);
            if (site.cleared() || !level.isLoaded(site.centre()) || Detonation.armed(level, site.centre(), site.radius())) {
                continue;
            }
            double reach = site.radius() + NEAR;
            List<ServerPlayer> near = level.getPlayers(p -> p.blockPosition().distSqr(site.centre()) <= reach * reach);
            if (near.isEmpty() || !clear(level, site)) {
                continue;
            }
            sites.list.set(i, new Site(site.centre(), site.radius(), site.points(), true));
            sites.setDirty();
            for (ServerPlayer player : near) {
                CMTriggers.grant(player, "all_clear");
                player.displayClientMessage(Component.translatable("criticalmass.recovery.all_clear"), false);
            }
        }
    }

    public record Site(BlockPos centre, int radius, List<BlockPos> points, boolean cleared) {
    }

    public static final class Sites extends SavedData {
        private final List<Site> list = new ArrayList<>();

        public static Sites get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Sites::new, Sites::load, null), NAME);
        }

        public List<Site> all() {
            return List.copyOf(list);
        }

        public void add(Site site) {
            list.removeIf(old -> old.centre().equals(site.centre()));
            list.add(site);
            setDirty();
        }

        public static Sites load(CompoundTag tag, HolderLookup.Provider registries) {
            Sites sites = new Sites();
            for (Tag entry : tag.getList("Sites", Tag.TAG_COMPOUND)) {
                CompoundTag one = (CompoundTag) entry;
                List<BlockPos> points = new ArrayList<>();
                for (long packed : one.getLongArray("Points")) {
                    points.add(BlockPos.of(packed));
                }
                sites.list.add(new Site(BlockPos.of(one.getLong("Centre")), one.getInt("Radius"), points, one.getBoolean("Cleared")));
            }
            return sites;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag out = new ListTag();
            for (Site site : list) {
                CompoundTag one = new CompoundTag();
                one.putLong("Centre", site.centre().asLong());
                one.putInt("Radius", site.radius());
                one.putLongArray("Points", site.points().stream().mapToLong(BlockPos::asLong).toArray());
                one.putBoolean("Cleared", site.cleared());
                out.add(one);
            }
            tag.put("Sites", out);
            return tag;
        }
    }
}
