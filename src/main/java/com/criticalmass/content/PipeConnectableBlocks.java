package com.criticalmass.content;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PipeConnectableBlocks {
    private PipeConnectableBlocks() {
    }

    public static class Horizontal extends HorizontalBlock implements PipeConnectable {
        public Horizontal(Properties properties) {
            super(properties);
        }

        public Horizontal(Properties properties, VoxelShape north) {
            super(properties, north, false);
        }
    }

    public static class Simple extends Block implements PipeConnectable {
        public Simple(Properties properties) {
            super(properties);
        }
    }
}
