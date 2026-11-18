package com.criticalmass.content.control;

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

public class MeltdownSirenBlock extends Block implements IBE<MeltdownSirenBlockEntity>, IWrenchable {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WALL = BooleanProperty.create("wall");
    private static final VoxelShape HEAD = Shapes.or(Block.box(3, 5, 0, 13, 15, 14), Block.box(2, 4, 0, 14, 16, 2));
    private static final VoxelShape STANDING = Shapes.or(Block.box(1, 0, 1, 15, 3, 15), Block.box(5, 3, 5, 11, 6, 11), HEAD);
    private static final VoxelShape MOUNTED = Shapes.or(Block.box(1, 3, 12, 15, 16, 15), Block.box(4, 6, 5, 12, 14, 12), HEAD);

    public MeltdownSirenBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WALL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WALL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        BlockState state = face.getAxis().isHorizontal() ? defaultBlockState().setValue(WALL, true).setValue(FACING, face)
                : defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.getValue(WALL)) {
            return canSupportCenter(level, pos.below(), Direction.UP);
        }
        Direction facing = state.getValue(FACING);
        BlockPos behind = pos.relative(facing.getOpposite());
        return level.getBlockState(behind).isFaceSturdy(level, behind, facing);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CMShapes.rotated(state.getValue(WALL) ? MOUNTED : STANDING, state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof MeltdownSirenBlockEntity siren) || !siren.canSilence()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            siren.silence();
            player.displayClientMessage(Component.translatable("criticalmass.siren.silenced_now"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
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
    public Class<MeltdownSirenBlockEntity> getBlockEntityClass() {
        return MeltdownSirenBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MeltdownSirenBlockEntity> getBlockEntityType() {
        return CMBlockEntities.MELTDOWN_SIREN.get();
    }
}
