package com.criticalmass.content.thermal;

public interface BufferNode extends ThermalNode {
    String status();

    default double heatSourced() {
        return 0;
    }

    default double extraStoredHeat() {
        return 0;
    }
}
