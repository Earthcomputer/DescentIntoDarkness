package net.earthcomputer.descentintodarkness;

import com.mojang.serialization.Codec;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DIDPlatform {
    @ExpectPlatform
    public static void registerCustomDimension(MinecraftServer server, ResourceKey<Level> id, Holder<DimensionType> dimensionType, ChunkGenerator chunkGenerator) {
    }

    @ExpectPlatform
    public static void deleteCustomDimension(MinecraftServer server, ResourceKey<Level> id) {
    }

    @ExpectPlatform
    public static <T> void registerDynamicRegistry(ResourceKey<? extends Registry<T>> id, Codec<T> codec, @Nullable Codec<T> networkCodec) {
    }

    @ExpectPlatform
    @NotNull
    public static CreativeModeTab.Builder creativeTabBuilder() {
        //noinspection DataFlowIssue
        return null;
    }
}
