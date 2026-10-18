package com.criticalmass.content;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;

public class ReliefValveBlock extends FacingBlock {
    public ReliefValveBlock(Properties properties) {
        super(properties, Shapes.or(Block.box(3, 3, 12, 13, 13, 16), Block.box(4, 4, 6, 12, 12, 12), Block.box(6, 6, 2, 10, 10, 6), Block.box(7, 10, 3, 9, 14, 5)));
    }
}
