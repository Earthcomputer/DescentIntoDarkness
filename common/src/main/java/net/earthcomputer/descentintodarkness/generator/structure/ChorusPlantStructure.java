package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.Centroid;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.Collection;
import java.util.List;

public final class ChorusPlantStructure extends Structure {
    public static final MapCodec<ChorusPlantStructure> CODEC = RecordCodecBuilder.<ChorusPlantStructure>mapCodec(instance -> instance.group(
        StructureProperties.CODEC.forGetter(StructureProperties::new),
        DIDCodecs.BLOCK_PREDICATE_FROM_PROVIDER.optionalFieldOf("stem_block", Pair.of(BlockStateProvider.simple(Blocks.CHORUS_PLANT), BlockPredicate.matchesBlocks(Blocks.CHORUS_PLANT))).forGetter(structure -> Pair.of(structure.stemBlock, structure.stemPredicate)),
        DIDCodecs.BLOCK_PREDICATE_FROM_PROVIDER.optionalFieldOf("flower_block", Pair.of(BlockStateProvider.simple(Blocks.CHORUS_FLOWER), BlockPredicate.matchesBlocks(Blocks.CHORUS_FLOWER))).forGetter(structure -> Pair.of(structure.flowerBlock, structure.flowerPredicate)),
        DIDCodecs.NON_NEGATIVE_INT_PROVIDER.optionalFieldOf("radius", ConstantInt.of(8)).forGetter(structure -> structure.radius),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("v_length", UniformInt.of(1, 4)).forGetter(structure -> structure.vLength),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("h_length", ConstantInt.of(1)).forGetter(structure -> structure.hLength),
        DIDCodecs.INT_PROVIDER.optionalFieldOf("initial_height_boost", ConstantInt.of(1)).forGetter(structure -> structure.initialHeightBoost),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("branch_factor", UniformInt.of(1, 3)).forGetter(structure -> structure.branchFactor),
        DIDCodecs.INT_PROVIDER.optionalFieldOf("initial_branch_factor_boost", UniformInt.of(1, 1)).forGetter(structure -> structure.initialBranchFactorBoost),
        Codec.doubleRange(0, 1).optionalFieldOf("flower_chance", 0.25).forGetter(structure -> structure.flowerChance),
        Codec.doubleRange(0, 1).optionalFieldOf("initial_flower_chance", 0.2).forGetter(structure -> structure.initialFlowerChance),
        DIDCodecs.NON_NEGATIVE_INT_PROVIDER.optionalFieldOf("num_layers", ConstantInt.of(4)).forGetter(structure -> structure.numLayers)
    ).apply(instance, ChorusPlantStructure::new)).validate(structure ->  {
        if (structure.initialHeightBoost.getMinValue() <= -structure.vLength.getMaxValue()) {
            return DataResult.error(() -> "Initial height boost will always shrink chorus plant below the ground");
        }
        if (structure.initialBranchFactorBoost.getMinValue() <= -structure.branchFactor.getMaxValue()) {
            return DataResult.error(() -> "Initial branch factor boost will always result in a negative branch factor");
        }
        return DataResult.success(structure);
    });

    private final BlockStateProvider stemBlock;
    private final BlockPredicate stemPredicate;
    private final BlockStateProvider flowerBlock;
    private final BlockPredicate flowerPredicate;
    private final IntProvider radius;
    private final IntProvider vLength;
    private final IntProvider hLength;
    private final IntProvider initialHeightBoost;
    private final IntProvider branchFactor;
    private final IntProvider initialBranchFactorBoost;
    private final double flowerChance;
    private final double initialFlowerChance;
    private final IntProvider numLayers;

    private ChorusPlantStructure(StructureProperties props, Pair<BlockStateProvider, BlockPredicate> stemBlock, Pair<BlockStateProvider, BlockPredicate> flowerBlock, IntProvider radius, IntProvider vLength, IntProvider hLength, IntProvider initialHeightBoost, IntProvider branchFactor, IntProvider initialBranchFactorBoost, double flowerChance, double initialFlowerChance, IntProvider numLayers) {
        super(props);
        this.stemBlock = stemBlock.getFirst();
        this.stemPredicate = stemBlock.getSecond();
        this.flowerBlock = flowerBlock.getFirst();
        this.flowerPredicate = flowerBlock.getSecond();
        this.radius = radius;
        this.vLength = vLength;
        this.hLength = hLength;
        this.initialHeightBoost = initialHeightBoost;
        this.branchFactor = branchFactor;
        this.initialBranchFactorBoost = initialBranchFactorBoost;
        this.flowerChance = flowerChance;
        this.initialFlowerChance = initialFlowerChance;
        this.numLayers = numLayers;
    }

    @Override
    protected boolean shouldTransformBlocksByDefault() {
        return true;
    }

    @Override
    protected Direction getDefaultOriginSide(List<StructurePlacementEdge> edges) {
        return Direction.DOWN;
    }

    @Override
    public StructureType<?> type() {
        return StructureType.CHORUS_PLANT.get();
    }

    private boolean canConnectTo(CaveGenContext ctx, BlockPos pos) {
        return stemPredicate.test(ctx.asLevel(), pos) || flowerPredicate.test(ctx.asLevel(), pos) || canPlaceOn(ctx, pos);
    }

