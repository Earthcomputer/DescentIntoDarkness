package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public final class PatchStructure extends AbstractPatchStructure {
    public static final MapCodec<PatchStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        StructureProperties.CODEC.forGetter(StructureProperties::new),
        PatchProperties.CODEC.forGetter(PatchProperties::new),
        DIDCodecs.BLOCK_STATE_PROVIDER.fieldOf("block").forGetter(structure -> structure.block)
    ).apply(instance, PatchStructure::new));

    private final BlockStateProvider block;

    private PatchStructure(StructureProperties props, PatchProperties patchProps, BlockStateProvider block) {
        super(props, patchProps);
        this.block = block;
    }

    @Override
    public StructureType<?> type() {
        return StructureType.PATCH.get();
    }

    @Override
    protected boolean doPlace(CaveGenContext ctx, BlockPos pos, int roomIndex) {
        ctx.setBlock(pos, block, roomIndex);
        return true;
    }
}
