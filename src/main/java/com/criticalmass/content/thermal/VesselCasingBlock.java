package com.criticalmass.content.thermal;

import com.criticalmass.registry.CMItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

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

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(CMItems.REPAIR_KIT.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        boolean repaired = false;
        for (BlockEntity be : LoadedThermalBlocks.snapshot()) {
            if (be instanceof ReactorControllerBlockEntity c && be.getLevel() == level && c.owns(pos)) {
                repaired = c.repair(pos, CMThermal.REPAIR_KIT_AMOUNT);
                break;
            }
        }
        if (!repaired && state.getValue(CRACK) > 0) {
            level.setBlock(pos, state.setValue(CRACK, Math.max(0, state.getValue(CRACK) - 2)), Block.UPDATE_ALL);
            repaired = true;
        }
        if (!repaired) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 1.2f);
        return ItemInteractionResult.SUCCESS;
    }
}
