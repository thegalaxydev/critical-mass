package com.criticalmass.mixin;

import com.criticalmass.content.radiation.Irradiation;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityNameMixin {
    @ModifyReturnValue(method = "getTypeName", at = @At("RETURN"))
    private Component criticalmass$irradiatedName(Component original) {
        return (Object) this instanceof Mob mob && Irradiation.flagged(mob) ? Component.translatable("entity.criticalmass.irradiated", original) : original;
    }
}
