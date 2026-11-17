package com.criticalmass.registry;

import com.criticalmass.CriticalMass;
import java.util.function.Supplier;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.core.registries.Registries;

public final class CMFluids {
    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, CriticalMass.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, CriticalMass.MODID);

    public static final Entry PRIMARY_COOLANT = entry("primary_coolant", FluidType.Properties.create().density(1100).viscosity(1200).temperature(300).canPushEntity(true).canSwim(false).canDrown(false));
    public static final Entry STEAM = entry("steam", FluidType.Properties.create().density(-500).viscosity(200).temperature(400).canPushEntity(false).canSwim(false).canDrown(false));
    public static final Entry CONTAMINATED_COOLANT = entry("contaminated_coolant", FluidType.Properties.create().density(1150).viscosity(1400).temperature(300).canPushEntity(true).canSwim(false).canDrown(false));
    public static final Entry FUEL_OIL = entry("fuel_oil", FluidType.Properties.create().density(900).viscosity(2500).temperature(300).canPushEntity(true).canSwim(false).canDrown(false));

    private CMFluids() {
    }

    private static Entry entry(String name, FluidType.Properties props) {
        Entry e = new Entry();
        e.type = TYPES.register(name, () -> new FluidType(props.descriptionId("fluid.criticalmass." + name)));
        e.source = FLUIDS.register(name, () -> new BaseFlowingFluid.Source(e.properties()));
        e.flowing = FLUIDS.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(e.properties()));
        e.name = name;
        return e;
    }

    public static final class Entry {
        public String name;
        public DeferredHolder<FluidType, FluidType> type;
        public DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
        public DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;
        private BaseFlowingFluid.Properties props;

        BaseFlowingFluid.Properties properties() {
            if (props == null) {
                Supplier<FluidType> t = type;
                props = new BaseFlowingFluid.Properties(t, source, flowing);
            }
            return props;
        }
    }
}
