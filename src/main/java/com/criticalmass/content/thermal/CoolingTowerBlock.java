package com.criticalmass.content.thermal;

import com.criticalmass.content.PipeConnectable;
import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class CoolingTowerBlock extends KineticBlock implements IBE<CoolingTowerBlockEntity>, PipeConnectable {
    public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);

    public enum Shape implements StringRepresentable {
        SINGLE(false, false), BOTTOM(true, false), MIDDLE(true, true), TOP(false, true);

        private final boolean openTop;
        private final boolean openBottom;

        Shape(boolean openTop, boolean openBottom) {
            this.openTop = openTop;
            this.openBottom = openBottom;
        }

        public boolean openTop() {
            return openTop;
        }

        public boolean openBottom() {
            return openBottom;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public CoolingTowerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SHAPE, Shape.SINGLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE);
    }

    public static Shape shapeAt(BlockGetter level, BlockPos pos) {
        boolean above = level.getBlockState(pos.above()).getBlock() instanceof CoolingTowerBlock;
        boolean below = level.getBlockState(pos.below()).getBlock() instanceof CoolingTowerBlock;
        return above ? below ? Shape.MIDDLE : Shape.BOTTOM : below ? Shape.TOP : Shape.SINGLE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(SHAPE, shapeAt(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction.getAxis() == Direction.Axis.Y ? state.setValue(SHAPE, shapeAt(level, pos)) : state;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == Direction.Axis.Y;
    }

    @Override
    public boolean connectsOn(BlockState state, Direction face) {
        return true;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onBlockEntityUseItemOn(level, pos, be -> NodeBlocks.fill(level, pos, player, hand, be.coolant()));
    }

    @Override
    public Class<CoolingTowerBlockEntity> getBlockEntityClass() {
        return CoolingTowerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CoolingTowerBlockEntity> getBlockEntityType() {
        return CMBlockEntities.COOLING_TOWER.get();
    }
}
