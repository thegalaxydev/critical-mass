package com.criticalmass.content.thermal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class LoadedThermalBlocks {
    private static final Set<BlockEntity> LOADED = Collections.newSetFromMap(new WeakHashMap<>());

    private LoadedThermalBlocks() {
    }

    public static synchronized void add(BlockEntity be) {
        if (be.getLevel() != null && !be.getLevel().isClientSide) {
            LOADED.add(be);
        }
    }

    public static synchronized void remove(BlockEntity be) {
        LOADED.remove(be);
    }

    public static synchronized List<BlockEntity> snapshot() {
        List<BlockEntity> out = new ArrayList<>();
        for (BlockEntity be : LOADED) {
            if (!be.isRemoved() && be.getLevel() != null) {
                out.add(be);
            }
        }
        return out;
    }
}
