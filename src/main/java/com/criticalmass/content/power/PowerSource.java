package com.criticalmass.content.power;

import net.minecraft.core.BlockPos;

public interface PowerSource {
    boolean online();

    double frequency();

    double phase();

    double ratedSu();

    double outputSu();

    double sourceKilovolts();

    BlockPos sourcePos();

    void faultTrip();
}
