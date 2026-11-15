package com.criticalmass.content.fuel;

import com.criticalmass.CriticalMass;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ServicePortArmType extends ArmInteractionPointType {
    public static final DeferredRegister<ArmInteractionPointType> TYPES = DeferredRegister.create(CreateRegistries.ARM_INTERACTION_POINT_TYPE, CriticalMass.MODID);
    public static final Supplier<ServicePortArmType> SERVICE_PORT = TYPES.register("service_port", ServicePortArmType::new);

    @Override
    public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
        return state.is(CMBlocks.REACTOR_SERVICE_PORT.get());
    }

    @Override
    public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
        return new AllArmInteractionPointTypes.TopFaceArmInteractionPoint(this, level, pos, state);
    }
}
