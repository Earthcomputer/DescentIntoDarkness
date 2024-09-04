package net.earthcomputer.descentintodarkness.style;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.AnyOfPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.MatchingBlocksPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class DIDCodecs {
    private DIDCodecs() {
    }

    public static final Codec<Character> CHAR = Codec.STRING.comapFlatMap(str -> str.length() == 1 ? DataResult.success(str.charAt(0)) : DataResult.error(() -> "String must be a single character: " + str), Object::toString);
    public static final Codec<Float> NON_NEGATIVE_FLOAT = floatRangeWithMessage(Codec.FLOAT, 0f, Float.MAX_VALUE, value -> "Value must be non-negative: " + value);
    public static final Codec<Double> NON_NEGATIVE_DOUBLE = doubleRangeWithMessage(Codec.DOUBLE, 0.0, Double.MAX_VALUE, value -> "Value must be non-negative: " + value);

    public static final Codec<IntProvider> INT_PROVIDER = Codec.either(
        Codec.list(Codec.INT, 2, 2).validate(list -> list.getFirst() <= list.getLast() ? DataResult.success(list) : DataResult.error(() -> "Range max (" + list.getLast() + ") < range min (" + list.getFirst() + ")")),
        IntProvider.CODEC
    ).xmap(
        either -> either.map(list -> UniformInt.of(list.getFirst(), list.getLast()), Function.identity()),
        provider -> provider instanceof UniformInt uniform ? Either.left(List.of(uniform.getMinValue(), uniform.getMaxValue())) : Either.right(provider)
    );
    public static final Codec<IntProvider> POSITIVE_INT_PROVIDER = intProviderRange(1, Integer.MAX_VALUE);
    public static final Codec<IntProvider> NON_NEGATIVE_INT_PROVIDER = intProviderRange(0, Integer.MAX_VALUE);

    public static final Codec<FloatProvider> FLOAT_PROVIDER = Codec.either(
        Codec.list(Codec.FLOAT, 2, 2).validate(list -> list.getFirst() < list.getLast() ? DataResult.success(list) : DataResult.error(() -> "Range max (" + list.getLast() + ") <= range min (" + list.getFirst() + ")")),
        FloatProvider.CODEC
    ).xmap(
        either -> either.map(list -> UniformFloat.of(list.getFirst(), list.getLast()), Function.identity()),
        provider -> provider instanceof UniformFloat uniform ? Either.left(List.of(uniform.getMinValue(), uniform.getMaxValue())) : Either.right(provider)
    );
    public static final Codec<FloatProvider> NON_NEGATIVE_FLOAT_PROVIDER = floatProviderRange(0, Float.POSITIVE_INFINITY);

    public static final Codec<BlockState> BLOCK_STATE = Codec.either(BuiltInRegistries.BLOCK.byNameCodec(), BlockState.CODEC).xmap(
        either -> either.map(Block::defaultBlockState, Function.identity()),
        state -> state == state.getBlock().defaultBlockState() ? Either.left(state.getBlock()) : Either.right(state)
    );

    public static final Codec<BlockStateProvider> BLOCK_STATE_PROVIDER = Codec.either(BuiltInRegistries.BLOCK.byNameCodec(), BlockStateProvider.CODEC).xmap(
        either -> either.map(BlockStateProvider::simple, Function.identity()),
        provider -> provider instanceof SimpleStateProvider simple && simple.state == simple.state.getBlock().defaultBlockState() ? Either.left(simple.state.getBlock()) : Either.right(provider)
    );

    public static final Codec<BlockPredicate> BLOCK_PREDICATE = Codec.recursive(
        "BlockPredicate",
        blockPredicateCodec -> Codec.either(
            Codec.either(TagKey.hashedCodec(Registries.BLOCK), BuiltInRegistries.BLOCK.byNameCodec()),
            Codec.either(blockPredicateCodec.listOf(), BlockPredicate.CODEC)
        ).xmap(
            either -> either.map(either2 -> either2.map(BlockPredicate::matchesTag, BlockPredicate::matchesBlocks), either2 -> either2.map(BlockPredicate::anyOf, Function.identity())),
            predicate -> switch (predicate) {
                case MatchingBlocksPredicate matchingBlocks -> switch (matchingBlocks.blocks) {
                    case HolderSet.Named<Block> tag -> Either.<Either<TagKey<Block>, Block>, Either<List<BlockPredicate>, BlockPredicate>>left(Either.left(tag.key()));
                    case HolderSet.Direct<Block> direct when direct.size() == 1 -> Either.<Either<TagKey<Block>, Block>, Either<List<BlockPredicate>, BlockPredicate>>left(Either.right(direct.get(0).value()));
                    default -> Either.<Either<TagKey<Block>, Block>, Either<List<BlockPredicate>, BlockPredicate>>right(Either.right(predicate));
                };
                case AnyOfPredicate anyOfPredicate -> Either.<Either<TagKey<Block>, Block>, Either<List<BlockPredicate>, BlockPredicate>>right(Either.left(anyOfPredicate.predicates));
                default -> Either.<Either<TagKey<Block>, Block>, Either<List<BlockPredicate>, BlockPredicate>>right(Either.right(predicate));
            }
        )
    );

    private static final Codec<Double> VOXEL_COORD = doubleRange(-16, 32);
    private static final Codec<Vec3> VOXEL_POINT = Codec.list(VOXEL_COORD, 3, 3).xmap(list -> new Vec3(list.getFirst(), list.get(1), list.get(2)), vec -> List.of(vec.x, vec.y, vec.z));
    private static final Codec<AABB> VOXEL_BOX = RecordCodecBuilder.create(instance -> instance.group(
        VOXEL_POINT.fieldOf("from").forGetter(AABB::getMinPosition),
        VOXEL_POINT.fieldOf("to").forGetter(AABB::getMaxPosition)
    ).apply(instance, AABB::new));
    public static final Codec<VoxelShape> VOXEL_SHAPE = singleableList(VOXEL_BOX).xmap(
        boxes -> boxes.stream().map(aabb -> Block.box(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ)).reduce(Shapes::or).orElseGet(Shapes::empty),
        shape -> {
            List<AABB> boxes = new ArrayList<>();
            shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> boxes.add(new AABB(minX * 16, minY * 16, minZ * 16, maxX * 16, maxY * 16, maxZ * 16)));
            return boxes;
        }
    );

    private static Codec<Float> floatRangeWithMessage(Codec<Float> codec, float min, float max, Function<Float, String> message) {
        return codec.validate(value -> value >= min && value <= max ? DataResult.success(value) : DataResult.error(() -> message.apply(value)));
    }

    public static Codec<Double> doubleRange(double min, double max) {
        return doubleRangeWithMessage(Codec.DOUBLE, min, max, value -> "Value must be within range [" + min + ";" + max + "]: " + value);
    }

    private static Codec<Double> doubleRangeWithMessage(Codec<Double> codec, double min, double max, Function<Double, String> message) {
        return codec.validate(value -> value >= min && value <= max ? DataResult.success(value) : DataResult.error(() -> message.apply(value)));
    }

    public static <T> Codec<List<T>> singleableList(Codec<T> elementCodec) {
        return Codec.either(elementCodec.listOf(), elementCodec).xmap(
            either -> either.map(Function.identity(), List::of),
            list -> list.size() == 1 ? Either.right(list.getFirst()) : Either.left(list)
        );
    }

    public static Codec<IntProvider> intProviderRange(int min, int max) {
        return INT_PROVIDER.validate(provider -> {
            if (provider.getMinValue() < min) {
                return DataResult.error(() -> "Value provider too low: " + min + " [" + provider.getMinValue() + "-" + provider.getMaxValue() + "]");
            } else if (provider.getMaxValue() > max) {
                return DataResult.error(() -> "Value provider too high: " + max + " [" + provider.getMinValue() + "-" + provider.getMaxValue() + "]");
            } else {
                return DataResult.success(provider);
            }
        });
    }

    public static Codec<FloatProvider> floatProviderRange(float min, float max) {
        return FLOAT_PROVIDER.validate(provider -> {
            if (provider.getMinValue() < min) {
                return DataResult.error(() -> "Value provider too low: " + min + " [" + provider.getMinValue() + "-" + provider.getMaxValue() + "]");
            } else if (provider.getMaxValue() > max) {
                return DataResult.error(() -> "Value provider too high: " + max + " [" + provider.getMinValue() + "-" + provider.getMaxValue() + "]");
            } else {
                return DataResult.success(provider);
            }
        });
    }

    public static <A> MapCodec<A> withoutField(String fieldName, MapCodec<A> downstream) {
        return MapCodec.of(downstream, new MapDecoder.Implementation<>() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> dynamicOps) {
                return downstream.keys(dynamicOps);
            }

            @Override
            public <T> DataResult<A> decode(DynamicOps<T> dynamicOps, MapLike<T> mapLike) {
                if (mapLike.get(fieldName) != null) {
                    return DataResult.error(() -> "Map contained field " + fieldName);
                }
                return downstream.decode(dynamicOps, mapLike);
            }

            @Override
            public String toString() {
                return "UnitWithoutFieldDecoder[" + fieldName + ", " + downstream + "]";
            }
        });
    }
}
