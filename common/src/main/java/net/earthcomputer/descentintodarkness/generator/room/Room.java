package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.DIDRegistries;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.Centroid;
import net.earthcomputer.descentintodarkness.generator.ModuleGenerator;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public abstract class Room<D> {
    private static final RecordCodecBuilder<Room<?>, List<String>> TAGS_CODEC = DIDCodecs.singleableList(Codec.STRING).optionalFieldOf("tags", List.of()).forGetter(Room::tags);
    @SuppressWarnings("unchecked")
    protected static <R extends Room<?>> RecordCodecBuilder<R, List<String>> tagsCodec() {
        return (RecordCodecBuilder<R, List<String>>) TAGS_CODEC;
    }

    public static final Codec<Room<?>> CODEC = Codec.lazyInitialized(() -> DIDRegistries.roomType().byNameCodec().dispatch(Room::type, RoomType::codec));

    private final List<String> tags;

    protected Room(List<String> tags) {
        this.tags = tags;
    }

    public abstract RoomType<?> type();

    public final List<String> tags() {
        return tags;
    }

    public D createUserData(CaveGenContext ctx, RoomData roomData) {
        return null;
    }

    public Vec3 adjustDirection(CaveGenContext ctx, RoomData roomData, D userData) {
        return roomData.direction();
    }

    public Vec3 adjustLocation(CaveGenContext ctx, RoomData roomData, D userData) {
        return ModuleGenerator.vary(ctx, roomData.location()).add(roomData.direction().scale(roomData.caveRadius()));
    }

    public abstract void addCentroids(CaveGenContext ctx, RoomData roomData, D userData, List<Centroid> centroids);

    public boolean isBranch() {
        return false;
    }

    public char getBranchSymbol() {
        return 0;
    }
}
