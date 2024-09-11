package net.earthcomputer.descentintodarkness.instancing;

import dev.architectury.event.events.common.LifecycleEvent;
import net.earthcomputer.descentintodarkness.DIDPlatform;
import net.earthcomputer.descentintodarkness.generator.CaveGenProgressListener;
import net.earthcomputer.descentintodarkness.generator.DIDChunkGenerator;
import net.earthcomputer.descentintodarkness.style.CaveStyle;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class CaveTrackerManager {
    private static final Map<ResourceKey<Level>, CaveTracker> caves = new HashMap<>();

    public static void registerEvents() {
        LifecycleEvent.SERVER_STOPPING.register(CaveTrackerManager::cleanup);
    }

    private static void cleanup(MinecraftServer server) {
        for (ResourceKey<Level> levelKey : caves.keySet()) {
            DIDPlatform.deleteCustomDimension(server, levelKey);
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

    public static CaveTracker createCave(MinecraftServer server, ResourceKey<Level> levelKey, Holder<CaveStyle> style, int size, long seed, boolean debug, CaveGenProgressListener listener) {
        CaveTracker tracker = new CaveTracker(levelKey);
        CaveTracker oldTracker = caves.put(levelKey, tracker);
        if (oldTracker != null) {
            throw new IllegalStateException("Duplicate cave tracker: " + levelKey);
        }
        RegistryAccess registryAccess = server.registryAccess();
        DIDPlatform.registerCustomDimension(server, levelKey, style.value().dimension(registryAccess), new DIDChunkGenerator(registryAccess, style, size, seed, debug, listener));

        try {
            FileUtils.forceDeleteOnExit(server.storageSource.getDimensionPath(levelKey).toFile());
        } catch (IOException ignore) {
        }
        return tracker;
    }

    public static void deleteCave(MinecraftServer server, ResourceKey<Level> levelKey) {
        CaveTracker tracker = caves.remove(levelKey);
        if (tracker != null) {
            DIDPlatform.deleteCustomDimension(server, levelKey);
        }
    }

}
