package com.criticalmass.content.control;

import com.criticalmass.registry.CMBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.equipment.goggles.IProxyHoveringInformation;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ConsoleBlock extends HorizontalDirectionalBlock implements EntityBlock, IWrenchable, IProxyHoveringInformation {
    public static final MapCodec<ConsoleBlock> CODEC = simpleCodec(ConsoleBlock::new);
    public static final int COLUMNS = 5;
    public static final int CENTRE = 2;
    public static final IntegerProperty COLUMN = IntegerProperty.create("column", 0, COLUMNS - 1);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final double SLOPE = Math.tan(Math.toRadians(22.5));
    public static final double LIP = 1.2;
    private static final VoxelShape[][] SHAPES = new VoxelShape[4][4];

    public enum Part implements StringRepresentable {
        FRONT_BOTTOM(false, false),
        BACK_BOTTOM(true, false),
        FRONT_TOP(false, true),
        BACK_TOP(true, true);

        public final boolean back;
        public final boolean top;

        Part(boolean back, boolean top) {
            this.back = back;
            this.top = top;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    static {
        for (Part part : Part.values()) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                SHAPES[part.ordinal()][facing.get2DDataValue()] = shape(part, facing);
            }
        }
    }

    public ConsoleBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(COLUMN, CENTRE).setValue(PART, Part.FRONT_BOTTOM));
    }

    private static VoxelShape turned(Direction facing, double x1, double y1, double z1, double x2, double y2, double z2) {
        return switch (facing) {
            case SOUTH -> Block.box(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1);
            case EAST -> Block.box(16 - z2, y1, x1, 16 - z1, y2, x2);
            case WEST -> Block.box(z1, y1, 16 - x2, z2, y2, 16 - x1);
            default -> Block.box(x1, y1, z1, x2, y2, z2);
        };
    }

    private static VoxelShape shape(Part part, Direction facing) {
        if (!part.top) {
            return Shapes.block();
        }
        double start = part.back ? 16 : 0;
        VoxelShape out = Shapes.empty();
        for (int i = 0; i < 4; i++) {
            double height = Math.min(16, LIP + (start + i * 4 + 2) * SLOPE);
            out = Shapes.or(out, turned(facing, 0, 0, i * 4, 16, height, i * 4 + 4));
        }
        return out;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, COLUMN, PART);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(PART).ordinal()][state.getValue(FACING).get2DDataValue()];
    }

    public static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    public static BlockPos partPos(BlockPos master, Direction facing, int column, Part part) {
        BlockPos p = master.relative(right(facing), column - CENTRE);
        if (part.back) {
            p = p.relative(facing.getOpposite());
        }
        return part.top ? p.above() : p;
    }

    public static BlockPos masterPos(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        Part part = state.getValue(PART);
        BlockPos p = pos.relative(right(facing), CENTRE - state.getValue(COLUMN));
        if (part.back) {
            p = p.relative(facing);
        }
        return part.top ? p.below() : p;
    }

    public static boolean isMaster(BlockState state) {
        return state.getValue(COLUMN) == CENTRE && state.getValue(PART) == Part.FRONT_BOTTOM;
    }

    public static List<BlockPos> positions(BlockPos master, Direction facing) {
        List<BlockPos> out = new ArrayList<>();
        for (int column = 0; column < COLUMNS; column++) {
            for (Part part : Part.values()) {
                out.add(partPos(master, facing, column, part));
            }
        }
        return out;
    }

    @Nullable
    public static ConsoleBlockEntity console(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ConsoleBlock)) {
            return null;
        }
        return level.getBlockEntity(masterPos(pos, state)) instanceof ConsoleBlockEntity c ? c : null;
    }

    public static boolean fits(Level level, BlockPos master, Direction facing, BlockPlaceContext context) {
        for (BlockPos p : positions(master, facing)) {
            if (level.isOutsideBuildHeight(p) || !level.getBlockState(p).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!fits(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide || !isMaster(state) || oldState.getBlock() == this) {
            return;
        }
        Direction facing = state.getValue(FACING);
        for (int column = 0; column < COLUMNS; column++) {
            for (Part part : Part.values()) {
                BlockPos p = partPos(pos, facing, column, part);
                if (!p.equals(pos)) {
                    level.setBlock(p, state.setValue(COLUMN, column).setValue(PART, part), Block.UPDATE_ALL);
                }
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() == newState.getBlock()) {
            return;
        }
        BlockPos master = masterPos(pos, state);
        Direction facing = state.getValue(FACING);
        if (!level.isClientSide) {
            if (!isMaster(state)) {
                BlockState at = level.getBlockState(master);
                if (at.getBlock() == this && isMaster(at) && at.getValue(FACING) == facing) {
                    level.destroyBlock(master, true);
                }
            } else {
                for (BlockPos p : positions(pos, facing)) {
                    BlockState at = level.getBlockState(p);
                    if (!p.equals(pos) && at.getBlock() == this && at.getValue(FACING) == facing && masterPos(p, at).equals(pos)) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && !isMaster(state)) {
            BlockPos master = masterPos(pos, state);
            BlockState at = level.getBlockState(master);
            if (at.getBlock() == this && isMaster(at)) {
                level.setBlock(master, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        ConsoleBlockEntity console = console(level, pos);
        if (console == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            console.operate(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos master = masterPos(context.getClickedPos(), state);
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!turn(level, master)) {
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.translatable("criticalmass.console.no_turn"), true);
            }
            return InteractionResult.FAIL;
        }
        IWrenchable.playRotateSound(level, master);
        return InteractionResult.SUCCESS;
    }

    public boolean turn(Level level, BlockPos master) {
        BlockState at = level.getBlockState(master);
        if (at.getBlock() != this || !isMaster(at) || !(level.getBlockEntity(master) instanceof ConsoleBlockEntity console)) {
            return false;
        }
        Direction from = at.getValue(FACING);
        Direction to = from.getClockWise();
        List<BlockPos> old = positions(master, from);
        for (BlockPos p : positions(master, to)) {
            if (level.isOutsideBuildHeight(p) || !old.contains(p) && !level.getBlockState(p).canBeReplaced()) {
                return false;
            }
        }
        CompoundTag saved = console.saveWithoutMetadata(level.registryAccess());
        level.setBlock(master, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        level.setBlock(master, at.setValue(FACING, to), Block.UPDATE_ALL);
        if (level.getBlockEntity(master) instanceof ConsoleBlockEntity turned) {
            turned.loadWithComponents(saved, level.registryAccess());
            turned.turned();
        }
        return true;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos master = masterPos(context.getClickedPos(), state);
        BlockState at = level.getBlockState(master);
        if (at.getBlock() != this) {
            return InteractionResult.PASS;
        }
        return IWrenchable.super.onSneakWrenched(at, new UseOnContext(level, context.getPlayer(), context.getHand(), context.getItemInHand(),
                new BlockHitResult(context.getClickLocation(), context.getClickedFace(), master, context.isInside())));
    }

    @Override
    public BlockPos getInformationSource(Level level, BlockPos pos, BlockState state) {
        return masterPos(pos, state);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return state.getValue(PART).back;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        Part part = state.getValue(PART);
        if (!part.back) {
            return 0;
        }
        ConsoleBlockEntity console = console(level, pos);
        return console != null && console.output(state.getValue(COLUMN), part.top) ? 15 : 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isMaster(state) ? new ConsoleBlockEntity(CMBlockEntities.CONTROL_CONSOLE.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == CMBlockEntities.CONTROL_CONSOLE.get() && isMaster(state) ? (l, p, s, be) -> ((ConsoleBlockEntity) be).tick() : null;
    }
}
