package com.criticalmass.content.fuel;

import com.criticalmass.registry.CMBlockEntities;
import com.criticalmass.registry.CMSounds;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ChannelCapBlock extends Block implements IBE<ChannelCapBlockEntity>, IWrenchable {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final double OPEN_SHIELDING = 0.25;
    private static final float LOCK_PITCH = 0.8f;
    private static final float UNLOCK_PITCH = 1.1f;

    public ChannelCapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    public static void setOpen(Level level, BlockPos pos, BlockState state, boolean open) {
        if (state.getValue(OPEN) == open) {
            return;
        }
        level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);
        level.playSound(null, pos, CMSounds.PART_FIT.get(), SoundSource.BLOCKS, 0.7f, open ? UNLOCK_PITCH : LOCK_PITCH);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, ChannelCapBlockEntity::clearSignal);
            setOpen(level, pos, state, !state.getValue(OPEN));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, be -> be.setPowered(level.hasNeighborSignal(pos)));
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof FuelAssemblyItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        return onBlockEntityUseItemOn(level, pos, be -> {
            String reason = be.refuseReason(stack);
            if (reason != null) {
                player.displayClientMessage(Component.translatable(reason), true);
                return ItemInteractionResult.FAIL;
            }
            ItemStack rest = be.insert(stack, false);
            if (rest.getCount() == stack.getCount()) {
                return ItemInteractionResult.FAIL;
            }
            player.setItemInHand(hand, rest);
            return ItemInteractionResult.SUCCESS;
        });
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return onBlockEntityUse(level, pos, be -> {
            String reason = be.blocker(false);
            if (reason != null) {
                player.displayClientMessage(Component.translatable(reason), true);
                return InteractionResult.PASS;
            }
            ItemStack out = be.extract(false);
            if (out.isEmpty()) {
                return InteractionResult.PASS;
            }
            if (!player.getInventory().add(out)) {
                player.drop(out, false);
            }
            return InteractionResult.SUCCESS;
        });
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ChannelCapBlockEntity be ? be.comparator() : 0;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<ChannelCapBlockEntity> getBlockEntityClass() {
        return ChannelCapBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ChannelCapBlockEntity> getBlockEntityType() {
        return CMBlockEntities.FUEL_CHANNEL_CAP.get();
    }
}
