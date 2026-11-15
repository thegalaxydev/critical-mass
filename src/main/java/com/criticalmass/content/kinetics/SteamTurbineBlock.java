package com.criticalmass.content.kinetics;

import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public class SteamTurbineBlock extends HorizontalKineticBlock implements IBE<SteamTurbineBlockEntity> {
    public SteamTurbineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return TurbineStructure.clear(context.getLevel(), context.getClickedPos(), state.getValue(HORIZONTAL_FACING)) ? state : null;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (level.isClientSide() || oldState.is(this)) {
            return;
        }
        if (!TurbineStructure.build(level, pos, state.getValue(HORIZONTAL_FACING)) && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!TurbineStructure.build(level, pos, state.getValue(HORIZONTAL_FACING))) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (level instanceof Level real && !real.isClientSide() && !real.getBlockTicks().hasScheduledTick(pos, this)) {
            real.scheduleTick(pos, this, 1);
        }
        return state;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (!level.isClientSide() && !newState.is(this)) {
            TurbineStructure.clearAway(level, pos, state.getValue(HORIZONTAL_FACING));
        }
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(HORIZONTAL_FACING);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public Class<SteamTurbineBlockEntity> getBlockEntityClass() {
        return SteamTurbineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SteamTurbineBlockEntity> getBlockEntityType() {
        return CMBlockEntities.STEAM_TURBINE.get();
    }
}
