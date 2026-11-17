package com.criticalmass.client.effects;

import com.criticalmass.registry.CMParticles;
import com.criticalmass.registry.CMSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MushroomCloud {
    private static final int LIFE = 1500;
    private static final int FADE = 360;
    private static final int CAP_PUFFS = 440;
    private static final int DOME_PUFFS = 220;
    private static final int STEM_PUFFS = 260;
    private static final int CORE_FLAMES = 90;
    private static final int SURGE_TICKS = 90;
    private static final double WAVE_SPEED = 4.0;
    private static final RandomSource RANDOM = RandomSource.create();
    private static final List<MushroomCloud> ACTIVE = new ArrayList<>();
    private final Vec3 ground;
    private final float radius;
    private int age;
    private boolean heard;

    private MushroomCloud(Vec3 ground, float radius) {
        this.ground = ground;
        this.radius = radius;
    }

    private float rise(float t) {
        return radius * (0.4f + 3.4f * (1f - (float) Math.exp(-t / 170.0)));
    }

    private float ring(float t) {
        return radius * (0.42f + 0.9f * (1f - (float) Math.exp(-t / 190.0)));
    }

    private float tube(float t) {
        return ring(t) * 0.52f;
    }

    private float stem(float t) {
        return radius * (0.16f + 0.05f * (1f - (float) Math.exp(-t / 200.0)));
    }

    private float heat(float t) {
        return (float) Math.exp(-t / 110.0);
    }

    private float fade(float t) {
        return t > LIFE - FADE ? Math.max(0f, (LIFE - t) / FADE) : 1f;
    }

    private boolean done() {
        return age >= LIFE;
    }

    static void clear() {
        ACTIVE.clear();
    }

    static void start(ClientLevel level, Vec3 ground, float radius) {
        SpriteSet puffs = EffectParticles.puffs;
        if (puffs == null) {
            return;
        }
        MushroomCloud cloud = new MushroomCloud(ground, radius);
        ACTIVE.add(cloud);
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < CAP_PUFFS; i++) {
            mc.particleEngine.add(new Cap(level, cloud, puffs));
        }
        for (int i = 0; i < DOME_PUFFS; i++) {
            mc.particleEngine.add(new Dome(level, cloud, puffs));
        }
        for (int i = 0; i < STEM_PUFFS; i++) {
            mc.particleEngine.add(new Stem(level, cloud, puffs));
        }
        for (int i = 0; i < CORE_FLAMES; i++) {
            mc.particleEngine.add(new Core(level, cloud, puffs));
        }
        if (EffectParticles.rings != null) {
            for (int i = 0; i < 3; i++) {
                int life = 50 + i * 30;
                mc.particleEngine.add(new EffectParticles.Ring(level, ground.x, ground.y + 0.5 + i * 0.4, ground.z, EffectParticles.rings, radius * (2.2f + i * 0.9f)) {
                    {
                        lifetime = life;
                    }
                });
            }
        }
        for (int i = 0; i < 160; i++) {
            double angle = RANDOM.nextDouble() * Mth.TWO_PI;
            double out = 0.3 + RANDOM.nextDouble() * 1.4;
            level.addParticle(CMParticles.EMBER.get(), true, ground.x + Math.cos(angle) * radius * 0.3, ground.y + 1, ground.z + Math.sin(angle) * radius * 0.3,
                    Math.cos(angle) * out, 0.6 + RANDOM.nextDouble() * 1.6, Math.sin(angle) * out);
        }
    }

    static void tick(ClientLevel level) {
        Minecraft mc = Minecraft.getInstance();
        for (MushroomCloud cloud : new ArrayList<>(ACTIVE)) {
            cloud.age++;
            if (cloud.done()) {
                ACTIVE.remove(cloud);
                continue;
            }
            if (cloud.age >= 4 && cloud.age < SURGE_TICKS && EffectParticles.puffs != null) {
                cloud.surge(level, mc);
            }
            if (!cloud.heard && mc.player != null && cloud.age * WAVE_SPEED >= mc.player.position().distanceTo(cloud.ground)) {
                cloud.heard = true;
                double distance = mc.player.position().distanceTo(cloud.ground);
                float volume = (float) Mth.clamp(1.6 - distance / (10.0 * cloud.radius), 0.25, 1.0);
                mc.getSoundManager().play(new SimpleSoundInstance(CMSounds.NUCLEAR_BLAST.get().getLocation(), SoundSource.BLOCKS, volume, 1f, SoundInstance.createUnseededRandom(), false, 0,
                        SoundInstance.Attenuation.NONE, 0, 0, 0, true));
            }
        }
    }

    private void surge(ClientLevel level, Minecraft mc) {
        float t = age;
        double reach = radius * (0.6 + 1.9 * (1 - Math.exp(-t / 45.0)));
        double speed = 0.5 * Math.exp(-t / 40.0) + 0.05;
        for (int i = 0; i < 9; i++) {
            double angle = RANDOM.nextDouble() * Mth.TWO_PI;
            double x = ground.x + Math.cos(angle) * reach;
            double z = ground.z + Math.sin(angle) * reach;
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
            double y = surface < ground.y - radius ? ground.y : surface + 1.0 + RANDOM.nextDouble() * radius * 0.06;
            mc.particleEngine.add(new EffectParticles.Cloud(level, x, y, z, Math.cos(angle) * speed, 0.02 + RANDOM.nextDouble() * 0.03, Math.sin(angle) * speed, EffectParticles.puffs,
                    radius * 0.17f, 280, 1.004f, 0.0008f, 0.7f, 0.5f, 0.44f, 0.36f));
        }
    }

    static float flash(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0;
        }
        float strongest = 0;
        for (MushroomCloud cloud : ACTIVE) {
            float t = cloud.age + partialTick;
            if (t > 160) {
                continue;
            }
            double distance = mc.player.position().distanceTo(cloud.ground);
            float reach = (float) Mth.clamp(1.4 - distance / (12.0 * cloud.radius), 0.0, 1.0);
            float curve = t < 4 ? 1f : (float) Math.exp(-(t - 4) / 26.0);
            strongest = Math.max(strongest, reach * curve);
        }
        return strongest;
    }

    static float rumble() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0;
        }
        float strongest = 0;
        for (MushroomCloud cloud : ACTIVE) {
            double distance = mc.player.position().distanceTo(cloud.ground);
            float near = (float) Mth.clamp(1.0 - distance / (14.0 * cloud.radius), 0.0, 1.0);
            float since = cloud.age - (float) (distance / WAVE_SPEED);
            if (since < 0 || since > 260) {
                continue;
            }
            float hit = since < 3 ? 9f : 0f;
            strongest = Math.max(strongest, near * Math.max(hit, 2.2f * (float) Math.exp(-since / 70.0)));
        }
        return strongest;
    }

    private abstract static class Bound extends TextureSheetParticle {
        protected final MushroomCloud cloud;
        protected final float scale;
        protected final float tint;

        Bound(ClientLevel level, MushroomCloud cloud, SpriteSet sprites) {
            super(level, cloud.ground.x, cloud.ground.y, cloud.ground.z);
            this.cloud = cloud;
            scale = 0.8f + random.nextFloat() * 0.5f;
            tint = 0.85f + random.nextFloat() * 0.15f;
            lifetime = LIFE;
            hasPhysics = false;
            roll = random.nextFloat() * Mth.TWO_PI;
            oRoll = roll;
            alpha = 0;
            pickSprite(sprites);
        }

        protected abstract void place(float t);

        @Override
        public void tick() {
            xo = x;
            yo = y;
            zo = z;
            oRoll = roll;
            if (cloud.done() || !ACTIVE.contains(cloud)) {
                remove();
                return;
            }
            age = cloud.age;
            place(cloud.age);
            setPos(x, y, z);
        }

        protected void smoke(float lit, float glow, float r, float g, float b) {
            float day = Mth.clamp(level.getSkyDarken(1f), 0.3f, 1f);
            float shade = (0.32f + 0.68f * lit) * tint * day;
            float white = Math.max(0f, glow - 0.7f) * 1.6f;
            rCol = Math.min(1f, r * shade + glow * 1.2f);
            gCol = Math.min(1f, g * shade + glow * 0.5f + white * 0.8f);
            bCol = Math.min(1f, b * shade + glow * 0.1f + white * 0.7f);
        }

        @Override
        protected int getLightColor(float partialTick) {
            return LightTexture.FULL_BRIGHT;
        }

        @Override
        public AABB getRenderBoundingBox(float partialTicks) {
            return getBoundingBox().inflate(quadSize * 1.5);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return EffectParticles.SOFT;
        }
    }

    private static final class Cap extends Bound {
        private final float around;
        private final float depth;
        private float turn;

        Cap(ClientLevel level, MushroomCloud cloud, SpriteSet sprites) {
            super(level, cloud, sprites);
            around = random.nextFloat() * Mth.TWO_PI;
            turn = random.nextFloat() * Mth.TWO_PI;
            depth = (float) Math.sqrt(0.15 + 0.85 * random.nextFloat());
        }

        @Override
        protected void place(float t) {
            turn -= 0.004f + 0.022f * (float) Math.exp(-t / 380.0);
            roll += 0.003f;
            float tube = cloud.tube(t);
            float reach = cloud.ring(t) + depth * tube * Mth.cos(turn);
            x = cloud.ground.x + Mth.cos(around) * reach;
            z = cloud.ground.z + Mth.sin(around) * reach;
            y = cloud.ground.y + cloud.rise(t) + depth * tube * 0.78f * Mth.sin(turn);
            quadSize = tube * 0.42f * scale;
            float heat = cloud.heat(t);
            float lit = 0.5f + 0.5f * Mth.sin(turn);
            smoke(lit, heat * (1.15f - 0.6f * lit), 0.62f, 0.57f, 0.54f);
            alpha = 0.86f * Math.min(1f, t / 8f) * cloud.fade(t);
        }
    }

    private static final class Dome extends Bound {
        private static final float EDGE = 1.45f;
        private final float around;
        private float lean;

        Dome(ClientLevel level, MushroomCloud cloud, SpriteSet sprites) {
            super(level, cloud, sprites);
            around = random.nextFloat() * Mth.TWO_PI;
            lean = random.nextFloat() * EDGE;
        }

        @Override
        protected void place(float t) {
            lean += 0.003f + 0.012f * (float) Math.exp(-t / 380.0);
            if (lean > EDGE) {
                lean = 0.05f;
            }
            roll += 0.003f;
            float tube = cloud.tube(t);
            float reach = Mth.sin(lean) * (cloud.ring(t) + tube * 0.55f);
            x = cloud.ground.x + Mth.cos(around) * reach;
            z = cloud.ground.z + Mth.sin(around) * reach;
            y = cloud.ground.y + cloud.rise(t) + tube * (0.1f + 0.95f * Mth.cos(lean));
            quadSize = tube * 0.5f * scale;
            float heat = cloud.heat(t);
            smoke(0.55f + 0.45f * Mth.cos(lean), heat * 0.95f, 0.64f, 0.59f, 0.56f);
            alpha = 0.84f * Math.min(1f, t / 8f) * cloud.fade(t) * Math.min(1f, lean * 6f) * Math.min(1f, (EDGE - lean) * 6f);
        }
    }

    private static final class Stem extends Bound {
        private final float out;
        private float around;
        private float height;

        Stem(ClientLevel level, MushroomCloud cloud, SpriteSet sprites) {
            super(level, cloud, sprites);
            around = random.nextFloat() * Mth.TWO_PI;
            height = random.nextFloat();
            out = (float) Math.sqrt(random.nextFloat());
        }

        @Override
        protected void place(float t) {
            height += 0.0045f;
            if (height > 1f) {
                height -= 1f;
                around = random.nextFloat() * Mth.TWO_PI;
            }
            around += 0.004f;
            roll += 0.003f;
            float thick = cloud.stem(t);
            float low = 1f - height;
            float flare = 1f + 1.6f * low * low * low * low + 0.7f * (float) Math.pow(height, 6);
            float reach = thick * (0.35f + 0.75f * out) * flare;
            x = cloud.ground.x + Mth.cos(around) * reach;
            z = cloud.ground.z + Mth.sin(around) * reach;
            y = cloud.ground.y + height * (cloud.rise(t) - cloud.tube(t) * 0.3f);
            quadSize = thick * 0.62f * scale * (1f + 0.5f * low * low);
            float heat = cloud.heat(t);
            smoke(0.25f + 0.5f * out, heat * (0.35f + 0.6f * height), 0.44f, 0.38f, 0.34f);
            alpha = 0.8f * Math.min(1f, t / 12f) * cloud.fade(t) * Math.min(1f, Math.min(height, 1f - height) * 12f + 0.2f);
        }
    }

    private static final class Core extends Bound {
        private final float around;
        private final float lift;
        private final float out;
        private float spin;

        Core(ClientLevel level, MushroomCloud cloud, SpriteSet sprites) {
            super(level, cloud, sprites);
            around = random.nextFloat() * Mth.TWO_PI;
            lift = random.nextFloat() * 2f - 1f;
            out = (float) Math.sqrt(random.nextFloat());
            spin = (random.nextFloat() - 0.5f) * 0.02f;
        }

        @Override
        protected void place(float t) {
            float heat = cloud.heat(t);
            if (heat < 0.05f) {
                remove();
                return;
            }
            roll += spin;
            float tube = cloud.tube(t);
            float reach = cloud.ring(t) * 0.8f * out;
            x = cloud.ground.x + Mth.cos(around) * reach;
            z = cloud.ground.z + Mth.sin(around) * reach;
            y = cloud.ground.y + cloud.rise(t) + lift * tube * 0.55f;
            quadSize = tube * 0.5f * scale;
            alpha = 0.42f * Math.min(1f, heat * 1.6f) * Math.min(1f, t / 6f);
            rCol = 1f;
            gCol = 0.3f + 0.55f * heat;
            bCol = 0.04f + 0.5f * heat * heat;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return EffectParticles.ADDITIVE;
        }
    }
}
