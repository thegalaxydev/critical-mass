package com.criticalmass.content.thermal;

import com.criticalmass.registry.CMFluids;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

public final class CMThermal {
    public static final double AMBIENT = 20.0;
    public static final double HEAT_CAPACITY = 1.0;
    public static final int CONTROLLER_CAPACITY = 8000;
    public static final int TURBINE_CAPACITY = 1000;
    public static final double FUEL_BASE_OUTPUT = 50.0;
    public static final double FUEL_ADJACENCY_FACTOR = 0.25;
    public static final double FUEL_HEAT_CAPACITY = 200.0;
    public static final double COOLANT_EXTRACTION = 0.05;
    public static final int COOLANT_REFERENCE_FLOW = 64;
    public static final double HOT_SPOT_TEMPERATURE = 800.0;
    public static final int REVALIDATE_INTERVAL = 100;
    public static final double TURBINE_EXTRACTION = 0.02;
    public static final double SU_PER_HEAT = 4.0;
    public static final float TURBINE_RPM = 16.0f;
    public static final double TURBINE_MIN_SU = 64.0;
    public static final int PUMP_FLOW_PER_RPM = 2;
    public static final int PUMP_MAX_FLOW = 256;
    public static final int MAX_PIPE_LENGTH = 256;
    public static final int MAX_VESSEL_BLOCKS = 2048;

    private CMThermal() {
    }

    public static boolean isCoolant(FluidStack stack) {
        return stack.is(CMFluids.PRIMARY_COOLANT.source.get()) || stack.is(Fluids.WATER);
    }

    public static int pumpFlow(float speed) {
        return Math.min(PUMP_MAX_FLOW, Math.round(Math.abs(speed) * PUMP_FLOW_PER_RPM));
    }
}
