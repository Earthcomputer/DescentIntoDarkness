package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.valueproviders.FloatProvider;

import java.util.List;

public final class TurnRoom extends Room {
    public static final MapCodec<TurnRoom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        tagsCodec(),
        DIDCodecs.floatProviderRange(-360, 360).fieldOf("angle").forGetter(room -> room.angle)
    ).apply(instance, TurnRoom::new));

    private final FloatProvider angle;

    private TurnRoom(List<String> tags, FloatProvider angle) {
        super(tags);
        this.angle = angle;
    }

    @Override
    public RoomType<?> type() {
        return RoomType.TURN.get();
    }
}