package com.criticalmass.content.thermal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class ChannelBlocks {
    public static final int ROD_LEVELS = 4;
    public static final int GLOW_LEVELS = 3;
    public static final int GLOW_HOT = 3;

    private ChannelBlocks() {
    }

    public static class Control extends RotatedPillarBlock {
        public static final IntegerProperty ROD = IntegerProperty.create("rod", 0, ROD_LEVELS);

        public Control(Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(ROD, ROD_LEVELS));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(ROD);
        }
    }

    public static class Fuel extends RotatedPillarBlock {
        public static final IntegerProperty GLOW = IntegerProperty.create("glow", 0, GLOW_LEVELS);

        public Fuel(Properties properties) {
            super(properties.lightLevel(s -> s.getValue(GLOW) == 0 ? 0 : 3 + 4 * s.getValue(GLOW)));
            registerDefaultState(defaultBlockState().setValue(GLOW, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(GLOW);
        }
    }

    public static class Viewport extends VesselCasingBlock {
        public Viewport(Properties properties) {
            super(properties);
        }

        @Override
        protected boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
            return adjacent.getBlock() instanceof Viewport || super.skipRendering(state, adjacent, side);
        }

        @Override
        protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
            return 1.0f;
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
            return true;
        }
    }
}
