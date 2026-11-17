package com.criticalmass.client.effects;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ArcParticle extends Particle {
    private static final int FLICKER_TICKS = 2;
    private final Vec3 from;
    private final Vec3 to;
    private final float power;
    private final TextureAtlasSprite sprite;
    private final long seed;
    private final AABB bounds;
    private List<Vec3> path = List.of();
    private final List<List<Vec3>> forks = new ArrayList<>();
    private int shaped = -1;

    public ArcParticle(ClientLevel level, Vec3 from, Vec3 to, float power, TextureAtlasSprite sprite) {
        super(level, from.x, from.y, from.z);
        this.from = from;
        this.to = to;
        this.power = power;
        this.sprite = sprite;
        this.seed = random.nextLong();
        this.bounds = new AABB(from, to).inflate(2.0 + power);
        lifetime = Math.round(5 + power * 5);
        hasPhysics = false;
    }

    private static List<Vec3> jagged(Vec3 a, Vec3 b, Random random, double sway) {
        List<Vec3> points = new ArrayList<>();
        points.add(a);
        points.add(b);
        for (int pass = 0; pass < 4; pass++) {
            List<Vec3> next = new ArrayList<>();
            for (int i = 0; i < points.size() - 1; i++) {
                Vec3 p = points.get(i);
                Vec3 q = points.get(i + 1);
                next.add(p);
                next.add(p.add(q).scale(0.5).add((random.nextDouble() - 0.5) * sway, (random.nextDouble() - 0.5) * sway, (random.nextDouble() - 0.5) * sway));
            }
            next.add(points.get(points.size() - 1));
            points = next;
            sway *= 0.55;
        }
        return points;
    }

    private void shape() {
        Random random = new Random(seed + age / FLICKER_TICKS * 7919L);
        double length = from.distanceTo(to);
        double sway = Math.max(0.12, Math.min(1.2, length * 0.22));
        path = jagged(from, to, random, sway);
        forks.clear();
        int count = Math.round(power * 2);
        for (int i = 0; i < count; i++) {
            Vec3 root = path.get(1 + random.nextInt(Math.max(1, path.size() - 2)));
            double reach = 0.3 + random.nextDouble() * (0.5 + power * 0.5);
            Vec3 tip = root.add((random.nextDouble() - 0.5) * 2 * reach, (random.nextDouble() - 0.3) * reach, (random.nextDouble() - 0.5) * 2 * reach);
            forks.add(jagged(root, tip, random, reach * 0.4));
        }
    }

    @Override
    public void tick() {
        if (age++ >= lifetime) {
            remove();
        }
    }

    private void strip(VertexConsumer buffer, Vec3 eye, List<Vec3> points, float width, float r, float g, float b, float a) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v = (sprite.getV0() + sprite.getV1()) / 2;
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 p = points.get(i);
            Vec3 q = points.get(i + 1);
            Vec3 along = q.subtract(p);
            Vec3 side = along.cross(p.add(q).scale(0.5).subtract(eye));
            double size = side.length();
            if (size < 1e-6) {
                continue;
            }
            side = side.scale(width / size);
            vertex(buffer, eye, p.subtract(side), u0, v, r, g, b, a);
            vertex(buffer, eye, p.add(side), u1, v, r, g, b, a);
            vertex(buffer, eye, q.add(side), u1, v, r, g, b, a);
            vertex(buffer, eye, q.subtract(side), u0, v, r, g, b, a);
            vertex(buffer, eye, q.subtract(side), u0, v, r, g, b, a);
            vertex(buffer, eye, q.add(side), u1, v, r, g, b, a);
            vertex(buffer, eye, p.add(side), u1, v, r, g, b, a);
            vertex(buffer, eye, p.subtract(side), u0, v, r, g, b, a);
        }
    }

    private static void vertex(VertexConsumer buffer, Vec3 eye, Vec3 at, float u, float v, float r, float g, float b, float a) {
        buffer.addVertex((float) (at.x - eye.x), (float) (at.y - eye.y), (float) (at.z - eye.z)).setUv(u, v).setColor(r, g, b, a).setLight(LightTexture.FULL_BRIGHT);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        if (shaped != age / FLICKER_TICKS) {
            shaped = age / FLICKER_TICKS;
            shape();
        }
        Vec3 eye = camera.getPosition();
        float fade = Math.max(0f, 1f - (age + partialTicks) / (lifetime + 1f));
        float flicker = 0.7f + 0.3f * (float) Math.sin((age + partialTicks) * 2.9);
        float a = Math.min(1f, fade * 1.6f) * flicker;
        float girth = 0.05f + 0.05f * power;
        strip(buffer, eye, path, girth * 4.5f, 0.25f, 0.5f, 1f, a * 0.55f);
        strip(buffer, eye, path, girth * 1.6f, 0.75f, 0.9f, 1f, a);
        strip(buffer, eye, path, girth * 0.6f, 1f, 1f, 1f, a);
        for (List<Vec3> fork : forks) {
            strip(buffer, eye, fork, girth * 2.4f, 0.3f, 0.55f, 1f, a * 0.5f);
            strip(buffer, eye, fork, girth * 0.7f, 0.85f, 0.95f, 1f, a * 0.9f);
        }
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        return bounds;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return EffectParticles.ADDITIVE;
    }
}
