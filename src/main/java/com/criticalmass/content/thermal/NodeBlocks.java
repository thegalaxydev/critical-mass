package com.criticalmass.content.thermal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

public final class NodeBlocks {
    private NodeBlocks() {
    }

    public static ItemInteractionResult fill(Level level, BlockPos pos, Player player, InteractionHand hand, @Nullable CoolantBuffer buffer) {
        if (buffer == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return FluidUtil.interactWithFluidHandler(player, hand, buffer.handler(level, pos, null))
                ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public static boolean pipeOn(BlockGetter level, BlockPos pos, Direction face) {
        BlockPos at = pos.relative(face);
        BlockState s = level.getBlockState(at);
        return PipeCoolant.isPipe(s) && PipeCoolant.open(level, at, s, face.getOpposite());
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
