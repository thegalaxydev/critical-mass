package com.criticalmass.client;

import com.criticalmass.registry.CMCreativeTabs;
import com.criticalmass.registry.TabSection;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class TabBanners {
    private static final int COLUMNS = 9;
    private static final int ROWS_SHOWN = 5;
    private static final int LEFT = 8;
    private static final int TOP = 17;
    private static final List<Row> ROWS = new ArrayList<>();
    private static final Map<TabSection, Long> HOVERED_SINCE = new EnumMap<>(TabSection.class);

    private record Row(TabSection section, int row) {
    }

    private TabBanners() {
    }

    public static boolean ours(CreativeModeTab tab) {
        return tab == CMCreativeTabs.MAIN.get();
    }

    public static void pad(NonNullList<ItemStack> items) {
        Map<TabSection, List<ItemStack>> grouped = new EnumMap<>(TabSection.class);
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                grouped.computeIfAbsent(TabSection.of(stack.getItem()), s -> new ArrayList<>()).add(stack);
            }
        }
        items.clear();
        ROWS.clear();
        for (Map.Entry<TabSection, List<ItemStack>> group : grouped.entrySet()) {
            ROWS.add(new Row(group.getKey(), items.size() / COLUMNS));
            for (int i = 0; i < COLUMNS; i++) {
                items.add(ItemStack.EMPTY);
            }
            items.addAll(group.getValue());
            while (items.size() % COLUMNS != 0) {
                items.add(ItemStack.EMPTY);
            }
        }
    }

    public static void draw(GuiGraphics graphics, Font font, float scroll, int count, int mouseX, int mouseY) {
        int hidden = Mth.positiveCeilDiv(count, COLUMNS) - ROWS_SHOWN;
        int first = hidden <= 0 ? 0 : Math.max((int) (scroll * hidden + 0.5), 0);
        long now = Util.getMillis();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 150);
        for (Row row : ROWS) {
            int line = row.row() - first;
            TabSection section = row.section();
            if (line < 0 || line >= ROWS_SHOWN) {
                HOVERED_SINCE.remove(section);
                continue;
            }
            int y = TOP + line * TabSection.HEIGHT;
            boolean hovered = mouseX >= LEFT && mouseX < LEFT + TabSection.WIDTH && mouseY >= y && mouseY < y + TabSection.HEIGHT;
            int frame = 0;
            if (hovered) {
                long since = HOVERED_SINCE.computeIfAbsent(section, s -> now);
                frame = (int) ((now - since) / (50L * section.frameTicks) % section.frames);
            } else {
                HOVERED_SINCE.remove(section);
            }
            graphics.blit(section.texture(), LEFT, y, 0, frame * TabSection.HEIGHT, TabSection.WIDTH, TabSection.HEIGHT, TabSection.WIDTH, section.frames * TabSection.HEIGHT);
            graphics.drawString(font, Component.translatable(section.langKey()), LEFT + 6, y + 5, section.titleColour, true);
        }
        graphics.pose().popPose();
    }
}
