package com.criticalmass.content.power;

import net.minecraft.world.phys.Vec3;

public interface WireNode {
    WireLinks wires();

    Vec3 terminal(int index);

    void wiresChanged();

    default int terminalAt(Vec3 local) {
        return 0;
    }
}
