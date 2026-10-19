package com.criticalmass.registry;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CMConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue FUEL_LIFETIME_TICKS;
    public static final ModConfigSpec.DoubleValue FUEL_WEAR;
    public static final ModConfigSpec.BooleanValue RADIATION_ENABLED;
    public static final ModConfigSpec.BooleanValue RADIATION_EFFECTS;
    public static final ModConfigSpec.DoubleValue DOSE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue RECOVERY_PER_MINUTE;
    public static final ModConfigSpec.DoubleValue SHIELD_CONCRETE;
    public static final ModConfigSpec.DoubleValue SHIELD_CASING;
    public static final ModConfigSpec.DoubleValue SHIELD_GLASS;
    public static final ModConfigSpec.DoubleValue SHIELD_HATCH_CLOSED;
    public static final ModConfigSpec.DoubleValue SHIELD_HATCH_OPEN;
    public static final ModConfigSpec.DoubleValue SHIELD_WATER;
    public static final ModConfigSpec.DoubleValue SHIELD_SOLID;
    public static final ModConfigSpec.DoubleValue GEAR_CONTAMINATION_PROTECTION;
    public static final ModConfigSpec.DoubleValue GEAR_DOSE_PROTECTION;
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
        b.push("radiation");
        RADIATION_ENABLED = b.comment("Whether players receive dose and contamination").define("enabled", true);
        RADIATION_EFFECTS = b.comment("Whether accumulated dose causes status effects").define("effects", true);
        DOSE_MULTIPLIER = b.comment("Multiplier on every dose rate").defineInRange("doseMultiplier", 1.0, 0.0, 100.0);
        RECOVERY_PER_MINUTE = b.comment("Body dose the player recovers per minute (mSv)").defineInRange("recoveryPerMinute", 30.0, 0.0, 100000.0);
        b.push("shielding");
        SHIELD_CONCRETE = b.comment("Attenuation per block of Shielding Concrete").defineInRange("concrete", 3.0, 0.0, 100.0);
        SHIELD_CASING = b.comment("Attenuation per block of Reactor Vessel Casing").defineInRange("casing", 1.0, 0.0, 100.0);
        SHIELD_GLASS = b.comment("Attenuation per block of Shielded Glass").defineInRange("glass", 1.5, 0.0, 100.0);
        SHIELD_HATCH_CLOSED = b.comment("Attenuation of a closed Shielded Hatch").defineInRange("hatchClosed", 2.5, 0.0, 100.0);
        SHIELD_HATCH_OPEN = b.comment("Attenuation of an open Shielded Hatch").defineInRange("hatchOpen", 0.05, 0.0, 100.0);
        SHIELD_WATER = b.comment("Attenuation per block of water").defineInRange("water", 0.7, 0.0, 100.0);
        SHIELD_SOLID = b.comment("Attenuation per block of any other full solid block").defineInRange("solid", 0.4, 0.0, 100.0);
        b.pop();
        GEAR_CONTAMINATION_PROTECTION = b.comment("Share of contamination uptake a full protective set blocks").defineInRange("gearContaminationProtection", 0.9, 0.0, 1.0);
        GEAR_DOSE_PROTECTION = b.comment("Share of direct dose a full protective set blocks").defineInRange("gearDoseProtection", 0.05, 0.0, 1.0);
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

    public static boolean radiation() {
        return !SPEC.isLoaded() || RADIATION_ENABLED.get();
    }

    public static boolean effects() {
        return !SPEC.isLoaded() || RADIATION_EFFECTS.get();
    }

    public static double get(ModConfigSpec.DoubleValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
