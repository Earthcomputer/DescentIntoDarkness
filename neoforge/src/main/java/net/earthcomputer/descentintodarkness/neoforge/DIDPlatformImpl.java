package net.earthcomputer.descentintodarkness.neoforge;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.commoble.infiniverse.api.InfiniverseAPI;
import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class DIDPlatformImpl {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static List<DynamicRegistry<?>> dynamicRegistries = new ArrayList<>();

    public static void registerCustomDimension(MinecraftServer server, ResourceKey<Level> id) {
        Registry<DimensionType> dimTypeRegistry = server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        Holder<DimensionType> overworldDimType = dimTypeRegistry.getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD);
        InfiniverseAPI.get().getOrCreateLevel(server, id, () -> new LevelStem(overworldDimType, server.overworld().getChunkSource().getGenerator()));
    }

    public static void deleteCustomDimension(MinecraftServer server, ResourceKey<Level> id) {
        InfiniverseAPI.get().markDimensionForUnregistration(server, id);
    }

    public static <T> void registerDynamicRegistry(ResourceKey<Registry<T>> id, Codec<T> codec, @Nullable Codec<T> networkCodec) {
        if (dynamicRegistries == null) {
            throw new IllegalStateException("Too late to register a dynamic registry");
        }
        dynamicRegistries.add(new DynamicRegistry<>(id, codec, networkCodec));
    }

    public static CreativeModeTab.Builder creativeTabBuilder() {
        return CreativeModeTab.builder();
    }

    private record DynamicRegistry<T>(ResourceKey<Registry<T>> id, Codec<T> codec, @Nullable Codec<T> networkCodec) {
        void register(DataPackRegistryEvent.NewRegistry event) {
            event.dataPackRegistry(id, codec, networkCodec);
        }
    }

    @EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = DescentIntoDarkness.MOD_ID)
    private static class ModBusSubscriber {
        @SubscribeEvent
        public static void registerDynamicRegistries(DataPackRegistryEvent.NewRegistry event) {
            for (DynamicRegistry<?> registry : dynamicRegistries) {
                registry.register(event);
            }
            dynamicRegistries = null;
        }
    }
}
