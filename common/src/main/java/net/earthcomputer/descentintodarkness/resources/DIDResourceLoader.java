package net.earthcomputer.descentintodarkness.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.architectury.platform.Mod;
import dev.architectury.platform.Platform;
import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.earthcomputer.descentintodarkness.entity.DIDEntities;
import net.earthcomputer.descentintodarkness.item.DIDItems;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public final class DIDResourceLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean developmentResourcesDetected = false;
    private static boolean restartRequired = false;

    private DIDResourceLoader() {
    }

    public static void initialLoad() {
        reload();
        restartRequired = false;
        register();
    }

    public static void reload() {
        DIDSounds.reload();
        DIDBlocks.reload();
        DIDItems.reload();
        DIDEntities.reload();
    }

    private static void register() {
        DIDSounds.register();
        DIDBlocks.register();
        DIDItems.register();
        DIDEntities.register();
    }

    public static <T> Map<String, T> loadResources(String directory, Codec<T> codec) throws Exception {
        Map<String, T> loadedResources = new TreeMap<>();
        Map<String, Exception> exceptions = new LinkedHashMap<>();

        List<Mod> ourMods = new ArrayList<>(2);
        ourMods.add(Platform.getMod(DescentIntoDarkness.MOD_ID));
        if (Platform.isDevelopmentEnvironment()) {
            for (Mod mod : Platform.getMods()) {
                if (mod.getModId().startsWith("generated_")) {
                    ourMods.add(mod);
                }
            }
        }
        for (Mod mod : ourMods) {
            for (Path filePath : mod.getFilePaths()) {
                Path dir = filePath.resolve(directory);
                loadResourceInDir(codec, dir, loadedResources, exceptions);
            }
        }

        Path developmentDir = Platform.getConfigFolder().resolve(DescentIntoDarkness.MOD_ID).resolve("development_resources").resolve(directory);
        if (loadResourceInDir(codec, developmentDir, loadedResources, exceptions)) {
            developmentResourcesDetected = true;
        }

        if (!exceptions.isEmpty()) {
            exceptions.forEach((resource, exception) -> LOGGER.error(">> {}", resource, exception));
            throw new Exception("Failed to load " + directory + " resources due to aforementioned errors");
        }

        return loadedResources;
    }

    private static <T> boolean loadResourceInDir(Codec<T> codec, Path dir, Map<String, T> loadedResources, Map<String, Exception> exceptions) {
        if (!Files.isDirectory(dir)) {
            return false;
        }

        boolean[] resourcesDetected = {false};
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.forEach(path -> {
                if (Files.isRegularFile(path) && path.toString().endsWith(".json")) {
                    String resourcePath = dir.relativize(path).toString();
                    try (BufferedReader reader = Files.newBufferedReader(path)) {
                        JsonElement json = JsonParser.parseReader(reader);
                        T resource = codec.parse(JsonOps.INSTANCE, json).getOrThrow();
                        loadedResources.put(resourcePath.substring(0, resourcePath.length() - ".json".length()), resource);
                        resourcesDetected[0] = true;
                    } catch (Exception e) {
                        exceptions.put(resourcePath, e);
                    }
                }
            });
        } catch (Exception e) {
            exceptions.put(dir.toString(), e);
        }

        return resourcesDetected[0];
    }

    public static <T extends RestartRequiringEntry<T>> void checkRequiresRestart(Map<String, T> existingEntries, Map<String, T> newEntries) {
        if (!existingEntries.keySet().equals(newEntries.keySet())) {
            restartRequired = true;
        } else if (existingEntries.entrySet().stream().anyMatch(entry -> entry.getValue().requiresRestart(newEntries.get(entry.getKey())))) {
            restartRequired = true;
        }
    }

    public static boolean areDevelopmentResourcesDetected() {
        return developmentResourcesDetected;
    }

    public static boolean isRestartRequired() {
        return restartRequired;
    }

    public interface RestartRequiringEntry<T extends RestartRequiringEntry<T>> {
        default boolean requiresRestart(T other) {
            return !this.equals(other);
        }
    }
}
