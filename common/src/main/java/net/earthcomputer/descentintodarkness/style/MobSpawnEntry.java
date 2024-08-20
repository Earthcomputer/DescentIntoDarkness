package net.earthcomputer.descentintodarkness.style;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

public record MobSpawnEntry(
    EntityType<?> mob,
    int singleMobCost,
    IntProvider packCost,
    int weight,
    int minDistance,
    int maxDistance,
    int cooldown,
    int despawnRange,
    double xSize,
    double ySize,
    double zSize,
    BlockPredicate canSpawnOn,
    BlockPredicate canSpawnIn,
    boolean centeredSpawn,
    boolean randomRotation
) {
    public static final Codec<MobSpawnEntry> CODEC = RecordCodecBuilder.<MobSpawnEntry>create(instance -> instance.group(
        BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("mob").forGetter(MobSpawnEntry::mob),
        Codec.INT.optionalFieldOf("single_mob_cost", 50).forGetter(MobSpawnEntry::singleMobCost),
        DIDCodecs.INT_PROVIDER.optionalFieldOf("pack_cost", UniformInt.of(100, 300)).forGetter(MobSpawnEntry::packCost),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("weight", 10).forGetter(MobSpawnEntry::weight),
        ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("min_distance", 15).forGetter(MobSpawnEntry::minDistance),
        ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("max_distance", 25).forGetter(MobSpawnEntry::maxDistance),
        ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("cooldown", 20).forGetter(MobSpawnEntry::cooldown),
        ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("despawn_range", 48).forGetter(MobSpawnEntry::despawnRange),
        DIDCodecs.NON_NEGATIVE_DOUBLE.optionalFieldOf("x_size", 0.0).forGetter(MobSpawnEntry::xSize),
        DIDCodecs.NON_NEGATIVE_DOUBLE.optionalFieldOf("y_size", 0.0).forGetter(MobSpawnEntry::ySize),
        DIDCodecs.NON_NEGATIVE_DOUBLE.optionalFieldOf("z_size", 0.0).forGetter(MobSpawnEntry::zSize),
        DIDCodecs.BLOCK_PREDICATE.optionalFieldOf("can_spawn_on", BlockPredicate.solid()).forGetter(MobSpawnEntry::canSpawnOn),
        DIDCodecs.BLOCK_PREDICATE.optionalFieldOf("can_spawn_in", BlockPredicate.allOf(BlockPredicate.not(BlockPredicate.solid()), BlockPredicate.noFluid())).forGetter(MobSpawnEntry::canSpawnIn),
        Codec.BOOL.optionalFieldOf("centered_spawn", false).forGetter(MobSpawnEntry::centeredSpawn),
        Codec.BOOL.optionalFieldOf("random_rotation", true).forGetter(MobSpawnEntry::randomRotation)
    ).apply(instance, MobSpawnEntry::new)).validate(entry -> {
        if (entry.maxDistance < entry.minDistance) {
            return DataResult.error(() -> "Invalid mob spawn distance range");
        }
        return DataResult.success(entry);
    });
}
