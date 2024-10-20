package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.ModuleGenerator;
import net.earthcomputer.descentintodarkness.generator.RoomCarvingData;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class ShelfRoom extends Room<ShelfRoom.ShelfData> {
    public static final MapCodec<ShelfRoom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        tagsCodec(),
        DIDCodecs.INT_PROVIDER.optionalFieldOf("shelf_height", UniformInt.of(6, 10)).forGetter(room -> room.shelfHeight),
        DIDCodecs.NON_NEGATIVE_INT_PROVIDER.optionalFieldOf("shelf_size", ConstantInt.of(3)).forGetter(room -> room.shelfSize)
    ).apply(instance, ShelfRoom::new));

    private final IntProvider shelfHeight;
    private final IntProvider shelfSize;

    private static final IntProvider SMALL_NUM_CENTROIDS = UniformInt.of(4, 7);
    private static final int SMALL_MIN_SPREAD = 4;
    private static final int SMALL_MAX_SPREAD = Integer.MAX_VALUE;
    private static final IntProvider SMALL_CENTROID_SIZE_VARIANCE = ConstantInt.of(0);
    private static final IntProvider LARGE_NUM_CENTROIDS = UniformInt.of(3, 7);
    private static final int LARGE_MIN_SPREAD = 3;
    private static final int LARGE_MAX_SPREAD = Integer.MAX_VALUE;
    private static final IntProvider LARGE_CENTROID_SIZE_VARIANCE = UniformInt.of(0, 1);
    private static final FloatProvider TURN = UniformFloat.of(0, 90);

    private ShelfRoom(List<String> tags, IntProvider shelfHeight, IntProvider shelfSize) {
        super(tags);
        this.shelfHeight = shelfHeight;
        this.shelfSize = shelfSize;
    }

    @Override
    public RoomType<?> type() {
        return RoomType.SHELF.get();
    }

    @Override
    public ShelfData createUserData(CaveGenContext ctx, RoomData roomData) {
        List<CavernRoom.Sphere> spheres = new ArrayList<>();
        Vec3 newLocation;
        if (ctx.rand.nextBoolean()) {
            newLocation = generateFromBottom(ctx, roomData, spheres);
        } else {
            newLocation = generateFromTop(ctx, roomData, spheres);
        }
        CavernRoom.ensureConnected(spheres, roomData.caveRadius());
        return new ShelfData(newLocation, spheres);
    }

    private Vec3 generateFromBottom(CaveGenContext ctx, RoomData roomData, List<CavernRoom.Sphere> spheres) {
        Vec3 next = roomData.location();
        next = generateRoom(ctx, next, roomData.direction(), roomData.caveRadius(), LARGE_NUM_CENTROIDS, LARGE_MIN_SPREAD, LARGE_MAX_SPREAD, LARGE_CENTROID_SIZE_VARIANCE, spheres);
        next = generateRoom(ctx, next, roomData.direction(), roomData.caveRadius(), SMALL_NUM_CENTROIDS, SMALL_MIN_SPREAD, SMALL_MAX_SPREAD, SMALL_CENTROID_SIZE_VARIANCE, spheres);

        Vec3 shelf = roomData.location().add(0, shelfHeight.sample(ctx.rand), 0);
        int dir = ctx.rand.nextBoolean() ? 1 : -1;
        shelf = shelf.add(roomData.direction().yRot(Mth.HALF_PI + ctx.rand.nextFloat() * Mth.PI / 18 * dir));

        int shelfRadius = Math.max(roomData.caveRadius(), 5);
        int shelfSize = this.shelfSize.sample(ctx.rand);
        for (int i = 0; i < shelfSize; i++) {
            shelf = generateRoom(ctx, shelf, roomData.direction(), shelfRadius, SMALL_NUM_CENTROIDS, SMALL_MIN_SPREAD, SMALL_MAX_SPREAD, SMALL_CENTROID_SIZE_VARIANCE, spheres);
            shelf = ModuleGenerator.vary(ctx, shelf);
            shelf = shelf.add(roomData.direction().scale(shelfRadius));
        }

        return next;
    }

    private Vec3 generateFromTop(CaveGenContext ctx, RoomData roomData, List<CavernRoom.Sphere> spheres) {
        Vec3 shelf = roomData.location().add(0, shelfHeight.sample(ctx.rand), 0);
        int dir = ctx.rand.nextBoolean() ? 1 : -1;
        shelf = shelf.add(roomData.direction().yRot(Mth.HALF_PI + ctx.rand.nextFloat() * Mth.PI / 18 * dir));

        int shelfRadius = Math.max(roomData.caveRadius(), 5);
        int shelfSize = this.shelfSize.sample(ctx.rand);
        Vec3 next = roomData.location();
        Vec3 newLocation = roomData.location();
        for (int i = 0; i < shelfSize; i++) {
            next = generateRoom(ctx, next, roomData.direction(), shelfRadius, SMALL_NUM_CENTROIDS, SMALL_MIN_SPREAD, SMALL_MAX_SPREAD, SMALL_CENTROID_SIZE_VARIANCE, spheres);
            next = ModuleGenerator.vary(ctx, next);
            newLocation = next;
            next = next.add(roomData.direction().scale(shelfRadius));
        }

        shelf = generateRoom(ctx, shelf, roomData.direction(), roomData.caveRadius(), LARGE_NUM_CENTROIDS, LARGE_MIN_SPREAD, LARGE_MAX_SPREAD, LARGE_CENTROID_SIZE_VARIANCE, spheres);
        shelf = generateRoom(ctx, shelf, roomData.direction(), roomData.caveRadius(), SMALL_NUM_CENTROIDS, SMALL_MIN_SPREAD, SMALL_MAX_SPREAD, SMALL_CENTROID_SIZE_VARIANCE, spheres);

        return newLocation;
    }

    private static Vec3 generateRoom(CaveGenContext ctx, Vec3 location, Vec3 direction, int caveRadius, IntProvider numCentroids, int minSpread, int maxSpread, IntProvider centroidSizeVariance, List<CavernRoom.Sphere> spheres) {
        List<CavernRoom.Sphere> newSpheres = CavernRoom.createSpheres(ctx, location, direction, caveRadius, numCentroids, minSpread, maxSpread, centroidSizeVariance);
        spheres.addAll(newSpheres);

        double angle = TURN.sample(ctx.rand);
        if (ctx.rand.nextBoolean()) {
            angle = -angle;
        }
        direction = direction.yRot((float) Math.toRadians(angle));

        return CavernRoom.adjustLocation(ctx, location, direction, caveRadius, newSpheres);
    }

    @Override
    public Vec3 adjustLocation(CaveGenContext ctx, RoomData roomData, ShelfData userData) {
        return userData.newLocation;
    }

    @Override
    public void apply(RoomCarvingData carvingData, CaveGenContext ctx, RoomData roomData, ShelfData userData) {
        for (CavernRoom.Sphere sphere : userData.spheres) {
            SimpleRoom.applySphere(carvingData, sphere.center(), sphere.radius(), roomData.roomIndex());
        }
    }

    public record ShelfData(
        Vec3 newLocation,
        List<CavernRoom.Sphere> spheres
    ) {
    }
}
