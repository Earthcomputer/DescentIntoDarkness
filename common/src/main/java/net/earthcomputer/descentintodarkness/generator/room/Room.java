package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.DIDRegistries;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;

import java.util.List;

public abstract class Room {
    private static final RecordCodecBuilder<Room, List<String>> TAGS_CODEC = DIDCodecs.singleableListCodec(Codec.STRING).optionalFieldOf("tags", List.of()).forGetter(Room::tags);
    @SuppressWarnings("unchecked")
    protected static <R extends Room> RecordCodecBuilder<R, List<String>> tagsCodec() {
        return (RecordCodecBuilder<R, List<String>>) TAGS_CODEC;
    }

    public static final Codec<Room> CODEC = Codec.lazyInitialized(() -> DIDRegistries.roomType().byNameCodec().dispatch(Room::type, RoomType::codec));

    private final List<String> tags;

    protected Room(List<String> tags) {
        this.tags = tags;
    }

    public abstract RoomType<?> type();

    public final List<String> tags() {
        return tags;
    }

    public boolean isBranch() {
        return false;
    }

    public char getBranchSymbol() {
        return 0;
    }
}
