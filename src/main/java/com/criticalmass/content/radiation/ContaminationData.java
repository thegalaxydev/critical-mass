package com.criticalmass.content.radiation;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

public final class ContaminationData extends SavedData {
    private static final String NAME = "criticalmass_contamination";
    private final Long2DoubleOpenHashMap cells = new Long2DoubleOpenHashMap();

    public static ContaminationData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(ContaminationData::new, ContaminationData::load, null), NAME);
    }

    public double get(BlockPos pos) {
        return cells.get(pos.asLong());
    }

    public void add(BlockPos pos, double amount) {
        if (amount <= 0) {
            return;
        }
        cells.addTo(pos.asLong(), amount);
        setDirty();
    }

    public double clear(BlockPos pos) {
        double v = cells.remove(pos.asLong());
        if (v > 0) {
            setDirty();
        }
        return v;
    }

    public double wash(BlockPos pos, double keep, double floor) {
        double held = cells.get(pos.asLong());
        if (held <= 0) {
            return 0;
        }
        double left = held * keep;
        if (left < floor) {
            cells.remove(pos.asLong());
            left = 0;
        } else {
            cells.put(pos.asLong(), left);
        }
        setDirty();
        return held - left;
    }

    public double total() {
        double t = 0;
        for (double v : cells.values()) {
            t += v;
        }
        return t;
    }

    public int size() {
        return cells.size();
    }

    public double near(Vec3 p, int radius) {
        if (cells.isEmpty()) {
            return 0;
        }
        BlockPos c = BlockPos.containing(p);
        double sum = 0;
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-radius, -radius, -radius), c.offset(radius, radius, radius))) {
            double v = cells.get(q.asLong());
            if (v > 0) {
                sum += v / (1 + q.getCenter().distanceToSqr(p));
            }
        }
        return sum;
    }

    public static ContaminationData load(CompoundTag tag, HolderLookup.Provider registries) {
        ContaminationData d = new ContaminationData();
        for (Tag t : tag.getList("Cells", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            d.cells.put(c.getLong("P"), c.getDouble("L"));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Long2DoubleMap.Entry e : cells.long2DoubleEntrySet()) {
            CompoundTag c = new CompoundTag();
            c.putLong("P", e.getLongKey());
            c.putDouble("L", e.getDoubleValue());
            list.add(c);
        }
        tag.put("Cells", list);
        return tag;
    }
}
