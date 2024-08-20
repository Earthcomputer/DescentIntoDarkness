package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.Direction;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.List;

public final class ChorusPlantStructure extends Structure {
    public static final MapCodec<ChorusPlantStructure> CODEC = RecordCodecBuilder.<ChorusPlantStructure>mapCodec(instance -> instance.group(
        StructureProperties.CODEC.forGetter(StructureProperties::new),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("stem_block", BlockStateProvider.simple(Blocks.CHORUS_PLANT)).forGetter(structure -> structure.stemBlock),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("flower_block", BlockStateProvider.simple(Blocks.CHORUS_FLOWER)).forGetter(structure -> structure.flowerBlock),
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
    private final BlockStateProvider flowerBlock;
    private final IntProvider radius;
    private final IntProvider vLength;
    private final IntProvider hLength;
    private final IntProvider initialHeightBoost;
    private final IntProvider branchFactor;
    private final IntProvider initialBranchFactorBoost;
    private final double flowerChance;
    private final double initialFlowerChance;
    private final IntProvider numLayers;

    private ChorusPlantStructure(StructureProperties props, BlockStateProvider stemBlock, BlockStateProvider flowerBlock, IntProvider radius, IntProvider vLength, IntProvider hLength, IntProvider initialHeightBoost, IntProvider branchFactor, IntProvider initialBranchFactorBoost, double flowerChance, double initialFlowerChance, IntProvider numLayers) {
        super(props);
        this.stemBlock = stemBlock;
        this.flowerBlock = flowerBlock;
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
}
