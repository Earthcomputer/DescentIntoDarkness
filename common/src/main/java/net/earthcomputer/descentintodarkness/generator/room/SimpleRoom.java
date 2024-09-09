package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.Centroid;

import java.util.List;

public final class SimpleRoom extends Room<Object> {
    public static final MapCodec<SimpleRoom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(tagsCodec()).apply(instance, SimpleRoom::new));

    private SimpleRoom(List<String> tags) {
        super(tags);
    }

    @Override
    public RoomType<?> type() {
        return RoomType.SIMPLE.get();
    }

    @Override
    public void addCentroids(CaveGenContext ctx, RoomData roomData, Object userData, List<Centroid> centroids) {
        centroids.add(new Centroid(roomData.location(), roomData.caveRadius(), roomData));
    }
}
