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

    private Power() {
    }

    public static double megawatts(double su) {
        return su * KW_PER_SU / 1000.0;
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
