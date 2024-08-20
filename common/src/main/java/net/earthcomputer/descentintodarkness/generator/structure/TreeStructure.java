package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class TreeStructure extends Structure {
    public static final MapCodec<TreeStructure> CODEC = RecordCodecBuilder.<TreeStructure>mapCodec(instance -> instance.group(
        StructureProperties.CODEC.forGetter(StructureProperties::new),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("log", BlockStateProvider.simple(Blocks.OAK_LOG)).forGetter(structure -> structure.log),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("dirt").forGetter(structure -> structure.dirt),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("height", UniformInt.of(4, 6)).forGetter(structure -> structure.height),
        LeafProperties.CODEC.forGetter(structure -> structure.leafProps),
        VineProperties.CODEC.forGetter(structure -> structure.vineProps),
        CocoaProperties.CODEC.forGetter(structure -> structure.cocoaProps)
    ).apply(instance, TreeStructure::new)).validate(structure -> {
        if (structure.leafProps.leafHeight.getMinValue() > structure.height.getMinValue()) {
            return DataResult.error(() -> "Tree leaves could spawn below the bottom of the trunk");
        }
        return DataResult.success(structure);
    });

    private final BlockStateProvider log;
    private final Optional<BlockStateProvider> dirt;
    private final IntProvider height;
    private final LeafProperties leafProps;
    private final VineProperties vineProps;
    private final CocoaProperties cocoaProps;

    private TreeStructure(StructureProperties props, BlockStateProvider log, Optional<BlockStateProvider> dirt, IntProvider height, LeafProperties leafProps, VineProperties vineProps, CocoaProperties cocoaProps) {
        super(props);
        this.log = log;
        this.dirt = dirt;
        this.height = height;
        this.leafProps = leafProps;
        this.vineProps = vineProps;
        this.cocoaProps = cocoaProps;
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
        return StructureType.TREE.get();
    }

    private record LeafProperties(
        BlockStateProvider leaf,
        IntProvider leafHeight,
        int topLeafRadius,
        int leafStepHeight,
        double cornerLeafChance
    ) {
        static final MapCodec<LeafProperties> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("leaf", BlockStateProvider.simple(Blocks.OAK_LEAVES)).forGetter(structure -> structure.leaf),
            DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("leaf_height", ConstantInt.of(4)).forGetter(LeafProperties::leafHeight),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("top_leaf_radius", 1).forGetter(LeafProperties::topLeafRadius),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("leaf_step_height", 2).forGetter(LeafProperties::leafStepHeight),
            Codec.doubleRange(0, 1).optionalFieldOf("corner_leaf_chance", 0.5).forGetter(LeafProperties::cornerLeafChance)
        ).apply(instance, LeafProperties::new));
    }

    private record VineProperties(
        BlockPredicate vinesCanReplace,
        Optional<BlockStateProvider> trunkVine,
        double trunkVineChance,
        Optional<BlockStateProvider> hangingVine,
        double hangingVineChance
    ) {
        static final MapCodec<VineProperties> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DIDCodecs.BLOCK_PREDICATE.optionalFieldOf("vines_can_replace", BlockPredicate.alwaysTrue()).forGetter(VineProperties::vinesCanReplace),
            vineCodec("trunk_vine").forGetter(VineProperties::trunkVine),
            Codec.doubleRange(0, 1).optionalFieldOf("trunk_vine_chance", 2.0 / 3).forGetter(VineProperties::trunkVineChance),
            vineCodec("hanging_vine").forGetter(VineProperties::hangingVine),
            Codec.doubleRange(0, 1).optionalFieldOf("hanging_vine_chance", 0.25).forGetter(VineProperties::hangingVineChance)
        ).apply(instance, VineProperties::new));

        private static MapCodec<Optional<BlockStateProvider>> vineCodec(String key) {
            return Codec.mapEither(DIDCodecs.BLOCK_STATE_PROVIDER.fieldOf(key), DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("vine")).xmap(
                either -> either.map(Optional::of, Function.identity()),
                trunkVine -> trunkVine.<Either<BlockStateProvider, Optional<BlockStateProvider>>>map(Either::left).orElseGet(() -> Either.right(Optional.empty()))
            );
        }
    }

    private record CocoaProperties(
        Optional<BlockStateProvider> cocoa,
        boolean invertCocoaFacing,
        double cocoaChance,
        int minCocoaTreeHeight
    ) {
        static final MapCodec<CocoaProperties> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("cocoa").forGetter(CocoaProperties::cocoa),
            Codec.BOOL.optionalFieldOf("invert_cocoa_facing", false).forGetter(CocoaProperties::invertCocoaFacing),
            Codec.doubleRange(0, 1).optionalFieldOf("cocoa_chance", 0.2).forGetter(CocoaProperties::cocoaChance),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("min_cocoa_tree_height", 6).forGetter(CocoaProperties::minCocoaTreeHeight)
        ).apply(instance, CocoaProperties::new));
    }
}
