package com.criticalmass.content.thermal;

import com.criticalmass.registry.CMFluids;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

public final class CMThermal {
    public static final double AMBIENT = 20.0;
    public static final double HEAT_CAPACITY = 1.0;
    public static final double BOILING = 100.0;
    public static final double SATURATION_ENERGY = (BOILING - AMBIENT) * HEAT_CAPACITY;
    public static final double LATENT_HEAT = 540.0;
    public static final double STEAM_HEAT_CAPACITY = 0.5;
    public static final double ATMOSPHERIC = 1.0;
    public static final double PRESSURE_PER_LATENT = 9.0;
    public static final double RELIEF_PRESSURE = 8.0;
    public static final double RESEAT_PRESSURE = 6.0;
    public static final double RUPTURE_PRESSURE = 12.0;
    public static final int RELIEF_CAPACITY = 250;
    public static final int RELIEF_VENT_RATE = 20;
    public static final int CONDENSER_CAPACITY = 1000;
    public static final int TOWER_CAPACITY = 500;
    public static final double TOWER_RATE = 0.004;
    public static final int TOWER_MAX_PACKING = 12;
    public static final double TOWER_MAX_TILT = 15.0;
    public static final double EVAPORATION_HEAT_PER_MB = 2000.0;
    public static final int RADIATOR_CAPACITY = 250;
    public static final double RADIATOR_RATE = 0.0015;
    public static final double RADIATOR_MAX_BONUS = 2.0;
    public static final double RADIATOR_REFERENCE_SPEED = 20.0;
    public static final int BOILER_PLATE_CAPACITY = 500;
    public static final double BOILER_HEAT_PER_LEVEL = 400.0;
    public static final double BOILER_LEVEL_ONE_TEMPERATURE = 120.0;
    public static final double BOILER_LEVEL_TWO_TEMPERATURE = 200.0;
    public static final int CONTROLLER_CAPACITY = 8000;
    public static final int TURBINE_CAPACITY = 1000;
    public static final int EXCHANGER_CAPACITY = 1000;
    public static final double EXCHANGER_RATE = 0.1;
    public static final double EXCHANGER_REACH = 0.9;
    public static final double FUEL_BASE_OUTPUT = 50.0;
    public static final double FUEL_ADJACENCY_FACTOR = 0.25;
    public static final double FUEL_HEAT_CAPACITY = 50.0;
    public static final double FUEL_CONDUCTANCE = 0.25;
    public static final int COOLANT_REFERENCE_FLOW = 64;
    public static final double HOT_SPOT_TEMPERATURE = 800.0;
    public static final int REVALIDATE_INTERVAL = 100;
    public static final double DECAY_FRACTION = 0.07;
    public static final double DECAY_BUILDUP_TICKS = 600.0;
    public static final double DECAY_HALF_LIFE_TICKS = 1200.0;
    public static final double RESIDUAL_COLD_LIMIT = 1.0;
    public static final double COLD_TEMPERATURE = 60.0;
    public static final double TRIP_TEMPERATURE = 1100.0;
    public static final int LOW_FLOW_TRIP_TICKS = 40;
    public static final double ROD_RATE_PER_RPM = 0.00005;
    public static final double ROD_MAX_RATE = 0.002;
    public static final double ROD_MANUAL_RATE = 0.001;
    public static final int ROD_FAILSAFE_TICKS = 40;
    public static final int BREACH_LEAK_RATE = 40;
    public static final int REPAIR_TICKS = 40;
    public static final double REPAIR_RESIDUAL_LIMIT = 20.0;
    public static final double CASING_DAMAGE_PER_SECOND = 0.02;
    public static final double DAMAGED_THRESHOLD = 0.25;
    public static final double REPAIR_AMOUNT = 0.5;
    public static final double TURBINE_EXTRACTION = 0.02;
    public static final double SU_PER_HEAT = 4.0;
    public static final float TURBINE_RPM = 16.0f;
    public static final double TURBINE_MIN_SU = 64.0;
    public static final int TURBINE_MAX_RPM = 256;
    public static final double TURBINE_WINDAGE_SU = 16.0;
    public static final double TURBINE_WINDAGE_SHARE = 0.02;
    public static final double GOVERNOR_RATE = 0.05;
    public static final double ROTOR_INERTIA_TICKS = 50.0;
    public static final double ROTOR_DAMPING = 0.02;
    public static final double OVERSPEED_TRIP = 0.1;
    public static final double BLADE_WEAR = 1.0 / 720000.0;
    public static final double BLADE_TRIP_WEAR = 0.02;
    public static final double BLADE_DERATE = 0.25;
    public static final double BLADE_STEP = 0.25;
    public static final float BLADE_HURT = 6.0f;
    public static final int LOAD_SAMPLE_TICKS = 5;
    public static final int PUMP_FLOW_PER_RPM = 2;
    public static final int PUMP_MAX_FLOW = 256;
    public static final int PIPE_CAPACITY = 100;
    public static final int MAX_FLOW_STEPS = 8;
    public static final float PUMP_IDLE_STRESS = 0.25f;
    public static final int MAX_PIPE_LENGTH = 256;
    public static final double MELT_TEMPERATURE = 1800.0;
    public static final double CORIUM_QUENCH = 0.01;
    public static final double CORIUM_ATTACK_TEMPERATURE = 1000.0;
    public static final double CORIUM_ATTACK_PER_SECOND = 0.1;
    public static final int CORIUM_TICKS = 20;
    public static final double CORIUM_COOLING = 0.0005;
    public static final double CORIUM_WATER_BONUS = 6.0;
    public static final double CORIUM_SOLID_TEMPERATURE = 300.0;
    public static final double CORIUM_SINK_TEMPERATURE = 1200.0;
    public static final double CORIUM_DOSE = 10.0;
    public static final double CORIUM_DECAY_DOSE = 20.0;
    public static final double CORIUM_ITEM_DOSE = 10.0;
    public static final double RUINED_ITEM_DOSE = 2.0;
    public static final double CORIUM_SPILL = 40.0;
    public static final double DELAYED_FRACTION = 0.0065;
    public static final double PRECURSOR_DECAY = 0.016;
    public static final double NEUTRON_SOURCE = 1.0e-10;
    public static final double PROMPT_FLOOR = 0.05;
    public static final double NEUTRON_CAP = 1000.0;
    public static final double FRESH_REACTIVITY = 0.085;
    public static final double BURNUP_LOSS = 0.35;
    public static final double ROD_WORTH = 0.10;
    public static final double MIN_FISSION_HEAT = 1.0e-3;
    public static final double ROD_SHADOW = 0.5;
    public static final double BORON_WORTH = 7.0e-5;
    public static final double FUEL_COEFFICIENT = 2.0e-5;
    public static final double MODERATOR_COEFFICIENT = 1.0e-4;
    public static final double XENON_WORTH = 0.025;
    public static final double VOID_WORTH = 0.2;
    public static final double MODERATOR_FULL_LEVEL = 0.5;
    public static final double IODINE_DECAY = 8.7e-5;
    public static final double XENON_DECAY = 6.3e-5;
    public static final double XENON_BURNOUT = 1.575e-4;
    public static final double IODINE_SHARE = 0.95;
    public static final double PERIOD_TRIP_SECONDS = 3.0;
    public static final double PERIOD_SMOOTHING = 0.05;
    public static final double HIGH_FLUX_TRIP = 1.18;
    public static final double POWER_RANGE = 0.001;
    public static final double CRITICAL_BAND = 1.0e-4;
    public static final double START_COOLANT_SHARE = 0.5;
    public static final int WITHDRAW_BLOCK_TICKS = 5;
    public static final double TOWER_NATURAL_DRAFT = 0.25;
    public static final double TOWER_NATURAL_PER_BLOCK = 0.05;
    public static final double TOWER_NATURAL_MAX = 0.75;
    public static final float TOWER_FAN_RPM = 64.0f;
    public static final int TOWER_SCAN_TICKS = 10;
    public static final double TOWER_PLUME_HEAT = 20.0;
    public static final double TOWER_SCALD_TEMPERATURE = 50.0;
    public static final double TOWER_SCALD_PER_DEGREE = 0.04;
    public static final double TOWER_SCALD_MAX = 4.0;
    public static final double LOW_LEVEL_SHARE = 0.25;
    public static final double UNPRESSURIZED_BOILING = 350.0;
    public static final double BOILING_PER_BAR = 2.0;
    public static final double PRESSURIZER_MAX = 150.0;
    public static final double PRESSURIZER_HEAT_RATE = 0.25;
    public static final double PRESSURIZER_SPRAY_RATE = 0.5;
    public static final double PRESSURIZER_LEAK = 0.02;
    public static final double PRESSURIZER_MAX_BURNER = 2.0;
    public static final int PRESSURE_LINK_TICKS = 20;
    public static final double FLASH_SHARE = 0.25;
    public static final int CHANNEL_VISUAL_TICKS = 5;
    public static final int ROD_BANKS = 4;
    public static final double ROD_JOG = 0.005;
    public static final double LOW_COOLANT_ALARM = 0.5;
    public static final int ROD_LINK_TICKS = 20;
    public static final double PIPE_LEAK_PER_BAR = 0.004;
    public static final double PIPE_LEAK_MAX = 1.0;
    public static final double PLAIN_PIPE_DOSE = 1.0;
    public static final double CONTAMINATED_PIPE_DOSE = 5.0;
    public static final float PIPE_BURST_DAMAGE = 4.0f;
    public static final int MAX_VESSEL_BLOCKS = 2048;

