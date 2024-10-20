package net.earthcomputer.descentintodarkness.generator;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.earthcomputer.descentintodarkness.DIDUtil;
import net.earthcomputer.descentintodarkness.generator.painter.PainterStep;
import net.earthcomputer.descentintodarkness.generator.room.RoomData;
import net.earthcomputer.descentintodarkness.generator.structure.Structure;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class PostProcessor {
    private static final double SMOOTHING_CHANCE = 0.95;
    private static final int SMOOTHING_THRESHOLD = 5;

    public static void postProcess(CaveGenContext ctx) {
        smooth(ctx);
        ctx.roomCarvingData().carve(ctx);
        paint(ctx);

        ctx.listener().setProgress((float) CaveGenerator.STEP_GENERATING_STRUCTURES / CaveGenerator.TOTAL_STEPS);
        ctx.listener().pushProgress(Component.translatable("descent_into_darkness.generating.structures", ctx.style().structures().size()));
        int generatedStructures = 0;
        for (var structureEntry : ctx.style().structures().entrySet()) {
            ctx.listener().setProgress((float) generatedStructures++ / ctx.style().structures().size());
            ctx.listener().pushProgress(Component.translatable("descent_into_darkness.generating.structure", structureEntry.getKey(), ctx.rooms().size()));
            generateStructure(ctx, structureEntry.getValue());
            ctx.listener().popProgress();
        }
        ctx.listener().popProgress();

        generatePortal(ctx);

        if (ctx.isDebug()) {
            for (int roomIndex = 1; roomIndex < ctx.rooms().size(); roomIndex++) {
                RoomData prevRoom = ctx.rooms().get(roomIndex - 1);
                RoomData room = ctx.rooms().get(roomIndex);
                if (!prevRoom.isBranchEnd()) {
                    Vec3 direction = prevRoom.location().equals(room.location()) ? Vec3.ZERO : room.location().subtract(prevRoom.location()).normalize();
                    double maxDist = prevRoom.location().distanceTo(room.location());
                    for (int dist = 0; dist < maxDist; dist++) {
                        ctx.setBlock(BlockPos.containing(prevRoom.location().add(direction.scale(dist))), Blocks.GREEN_STAINED_GLASS.defaultBlockState());
                    }
                }
            }
            for (RoomData room : ctx.rooms()) {
                ctx.setBlock(BlockPos.containing(room.location()), Blocks.EMERALD_BLOCK.defaultBlockState());
            }
        }
    }

    private static void paint(CaveGenContext ctx) {
        ctx.listener().setProgress((float) CaveGenerator.STEP_PAINTING / CaveGenerator.TOTAL_STEPS);
        List<PainterStep> painterSteps = ctx.style().painterSteps();
        ctx.listener().pushProgress(Component.translatable("descent_into_darkness.generating.painting", painterSteps.size()));
        LongSet paintedBlocks = new LongOpenHashSet();
        for (int painterStepIndex = 0; painterStepIndex < painterSteps.size(); painterStepIndex++) {
            ctx.listener().setProgress((float) painterStepIndex / painterSteps.size());
            PainterStep painterStep = painterSteps.get(painterStepIndex);
            ctx.listener().pushProgress(Component.translatable("descent_into_darkness.generating.painting.step", ctx.rooms().size()));
            for (int roomIndex = 0; roomIndex < ctx.rooms().size(); roomIndex++) {
                ctx.listener().setProgress((float) roomIndex / ctx.rooms().size());
                int roomIndex_f = roomIndex;
                RoomData room = ctx.rooms().get(roomIndex);
                if (painterStep.tags().matches(room.tags())) {
                    for (Direction side : painterStep.sides()) {
                        ctx.roomCarvingData().forEachWallInRoom(roomIndex, side, wallPos -> {
                            if (ctx.rand.nextDouble() < painterStep.chance()) {
                                BlockPos posToPaint = wallPos.relative(side, painterStep.depth());
                                if (paintedBlocks.add(posToPaint.asLong())) {
                                    painterStep.apply(ctx, posToPaint, side, roomIndex_f);
                                }
                            }
                        });
                    }
                }
            }
            ctx.listener().popProgress();
        }
        ctx.listener().popProgress();
    }

    private static void smooth(CaveGenContext ctx) {
        ctx.listener().setProgress((float) CaveGenerator.STEP_SMOOTHING_CALCULATING / CaveGenerator.TOTAL_STEPS);
        ctx.listener().pushProgress(Component.translatable("descent_into_darkness.generating.smoothing_calculating", ctx.rooms().size()));
        LongSet alreadySmoothed = new LongOpenHashSet();
        Long2IntMap blocksToSetToAir = new Long2IntOpenHashMap();
        for (RoomData room : ctx.rooms()) {
            ctx.listener().setProgress((float) room.roomIndex() / ctx.rooms().size());
            for (Direction side : DIDUtil.DIRECTIONS) {
                ctx.roomCarvingData().forEachWallInRoom(room.roomIndex(), side, wallPos -> {
                    if (alreadySmoothed.add(wallPos.asLong())) {
                        int neighborAirCount = 0;
                        for (Direction dir : DIDUtil.DIRECTIONS) {
                            if (ctx.roomCarvingData().isAir(wallPos.relative(dir))) {
                                neighborAirCount++;
                            }
                        }
                        if (neighborAirCount >= SMOOTHING_THRESHOLD && ctx.rand.nextDouble() < SMOOTHING_CHANCE) {
                            blocksToSetToAir.put(wallPos.asLong(), room.roomIndex());
                        }
                    }
                });
            }
        }
        ctx.listener().popProgress();

        ctx.listener().setProgress((float) CaveGenerator.STEP_SMOOTHING_APPLYING / CaveGenerator.TOTAL_STEPS);
        ctx.listener().pushProgress(Component.translatable("descent_into_darkness.generating.smoothing_applying", blocksToSetToAir.size()));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int appliedBlocks = 0;
        for (Long2IntMap.Entry entry : blocksToSetToAir.long2IntEntrySet()) {
            ctx.listener().setProgress((float) appliedBlocks++ / blocksToSetToAir.size());
            long blockLong = entry.getLongKey();
            int roomIndex = entry.getIntValue();
            pos.set(BlockPos.getX(blockLong), BlockPos.getY(blockLong), BlockPos.getZ(blockLong));
            ctx.roomCarvingData().setAir(roomIndex, pos);
        }
        ctx.listener().popProgress();
    }


    public static void generateStructure(CaveGenContext ctx, Structure structure) {
        if (structure.sides().isEmpty()) {
            return;
        }
        for (int roomIndex = 0; roomIndex < ctx.rooms().size(); roomIndex++) {
            ctx.listener().setProgress((float) roomIndex / ctx.rooms().size());
            RoomData room = ctx.rooms().get(roomIndex);

            // compute the number of structures in this centroid using the Poisson distribution
            // https://stackoverflow.com/questions/9832919/generate-poisson-arrival-in-java
            double L = Math.exp(-structure.count());
            int numStructures = -1;
            double p = 1;
            do {
                p *= ctx.rand.nextDouble();
                numStructures++;
            } while (p > L);

            for (int j = 0; j < numStructures; j++) {
                if (structure.tags().matches(room.tags())) {
                    placeStructure(ctx, structure, roomIndex, false);
                }
            }
        }
    }

    @Nullable
    private static BlockPos placeStructure(CaveGenContext ctx, Structure structure, int roomIndex, boolean force) {
        BlockPos wallPos;
        Direction side;
        if (structure.shouldSnapToAxis()) {
            side = Util.getRandom(structure.sides(), ctx.rand);
            BlockPos pos = BlockPos.containing(ctx.rooms().get(roomIndex).location());
            for (int i = 0; i < 256; i++) {
                if (structure.canPlaceOn(ctx, pos)) {
                    break;
                }
                pos = pos.relative(side);
            }
            wallPos = pos;
        } else {
            RoomCarvingData.Wall wall = ctx.roomCarvingData().getRandomWall(roomIndex, ctx.rand, structure.sides());
            if (wall == null) {
                return null;
            }
            wallPos = wall.pos();
            side = wall.side();
        }

        if (!force && !structure.canPlaceOn(ctx, wallPos)) {
            return null;
        }

        BlockPos pos = wallPos.relative(side, structure.depth());

        double randomYRotation = ctx.rand.nextInt(4) * (Math.PI / 2);
        ctx.pushTransform(structure.getBlockTransform(randomYRotation, pos, side), structure.getPositionTransform(randomYRotation, pos, side));
        boolean placed = structure.place(ctx, pos, roomIndex, force);
        ctx.popTransform();
        return placed ? pos : null;
    }

    private static void generatePortal(CaveGenContext ctx) {
        if (ctx.style().portals().isEmpty() || ctx.rooms().isEmpty()) {
            return;
        }
        List<Structure> portals = new ArrayList<>(ctx.style().portals().values());
        // 100 attempts to place a portal without force (in a nice location)
        for (int i = 0; i < 100; i++) {
            Structure portal = Util.getRandom(portals, ctx.rand);
            BlockPos portalPos = placeStructure(ctx, portal, 0, false);
            if (portalPos != null) {
                ctx.setSpawnPos(findSpawnPos(ctx, portalPos));
                return;
            }
        }
        // if we can't place a portal, try again with force
        Structure portal = Util.getRandom(portals, ctx.rand);
        BlockPos portalPos = placeStructure(ctx, portal, 0, true);
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
}
