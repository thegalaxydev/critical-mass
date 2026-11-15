package com.criticalmass.content;

import com.criticalmass.content.thermal.NodeBlocks;
import com.criticalmass.content.thermal.ReliefValveBlockEntity;
import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.foundation.block.IBE;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ReliefValveBlock extends FacingBlock implements IBE<ReliefValveBlockEntity>, PipeConnectable {
    public static final BooleanProperty VENTING = BooleanProperty.create("venting");
    public static final Map<Direction, BooleanProperty> PIPED = PipeBlock.PROPERTY_BY_DIRECTION;
    private static final VoxelShape BODY = Shapes.or(Block.box(3, 3, 3, 13, 13, 13), Block.box(4, 4, 2, 12, 12, 3), Block.box(5, 5, 0, 11, 11, 2));
    private static final VoxelShape STUB = Shapes.or(Block.box(3, 3, 0, 13, 13, 2), Block.box(4, 4, 2, 12, 12, 3));
    private final Map<BlockState, VoxelShape> shapes = new ConcurrentHashMap<>();

    public ReliefValveBlock(Properties properties) {
        super(properties, BODY);
        BlockState state = defaultBlockState().setValue(VENTING, false);
        for (BooleanProperty piped : PIPED.values()) {
            state = state.setValue(piped, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VENTING);
        PIPED.values().forEach(builder::add);
    }

    public static BlockState withPipes(LevelAccessor level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values()) {
            state = state.setValue(PIPED.get(d), piped(level, pos, state, d));
        }
        return state;
    }

    private static boolean piped(LevelAccessor level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() != state.getValue(FACING).getAxis() && NodeBlocks.piped(level, pos, face);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withPipes(context.getLevel(), context.getClickedPos(), super.getStateForPlacement(context));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PIPED.get(direction), piped(level, pos, state, direction));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.computeIfAbsent(state, ReliefValveBlock::shape);
    }

    private static VoxelShape shape(BlockState state) {
        VoxelShape shape = CMShapes.rotated(BODY, state.getValue(FACING));
        for (Direction d : Direction.values()) {
            if (state.getValue(PIPED.get(d))) {
                shape = Shapes.or(shape, CMShapes.rotated(STUB, d));
            }
        }
        return shape;
    }

    @Override
    public boolean connectsOn(BlockState state, Direction face) {
        return face.getAxis() != state.getValue(FACING).getAxis();
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(VENTING) ? 15 : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<ReliefValveBlockEntity> getBlockEntityClass() {
        return ReliefValveBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ReliefValveBlockEntity> getBlockEntityType() {
        return CMBlockEntities.RELIEF_VALVE.get();
    }
}
