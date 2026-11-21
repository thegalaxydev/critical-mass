package com.criticalmass.content.thermal;

import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMFluids;
import com.simibubi.create.api.effect.OpenPipeEffectHandler;
import com.simibubi.create.api.event.PipeCollisionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public final class LavaReaction {
    private LavaReaction() {
    }

    public static void register() {
        OpenPipeEffectHandler.REGISTRY.register(CMFluids.CONTAMINATED_COOLANT.source.get(), LavaReaction::spill);
        NeoForge.EVENT_BUS.addListener(LavaReaction::collide);
    }

    public static BlockState stone(RandomSource random, double share) {
        return random.nextDouble() < share ? CMBlocks.CHARTRINE.get().defaultBlockState() : Blocks.TUFF.defaultBlockState();
    }

    private static void spill(Level level, AABB area, FluidStack fluid) {
        if (level.isClientSide || fluid.isEmpty()) {
            return;
        }
        BlockPos lava = lavaIn(level, area);
        if (lava == null || level.random.nextDouble() * CMThermal.CHARTRINE_COOLANT >= fluid.getAmount()) {
            return;
        }
        double share = CoolantBuffer.flightShare(level, fluid);
        level.setBlockAndUpdate(lava, stone(level.random, share < 0 ? CMThermal.CLEAN_ACTIVITY : share));
        level.levelEvent(LevelEvent.LAVA_FIZZ, lava, 0);
    }

    @Nullable
    private static BlockPos lavaIn(Level level, AABB area) {
        int x = Mth.floor(area.minX + 0.5);
        int z = Mth.floor(area.minZ + 0.5);
        for (int y = Mth.floor(area.maxY - 0.5); y >= Mth.floor(area.minY + 0.5); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.getFluidState().is(FluidTags.LAVA)) {
                return pos;
            }
            if (!state.canBeReplaced()) {
                return null;
            }
        }
        return null;
    }

    private static void collide(PipeCollisionEvent.Flow event) {
        Fluid dirty = CMFluids.CONTAMINATED_COOLANT.source.get();
        Fluid first = event.getFirstFluid();
        Fluid second = event.getSecondFluid();
        if (first == Fluids.LAVA && second == dirty || first == dirty && second == Fluids.LAVA) {
            event.setState(stone(event.getLevel().random, CMThermal.CLEAN_ACTIVITY));
        }
    }
}
