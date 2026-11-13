package com.criticalmass.content.kinetics;

import com.criticalmass.content.CMShapes;
import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CirculationPumpBlock extends DirectionalAxisKineticBlock implements IBE<CirculationPumpBlockEntity> {
    private static final VoxelShape NORTH = Shapes.or(
            box(2, 2, 4, 14, 14, 12),
            box(5, 5, 2, 11, 11, 14),
            box(4, 4, 0, 12, 12, 2),
            box(4, 4, 14, 12, 12, 16));

    public CirculationPumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Direction getFacingForPlacement(BlockPlaceContext context) {
        Direction facing = super.getFacingForPlacement(context);
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return facing;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Direction d : Direction.values()) {
            BlockPos other = pos.relative(d);
            if (!FluidPipeBlock.canConnectTo(level, other, level.getBlockState(other), d)) {
                continue;
            }
            double distance = Vec3.atLowerCornerOf(d.getNormal()).distanceTo(Vec3.atLowerCornerOf(facing.getNormal()));
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = d;
            }
        }
        return best != null && best.getAxis() != facing.getAxis() ? best : facing;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == getRotationAxis(state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CMShapes.rotated(NORTH, state.getValue(FACING));
    }

    @Override
    public Class<CirculationPumpBlockEntity> getBlockEntityClass() {
        return CirculationPumpBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CirculationPumpBlockEntity> getBlockEntityType() {
        return CMBlockEntities.CIRCULATION_PUMP.get();
    }
}
