package com.criticalmass.registry;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.fuel.FuelState;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CMComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CriticalMass.MODID);

    public static final Supplier<DataComponentType<FuelState>> FUEL_STATE = COMPONENTS.registerComponentType("fuel_state",
            b -> b.persistent(FuelState.CODEC).networkSynchronized(FuelState.STREAM_CODEC));

    private CMComponents() {
    }
}
