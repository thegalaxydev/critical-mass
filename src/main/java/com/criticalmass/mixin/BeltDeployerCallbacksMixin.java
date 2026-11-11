package com.criticalmass.mixin;

import com.criticalmass.content.fuel.WasteCanisterSequence;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.deployer.BeltDeployerCallbacks;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeltDeployerCallbacks.class)
public abstract class BeltDeployerCallbacksMixin {
    @Inject(method = "activate", at = @At("HEAD"))
    private static void criticalmass$noteDeployedItem(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler, DeployerBlockEntity blockEntity, Recipe<?> recipe, CallbackInfo ci) {
        WasteCanisterSequence.deploying(blockEntity.getPlayer() == null ? null : blockEntity.getPlayer().getMainHandItem());
    }

    @Inject(method = "activate", at = @At("RETURN"))
    private static void criticalmass$forgetDeployedItem(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler, DeployerBlockEntity blockEntity, Recipe<?> recipe, CallbackInfo ci) {
        WasteCanisterSequence.deployed();
    }
}
