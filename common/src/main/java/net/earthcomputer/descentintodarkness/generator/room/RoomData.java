package net.earthcomputer.descentintodarkness.generator.room;

import net.minecraft.world.phys.Vec3;

import java.util.List;

public record RoomData(
    Vec3 location,
    Vec3 direction,
    int caveRadius,
    List<String> tags,
    List<List<Vec3>> roomLocations,
    int roomIndex
) {
    public RoomData withLocation(Vec3 location) {
        return new RoomData(location, direction, caveRadius, tags, roomLocations, roomIndex);
    }

    public RoomData withDirection(Vec3 direction) {
        return new RoomData(location, direction, caveRadius, tags, roomLocations, roomIndex);
    }

    public RoomData withCaveRadius(int caveRadius) {
        return new RoomData(location, direction, caveRadius, tags, roomLocations, roomIndex);
    }
}