    public static final double FUEL_OVERHEAT_WEAR = 0.0005;
    public static final double CASK_RESIDUAL_LIMIT = 0.5;
    public static final int RACK_SLOTS = 4;
    public static final int RACK_CAPACITY = 4000;
    public static final double RACK_POOL_RATE = 0.0005;
    public static final double RACK_DRY_HEAT_CAPACITY = 20.0;
    public static final double RACK_DRY_RATE = 0.002;
    public static final double RACK_DRY_DAMAGE_TEMPERATURE = 300.0;
    public static final double RACK_DRY_WEAR = 0.0005;
    public static final int CASK_SLOTS = 4;
    public static final double CLEAN_SPILL = 0.05;
    public static final double CONTAMINATED_SPILL = 0.2;
    public static final double DECAY_FACTOR = Math.pow(0.5, 1.0 / DECAY_HALF_LIFE_TICKS);

    private CMThermal() {
    }

    public static boolean isCoolant(FluidStack stack) {
        return stack.is(CMFluids.PRIMARY_COOLANT.source.get()) || stack.is(Fluids.WATER) || isContaminated(stack);
    }

    public static boolean isContaminated(FluidStack stack) {
        return stack.is(CMFluids.CONTAMINATED_COOLANT.source.get());
    }

    public static boolean isPrimaryMedium(FluidStack stack) {
        return stack.is(CMFluids.PRIMARY_COOLANT.source.get()) || isContaminated(stack);
    }

    public static float pumpCapRpm() {
        return PUMP_MAX_FLOW / (float) PUMP_FLOW_PER_RPM;
    }

    public static int pumpFlow(float speed) {
        return Math.min(PUMP_MAX_FLOW, Math.round(Math.abs(speed) * PUMP_FLOW_PER_RPM));
    }
}
