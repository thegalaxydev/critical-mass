package com.criticalmass.content.thermal;

import com.criticalmass.content.CMShapes;
import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.content.PipeConnectable;
import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.foundation.block.IBE;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;

public class HeatExchangerBlock extends HorizontalBlock implements IBE<HeatExchangerBlockEntity>, PipeConnectable {
    public static final Map<Direction, BooleanProperty> PIPED = PipeBlock.PROPERTY_BY_DIRECTION;
    private static final VoxelShape UPRIGHT = Shapes.or(Block.box(2, 1, 2, 14, 15, 14), Block.box(1, 0, 1, 15, 1, 15), Block.box(1, 15, 1, 15, 16, 15));
    private static final VoxelShape STUB = Shapes.or(Block.box(3, 3, 0, 13, 13, 2), Block.box(4, 4, 2, 12, 12, 3));
    private final Map<BlockState, VoxelShape> shapes = new ConcurrentHashMap<>();

    public HeatExchangerBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState();
        for (BooleanProperty piped : PIPED.values()) {
            state = state.setValue(piped, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        PIPED.values().forEach(builder::add);
    }

    public Direction.Axis bankAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    protected VoxelShape body(BlockState state) {
        return UPRIGHT;
    }

    public boolean sameBank(BlockState state, BlockState other) {
        return other.getBlock() == this && other.getValue(FACING).getAxis() == state.getValue(FACING).getAxis();
    }

    @Override
    public boolean connectsOn(BlockState state, Direction face) {
        return face.getAxis() == Direction.Axis.Y || face.getAxis() == state.getValue(FACING).getAxis();
    }

    private boolean stub(LevelAccessor level, BlockPos pos, BlockState state, Direction face) {
        return connectsOn(state, face) && face.getAxis() != bankAxis(state) && NodeBlocks.piped(level, pos, face);
    }

    public BlockState withStubs(LevelAccessor level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values()) {
            state = state.setValue(PIPED.get(d), stub(level, pos, state, d));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withStubs(context.getLevel(), context.getClickedPos(), super.getStateForPlacement(context));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PIPED.get(direction), stub(level, pos, state, direction));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.computeIfAbsent(state, this::shape);
    }

    private VoxelShape shape(BlockState state) {
        VoxelShape shape = body(state);
        for (Direction d : Direction.values()) {
            if (state.getValue(PIPED.get(d))) {
                shape = Shapes.or(shape, CMShapes.rotated(STUB, d));
            }
        }
        return shape;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onBlockEntityUseItemOn(level, pos, be -> {
            ItemInteractionResult retubed = be.retube(stack, player);
            if (retubed != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
                return retubed;
            }
            return FluidUtil.interactWithFluidHandler(player, hand, be.bufferFor(hit.getDirection()).handler())
                    ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                    : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        });
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<HeatExchangerBlockEntity> getBlockEntityClass() {
        return HeatExchangerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HeatExchangerBlockEntity> getBlockEntityType() {
        return CMBlockEntities.HEAT_EXCHANGER.get();
    }
}
