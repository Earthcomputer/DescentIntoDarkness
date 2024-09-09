package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.DIDConstants;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.Centroid;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    @Override
    public boolean place(CaveGenContext ctx, BlockPos pos, Centroid centroid, boolean force) {
        pos = pos.above();

        int trunkHeight = this.height.sample(ctx.rand);

        // check whether the tree can generate
        if (!force && (pos.getY() < DIDConstants.MIN_Y + 1 || pos.getY() + trunkHeight > DIDConstants.MAX_Y)) {
            return false;
        }

        boolean canGenerate = true;
        BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();
        for (int y = pos.getY(); y <= pos.getY() + 1 + trunkHeight; y++) {
            int leafRadius = 1;
            if (y == pos.getY()) {
                leafRadius = 0;
            }
            if (y >= pos.getY() + 1 + trunkHeight - 2) {
                leafRadius = 2;
            }

            for (int x = pos.getX() - leafRadius; x <= pos.getX() + leafRadius && canGenerate; x++) {
                for (int z = pos.getZ() - leafRadius; z <= pos.getZ() + leafRadius && canGenerate; z++) {
                    if (y >= DIDConstants.MIN_Y && y <= DIDConstants.MAX_Y) {
                        if (!canReplace(ctx, searchPos.set(x, y, z))) {
                            canGenerate = false;
                        }
                    } else {
                        canGenerate = false;
                    }
                }
            }
        }

        if (!force && !canGenerate) {
            return false;
        }

        if (dirt.isPresent()) {
            ctx.setBlock(pos.below(), dirt.get(), centroid);
        }

        int leafHeight = this.leafProps.leafHeight.sample(ctx.rand);

        // place leaves
        BlockPos.MutableBlockPos leafPos = new BlockPos.MutableBlockPos();
        Set<BlockPos> leafPositions = new HashSet<>();
        for (int y = pos.getY() - leafHeight + 1 + trunkHeight; y <= pos.getY() + trunkHeight; y++) {
            int yRelativeToTop = y - (pos.getY() + trunkHeight);
            int leafRadius = leafProps.topLeafRadius - yRelativeToTop / leafProps.leafStepHeight;

            for (int x = pos.getX() - leafRadius; x <= pos.getX() + leafRadius; x++) {
                int dx = x - pos.getX();
                for (int z = pos.getZ() - leafRadius; z <= pos.getZ() + leafRadius; z++) {
                    int dz = z - pos.getZ();
                    if (Math.abs(dx) != leafRadius || Math.abs(dz) != leafRadius || ctx.rand.nextDouble() < leafProps.cornerLeafChance && yRelativeToTop != 0) {
                        leafPos.set(x, y, z);
                        if (canReplace(ctx, leafPos)) {
                            ctx.setBlock(leafPos, leafProps.leaf, centroid);
                            leafPositions.add(leafPos.immutable());
                        }
                    }
                }
            }
        }

        // place trunk
        for (int dy = 0; dy < trunkHeight; dy++) {
            if (canReplace(ctx, pos.above(dy))) {
                ctx.setBlock(pos.above(dy), this.log, centroid);

                if (vineProps.trunkVine.isPresent() && dy > 0) {
                    if (ctx.rand.nextDouble() < vineProps.trunkVineChance && vinesCanReplace(ctx, pos.offset(-1, dy, 0))) {
                        placeVine(ctx, pos.offset(-1, dy, 0), vineProps.trunkVine.get(), BlockStateProperties.EAST, centroid);
                    }
                    if (ctx.rand.nextDouble() < vineProps.trunkVineChance && vinesCanReplace(ctx, pos.offset(1, dy, 0))) {
                        placeVine(ctx, pos.offset(1, dy, 0), vineProps.trunkVine.get(), BlockStateProperties.WEST, centroid);
                    }
                    if (ctx.rand.nextDouble() < vineProps.trunkVineChance && vinesCanReplace(ctx, pos.offset(0, dy, -1))) {
                        placeVine(ctx, pos.offset(0, dy, -1), vineProps.trunkVine.get(), BlockStateProperties.SOUTH, centroid);
                    }
                    if (ctx.rand.nextDouble() < vineProps.trunkVineChance && vinesCanReplace(ctx, pos.offset(0, dy, 1))) {
                        placeVine(ctx, pos.offset(0, dy, 1), vineProps.trunkVine.get(), BlockStateProperties.NORTH, centroid);
                    }
                }
            }
        }

        // place hanging vines
        if (vineProps.hangingVine.isPresent()) {
            BlockPos.MutableBlockPos vinePos = new BlockPos.MutableBlockPos();
            for (int y = pos.getY() - leafHeight + 1 + trunkHeight; y <= pos.getY() + trunkHeight; y++) {
                int yRelativeToTop = y - (pos.getY() + trunkHeight);
                int vineRadius = leafProps.topLeafRadius + 1 - yRelativeToTop / leafProps.leafStepHeight;

                for (int x = pos.getX() - vineRadius; x <= pos.getX() + vineRadius; x++) {
                    for (int z = pos.getZ() - vineRadius; z <= pos.getZ() + vineRadius; z++) {
                        vinePos.set(x, y, z);
                        if (leafPositions.contains(vinePos)) {
                            BlockPos vinePosWest = vinePos.west();
                            BlockPos vinePosEast = vinePos.east();
                            BlockPos vinePosNorth = vinePos.north();
                            BlockPos vinePosSouth = vinePos.south();

                            if (ctx.rand.nextDouble() < vineProps.hangingVineChance && vinesCanReplace(ctx, vinePosWest)) {
                                placeHangingVine(ctx, vinePosWest, BlockStateProperties.EAST, centroid);
                            }
                            if (ctx.rand.nextDouble() < vineProps.hangingVineChance && vinesCanReplace(ctx, vinePosEast)) {
                                placeHangingVine(ctx, vinePosEast, BlockStateProperties.WEST, centroid);
                            }
                            if (ctx.rand.nextDouble() < vineProps.hangingVineChance && vinesCanReplace(ctx, vinePosNorth)) {
                                placeHangingVine(ctx, vinePosNorth, BlockStateProperties.SOUTH, centroid);
                            }
                            if (ctx.rand.nextDouble() < vineProps.hangingVineChance && vinesCanReplace(ctx, vinePosSouth)) {
                                placeHangingVine(ctx, vinePosSouth, BlockStateProperties.NORTH, centroid);
                            }
                        }
                    }
                }
            }
        }

        // place cocoa
        if (cocoaProps.cocoa.isPresent()) {
            if (ctx.rand.nextDouble() < cocoaProps.cocoaChance && trunkHeight >= cocoaProps.minCocoaTreeHeight) {
                for (int cocoaDy = 0; cocoaDy < cocoaProps.minCocoaTreeHeight - leafHeight; cocoaDy++) {
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        if (ctx.rand.nextInt(4 - cocoaDy) == 0) {
                            Direction cocoaAttachDir = dir.getOpposite();
                            BlockPos cocoaPos = pos.offset(cocoaAttachDir.getStepX(), trunkHeight - cocoaProps.minCocoaTreeHeight + cocoaDy + 1, cocoaAttachDir.getStepZ());
                            placeCocoa(ctx, cocoaProps.cocoa.get(), cocoaPos, dir, centroid);
                        }
                    }
                }
            }
        }

        return true;
    }

    private boolean vinesCanReplace(CaveGenContext ctx, BlockPos pos) {
        return canReplace(ctx, pos) && vineProps.vinesCanReplace.test(ctx.asLevel(), pos);
    }

    private void placeVine(CaveGenContext ctx, BlockPos pos, BlockStateProvider vine, Property<Boolean> attachProp, Centroid centroid) {
        BlockState vineBlock = ctx.getState(vine, pos, centroid);
        if (vineBlock.getProperties().contains(attachProp)) {
            ctx.setBlock(pos, vineBlock.setValue(attachProp, true));
        } else {
            ctx.setBlock(pos, vineBlock);
        }
    }

    private void placeHangingVine(CaveGenContext ctx, BlockPos pos, Property<Boolean> attachProp, Centroid centroid) {
        assert vineProps.hangingVine.isPresent();
        placeVine(ctx, pos, vineProps.hangingVine.get(), attachProp, centroid);
        BlockPos vinePos = pos.below();
        for (int y = 4; vinesCanReplace(ctx, vinePos) && y > 0; y--) {
            placeVine(ctx, vinePos, vineProps.hangingVine.get(), attachProp, centroid);
            vinePos = vinePos.below();
        }
    }

    private void placeCocoa(CaveGenContext ctx, BlockStateProvider cocoa, BlockPos pos, Direction dir, Centroid centroid) {
        BlockState cocoaBlock = ctx.getState(cocoa, pos, centroid);
        if (cocoaBlock.getProperties().contains(BlockStateProperties.FACING)) {
            cocoaBlock = cocoaBlock.setValue(BlockStateProperties.FACING, cocoaProps.invertCocoaFacing ? dir.getOpposite() : dir);
        }
        ctx.setBlock(pos, cocoaBlock);
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
            DIDCodecs.INT_PROVIDER.optionalFieldOf("leaf_height", ConstantInt.of(4)).forGetter(LeafProperties::leafHeight),
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
