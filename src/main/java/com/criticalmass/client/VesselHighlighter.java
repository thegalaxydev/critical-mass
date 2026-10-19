package com.criticalmass.client;

import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import java.util.List;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class VesselHighlighter {
    private static final double RANGE_SQR = 48 * 48;

    private VesselHighlighter() {
    }

    public static void show(ReactorControllerBlockEntity controller) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !GogglesItem.isWearingGoggles(player)) {
            return;
        }
        if (player.distanceToSqr(controller.getBlockPos().getCenter()) > RANGE_SQR) {
            return;
        }
        List<BlockPos> problems = controller.problemPositions();
        for (int i = 0; i < problems.size(); i++) {
            BlockPos p = problems.get(i);
            Outliner.getInstance().showAABB(new Key(controller.getBlockPos(), p), new AABB(p).inflate(0.02))
                    .colored(0xFF4A3A)
                    .lineWidth(1 / 16f);
        }
    }

    private record Key(BlockPos controller, BlockPos pos) {
    }
}
