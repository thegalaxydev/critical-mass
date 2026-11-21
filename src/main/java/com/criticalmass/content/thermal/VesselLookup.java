package com.criticalmass.content.thermal;

import com.criticalmass.registry.CMBlocks;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class VesselLookup {
    private VesselLookup() {
    }

    public static boolean isVesselPart(BlockState state) {
        Block b = state.getBlock();
        return b instanceof VesselCasingBlock || b == CMBlocks.REACTOR_CONTROLLER.get() || b == CMBlocks.PRIMARY_COOLANT_PORT.get()
                || b == CMBlocks.REACTOR_SERVICE_PORT.get() || b == CMBlocks.FUEL_CHANNEL_CAP.get() || b == CMBlocks.FUEL_CHANNEL.get() || b == CMBlocks.CONTROL_CHANNEL.get()
                || b == CMBlocks.COOLANT_CHANNEL.get();
    }

    public static Result findController(Level level, BlockPos start) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(start);
        seen.add(start);
        BlockPos found = null;
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!level.isLoaded(pos)) {
                return Result.error("criticalmass.circuit.unloaded", pos);
            }
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() == CMBlocks.REACTOR_CONTROLLER.get()) {
                if (found != null) {
                    return Result.error("criticalmass.circuit.two_controllers", pos);
                }
                found = pos;
            }
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                if (seen.contains(next) || !isVesselPart(level.getBlockState(next))) {
                    continue;
                }
                if (seen.size() >= CMThermal.MAX_VESSEL_BLOCKS) {
                    return Result.error("criticalmass.circuit.vessel_too_large", start);
                }
                seen.add(next);
                queue.add(next);
            }
        }
        return found == null ? Result.error("criticalmass.circuit.no_controller", start) : new Result(found, null, null);
    }

    public record Result(BlockPos controller, String error, BlockPos at) {
        static Result error(String key, BlockPos at) {
            return new Result(null, key, at);
        }

        public boolean ok() {
            return controller != null;
        }
    }
}
