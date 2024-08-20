package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public final class SimpleRoom extends Room {
    public static final MapCodec<SimpleRoom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(tagsCodec()).apply(instance, SimpleRoom::new));

    private SimpleRoom(List<String> tags) {
        super(tags);
    }

    @Override
    public RoomType<?> type() {
        return RoomType.SIMPLE.get();
    }
}
