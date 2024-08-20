package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.Optional;

public final class VinePatchStructure extends AbstractPatchStructure {
    public static final MapCodec<VinePatchStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        StructureProperties.CODEC.forGetter(StructureProperties::new),
        PatchProperties.CODEC.forGetter(PatchProperties::new),
        DIDCodecs.BLOCK_STATE_PROVIDER.fieldOf("vine").forGetter(structure -> structure.vine),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("first_block").forGetter(structure -> structure.firstBlock),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("last_block").forGetter(structure -> structure.lastBlock),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("height", UniformInt.of(5, 10)).forGetter(structure -> structure.height),
        Codec.BOOL.optionalFieldOf("vine_random_rotation", true).forGetter(structure -> structure.vineRandomRotation)
    ).apply(instance, VinePatchStructure::new));

    private final BlockStateProvider vine;
    private final Optional<BlockStateProvider> firstBlock;
    private final Optional<BlockStateProvider> lastBlock;
    private final IntProvider height;
    private final boolean vineRandomRotation;

    private VinePatchStructure(StructureProperties props, PatchProperties patchProps, BlockStateProvider vine, Optional<BlockStateProvider> firstBlock, Optional<BlockStateProvider> lastBlock, IntProvider height, boolean vineRandomRotation) {
        super(props, patchProps);
        this.vine = vine;
        this.firstBlock = firstBlock;
        this.lastBlock = lastBlock;
        this.height = height;
        this.vineRandomRotation = vineRandomRotation;
    }

    @Override
    public StructureType<?> type() {
        return StructureType.VINE_PATCH.get();
    }
}
