package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.ModuleGenerator;
import net.earthcomputer.descentintodarkness.generator.RoomCarvingData;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public final class CavernRoom extends Room<CavernRoom.CavernData> {
    public static final MapCodec<CavernRoom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        tagsCodec(),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("spheres", UniformInt.of(4, 7)).forGetter(room -> room.centroids),
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
    public CavernData createUserData(CaveGenContext ctx, RoomData roomData) {
        return new CavernData(createSpheres(ctx, roomData.location(), roomData.direction(), roomData.caveRadius(), centroids, minSpread, maxSpread, centroidSizeVariance));
    }

    public static List<Sphere> createSpheres(CaveGenContext ctx, Vec3 location, Vec3 direction, int caveRadius, IntProvider numCentroids, int minSpread, int maxSpread, IntProvider centroidSizeVariance) {
        List<Sphere> spheres = new ArrayList<>();

        int count = numCentroids.sample(ctx.rand);

        int spread = caveRadius - 1;
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

            spheres.add(new Sphere(location.add(tx, ty, tz), spread + sizeMod));
        }

        if (count > 0) {
            double minDot = Double.POSITIVE_INFINITY;
            Vec3 minPos = null;
            for (Sphere centroid : spheres) {
                double dot = centroid.center.dot(direction);
                if (dot < minDot) {
                    minDot = dot;
                    minPos = centroid.center;
                }
            }
            assert minPos != null;

            Vec3 shift = location.subtract(minPos);
            spheres.replaceAll(sphere -> sphere.shift(shift));

            ensureConnected(spheres, caveRadius);
        }

        return spheres;
    }

    @Override
    public Vec3 adjustDirection(CaveGenContext ctx, RoomData roomData, CavernData userData) {
        double angle = turn.sample(ctx.rand);
        if (ctx.rand.nextBoolean()) {
            angle = -angle;
        }
        return roomData.direction().yRot((float) Math.toRadians(angle));
    }

    @Override
    public Vec3 adjustLocation(CaveGenContext ctx, RoomData roomData, CavernData userData) {
        return adjustLocation(ctx, roomData.location(), roomData.direction(), roomData.caveRadius(), userData.spheres);
    }

    public static Vec3 adjustLocation(CaveGenContext ctx, Vec3 location, Vec3 direction, int caveRadius, List<Sphere> spheres) {
        if (!spheres.isEmpty()) {
            double maxDot = Double.NEGATIVE_INFINITY;
            Vec3 maxPos = null;
            for (Sphere sphere : spheres) {
                double dot  = sphere.center.dot(direction);
                if (dot > maxDot) {
                    maxDot = dot;
                    maxPos = sphere.center;
                }
            }
            assert maxPos != null;

            return ModuleGenerator.vary(ctx, maxPos).add(direction.scale(caveRadius));
        }

        return ModuleGenerator.vary(ctx, location).add(direction.scale(caveRadius));
    }

    @Override
    public void apply(RoomCarvingData carvingData, CaveGenContext ctx, RoomData roomData, CavernData userData) {
        for (Sphere sphere : userData.spheres) {
            SimpleRoom.applySphere(carvingData, sphere.center, sphere.radius, roomData.roomIndex());
        }
    }

    public static void ensureConnected(List<Sphere> spheresInOut, int connectingCentroidRadius) {
        // find the minimum spanning tree of the spheres using Kruskal's algorithm, to ensure they are connected
        List<Pair<Sphere, Sphere>> edges = new ArrayList<>();
        for (int i = 0; i < spheresInOut.size() - 1; i++) {
            for (int j = i + 1; j < spheresInOut.size(); j++) {
                edges.add(Pair.of(spheresInOut.get(i), spheresInOut.get(j)));
            }
        }
        edges.sort(Comparator.comparingDouble(edge -> edge.getLeft().center.distanceTo(edge.getRight().center) - edge.getLeft().radius - edge.getRight().radius));
        Map<Sphere, Integer> nodeGroups = new HashMap<>();
        int groupCount = 0;
        ListIterator<Pair<Sphere, Sphere>> edgesItr = edges.listIterator();
        while (edgesItr.hasNext()) {
            Pair<Sphere, Sphere> edge = edgesItr.next();
            Integer groupA = nodeGroups.get(edge.getLeft());
            Integer groupB = nodeGroups.get(edge.getRight());
            if (groupA != null && groupA.equals(groupB)) {
                edgesItr.remove();
            } else {
                if (groupA != null && groupB != null) {
                    nodeGroups.replaceAll((key, val) -> val.equals(groupB) ? groupA : val);
                } else {
                    Integer group = groupA == null ? groupB == null ? groupCount++ : groupB : groupA;
                    nodeGroups.put(edge.getLeft(), group);
                    nodeGroups.put(edge.getRight(), group);
                }
            }
        }

        for (Pair<Sphere, Sphere> edge : edges) {
            double distance = edge.getLeft().center.distanceTo(edge.getRight().center);
            double actualDistance = distance - edge.getLeft().radius - edge.getRight().radius;
            if (actualDistance < 0) {
                continue;
            }
            actualDistance = Math.max(1, actualDistance);
            Vec3 dir = edge.getRight().center.subtract(edge.getLeft().center).scale(1.0 / distance);

            int numSegments = (int) Math.ceil(actualDistance / connectingCentroidRadius) + 1;
            double segmentLength = actualDistance / numSegments;

            Vec3 startPos = edge.getLeft().center.add(dir.scale(edge.getLeft().radius));
            Vec3 segment = dir.scale(segmentLength);
            for (int i = 1; i < numSegments; i++) {
                spheresInOut.add(new Sphere(startPos.add(segment.scale(i)), connectingCentroidRadius));
            }
        }
    }

    public record Sphere(Vec3 center, int radius) {
        private Sphere shift(Vec3 amount) {
            return new Sphere(center.add(amount), radius);
        }
    }

    public record CavernData(List<Sphere> spheres) {
    }
}
