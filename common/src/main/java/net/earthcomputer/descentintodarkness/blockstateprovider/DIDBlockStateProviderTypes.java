package net.earthcomputer.descentintodarkness.blockstateprovider;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.registries.RegistrySupplier;
import net.earthcomputer.descentintodarkness.DIDRegistries;
import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;

public final class DIDBlockStateProviderTypes {
    private DIDBlockStateProviderTypes() {
    }

    public static final RegistrySupplier<BlockStateProviderType<RoomWeightedStateProvider>> ROOM_WEIGHTED = register("room_weighted", RoomWeightedStateProvider.CODEC);

    public static void register() {
        // load class
    }

    private static <P extends BlockStateProvider> RegistrySupplier<BlockStateProviderType<P>> register(String id, MapCodec<P> codec) {
        return DIDRegistries.REGISTRAR_MANAGER.get(Registries.BLOCK_STATE_PROVIDER_TYPE).register(DescentIntoDarkness.id(id), () -> new BlockStateProviderType<>(codec));
    }
}
