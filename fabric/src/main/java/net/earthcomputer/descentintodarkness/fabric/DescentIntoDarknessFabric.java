package net.earthcomputer.descentintodarkness.fabric;

import net.fabricmc.api.ModInitializer;

import net.earthcomputer.descentintodarkness.DescentIntoDarkness;

public final class DescentIntoDarknessFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        DescentIntoDarkness.init();
        DescentIntoDarkness.postRegistryInit();
    }
}
