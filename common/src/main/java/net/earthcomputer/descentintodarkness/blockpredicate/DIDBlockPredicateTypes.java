package net.earthcomputer.descentintodarkness.blockpredicate;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.registries.RegistrySupplier;
import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.earthcomputer.descentintodarkness.DIDRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;

public final class DIDBlockPredicateTypes {
    private DIDBlockPredicateTypes() {
    }

    public static final RegistrySupplier<BlockPredicateType<MatchesStatePredicate>> MATCHES_STATE = register("matches_state", MatchesStatePredicate.CODEC);

    public static void register() {
        // load class
    }

    private static <P extends BlockPredicate> RegistrySupplier<BlockPredicateType<P>> register(String id, MapCodec<P> codec) {
        return DIDRegistries.REGISTRAR_MANAGER.get(Registries.BLOCK_PREDICATE_TYPE).register(DescentIntoDarkness.id(id), () -> () -> codec);
    }
}
