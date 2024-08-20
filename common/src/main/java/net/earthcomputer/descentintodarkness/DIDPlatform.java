package net.earthcomputer.descentintodarkness;

import com.mojang.serialization.Codec;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class DIDPlatform {
    @ExpectPlatform
    public static void registerCustomDimension(MinecraftServer server, ResourceKey<Level> id) {
    }

    @ExpectPlatform
    public static void deleteCustomDimension(MinecraftServer server, ResourceKey<Level> id) {
    }

    @ExpectPlatform
    public static <T> void registerDynamicRegistry(ResourceKey<? extends Registry<T>> id, Codec<T> codec, @Nullable Codec<T> networkCodec) {
    }
}
