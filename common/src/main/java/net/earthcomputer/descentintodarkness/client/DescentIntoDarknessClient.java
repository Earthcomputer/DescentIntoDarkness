package net.earthcomputer.descentintodarkness.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.earthcomputer.descentintodarkness.resources.DIDBlocks;
import net.earthcomputer.descentintodarkness.resources.DIDResourceLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

public final class DescentIntoDarknessClient {
    private DescentIntoDarknessClient() {
    }

    public static void init() {
        ClientGuiEvent.RENDER_HUD.register(DescentIntoDarknessClient::renderHud);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> CaveGenProgress.clear());

        DIDNetworkClient.registerClient();

        registerBlockRenderTypes();
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

        CaveGenProgress.render(graphics, deltaTracker);
    }

    private static void registerBlockRenderTypes() {
        DIDBlocks.forEach((id, entry) -> {
            RenderType renderType = switch (entry.renderType()) {
                case SOLID -> RenderType.solid();
                case CUTOUT -> RenderType.cutout();
                case TRANSLUCENT -> RenderType.translucent();
            };
            Block block = BuiltInRegistries.BLOCK.get(DescentIntoDarkness.id(id));
            RenderTypeRegistry.register(renderType, block);
        });
    }
}
