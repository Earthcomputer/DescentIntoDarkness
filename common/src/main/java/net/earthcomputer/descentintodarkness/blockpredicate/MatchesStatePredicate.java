package net.earthcomputer.descentintodarkness.blockpredicate;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;

import java.util.Map;
import java.util.Optional;

public final class MatchesStatePredicate implements BlockPredicate {
    public static final MapCodec<MatchesStatePredicate> CODEC = codec("name", "properties");

    public static MapCodec<MatchesStatePredicate> codec(String nameKey, String propertiesKey) {
        return BuiltInRegistries.BLOCK.byNameCodec().dispatchMap(nameKey, predicate -> predicate.block, block -> {
            MapCodec<Map<Property<?>, Object>> propertiesCodec = MapCodec.unit(Map.of());
            for (Property<?> property : block.getStateDefinition().getProperties()) {
                propertiesCodec = appendPropertyCodec(propertiesCodec, property);
            }
            return propertiesCodec
                .xmap(properties -> new MatchesStatePredicate(block, properties), predicate -> predicate.properties)
                .codec()
                .optionalFieldOf(propertiesKey, new MatchesStatePredicate(block, Map.of()));
        });
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> MapCodec<Map<Property<?>, Object>> appendPropertyCodec(MapCodec<Map<Property<?>, Object>> propertiesCodec, Property<T> property) {
        return Codec.mapPair(propertiesCodec, property.valueCodec().optionalFieldOf(property.getName())).xmap(
            pair -> {
                if (pair.getSecond().isPresent()) {
                    var newProperties = ImmutableMap.<Property<?>, Object>builderWithExpectedSize(pair.getFirst().size() + 1);
                    newProperties.putAll(pair.getFirst());
                    newProperties.put(property, pair.getSecond().get().value());
                    return newProperties.build();
                } else {
                    return pair.getFirst();
                }
            },
            properties -> Pair.of(properties, Optional.ofNullable(properties.get(property)).map(value -> new Property.Value<>(property, (T) value)))
        );
    }

    private final Block block;
    private final Map<Property<?>, Object> properties;

    private MatchesStatePredicate(Block block, Map<Property<?>, Object> properties) {
        this.block = block;
        this.properties = properties;
    }

    @Override
    public BlockPredicateType<?> type() {
        return DIDBlockPredicateTypes.MATCHES_STATE.get();
    }

    @Override
    public boolean test(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this.block)) {
            return false;
        }
        for (var entry : this.properties.entrySet()) {
            if (!state.getValue(entry.getKey()).equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }
}
