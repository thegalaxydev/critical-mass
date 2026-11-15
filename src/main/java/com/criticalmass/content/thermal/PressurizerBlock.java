package com.criticalmass.content.thermal;

import com.criticalmass.content.control.DashPart;
import com.criticalmass.content.control.DashPartItem;
import com.criticalmass.registry.CMBlockEntities;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class PressurizerBlock extends Block implements IBE<PressurizerBlockEntity>, IWrenchable {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 1, 15), Block.box(2, 1, 2, 14, 16, 14));

    public PressurizerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return context.getLevel().getBlockState(context.getClickedPos().above()).canBeReplaced(context) ? defaultBlockState() : null;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide || oldState.is(this)) {
            return;
        }
        BlockPos above = pos.above();
        if (level.getBlockState(above).canBeReplaced()) {
            level.setBlock(above, CMBlocks.PRESSURIZER_TOP.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof DashPartItem part) || part.type != DashPart.Type.GAUGE || !hit.getDirection().getAxis().isHorizontal()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        return onBlockEntityUseItemOn(level, pos, be -> {
            if (!be.fitGauge(hit.getDirection())) {
                return ItemInteractionResult.FAIL;
            }
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.6f, 1.2f);
            return ItemInteractionResult.SUCCESS;
        });
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof PressurizerBlockEntity be) || be.gaugeFace() == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ItemStack gauge = be.takeGauge();
            Player player = context.getPlayer();
            if (player == null || !player.getInventory().add(gauge)) {
                Block.popResource(level, context.getClickedPos(), gauge);
            }
            level.playSound(null, context.getClickedPos(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.6f, 1.2f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof PressurizerBlockEntity p ? (int) Math.round(15 * p.pressure() / CMThermal.PRESSURIZER_MAX) : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(this) && level.getBlockState(pos.above()).is(CMBlocks.PRESSURIZER_TOP.get())) {
            level.removeBlock(pos.above(), false);
        }
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<PressurizerBlockEntity> getBlockEntityClass() {
        return PressurizerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PressurizerBlockEntity> getBlockEntityType() {
        return CMBlockEntities.PRESSURIZER.get();
    }
}
