package net.earthcomputer.descentintodarkness.generator;

import com.mojang.serialization.Codec;
import net.earthcomputer.descentintodarkness.DIDUtil;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.List;

public enum PlacementEdge implements StringRepresentable {
    FLOOR("floor", Direction.DOWN),
    CEILING("ceiling", Direction.UP),
    WALL("wall", Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST),
    NORTH("north", Direction.NORTH),
    SOUTH("south", Direction.SOUTH),
    WEST("west", Direction.WEST),
    EAST("east", Direction.EAST),
    ;

    public static final Codec<PlacementEdge> CODEC = StringRepresentable.fromEnum(PlacementEdge::values);

    private final String name;
    private final Direction[] directions;

    PlacementEdge(String name, Direction... directions) {
        this.name = name;
        this.directions = directions;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static List<Direction> directions(List<PlacementEdge> edges) {
        if (edges.isEmpty()) {
            return List.of(DIDUtil.DIRECTIONS);
        }

        return edges.stream().flatMap(edge -> Arrays.stream(edge.directions)).distinct().toList();
    }
}
