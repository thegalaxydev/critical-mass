package com.criticalmass.content.thermal;

import com.criticalmass.content.CMShapes;
import com.criticalmass.registry.CMBlockEntities;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CondenserBlock extends HeatExchangerBlock {
    private static final VoxelShape LYING = Shapes.or(Block.box(2, 2, 1, 14, 14, 15), Block.box(1, 1, 0, 15, 15, 1), Block.box(1, 1, 15, 15, 15, 16));

    public CondenserBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis bankAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    protected VoxelShape body(BlockState state) {
        return CMShapes.rotated(LYING, state.getValue(FACING));
    }

    @Override
    public BlockEntityType<? extends HeatExchangerBlockEntity> getBlockEntityType() {
        return CMBlockEntities.CONDENSER.get();
    }
}