    private BlockState withConnectionProperties(CaveGenContext ctx, BlockPos pos, BlockState block) {
        Block blockType = block.getBlock();
        Collection<Property<?>> properties = blockType.getStateDefinition().getProperties();
        if (properties.contains(BlockStateProperties.UP) && canConnectTo(ctx, pos.above())) {
            block = block.setValue(BlockStateProperties.UP, true);
        }
        if (properties.contains(BlockStateProperties.DOWN) && canConnectTo(ctx, pos.below())) {
            block = block.setValue(BlockStateProperties.DOWN, true);
        }
        if (properties.contains(BlockStateProperties.WEST) && canConnectTo(ctx, pos.west())) {
            block = block.setValue(BlockStateProperties.WEST, true);
        }
        if (properties.contains(BlockStateProperties.EAST) && canConnectTo(ctx, pos.east())) {
            block = block.setValue(BlockStateProperties.EAST, true);
        }
        if (properties.contains(BlockStateProperties.NORTH) && canConnectTo(ctx, pos.north())) {
            block = block.setValue(BlockStateProperties.NORTH, true);
        }
        if (properties.contains(BlockStateProperties.SOUTH) && canConnectTo(ctx, pos.south())) {
            block = block.setValue(BlockStateProperties.SOUTH, true);
        }
        return block;
    }

    private boolean isSurroundedByAir(CaveGenContext ctx, BlockPos pos, Direction exceptDirection) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction != exceptDirection && !canReplace(ctx, pos.relative(direction))) {
                return false;
            }
        }

        BlockPos posAbove = pos.above();
        if (stemPredicate.test(ctx.asLevel(), posAbove) || flowerPredicate.test(ctx.asLevel(), posAbove)) {
            return canReplace(ctx, posAbove);
        }

        return true;
    }

    @Override
    public boolean place(CaveGenContext ctx, BlockPos pos, Centroid centroid, boolean force) {
        pos = pos.above();
        if (!canReplace(ctx, pos)
            || !canReplace(ctx, pos.above())
            || !isSurroundedByAir(ctx, pos.above(), null)
        ) {
            return false;
        }

        ctx.setBlock(pos, withConnectionProperties(ctx, pos, ctx.getState(stemBlock, pos, centroid)));
        int radius = this.radius.sample(ctx.rand);
        int numLayers = this.numLayers.sample(ctx.rand);
        generate(ctx, centroid, pos, pos, radius, 0, numLayers, force);
        return true;
    }

    private void generate(CaveGenContext ctx, Centroid centroid, BlockPos pos, BlockPos rootPos, int radius, int layer, int numLayers, boolean force) {
        int length = vLength.sample(ctx.rand);
        if (layer == 0) {
            length += initialHeightBoost.sample(ctx.rand);
        }

        for (int i = 0; i < length; i++) {
            BlockPos offsetPos = pos.above(i + 1);
            if (!force && (!canReplace(ctx, offsetPos) || !isSurroundedByAir(ctx, offsetPos, null))) {
                ctx.setBlock(pos.above(i), flowerBlock, centroid);
                return;
            }

            ctx.setBlock(offsetPos, withConnectionProperties(ctx, offsetPos, ctx.getState(stemBlock, offsetPos, centroid)));
            BlockPos posBelow = offsetPos.below();
            ctx.setBlock(posBelow, withConnectionProperties(ctx, posBelow, ctx.getBlock(posBelow)));
        }

        boolean extended = false;
        if (layer < numLayers) {
            double flowerChanceHere = layer == 0 ? initialFlowerChance : flowerChance;
            if (ctx.rand.nextDouble() >= flowerChanceHere) {
                int branchFactor = this.branchFactor.sample(ctx.rand);
                if (layer == 0) {
                    branchFactor += this.initialBranchFactorBoost.sample(ctx.rand);
                }

                for (int i = 0; i < branchFactor; i++) {
                    Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(ctx.rand);
                    int hLength = this.hLength.sample(ctx.rand);
                    for (int j = 1; j < hLength; j++) {
                        BlockPos offsetPos = pos.above(length).relative(direction, j);
                        if (Math.abs(offsetPos.getX() - rootPos.getX()) < radius
                            && Math.abs(offsetPos.getZ() - rootPos.getZ()) < radius && canReplace(ctx, offsetPos)
                            && canReplace(ctx, offsetPos.below())
                            && isSurroundedByAir(ctx, offsetPos, direction)) {
                            extended = true;
                            ctx.setBlock(offsetPos, withConnectionProperties(ctx, offsetPos, ctx.getState(stemBlock, offsetPos, centroid)));
                            BlockPos posBehind = offsetPos.relative(direction.getOpposite());
                            ctx.setBlock(posBehind, withConnectionProperties(ctx, posBehind, ctx.getBlock(posBehind)));
                            if (j == hLength - 1) {
                                generate(ctx, centroid, offsetPos, rootPos, radius, layer + 1, numLayers, force);
                            }
                        } else {
                            if (j != 1) {
                                ctx.setBlock(offsetPos.relative(direction.getOpposite()), flowerBlock, centroid);
                            }
                            break;
                        }
                    }
                }
            }
        }

        if (!extended) {
            ctx.setBlock(pos.above(length), flowerBlock, centroid);
        }

    }
}
