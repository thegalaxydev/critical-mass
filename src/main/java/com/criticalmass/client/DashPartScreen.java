package com.criticalmass.client;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.control.DashPart;
import com.criticalmass.content.control.DashPartEdit;
import com.criticalmass.content.control.DashboardBlockEntity;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public class DashPartScreen extends AbstractSimiScreen {
    private static final ResourceLocation TEXTURE = CriticalMass.asResource("textures/gui/dashboard_part.png");
    private static final int SHEET_W = 512;
    private static final int SHEET_H = 256;
    private static final int PANEL_W = 339;
    private static final int PANEL_H = 193;
    private static final int VIEW_X = 34;
    private static final int VIEW_Y = 43;
    private static final int VIEW_SIZE = 92;
    private static final int STRIP_Y = 72;
    private static final int STRIP_W = 40;
    private static final int STRIP_H = 82;
    private static final int TOP_SLOT_Y = 86;
    private static final int MID_SLOT_Y = 112;
    private static final int BOTTOM_SLOT_Y = 138;
    private static final int ROW_X = 151;
    private static final int ROW_Y = 23;
    private static final int ROW_W = 172;
    private static final int ROW_H = 30;
    private static final int ROW_TEXT_Y = 34;
    private static final int TOGGLE_Y = 169;
    private static final int ARROW_Y = 84;
    private static final int ARROW_PREV_X = 20;
    private static final int ARROW_NEXT_X = 130;
    private static final int ARROW_SIZE = 10;
    private static final int DOTS_Y = 142;
    private static final int DOT_STEP = 8;
    private static final int LABEL_W = 200;
    private static final int LABEL_TEXT = 0xFFFFFFFF;
    private static final int LABEL_FIELD = 0xFF3A3A44;
    private static final int LABEL_Y = 2;
    private static final int LABEL_H = 12;
    private static final int OVERLAY_Z = 800;
    private static final int FLASH_MILLIS = 600;
    private static final String KEY = "criticalmass.dashboard.gui.";

    private record Column(String key, int count, ToIntFunction<DashPart> get, BiConsumer<DashPart, Integer> set) {
    }

    private final DashboardBlockEntity desk;
    private final BlockPos pos;
    private final int slot;
    private final DashPart preview;
    private final DashViewer viewer;
    private final List<Column> specs = new ArrayList<>();
    private final List<DashColumn> columns = new ArrayList<>();
    private final int modes;
    private EditBox label;
    private IconButton plate;
    private IconButton invert;

    public DashPartScreen(DashboardBlockEntity desk, int slot) {
        super(Component.translatable("item.criticalmass.dashboard_" + desk.part(slot).type.key));
        this.desk = desk;
        this.pos = desk.getBlockPos();
        this.slot = slot;
        this.preview = DashPart.load(desk.part(slot).save());
        this.viewer = new DashViewer(desk.getBlockState());
        this.modes = preview.modes(desk.getLevel());
        Column body = new Column(preview.type == DashPart.Type.GAUGE ? "bezel" : "body", DashPart.BODIES, p -> p.body, (p, v) -> p.body = v);
        Column color = new Column(switch (preview.type) {
            case BUTTON -> "cap";
            case GAUGE -> "needle";
            case ALARM -> "lamp";
            default -> "knob";
        }, DashPart.colors(preview.type), p -> p.color, (p, v) -> p.color = v);
        if (preview.type == DashPart.Type.GAUGE) {
            specs.add(new Column("face", DashPart.FACES, p -> p.face, (p, v) -> p.face = v));
        }
        specs.add(body);
        specs.add(color);
    }

    private int[] stripXs() {
        return specs.size() == 3 ? new int[]{165, 213, 261} : new int[]{185, 249};
    }

    @Override
    protected void init() {
        setWindowSize(PANEL_W, PANEL_H);
        super.init();
        columns.clear();
        int[] xs = stripXs();
        for (int i = 0; i < specs.size(); i++) {
            Column spec = specs.get(i);
            DashColumn column = new DashColumn(guiLeft + xs[i] + STRIP_W / 2, guiTop + TOP_SLOT_Y, guiTop + MID_SLOT_Y, guiTop + BOTTOM_SLOT_Y, spec.count(),
                    (g, index, cx, cy, size) -> {
                        DashPart variant = DashPart.load(preview.save());
                        spec.set().accept(variant, index);
                        viewer.icon(g, variant, animation(), true, cx, cy, size);
                    });
            column.select(spec.get().applyAsInt(preview));
            columns.add(column);
        }
        label = new EditBox(font, guiLeft + (PANEL_W - LABEL_W) / 2 + 4, guiTop + LABEL_Y + 2, LABEL_W - 8, 10, Component.translatable(KEY + "label"));
        label.setBordered(false);
        label.setMaxLength(DashPart.LABEL_LENGTH);
        label.setTextColor(LABEL_TEXT);
        label.setValue(preview.label);
        label.setHint(Component.translatable(KEY + "label.empty").withStyle(ChatFormatting.GRAY));
        addRenderableWidget(label);
        IconButton confirm = new IconButton(guiLeft + 314, guiTop + TOGGLE_Y, AllIcons.I_CONFIRM);
        confirm.withCallback(this::onClose);
        confirm.setToolTip(Component.translatable(KEY + "done"));
        addRenderableWidget(confirm);
        int toggleX = guiLeft + 5;
        if (preview.controls()) {
            invert = new IconButton(toggleX, guiTop + TOGGLE_Y, AllIcons.I_FLIP);
            invert.green = preview.inverted;
            invert.withCallback(() -> {
                preview.inverted = !preview.inverted;
                invert.green = preview.inverted;
            });
            invert.setToolTip(Component.translatable(KEY + "invert"));
            addRenderableWidget(invert);
            toggleX += 22;
        }
        if (preview.type != DashPart.Type.ALARM) {
            plate = new IconButton(toggleX, guiTop + TOGGLE_Y, AllIcons.I_FX_SURFACE_ON);
            plate.green = (preview.options & DashPart.PLATE) != 0;
            plate.withCallback(() -> {
                preview.options ^= DashPart.PLATE;
                plate.green = (preview.options & DashPart.PLATE) != 0;
            });
            plate.setToolTip(Component.translatable(KEY + "plate"));
            addRenderableWidget(plate);
        }
    }

    private float animation() {
        return switch (preview.type) {
            case LEVER -> 0.5f;
            case GAUGE -> 0.6f;
            default -> 0f;
        };
    }

    private boolean flash() {
        return preview.type == DashPart.Type.ALARM && Util.getMillis() / FLASH_MILLIS % 2 == 0;
    }

    private int styles() {
        return DashPart.styles(preview.type);
    }

    private void pull() {
        for (int i = 0; i < specs.size(); i++) {
            specs.get(i).set().accept(preview, columns.get(i).selected());
        }
    }

    private boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= guiLeft + x && mx < guiLeft + x + w && my >= guiTop + y && my < guiTop + y + h;
    }

    private void arrow(GuiGraphics g, int x, boolean next, boolean hovered) {
        int offset = hovered ? 12 : 0;
        g.blit(TEXTURE, guiLeft + x, guiTop + ARROW_Y, 352 + offset, 150, ARROW_SIZE, ARROW_SIZE, SHEET_W, SHEET_H);
        if (next) {
            g.blit(TEXTURE, guiLeft + x, guiTop + ARROW_Y, ARROW_SIZE, ARROW_SIZE, 386 + offset, 150, -ARROW_SIZE, ARROW_SIZE, SHEET_W, SHEET_H);
        } else {
            g.blit(TEXTURE, guiLeft + x, guiTop + ARROW_Y, 376 + offset, 150, ARROW_SIZE, ARROW_SIZE, SHEET_W, SHEET_H);
        }
    }

    private int dotsLeft() {
        return guiLeft + VIEW_X + (VIEW_SIZE - styles() * DOT_STEP) / 2;
    }

    @Override
    protected void renderWindow(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        pull();
        g.blit(TEXTURE, guiLeft, guiTop, 0, 0, PANEL_W, PANEL_H, SHEET_W, SHEET_H);
        g.fill(guiLeft + (PANEL_W - LABEL_W) / 2, guiTop + LABEL_Y, guiLeft + (PANEL_W + LABEL_W) / 2, guiTop + LABEL_Y + LABEL_H, LABEL_FIELD);
        if (modes > 1) {
            g.blit(TEXTURE, guiLeft + ROW_X, guiTop + ROW_Y, 340, 198, ROW_W, ROW_H, SHEET_W, SHEET_H);
            Component mode = Component.translatable(desk.modeKey(preview));
            g.drawString(font, mode, guiLeft + ROW_X + (ROW_W - font.width(mode)) / 2, guiTop + ROW_TEXT_Y, 0xFFFFFFFF, true);
        }
        for (int x : stripXs()) {
            g.blit(TEXTURE, guiLeft + x, guiTop + STRIP_Y, 352, 64, STRIP_W, STRIP_H, SHEET_W, SHEET_H);
        }
        viewer.render(g, preview, animation(), flash(), guiLeft + VIEW_X, guiTop + VIEW_Y, VIEW_SIZE, VIEW_SIZE);
        for (DashColumn column : columns) {
            column.render(g);
        }
        if (styles() > 1) {
            arrow(g, ARROW_PREV_X, false, over(mouseX, mouseY, ARROW_PREV_X, ARROW_Y, ARROW_SIZE, ARROW_SIZE));
            arrow(g, ARROW_NEXT_X, true, over(mouseX, mouseY, ARROW_NEXT_X, ARROW_Y, ARROW_SIZE, ARROW_SIZE));
        }
    }

    @Override
    protected void renderWindowForeground(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        g.pose().pushPose();
        g.pose().translate(0, 0, OVERLAY_Z);
        AllIcons.I_REFRESH.render(g, guiLeft + VIEW_X - 1, guiTop + VIEW_Y + VIEW_SIZE - 15);
        int zoom = viewer.zoom();
        g.drawCenteredString(font, zoom > 0 ? "+" + zoom : String.valueOf(zoom), guiLeft + VIEW_X + VIEW_SIZE - 7, guiTop + VIEW_Y + VIEW_SIZE - 9, 0xFFD0D0D8);
        if (styles() > 1) {
            int x0 = dotsLeft() + 2;
            for (int i = 0; i < styles(); i++) {
                int color = i == preview.style ? 0xFFFFFFFF : 0xFF5A5A68;
                int x = x0 + i * DOT_STEP;
                g.fill(x + 1, guiTop + DOTS_Y, x + 3, guiTop + DOTS_Y + 4, color);
                g.fill(x, guiTop + DOTS_Y + 1, x + 4, guiTop + DOTS_Y + 3, color);
            }
        }
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).hovered(mouseX, mouseY)) {
                g.renderComponentTooltip(font, List.of(Component.translatable(KEY + specs.get(i).key()), Component.translatable(KEY + "scroll_hint").withStyle(ChatFormatting.DARK_GRAY)), mouseX, mouseY);
            }
        }
        if (modes > 1 && over(mouseX, mouseY, ROW_X, ROW_Y, ROW_W, ROW_H)) {
            g.renderComponentTooltip(font, List.of(Component.translatable(KEY + "function." + preview.type.key), Component.translatable(KEY + "scroll_hint").withStyle(ChatFormatting.DARK_GRAY)), mouseX, mouseY);
        }
        if (styles() > 1 && (over(mouseX, mouseY, ARROW_PREV_X, ARROW_Y, ARROW_SIZE, ARROW_SIZE) || over(mouseX, mouseY, ARROW_NEXT_X, ARROW_Y, ARROW_SIZE, ARROW_SIZE))) {
            g.renderComponentTooltip(font, List.of(Component.translatable(KEY + "style")), mouseX, mouseY);
        }
        g.pose().popPose();
        super.renderWindowForeground(g, mouseX, mouseY, partialTicks);
    }

    private void stepStyle(int by) {
        preview.style = Math.floorMod(preview.style + by, styles());
    }

    private void stepMode(int by) {
        preview.mode = Math.floorMod(preview.mode + by, modes);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && over(mx, my, VIEW_X - 1, VIEW_Y + VIEW_SIZE - 15, 16, 16)) {
            viewer.reset();
            return true;
        }
        if (styles() > 1 && button == 0) {
            if (over(mx, my, ARROW_PREV_X, ARROW_Y, ARROW_SIZE, ARROW_SIZE)) {
                stepStyle(-1);
                return true;
            }
            if (over(mx, my, ARROW_NEXT_X, ARROW_Y, ARROW_SIZE, ARROW_SIZE)) {
                stepStyle(1);
                return true;
            }
        }
        if (modes > 1 && over(mx, my, ROW_X, ROW_Y, ROW_W, ROW_H)) {
            stepMode(button == 1 ? -1 : 1);
            return true;
        }
        for (DashColumn column : columns) {
            if (column.mouseClicked(mx, my)) {
                return true;
            }
        }
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        return viewer.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return viewer.mouseDragged(dx, dy) || super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        return viewer.mouseReleased() || super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (modes > 1 && scrollY != 0 && over(mx, my, ROW_X, ROW_Y, ROW_W, ROW_H)) {
            stepMode(scrollY > 0 ? -1 : 1);
            return true;
        }
        for (DashColumn column : columns) {
            if (column.mouseScrolled(mx, my, scrollY)) {
                return true;
            }
        }
        return viewer.mouseScrolled(mx, my, scrollY) || super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (label != null && label.isFocused() && keyCode != 256) {
            return label.keyPressed(keyCode, scanCode, modifiers) || true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        pull();
        PacketDistributor.sendToServer(new DashPartEdit(pos, slot, preview.body, preview.color, preview.style, preview.options, preview.face, preview.mode, preview.inverted,
                label == null ? preview.label : label.getValue()));
        super.onClose();
    }
}
