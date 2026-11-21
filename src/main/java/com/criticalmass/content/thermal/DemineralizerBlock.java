package com.criticalmass.content.thermal;

import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class DemineralizerBlock extends HorizontalBlock implements IBE<DemineralizerBlockEntity>, IWrenchable {
    public enum Resin implements StringRepresentable {
        NONE, FRESH, USED, SPENT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Resin> RESIN = EnumProperty.create("resin", Resin.class);

    public DemineralizerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(RESIN, Resin.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RESIN);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!ResinCartridgeItem.isCartridge(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        return onBlockEntityUseItemOn(level, pos, be -> {
            String refused = be.refuseReason(stack);
            if (refused != null) {
                player.displayClientMessage(Component.translatable(refused), true);
                return ItemInteractionResult.FAIL;
            }
            be.insert(stack);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            return ItemInteractionResult.SUCCESS;
        });
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return onBlockEntityUse(level, pos, be -> {
            ItemStack out = be.take();
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
        return level.getBlockEntity(pos) instanceof DemineralizerBlockEntity be ? be.analog() : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<DemineralizerBlockEntity> getBlockEntityClass() {
        return DemineralizerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DemineralizerBlockEntity> getBlockEntityType() {
        return CMBlockEntities.DEMINERALIZER.get();
    }
}
