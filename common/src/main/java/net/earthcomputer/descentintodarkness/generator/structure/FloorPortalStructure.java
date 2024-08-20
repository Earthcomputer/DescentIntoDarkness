package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.List;
import java.util.Optional;

public final class FloorPortalStructure extends Structure {
    public static final MapCodec<FloorPortalStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        StructureProperties.CODEC.forGetter(StructureProperties::new),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("width", ConstantInt.of(2)).forGetter(structure -> structure.width),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("height", ConstantInt.of(3)).forGetter(structure -> structure.height),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("portal_block", BlockStateProvider.simple(Blocks.NETHER_PORTAL)).forGetter(structure -> structure.portalBlock),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("frame_block").forGetter(structure -> structure.frameBlock),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("floor_block").forGetter(structure -> structure.floorBlock)
    ).apply(instance, FloorPortalStructure::new));

    private final IntProvider width;
    private final IntProvider height;
    private final BlockStateProvider portalBlock;
    private final Optional<BlockStateProvider> frameBlock;
    private final Optional<BlockStateProvider> floorBlock;

    private FloorPortalStructure(StructureProperties props, IntProvider width, IntProvider height, BlockStateProvider portalBlock, Optional<BlockStateProvider> frameBlock, Optional<BlockStateProvider> floorBlock) {
        super(props);
        this.width = width;
        this.height = height;
        this.portalBlock = portalBlock;
        this.frameBlock = frameBlock;
        this.floorBlock = floorBlock;
    }

    @Override
    protected List<StructurePlacementEdge> getDefaultEdges() {
        return List.of(StructurePlacementEdge.FLOOR);
    }

    @Override
    protected boolean shouldTransformBlocksByDefault() {
        return true;
    }

    @Override
    protected boolean shouldSnapToAxisByDefault() {
        return true;
    }

    @Override
    public StructureType<?> type() {
        return StructureType.FLOOR_PORTAL.get();
    }
}
