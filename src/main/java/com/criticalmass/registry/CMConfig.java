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
    public static final ModConfigSpec.BooleanValue MELTDOWN;
    public static final ModConfigSpec.IntValue CORIUM_DEPTH;
    public static final ModConfigSpec.DoubleValue CORIUM_CONTAMINATION;
    public static final ModConfigSpec.BooleanValue NUCLEAR_EXPLOSION;
    public static final ModConfigSpec.IntValue EXPLOSION_RADIUS;
    public static final ModConfigSpec.BooleanValue BLAST_SHELTER;
    public static final ModConfigSpec.BooleanValue FALLOUT;
    public static final ModConfigSpec.BooleanValue MUTATIONS;
    public static final ModConfigSpec.DoubleValue FALLOUT_HALF_LIFE;
    public static final ModConfigSpec.BooleanValue ELECTRICAL_FIRES;
    public static final ModConfigSpec.BooleanValue ELECTRICAL_BLASTS_BREAK_BLOCKS;
    public static final ModConfigSpec.BooleanValue LIVE_WORK_HURTS;
    public static final ModConfigSpec.BooleanValue ELECTRICAL_WEATHER;
    public static final ModConfigSpec.BooleanValue GROW_CHARTRINE;
    public static final ModConfigSpec.IntValue CHARTRINE_COOLANT;
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
        b.push("accidents");
        MELTDOWN = b.comment("Whether fuel above 1800 C melts into corium and ruins its channel").define("meltdown", true);
        CORIUM_DEPTH = b.comment("How many solid blocks hot corium can melt down through after leaving the vessel").defineInRange("coriumDepth", 3, 0, 64);
        CORIUM_CONTAMINATION = b.comment("Multiplier on the contamination released when corium breaches the vessel").defineInRange("coriumContamination", 1.0, 0.0, 100.0);
        NUCLEAR_EXPLOSION = b.comment("Whether a core whose every fuel column has melted goes off as a nuclear explosion; turned off, a full melt only leaves corium").define("nuclearExplosion", true);
        EXPLOSION_RADIUS = b.comment("Radius in blocks of the crater a 15-block core leaves; bigger cores leave bigger ones, and fire and blast reach about twice as far").defineInRange("explosionRadius", 40, 4, 96);
        BLAST_SHELTER = b.comment("Whether blast-proof blocks shelter what is behind them from a nuclear explosion; turned off, they only survive it themselves").define("blastShelter", true);
        FALLOUT = b.comment("Whether a nuclear explosion leaves a plume of fallout downwind").define("fallout", true);
        MUTATIONS = b.comment("Whether fallout and contaminated ground turn mobs into irradiated ones").define("mutations", true);
        FALLOUT_HALF_LIFE = b.comment("Minutes for the fallout in the air to lose half its strength; the ash on the ground stays until it is cleared").defineInRange("falloutHalfLifeMinutes", 5.0, 0.5, 600.0);
        b.pop();
        b.push("electrical");
        ELECTRICAL_FIRES = b.comment("Whether electrical faults start fires").define("fires", true);
        ELECTRICAL_BLASTS_BREAK_BLOCKS = b.comment("Whether the blast from an electrical fault breaks the blocks round it; the failed equipment is always lost").define("blastsBreakBlocks", false);
        LIVE_WORK_HURTS = b.comment("Whether stringing or cutting a wire on a live terminal hurts the player").define("liveWorkHurts", true);
        ELECTRICAL_WEATHER = b.comment("Whether rain faults indoor electrical equipment left under the sky, and thunderstorms strike wires and disturb the grid").define("weather", true);
        b.pop();
        b.push("resources");
        GROW_CHARTRINE = b.comment("Whether Contaminated Coolant on the reactor's record grows Chartrine where it lands on lava; turned off, it only ever makes tuff").define("growChartrine", true);
        CHARTRINE_COOLANT = b.comment("How many mB of Contaminated Coolant land on lava, on average, for each block of stone that forms")
                .defineInRange("chartrineCoolant", (int) com.criticalmass.content.thermal.CMThermal.CHARTRINE_COOLANT, 1, 100000);
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

    public static boolean meltdown() {
        return !SPEC.isLoaded() || MELTDOWN.get();
    }

    public static boolean nuclearExplosion() {
        return !SPEC.isLoaded() || NUCLEAR_EXPLOSION.get();
    }

    public static boolean mutations() {
        return !SPEC.isLoaded() || MUTATIONS.get();
    }

    public static boolean fallout() {
        return !SPEC.isLoaded() || FALLOUT.get();
    }

    public static double falloutHalfLife() {
        return SPEC.isLoaded() ? FALLOUT_HALF_LIFE.get() : 5.0;
    }

    public static boolean blastShelter() {
        return !SPEC.isLoaded() || BLAST_SHELTER.get();
    }

    public static int explosionRadius() {
        return SPEC.isLoaded() ? EXPLOSION_RADIUS.get() : 40;
    }

    public static int coriumDepth() {
        return SPEC.isLoaded() ? CORIUM_DEPTH.get() : 3;
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
