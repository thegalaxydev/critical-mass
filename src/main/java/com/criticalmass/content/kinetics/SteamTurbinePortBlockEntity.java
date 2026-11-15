package com.criticalmass.content.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public class SteamTurbinePortBlockEntity extends BlockEntity {
    public SteamTurbinePortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public IFluidHandler handler(@Nullable Direction side) {
        BlockState state = getBlockState();
        if (level == null || side == null || side != TurbineStructure.portFace(state) || !TurbineStructure.valid(level, worldPosition, state)) {
            return null;
        }
        return level.getBlockEntity(TurbineStructure.master(worldPosition, state)) instanceof SteamTurbineBlockEntity turbine
                ? turbine.coolant().handler(level, turbine.getBlockPos(), side) : null;
    }
}
