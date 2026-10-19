package com.criticalmass.registry;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.fuel.FuelState;
import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CMComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CriticalMass.MODID);

    public static final Supplier<DataComponentType<FuelState>> FUEL_STATE = COMPONENTS.registerComponentType("fuel_state",
            b -> b.persistent(FuelState.CODEC).networkSynchronized(FuelState.STREAM_CODEC));

    public static final Supplier<DataComponentType<Integer>> RECOVERY_STEP = COMPONENTS.registerComponentType("recovery_step",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final Supplier<DataComponentType<Integer>> METER_MODE = COMPONENTS.registerComponentType("meter_mode",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final Supplier<DataComponentType<Double>> CONTAMINATION = COMPONENTS.registerComponentType("contamination",
            b -> b.persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE));

    private CMComponents() {
    }
}
