package net.earthcomputer.descentintodarkness.generator.room;

import net.minecraft.world.phys.Vec3;

import java.util.List;

public record RoomData(
    Vec3 location,
    Vec3 direction,
    int caveRadius,
    List<String> tags,
    int roomIndex,
    boolean isBranchEnd
) {
    public RoomData withDirection(Vec3 direction) {
        return new RoomData(location, direction, caveRadius, tags, roomIndex, isBranchEnd);
    }
}
