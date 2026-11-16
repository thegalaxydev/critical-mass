package com.criticalmass.content.control;

import com.criticalmass.content.CMShapes;
import com.criticalmass.registry.CMBlockEntities;
import com.criticalmass.registry.CMComponents;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import com.criticalmass.client.DashScreens;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class DashboardBlock extends HorizontalDirectionalBlock implements IBE<DashboardBlockEntity>, IWrenchable {
    public static final MapCodec<DashboardBlock> CODEC = simpleCodec(DashboardBlock::new);
    public static final int MAX_RUN = 5;
    public static final BooleanProperty JOIN_CW = BooleanProperty.create("join_cw");
    public static final BooleanProperty JOIN_CCW = BooleanProperty.create("join_ccw");
    public static final EnumProperty<Skin> SKIN = EnumProperty.create("skin", Skin.class);

    public enum Skin implements StringRepresentable {
        ALLOY, WHITE, ORANGE, MAGENTA, LIGHT_BLUE, YELLOW, LIME, PINK, GRAY, LIGHT_GRAY, CYAN, PURPLE, BLUE, BROWN, GREEN, RED, BLACK;

        public final String key = name().toLowerCase(Locale.ROOT);

        public static Skin of(DyeColor color) {
            return values()[color.getId() + 1];
        }

        public static Skin byKey(String key) {
            for (Skin skin : values()) {
                if (skin.key.equals(key)) {
                    return skin;
                }
            }
            return ALLOY;
        }

        @Override
        public String getSerializedName() {
            return key;
        }
    }
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
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(JOIN_CW, false).setValue(JOIN_CCW, false).setValue(SKIN, Skin.ALLOY));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, JOIN_CW, JOIN_CCW, SKIN);
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

    private boolean dye(Level level, BlockPos pos, BlockState state, Skin skin) {
        Direction facing = state.getValue(FACING);
        BlockPos at = pos;
        BlockState current = state;
        for (int i = 0; i < MAX_RUN && current.getValue(JOIN_CCW); i++) {
            BlockPos previous = at.relative(facing.getCounterClockWise());
            BlockState previousState = level.getBlockState(previous);
            if (!matches(previousState, facing)) {
                break;
            }
            at = previous;
            current = previousState;
        }
        boolean changed = false;
        for (int i = 0; i < MAX_RUN; i++) {
            if (current.getValue(SKIN) != skin) {
                level.setBlock(at, current.setValue(SKIN, skin), Block.UPDATE_ALL);
                changed = true;
            }
            if (!current.getValue(JOIN_CW)) {
                break;
            }
            at = at.relative(facing.getClockWise());
            current = level.getBlockState(at);
            if (!matches(current, facing)) {
                break;
            }
        }
        return changed;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("criticalmass.dashboard.credit").withStyle(ChatFormatting.DARK_GRAY));
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
        if (stack.getItem() instanceof DyeItem dyeItem) {
            Skin skin = Skin.of(dyeItem.getDyeColor());
            if (!LoconauticsCompat.loaded() || state.getValue(SKIN) == skin) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide && dye(level, pos, state, skin)) {
                level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof DashPartItem item && be.part(slot) == null) {
            if (!level.isClientSide) {
                fit(level, pos, be, slot, item.type, stack, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return linkPart(stack, level, pos, player, be, slot);
    }

    public static ItemInteractionResult linkPart(ItemStack stack, Level level, BlockPos pos, Player player, DashboardBlockEntity be, int slot) {
        DashPart fitted = be.part(slot);
        if (fitted != null && stack.is(Items.NAME_TAG) && stack.has(DataComponents.CUSTOM_NAME)) {
            if (!level.isClientSide) {
                be.relabel(slot, stack.get(DataComponents.CUSTOM_NAME).getString());
                tell(player, Component.translatable("criticalmass.dashboard.labelled", fitted.labelText()));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (fitted != null && (stack.getItem() instanceof PlantLinkerItem || spare(stack, fitted))) {
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
        return be == null ? InteractionResult.PASS : usePart(level, pos, player, be, slotAt(state, pos, hit.getLocation()), depthAt(state, pos, hit.getLocation()));
    }

    public static InteractionResult usePart(Level level, BlockPos pos, Player player, DashboardBlockEntity be, int slot, double depth) {
        DashPart part = be.part(slot);
        if (part == null) {
            return InteractionResult.PASS;
        }
        if (part.glass()) {
            if (player.isShiftKeyDown()) {
                if (!level.isClientSide) {
                    be.toggleGlass(slot);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!part.glassOpen) {
                return InteractionResult.CONSUME;
            }
        }
        if (part.type == DashPart.Type.LEVER && !(player instanceof FakePlayer)) {
            if (level.isClientSide) {
                DashScreens.grabLever(pos, slot, part.level, part.inverted);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            String result = be.use(slot, depth);
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
            IWrenchable.playRemoveSound(level, pos);
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
                return player != null && player.isShiftKeyDown() ? part.asConfiguredStack() : new ItemStack(part.asStack().getItem());
            }
        }
        return desk(state);
    }

    public static boolean spare(ItemStack stack, @Nullable DashPart fitted) {
        return fitted != null && stack.getItem() instanceof DashPartItem item && item.type == fitted.type && stack.has(CMComponents.LINK_TARGET.get());
    }

    public static void fit(Level level, BlockPos pos, DashboardBlockEntity be, int slot, DashPart.Type type, ItemStack stack, @Nullable Player player) {
        DashPart part = new DashPart(type);
        BlockPos target = stack.get(CMComponents.LINK_TARGET.get());
        if (target != null && DashPart.accepts(type, level, target)) {
            part.target = target;
        }
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        if (name != null) {
            part.label = name.getString();
        }
        part.configure(stack);
        be.install(slot, part);
        SoundType sound = level.getBlockState(pos).getSoundType();
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8f);
        if (type == DashPart.Type.GAUGE && player instanceof ServerPlayer opener) {
            PacketDistributor.sendToPlayer(opener, new DashOpenSettings(pos, slot));
        }
        if (player == null || !player.isCreative()) {
            stack.shrink(1);
        }
    }

    private ItemStack desk(BlockState state) {
        Item skinned = LoconauticsCompat.deskItem(state.getValue(SKIN));
        return new ItemStack(skinned == null ? asItem() : skinned);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
        Item skinned = LoconauticsCompat.deskItem(state.getValue(SKIN));
        if (skinned != null) {
            drops.replaceAll(stack -> stack.is(asItem()) ? new ItemStack(skinned, stack.getCount()) : stack);
        }
        return drops;
    }

    public boolean adopt(Level level, BlockPos pos, @Nullable Player player) {
        BlockState first = level.getBlockState(pos);
        if (!LoconauticsCompat.theirDesk(first)) {
            return false;
        }
        Direction facing = LoconauticsCompat.facing(first);
        List<BlockPos> run = new ArrayList<>();
        run.add(pos);
        for (boolean clockwise : new boolean[]{true, false}) {
            BlockPos at = pos;
            BlockState current = first;
            for (int i = 0; i < MAX_RUN && LoconauticsCompat.joined(current, clockwise); i++) {
                at = at.relative(clockwise ? facing.getClockWise() : facing.getCounterClockWise());
                current = level.getBlockState(at);
                if (!LoconauticsCompat.theirDesk(current) || LoconauticsCompat.facing(current) != facing || run.contains(at)) {
                    break;
                }
                run.add(at);
            }
        }
        List<BlockState> wanted = new ArrayList<>();
        for (BlockPos at : run) {
            BlockState theirs = level.getBlockState(at);
            if (LoconauticsCompat.holdsParts(level, at)) {
                if (player != null) {
                    tell(player, Component.translatable("criticalmass.dashboard.adopt_refused"));
                }
                return false;
            }
            wanted.add(defaultBlockState().setValue(FACING, facing).setValue(JOIN_CW, LoconauticsCompat.joined(theirs, true))
                    .setValue(JOIN_CCW, LoconauticsCompat.joined(theirs, false)).setValue(SKIN, LoconauticsCompat.skin(theirs)));
        }
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < run.size(); i++) {
                level.setBlock(run.get(i), wanted.get(i), Block.UPDATE_CLIENTS);
            }
        }
        return true;
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
