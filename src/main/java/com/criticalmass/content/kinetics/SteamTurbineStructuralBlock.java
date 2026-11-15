package com.criticalmass.content.kinetics;

import com.criticalmass.registry.CMBlockEntities;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.api.equipment.goggles.IProxyHoveringInformation;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SteamTurbineStructuralBlock extends KineticBlock implements EntityBlock, IProxyHoveringInformation {
    public SteamTurbineStructuralBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TurbineStructure.FACING, Direction.NORTH).setValue(TurbineStructure.ALONG, 1).setValue(TurbineStructure.ACROSS, 1).setValue(TurbineStructure.RISE, 0).setValue(TurbineStructure.DUCT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(TurbineStructure.FACING, TurbineStructure.ALONG, TurbineStructure.ACROSS, TurbineStructure.RISE, TurbineStructure.DUCT));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(TurbineStructure.DUCT) ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TurbineStructure.shape(state);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(TurbineStructure.FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return TurbineStructure.isShaft(state) && face.getAxis() == state.getValue(TurbineStructure.FACING).getAxis();
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (TurbineStructure.isShaft(state)) {
            return new KineticBlockEntity(CMBlockEntities.STEAM_TURBINE_SHAFT.get(), pos, state);
        }
        if (TurbineStructure.portFace(state) != null) {
            return new SteamTurbinePortBlockEntity(CMBlockEntities.STEAM_TURBINE_PORT.get(), pos, state);
        }
        return null;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == CMBlockEntities.STEAM_TURBINE_SHAFT.get() ? new SmartBlockEntityTicker<>() : null;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(CMBlocks.STEAM_TURBINE.get());
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!TurbineStructure.valid(level, pos, state)) {
            return InteractionResult.PASS;
        }
        BlockPos master = TurbineStructure.master(pos, state);
        UseOnContext onMaster = new UseOnContext(level, context.getPlayer(), context.getHand(), context.getItemInHand(),
                new BlockHitResult(context.getClickLocation(), context.getClickedFace(), master, context.isInside()));
        BlockState masterState = level.getBlockState(master);
        return masterState.getBlock() instanceof SteamTurbineBlock turbine ? turbine.onSneakWrenched(masterState, onMaster) : InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        boolean gone = !state.is(newState.getBlock());
        super.onRemove(state, level, pos, newState, moving);
        if (gone && TurbineStructure.valid(level, pos, state)) {
            level.destroyBlock(TurbineStructure.master(pos, state), true);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (TurbineStructure.valid(level, pos, state)) {
            BlockPos master = TurbineStructure.master(pos, state);
            level.destroyBlockProgress(master.hashCode(), master, -1);
            if (!level.isClientSide() && player.isCreative()) {
                level.destroyBlock(master, false);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (level instanceof Level real && !real.isClientSide() && !TurbineStructure.valid(level, pos, state) && !real.getBlockTicks().hasScheduledTick(pos, this)) {
            real.scheduleTick(pos, this, 1);
        }
        Direction duct = TurbineStructure.ductFace(state);
        return duct != null && direction == duct ? state.setValue(TurbineStructure.DUCT, TurbineStructure.piped(level, pos, duct)) : state;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!TurbineStructure.valid(level, pos, state)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    @Override
    public boolean addLandingEffects(BlockState state, ServerLevel level, BlockPos pos, BlockState other, LivingEntity entity, int particles) {
        return true;
    }

    @Override
    public BlockPos getInformationSource(Level level, BlockPos pos, BlockState state) {
        return TurbineStructure.valid(level, pos, state) ? TurbineStructure.master(pos, state) : pos;
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return false;
    }
}
