package com.criticalmass.content.thermal;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class EmergencyTankBlock extends Block implements IBE<EmergencyTankBlockEntity>, PipeConnectable {
    public EmergencyTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean connectsOn(BlockState state, Direction face) {
        return false;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onBlockEntityUseItemOn(level, pos, be -> NodeBlocks.fill(level, pos, player, hand, be.coolant()));
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof EmergencyTankBlockEntity tank
                ? (int) Math.ceil(15.0 * tank.coolant().amount() / tank.coolant().capacity()) : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<EmergencyTankBlockEntity> getBlockEntityClass() {
        return EmergencyTankBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EmergencyTankBlockEntity> getBlockEntityType() {
        return CMBlockEntities.EMERGENCY_TANK.get();
    }
}
