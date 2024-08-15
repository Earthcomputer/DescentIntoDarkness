package net.earthcomputer.descentintodarkness.instancing;

import dev.architectury.event.events.common.LifecycleEvent;
import net.earthcomputer.descentintodarkness.DescentIntoDarknessPlatform;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class CaveTrackerManager {
    private static final Map<ResourceKey<Level>, CaveTracker> caves = new HashMap<>();

    public static void registerEvents() {
        LifecycleEvent.SERVER_STOPPING.register(CaveTrackerManager::cleanup);
    }

    private static void cleanup(MinecraftServer server) {
        for (ResourceKey<Level> levelKey : caves.keySet()) {
            DescentIntoDarknessPlatform.deleteCustomDimension(server, levelKey);
        }
        caves.clear();
    }

    @Nullable
    public static CaveTracker getCave(MinecraftServer server, ResourceKey<Level> levelKey) {
        CaveTracker tracker = caves.get(levelKey);
        if (tracker == null) {
            return null;
        }
        if (!tracker.isValid(server)) {
            caves.remove(levelKey);
            return null;
        }
        return tracker;
    }

    public static CaveTracker createCave(MinecraftServer server, ResourceKey<Level> levelKey) {
        CaveTracker tracker = new CaveTracker(levelKey);
        CaveTracker oldTracker = caves.put(levelKey, tracker);
        if (oldTracker != null) {
            throw new IllegalStateException("Duplicate cave tracker: " + levelKey);
        }
        DescentIntoDarknessPlatform.registerCustomDimension(server, levelKey);
        return tracker;
    }

    public static void deleteCave(MinecraftServer server, ResourceKey<Level> levelKey) {
        CaveTracker tracker = caves.remove(levelKey);
        if (tracker != null) {
            DescentIntoDarknessPlatform.deleteCustomDimension(server, levelKey);
        }
    }
}
