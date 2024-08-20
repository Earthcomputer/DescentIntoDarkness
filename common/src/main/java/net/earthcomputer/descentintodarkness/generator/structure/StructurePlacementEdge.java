package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

public enum StructurePlacementEdge implements StringRepresentable {
    FLOOR("floor", Direction.DOWN),
    CEILING("ceiling", Direction.UP),
    WALL("wall", Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST),
    NORTH("north", Direction.NORTH),
    SOUTH("south", Direction.SOUTH),
    WEST("west", Direction.WEST),
    EAST("east", Direction.EAST),
    ;

    public static final Codec<StructurePlacementEdge> CODEC = StringRepresentable.fromEnum(StructurePlacementEdge::values);

    private final String name;
    private final Direction[] directions;

    StructurePlacementEdge(String name, Direction... directions) {
        this.name = name;
        this.directions = directions;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public Direction[] directions() {
        return directions;
    }
}
