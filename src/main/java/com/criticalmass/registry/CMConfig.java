package com.criticalmass.registry;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CMConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue FUEL_LIFETIME_TICKS;
    public static final ModConfigSpec.DoubleValue FUEL_WEAR;
    public static final int DEFAULT_FUEL_LIFETIME = 120000;
    public static final double DEFAULT_FUEL_WEAR = 0.25;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("fuel");
        FUEL_LIFETIME_TICKS = b.comment("Ticks of full-power operation one fuel assembly lasts before it is spent")
                .defineInRange("lifetimeTicks", DEFAULT_FUEL_LIFETIME, 1200, 100_000_000);
        FUEL_WEAR = b.comment("Condition lost over a full burn at normal temperature")
                .defineInRange("normalWear", DEFAULT_FUEL_WEAR, 0.0, 0.95);
        b.pop();
        SPEC = b.build();
    }

    private CMConfig() {
    }

    public static int fuelLifetime() {
        return SPEC.isLoaded() ? FUEL_LIFETIME_TICKS.get() : DEFAULT_FUEL_LIFETIME;
    }

    public static double fuelWear() {
        return SPEC.isLoaded() ? FUEL_WEAR.get() : DEFAULT_FUEL_WEAR;
    }
}
