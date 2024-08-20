package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.List;
import java.util.Optional;

public final class WallPortalStructure extends Structure {
    public static final MapCodec<WallPortalStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        StructureProperties.CODEC.forGetter(StructureProperties::new),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("width", ConstantInt.of(2)).forGetter(structure -> structure.width),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("height", ConstantInt.of(3)).forGetter(structure -> structure.height),
        SnappingSide.CODEC.optionalFieldOf("snapping_side", SnappingSide.NONE).forGetter(structure -> structure.snappingSide),
        DIDCodecs.BLOCK_PREDICATE.optionalFieldOf("portal_clear_blocks").forGetter(structure -> structure.portalClearBlocks),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("portal_block", BlockStateProvider.simple(Blocks.NETHER_PORTAL)).forGetter(structure -> structure.portalBlock),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("frame_block").forGetter(structure -> structure.frameBlock)
    ).apply(instance, WallPortalStructure::new));

    private final IntProvider width;
    private final IntProvider height;
    private final SnappingSide snappingSide;
    private final Optional<BlockPredicate> portalClearBlocks;
    private final BlockStateProvider portalBlock;
    private final Optional<BlockStateProvider> frameBlock;

    private WallPortalStructure(StructureProperties props, IntProvider width, IntProvider height, SnappingSide snappingSide, Optional<BlockPredicate> portalClearBlocks, BlockStateProvider portalBlock, Optional<BlockStateProvider> frameBlock) {
        super(props);
        this.width = width;
        this.height = height;
        this.snappingSide = snappingSide;
        this.portalClearBlocks = portalClearBlocks;
        this.portalBlock = portalBlock;
        this.frameBlock = frameBlock;
    }

    @Override
    protected List<StructurePlacementEdge> getDefaultEdges() {
        return List.of(StructurePlacementEdge.WALL);
    }

    @Override
    protected boolean shouldTransformBlocksByDefault() {
        return true;
    }

    @Override
    protected Direction getDefaultOriginSide(List<StructurePlacementEdge> edges) {
        return Direction.SOUTH;
    }

    @Override
    protected boolean shouldSnapToAxisByDefault() {
        return true;
    }

    @Override
    public StructureType<?> type() {
        return StructureType.WALL_PORTAL.get();
    }

    private enum SnappingSide implements StringRepresentable {
        NONE("none"),
        FLOOR("floor"),
        CEILING("ceiling"),
        ;

        static final Codec<SnappingSide> CODEC = StringRepresentable.fromEnum(SnappingSide::values);

        private final String name;

        SnappingSide(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
