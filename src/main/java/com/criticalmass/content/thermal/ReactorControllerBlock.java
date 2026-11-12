package com.criticalmass.content.thermal;

import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

public class ReactorControllerBlock extends HorizontalBlock implements IBE<ReactorControllerBlockEntity> {
    public ReactorControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onBlockEntityUseItemOn(level, pos, be -> FluidUtil.interactWithFluidHandler(player, hand, be.coolant().handler())
                ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ReactorControllerBlockEntity c ? c.comparator() : 0;
    }

    @Override
    public Class<ReactorControllerBlockEntity> getBlockEntityClass() {
        return ReactorControllerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ReactorControllerBlockEntity> getBlockEntityType() {
        return CMBlockEntities.REACTOR_CONTROLLER.get();
    }
}
