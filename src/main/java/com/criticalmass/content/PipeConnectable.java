package com.criticalmass.content;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public interface PipeConnectable {
    default boolean connectsOn(BlockState state, Direction face) {
        return true;
    }
}
