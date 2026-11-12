package com.criticalmass.content.control;

import com.criticalmass.content.WallPanelBlock;
import com.criticalmass.registry.CMBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class PanelBlocks {
    private PanelBlocks() {
    }

    public abstract static class Bound extends WallPanelBlock {
        protected Bound(Properties properties, VoxelShape north) {
            super(properties, north);
        }

        @Override
        protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return stack.getItem() instanceof PlantLinkerItem ? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
    }

    public static class Button extends Bound implements IBE<ControlButtonBlockEntity> {
        public static final MapCodec<Button> CODEC = simpleCodec(Button::new);

        public Button(Properties properties) {
            super(properties, Block.box(4, 4, 13, 12, 12, 16));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            return onBlockEntityUse(level, pos, be -> {
                String result = be.press();
                if (!result.isEmpty()) {
                    player.displayClientMessage(Component.translatable(result), true);
                }
                return InteractionResult.SUCCESS;
            });
        }

        @Override
        protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
            if (!level.isClientSide) {
                withBlockEntityDo(level, pos, be -> be.signal(level.hasNeighborSignal(pos)));
            }
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
            IBE.onRemove(state, level, pos, newState);
        }

        @Override
        public Class<ControlButtonBlockEntity> getBlockEntityClass() {
            return ControlButtonBlockEntity.class;
        }

        @Override
        public BlockEntityType<? extends ControlButtonBlockEntity> getBlockEntityType() {
            return CMBlockEntities.CONTROL_BUTTON.get();
        }
    }

    public static class Annunciator extends Bound implements IBE<AnnunciatorBlockEntity> {
        public static final MapCodec<Annunciator> CODEC = simpleCodec(Annunciator::new);

        public Annunciator(Properties properties) {
            super(properties, Block.box(1, 1, 13, 15, 15, 16));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            return onBlockEntityUse(level, pos, be -> {
                be.acknowledge();
                player.displayClientMessage(Component.translatable("criticalmass.button.result.acknowledged"), true);
                return InteractionResult.SUCCESS;
            });
        }

        @Override
        protected boolean isSignalSource(BlockState state) {
            return true;
        }

        @Override
        protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
            return level.getBlockEntity(pos) instanceof AnnunciatorBlockEntity a && a.sounding() ? 15 : 0;
        }

        @Override
        protected boolean hasAnalogOutputSignal(BlockState state) {
            return true;
        }

        @Override
        protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof AnnunciatorBlockEntity a ? a.activeCount() : 0;
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
            IBE.onRemove(state, level, pos, newState);
        }

        @Override
        public Class<AnnunciatorBlockEntity> getBlockEntityClass() {
            return AnnunciatorBlockEntity.class;
        }

        @Override
        public BlockEntityType<? extends AnnunciatorBlockEntity> getBlockEntityType() {
            return CMBlockEntities.ANNUNCIATOR.get();
        }
    }
}
