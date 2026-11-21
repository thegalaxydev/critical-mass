package com.criticalmass.content.radiation;

import com.criticalmass.content.CMShapes;
import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AreaMonitorBlock extends Block implements IBE<AreaMonitorBlockEntity>, IWrenchable {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ALARM = BooleanProperty.create("alarm");
    public static final int ALARM_LIGHT = 7;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(3, 3, 12, 13, 14, 16), Block.box(7, 14, 13, 9, 16, 15), Block.box(4, 1, 13, 12, 3, 15));

    public AreaMonitorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ALARM, false));
    }

    public static BlockPos support(BlockState state, BlockPos pos) {
        return pos.relative(state.getValue(FACING).getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ALARM);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face.getAxis().isHorizontal()) {
            BlockState state = defaultBlockState().setValue(FACING, face);
            if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                return state;
            }
        }
        for (Direction looking : context.getNearestLookingDirections()) {
            if (looking.getAxis().isHorizontal()) {
                BlockState state = defaultBlockState().setValue(FACING, looking.getOpposite());
                if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                    return state;
                }
            }
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos behind = support(state, pos);
        return level.getBlockState(behind).isFaceSturdy(level, behind, state.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CMShapes.rotated(SHAPE, state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AreaMonitorBlockEntity monitor) || !monitor.canSilence()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            monitor.silence();
            player.displayClientMessage(Component.translatable("criticalmass.monitor.silenced_now"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(ALARM) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(ALARM) && state.getValue(FACING) == direction ? 15 : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AreaMonitorBlockEntity monitor ? monitor.analog() : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        boolean gone = !next.is(this);
        if (state.getValue(ALARM) && (gone || !next.getValue(ALARM))) {
            level.updateNeighborsAt(support(state, pos), this);
        }
        if (gone) {
            super.onRemove(state, level, pos, next, moved);
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public Class<AreaMonitorBlockEntity> getBlockEntityClass() {
        return AreaMonitorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AreaMonitorBlockEntity> getBlockEntityType() {
        return CMBlockEntities.AREA_RADIATION_MONITOR.get();
    }
}
