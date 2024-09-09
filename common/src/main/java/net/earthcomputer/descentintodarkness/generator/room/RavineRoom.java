package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.Centroid;
import net.earthcomputer.descentintodarkness.generator.ModuleGenerator;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class RavineRoom extends Room<RavineRoom.RavineData> {
    /**
     * The maximum distance unit spheres can be apart to leave no gaps, if they are arranged in an axis-aligned grid.
     */
    public static final double GAP_FACTOR = 2 / Math.sqrt(3);

    public static final MapCodec<RavineRoom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        tagsCodec(),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("length", UniformInt.of(70, 100)).forGetter(room -> room.length),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("height", UniformInt.of(80, 120)).forGetter(room -> room.height),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("width", UniformInt.of(10, 20)).forGetter(room -> room.width),
        DIDCodecs.NON_NEGATIVE_FLOAT_PROVIDER.optionalFieldOf("turn", UniformFloat.of(0, 30)).forGetter(room -> room.turn),
        Codec.doubleRange(0, 1).optionalFieldOf("height_vary_chance", 0.2).forGetter(room -> room.heightVaryChance)
    ).apply(instance, RavineRoom::new));

    private final IntProvider length;
    private final IntProvider height;
    private final IntProvider width;
    private final FloatProvider turn;
    private final double heightVaryChance;

    private RavineRoom(List<String> tags, IntProvider length, IntProvider height, IntProvider width, FloatProvider turn, double heightVaryChance) {
        super(tags);
        this.length = length;
        this.height = height;
        this.width = width;
        this.turn = turn;
        this.heightVaryChance = heightVaryChance;
    }

    @Override
    public RoomType<?> type() {
        return RoomType.RAVINE.get();
    }

    @Override
    public RavineData createUserData(CaveGenContext ctx, RoomData roomData) {
        int length = this.length.sample(ctx.rand);
        int height = this.height.sample(ctx.rand);
        int width = this.width.sample(ctx.rand);
        double turn = this.turn.sample(ctx.rand);
        if (ctx.rand.nextBoolean()) {
            turn = -turn;
        }
        // move the origin to the center of the ravine
        Vec3 origin = roomData.location().add(roomData.direction().scale(width * 0.5));
        Vec3 entrance = getRandomEntranceLocation(ctx, origin, roomData.direction(), length, height, width, turn);
        // entrance should in fact be at location, move the origin of the ravine
        origin = origin.add(roomData.location().subtract(entrance));
        return new RavineData(length, height, width, turn, origin);
    }

    @Override
    public Vec3 adjustLocation(CaveGenContext ctx, RoomData roomData, RavineData userData) {
        // Get an exit location by getting an entrance location but inverting stuff
        return getRandomEntranceLocation(ctx, userData.origin, roomData.direction().scale(-1), userData.length, userData.height, userData.width, -userData.turn);
    }

    @Override
    public void addCentroids(CaveGenContext ctx, RoomData roomData, RavineData userData, List<Centroid> centroids) {
        int length = userData.length;
        int height = userData.height;
        int width = userData.width;
        double turn = userData.turn;
        Vec3 origin = userData.origin;

        float turnPerBlock = (float) Math.toRadians(turn / length);
        for (int dir : new int[]{-1, 1}) {
            Vec3 localPosition = origin;
            Vec3 localDirection = roomData.direction().yRot(Mth.HALF_PI * dir);
            int distanceSinceCentroids = dir == -1 ? Integer.MAX_VALUE - 1 : 0;

            for (int distance = 0; distance < length / 2; distance++) {
                double localWidth = width * Math.cos((double) distance / length * Math.PI);
                int centroidWidth = Math.max(Math.min((int) Math.ceil(localWidth), 10), 3);
                int centroidRadius = (centroidWidth + 1) / 2;
                double gap = centroidRadius * GAP_FACTOR;
                int numCentroidsAcross = (int) Math.ceil(localWidth / gap);
                int numCentroidsVertically = (int) Math.ceil(height / gap);

                // don't spawn centroids too frequently
                distanceSinceCentroids++;
                if (distanceSinceCentroids > (centroidRadius - 1) * GAP_FACTOR) {
                    distanceSinceCentroids = 0;

                    Vec3 horizontalVector = localDirection.yRot(Mth.HALF_PI);
                    for (int y = 0; y < numCentroidsVertically; y++) {
                        for (int x = 0; x < numCentroidsAcross; x++) {
                            Vec3 centroidPos = localPosition.add(
                                horizontalVector.scale(-localWidth * 0.5 + gap * 0.5 + x * localWidth / numCentroidsAcross)
                            ).add(0, gap * 0.5 + (double) y * height / numCentroidsVertically, 0);
                            centroids.add(new Centroid(centroidPos, centroidRadius, roomData));
                        }
                    }
                }

                localPosition = localPosition.add(localDirection);
                if (ctx.rand.nextDouble() < heightVaryChance) {
                    localPosition = ModuleGenerator.vary(ctx, localPosition);
                }
                localDirection = localDirection.yRot(turnPerBlock * dir);
            }
        }
    }

    private static Vec3 getRandomEntranceLocation(CaveGenContext ctx, Vec3 origin, Vec3 direction, int length,
                                              int height, int width, double turn) {
        Vec3 pos = origin;

        // follow the center of the ravine our chosen distance along it
        final int PROPORTION_OF_LENGTH = 5;
        float turnPerBlock = (float) Math.toRadians(turn / length);
        int distance =
            ctx.rand.nextInt((length + PROPORTION_OF_LENGTH - 1) / PROPORTION_OF_LENGTH) - ctx.rand.nextInt((length + PROPORTION_OF_LENGTH - 1) / PROPORTION_OF_LENGTH);
        Vec3 localDirection = direction.yRot(Math.copySign(Mth.HALF_PI, distance));
        for (int i = 0; i < Math.abs(distance); i++) {
            pos = pos.add(localDirection);
            localDirection = localDirection.yRot(turnPerBlock * Math.signum(distance));
        }

        // move to the edge of the ravine
        double localWidth = width * Math.cos((double) distance / length * Math.PI);
        pos = pos.add(localDirection.scale(localWidth * 0.5).yRot(Math.copySign(Mth.HALF_PI, distance)));

        // pick a random height
        final int PROPORTION_OF_HEIGHT = 5;
        int up =
            height / 2 + ctx.rand.nextInt((height + PROPORTION_OF_HEIGHT - 1) / PROPORTION_OF_HEIGHT) - ctx.rand.nextInt((height + PROPORTION_OF_HEIGHT - 1) / PROPORTION_OF_HEIGHT);
        pos = pos.add(0, up, 0);

        return pos;
    }

    public record RavineData(
        int length,
        int height,
        int width,
        double turn,
        Vec3 origin
    ) {
    }
}
