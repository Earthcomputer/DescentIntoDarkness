package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.DIDUtil;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.Centroid;
import net.earthcomputer.descentintodarkness.generator.ModuleGenerator;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.ConstantInt;
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

    private final Room<?> smallRoom;
    private final Room<?> largeRoom;

    private ShelfRoom(List<String> tags, IntProvider shelfHeight, IntProvider shelfSize) {
        super(tags);
        this.shelfHeight = shelfHeight;
        this.shelfSize = shelfSize;

        smallRoom = new CavernRoom(tags, UniformInt.of(4, 7), 4, Integer.MAX_VALUE, ConstantInt.of(0), UniformFloat.of(0, 90));
        largeRoom = new CavernRoom(tags, UniformInt.of(3, 7), 3, Integer.MAX_VALUE, UniformInt.of(0, 1), UniformFloat.of(0, 90));
    }

    @Override
    public RoomType<?> type() {
        return RoomType.SHELF.get();
    }

    @Override
    public ShelfData createUserData(CaveGenContext ctx, RoomData roomData) {
        List<Centroid> centroids = new ArrayList<>();
        Vec3 newLocation;
        if (ctx.rand.nextBoolean()) {
            newLocation = generateFromBottom(ctx, roomData, centroids);
        } else {
            newLocation = generateFromTop(ctx, roomData, centroids);
        }
        DIDUtil.ensureConnected(centroids, roomData.caveRadius(), pos -> new Centroid(pos, roomData.caveRadius(), roomData));
        return new ShelfData(newLocation, centroids);
    }

    private Vec3 generateFromBottom(CaveGenContext ctx, RoomData roomData, List<Centroid> centroids) {
        Vec3 next = roomData.location();
        next = generateRoom(largeRoom, ctx, roomData.withLocation(next), centroids);
        next = generateRoom(smallRoom, ctx, roomData.withLocation(next), centroids);

        Vec3 shelf = roomData.location().add(0, shelfHeight.sample(ctx.rand), 0);
        int dir = ctx.rand.nextBoolean() ? 1 : -1;
        shelf = shelf.add(roomData.direction().yRot(Mth.HALF_PI + ctx.rand.nextFloat() * Mth.PI / 18 * dir));

        int shelfRadius = Math.max(roomData.caveRadius(), 5);
        int shelfSize = this.shelfSize.sample(ctx.rand);
        for (int i = 0; i < shelfSize; i++) {
            shelf = generateRoom(smallRoom, ctx, roomData.withLocation(shelf).withCaveRadius(shelfRadius), centroids);
            shelf = ModuleGenerator.vary(ctx, shelf);
            shelf = shelf.add(roomData.direction().scale(shelfRadius));
        }

        return next;
    }

    private Vec3 generateFromTop(CaveGenContext ctx, RoomData roomData, List<Centroid> centroids) {
        Vec3 shelf = roomData.location().add(0, shelfHeight.sample(ctx.rand), 0);
        int dir = ctx.rand.nextBoolean() ? 1 : -1;
        shelf = shelf.add(roomData.direction().yRot(Mth.HALF_PI + ctx.rand.nextFloat() * Mth.PI / 18 * dir));

        int shelfRadius = Math.max(roomData.caveRadius(), 5);
        int shelfSize = this.shelfSize.sample(ctx.rand);
        Vec3 next = roomData.location();
        Vec3 newLocation = roomData.location();
        for (int i = 0; i < shelfSize; i++) {
            next = generateRoom(smallRoom, ctx, roomData.withLocation(next).withCaveRadius(shelfRadius), centroids);
            next = ModuleGenerator.vary(ctx, next);
            newLocation = next;
            next = next.add(roomData.direction().scale(shelfRadius));
        }

        shelf = generateRoom(largeRoom, ctx, roomData.withLocation(shelf), centroids);
        shelf = generateRoom(smallRoom, ctx, roomData.withLocation(shelf), centroids);

        return newLocation;
    }

    private <D> Vec3 generateRoom(Room<D> room, CaveGenContext ctx, RoomData roomData, List<Centroid> centroids) {
        D userData = room.createUserData(ctx, roomData);
        room.addCentroids(ctx, roomData, userData, centroids);
        Vec3 direction = room.adjustDirection(ctx, roomData, userData);
        return room.adjustLocation(ctx, roomData.withDirection(direction), userData);
    }

    @Override
    public Vec3 adjustLocation(CaveGenContext ctx, RoomData roomData, ShelfData userData) {
        return userData.newLocation;
    }

    @Override
    public void addCentroids(CaveGenContext ctx, RoomData roomData, ShelfData userData, List<Centroid> centroids) {
        centroids.addAll(userData.centroids);
    }

    public record ShelfData(
        Vec3 newLocation,
        List<Centroid> centroids
    ) {
    }
}
