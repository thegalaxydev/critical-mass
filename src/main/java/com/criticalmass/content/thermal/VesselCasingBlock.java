package com.criticalmass.content.thermal;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class VesselCasingBlock extends Block {
    public static final IntegerProperty CRACK = IntegerProperty.create("crack", 0, 3);

    public VesselCasingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CRACK, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CRACK);
    }

    public static int stageFor(double integrity) {
        if (integrity >= 0.75) {
            return 0;
        }
        if (integrity >= 0.5) {
            return 1;
        }
        return integrity >= 0.25 ? 2 : 3;
    }

    public static double integrityFor(int stage) {
        return switch (stage) {
            case 1 -> 0.625;
            case 2 -> 0.375;
            case 3 -> 0.125;
            default -> 1.0;
        };
    }
}
