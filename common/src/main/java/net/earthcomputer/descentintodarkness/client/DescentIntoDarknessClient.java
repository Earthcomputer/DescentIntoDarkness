package net.earthcomputer.descentintodarkness.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import net.earthcomputer.descentintodarkness.resources.DIDResourceLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class DescentIntoDarknessClient {
    private DescentIntoDarknessClient() {
    }

    public static void init() {
        ClientGuiEvent.RENDER_HUD.register(DescentIntoDarknessClient::renderHud);
    }

    private static void renderHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Font font = Minecraft.getInstance().font;
        if (!Minecraft.getInstance().gui.getDebugOverlay().showDebugScreen()) {
            int y = 10;

            if (DIDResourceLoader.areDevelopmentResourcesDetected()) {
                graphics.drawString(font, "Development resources detected", 10, y, 0xff0000);
                y += font.lineHeight;
            }

            if (DIDResourceLoader.isRestartRequired()) {
                graphics.drawString(font, "Restart required", 10, y, 0xff0000);
                y += font.lineHeight;
            }
        }
    }
}
