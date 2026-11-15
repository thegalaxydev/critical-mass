package com.criticalmass.client;

import com.criticalmass.content.kinetics.TurbineStructure;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;

public class TurbineStructureEffects implements IClientBlockExtensions {
    @Override
    public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
        return true;
    }

    @Override
    public boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) {
        if (target instanceof BlockHitResult hit && TurbineStructure.valid(level, hit.getBlockPos(), state)) {
            manager.crack(TurbineStructure.master(hit.getBlockPos(), state), hit.getDirection());
        }
        return true;
    }
}
