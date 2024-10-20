package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.earthcomputer.descentintodarkness.DIDUtil;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public final class WaterfallStructure extends Structure {
    public static final MapCodec<WaterfallStructure> CODEC = RecordCodecBuilder.<WaterfallStructure>mapCodec(instance -> instance.group(
        StructureProperties.CODEC.forGetter(StructureProperties::new),
        FluidType.CODEC.fieldOf("fluid_type").forGetter(structure -> structure.fluidType),
        DIDCodecs.BLOCK_PREDICATE_FROM_PROVIDER.optionalFieldOf("block").forGetter(structure -> structure.block.map(block -> Pair.of(block, structure.blockPredicate.orElseThrow()))),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("flow_distance", 8).forGetter(structure -> structure.flowDistance)
    ).apply(instance, WaterfallStructure::new)).validate(structure -> {
        if (structure.fluidType == FluidType.BLOCK) {
            if (structure.block.isEmpty()) {
                return DataResult.error(() -> "Did not specify the block type for a waterfall block fluid");
            }
        } else {
            if (structure.block.isPresent()) {
                return DataResult.error(() -> "Cannot specify the block type for a waterfall non-block fluid");
            }
            if (structure.flowDistance != 8) {
                return DataResult.error(() -> "Cannot specify the flow distance for a waterfall non-block fluid");
            }
        }
        return DataResult.success(structure);
    });

    private final FluidType fluidType;
    private final Optional<BlockStateProvider> block;
    private final Optional<BlockPredicate> blockPredicate;
    private final int flowDistance;

    private WaterfallStructure(StructureProperties props, FluidType fluidType, Optional<Pair<BlockStateProvider, BlockPredicate>> block, int flowDistance) {
        super(props);
        this.fluidType = fluidType;
        this.block = block.map(Pair::getFirst);
        this.blockPredicate = block.map(Pair::getSecond);
        this.flowDistance = flowDistance;
    }

    @Override
    protected boolean shouldTransformPositionByDefault() {
        return false;
    }

    @Override
    public StructureType<?> type() {
        return StructureType.WATERFALL.get();
    }

    @Override
    public boolean place(CaveGenContext ctx, BlockPos pos, int roomIndex, boolean force) {
        if (!force) {
            int wallCount = 0;
            for (Direction dir : DIDUtil.DIRECTIONS) {
                if (canPlaceOn(ctx, pos.relative(dir))) {
                    wallCount++;
                }
            }
            if (wallCount != 5) {
                return false;
            }
        }

        if (ctx.setBlock(pos, fluidType.getBlockProvider(this.block), roomIndex)) {
            Long2IntOpenHashMap blockLevels = new Long2IntOpenHashMap();
            blockLevels.defaultReturnValue(-1);
            simulateFluidTick(ctx, roomIndex, pos, blockLevels);
        }

        return true;
    }

    private void simulateFluidTick(CaveGenContext ctx, int roomIndex, BlockPos pos, Long2IntMap blockLevels) {
        BlockState state = ctx.getBlock(pos);
        int level = getLevel(ctx,pos, state, blockLevels);
        int levelDecrease = fluidType == FluidType.LAVA ? 2 : 1;

        if (level > 0) {
            int minLevel = -100;
            int adjacentSourceBlocks = 0;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos offsetPos = pos.relative(dir);
                int depth = getLevel(ctx, offsetPos, ctx.getBlock(offsetPos), blockLevels);
                if (depth >= 0) {
                    if (depth == 0) {
                        adjacentSourceBlocks++;
                    } else if (depth >= flowDistance) {
                        depth = 0;
                    }
                    minLevel = minLevel >= 0 && depth >= minLevel ? minLevel : depth;
                }
            }

            int newLevel = minLevel + levelDecrease;
            if (newLevel >= flowDistance || minLevel < 0) {
                newLevel = -1;
            }

            BlockPos posAbove = pos.above();
            int depthAbove = getLevel(ctx, posAbove, ctx.getBlock(posAbove), blockLevels);
            if (depthAbove >= 0) {
                if (depthAbove >= flowDistance) {
                    newLevel = depthAbove;
                } else {
                    newLevel = depthAbove + flowDistance;
                }
            }

            if (adjacentSourceBlocks >= 2 && fluidType == FluidType.WATER) {
                BlockState stateBelow = ctx.getBlock(pos.below());
                if (!canReplace(ctx, pos.below())) {
                    newLevel = 0;
                } else if (stateBelow.is(Blocks.WATER) && stateBelow.getValue(LiquidBlock.LEVEL) == 0) {
                    newLevel = 0;
                }
            }

            if (newLevel != level) {
                level = newLevel;

                if (newLevel < 0) {
                    ctx.setBlock(pos, Blocks.AIR.defaultBlockState());
                } else {
                    if (setLevel(ctx, roomIndex, pos, newLevel, blockLevels)) {
                        simulateFluidTick(ctx, roomIndex, pos, blockLevels);
                    }
                }
            }
        }

        BlockPos posBelow = pos.below();
        BlockState blockBelow = ctx.getBlock(posBelow);
        if (canFlowInto(ctx, posBelow, blockBelow)) {
            // skipped: trigger mix effects

            if (level >= flowDistance) {
                tryFlowInto(ctx, roomIndex, posBelow, blockBelow, level, blockLevels);
            } else {
                tryFlowInto(ctx, roomIndex, posBelow, blockBelow, level + flowDistance, blockLevels);
            }
        } else if (level >= 0 && (level == 0 || isBlocked(ctx, posBelow))) {
            Set<Direction> flowDirs = getPossibleFlowDirections(ctx, pos, level, blockLevels);
            int newLevel = level + levelDecrease;
            if (level >= flowDistance) {
                newLevel = 1;
            }
            if (newLevel >= flowDistance) {
                return;
            }
            for (Direction flowDir : flowDirs) {
                BlockPos offsetPos = pos.relative(flowDir);
                tryFlowInto(ctx, roomIndex, offsetPos, ctx.getBlock(offsetPos), newLevel, blockLevels);
            }
        }
    }

    private int getLevel(CaveGenContext ctx, BlockPos pos, BlockState state, Long2IntMap blockLevels) {
        if (!fluidType.getBlockPredicate(blockPredicate).test(ctx.asLevel(), pos)) {
            return -1;
        }
        if (fluidType == FluidType.BLOCK || fluidType == FluidType.SNOW_LAYER) {
            return blockLevels.getOrDefault(pos.asLong(), 0);
        } else {
            if (fluidType == FluidType.WATER && state.getBlock() != Blocks.WATER) {
                return -1;
            }
            if (fluidType == FluidType.LAVA && state.getBlock() != Blocks.LAVA) {
                return -1;
            }
            return state.getValue(LiquidBlock.LEVEL);
        }
    }

    private boolean setLevel(CaveGenContext ctx, int roomIndex, BlockPos pos, int level, Long2IntMap blockLevels) {
        if (fluidType == FluidType.BLOCK) {
            return blockLevels.put(pos.asLong(), level) != level | ctx.setBlock(pos, fluidType.getBlockProvider(block), roomIndex);
        } else if (fluidType == FluidType.SNOW_LAYER) {
            BlockPos posBelow = pos.below();
            if (ctx.getBlock(posBelow).is(Blocks.SNOW)) {
                ctx.setBlock(posBelow, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 8));
            }
            int layers = level == 0 ? 8 : 9 - (int) Math.ceil((double) level / flowDistance * 8);
            if (layers <= 0) layers = 1;
            else if (layers > 8) layers = 8;
            if (ctx.getBlock(pos.above()).is(Blocks.SNOW)) {
                layers = 8;
            }
            return blockLevels.put(pos.asLong(), level) != level | ctx.setBlock(pos, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers));
        } else if (fluidType == FluidType.WATER) {
            return ctx.setBlock(pos, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, level));
        } else {
            return ctx.setBlock(pos, Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, level));
        }
    }

    private boolean canFlowInto(CaveGenContext ctx, BlockPos pos, BlockState block) {
        return !this.fluidType.getBlockPredicate(this.blockPredicate).test(ctx.asLevel(), pos) && !block.is(Blocks.LAVA) && !isBlocked(ctx, pos);
    }

    private void tryFlowInto(CaveGenContext ctx, int roomIndex, BlockPos pos, BlockState block, int level, Long2IntMap blockLevels) {
        if (!canFlowInto(ctx, pos, block)) {
            return;
        }

        // skipped: trigger mix effects and block dropping
        if (setLevel(ctx, roomIndex, pos, level, blockLevels)) {
            simulateFluidTick(ctx, roomIndex, pos, blockLevels);
        }
    }

    private boolean isBlocked(CaveGenContext ctx, BlockPos pos) {
        return !canReplace(ctx, pos) && !this.fluidType.getBlockPredicate(this.blockPredicate).test(ctx.asLevel(), pos);
    }

    private Set<Direction> getPossibleFlowDirections(CaveGenContext ctx, BlockPos pos, int level, Long2IntMap blockLevels) {
        int minDistanceToLower = Integer.MAX_VALUE;
        Set<Direction> flowDirs = EnumSet.noneOf(Direction.class);

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos offsetPos = pos.relative(dir);
            BlockState offsetState = ctx.getBlock(offsetPos);

            if (!isBlocked(ctx, offsetPos) && (!this.fluidType.getBlockPredicate(this.blockPredicate).test(ctx.asLevel(), offsetPos) || getLevel(ctx, offsetPos, offsetState, blockLevels) > 0)) {
                int distanceToLower;
                BlockPos posBelow = offsetPos.below();
                if (isBlocked(ctx, posBelow)) {
                    distanceToLower = getDistanceToLower(ctx, offsetPos, 1, dir.getOpposite(), blockLevels);
                } else {
                    distanceToLower = 0;
                }

                if (distanceToLower < minDistanceToLower) {
                    flowDirs.clear();
                }

                if (distanceToLower <= minDistanceToLower) {
                    flowDirs.add(dir);
                    minDistanceToLower = distanceToLower;
                }
            }
        }

        return flowDirs;
    }

    private int getDistanceToLower(CaveGenContext ctx, BlockPos pos, int distance, Direction excludingDir, Long2IntMap blockLevels) {
        int minDistanceToLower = Integer.MAX_VALUE;

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (dir == excludingDir) {
                continue;
            }

            BlockPos offsetPos = pos.relative(dir);
            BlockState offsetState = ctx.getBlock(offsetPos);

            if (!isBlocked(ctx, offsetPos) && (this.fluidType.getBlockPredicate(this.blockPredicate).test(ctx.asLevel(), offsetPos) || getLevel(ctx, offsetPos, offsetState, blockLevels) > 0)) {
                if (!isBlocked(ctx, offsetPos.below())) {
                    return distance;
                }

                if (distance < getSlopeFindDistance()) {
                    int distanceToLower = getDistanceToLower(ctx, offsetPos, distance + 1, dir.getOpposite(), blockLevels);
                    if (distanceToLower < minDistanceToLower) {
                        minDistanceToLower = distanceToLower;
                    }
                }
            }
        }

        return minDistanceToLower;
    }

    private int getSlopeFindDistance() {
        return fluidType == FluidType.LAVA ? 2 : 4;
    }

    private enum FluidType implements StringRepresentable {
        WATER("water"),
        LAVA("lava"),
        SNOW_LAYER("snow_layer"),
        BLOCK("block"),
        ;

        public static final Codec<FluidType> CODEC = StringRepresentable.fromEnum(FluidType::values);

        private final String name;

        FluidType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public BlockStateProvider getBlockProvider(Optional<BlockStateProvider> block) {
            return switch (this) {
                case WATER -> BlockStateProvider.simple(Blocks.WATER);
                case LAVA -> BlockStateProvider.simple(Blocks.LAVA);
                case SNOW_LAYER -> BlockStateProvider.simple(Blocks.SNOW);
                case BLOCK -> block.orElseThrow();
            };
        }

        public BlockPredicate getBlockPredicate(Optional<BlockPredicate> blockPredicate) {
            return switch (this) {
                case WATER -> BlockPredicate.matchesBlocks(Blocks.WATER);
                case LAVA -> BlockPredicate.matchesBlocks(Blocks.LAVA);
                case SNOW_LAYER -> BlockPredicate.matchesBlocks(Blocks.SNOW);
                case BLOCK -> blockPredicate.orElseThrow();
            };
        }
    }
}
