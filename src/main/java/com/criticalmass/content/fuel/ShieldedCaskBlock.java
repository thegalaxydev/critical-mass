package com.criticalmass.content.fuel;

import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ShieldedCaskBlock extends Block implements IBE<ShieldedCaskBlockEntity>, IWrenchable {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 12, 15), Block.box(2, 12, 2, 14, 16, 14));

    public ShieldedCaskBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, cask -> {
                cask.toggle();
                if (cask.bolted() && context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player
                        && cask.slots().stream().anyMatch(s -> s.is(com.criticalmass.registry.CMBlocks.CORE_DEBRIS.get().asItem()))) {
                    com.criticalmass.registry.CMTriggers.grant(player, "debris_sealed");
                }
                if (cask.bolted() && context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer keeper
                        && cask.slots().stream().anyMatch(s -> s.getItem() instanceof FuelAssemblyItem fuel && fuel.form() == FuelAssemblyItem.Form.SPENT)) {
                    com.criticalmass.registry.CMTriggers.grant(keeper, "dry_storage");
                }
            });
            IWrenchable.playRotateSound(level, pos);
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
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, be -> be.setPowered(level.hasNeighborSignal(pos)));
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (AllItems.WRENCH.isIn(stack)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return onBlockEntityUseItemOn(level, pos, be -> {
            if (!be.open()) {
                player.displayClientMessage(Component.translatable("criticalmass.cask.closed"), true);
                return ItemInteractionResult.FAIL;
            }
            String reason = ShieldedCaskBlockEntity.refuseReason(stack);
            if (reason != null) {
                player.displayClientMessage(Component.translatable(reason), true);
                return ItemInteractionResult.FAIL;
            }
            if (!be.insert(stack)) {
                player.displayClientMessage(Component.translatable("criticalmass.cask.full"), true);
                return ItemInteractionResult.FAIL;
            }
            stack.shrink(1);
            return ItemInteractionResult.SUCCESS;
        });
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return onBlockEntityUse(level, pos, be -> {
            if (!be.open()) {
                player.displayClientMessage(Component.translatable("criticalmass.cask.closed"), true);
                return InteractionResult.PASS;
            }
            ItemStack out = be.takeLast();
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
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof ShieldedCaskBlockEntity be && !be.isEmpty()) {
            ItemStack drop = new ItemStack(this);
            drop.applyComponents(be.collectComponents());
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) {
            return;
        }
        contents.nonEmptyItems().forEach(s -> tooltip.add(s.getHoverName().copy().withStyle(ChatFormatting.GRAY)));
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof ShieldedCaskBlockEntity be)) {
            return 0;
        }
        int n = 0;
        for (ItemStack s : be.slots()) {
            if (!s.isEmpty()) {
                n++;
            }
        }
        return n * 15 / be.slots().size();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<ShieldedCaskBlockEntity> getBlockEntityClass() {
        return ShieldedCaskBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ShieldedCaskBlockEntity> getBlockEntityType() {
        return CMBlockEntities.SHIELDED_CASK.get();
    }
}
