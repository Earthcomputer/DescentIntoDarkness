package net.earthcomputer.descentintodarkness.fabric;

import net.fabricmc.api.ModInitializer;

import net.earthcomputer.descentintodarkness.DescentIntoDarkness;

public final class DescentIntoDarknessFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        DescentIntoDarkness.init();
    }
}
