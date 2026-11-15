package com.criticalmass.client;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

public final class TurbinePlacementOutline {
    private static final int RED = 0xFF_ff5d6c;

    private TurbinePlacementOutline() {
    }

    public static void show(BlockPos centre) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        Outliner.getInstance().showAABB(Pair.of("steam_turbine", centre), new AABB(centre).inflate(1)).colored(RED);
        mc.player.displayClientMessage(Component.translatable("criticalmass.turbine.not_enough_space").withStyle(style -> style.withColor(RED & 0xFFFFFF)), true);
    }
}
