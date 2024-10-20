package net.earthcomputer.descentintodarkness.generator;

import net.earthcomputer.descentintodarkness.generator.room.Room;
import net.earthcomputer.descentintodarkness.generator.room.RoomData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class ModuleGenerator {
    private ModuleGenerator() {
    }

    public static void read(CaveGenContext ctx, LayoutGenerator.Layout layout, Vec3 start, Vec3 dir, int caveRadius) {
        addRooms(ctx, layout, start, dir, caveRadius);
    }

    private static <D> void addRooms(CaveGenContext ctx, LayoutGenerator.Layout layout, Vec3 start, Vec3 dir, int caveRadius) {
        record CreatedRoom<D>(Room<D> room, RoomData roomData, D userData) {
        }

        String cave = layout.getValue();
        ctx.listener().setProgress((float) CaveGenerator.STEP_CREATING_ROOMS / CaveGenerator.TOTAL_STEPS);
        ctx.listener().pushProgress(Component.translatable("descent_into_darkness.generating.creating_rooms", cave.length()));
        Map<Character, Room<?>> rooms = ctx.style().rooms();
        Vec3 location = start;

        List<CreatedRoom<D>> createdRooms = new ArrayList<>();
        for (int i = 0; i < cave.length(); i++) {
            ctx.listener().setProgress((float) i / cave.length());
            @SuppressWarnings("unchecked")
            Room<D> room = (Room<D>) rooms.get(cave.charAt(i));
            List<String> tags = new ArrayList<>(layout.getTags().get(i));
            tags.addAll(room.tags());
            int roomIndex = ctx.rooms().size();
            RoomData roomData = new RoomData(location, dir, caveRadius, tags, roomIndex, i == cave.length() - 1);
            D userData = room.createUserData(ctx, roomData);
            dir = room.adjustDirection(ctx, roomData, userData);
            roomData = roomData.withDirection(dir);
            location = room.adjustLocation(ctx, roomData, userData);
            ctx.rooms().add(roomData);
            createdRooms.add(new CreatedRoom<>(room, roomData, userData));
        }
        ctx.listener().popProgress();

        ctx.listener().setProgress((float) CaveGenerator.STEP_APPLYING_ROOMS / CaveGenerator.TOTAL_STEPS);
        ctx.listener().pushProgress(Component.translatable("descent_into_darkness.generating.applying_rooms", cave.length()));
        for (int i = 0; i < createdRooms.size(); i++) {
            ctx.listener().setProgress((float) i / cave.length());
            CreatedRoom<D> createdRoom = createdRooms.get(i);
            createdRoom.room.apply(ctx.roomCarvingData(), ctx, createdRoom.roomData, createdRoom.userData);
        }
        ctx.listener().popProgress();
    }

    public static Vec3 vary(CaveGenContext ctx, Vec3 loc) {
        int x = ctx.style().centroidVaryHorizontal().sample(ctx.rand);
        int y = ctx.style().centroidVaryVertical().sample(ctx.rand);
        int z = ctx.style().centroidVaryHorizontal().sample(ctx.rand);
        return loc.add(x,y,z);
    }

    public static int generateOreCluster(CaveGenContext ctx, int roomIndex, BlockPos loc, int radius, Predicate<BlockPos> oldBlocks, BlockStateProvider ore) {
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
                                ctx.setBlock(pos, ore, roomIndex);
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
