package net.earthcomputer.descentintodarkness.generator;

import net.earthcomputer.descentintodarkness.generator.room.RoomData;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class Centroid {
    public Vec3 pos;
    public final int size;
    public final List<String> tags;
    public final int roomIndex;

    public Centroid(Vec3 pos, int size, List<String> tags, int roomIndex) {
        this.pos = pos;
        this.size = size;
        this.tags = tags;
        this.roomIndex = roomIndex;
    }

    public Centroid(Vec3 pos, int size, RoomData roomData) {
        this(pos, size, roomData.tags(), roomData.roomIndex());
    }
}
