package com.criticalmass.content.thermal;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

public interface ThermalNode {
    CoolantBuffer coolant();

    @Nullable
    Direction passThroughExit(Direction enteredFrom);

    default void onCoolantPassed(int amount) {
    }
}
