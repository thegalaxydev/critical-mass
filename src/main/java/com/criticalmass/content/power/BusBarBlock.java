package com.criticalmass.content.power;

import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.foundation.block.IBE;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BusBarBlock extends RotatedPillarBlock implements IBE<BusBarBlockEntity> {
    private static final int SEARCH = 256;
    private static final VoxelShape FOOT = Block.box(6, 0, 6, 10, 6, 10);
    private static final VoxelShape ALONG_X = Shapes.or(Block.box(0, 6, 6, 16, 10, 10), FOOT);
    private static final VoxelShape ALONG_Y = Shapes.or(Block.box(6, 0, 6, 10, 16, 10), Block.box(6, 6, 0, 10, 10, 6));
    private static final VoxelShape ALONG_Z = Shapes.or(Block.box(6, 6, 0, 10, 10, 16), FOOT);

    public BusBarBlock(Properties properties) {
        super(properties);
    }

    private static boolean bar(Level level, BlockPos pos, Direction.Axis axis) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof BusBarBlock && state.getValue(AXIS) == axis;
    }

    public static List<BlockPos> run(Level level, BlockPos pos) {
        List<BlockPos> out = new ArrayList<>();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BusBarBlock)) {
            return out;
        }
        Direction.Axis axis = state.getValue(AXIS);
        Direction back = Direction.get(Direction.AxisDirection.NEGATIVE, axis);
        Direction forward = back.getOpposite();
        int behind = 0;
        while (behind < SEARCH && bar(level, pos.relative(back, behind + 1), axis)) {
            behind++;
        }
        BlockPos first = pos.relative(back, behind % Power.MAX_BUS);
        for (int i = 0; i < Power.MAX_BUS && bar(level, first.relative(forward, i), axis); i++) {
            out.add(first.relative(forward, i));
        }
        return out;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> ALONG_X;
            case Y -> ALONG_Y;
            case Z -> ALONG_Z;
        };
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        PowerNetwork.changed(level);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            WireItem.dropWires(level, pos);
        }
        IBE.onRemove(state, level, pos, newState);
        PowerNetwork.changed(level);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return getBlockEntityOptional(level, pos).map(be -> be.live() ? 15 : 0).orElse(0);
    }

    @Override
    public Class<BusBarBlockEntity> getBlockEntityClass() {
        return BusBarBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BusBarBlockEntity> getBlockEntityType() {
        return CMBlockEntities.BUS_BAR.get();
    }
}
