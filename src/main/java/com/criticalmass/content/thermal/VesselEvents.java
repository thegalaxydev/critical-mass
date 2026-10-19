package com.criticalmass.content.thermal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class VesselEvents {
    private VesselEvents() {
    }

    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
            return;
        }
        BlockPos pos = event.getPos();
        for (BlockEntity be : LoadedThermalBlocks.snapshot()) {
            if (be instanceof ReactorControllerBlockEntity c && be.getLevel() == level && c.watches(pos)) {
                c.markStructureDirty();
            }
        }
    }
}
