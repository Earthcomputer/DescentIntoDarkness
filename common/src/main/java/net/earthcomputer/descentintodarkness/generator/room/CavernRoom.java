package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;

import java.util.List;

public final class CavernRoom extends Room {
    public static final MapCodec<CavernRoom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        tagsCodec(),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("centroids", UniformInt.of(4, 7)).forGetter(room -> room.centroids),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("min_spread", 1).forGetter(room -> room.minSpread),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("max_spread", 2).forGetter(room -> room.maxSpread),
        DIDCodecs.INT_PROVIDER.optionalFieldOf("centroid_size_variance", ConstantInt.of(0)).forGetter(room -> room.centroidSizeVariance),
        DIDCodecs.floatProviderRange(-360, 360).optionalFieldOf("turn", UniformFloat.of(0, 90)).forGetter(room -> room.turn)
    ).apply(instance, CavernRoom::new));

    private final IntProvider centroids;
    private final int minSpread;
    private final int maxSpread;
    private final IntProvider centroidSizeVariance;
    private final FloatProvider turn;

    private CavernRoom(List<String> tags, IntProvider centroids, int minSpread, int maxSpread, IntProvider centroidSizeVariance, FloatProvider turn) {
        super(tags);
        this.centroids = centroids;
        this.minSpread = minSpread;
        this.maxSpread = maxSpread;
        this.centroidSizeVariance = centroidSizeVariance;
        this.turn = turn;
    }

    @Override
    public RoomType<?> type() {
        return RoomType.CAVERN.get();
    }
}
