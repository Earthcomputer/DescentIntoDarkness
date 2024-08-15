package net.earthcomputer.descentintodarkness.neoforge;

import net.commoble.infiniverse.api.InfiniverseAPI;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

public final class DescentIntoDarknessPlatformImpl {
    public static void registerCustomDimension(MinecraftServer server, ResourceKey<Level> id) {
        Registry<DimensionType> dimTypeRegistry = server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        Holder<DimensionType> overworldDimType = dimTypeRegistry.getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD);
        InfiniverseAPI.get().getOrCreateLevel(server, id, () -> new LevelStem(overworldDimType, server.overworld().getChunkSource().getGenerator()));
    }

    public static void deleteCustomDimension(MinecraftServer server, ResourceKey<Level> id) {
        InfiniverseAPI.get().markDimensionForUnregistration(server, id);
    }
}
