package com.criticalmass.client;

import com.criticalmass.content.power.WireLinks;
import com.criticalmass.content.power.WireNode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class WireDraw {
    private static final int SEGMENTS = 16;
    private static final float HALF = 0.035f;
    private static final float SAG = 0.05f;
    private static final float MAX_SAG = 1.5f;
    private static final float[] TONE = {0.11f, 0.13f, 0.16f};
    private static final Vec3 CENTRE = new Vec3(0.5, 0.5, 0.5);

    private WireDraw() {
    }

    public static AABB bounds(BlockEntity be) {
        AABB box = new AABB(be.getBlockPos());
        if (be instanceof WireNode node) {
            for (WireLinks.Link link : node.wires().all()) {
                box = box.minmax(new AABB(link.other()));
            }
        }
        return box.inflate(0.5, MAX_SAG, 0.5);
    }

    public static void draw(BlockEntity be, PoseStack ms, MultiBufferSource buffers) {
        Level level = be.getLevel();
        if (level == null || !(be instanceof WireNode node)) {
            return;
        }
        BlockPos pos = be.getBlockPos();
        for (WireLinks.Link link : node.wires().all()) {
            BlockPos other = link.other();
            WireNode far = level.getBlockEntity(other) instanceof WireNode n ? n : null;
            if (far != null && pos.compareTo(other) > 0) {
                continue;
            }
            Vec3 from = node.terminal(link.mine());
            Vec3 to = new Vec3(other.getX() - pos.getX(), other.getY() - pos.getY(), other.getZ() - pos.getZ()).add(far == null ? CENTRE : far.terminal(link.theirs()));
            int near = LevelRenderer.getLightColor(level, pos);
            int away = LevelRenderer.getLightColor(level, other);
            strip(ms, buffers, from, to, near, away, true);
            strip(ms, buffers, from, to, near, away, false);
        }
    }

    private static void strip(PoseStack ms, MultiBufferSource buffers, Vec3 from, Vec3 to, int near, int away, boolean upright) {
        VertexConsumer vc = buffers.getBuffer(RenderType.leash());
        Matrix4f pose = ms.last().pose();
        Vec3 span = to.subtract(from);
        float sag = Math.min(MAX_SAG, (float) span.length() * SAG);
        double flat = Math.sqrt(span.x * span.x + span.z * span.z);
        float sideX = flat < 1e-4 ? HALF : (float) (-span.z / flat) * HALF;
        float sideZ = flat < 1e-4 ? 0 : (float) (span.x / flat) * HALF;
        for (int i = 0; i <= SEGMENTS; i++) {
            float t = i / (float) SEGMENTS;
            float x = (float) (from.x + span.x * t);
            float y = (float) (from.y + span.y * t) - sag * 4 * t * (1 - t);
            float z = (float) (from.z + span.z * t);
            int light = LightTexture.pack((int) Mth.lerp(t, LightTexture.block(near), LightTexture.block(away)), (int) Mth.lerp(t, LightTexture.sky(near), LightTexture.sky(away)));
            float shade = i % 2 == 0 ? 1f : 0.8f;
            float ox = upright ? 0 : sideX;
            float oy = upright ? HALF : 0;
            float oz = upright ? 0 : sideZ;
            vc.addVertex(pose, x - ox, y - oy, z - oz).setColor(TONE[0] * shade, TONE[1] * shade, TONE[2] * shade, 1f).setLight(light);
            vc.addVertex(pose, x + ox, y + oy, z + oz).setColor(TONE[0] * shade, TONE[1] * shade, TONE[2] * shade, 1f).setLight(light);
        }
    }
}
