package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

public abstract class AbstractPatchStructure extends Structure {
    private final int spreadX;
    private final int spreadY;
    private final int spreadZ;
    private final boolean spreadLocal;
    private final int tries;

    protected AbstractPatchStructure(StructureProperties props, PatchProperties patchProps) {
        super(props);
        this.spreadX = patchProps.spreadX;
        this.spreadY = patchProps.spreadY;
        this.spreadZ = patchProps.spreadZ;
        this.spreadLocal = patchProps.spreadLocal;
        this.tries = patchProps.tries;
    }

    protected record PatchProperties(
        int spreadX,
        int spreadY,
        int spreadZ,
        boolean spreadLocal,
        int tries
    ) {
        public PatchProperties(AbstractPatchStructure structure) {
            this(
                structure.spreadX,
                structure.spreadY,
                structure.spreadZ,
                structure.spreadLocal,
                structure.tries
            );
        }

        public static final MapCodec<PatchProperties> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("spread_x", 8).forGetter(PatchProperties::spreadX),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("spread_y", 4).forGetter(PatchProperties::spreadY),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("spread_z", 8).forGetter(PatchProperties::spreadZ),
            Codec.BOOL.optionalFieldOf("spread_local", false).forGetter(PatchProperties::spreadLocal),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("tries", 64).forGetter(PatchProperties::tries)
        ).apply(instance, PatchProperties::new));
    }
}
