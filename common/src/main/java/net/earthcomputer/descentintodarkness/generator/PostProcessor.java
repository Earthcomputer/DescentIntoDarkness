package net.earthcomputer.descentintodarkness.generator;

import net.earthcomputer.descentintodarkness.DIDConstants;
import net.earthcomputer.descentintodarkness.generator.painter.PainterStep;
import net.earthcomputer.descentintodarkness.generator.structure.Structure;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PostProcessor {
    private static final int STRUCTURE_CHANCE_ADJUST = 6 * 6;

    public static void postProcess(CaveGenContext ctx, List<List<Vec3>> roomLocations) {
        List<Centroid> centroids = ctx.centroids();
        int roomStart = 0;
        while (roomStart < centroids.size()) {
            int roomIndex = centroids.get(roomStart).roomIndex;
            int roomEnd;
            roomEnd = roomStart;
            while (roomEnd < centroids.size() && centroids.get(roomEnd).roomIndex == roomIndex) {
                roomEnd++;
            }

            List<Centroid> roomCentroids = centroids.subList(roomStart, roomEnd);
            int minRoomY = roomCentroids.stream().mapToInt(centroid -> Mth.floor(centroid.pos.y) - centroid.size).min().orElse(DIDConstants.MIN_Y);
            int maxRoomY = roomCentroids.stream().mapToInt(centroid -> Mth.floor(centroid.pos.y) + centroid.size).max().orElse(DIDConstants.MAX_Y);
            for (Centroid centroid : roomCentroids) {
                smooth(ctx, centroid, minRoomY, maxRoomY);
            }

            roomStart = roomEnd;
        }

        Set<BlockPos> paintedBlocks = new HashSet<>();
        List<BlockPos> paintedBlocksThisCentroid = new ArrayList<>();
        for(Centroid centroid : centroids) {
            for (PainterStep painterStep : ctx.style().painterSteps()) {
                if (painterStep.tags().matches(centroid.tags)) {
                    painterStep.apply(ctx, centroid, pos -> {
                        if (paintedBlocks.contains(pos)) {
                            return false;
                        }
                        paintedBlocksThisCentroid.add(pos);
                        return true;
                    });
                }
            }
            paintedBlocks.addAll(paintedBlocksThisCentroid);
            paintedBlocksThisCentroid.clear();
        }

        for (Structure structure : ctx.style().structures().values()) {
            generateStructure(ctx, centroids, structure);
        }

        if (!centroids.isEmpty()) {
            generatePortal(ctx, centroids.getFirst());
        }

        if (ctx.isDebug()) {
            for (List<Vec3> tunnel : roomLocations) {
                for (int i = 1; i < tunnel.size(); i++) {
                    Vec3 startPos = tunnel.get(i - 1);
                    Vec3 endPos = tunnel.get(i);
                    Vec3 direction = startPos.equals(endPos) ? Vec3.ZERO : endPos.subtract(startPos).normalize();
                    double maxDist = startPos.distanceTo(endPos);
                    for (int dist = 0; dist < maxDist; dist++) {
                        ctx.setBlock(BlockPos.containing(startPos.add(direction.scale(dist))), Blocks.GREEN_STAINED_GLASS.defaultBlockState());
                    }
                }
            }
            for (Centroid centroid : centroids) {
                ctx.setBlock(BlockPos.containing(centroid.pos), Blocks.EMERALD_BLOCK.defaultBlockState());
            }
            for (List<Vec3> tunnel : roomLocations) {
                for (Vec3 roomCenter : tunnel) {
                    ctx.setBlock(BlockPos.containing(roomCenter), Blocks.REDSTONE_BLOCK.defaultBlockState());
                }
            }
        }
    }

    public static void smooth(CaveGenContext ctx, Centroid centroid, int minRoomY, int maxRoomY) {
        int x = Mth.floor(centroid.pos.x);
        int y = Mth.floor(centroid.pos.y);
        int z = Mth.floor(centroid.pos.z);
        int r = centroid.size + 2;

        for(int tx = -r; tx <= r; tx++){
            for(int ty = -r; ty <= r; ty++){
                for(int tz = -r; tz <= r; tz++){
                    if(tx * tx  +  ty * ty  +  tz * tz <= r * r){
                        BlockPos pos = new BlockPos(tx+x, ty+y, tz+z);

                        if(ctx.style().baseBlock() == ctx.getBlock(pos)) {
                            int amt = countTransparent(ctx, pos);
                            if(amt >= 13) {
                                if(ctx.rand.nextInt(100) < 95) {
                                    ctx.setBlock(pos, ctx.style().getAirBlock(pos.getY(), centroid, minRoomY, maxRoomY), centroid);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static int countTransparent(CaveGenContext ctx, BlockPos loc) {
        final int r = 1;
        int count = 0;
        for (int tx = -r; tx <= r; tx++) {
            for (int ty = -r; ty <= r; ty++) {
                for (int tz = -r; tz <= r; tz++) {
                    BlockPos pos = loc.offset(tx, ty, tz);
                    if (ctx.style().isTransparentBlock(ctx, pos)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }



    public static void generateStructure(CaveGenContext ctx, List<Centroid> centroids, Structure structure) {
        if (structure.validDirections().isEmpty()) {
            return;
        }
        for (Centroid centroid : centroids) {
            if (centroid.size <= 0) {
                continue;
            }

            double averageStructures = structure.count() * (centroid.size * centroid.size) / STRUCTURE_CHANCE_ADJUST;
            // compute the number of structures in this centroid using the Poisson distribution
            // https://stackoverflow.com/questions/9832919/generate-poisson-arrival-in-java
            double L = Math.exp(-averageStructures);
            int numStructures = -1;
            double p = 1;
            do {
                p *= ctx.rand.nextDouble();
                numStructures++;
            } while (p > L);

            for (int j = 0; j < numStructures; j++) {
                if (structure.tags().matches(centroid.tags)) {
                    placeStructure(ctx, structure, centroid, false);
                }
            }
        }
    }

    @Nullable
    private static BlockPos placeStructure(CaveGenContext ctx, Structure structure, Centroid centroid, boolean force) {
        List<Direction> validDirections = structure.validDirections();
        if (validDirections.isEmpty()) {
            return null;
        }

        Vec3 vector;
        Direction dir;
        if (structure.shouldSnapToAxis()) {
            dir = validDirections.get(ctx.rand.nextInt(validDirections.size()));
            vector = Vec3.atLowerCornerOf(dir.getNormal().multiply(centroid.size));
        } else {
            // pick a random point on the unit sphere until it's a valid direction
            do {
                vector = new Vec3(ctx.rand.nextGaussian(), ctx.rand.nextGaussian(), ctx.rand.nextGaussian()).normalize().scale(centroid.size);
                dir = Direction.getNearest(vector);
            } while (!validDirections.contains(dir));
        }
        double distanceToWall = Vec3.atLowerCornerOf(dir.getNormal()).dot(vector);
        Vec3 orthogonal = vector.subtract(Vec3.atLowerCornerOf(dir.getNormal()).scale(distanceToWall));
        BlockPos origin = BlockPos.containing(centroid.pos.add(orthogonal));

        BlockPos pos;
        if (dir == Direction.DOWN) {
            pos = PostProcessor.getFloor(ctx, origin, (int) Math.ceil(distanceToWall) + 2);
        } else if (dir == Direction.UP) {
            pos = PostProcessor.getCeiling(ctx, origin, (int) Math.ceil(distanceToWall) + 2);
        } else {
            pos = PostProcessor.getWall(ctx, origin, (int) Math.ceil(distanceToWall) + 2, dir);
        }

        if (!force && !structure.canPlaceOn(ctx, pos)) {
            return null;
        }

        double randomYRotation = ctx.rand.nextInt(4) * (Math.PI / 2);
        ctx.pushTransform(structure.getBlockTransform(randomYRotation, pos, dir), structure.getPositionTransform(randomYRotation, pos, dir));
        boolean placed = structure.place(ctx, pos, centroid, force);
        ctx.popTransform();
        return placed ? pos : null;
    }

    private static void generatePortal(CaveGenContext ctx, Centroid firstCentroid) {
        if (ctx.style().portals().isEmpty()) {
            return;
        }
        List<Structure> portals = new ArrayList<>(ctx.style().portals().values());
        // 100 attempts to place a portal without force (in a nice location)
        for (int i = 0; i < 100; i++) {
            Structure portal = Util.getRandom(portals, ctx.rand);
            BlockPos portalPos = placeStructure(ctx, portal, firstCentroid, false);
            if (portalPos != null) {
                ctx.setSpawnPos(findSpawnPos(ctx, portalPos));
                return;
            }
        }
        // if we can't place a portal, try again with force
        Structure portal = Util.getRandom(portals, ctx.rand);
        BlockPos portalPos = placeStructure(ctx, portal, firstCentroid, true);
        ctx.setSpawnPos(findSpawnPos(ctx, portalPos));
    }

    private static BlockPos findSpawnPos(CaveGenContext ctx, BlockPos startPos) {
        List<BlockPos> appropriateSpawnPositions = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -5; dy <= 5; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    pos.setWithOffset(startPos, dx, dy, dz);
                    if (!ctx.style().isTransparentBlock(ctx, pos.below())
                        && ctx.style().isTransparentBlock(ctx, pos)
                        && ctx.style().isTransparentBlock(ctx, pos.above())) {
                        appropriateSpawnPositions.add(pos.immutable());
                    }
                }
            }
        }
        if (appropriateSpawnPositions.isEmpty()) {
            return startPos;
        }
        int minYDistance = Integer.MAX_VALUE;
        for (BlockPos spawnPos : appropriateSpawnPositions) {
            minYDistance = Math.min(minYDistance, Math.abs(spawnPos.getY() - startPos.getY()));
        }
        int minYDistance_f = minYDistance;
        appropriateSpawnPositions.removeIf(spawnPos -> Math.abs(spawnPos.getY() - startPos.getY()) > minYDistance_f);
        return Util.getRandom(appropriateSpawnPositions, ctx.rand);
    }

    public static boolean isFloor(CaveGenContext ctx, BlockPos pos) {
        return isSolid(ctx, pos) && isSolid(ctx, pos.below()) && !isSolid(ctx, pos.above());
    }

    public static boolean isRoof(CaveGenContext ctx, BlockPos pos) {
        return isSolid(ctx, pos) && !isSolid(ctx, pos.below()) && isSolid(ctx, pos.above());
    }

    public static boolean isSolid(CaveGenContext ctx, BlockPos pos) {
        return !ctx.style().isTransparentBlock(ctx, pos);
    }

    public static BlockPos getWall(CaveGenContext ctx, BlockPos loc, int r, Direction direction) {
        r= (int) (r *1.8);
        BlockPos ret = new BlockPos.MutableBlockPos().set(loc);
        for(int i = 0; i < r; i++) {
            ret = ret.relative(direction);
            if (!ctx.style().isTransparentBlock(ctx, ret)) {
                return ret;
            }
        }
        return ret;
    }

    public static BlockPos getCeiling(CaveGenContext ctx, BlockPos loc, int r) {
        BlockPos ret = loc;
        for(int i = 0; i < r+2; i++) {
            ret = ret.above();
            if (!ctx.style().isTransparentBlock(ctx, ret)) {
                return ret;
            }
        }
        return ret;
    }

    public static BlockPos getFloor(CaveGenContext ctx, BlockPos loc, int r) {
        BlockPos ret = loc;
        for(int i = 0; i < r+2; i++) {
            ret = ret.below();
            if (!ctx.style().isTransparentBlock(ctx, ret)) {
                return ret;
            }
        }
        return ret;
    }
}
