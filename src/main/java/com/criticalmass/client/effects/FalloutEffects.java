package com.criticalmass.client.effects;

import com.criticalmass.registry.CMSounds;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;

public final class FalloutEffects {
    private static final ResourceLocation VIGNETTE = ResourceLocation.withDefaultNamespace("textures/misc/vignette.png");
    private static final RandomSource RANDOM = RandomSource.create();
    private static final int STALE_TICKS = 60;
    private static final float FOG_FAR = 30f;
    private static final float FOG_NEAR = 1.5f;
    private static final int FLAKES = 14;
    private static final int WISPS = 6;
    private static final double SPREAD = 13.0;
    private static float target;
    private static float value;
    private static float open;
    private static float windX;
    private static float windZ;
    private static boolean sheltered;
    private static Vec3 heart = Vec3.ZERO;
    private static float span;
    private static float glow;
    private static int age = STALE_TICKS;
    private static Ambience ambience;

    private FalloutEffects() {
    }

    public static void set(float intensity, float wx, float wz, boolean covered, double x, double y, double z, float radius, float strength) {
        target = Mth.clamp((float) Math.sqrt(Math.max(0f, intensity)) * 1.15f, 0f, 1f);
        heart = new Vec3(x, y, z);
        span = radius;
        glow = strength;
        windX = wx;
        windZ = wz;
        sheltered = covered;
        age = 0;
    }

    static void clear() {
        target = 0;
        value = 0;
        open = 0;
        glow = 0;
        span = 0;
        ambience = null;
    }

