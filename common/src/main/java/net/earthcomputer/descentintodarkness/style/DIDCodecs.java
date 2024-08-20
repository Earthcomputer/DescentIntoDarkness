package net.earthcomputer.descentintodarkness.style;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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

import java.util.List;
import java.util.function.Function;

public final class DIDCodecs {
    private DIDCodecs() {
    }

    public static final Codec<Character> CHAR = Codec.STRING.comapFlatMap(str -> str.length() == 1 ? DataResult.success(str.charAt(0)) : DataResult.error(() -> "String must be a single character: " + str), Object::toString);
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
}
