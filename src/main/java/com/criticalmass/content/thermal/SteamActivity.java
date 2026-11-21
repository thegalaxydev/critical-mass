package com.criticalmass.content.thermal;

import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class SteamActivity {
    public static final int EVERY = 20;
    public static final int HALF_LIFE_TICKS = 1200;
    public static final double KEPT = Math.pow(0.5, (double) EVERY / HALF_LIFE_TICKS);
    public static final double DOSE = 0.003;

    private SteamActivity() {
    }

    public static boolean carries(CoolantBuffer buffer) {
        return buffer.steamMedium() && buffer.activityLoad() > 0;
    }

    public static void decay(List<CoolantBuffer> buffers) {
        for (CoolantBuffer buffer : buffers) {
            if (carries(buffer)) {
                buffer.decayActivity(KEPT);
            }
        }
    }

    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % EVERY != 0) {
            return;
        }
        for (BlockEntity be : LoadedThermalBlocks.snapshot()) {
            if (be.getLevel() != level) {
                continue;
            }
            if (be instanceof ThermalNode node) {
                decay(node.buffers());
            }
            if (be instanceof CirculationPumpBlockEntity pump) {
                for (ThermalNode held : pump.pipeContents()) {
                    decay(held.buffers());
                }
            }
        }
    }
}
