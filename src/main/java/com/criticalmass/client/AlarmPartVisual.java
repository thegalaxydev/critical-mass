package com.criticalmass.client;

import com.criticalmass.content.control.AlarmPart;
import com.lycoris.loconautics.content.controls.panel.PanelBlockEntity;
import com.lycoris.loconautics.content.controls.panel.PanelPart;
import com.lycoris.loconautics.content.controls.panel.PanelSlot;
import com.lycoris.loconautics.content.controls.panel.visual.PanelModels;
import com.lycoris.loconautics.content.controls.panel.visual.PanelPartVisual;
import com.lycoris.loconautics.content.controls.panelbutton.ButtonPartRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.Level;

public final class AlarmPartVisual extends PanelPartVisual {
    private final TransformedInstance shell;
    private final TransformedInstance dark;
    private final TransformedInstance bright;
    private int shown = -1;

    public AlarmPartVisual(VisualizationContext context) {
        super(context);
        ButtonPartRenderer.CapStyle cap = AlarmLook.cap();
        shell = create(cap.big() ? PanelModels.bigBody(AlarmLook.BODY) : PanelModels.body(AlarmLook.BODY));
        dark = create(AlarmLook.model(cap, AlarmLook.UNLIT));
        bright = create(AlarmLook.model(cap, AlarmLook.LIT));
    }

    @Override
    public boolean matches(PanelPart part) {
        return part instanceof AlarmPart;
    }

    @Override
    public void update(PanelBlockEntity panel, PanelSlot slot, PoseStack ms, float partialTick, int light, boolean full) {
        if (full) {
            place(shell, ms, light);
        }
        Level level = panel.getLevel();
        int state = level != null && panel.getPart(slot) instanceof AlarmPart alarm && alarm.lit(level.getGameTime()) ? 1 : 0;
        if (!full && state == shown) {
            return;
        }
        shown = state;
        if (state == 1) {
            place(bright, ms, LightTexture.FULL_BRIGHT);
            dark.setZeroTransform().setChanged();
        } else {
            place(dark, ms, light);
            bright.setZeroTransform().setChanged();
        }
    }
}
