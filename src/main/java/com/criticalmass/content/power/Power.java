package com.criticalmass.content.power;

public final class Power {
    public static final double GRID_HZ = 50.0;
    public static final float SYNC_RPM = 64f;
    public static final double KW_PER_SU = 4.0;
    public static final double SECTION_RATED_SU = 8192.0;
    public static final int MAX_SECTIONS = 8;
    public static final double TRIM_STEP = 0.02;
    public static final double TRIM_MAX = 1.0;
    public static final double LOAD_STEP = 0.02;
    public static final double SYNC_HZ = 0.2;
    public static final double SYNC_DEGREES = 20.0;
    public static final double OUT_OF_STEP_HZ = 1.5;
    public static final int SCAN_LIMIT = 2048;
    public static final int ORDER_GAP_TICKS = 600;
    public static final int ORDER_DEADLINE_TICKS = 2400;
    public static final int ORDER_HOLD_TICKS = 400;
    public static final double ORDER_MARGIN = 0.05;
    public static final int ORDER_STEP_MW = 5;
    public static final int ORDER_MAX_STEPS = 60;
    public static final double MOTOR_RATED_SU = 2048.0;
    public static final double MOTOR_EFFICIENCY = 0.95;
    public static final double MOTOR_IDLE = 0.01;
    public static final double SUPPLY_MIN_HZ = 47.0;
    public static final double SUPPLY_MAX_HZ = 53.0;
    public static final int OVERLOAD_TICKS = 20;
    public static final int LOCKOUT_TICKS = 100;
    public static final int MAX_BUS = 8;
    public static final double BATTERY_RATED_SU = 512.0;
    public static final int BATTERY_FULL_TICKS = 6000;
    public static final double BATTERY_CHARGE_SU = 128.0;
    public static final double BATTERY_EFFICIENCY = 0.9;
    public static final double DIESEL_RATED_SU = 4096.0;
    public static final int DIESEL_START_TICKS = 60;
    public static final double DIESEL_IDLE_MB = 0.02;
    public static final double DIESEL_LOAD_MB = 0.18;
    public static final int TRANSFER_LOSS_TICKS = 20;
    public static final int TRANSFER_RETURN_TICKS = 100;
    public static final int TRANSFER_PAUSE_TICKS = 10;
    public static final double RELAY_SECONDS = 2.0;
    public static final double RELAY_RESET_SECONDS = 5.0;
    public static final double RELAY_INSTANT = 8.0;
    public static final double BREAKER_INTERRUPT_MW = 1500.0;
    public static final double WIRE_STIFFNESS = 20000.0;
    public static final double ISOLATOR_BREAK_KA = 0.02;
    public static final double GRID_FAULT_MW = 5000.0;
    public static final double GENERATOR_FAULT_FACTOR = 5.0;
    public static final int EARTH_FAULT_STEPS = 3;
    public static final double SHAFT_SHARE = 0.9;
    public static final int SHED_TICKS = 10;
    public static final double GRID_KV = 220.0;
    public static final double GENERATOR_KV = 20.0;
    public static final double MOTOR_KV = 6.0;
    public static final double OVERVOLTAGE = 1.1;
    public static final double UNDERVOLTAGE = 0.9;
    public static final double TRANSFORMER_MW = 100.0;
    public static final double TRANSFORMER_HEAT_SECONDS = 15.0;
    public static final double TRANSFORMER_COOL_SECONDS = 40.0;
    public static final double WIRE_HEAT_SECONDS = 10.0;
    public static final double WIRE_COOL_SECONDS = 20.0;
    private static final double ROOT_THREE = Math.sqrt(3.0);

    private Power() {
    }

    public static double megawatts(double su) {
        return su * KW_PER_SU / 1000.0;
    }

    public static double megawatts(double kilovolts, double kiloamps) {
        return ROOT_THREE * kilovolts * kiloamps;
    }

    public static double kiloamps(double megawatts, double kilovolts) {
        return kilovolts <= 0 ? 0 : megawatts / (ROOT_THREE * kilovolts);
    }

    public static double su(double megawatts) {
        return megawatts * 1000.0 / KW_PER_SU;
    }

    public static double wrap(double degrees) {
        double d = degrees % 360.0;
        if (d > 180) {
            d -= 360;
        } else if (d <= -180) {
            d += 360;
        }
        return d;
    }
}
