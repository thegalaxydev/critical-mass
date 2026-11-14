package com.criticalmass.registry;

import com.simibubi.create.api.stress.BlockStressValues;

public final class CMStress {
    public static final double CIRCULATION_PUMP_IMPACT = 4.0;
    public static final double CONTROL_ROD_DRIVE_IMPACT = 1.0;
    public static final double PLANT_SCREEN_IMPACT = 0.25;
    public static final double CENTRIFUGE_IMPACT = 8.0;

    private CMStress() {
    }

    public static void register() {
        BlockStressValues.IMPACTS.register(CMBlocks.CIRCULATION_PUMP.get(), () -> CIRCULATION_PUMP_IMPACT);
        BlockStressValues.IMPACTS.register(CMBlocks.CONTROL_ROD_DRIVE.get(), () -> CONTROL_ROD_DRIVE_IMPACT);
        BlockStressValues.IMPACTS.register(CMBlocks.PLANT_SCREEN.get(), () -> PLANT_SCREEN_IMPACT);
        BlockStressValues.IMPACTS.register(CMBlocks.CENTRIFUGE.get(), () -> CENTRIFUGE_IMPACT);
    }
}
