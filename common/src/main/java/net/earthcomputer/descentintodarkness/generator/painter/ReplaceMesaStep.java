package net.earthcomputer.descentintodarkness.generator.painter;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.style.BlockTypeRange;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public final class ReplaceMesaStep extends PainterStep {
    public static final MapCodec<ReplaceMesaStep> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        PainterStepProperties.CODEC.forGetter(PainterStepProperties::new),
        DIDCodecs.BLOCK_PREDICATE.fieldOf("old").forGetter(step -> step.old),
        BlockTypeRange.INCOMPLETE_INT_CODEC.fieldOf("layers").forGetter(step -> step.mesaLayers)
    ).apply(instance, ReplaceMesaStep::new));

    private final BlockPredicate old;
    private final BlockTypeRange<Integer> mesaLayers;

    private ReplaceMesaStep(PainterStepProperties props, BlockPredicate old, BlockTypeRange<Integer> mesaLayers) {
        super(props);
        this.old = old;
        this.mesaLayers = mesaLayers;
    }

    @Override
    public PainterStepType<?> type() {
        return PainterStepType.REPLACE_MESA.get();
    }

    @Override
    public void apply(CaveGenContext ctx, BlockPos pos, Direction side, int roomIndex) {
        BlockStateProvider _new = mesaLayers.get(pos.getY());
        if (_new != null) {
            ctx.setBlock(pos, _new, roomIndex);
        }
    }
}
