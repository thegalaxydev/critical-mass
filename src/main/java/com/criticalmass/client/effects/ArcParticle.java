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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ArcParticle extends Particle {
    private static final int WRITHE_TICKS = 3;
    private static final int STEPS = 5;
    private static final int PASSES = 3;
    private static final int FORK_PASSES = 2;
    private static final int FLARE_SIDES = 14;
    private final Vec3 from;
    private final Vec3 to;
    private final float power;
    private final float u;
    private final float v;
    private final long seed;
    private final AABB bounds;
    private final int[] roots;
    private final Vec3[] tips;
    private List<List<Vec3>> now = List.of();
    private List<List<Vec3>> next = List.of();
    private int shaped = -1;

    public ArcParticle(ClientLevel level, Vec3 from, Vec3 to, float power, TextureAtlasSprite sprite) {
        super(level, from.x, from.y, from.z);
        this.from = from;
        this.to = to;
        this.power = power;
        this.u = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * 0.53f;
        this.v = (sprite.getV0() + sprite.getV1()) / 2;
        this.seed = random.nextLong();
        this.bounds = new AABB(from, to).inflate(2.0 + power);
        Random fixed = new Random(seed);
        int count = Math.round(power * 2);
        roots = new int[count];
        tips = new Vec3[count];
        for (int i = 0; i < count; i++) {
            roots[i] = 1 + fixed.nextInt((1 << PASSES) - 1);
            double reach = 0.3 + fixed.nextDouble() * (0.5 + power * 0.5);
            tips[i] = new Vec3((fixed.nextDouble() - 0.5) * 2 * reach, (fixed.nextDouble() - 0.3) * reach, (fixed.nextDouble() - 0.5) * 2 * reach);
        }
        lifetime = Math.round(5 + power * 5);
        hasPhysics = false;
    }

    private static List<Vec3> knots(Vec3 a, Vec3 b, Random random, double sway, int passes) {
        Vec3 axis = b.subtract(a);
        double length = axis.length();
        Vec3 along = length < 1e-6 ? new Vec3(0, 1, 0) : axis.scale(1 / length);
        List<Vec3> points = new ArrayList<>();
        points.add(a);
        points.add(b);
        for (int pass = 0; pass < passes; pass++) {
            List<Vec3> finer = new ArrayList<>();
            for (int i = 0; i < points.size() - 1; i++) {
                Vec3 p = points.get(i);
                Vec3 q = points.get(i + 1);
                Vec3 push = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5);
                push = push.subtract(along.scale(push.dot(along))).scale(sway);
                finer.add(p);
                finer.add(p.add(q).scale(0.5).add(push));
            }
            finer.add(points.get(points.size() - 1));
            points = finer;
            sway *= 0.5;
        }
        return points;
    }

    private List<List<Vec3>> shape(int index) {
        Random random = new Random(seed + index * 7919L);
        double length = from.distanceTo(to);
        List<Vec3> main = knots(from, to, random, Math.max(0.1, Math.min(1.0, length * 0.2)), PASSES);
        double bow = Math.min(0.5, length * 0.12);
        for (int i = 1; i < main.size() - 1; i++) {
            main.set(i, main.get(i).add(0, bow * Math.sin(Math.PI * i / (main.size() - 1)), 0));
        }
        List<List<Vec3>> out = new ArrayList<>();
        out.add(main);
        for (int i = 0; i < roots.length; i++) {
            Vec3 root = main.get(roots[i]);
            double reach = tips[i].length();
            Vec3 tip = root.add(tips[i]).add((random.nextDouble() - 0.5) * reach * 0.5, (random.nextDouble() - 0.5) * reach * 0.5, (random.nextDouble() - 0.5) * reach * 0.5);
            out.add(knots(root, tip, random, reach * 0.5, FORK_PASSES));
        }
        return out;
    }

    private static Vec3 spline(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return p0.scale(-0.5 * t3 + t2 - 0.5 * t).add(p1.scale(1.5 * t3 - 2.5 * t2 + 1)).add(p2.scale(-1.5 * t3 + 2 * t2 + 0.5 * t)).add(p3.scale(0.5 * t3 - 0.5 * t2));
    }

    private static List<Vec3> curve(List<Vec3> a, List<Vec3> b, double mix) {
        int n = a.size();
        Vec3[] knots = new Vec3[n];
        for (int i = 0; i < n; i++) {
            knots[i] = a.get(i).lerp(b.get(i), mix);
        }
        List<Vec3> out = new ArrayList<>();
        for (int i = 0; i < n - 1; i++) {
            Vec3 p0 = knots[Math.max(0, i - 1)];
            Vec3 p3 = knots[Math.min(n - 1, i + 2)];
            for (int step = 0; step < STEPS; step++) {
                out.add(spline(p0, knots[i], knots[i + 1], p3, step / (double) STEPS));
            }
        }
        out.add(knots[n - 1]);
        return out;
    }

    @Override
    public void tick() {
        if (age++ >= lifetime) {
            remove();
        }
    }

    private static float near(Vec3 eye, Vec3 at) {
        return (float) Mth.clamp((at.distanceTo(eye) - 0.25) / 0.9, 0.0, 1.0);
    }

    private void vertex(VertexConsumer buffer, Vec3 eye, Vec3 at, float r, float g, float b, float a) {
        buffer.addVertex((float) (at.x - eye.x), (float) (at.y - eye.y), (float) (at.z - eye.z)).setUv(u, v).setColor(r, g, b, a).setLight(LightTexture.FULL_BRIGHT);
    }

    private void quad(VertexConsumer buffer, Vec3 eye, Vec3 p0, float a0, Vec3 p1, float a1, Vec3 p2, float a2, Vec3 p3, float a3, float r, float g, float b) {
        if (p1.subtract(p0).cross(p2.subtract(p0)).dot(eye.subtract(p0)) > 0) {
            vertex(buffer, eye, p0, r, g, b, a0);
            vertex(buffer, eye, p1, r, g, b, a1);
            vertex(buffer, eye, p2, r, g, b, a2);
            vertex(buffer, eye, p3, r, g, b, a3);
        } else {
            vertex(buffer, eye, p3, r, g, b, a3);
            vertex(buffer, eye, p2, r, g, b, a2);
            vertex(buffer, eye, p1, r, g, b, a1);
            vertex(buffer, eye, p0, r, g, b, a0);
        }
    }

    private void ribbon(VertexConsumer buffer, Vec3 eye, List<Vec3> points, float width, boolean pointed, float r, float g, float b, float a) {
        int n = points.size();
        Vec3 unit = Vec3.ZERO;
        Vec3 lastLeft = null;
        Vec3 lastRight = null;
        Vec3 lastCentre = null;
        float lastAlpha = 0;
        for (int i = 0; i < n; i++) {
            Vec3 p = points.get(i);
            Vec3 along = points.get(Math.min(n - 1, i + 1)).subtract(points.get(Math.max(0, i - 1)));
            Vec3 view = p.subtract(eye);
            Vec3 side = along.cross(view);
            double size = side.length();
            float facing = (float) Mth.clamp((size / Math.max(1e-6, along.length() * view.length()) - 0.08) / 0.35, 0.0, 1.0);
            if (size > 1e-6) {
                Vec3 turned = side.scale(1 / size);
                unit = turned.dot(unit) < 0 ? turned.reverse() : turned;
            }
            double s = i / (double) (n - 1);
            double taper = pointed ? Math.sqrt(1 - s) : 0.35 + 0.65 * Math.pow(Math.sin(Math.PI * s), 0.6);
            float close = near(eye, p);
            Vec3 half = unit.scale(width * taper * (0.4 + 0.6 * close));
            Vec3 left = p.subtract(half);
            Vec3 right = p.add(half);
            float alpha = a * close * facing * (pointed ? (float) Math.sqrt(1 - s) : 1f);
            if (lastCentre != null) {
                quad(buffer, eye, lastLeft, 0, lastCentre, lastAlpha, p, alpha, left, 0, r, g, b);
                quad(buffer, eye, lastCentre, lastAlpha, lastRight, 0, right, 0, p, alpha, r, g, b);
            }
            lastLeft = left;
            lastRight = right;
            lastCentre = p;
            lastAlpha = alpha;
        }
    }

    private void flare(VertexConsumer buffer, Camera camera, Vec3 eye, Vec3 at, float radius, float r, float g, float b, float a) {
        Vec3 left = new Vec3(camera.getLeftVector());
        Vec3 up = new Vec3(camera.getUpVector());
        float alpha = a * near(eye, at);
        Vec3 last = at.add(left.scale(radius));
        for (int i = 1; i <= FLARE_SIDES; i++) {
            double angle = Math.PI * 2 * i / FLARE_SIDES;
            Vec3 rim = at.add(left.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
            quad(buffer, eye, at, alpha, last, 0, rim, 0, at, alpha, r, g, b);
            last = rim;
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        int index = age / WRITHE_TICKS;
        if (shaped != index) {
            now = shaped == index - 1 && !next.isEmpty() ? next : shape(index);
            next = shape(index + 1);
            shaped = index;
        }
        double mix = Mth.clamp((age % WRITHE_TICKS + partialTicks) / WRITHE_TICKS, 0.0, 1.0);
        mix = mix * mix * (3 - 2 * mix);
        Vec3 eye = camera.getPosition();
        float fade = Math.max(0f, 1f - (age + partialTicks) / (lifetime + 1f));
        float flicker = 0.78f + 0.22f * (float) Math.sin((age + partialTicks) * 2.9);
        float a = Math.min(1f, fade * 1.6f) * flicker;
        float girth = 0.05f + 0.05f * power;
        List<Vec3> main = curve(now.get(0), next.get(0), mix);
        ribbon(buffer, eye, main, girth * 3.2f, false, 0.22f, 0.48f, 1f, a * 0.5f);
        ribbon(buffer, eye, main, girth * 1.3f, false, 0.6f, 0.82f, 1f, a * 0.85f);
        ribbon(buffer, eye, main, girth * 0.45f, false, 1f, 1f, 1f, a);
        for (int i = 1; i < now.size(); i++) {
            List<Vec3> fork = curve(now.get(i), next.get(i), mix);
            ribbon(buffer, eye, fork, girth * 1.1f, true, 0.25f, 0.52f, 1f, a * 0.45f);
            ribbon(buffer, eye, fork, girth * 0.32f, true, 0.8f, 0.92f, 1f, a * 0.9f);
        }
        float burst = girth * (2.6f + 1.2f * flicker);
        for (Vec3 end : List.of(from, to)) {
            flare(buffer, camera, eye, end, burst * 1.9f, 0.25f, 0.5f, 1f, a * 0.5f);
            flare(buffer, camera, eye, end, burst * 0.7f, 0.85f, 0.95f, 1f, a * 0.9f);
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
