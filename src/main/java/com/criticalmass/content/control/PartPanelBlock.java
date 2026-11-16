package com.criticalmass.content.control;

import com.criticalmass.client.DashScreens;
import com.criticalmass.registry.CMBlockEntities;
import com.criticalmass.registry.CMBlocks;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class PartPanelBlock extends FaceAttachedHorizontalDirectionalBlock implements IBE<DashboardBlockEntity>, IWrenchable {
    public static final MapCodec<PartPanelBlock> CODEC = simpleCodec(PartPanelBlock::new);
    public static final int SLOTS = 4;
    public static final double CELL = 0.5;
    private static final double CENTER_REACH = 0.18;
    private static final double[] PART = {1 / 16.0, 0, 1 / 16.0, 7 / 16.0, 3 / 16.0, 7 / 16.0};
    private static final double[] PLATE = {0, 0, 0, 1, 1 / 16.0, 1};
    private static final Map<Integer, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    public PartPanelBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACE, AttachFace.FLOOR).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING);
    }

    private static float turn(BlockState state) {
        return 180 - state.getValue(FACING).toYRot();
    }

    public static Vec3 toLocal(BlockState state, Vec3 inBlock) {
        Vec3 q = VecHelper.rotateCentered(inBlock, -turn(state), Direction.Axis.Y);
        return switch (state.getValue(FACE)) {
            case FLOOR -> q;
            case WALL -> new Vec3(q.x, 1 - q.z, q.y);
            case CEILING -> new Vec3(q.x, 1 - q.y, 1 - q.z);
        };
    }

    public static Vec3 toBlock(BlockState state, Vec3 local) {
        Vec3 q = switch (state.getValue(FACE)) {
            case FLOOR -> local;
            case WALL -> new Vec3(local.x, local.z, 1 - local.y);
            case CEILING -> new Vec3(local.x, 1 - local.y, 1 - local.z);
        };
        return VecHelper.rotateCentered(q, turn(state), Direction.Axis.Y);
    }

    public static double cellX(int slot, boolean centered) {
        return centered ? CELL / 2 : (slot % 2) * CELL;
    }

    public static double cellZ(int slot, boolean centered) {
        return centered ? CELL / 2 : (slot / 2) * CELL;
    }

    public static int slotAt(BlockState state, BlockPos pos, Vec3 hit, boolean centered) {
        if (centered) {
            return 0;
        }
        Vec3 local = toLocal(state, hit.subtract(Vec3.atLowerCornerOf(pos)));
        return (local.z < CELL ? 0 : 2) + (local.x < CELL ? 0 : 1);
    }

    public static double depthAt(BlockState state, BlockPos pos, Vec3 hit, int slot, boolean centered) {
        Vec3 local = toLocal(state, hit.subtract(Vec3.atLowerCornerOf(pos)));
        return (local.z - cellZ(slot, centered)) / CELL;
    }

    public static boolean aimedAtCenter(BlockState state, BlockPos pos, Vec3 hit) {
        Vec3 local = toLocal(state, hit.subtract(Vec3.atLowerCornerOf(pos)));
        double dx = local.x - 0.5;
        double dz = local.z - 0.5;
        return dx * dx + dz * dz < CENTER_REACH * CENTER_REACH;
    }

    private static VoxelShape box(BlockState state, double[] b, double offX, double offZ) {
        Vec3 a = toBlock(state, new Vec3(b[0] + offX, b[1], b[2] + offZ));
        Vec3 c = toBlock(state, new Vec3(b[3] + offX, b[4], b[5] + offZ));
        return Shapes.box(clamp(Math.min(a.x, c.x)), clamp(Math.min(a.y, c.y)), clamp(Math.min(a.z, c.z)), clamp(Math.max(a.x, c.x)), clamp(Math.max(a.y, c.y)), clamp(Math.max(a.z, c.z)));
    }

    private static double clamp(double v) {
        return Math.round(Math.max(0, Math.min(1, v)) * 1024) / 1024.0;
    }

    private static VoxelShape shape(BlockState state, int mask, boolean centered) {
        int key = state.getValue(FACE).ordinal() | state.getValue(FACING).get2DDataValue() << 2 | mask << 4 | (centered ? 1 << 8 : 0);
        return SHAPES.computeIfAbsent(key, k -> {
            VoxelShape out = Shapes.empty();
            for (int slot = 0; slot < SLOTS; slot++) {
                if ((mask & 1 << slot) != 0) {
                    out = Shapes.or(out, box(state, PART, cellX(slot, centered), cellZ(slot, centered)));
                }
            }
            return out.isEmpty() ? box(state, PLATE, 0, 0) : out;
        });
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof DashboardBlockEntity be) {
            int mask = 0;
            for (int slot = 0; slot < SLOTS; slot++) {
                if (be.part(slot) != null) {
                    mask |= 1 << slot;
                }
            }
            return shape(state, mask, be.centered());
        }
        return shape(state, 0, false);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    private static AttachFace attach(Direction face) {
        return face == Direction.UP ? AttachFace.FLOOR : face == Direction.DOWN ? AttachFace.CEILING : AttachFace.WALL;
    }

    public static InteractionResult mount(UseOnContext context, DashPart.Type type) {
        Level level = context.getLevel();
        Direction face = context.getClickedFace();
        BlockPos support = context.getClickedPos();
        BlockPos pos = support.relative(face);
        BlockState there = level.getBlockState(pos);
        Player player = context.getPlayer();
        PartPanelBlock block = CMBlocks.PART_PANEL.get();
        if (there.is(block)) {
            if (getConnectedDirection(there) != face || !(level.getBlockEntity(pos) instanceof DashboardBlockEntity be)) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            return add(level, pos, there, be, context.getClickLocation(), type, context.getItemInHand(), player) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        if (!there.canBeReplaced() || !level.getBlockState(support).isFaceSturdy(level, support, face)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Direction toward = player == null ? Direction.NORTH : player.getDirection().getOpposite();
        BlockState state = block.defaultBlockState().setValue(FACE, attach(face)).setValue(FACING, face.getAxis().isHorizontal() ? face : toward);
        level.setBlock(pos, state, Block.UPDATE_ALL);
        if (!(level.getBlockEntity(pos) instanceof DashboardBlockEntity be)) {
            return InteractionResult.FAIL;
        }
        Vec3 click = context.getClickLocation();
        boolean center = aimedAtCenter(state, pos, click);
        be.setCentered(center);
        DashboardBlock.fit(level, pos, be, slotAt(state, pos, click, center), type, context.getItemInHand(), player);
        return InteractionResult.SUCCESS;
    }

    private static boolean add(Level level, BlockPos pos, BlockState state, DashboardBlockEntity be, Vec3 click, DashPart.Type type, ItemStack stack, @Nullable Player player) {
        int slot = slotAt(state, pos, click, false);
        if (be.centered() || be.part(slot) != null) {
            slot = -1;
            for (int i = 0; i < SLOTS && slot < 0; i++) {
                int candidate = be.centered() ? (i + 1) % SLOTS : i;
                if (be.part(candidate) == null) {
                    slot = candidate;
                }
            }
        }
        if (slot < 0) {
            return false;
        }
        be.setCentered(false);
        DashboardBlock.fit(level, pos, be, slot, type, stack, player);
        return true;
    }

    private static void clearIfEmpty(Level level, BlockPos pos, DashboardBlockEntity be) {
        if (be.empty()) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        DashboardBlockEntity be = getBlockEntity(level, pos);
        if (be == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (AllItems.WRENCH.isIn(stack)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getItem() instanceof DashPartItem item) {
            if (!level.isClientSide && !add(level, pos, state, be, hit.getLocation(), item.type, stack, player)) {
                return ItemInteractionResult.FAIL;
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return DashboardBlock.linkPart(stack, level, pos, player, be, be.slotAt(hit.getLocation()));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        DashboardBlockEntity be = getBlockEntity(level, pos);
        return be == null ? InteractionResult.PASS : DashboardBlock.usePart(level, pos, player, be, be.slotAt(hit.getLocation()), be.depthAt(hit.getLocation()));
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        DashboardBlockEntity be = getBlockEntity(level, pos);
        if (be == null) {
            return InteractionResult.PASS;
        }
        int slot = be.slotAt(context.getClickLocation());
        if (be.part(slot) == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            DashScreens.open(pos, slot);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        DashboardBlockEntity be = getBlockEntity(level, pos);
        if (be == null) {
            return InteractionResult.PASS;
        }
        int slot = be.slotAt(context.getClickLocation());
        if (be.part(slot) == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ItemStack stack = be.remove(slot);
            Player player = context.getPlayer();
            if (player != null && !player.isCreative()) {
                player.getInventory().placeItemBackInInventory(stack);
            }
            level.playSound(null, pos, SoundEvents.COPPER_BREAK, SoundSource.BLOCKS, 0.5f, 1.3f);
            clearIfEmpty(level, pos, be);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        DashboardBlockEntity be = getBlockEntity(level, pos);
        if (be != null && player.pick(10.0, 0.0f, false) instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) {
            int slot = be.slotAt(hit.getLocation());
            if (be.part(slot) != null) {
                if (!level.isClientSide) {
                    ItemStack stack = be.remove(slot);
                    if (!player.isCreative()) {
                        Block.popResource(level, pos, stack);
                    }
                    clearIfEmpty(level, pos, be);
                }
                return false;
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        if (target instanceof BlockHitResult hit && level.getBlockEntity(pos) instanceof DashboardBlockEntity be) {
            DashPart part = be.part(be.slotAt(hit.getLocation()));
            if (part != null) {
                return player != null && player.isShiftKeyDown() ? part.asStack() : new ItemStack(part.asStack().getItem());
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            withBlockEntityDo(level, pos, DashboardBlockEntity::dropAll);
        }
        IBE.onRemove(state, level, pos, newState);
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
