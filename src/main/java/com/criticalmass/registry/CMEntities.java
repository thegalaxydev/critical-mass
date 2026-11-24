package com.criticalmass.registry;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.radiation.IrradiatedCreeper;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CMEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, CriticalMass.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<IrradiatedCreeper>> IRRADIATED_CREEPER = ENTITIES.register("irradiated_creeper",
            () -> EntityType.Builder.<IrradiatedCreeper>of(IrradiatedCreeper::new, MobCategory.MONSTER).sized(0.75F, 1.95F).clientTrackingRange(8).build("irradiated_creeper"));

    private CMEntities() {
    }

    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(IRRADIATED_CREEPER.get(), IrradiatedCreeper.createAttributes().build());
    }
}
