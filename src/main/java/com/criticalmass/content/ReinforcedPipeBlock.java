package com.criticalmass.content;

import com.criticalmass.registry.CMBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ReinforcedPipeBlock extends FluidPipeBlock {
    public static final MapCodec<ReinforcedPipeBlock> CODEC = simpleCodec(ReinforcedPipeBlock::new);
    public static final Map<Direction, BooleanProperty> RIM = new EnumMap<>(Direction.class);

    static {
        for (Direction d : Direction.values()) {
            RIM.put(d, BooleanProperty.create("rim_" + d.getSerializedName()));
        }
    }

    public ReinforcedPipeBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState();
        for (Direction d : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(d), false).setValue(RIM.get(d), false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        RIM.values().forEach(builder::add);
    }

    @Override
    public BlockState updateBlockState(BlockState state, Direction preferredDirection, @Nullable Direction ignore, BlockAndTintGetter world, BlockPos pos) {
        return withRims(super.updateBlockState(state, preferredDirection, ignore, world, pos), world, pos);
    }

    private static BlockState withRims(BlockState state, BlockAndTintGetter world, BlockPos pos) {
        for (Direction d : Direction.values()) {
            state = state.setValue(RIM.get(d), isOpenAt(state, d) && shouldDrawRim(world, pos, state, d));
        }
        return state;
    }

    public static BlockState withConnections(BlockAndTintGetter world, BlockPos pos, BlockState state) {
        return ((ReinforcedPipeBlock) state.getBlock()).updateBlockState(state, Direction.NORTH, null, world, pos);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
        if (world.isClientSide || oldState.getBlock() == this) {
            return;
        }
        BlockState rimmed = withRims(state, world, pos);
        if (rimmed != state) {
            world.setBlock(pos, rimmed, Block.UPDATE_CLIENTS);
        }
    }

    public static boolean connectsTo(BlockState neighbor, Direction towards) {
        return isPipe(neighbor);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (!tryRemoveBracket(context)) {
            return InteractionResult.PASS;
        }
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState freed = world.getBlockState(pos);
        if (!world.isClientSide && freed.getBlock() == this) {
            Direction.Axis axis = FluidPropagator.getStraightPipeAxis(freed);
            BlockState joined = updateBlockState(freed, axis == null ? Direction.UP : Direction.get(Direction.AxisDirection.POSITIVE, axis), null, world, pos);
            if (joined != freed) {
                world.setBlockAndUpdate(pos, joined);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public BlockEntityType<? extends FluidPipeBlockEntity> getBlockEntityType() {
        return CMBlockEntities.REINFORCED_PIPE.get();
    }

    @Override
    protected MapCodec<? extends PipeBlock> codec() {
        return CODEC;
    }
}
