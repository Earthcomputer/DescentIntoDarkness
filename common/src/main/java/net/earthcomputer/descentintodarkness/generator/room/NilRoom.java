package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public final class NilRoom extends Room {
    public static final MapCodec<NilRoom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(tagsCodec()).apply(instance, NilRoom::new));

    private NilRoom(List<String> tags) {
        super(tags);
    }

    @Override
    public RoomType<?> type() {
        return RoomType.NIL.get();
    }
}
