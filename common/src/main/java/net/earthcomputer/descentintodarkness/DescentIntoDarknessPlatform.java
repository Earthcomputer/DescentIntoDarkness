package net.earthcomputer.descentintodarkness;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

public final class DescentIntoDarknessPlatform {
    @ExpectPlatform
    public static void registerCustomDimension(MinecraftServer server, ResourceKey<Level> id) {
    }

    @ExpectPlatform
    public static void deleteCustomDimension(MinecraftServer server, ResourceKey<Level> id) {
    }
}
