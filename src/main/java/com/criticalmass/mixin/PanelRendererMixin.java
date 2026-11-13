package com.criticalmass.mixin;

import com.criticalmass.client.AlarmLook;
import com.criticalmass.content.control.AlarmPart;
import com.lycoris.loconautics.content.controls.panel.PanelBlockEntity;
import com.lycoris.loconautics.content.controls.panel.PanelRenderer;
import com.lycoris.loconautics.content.controls.panel.PanelSlot;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PanelRenderer.class, remap = false)
public abstract class PanelRendererMixin {
    @Shadow
    protected abstract void applyPartTransform(PoseStack ms, BlockState state);

    @Inject(method = "render(Lcom/lycoris/loconautics/content/controls/panel/PanelBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("TAIL"))
    private void criticalmass$alarms(PanelBlockEntity panel, float partialTick, PoseStack ms, MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        Level level = panel.getLevel();
        if (level == null) {
            return;
        }
        BlockState state = panel.getBlockState();
        for (PanelSlot slot : PanelSlot.values()) {
            if (!(panel.getPart(slot) instanceof AlarmPart alarm)) {
                continue;
            }
            ms.pushPose();
            applyPartTransform(ms, state);
            ms.translate((panel.isCentered() ? 4f : slot.modelX) / 16f, 0f, (panel.isCentered() ? 4f : slot.modelZ) / 16f);
            AlarmLook.render(alarm, level.getGameTime(), ms, buffers, light, state);
            ms.popPose();
        }
    }
}
