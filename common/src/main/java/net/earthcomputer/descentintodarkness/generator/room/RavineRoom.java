package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;

import java.util.List;

public final class RavineRoom extends Room {
    public static final MapCodec<RavineRoom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        tagsCodec(),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("length", UniformInt.of(70, 100)).forGetter(room -> room.length),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("height", UniformInt.of(80, 120)).forGetter(room -> room.height),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("width", UniformInt.of(10, 20)).forGetter(room -> room.width),
        DIDCodecs.NON_NEGATIVE_FLOAT_PROVIDER.optionalFieldOf("turn", UniformFloat.of(0, 30)).forGetter(room -> room.turn),
        Codec.doubleRange(0, 1).optionalFieldOf("height_vary_chance", 0.2).forGetter(room -> room.heightVaryChance)
    ).apply(instance, RavineRoom::new));

    private final IntProvider length;
    private final IntProvider height;
    private final IntProvider width;
    private final FloatProvider turn;
    private final double heightVaryChance;

    private RavineRoom(List<String> tags, IntProvider length, IntProvider height, IntProvider width, FloatProvider turn, double heightVaryChance) {
        super(tags);
        this.length = length;
        this.height = height;
        this.width = width;
        this.turn = turn;
        this.heightVaryChance = heightVaryChance;
    }

    @Override
    public RoomType<?> type() {
        return RoomType.RAVINE.get();
    }
}
