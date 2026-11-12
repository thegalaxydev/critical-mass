package com.criticalmass.content.thermal;

import com.criticalmass.content.PipeConnectable;
import com.simibubi.create.foundation.block.IBE;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
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

public class PlantNodeBlock<T extends SingleBufferNodeBlockEntity> extends Block implements IBE<T>, PipeConnectable {
    private final Class<T> type;
    private final Supplier<BlockEntityType<T>> entity;
    private final BiPredicate<BlockState, Direction> pipeFaces;

    public PlantNodeBlock(Properties properties, Class<T> type, Supplier<BlockEntityType<T>> entity, BiPredicate<BlockState, Direction> pipeFaces) {
        super(properties);
        this.type = type;
        this.entity = entity;
        this.pipeFaces = pipeFaces;
    }

    @Override
    public boolean connectsOn(BlockState state, Direction face) {
        return pipeFaces.test(state, face);
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
        return level.getBlockEntity(pos) instanceof SingleBufferNodeBlockEntity node
                ? (int) Math.ceil(15.0 * node.coolant().amount() / node.coolant().capacity()) : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<T> getBlockEntityClass() {
        return type;
    }

    @Override
    public BlockEntityType<? extends T> getBlockEntityType() {
        return entity.get();
    }
}
