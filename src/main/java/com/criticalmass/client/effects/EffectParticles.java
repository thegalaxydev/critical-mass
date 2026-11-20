package com.criticalmass.client.effects;

import com.criticalmass.registry.CMParticles;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.joml.Quaternionf;

public final class EffectParticles {
    public static final ParticleRenderType ADDITIVE = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textures) {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "criticalmass:additive";
        }
    };
    public static final ParticleRenderType BEHIND_GLASS = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textures) {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public boolean isTranslucent() {
            return false;
        }

        @Override
        public String toString() {
            return "criticalmass:behind_glass";
        }
    };
    public static final ParticleRenderType SOFT = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textures) {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "criticalmass:soft";
        }
    };

    static SpriteSet puffs;
    static SpriteSet glows;
    static SpriteSet rings;
    static SpriteSet motes;

    private EffectParticles() {
    }

    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(CMParticles.STEAM.get(), sprites -> {
            puffs = sprites;
            return (type, level, x, y, z, xd, yd, zd) -> new Cloud(level, x, y, z, xd, yd, zd, sprites, 0.5f, 34, 1.022f, 0.004f, 0.62f, 1f, 1f, 1f);
        });
        event.registerSpriteSet(CMParticles.PLUME.get(), sprites -> (type, level, x, y, z, xd, yd, zd) -> new Cloud(level, x, y, z, xd, yd, zd, sprites, 2.4f, 190, 1.011f, 0.0015f, 0.5f, 1f, 1f, 1f));
        event.registerSpriteSet(CMParticles.OIL_SMOKE.get(), sprites -> (type, level, x, y, z, xd, yd, zd) -> new Cloud(level, x, y, z, xd, yd, zd, sprites, 1.1f, 110, 1.014f, 0.003f, 0.85f, 0.13f, 0.12f, 0.12f));
        event.registerSpriteSet(CMParticles.FOAM.get(), sprites -> (type, level, x, y, z, xd, yd, zd) -> new Cloud(level, x, y, z, xd, yd, zd, sprites, 0.26f, 30, 0.975f, -0.0012f, 0.9f, 0.86f, 1f, 0.9f));
        event.registerSpriteSet(CMParticles.ARC_SPARK.get(), sprites -> (type, level, x, y, z, xd, yd, zd) -> new Spark(level, x, y, z, xd, yd, zd, sprites, 0.75f, 0.9f, 1f, 0.05f));
        event.registerSpriteSet(CMParticles.EMBER.get(), sprites -> (type, level, x, y, z, xd, yd, zd) -> new Spark(level, x, y, z, xd, yd, zd, sprites, 1f, 0.55f, 0.15f, -0.012f));
        event.registerSpriteSet(CMParticles.GLOW.get(), sprites -> {
            glows = sprites;
            return (type, level, x, y, z, xd, yd, zd) -> new Glow(level, x, y, z, sprites, (float) Math.max(0.5, xd), 5, 0.8f, 0.9f, 1f, false);
        });
        event.registerSpriteSet(CMParticles.FIREBALL.get(), sprites -> (type, level, x, y, z, xd, yd, zd) -> new Glow(level, x, y, z, sprites, (float) Math.max(0.5, xd), 16, 1f, 0.62f, 0.2f, true));
        event.registerSpriteSet(CMParticles.SHOCKWAVE.get(), sprites -> {
            rings = sprites;
            return (type, level, x, y, z, xd, yd, zd) -> new Ring(level, x, y, z, sprites, (float) Math.max(1.0, xd));
        });
        event.registerSpriteSet(CMParticles.CHERENKOV.get(), sprites -> (type, level, x, y, z, xd, yd, zd) -> new Halo(level, x, y, z, sprites, (float) Math.max(0.2, xd)));
        event.registerSpriteSet(CMParticles.MOTE.get(), sprites -> {
            motes = sprites;
            return (type, level, x, y, z, xd, yd, zd) -> new Mote(level, x, y, z, xd, yd, zd, sprites);
        });
        event.registerSpriteSet(CMParticles.FALLOUT.get(), sprites -> (type, level, x, y, z, xd, yd, zd) -> new FalloutEffects.Flake(level, x, y, z, xd, yd, zd, sprites));
    }

    static class Cloud extends TextureSheetParticle {
        private final float growth;
        private final float lift;
        private final float peak;

        Cloud(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, float size, int life, float growth, float lift, float peak, float r, float g, float b) {
            super(level, x, y, z);
            this.xd = xd;
            this.yd = yd;
            this.zd = zd;
            this.growth = growth;
            this.lift = lift;
            this.peak = peak;
            quadSize = size * (0.75f + random.nextFloat() * 0.5f);
            lifetime = (int) (life * (0.8f + random.nextFloat() * 0.4f));
            roll = random.nextFloat() * Mth.TWO_PI;
            oRoll = roll;
            hasPhysics = false;
            float shade = 0.86f + random.nextFloat() * 0.14f;
            setColor(r * shade, g * shade, b * shade);
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
            xd *= 0.97;
            zd *= 0.97;
            yd = yd * 0.97 + lift;
            quadSize *= growth;
            roll += 0.004f;
            float t = age / (float) lifetime;
            alpha = peak * Math.min(1f, t * 8f) * (1f - t) * (1f - t * 0.4f);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return SOFT;
        }
    }

    static class Spark extends TextureSheetParticle {
        private final float weight;

        Spark(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, float r, float g, float b, float weight) {
            super(level, x, y, z);
            this.xd = xd;
            this.yd = yd;
            this.zd = zd;
            this.weight = weight;
            quadSize = 0.045f + random.nextFloat() * 0.05f;
            lifetime = 12 + random.nextInt(22);
            hasPhysics = weight > 0;
            setColor(r, g, b);
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
            yd -= weight;
            move(xd, yd, zd);
            xd *= 0.96;
            yd *= 0.96;
            zd *= 0.96;
            if (onGround) {
                yd = Math.abs(yd) * 0.35 + 0.02;
                xd *= 0.6;
                zd *= 0.6;
            }
            float t = age / (float) lifetime;
            alpha = 1f - t * t;
            if (weight < 0) {
                gCol = Math.max(0.1f, 0.55f - t * 0.45f);
            }
        }

        @Override
        protected int getLightColor(float partialTick) {
            return LightTexture.FULL_BRIGHT;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ADDITIVE;
        }
    }

    static class Glow extends TextureSheetParticle {
        private final float size;
        private final boolean swelling;

        Glow(ClientLevel level, double x, double y, double z, SpriteSet sprites, float size, int life, float r, float g, float b, boolean swelling) {
            super(level, x, y, z);
            this.size = size;
            this.swelling = swelling;
            lifetime = life;
            hasPhysics = false;
            quadSize = size;
            setColor(r, g, b);
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
            float t = age / (float) lifetime;
            quadSize = swelling ? size * (0.4f + 0.9f * (float) Math.sqrt(t)) : size * (1f - t * 0.3f);
            alpha = swelling ? (1f - t) * (1f - t) : 0.7f * (1f - t) * (1f - t);
            if (swelling) {
                gCol = Math.max(0.12f, 0.62f - t * 0.5f);
                bCol = Math.max(0.02f, 0.2f - t * 0.2f);
                y += 0.03;
            }
        }

        @Override
        protected int getLightColor(float partialTick) {
            return LightTexture.FULL_BRIGHT;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ADDITIVE;
        }
    }

    static class Ring extends TextureSheetParticle {
        private static final Quaternionf FLAT = new Quaternionf().rotateX(-Mth.HALF_PI);
        private static final Quaternionf UNDER = new Quaternionf().rotateX(Mth.HALF_PI);
        private final float reach;

        Ring(ClientLevel level, double x, double y, double z, SpriteSet sprites, float reach) {
            super(level, x, y, z);
            this.reach = reach;
            lifetime = 12;
            hasPhysics = false;
            quadSize = 0.3f;
            setColor(1f, 0.92f, 0.75f);
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
            float t = age / (float) lifetime;
            quadSize = 0.3f + reach * (float) Math.sqrt(t);
            alpha = (1f - t) * 0.7f;
        }

        @Override
        public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
            renderRotatedQuad(buffer, camera, FLAT, partialTicks);
            renderRotatedQuad(buffer, camera, UNDER, partialTicks);
        }

        @Override
        protected int getLightColor(float partialTick) {
            return LightTexture.FULL_BRIGHT;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ADDITIVE;
        }
    }

    static class Halo extends TextureSheetParticle {
        private final float size;
        private final float beat;

        Halo(ClientLevel level, double x, double y, double z, SpriteSet sprites, float size) {
            super(level, x, y, z);
            this.size = size;
            beat = random.nextFloat() * Mth.TWO_PI;
            lifetime = 50 + random.nextInt(30);
            hasPhysics = false;
            quadSize = size;
            setColor(0.25f, 0.62f, 1f);
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
            float t = age / (float) lifetime;
            float swell = 0.5f + 0.5f * Mth.sin(beat + age * 0.21f);
            quadSize = size * (0.9f + 0.18f * swell);
            alpha = 0.34f * Mth.sin(t * Mth.PI) * (0.75f + 0.25f * swell);
        }

        @Override
        protected int getLightColor(float partialTick) {
            return LightTexture.FULL_BRIGHT;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return BEHIND_GLASS;
        }
    }

    static class Jet extends TextureSheetParticle {
        static final double GRAVITY = 0.035;

        Jet(ClientLevel level, net.minecraft.world.phys.Vec3 from, net.minecraft.world.phys.Vec3 speed, int life, float size, SpriteSet sprites) {
            super(level, from.x, from.y, from.z);
            xd = speed.x;
            yd = speed.y;
            zd = speed.z;
            lifetime = life;
            hasPhysics = false;
            quadSize = size;
            roll = random.nextFloat() * Mth.TWO_PI;
            oRoll = roll;
            float shade = 0.88f + random.nextFloat() * 0.12f;
            setColor(0.7f * shade, 0.87f * shade, shade);
            alpha = 0.95f;
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
            yd -= GRAVITY;
            move(xd, yd, zd);
            quadSize *= 1.02f;
            alpha = 0.95f * Math.min(1f, (lifetime - age + 2) / 4f);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return SOFT;
        }
    }

    static class Mote extends TextureSheetParticle {
        Mote(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
            super(level, x, y, z);
            this.xd = xd;
            this.yd = yd;
            this.zd = zd;
            lifetime = 30 + random.nextInt(40);
            hasPhysics = false;
            quadSize = 0.03f + random.nextFloat() * 0.035f;
            setColor(0.55f, 0.82f, 1f);
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
            xd += (random.nextFloat() - 0.5f) * 0.002;
            zd += (random.nextFloat() - 0.5f) * 0.002;
            alpha = 0.9f * Mth.sin(age / (float) lifetime * Mth.PI);
        }

        @Override
        protected int getLightColor(float partialTick) {
            return LightTexture.FULL_BRIGHT;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return BEHIND_GLASS;
        }
    }
}
