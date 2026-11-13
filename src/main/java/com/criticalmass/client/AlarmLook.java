package com.criticalmass.client;

import com.criticalmass.content.control.AlarmPart;
import com.lycoris.loconautics.content.controls.panel.PanelBodyRenderer;
import com.lycoris.loconautics.content.controls.panel.style.PanelStyle;
import com.lycoris.loconautics.content.controls.panel.style.PanelStyles;
import com.lycoris.loconautics.content.controls.panel.visual.ShiftedModels;
import com.lycoris.loconautics.content.controls.panelbutton.ButtonPartRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.api.model.Model;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.state.BlockState;

public final class AlarmLook {
    public static final int BODY = 3;
    public static final int LIT = 0;
    public static final int UNLIT = 1;
    private static final float SHEET = 64f;

    private AlarmLook() {
    }

    public static ButtonPartRenderer.CapStyle cap() {
        int index = 0;
        for (PanelStyle style : PanelStyles.BUTTON.all()) {
            if (style.kit() != null) {
                return ButtonPartRenderer.capStyle(index);
            }
            index++;
        }
        return ButtonPartRenderer.capStyle(0);
    }

    public static Model model(ButtonPartRenderer.CapStyle cap, int color) {
        return ShiftedModels.shifted(cap.cap(), cap.shift(), cap.strideX() * (color % 5), cap.strideY() * (color / 5));
    }

    public static void render(AlarmPart part, long gameTime, PoseStack ms, MultiBufferSource buffer, int light, BlockState state) {
        ButtonPartRenderer.CapStyle cap = cap();
        TextureAtlasSprite sheet = cap.shift().getTarget();
        float pixelU = (sheet.getU1() - sheet.getU0()) / SHEET;
        float pixelV = (sheet.getV1() - sheet.getV0()) / SHEET;
        boolean lit = part.lit(gameTime);
        int color = lit ? LIT : UNLIT;
        VertexConsumer cutout = buffer.getBuffer(RenderType.cutout());
        if (cap.big()) {
            PanelBodyRenderer.renderBigBody(ms, cutout, light, state, BODY);
        } else {
            PanelBodyRenderer.renderBody(ms, cutout, light, state, BODY);
        }
        CachedBuffers.partial(cap.cap(), state).light(lit ? LightTexture.FULL_BRIGHT : light)
                .shiftUVScrolling(cap.shift(), pixelU * cap.strideX() * (color % 5), pixelV * cap.strideY() * (color / 5))
                .renderInto(ms, cutout);
    }
}
