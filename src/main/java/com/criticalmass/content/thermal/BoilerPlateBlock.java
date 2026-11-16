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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class BoilerPlateBlock extends HorizontalBlock implements IBE<BoilerPlateBlockEntity>, PipeConnectable {
    public static final IntegerProperty HEAT = IntegerProperty.create("heat", 0, 2);

    public BoilerPlateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HEAT);
    }

    @Override
    public boolean connectsOn(BlockState state, Direction face) {
        return face.getAxis() == state.getValue(FACING).getAxis();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onBlockEntityUseItemOn(level, pos, be -> NodeBlocks.fill(level, pos, player, hand, be.coolant()));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<BoilerPlateBlockEntity> getBlockEntityClass() {
        return BoilerPlateBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BoilerPlateBlockEntity> getBlockEntityType() {
        return CMBlockEntities.BOILER_PLATE.get();
    }
}
