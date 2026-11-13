package com.criticalmass.content.control;

import com.criticalmass.content.CMShapes;
import com.criticalmass.registry.CMBlockEntities;
import com.criticalmass.registry.CMComponents;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DashboardBlock extends HorizontalDirectionalBlock implements IBE<DashboardBlockEntity>, IWrenchable {
    public static final MapCodec<DashboardBlock> CODEC = simpleCodec(DashboardBlock::new);
    public static final int MAX_RUN = 5;
    public static final BooleanProperty JOIN_CW = BooleanProperty.create("join_cw");
    public static final BooleanProperty JOIN_CCW = BooleanProperty.create("join_ccw");
    public static final double CELL_FRONT = 7 / 16.0;
    public static final double CELL_DEPTH = 8 / 16.0;
    private static final VoxelShape NORTH = Shapes.or(
            box(0, 0, 4, 16, 2, 16),
            box(0, 2, 8, 16, 8, 16),
            box(0, 8, 6, 16, 10.6, 8),
            box(0, 8, 8, 16, 11.4, 10),
            box(0, 8, 10, 16, 12.2, 12),
            box(0, 8, 12, 16, 13, 14),
            box(0, 8, 14, 16, 13.8, 15),
            box(0, 2, 15, 16, 16, 16));

    public DashboardBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(JOIN_CW, false).setValue(JOIN_CCW, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, JOIN_CW, JOIN_CCW);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CMShapes.rotated(NORTH, state.getValue(FACING));
    }

    private boolean matches(BlockState state, Direction facing) {
        return state.is(this) && state.getValue(FACING) == facing;
    }

    private int runLength(BlockGetter level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BlockPos at = pos;
        BlockState current = state;
        for (int i = 0; i < MAX_RUN && current.getValue(JOIN_CCW); i++) {
            BlockPos previous = at.relative(facing.getCounterClockWise());
            BlockState previousState = level.getBlockState(previous);
            if (!matches(previousState, facing) || !previousState.getValue(JOIN_CW)) {
                break;
            }
            at = previous;
            current = previousState;
        }
        int length = 1;
        while (length < MAX_RUN && current.getValue(JOIN_CW)) {
            at = at.relative(facing.getClockWise());
            current = level.getBlockState(at);
            if (!matches(current, facing) || !current.getValue(JOIN_CCW)) {
                break;
            }
            length++;
        }
        return length;
    }

    private int freeEnd(BlockGetter level, BlockPos pos, Direction facing, BooleanProperty towardUs) {
        BlockState state = level.getBlockState(pos);
        return matches(state, facing) && !state.getValue(towardUs) ? runLength(level, pos, state) : 0;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        int before = freeEnd(level, pos.relative(facing.getCounterClockWise()), facing, JOIN_CW);
        int after = freeEnd(level, pos.relative(facing.getClockWise()), facing, JOIN_CCW);
        boolean joinBefore = before > 0 && before < MAX_RUN;
        boolean joinAfter = after > 0 && (joinBefore ? before + 1 : 1) + after <= MAX_RUN;
        return defaultBlockState().setValue(FACING, facing).setValue(JOIN_CCW, joinBefore).setValue(JOIN_CW, joinAfter);
    }

    private void setJoin(Level level, BlockPos pos, Direction facing, BooleanProperty property, boolean joined) {
        BlockState neighbor = level.getBlockState(pos);
        if (matches(neighbor, facing) && neighbor.getValue(property) != joined) {
            level.setBlock(pos, neighbor.setValue(property, joined), Block.UPDATE_ALL);
        }
    }

    private void joinNeighbors(Level level, BlockPos pos, BlockState state, boolean joined) {
        Direction facing = state.getValue(FACING);
        if (state.getValue(JOIN_CCW)) {
            setJoin(level, pos.relative(facing.getCounterClockWise()), facing, JOIN_CW, joined);
        }
        if (state.getValue(JOIN_CW)) {
            setJoin(level, pos.relative(facing.getClockWise()), facing, JOIN_CCW, joined);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(this)) {
            joinNeighbors(level, pos, state, true);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            withBlockEntityDo(level, pos, DashboardBlockEntity::dropAll);
            if (!level.isClientSide) {
                joinNeighbors(level, pos, state, false);
            }
        }
        IBE.onRemove(state, level, pos, newState);
    }

    private static double along(Vec3 local, Direction axis) {
        double v = axis.getAxis() == Direction.Axis.X ? local.x : local.z;
        return axis.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 1 - v : v;
    }

    public static int slotAt(BlockState state, BlockPos pos, Vec3 hit) {
        Vec3 local = hit.subtract(Vec3.atLowerCornerOf(pos));
        return along(local, state.getValue(FACING).getClockWise()) < 0.5 ? 0 : 1;
    }

    public static double depthAt(BlockState state, BlockPos pos, Vec3 hit) {
        Vec3 local = hit.subtract(Vec3.atLowerCornerOf(pos));
        return (along(local, state.getValue(FACING).getOpposite()) - CELL_FRONT) / CELL_DEPTH;
    }

    private static void tell(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        DashboardBlockEntity be = getBlockEntity(level, pos);
        if (be == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int slot = slotAt(state, pos, hit.getLocation());
        if (AllItems.WRENCH.isIn(stack)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getItem() instanceof DashPartItem item && be.part(slot) == null) {
            if (!level.isClientSide) {
                DashPart part = new DashPart(item.type);
                BlockPos target = stack.get(CMComponents.LINK_TARGET.get());
                if (target != null && DashPart.accepts(item.type, level, target)) {
                    part.target = target;
                }
                be.install(slot, part);
                level.playSound(null, pos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 0.6f, 1.3f);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof PlantLinkerItem && be.part(slot) != null) {
            if (!level.isClientSide) {
                BlockPos stored = stack.get(CMComponents.LINK_TARGET.get());
                if (stored == null) {
                    tell(player, Component.translatable("criticalmass.linker.nothing_stored"));
                } else if (!be.bind(slot, stored)) {
                    tell(player, Component.translatable("criticalmass.dashboard.item.refused"));
                } else {
                    level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.4f, 1.6f);
                    tell(player, Component.translatable("criticalmass.linker.bound", level.getBlockState(stored).getBlock().getName(), stored.getX(), stored.getY(), stored.getZ()));
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        DashboardBlockEntity be = getBlockEntity(level, pos);
        int slot = slotAt(state, pos, hit.getLocation());
        DashPart part = be == null ? null : be.part(slot);
        if (part == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            String result = be.use(slot, depthAt(state, pos, hit.getLocation()));
            if (part.type == DashPart.Type.GAUGE && !part.readout.isEmpty()) {
                tell(player, Component.translatable("criticalmass.dashboard.readout", Component.translatable(be.modeKey(part)), part.readout));
            } else if (!result.isEmpty()) {
                tell(player, Component.translatable(result, part.level));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        DashboardBlockEntity be = getBlockEntity(level, pos);
        int slot = slotAt(state, pos, context.getClickLocation());
        if (be == null || be.part(slot) == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            String key = be.cycle(slot);
            if (context.getPlayer() != null && !key.isEmpty()) {
                tell(context.getPlayer(), Component.translatable(key));
            }
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3f, 1.2f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        DashboardBlockEntity be = getBlockEntity(level, pos);
        int slot = slotAt(state, pos, context.getClickLocation());
        if (be == null || be.part(slot) == null) {
            return IWrenchable.super.onSneakWrenched(state, context);
        }
        if (!level.isClientSide) {
            ItemStack stack = be.remove(slot);
            Player player = context.getPlayer();
            if (player != null && !player.isCreative()) {
                player.getInventory().placeItemBackInInventory(stack);
            }
            level.playSound(null, pos, SoundEvents.COPPER_BREAK, SoundSource.BLOCKS, 0.5f, 1.3f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        DashboardBlockEntity be = getBlockEntity(level, pos);
        if (be != null && player.pick(10.0, 0.0f, false) instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) {
            int slot = slotAt(state, pos, hit.getLocation());
            if (be.part(slot) != null) {
                if (!level.isClientSide) {
                    ItemStack stack = be.remove(slot);
                    if (!player.isCreative()) {
                        Block.popResource(level, pos, stack);
                    }
                }
                return false;
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        if (target instanceof BlockHitResult hit && level.getBlockEntity(pos) instanceof DashboardBlockEntity be) {
            DashPart part = be.part(slotAt(state, pos, hit.getLocation()));
            if (part != null) {
                return player != null && player.isShiftKeyDown() ? part.asStack() : new ItemStack(part.asStack().getItem());
            }
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof DashboardBlockEntity be ? be.signal() : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof DashboardBlockEntity be ? Math.min(15, be.activeAlarms()) : 0;
    }

    @Override
    public Class<DashboardBlockEntity> getBlockEntityClass() {
        return DashboardBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DashboardBlockEntity> getBlockEntityType() {
        return CMBlockEntities.DASHBOARD.get();
    }
}
