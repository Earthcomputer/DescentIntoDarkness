package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;

import java.util.List;

public final class VerticalRoom extends Room {
    public static final MapCodec<VerticalRoom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        tagsCodec(),
        DIDCodecs.floatProviderRange(-180, 180).optionalFieldOf("pitch", ConstantFloat.of(90)).forGetter(room -> room.pitch),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("length", UniformInt.of(3, 5)).forGetter(room -> room.length)
    ).apply(instance, VerticalRoom::new));

    private final FloatProvider pitch;
    private final IntProvider length;

    private VerticalRoom(List<String> tags, FloatProvider pitch, IntProvider length) {
        super(tags);
        this.pitch = pitch;
        this.length = length;
    }

    @Override
    public RoomType<?> type() {
        return RoomType.VERTICAL.get();
    }
}
