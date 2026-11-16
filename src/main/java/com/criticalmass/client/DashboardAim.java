package com.criticalmass.client;

import com.criticalmass.content.control.DashboardBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class DashboardAim {
    private DashboardAim() {
    }

    public static int slot(DashboardBlockEntity be) {
        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK && block.getBlockPos().equals(be.getBlockPos())) {
            return be.slotAt(block.getLocation());
        }
        return -1;
    }
}
