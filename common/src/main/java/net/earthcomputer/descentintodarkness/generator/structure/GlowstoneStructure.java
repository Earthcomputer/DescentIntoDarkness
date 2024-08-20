package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public final class GlowstoneStructure extends Structure {
    public static final MapCodec<GlowstoneStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        StructureProperties.CODEC.forGetter(StructureProperties::new),
        ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("density", 1500).forGetter(structure -> structure.density),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("spread_x", 8).forGetter(structure -> structure.spreadX),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("height", 12).forGetter(structure -> structure.height),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("spread_z", 8).forGetter(structure -> structure.spreadZ),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("block", BlockStateProvider.simple(Blocks.GLOWSTONE)).forGetter(structure -> structure.block)
    ).apply(instance, GlowstoneStructure::new));

    private final int density;
    private final int spreadX;
    private final int height;
    private final int spreadZ;
    private final BlockStateProvider block;

    private GlowstoneStructure(StructureProperties props, int density, int spreadX, int height, int spreadZ, BlockStateProvider block) {
        super(props);
        this.density = density;
        this.spreadX = spreadX;
        this.height = height;
        this.spreadZ = spreadZ;
        this.block = block;
    }

    @Override
    public StructureType<?> type() {
        return StructureType.GLOWSTONE.get();
    }
}
