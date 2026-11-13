package com.criticalmass.content.control;

import com.lycoris.loconautics.content.dashboard.DashboardBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class AlloyDashboardBlock extends DashboardBlock {
    public static final MapCodec<AlloyDashboardBlock> CODEC = simpleCodec(AlloyDashboardBlock::new);

    public AlloyDashboardBlock(Properties properties) {
        super(properties, DyeColor.GRAY);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState paintedState(BlockState state, DyeColor dyed) {
        return state;
    }

    @Override
    public ItemInteractionResult paint(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
