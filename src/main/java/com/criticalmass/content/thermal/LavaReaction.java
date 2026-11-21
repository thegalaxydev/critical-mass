package com.criticalmass.content.thermal;

import com.criticalmass.content.radiation.ContaminationData;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMFluids;
import com.simibubi.create.api.effect.OpenPipeEffectHandler;
import com.simibubi.create.api.event.PipeCollisionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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

    public static BlockPos mouth(AABB area) {
        return new BlockPos(Mth.floor(area.minX + 0.5), Mth.floor(area.maxY - 0.5), Mth.floor(area.minZ + 0.5));
    }

    private static void spill(Level level, AABB area, FluidStack fluid) {
        if (!(level instanceof ServerLevel server) || fluid.isEmpty()) {
            return;
        }
        CoolantBuffer.Packet arrived = CoolantBuffer.claim(level, fluid);
        BlockPos lava = lavaIn(level, area);
        if (lava == null) {
            ContaminationData.get(server).add(mouth(area), arrived.activity() * CMThermal.CONTAMINATED_SPILL + (fluid.getAmount() - arrived.activity()) * CMThermal.CLEAN_SPILL);
            return;
        }
        if (level.random.nextDouble() * CMThermal.CHARTRINE_COOLANT >= fluid.getAmount()) {
            return;
        }
        BlockState made = stone(level.random, arrived.proven() / fluid.getAmount());
        level.setBlockAndUpdate(lava, made);
        level.levelEvent(LevelEvent.LAVA_FIZZ, lava, 0);
        if (made.is(CMBlocks.CHARTRINE.get())) {
            com.criticalmass.registry.CMTriggers.near(level, lava, 16, "chartrine_grown");
        }
    }

    @Nullable
    private static BlockPos lavaIn(Level level, AABB area) {
        BlockPos top = mouth(area);
        for (int y = top.getY(); y >= Mth.floor(area.minY + 0.5); y--) {
            BlockPos pos = new BlockPos(top.getX(), y, top.getZ());
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
            event.setState(Blocks.TUFF.defaultBlockState());
        }
    }
}
