package com.criticalmass.content.thermal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public final class NodeBlocks {
    private NodeBlocks() {
    }

    public static ItemInteractionResult fill(Level level, BlockPos pos, Player player, InteractionHand hand, @Nullable CoolantBuffer buffer) {
        return buffer == null ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : fill(level, player, hand, buffer.handler(level, pos, null));
    }

    public static ItemInteractionResult fill(Level level, Player player, InteractionHand hand, IFluidHandler handler) {
        return FluidUtil.interactWithFluidHandler(player, hand, handler)
                ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public static boolean pipeOn(BlockGetter level, BlockPos pos, Direction face) {
        BlockPos at = pos.relative(face);
        BlockState s = level.getBlockState(at);
        return PipeCoolant.isPipe(s) && PipeCoolant.open(level, at, s, face.getOpposite());
    }

    public static boolean piped(LevelAccessor level, BlockPos pos, Direction face) {
        BlockPos at = pos.relative(face);
        BlockState state = level.getBlockState(at);
        if (PipeCoolant.isPipe(state)) {
            return PipeCoolant.open(level, at, state, face.getOpposite());
        }
        return level instanceof Level real && real.getCapability(Capabilities.FluidHandler.BLOCK, at, face.getOpposite()) != null;
    }

    @Nullable
    public static Direction otherPipeFace(BlockGetter level, BlockPos pos, Direction entered, Iterable<Direction> faces) {
        for (Direction d : faces) {
            if (d != entered && pipeOn(level, pos, d)) {
                return d;
            }
        }
        return null;
    }
}
