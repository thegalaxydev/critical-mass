package com.criticalmass.content.fuel;

import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.content.logistics.depot.SharedDepotBlockMethods;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FuelAssemblyBenchBlock extends HorizontalBlock implements IBE<FuelAssemblyBenchBlockEntity> {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 10, 0, 16, 16, 16), Block.box(1, 0, 1, 15, 10, 15));

    public FuelAssemblyBenchBlock(Properties properties) {
        super(properties, SHAPE, false);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FuelAssemblyBenchBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        boolean pressing = stack.isEmpty() && player.isShiftKeyDown();
        if (!be.workable() || (stack.isEmpty() && !pressing)) {
            return SharedDepotBlockMethods.onUse(stack, state, level, pos, player, hand, hit);
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        FuelAssemblyBenchBlockEntity.Outcome o = be.work(stack, pressing);
        switch (o) {
            case CONSUME, KEEP -> {
                if (o == FuelAssemblyBenchBlockEntity.Outcome.CONSUME && !player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, pressing ? SoundEvents.ANVIL_LAND : SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, pressing ? 0.3f : 0.8f, 1.2f);
                return ItemInteractionResult.SUCCESS;
            }
            case TOO_HOT -> {
                player.displayClientMessage(Component.translatable("criticalmass.bench.too_hot"), true);
                return ItemInteractionResult.FAIL;
            }
            case WRONG -> {
                Ingredient next = be.nextIngredient();
                if (next != null && next.getItems().length > 0) {
                    player.displayClientMessage(Component.translatable("criticalmass.bench.next_deploy", next.getItems()[0].getHoverName()), true);
                } else if (be.pressNext()) {
                    player.displayClientMessage(Component.translatable("criticalmass.bench.next_press"), true);
                }
                return ItemInteractionResult.FAIL;
            }
            default -> {
                return SharedDepotBlockMethods.onUse(stack, state, level, pos, player, hand, hit);
            }
        }
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        super.updateEntityAfterFallOn(level, entity);
        SharedDepotBlockMethods.onLanded(level, entity);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return SharedDepotBlockMethods.getComparatorInputOverride(state, level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<FuelAssemblyBenchBlockEntity> getBlockEntityClass() {
        return FuelAssemblyBenchBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FuelAssemblyBenchBlockEntity> getBlockEntityType() {
        return CMBlockEntities.FUEL_ASSEMBLY_BENCH.get();
    }
}
