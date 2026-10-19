package com.criticalmass.content.thermal;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public final class BodyMotion {
    private BodyMotion() {
    }

    public static boolean onBody(BlockEntity be) {
        return SableCompanion.INSTANCE.getContaining(be) != null;
    }

    public static double tiltDegrees(BlockEntity be) {
        SubLevelAccess body = SableCompanion.INSTANCE.getContaining(be);
        if (body == null) {
            return 0;
        }
        Vector3d up = body.logicalPose().transformNormal(new Vector3d(0, 1, 0));
        double len = up.length();
        return len == 0 ? 0 : Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, up.y / len))));
    }

    public static double airSpeed(BlockEntity be) {
        if (be.getLevel() == null) {
            return 0;
        }
        Vec3 v = SableCompanion.INSTANCE.getVelocityRelativeToAir(be.getLevel(), be.getBlockPos().getCenter());
        return v == null ? 0 : v.length();
    }
}
