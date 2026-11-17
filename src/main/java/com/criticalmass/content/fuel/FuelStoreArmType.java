package com.criticalmass.content.fuel;

import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class FuelStoreArmType extends ArmInteractionPointType {
    public static final Supplier<FuelStoreArmType> RACK = ServicePortArmType.TYPES.register("spent_fuel_rack", () -> new FuelStoreArmType(CMBlocks.SPENT_FUEL_RACK));
    public static final Supplier<FuelStoreArmType> CASK = ServicePortArmType.TYPES.register("shielded_cask", () -> new FuelStoreArmType(CMBlocks.SHIELDED_CASK));
    private final Supplier<? extends Block> block;

    public FuelStoreArmType(Supplier<? extends Block> block) {
        this.block = block;
    }

    public static void init() {
    }

    @Override
    public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
        return state.is(block.get());
    }

    @Override
    public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
        return new AllArmInteractionPointTypes.TopFaceArmInteractionPoint(this, level, pos, state);
    }
}
