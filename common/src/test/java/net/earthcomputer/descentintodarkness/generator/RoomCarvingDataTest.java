package net.earthcomputer.descentintodarkness.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RoomCarvingDataTest {
    @Test
    public void testOneBlock() {
        RoomCarvingData carvingData = new RoomCarvingData();
        carvingData.setAir(10, BlockPos.ZERO);
        assertCarvingData(carvingData, 10, Direction.WEST, -1, 0, 0);
        assertCarvingData(carvingData, 10, Direction.EAST, 1, 0, 0);
        assertCarvingData(carvingData, 10, Direction.DOWN, 0, -1, 0);
        assertCarvingData(carvingData, 10, Direction.UP, 0, 1, 0);
        assertCarvingData(carvingData, 10, Direction.NORTH, 0, 0, -1);
        assertCarvingData(carvingData, 10, Direction.SOUTH, 0, 0, 1);
    }

    @Test
    public void testTwoBlocks() {
        RoomCarvingData carvingData = new RoomCarvingData();
        carvingData.setAir(10, BlockPos.ZERO);
        carvingData.setAir(10, new BlockPos(0, 1, 0));
        assertCarvingData(carvingData, 10, Direction.WEST, -1, 0, 0, -1, 1, 0);
        assertCarvingData(carvingData, 10, Direction.EAST, 1, 0, 0, 1, 1, 0);
        assertCarvingData(carvingData, 10, Direction.DOWN, 0, -1, 0);
        assertCarvingData(carvingData, 10, Direction.UP, 0, 2, 0);
        assertCarvingData(carvingData, 10, Direction.NORTH, 0, 0, -1, 0, 1, -1);
        assertCarvingData(carvingData, 10, Direction.SOUTH, 0, 0, 1, 0, 1, 1);
    }

    @Test
    public void testOneBlockTwoRooms() {
        RoomCarvingData carvingData = new RoomCarvingData();
        carvingData.setAir(10, BlockPos.ZERO);
        carvingData.setAir(20, BlockPos.ZERO);
        assertCarvingData(carvingData, 10, Direction.WEST, -1, 0, 0);
        assertCarvingData(carvingData, 20, Direction.WEST, -1, 0, 0);
    }

    @Test
    public void testAdjacentRooms() {
        RoomCarvingData carvingData = new RoomCarvingData();
        carvingData.setAir(10, BlockPos.ZERO);
        carvingData.setAir(20, new BlockPos(0, 1, 0));
        assertCarvingData(carvingData, 10, Direction.DOWN, 0, -1, 0);
        assertCarvingData(carvingData, 20, Direction.UP, 0, 2, 0);
        assertCarvingData(carvingData, 10, Direction.UP);
        assertCarvingData(carvingData, 20, Direction.DOWN);
    }

    @Test
    public void testRefillWithWall() {
        RoomCarvingData carvingData = new RoomCarvingData();
        carvingData.setAir(10, BlockPos.ZERO);
        carvingData.setWall(10, BlockPos.ZERO);
        assertCarvingData(carvingData, 10, Direction.WEST);
        assertCarvingData(carvingData, 10, Direction.EAST);
        assertCarvingData(carvingData, 10, Direction.DOWN);
        assertCarvingData(carvingData, 10, Direction.UP);
        assertCarvingData(carvingData, 10, Direction.NORTH);
        assertCarvingData(carvingData, 10, Direction.SOUTH);
    }

    @Test
    public void testRefillWithWallOverlappingRooms() {
        RoomCarvingData carvingData = new RoomCarvingData();
        carvingData.setAir(10, BlockPos.ZERO);
        carvingData.setAir(10, new BlockPos(0, 1, 0));
        carvingData.setAir(20, new BlockPos(0, 1, 0));
        carvingData.setAir(20, new BlockPos(0, 2, 0));
        carvingData.setWall(10, new BlockPos(0, 1, 0));
        assertCarvingData(carvingData, 20, Direction.WEST, -1, 2, 0);
        assertCarvingData(carvingData, 20, Direction.DOWN);
    }

    private static void assertCarvingData(RoomCarvingData roomCarvingData, int roomIndex, Direction side, int... expectedCoords) {
        if (expectedCoords.length % 3 != 0) {
            throw new IllegalArgumentException("Expected coords must be a list of 3d coordinates");
        }
        List<BlockPos> expectedCoordsList = new ArrayList<>(expectedCoords.length / 3);
        for (int i = 0; i < expectedCoords.length; i += 3) {
            expectedCoordsList.add(new BlockPos(expectedCoords[i], expectedCoords[i + 1], expectedCoords[i + 2]));
        }

        List<BlockPos> actualCoords = new ArrayList<>();
        roomCarvingData.forEachWallInRoom(roomIndex, side, pos -> actualCoords.add(pos.immutable()));

        assertEquals(expectedCoordsList, actualCoords);
    }
}