    static void tick(ClientLevel level) {
        Minecraft mc = Minecraft.getInstance();
        if (age++ > STALE_TICKS) {
            target = 0;
            glow = 0;
        }
        mist(level, mc);
        value += (target - value) * 0.04f;
        open += ((sheltered ? 0.35f : 1f) - open) * 0.06f;
        if (value < 0.004f) {
            value = target > 0 ? value : 0;
            return;
        }
        if (mc.player == null) {
            return;
        }
        if (ambience == null || ambience.isStopped()) {
            ambience = new Ambience();
            mc.getSoundManager().play(ambience);
        }
        Vec3 eye = mc.player.getEyePosition();
        int count = Math.round(value * FLAKES);
        for (int i = 0; i < count; i++) {
            double x = eye.x + (RANDOM.nextDouble() - 0.5) * 2 * SPREAD;
            double z = eye.z + (RANDOM.nextDouble() - 0.5) * 2 * SPREAD;
            double y = eye.y + 1.5 + RANDOM.nextDouble() * 8;
            if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z)) > y) {
                continue;
            }
            mc.particleEngine.add(new Flake(level, x, y, z, windX * 0.05 + (RANDOM.nextDouble() - 0.5) * 0.02, -0.035 - RANDOM.nextDouble() * 0.03, windZ * 0.05 + (RANDOM.nextDouble() - 0.5) * 0.02, EffectParticles.motes));
        }
    }

    private static void mist(ClientLevel level, Minecraft mc) {
        if (glow < 0.03f || span <= 0 || EffectParticles.puffs == null || mc.player == null || mc.isPaused()) {
            return;
        }
        for (int i = 0; i < WISPS; i++) {
            double angle = RANDOM.nextDouble() * Mth.TWO_PI;
            double out = Math.sqrt(RANDOM.nextDouble()) * span * 1.5;
            double x = heart.x + Math.cos(angle) * out + windX * span * 0.4;
            double z = heart.z + Math.sin(angle) * out + windZ * span * 0.4;
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
            if (surface < heart.y - span) {
                continue;
            }
            mc.particleEngine.add(new Wisp(level, x, surface + 0.6 + RANDOM.nextDouble() * 3, z, windX * 0.02, 0.008 + RANDOM.nextDouble() * 0.02, windZ * 0.02, EffectParticles.puffs, (float) Math.sqrt(glow)));
        }
    }

    private static float haze() {
        return value * open;
    }

    static void fogColor(ViewportEvent.ComputeFogColor event) {
        float k = haze() * 0.85f;
        if (k <= 0.004f) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float day = mc.level == null ? 1f : Mth.clamp(mc.level.getSkyDarken(1f), 0.45f, 1f);
        event.setRed(Mth.lerp(k, event.getRed(), 0.36f * day));
        event.setGreen(Mth.lerp(k, event.getGreen(), 0.56f * day));
        event.setBlue(Mth.lerp(k, event.getBlue(), 0.18f * day));
    }

    static void fog(ViewportEvent.RenderFog event) {
        float k = haze();
        if (k <= 0.004f) {
            return;
        }
        event.setFarPlaneDistance(Mth.lerp(k, event.getFarPlaneDistance(), FOG_FAR));
        event.setNearPlaneDistance(Mth.lerp(k, event.getNearPlaneDistance(), FOG_NEAR));
        event.setCanceled(true);
    }

    static void overlay(GuiGraphics graphics, DeltaTracker delta) {
        if (value <= 0.004f) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float time = (mc.level == null ? 0 : mc.level.getGameTime()) + delta.getGameTimeDeltaPartialTick(false);
        float pulse = value * (0.78f + 0.22f * Mth.sin(time * 0.13f));
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        graphics.fill(0, 0, width, height, Math.round(value * open * 30) << 24 | 0x78D24A);
        if (value > 0.45f && RANDOM.nextInt(5) == 0) {
            int y = RANDOM.nextInt(height);
            graphics.fill(0, y, width, y + 1 + RANDOM.nextInt(2), Math.round(value * 70) << 24 | 0x9CFF6A);
        }
        graphics.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        graphics.setColor(pulse * 0.95f, pulse * 0.2f, pulse * 0.85f, 1f);
        graphics.blit(VIGNETTE, 0, 0, -90, 0f, 0f, width, height, width, height);
        graphics.flush();
        graphics.setColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static final class Ambience extends AbstractTickableSoundInstance {
        Ambience() {
            super(CMSounds.FALLOUT.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            looping = true;
            delay = 0;
            relative = true;
            attenuation = Attenuation.NONE;
            volume = 0.01f;
            pitch = 1f;
        }

        @Override
        public void tick() {
            volume += (Math.min(1f, value * 1.3f) * (0.5f + 0.5f * open) - volume) * 0.08f;
            if (value <= 0.004f && volume < 0.02f) {
                stop();
            }
        }
    }

    private static final class Wisp extends TextureSheetParticle {
        private final float peak;

        Wisp(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, float strength) {
            super(level, x, y, z);
            this.xd = xd;
            this.yd = yd;
            this.zd = zd;
            lifetime = 170 + random.nextInt(90);
            hasPhysics = false;
            quadSize = 3.2f + random.nextFloat() * 4.4f;
            peak = (0.11f + random.nextFloat() * 0.09f) * strength;
            roll = random.nextFloat() * Mth.TWO_PI;
            oRoll = roll;
            setColor(0.3f + random.nextFloat() * 0.2f, 1f, 0.25f + random.nextFloat() * 0.15f);
            alpha = 0;
            pickSprite(sprites);
        }

        @Override
        public void tick() {
            xo = x;
            yo = y;
            zo = z;
            oRoll = roll;
            if (age++ >= lifetime) {
                remove();
                return;
            }
            move(xd, yd, zd);
            roll += 0.002f;
            quadSize *= 1.002f;
            alpha = peak * Mth.sin(age / (float) lifetime * Mth.PI);
        }

        @Override
        protected int getLightColor(float partialTick) {
            return LightTexture.FULL_BRIGHT;
        }

        @Override
        public net.minecraft.world.phys.AABB getRenderBoundingBox(float partialTicks) {
            return getBoundingBox().inflate(quadSize * 1.5);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return EffectParticles.ADDITIVE;
        }
    }

    static final class Flake extends TextureSheetParticle {
        private final boolean rising;
        private final float peak;

        Flake(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z);
            this.xd = xd;
            this.yd = yd;
            this.zd = zd;
            rising = yd > 0;
            lifetime = rising ? 50 + random.nextInt(40) : 130 + random.nextInt(90);
            hasPhysics = !rising;
            quadSize = 0.045f + random.nextFloat() * 0.06f;
            peak = 0.6f + random.nextFloat() * 0.4f;
            float tone = random.nextFloat();
            setColor(0.45f + 0.3f * tone, 1f, 0.25f + 0.25f * tone);
            alpha = 0;
            pickSprite(sprites);
        }

        @Override
        public void tick() {
            xo = x;
            yo = y;
            zo = z;
            if (age++ >= lifetime) {
                remove();
                return;
            }
            move(xd, yd, zd);
            if (onGround) {
                xd = 0;
                zd = 0;
                age += 3;
            } else {
                xd += (random.nextFloat() - 0.5f) * 0.004;
                zd += (random.nextFloat() - 0.5f) * 0.004;
            }
            float t = age / (float) lifetime;
            alpha = peak * Math.min(1f, t * 10f) * Math.min(1f, (1f - t) * 4f);
        }

        @Override
        protected int getLightColor(float partialTick) {
            return LightTexture.FULL_BRIGHT;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return EffectParticles.ADDITIVE;
        }
    }
}
