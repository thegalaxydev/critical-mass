package com.criticalmass.registry;

import com.criticalmass.CriticalMass;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CMParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, CriticalMass.MODID);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STEAM = simple("steam", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FALLOUT = simple("fallout", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PLUME = simple("plume", true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ARC_SPARK = simple("arc_spark", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EMBER = simple("ember", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> OIL_SMOKE = simple("oil_smoke", true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLOW = simple("glow", true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIREBALL = simple("fireball", true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHOCKWAVE = simple("shockwave", true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CHERENKOV = simple("cherenkov", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MOTE = simple("mote", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ARC = simple("arc", true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FOAM = simple("foam", false);

    private CMParticles() {
    }

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> simple(String name, boolean always) {
        return PARTICLES.register(name, () -> new SimpleParticleType(always));
    }
}
