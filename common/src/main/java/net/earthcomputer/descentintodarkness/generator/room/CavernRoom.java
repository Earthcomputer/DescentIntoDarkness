package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.DIDUtil;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.Centroid;
import net.earthcomputer.descentintodarkness.generator.ModuleGenerator;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class CavernRoom extends Room<List<Centroid>> {
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

    CavernRoom(List<String> tags, IntProvider centroids, int minSpread, int maxSpread, IntProvider centroidSizeVariance, FloatProvider turn) {
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

    @Override
    public List<Centroid> createUserData(CaveGenContext ctx, RoomData roomData) {
        List<Centroid> centroids = new ArrayList<>();

        int count = this.centroids.sample(ctx.rand);

        int spread = roomData.caveRadius() - 1;
        if (spread < minSpread) {
            spread = minSpread;
        } else if (spread > maxSpread) {
            spread = maxSpread;
        }

        for (int i = 0; i < count; i++) {
            int tx = ctx.rand.nextInt(spread) + 2;
            int ty = ctx.rand.nextInt(spread + 2);
            int tz = ctx.rand.nextInt(spread) + 2;

            if (ctx.rand.nextBoolean()) {
                tx = -tx;
            }
            if (ctx.rand.nextBoolean()) {
                tz = -tz;
            }

            int sizeMod = centroidSizeVariance.sample(ctx.rand);
            if (ctx.rand.nextBoolean()) {
                sizeMod = -sizeMod;
            }

            centroids.add(new Centroid(roomData.location().add(tx, ty, tz), spread + sizeMod, roomData));
        }

        if (count > 0) {
            double minDot = Double.POSITIVE_INFINITY;
            Vec3 minPos = null;
            for (Centroid centroid : centroids) {
                double dot = centroid.pos.dot(roomData.direction());
                if (dot < minDot) {
                    minDot = dot;
                    minPos = centroid.pos;
                }
            }
            assert minPos != null;

            Vec3 shift = roomData.location().subtract(minPos);
            for (Centroid centroid : centroids) {
                centroid.pos = centroid.pos.add(shift);
            }

            DIDUtil.ensureConnected(centroids, roomData.caveRadius(), pos -> new Centroid(pos, roomData.caveRadius(), roomData));
        }

        return centroids;
    }

    @Override
    public Vec3 adjustDirection(CaveGenContext ctx, RoomData roomData, List<Centroid> userData) {
        double angle = turn.sample(ctx.rand);
        if (ctx.rand.nextBoolean()) {
            angle = -angle;
        }
        return roomData.direction().yRot((float) Math.toRadians(angle));
    }

    @Override
    public Vec3 adjustLocation(CaveGenContext ctx, RoomData roomData, List<Centroid> centroids) {
        if (!centroids.isEmpty()) {
            double maxDot = Double.NEGATIVE_INFINITY;
            Vec3 maxPos = null;
            for (Centroid centroid : centroids) {
                double dot  = centroid.pos.dot(roomData.direction());
                if (dot > maxDot) {
                    maxDot = dot;
                    maxPos = centroid.pos;
                }
            }
            assert maxPos != null;

            return ModuleGenerator.vary(ctx, maxPos).add(roomData.direction().scale(roomData.caveRadius()));
        }

        return super.adjustLocation(ctx, roomData, centroids);
    }

    @Override
    public void addCentroids(CaveGenContext ctx, RoomData roomData, List<Centroid> userData, List<Centroid> centroids) {
        centroids.addAll(userData);
    }
}
