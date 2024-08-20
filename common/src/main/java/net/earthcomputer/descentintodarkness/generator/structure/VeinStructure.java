package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public final class VeinStructure extends Structure {
    public static final MapCodec<VeinStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        StructureProperties.CODEC.forGetter(StructureProperties::new),
        DIDCodecs.BLOCK_STATE_PROVIDER.fieldOf("ore").forGetter(structure -> structure.ore),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("radius", ConstantInt.of(4)).forGetter(structure -> structure.radius)
    ).apply(instance, VeinStructure::new));

    private final BlockStateProvider ore;
    private final IntProvider radius;

    private VeinStructure(StructureProperties props, BlockStateProvider ore, IntProvider radius) {
        super(props);
        this.ore = ore;
        this.radius = radius;
    }

    @Override
    public StructureType<?> type() {
        return StructureType.VEIN.get();
    }
}
