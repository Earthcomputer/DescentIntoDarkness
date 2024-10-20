package net.earthcomputer.descentintodarkness.generator;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.earthcomputer.descentintodarkness.DIDConstants;
import net.earthcomputer.descentintodarkness.DIDUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class RoomCarvingData {
    private final Long2IntMap airBlocks = new Long2IntOpenHashMap();
    private final Long2IntMap roomByWall = new Long2IntOpenHashMap();
    private final Long2ObjectMap<IntSet> roomByWallMulti = new Long2ObjectOpenHashMap<>();
    private final List<EnumMap<Direction, LongSet>> edges = new ArrayList<>();
    private final IntList minRoomY = new IntArrayList();
    private final IntList maxRoomY = new IntArrayList();
    @Nullable
    private List<EnumMap<Direction, LongList>> randomAccessEdges;

    public RoomCarvingData() {
        roomByWall.defaultReturnValue(-1);
    }

    private void addRoomToWall(int roomIndex, long wallPos) {
        IntSet multiSet = roomByWallMulti.computeIfAbsent(wallPos, new Long2ObjectFunction<>() {
            @Override
            public IntSet get(long key) {
                return new IntArraySet();
            }

            @Override
            public boolean containsKey(long key) {
                return roomByWall.remove(key) != -1;
            }
        });
        if (multiSet != null) {
            multiSet.add(roomIndex);
        } else {
            roomByWall.put(wallPos, roomIndex);
        }
    }

    private void removeRoomFromWall(int roomIndex, long wallPos) {
        IntSet multiSet = roomByWallMulti.get(wallPos);
        if (multiSet != null) {
            multiSet.remove(roomIndex);
            if (multiSet.size() == 1) {
                roomByWallMulti.remove(roomIndex);
                int remainingRoom = multiSet.iterator().nextInt();
                roomByWall.put(wallPos, remainingRoom);
            }
        } else {
            roomByWall.remove(wallPos);
        }
    }

    private void forEachRoomWithWall(long wallPos, IntConsumer action) {
        int roomIndex = roomByWall.get(wallPos);
        if (roomIndex != -1) {
            action.accept(roomIndex);
        } else {
            IntSet multiSet = roomByWallMulti.get(wallPos);
            if (multiSet != null) {
                multiSet.forEach(action);
            }
        }
    }

    private EnumMap<Direction, LongSet> getRoomEdges(int roomIndex) {
        while (edges.size() <= roomIndex) {
            var map = new EnumMap<Direction, LongSet>(Direction.class);
            for (Direction dir : DIDUtil.DIRECTIONS) {
                map.put(dir, new LongLinkedOpenHashSet());
            }
            edges.add(map);
        }
        return edges.get(roomIndex);
    }

    public void setAir(int roomIndex, BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        long airPos = pos.asLong();

        airBlocks.putIfAbsent(airPos, roomIndex);

        var roomEdges = getRoomEdges(roomIndex);
        for (Direction dir : DIDUtil.DIRECTIONS) {
            long wallPos = BlockPos.asLong(x + dir.getStepX(), y + dir.getStepY(), z + dir.getStepZ());
            if (!airBlocks.containsKey(wallPos)) {
                roomEdges.get(dir).add(wallPos);
                addRoomToWall(roomIndex, wallPos);
            }
        }

        forEachRoomWithWall(airPos, room -> {
            var roomEdges2 = edges.get(room);
            for (Direction dir : DIDUtil.DIRECTIONS) {
                roomEdges2.get(dir).remove(airPos);
            }
        });
        roomByWall.remove(airPos);
        roomByWallMulti.remove(airPos);

        if (minRoomY.size() <= roomIndex) {
            while (minRoomY.size() < roomIndex) {
                minRoomY.add(Integer.MAX_VALUE);
            }
            minRoomY.add(y);
        } else {
            minRoomY.set(roomIndex, Math.min(y, minRoomY.getInt(roomIndex)));
        }

        if (maxRoomY.size() <= roomIndex) {
            while (maxRoomY.size() < roomIndex) {
                maxRoomY.add(Integer.MIN_VALUE);
            }
            maxRoomY.add(y);
        } else {
            maxRoomY.set(roomIndex, Math.max(y, maxRoomY.getInt(roomIndex)));
        }
    }

    public void setWall(int roomIndex, BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        long wallPos = pos.asLong();

        airBlocks.remove(wallPos);

        var roomEdges = getRoomEdges(roomIndex);
        for (Direction dir : DIDUtil.DIRECTIONS) {
            long neighborPos = BlockPos.asLong(x + dir.getStepX(), y + dir.getStepY(), z + dir.getStepZ());
            if (airBlocks.containsKey(neighborPos)) {
                roomEdges.get(dir.getOpposite()).add(wallPos);
            } else {
                forEachRoomWithWall(neighborPos, room -> {
                    var roomEdges2 = edges.get(room);
                    roomEdges2.get(dir).remove(neighborPos);
                    boolean wallHasAnyEdges = false;
                    for (Direction dir2 : DIDUtil.DIRECTIONS) {
                        if (roomEdges2.get(dir2).contains(neighborPos)) {
                            wallHasAnyEdges = true;
                            break;
                        }
                    }
                    if (!wallHasAnyEdges) {
                        removeRoomFromWall(room, neighborPos);
                    }
                });
            }
        }

        addRoomToWall(roomIndex, wallPos);
    }

    public void forEachWallInRoom(int roomIndex, Direction side, Consumer<BlockPos> action) {
        if (roomIndex >= edges.size()) {
            return;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        LongIterator itr = edges.get(roomIndex).get(side).iterator();
        while (itr.hasNext()) {
            long posLong = itr.nextLong();
            action.accept(pos.set(BlockPos.getX(posLong), BlockPos.getY(posLong), BlockPos.getZ(posLong)));
        }
    }

    @Nullable
    public Wall getRandomWall(int roomIndex, RandomSource rand, List<Direction> validSides) {
        if (randomAccessEdges == null) {
            throw new IllegalStateException("Cannot get random wall before carving");
        }

        var roomEdges = randomAccessEdges.get(roomIndex);
        if (roomEdges == null) {
            return null;
        }

        int totalWeight = 0;
        for (Direction side : validSides) {
            totalWeight += roomEdges.get(side).size();
        }
        if (totalWeight <= 0) {
            return null;
        }

        int wallIndex = rand.nextInt(totalWeight);
        for (Direction side : validSides) {
            LongList wallsOnSide = roomEdges.get(side);
            if (wallIndex < wallsOnSide.size()) {
                long wallPos = wallsOnSide.getLong(wallIndex);
                return new Wall(new BlockPos(BlockPos.getX(wallPos), BlockPos.getY(wallPos), BlockPos.getZ(wallPos)), side);
            }
            wallIndex -= wallsOnSide.size();
        }

        return null;
    }

    public int getMinRoomY(int roomIndex) {
        if (roomIndex >= minRoomY.size()) {
            return DIDConstants.MIN_Y;
        }
        int minY = minRoomY.getInt(roomIndex);
        return minY == Integer.MAX_VALUE ? DIDConstants.MIN_Y : minY;
    }

    public int getMaxRoomY(int roomIndex) {
        if (roomIndex >= maxRoomY.size()) {
            return DIDConstants.MAX_Y;
        }
        int maxY = maxRoomY.getInt(roomIndex);
        return maxY == Integer.MIN_VALUE ? DIDConstants.MAX_Y : maxY;
    }

    public boolean isAir(BlockPos pos) {
        return airBlocks.containsKey(pos.asLong());
    }

    public void carve(CaveGenContext ctx) {
        ctx.listener().setProgress((float) CaveGenerator.STEP_CARVING / CaveGenerator.TOTAL_STEPS);
        ctx.listener().pushProgress(Component.translatable("descent_into_darkness.generating.carving", airBlocks.size()));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int carvedBlocks = 0;
        for (Long2IntMap.Entry entry : airBlocks.long2IntEntrySet()) {
            ctx.listener().setProgress((float) carvedBlocks++ / airBlocks.size());
            long posLong = entry.getLongKey();
            int roomIndex = entry.getIntValue();
            pos.set(BlockPos.getX(posLong), BlockPos.getY(posLong), BlockPos.getZ(posLong));

            BlockStateProvider airBlock = ctx.style().getAirBlock(pos.getY(), ctx.rooms().get(roomIndex).tags(), getMinRoomY(roomIndex), getMaxRoomY(roomIndex));
            ctx.setBlock(pos, airBlock, roomIndex);
        }
        ctx.listener().popProgress();

        randomAccessEdges = edges.stream().map(roomEdges -> {
            EnumMap<Direction, LongList> randomAccessRoomEdges = new EnumMap<>(Direction.class);
            roomEdges.forEach((direction, edges) -> {
                LongList walls = new LongArrayList(edges.size());
                edges.forEach(walls::add);
                randomAccessRoomEdges.put(direction, walls);
            });
            return randomAccessRoomEdges;
        }).toList();
    }

    public record Wall(BlockPos pos, Direction side) {
    }
}
