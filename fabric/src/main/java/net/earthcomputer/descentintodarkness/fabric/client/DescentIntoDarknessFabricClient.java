package net.earthcomputer.descentintodarkness.fabric.client;

import net.earthcomputer.descentintodarkness.client.DescentIntoDarknessClient;
import net.fabricmc.api.ClientModInitializer;

public final class DescentIntoDarknessFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DescentIntoDarknessClient.init();
    }
}
