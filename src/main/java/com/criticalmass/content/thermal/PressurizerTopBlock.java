package com.criticalmass.content.thermal;

import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.api.equipment.goggles.IProxyHoveringInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PressurizerTopBlock extends Block implements IProxyHoveringInformation {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 9, 14), Block.box(1, 9, 1, 15, 10, 15), Block.box(3, 10, 3, 13, 13, 13), Block.box(5, 13, 5, 11, 15, 11),
            Block.box(7, 15, 7, 9, 16, 9));

    public PressurizerTopBlock(Properties properties) {
        super(properties);
    }

    private static boolean standsOn(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(CMBlocks.PRESSURIZER.get());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return standsOn(level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !standsOn(level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && standsOn(level, pos)) {
            level.destroyBlock(pos.below(), !player.isCreative(), player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && !newState.is(this) && standsOn(level, pos)) {
            level.destroyBlock(pos.below(), true);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(CMBlocks.PRESSURIZER.get());
    }

    @Override
    public BlockPos getInformationSource(Level level, BlockPos pos, BlockState state) {
        return standsOn(level, pos) ? pos.below() : pos;
    }
}
