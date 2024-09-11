package net.earthcomputer.descentintodarkness.generator;

import net.earthcomputer.descentintodarkness.DIDConstants;
import net.earthcomputer.descentintodarkness.generator.room.Room;
import net.earthcomputer.descentintodarkness.generator.room.RoomData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class ModuleGenerator {
    private ModuleGenerator() {
    }

    public static void read(CaveGenContext ctx, LayoutGenerator.Layout layout, Vec3 start, Vec3 dir, int caveRadius, List<List<Vec3>> roomLocations) {
        List<Vec3> theseRoomLocations = new ArrayList<>();
        roomLocations.add(theseRoomLocations);

        addRooms(ctx, layout, start, dir, caveRadius, roomLocations, theseRoomLocations);

        ctx.listener().setProgress((float) CaveGenerator.STEP_CARVING_CENTROIDS / CaveGenerator.TOTAL_STEPS);
        ctx.listener().pushProgress(Component.translatable("descent_into_darkness.generating.carving_centroids", ctx.centroids().size()));
        int carvedCentroidCount = 0;
        int roomStart = 0;
        while (roomStart < ctx.centroids().size()) {
            int roomIndex = ctx.centroids().get(roomStart).roomIndex;
            int roomEnd;
            roomEnd = roomStart;
            while (roomEnd < ctx.centroids().size() && ctx.centroids().get(roomEnd).roomIndex == roomIndex) {
                roomEnd++;
            }

            List<Centroid> roomCentroids = ctx.centroids().subList(roomStart, roomEnd);
            int minRoomY = roomCentroids.stream().mapToInt(centroid -> Mth.floor(centroid.pos.y) - centroid.size).min().orElse(DIDConstants.MIN_Y);
            int maxRoomY = roomCentroids.stream().mapToInt(centroid -> Mth.floor(centroid.pos.y) + centroid.size).max().orElse(DIDConstants.MAX_Y);
            for (Centroid centroid : roomCentroids) {
                ctx.listener().setProgress((float) carvedCentroidCount++ / ctx.centroids().size());
                deleteCentroid(ctx, centroid, minRoomY, maxRoomY);
            }

            roomStart = roomEnd;
        }
        ctx.listener().popProgress();
    }

    private static <D> void addRooms(CaveGenContext ctx, LayoutGenerator.Layout layout, Vec3 start, Vec3 dir, int caveRadius, List<List<Vec3>> roomLocations, List<Vec3> theseRoomLocations) {
        String cave = layout.getValue();
        ctx.listener().setProgress((float) CaveGenerator.STEP_CREATING_ROOMS / CaveGenerator.TOTAL_STEPS);
        ctx.listener().pushProgress(Component.translatable("descent_into_darkness.generating.creating_rooms", cave.length()));
        Map<Character, Room<?>> rooms = ctx.style().rooms();
        Vec3 location = start;
        for (int i = 0; i < cave.length(); i++) {
            ctx.listener().setProgress((float) i / cave.length());
            @SuppressWarnings("unchecked")
            Room<D> room = (Room<D>) rooms.get(cave.charAt(i));
            List<String> tags = new ArrayList<>(layout.getTags().get(i));
            tags.addAll(room.tags());
            int roomIndex = ctx.centroids().isEmpty() ? 0 : ctx.centroids().getLast().roomIndex + 1;
            RoomData roomData = new RoomData(location, dir, caveRadius, tags, roomLocations, roomIndex);
            D userData = room.createUserData(ctx, roomData);
            room.addCentroids(ctx, roomData, userData, ctx.centroids());
            dir = room.adjustDirection(ctx, roomData, userData);
            roomData = roomData.withDirection(dir);
            location = room.adjustLocation(ctx, roomData, userData);
            theseRoomLocations.add(location);
        }
        ctx.listener().popProgress();
    }

    private static void deleteCentroid(CaveGenContext ctx, Centroid centroid, int minRoomY, int maxRoomY) {
        BlockPos centroidPos = BlockPos.containing(centroid.pos);
        int r = centroid.size;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for(int ty = -r; ty <= r; ty++) {
            BlockStateProvider airBlock = ctx.style().getAirBlock(ty + centroidPos.getY(), centroid, minRoomY, maxRoomY);
            for(int tx = -r; tx <= r; tx++){
                for(int tz = -r; tz <= r; tz++){
                    if(tx * tx  +  ty * ty  +  tz * tz <= r * r){
                        if (((tx != 0 || ty != 0) && (tx != 0 || tz != 0) && (ty != 0 || tz != 0)) || (Math.abs(tx + ty + tz) != r)) {
                            ctx.setBlock(pos.setWithOffset(centroidPos, tx, ty, tz), airBlock, centroid);
                        }
                    }
                }
            }
        }
    }

    public static Vec3 vary(CaveGenContext ctx, Vec3 loc) {
        int x = ctx.style().centroidVaryHorizontal().sample(ctx.rand);
        int y = ctx.style().centroidVaryVertical().sample(ctx.rand);
        int z = ctx.style().centroidVaryHorizontal().sample(ctx.rand);
        return loc.add(x,y,z);
    }

    public static int generateOreCluster(CaveGenContext ctx, Centroid centroid, BlockPos loc, int radius, Predicate<BlockPos> oldBlocks, BlockStateProvider ore) {
        int count = 0;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for(int tx = -radius; tx< radius +1; tx++){
            for(int ty = -radius; ty< radius +1; ty++){
                for(int tz = -radius; tz< radius +1; tz++){
                    if(tx * tx  +  ty * ty  +  tz * tz <= (radius - 2) * (radius - 2)) {
                        if(ty+loc.getY() > 0) {
                            pos.setWithOffset(loc, tx, ty, tz);
                            if(oldBlocks.test(pos)) {
                                if(((tx == 0 && ty == 0) || (tx == 0 && tz == 0) || (ty == 0 && tz == 0)) && (Math.abs(tx+ty+tz) == radius - 2)) {
                                    if(ctx.rand.nextBoolean())
                                        continue;
                                }
                                ctx.setBlock(pos, ore, centroid);
                                count++;
                            }

                        }
                    }
                }
            }
        }

        return count;
    }
}
