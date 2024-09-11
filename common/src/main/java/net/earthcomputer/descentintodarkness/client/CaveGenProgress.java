package net.earthcomputer.descentintodarkness.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class CaveGenProgress {
    private static final int[] COLORS = {0x00ff00, 0x8080ff, 0xff8080, 0x00ffff, 0xff80ff, 0xffff00};
    private static final int[] DARK_COLORS = {0x008000, 0x0000ff, 0xff0000, 0x008080, 0xff00ff, 0x808000};

    private CaveGenProgress() {
    }

    private static final List<Entry> entries = new ArrayList<>();

    public static void pushEntry(Component name) {
        entries.add(new Entry(name, 0));
    }

    public static void popEntry() {
        if (!entries.isEmpty()) {
            entries.removeLast();
        }
    }

    public static void setProgress(float progress) {
        if (!entries.isEmpty()) {
            entries.set(entries.size() - 1, new Entry(entries.getLast().name, progress));
        }
    }

    public static void clear() {
        entries.clear();
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Font font = Minecraft.getInstance().font;
        int center = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() - 20 - (font.lineHeight + 5) * entries.size();
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            int color = COLORS[i % COLORS.length];
            int darkColor = DARK_COLORS[i % COLORS.length];
            graphics.drawCenteredString(font, entry.name, center, y, color);
            y += font.lineHeight + 1;
            graphics.fill(center - 100, y, center + 100, y + 2, 0xff000000 | darkColor);
            graphics.fill(center - 100, y, center - 100 + (int) (entry.progress * 200), y + 2, 0xff000000 | color);
            y += 4;
        }
    }

    private record Entry(Component name, float progress) {
    }
}
