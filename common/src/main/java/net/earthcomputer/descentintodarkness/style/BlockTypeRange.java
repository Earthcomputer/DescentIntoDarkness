package net.earthcomputer.descentintodarkness.style;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.DIDConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public final class BlockTypeRange<T extends Comparable<T>> {
    public static final Codec<BlockTypeRange<Integer>> INT_CODEC = codec(Codec.INT, DIDConstants.MIN_Y, DIDConstants.MAX_Y, i -> i - 1, i -> i + 1);
    public static final Codec<BlockTypeRange<Double>> DOUBLE_CODEC = codec(Codec.DOUBLE, 0.0, 1.0, Math::nextDown, Math::nextUp);

    private final List<Entry<T>> entries;

    private static <T extends Comparable<T>> Codec<BlockTypeRange<T>> codec(Codec<T> tCodec, T min, T max, UnaryOperator<T> nextDown, UnaryOperator<T> nextUp) {
        return DIDCodecs.singleableListCodec(Entry.codec(tCodec, min, max))
            .xmap(BlockTypeRange::new, range -> range.entries)
            .validate(range -> range.validateRange(min, max, nextDown, nextUp));
    }

    private BlockTypeRange(List<Entry<T>> entries) {
        this.entries = entries;
    }

    public static BlockTypeRange<Integer> simpleInt(BlockStateProvider block) {
        return new BlockTypeRange<>(List.of(new Entry<>(DIDConstants.MIN_Y, DIDConstants.MAX_Y, block)));
    }

    public static BlockTypeRange<Double> simpleDouble(BlockStateProvider block) {
        return new BlockTypeRange<>(List.of(new Entry<>(0.0, 1.0, block)));
    }

    @Nullable
    public BlockStateProvider get(T yLevel) {
        for (Entry<T> entry : entries) {
            if (entry.min.compareTo(yLevel) <= 0 && yLevel.compareTo(entry.max) <= 0) {
                return entry.block;
            }
        }
        return null;
    }

    public DataResult<BlockTypeRange<T>> validateRange(T min, T max, UnaryOperator<T> nextDown, UnaryOperator<T> nextUp) {
        List<Pair<T, T>> unaccountedList = Lists.newArrayList(Pair.of(min, max));
        for (Entry<T> entry : entries) {
            for (int i = unaccountedList.size() - 1; i >= 0; i--) {
                Pair<T, T> unaccounted = unaccountedList.get(i);
                if (unaccounted.getRight().compareTo(entry.min) < 0 || unaccounted.getLeft().compareTo(entry.max) > 0) {
                    // ranges do not overlap
                    continue;
                }
                // ranges overlap, so this whole range surely can't stay
                unaccountedList.remove(i);
                // the top side of the range
                if (entry.max.compareTo(unaccounted.getRight()) < 0) {
                    unaccountedList.add(i, Pair.of(nextUp.apply(entry.max), unaccounted.getRight()));
                }
                // the bottom side of the range
                if (unaccounted.getLeft().compareTo(entry.min) < 0) {
                    unaccountedList.add(i, Pair.of(unaccounted.getLeft(), nextDown.apply(entry.min)));
                }
            }
        }
        if (!unaccountedList.isEmpty()) {
            return DataResult.error(() -> "Range list does not take into account ranges " + unaccountedList.stream()
                .map(range -> range.getLeft().equals(range.getRight()) ? String.valueOf(range.getLeft()) : (range.getLeft() + "-" + range.getRight()))
                .collect(Collectors.joining(", ")));
        }

        return DataResult.success(this);
    }

    public static final class Entry<T extends Comparable<T>> {
        private final T min;
        private final T max;
        private final BlockStateProvider block;

        private static <T extends Comparable<T>> Codec<Entry<T>> codec(Codec<T> tCodec, T defaultMin, T defaultMax) {
            Codec<Entry<T>> fullCodec = RecordCodecBuilder.<Entry<T>>create(instance -> instance.group(
                tCodec.optionalFieldOf("min", defaultMin).forGetter(entry -> entry.min),
                tCodec.optionalFieldOf("max", defaultMax).forGetter(entry -> entry.max),
                DIDCodecs.BLOCK_STATE_PROVIDER.fieldOf("block").forGetter(entry -> entry.block)
            ).apply(instance, Entry::new))
                .validate(entry -> entry.max.compareTo(entry.min) < 0
                    ? DataResult.error(() -> "Range max (" + entry.max + ") < min (" + entry.min + ")")
                    : DataResult.success(entry)
                );
            return Codec.either(fullCodec, BuiltInRegistries.BLOCK.byNameCodec()).xmap(
                either -> either.map(
                    Function.identity(),
                    block -> new Entry<>(defaultMin, defaultMax, BlockStateProvider.simple(block))
                ),
                entry -> entry.min.equals(defaultMin)
                    && entry.max.equals(defaultMax)
                    && entry.block instanceof SimpleStateProvider simple
                    && simple.state == simple.state.getBlock().defaultBlockState()
                    ? Either.right(simple.state.getBlock())
                    : Either.left(entry)
            );
        }

        private Entry(T min, T max, BlockStateProvider block) {
            this.min = min;
            this.max = max;
            this.block = block;
        }
    }
}
