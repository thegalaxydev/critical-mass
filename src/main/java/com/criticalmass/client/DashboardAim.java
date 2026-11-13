package com.criticalmass.client;

import com.criticalmass.content.control.DashboardBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class DashboardAim {
    private DashboardAim() {
    }

    public static int slot(BlockPos pos, BlockState state) {
        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK && block.getBlockPos().equals(pos)) {
            return DashboardBlock.slotAt(state, pos, block.getLocation());
        }
        return -1;
    }
}
