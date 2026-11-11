package com.criticalmass.content.thermal;

import java.util.List;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

public interface ThermalNode {
    CoolantBuffer coolant();

    @Nullable
    Direction passThroughExit(Direction enteredFrom);

    default CoolantBuffer bufferFor(Direction enteredFrom) {
        return coolant();
    }

    default List<CoolantBuffer> buffers() {
        return List.of(coolant());
    }

    default void onCoolantPassed(Direction enteredFrom, int amount) {
    }

    @Nullable
    default ThermalSinks sinks() {
        return null;
    }

    default boolean blocksFlow() {
        return false;
    }

    default void setCircuitPressure(double pressure, @Nullable CoolantBuffer worst) {
    }
}
