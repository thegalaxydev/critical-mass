package com.criticalmass.content.thermal;

import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.content.PipeConnectable;
import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

public class HeatExchangerBlock extends HorizontalBlock implements IBE<HeatExchangerBlockEntity>, PipeConnectable {
    public HeatExchangerBlock(Properties properties) {
        super(properties);
    }

    public static boolean connectsOn(BlockState state, Direction face) {
        return face.getAxis() == Direction.Axis.Y || face.getAxis() == state.getValue(FACING).getAxis();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onBlockEntityUseItemOn(level, pos, be -> FluidUtil.interactWithFluidHandler(player, hand, be.bufferFor(hit.getDirection()).handler())
                ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<HeatExchangerBlockEntity> getBlockEntityClass() {
        return HeatExchangerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HeatExchangerBlockEntity> getBlockEntityType() {
        return CMBlockEntities.HEAT_EXCHANGER.get();
    }
}
