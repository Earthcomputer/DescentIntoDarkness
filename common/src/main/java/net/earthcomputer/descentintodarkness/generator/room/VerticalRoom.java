package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.Centroid;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class VerticalRoom extends Room<VerticalRoom.VerticalData> {
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

    @Override
    public VerticalData createUserData(CaveGenContext ctx, RoomData roomData) {
        double pitch = this.pitch.sample(ctx.rand);
        int length = this.length.sample(ctx.rand);
        return new VerticalData(pitch, length);
    }

    @Override
    public Vec3 adjustLocation(CaveGenContext ctx, RoomData roomData, VerticalData userData) {
        double pitch = userData.pitch;
        int length = userData.length;
        return roomData.location()
            .add(roomData.direction().scale(length * roomData.caveRadius() * Math.cos(pitch)))
            .add(0, length * roomData.caveRadius() * Math.sin(-pitch), 0);
    }

    @Override
    public void addCentroids(CaveGenContext ctx, RoomData roomData, VerticalData userData, List<Centroid> centroids) {
        double pitch = userData.pitch;
        int length = userData.length;

        Vec3 moveVec = roomData.direction().scale(roomData.caveRadius() * Math.cos(pitch))
            .add(0, roomData.caveRadius() * Math.sin(-pitch), 0);
        Vec3 pos = roomData.location();
        for (int i = 0; i < length; i++) {
            centroids.add(new Centroid(pos, roomData.caveRadius(), roomData));
            pos = pos.add(moveVec);
        }
    }

    public record VerticalData(
        double pitch,
        int length
    ) {
    }
}
