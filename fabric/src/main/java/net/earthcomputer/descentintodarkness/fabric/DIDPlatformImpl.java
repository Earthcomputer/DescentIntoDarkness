package net.earthcomputer.descentintodarkness.fabric;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.HashMap;
import java.util.Map;

public final class DIDPlatformImpl {
    private static final Map<ResourceKey<Level>, RuntimeWorldHandle> handles = new HashMap<>();

    public static void registerCustomDimension(MinecraftServer server, ResourceKey<Level> id) {
        RuntimeWorldHandle handle = Fantasy.get(server)
            .openTemporaryWorld(id.location(), new RuntimeWorldConfig()
                .setDimensionType(BuiltinDimensionTypes.OVERWORLD)
                .setGenerator(server.overworld().getChunkSource().getGenerator()));
        handles.put(id, handle);
    }

    public static void deleteCustomDimension(MinecraftServer server, ResourceKey<Level> id) {
        RuntimeWorldHandle handle = handles.remove(id);
        if (handle != null) {
            handle.delete();
        }
    }

    public static <T> void registerDynamicRegistry(ResourceKey<? extends Registry<T>> id, Codec<T> codec, @Nullable Codec<T> networkCodec) {
        if (networkCodec != null) {
            DynamicRegistries.registerSynced(id, codec, networkCodec);
        } else {
            DynamicRegistries.register(id, codec);
        }
    }

    public static CreativeModeTab.Builder creativeTabBuilder() {
        return FabricItemGroup.builder();
    }
}
