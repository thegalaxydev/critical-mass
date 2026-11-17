package com.criticalmass.client.effects;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.effects.Effects;
import com.criticalmass.registry.CMParticles;
import com.criticalmass.registry.CMSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientEffects {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Map<String, Loop> LOOPS = new HashMap<>();
    private static final List<Emitter> EMITTERS = new ArrayList<>();
    private static final float GRAIN_FROM = 10f;
    private static final float GRAIN_FULL = 160f;
    private static SpriteSet streak;
    private static float shake;
    private static float doseRate;
    private static boolean doseMeter;
    private static int doseAge = 1000;

    private ClientEffects() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(EffectParticles::register);
        modBus.addListener((RegisterParticleProvidersEvent event) -> event.registerSpriteSet(CMParticles.ARC.get(), sprites -> {
            streak = sprites;
            return (type, level, x, y, z, xd, yd, zd) -> new ArcParticle(level, new Vec3(x, y, z), new Vec3(x + xd, y + yd, z + zd), 1f, sprites.get(RANDOM));
        }));
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerBelowAll(CriticalMass.asResource("radiation_grain"), ClientEffects::grain));
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(CriticalMass.asResource("blast_flash"), ClientEffects::flash));
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerBelowAll(CriticalMass.asResource("fallout_haze"), FalloutEffects::overlay));
        NeoForge.EVENT_BUS.addListener(FalloutEffects::fogColor);
        NeoForge.EVENT_BUS.addListener(FalloutEffects::fog);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        NeoForge.EVENT_BUS.addListener(ClientEffects::camera);
        Effects.client = ClientEffects::loop;
    }

    private record Emitter(int kind, Vec3 at, Vec3 aim, float power, int[] left) {
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        private int keep = 6;
        private float wanted;
        private float tone;

        Loop(SoundEvent sound, BlockPos pos, float volume, float pitch) {
            super(sound, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            x = pos.getX() + 0.5;
            y = pos.getY() + 0.5;
            z = pos.getZ() + 0.5;
            looping = true;
            delay = 0;
            this.volume = 0.01f;
            this.pitch = pitch;
            wanted = volume;
            tone = pitch;
        }

        void keep(float volume, float pitch) {
            keep = 6;
            wanted = volume;
            tone = pitch;
        }

        @Override
        public void tick() {
            float aim = keep-- > 0 ? wanted : 0;
            volume += (aim - volume) * 0.2f;
            pitch += (tone - pitch) * 0.2f;
            if (keep < -30) {
                stop();
            }
        }
    }

    private static void loop(BlockPos pos, SoundEvent sound, float volume, float pitch) {
        String key = pos.asLong() + ":" + sound.getLocation();
        Loop playing = LOOPS.get(key);
        if (playing == null || playing.isStopped()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 48 * 48) {
                return;
            }
            playing = new Loop(sound, pos, volume, pitch);
            LOOPS.put(key, playing);
            mc.getSoundManager().play(playing);
        } else {
            playing.keep(volume, pitch);
        }
    }

    private static void add(ClientLevel level, net.minecraft.core.particles.ParticleOptions type, Vec3 at, double xd, double yd, double zd) {
        level.addParticle(type, true, at.x, at.y, at.z, xd, yd, zd);
    }

    private static Vec3 scatter(double reach) {
        return new Vec3((RANDOM.nextDouble() - 0.5) * 2 * reach, (RANDOM.nextDouble() - 0.5) * 2 * reach, (RANDOM.nextDouble() - 0.5) * 2 * reach);
    }

    private static void arc(ClientLevel level, Vec3 from, Vec3 to, float power) {
        if (streak != null) {
            Minecraft.getInstance().particleEngine.add(new ArcParticle(level, from, to, power, streak.get(RANDOM)));
        }
    }

    private static void sparks(ClientLevel level, Vec3 at, int count, double speed) {
        for (int i = 0; i < count; i++) {
            Vec3 v = scatter(speed);
            add(level, CMParticles.ARC_SPARK.get(), at, v.x, Math.abs(v.y) * 0.8 + speed * 0.3, v.z);
        }
    }

    public static void handle(int kind, Vec3 from, Vec3 to, float power) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }
        double distance = Math.sqrt(mc.player.distanceToSqr(from));
        switch (kind) {
            case Effects.ARC -> {
                arc(level, from, to, power);
                add(level, CMParticles.GLOW.get(), to, 0.6 + power * 0.6, 0, 0);
                sparks(level, to, Math.round(4 + power * 6), 0.12 + power * 0.05);
            }
            case Effects.POP -> {
                add(level, CMParticles.GLOW.get(), from, 0.5 + power * 0.5, 0, 0);
                sparks(level, from, Math.round(5 + power * 8), 0.13);
                arc(level, from, from.add(scatter(0.5 + power * 0.4)), power * 0.6f);
            }
            case Effects.BLAST -> {
                add(level, CMParticles.GLOW.get(), from, 2.2 + power * 1.2, 0, 0);
                add(level, CMParticles.FIREBALL.get(), from, 1.6 + power * 1.3, 0, 0);
                add(level, CMParticles.SHOCKWAVE.get(), from.add(0, -0.3, 0), 2.5 + power * 2.2, 0, 0);
                sparks(level, from, Math.round(24 + power * 16), 0.3 + power * 0.08);
                for (int i = 0; i < 10 + power * 4; i++) {
                    Vec3 v = scatter(0.12);
                    add(level, CMParticles.EMBER.get(), from.add(scatter(0.6)), v.x, Math.abs(v.y) + 0.05, v.z);
                }
                for (int i = 0; i < 6 + power * 3; i++) {
                    Vec3 v = scatter(0.05);
                    add(level, CMParticles.OIL_SMOKE.get(), from.add(scatter(0.7)), v.x, 0.05 + RANDOM.nextDouble() * 0.06, v.z);
                }
                for (int i = 0; i < 3 + Math.round(power); i++) {
                    arc(level, from, from.add(scatter(1.2 + power * 0.9)), power * 0.7f);
                }
                EMITTERS.add(new Emitter(Effects.BLAST, from, from, power, new int[]{8}));
                shake = Math.max(shake, (float) (power * 1.6 * Math.max(0, 1 - distance / 48.0)));
            }
            case Effects.STEAM_JET -> EMITTERS.add(new Emitter(kind, from, to.subtract(from), power, new int[]{24}));
            case Effects.STEAM_BURST -> {
                for (int i = 0; i < 30 + power * 20; i++) {
                    Vec3 v = scatter(0.12 + power * 0.06);
                    add(level, CMParticles.STEAM.get(), from.add(scatter(0.3)), v.x, v.y * 0.6 + 0.03, v.z);
                }
                shake = Math.max(shake, (float) (power * 0.6 * Math.max(0, 1 - distance / 24.0)));
            }
            case Effects.OIL_FIRE -> EMITTERS.add(new Emitter(kind, from, from, 1f, new int[]{Math.round(power * 20)}));
            case Effects.NUKE -> MushroomCloud.start(level, from, power);
            default -> {
            }
        }
    }

    public static void dose(float rate, boolean meter) {
        doseRate = rate;
        doseMeter = meter;
        doseAge = 0;
    }

    private static void emit(ClientLevel level) {
        Iterator<Emitter> all = EMITTERS.iterator();
        while (all.hasNext()) {
            Emitter e = all.next();
            if (e.left[0]-- <= 0) {
                all.remove();
                continue;
            }
            switch (e.kind) {
                case Effects.STEAM_JET -> {
                    for (int i = 0; i < 3; i++) {
                        Vec3 v = e.aim.normalize().scale(0.25 + e.power * 0.2).add(scatter(0.04));
                        add(level, CMParticles.STEAM.get(), e.at, v.x, v.y, v.z);
                    }
                }
                case Effects.OIL_FIRE -> {
                    Vec3 v = scatter(0.03);
                    add(level, CMParticles.OIL_SMOKE.get(), e.at.add(scatter(0.4)), v.x, 0.07 + RANDOM.nextDouble() * 0.05, v.z);
                    if (RANDOM.nextInt(3) == 0) {
                        add(level, CMParticles.EMBER.get(), e.at.add(scatter(0.5)), v.x * 2, 0.06 + RANDOM.nextDouble() * 0.06, v.z * 2);
                    }
                }
                case Effects.BLAST -> {
                    for (int i = 0; i < 2; i++) {
                        Vec3 v = scatter(0.04);
                        add(level, CMParticles.OIL_SMOKE.get(), e.at.add(scatter(0.6)), v.x, 0.05 + RANDOM.nextDouble() * 0.05, v.z);
                    }
                }
                default -> {
                }
            }
        }
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            LOOPS.clear();
            EMITTERS.clear();
            MushroomCloud.clear();
            FalloutEffects.clear();
            shake = 0;
            doseRate = 0;
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        shake *= 0.86f;
        if (shake < 0.01f) {
            shake = 0;
        }
        emit(mc.level);
        MushroomCloud.tick(mc.level);
        FalloutEffects.tick(mc.level);
        shake = Math.max(shake, MushroomCloud.rumble());
        LOOPS.values().removeIf(Loop::isStopped);
        if (doseAge++ > 60) {
            doseRate = 0;
        }
        if (doseMeter && doseRate > 0.05f) {
            float perTick = Math.min(1.6f, 0.02f + doseRate * 0.012f);
            int clicks = (int) perTick + (RANDOM.nextFloat() < perTick - (int) perTick ? 1 : 0);
            for (int i = 0; i < clicks; i++) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(CMSounds.GEIGER.get(), 0.9f + RANDOM.nextFloat() * 0.25f, 0.35f));
            }
        }
    }

    private static void camera(ViewportEvent.ComputeCameraAngles event) {
        if (shake <= 0) {
            return;
        }
        double time = event.getPartialTick() + (Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime());
        event.setPitch(event.getPitch() + (float) (Math.sin(time * 2.7) * shake));
        event.setYaw(event.getYaw() + (float) (Math.sin(time * 3.9 + 1.3) * shake * 0.7));
        event.setRoll(event.getRoll() + (float) (Math.sin(time * 3.1 + 2.1) * shake * 0.8));
    }

    private static void flash(GuiGraphics graphics, DeltaTracker delta) {
        float strength = MushroomCloud.flash(delta.getGameTimeDeltaPartialTick(false));
        if (strength > 0.004f) {
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), Math.round(Mth.clamp(strength, 0f, 1f) * 255) << 24 | 0xFFF6E0);
        }
    }

    private static void grain(GuiGraphics graphics, DeltaTracker delta) {
        if (doseRate <= GRAIN_FROM) {
            return;
        }
        float strength = Mth.clamp((doseRate - GRAIN_FROM) / (GRAIN_FULL - GRAIN_FROM), 0f, 1f);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int specks = Math.round(40 + strength * 420);
        int alpha = Math.round(30 + strength * 90);
        for (int i = 0; i < specks; i++) {
            int x = RANDOM.nextInt(width);
            int y = RANDOM.nextInt(height);
            int size = 1 + RANDOM.nextInt(2);
            int tone = RANDOM.nextBoolean() ? 0xFFFFFF : 0xA8FFC0;
            graphics.fill(x, y, x + size * (1 + RANDOM.nextInt(3)), y + size, alpha << 24 | tone);
        }
        if (strength > 0.4f && RANDOM.nextInt(6) == 0) {
            int y = RANDOM.nextInt(height);
            graphics.fill(0, y, width, y + 1, Math.round(strength * 60) << 24 | 0xFFFFFF);
        }
    }
}
