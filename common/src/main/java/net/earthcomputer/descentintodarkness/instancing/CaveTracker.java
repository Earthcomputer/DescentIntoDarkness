package net.earthcomputer.descentintodarkness.instancing;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Objects;

public final class CaveTracker {
    private final ResourceKey<Level> levelKey;

    CaveTracker(ResourceKey<Level> levelKey) {
        this.levelKey = levelKey;
    }

    boolean isValid(MinecraftServer server) {
        return server.getLevel(levelKey) != null;
    }

    public ServerLevel getLevel(MinecraftServer server) {
        return Objects.requireNonNull(server.getLevel(levelKey), "Tried to get level from invalid cave tracker");
    }
}
