package net.earthcomputer.descentintodarkness.generator.painter;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.BlockTypeRange;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

import java.util.List;

public final class ReplaceMesaStep extends PainterStep {
    public static final MapCodec<ReplaceMesaStep> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        tagsCodec(),
        tagsInvertedCodec(),
        DIDCodecs.BLOCK_PREDICATE.fieldOf("old").forGetter(step -> step.old),
        BlockTypeRange.INCOMPLETE_INT_CODEC.fieldOf("layers").forGetter(step -> step.mesaLayers)
    ).apply(instance, ReplaceMesaStep::new));

    private final BlockPredicate old;
    private final BlockTypeRange<Integer> mesaLayers;

    private ReplaceMesaStep(List<String> tags, boolean tagsInverted, BlockPredicate old, BlockTypeRange<Integer> mesaLayers) {
        super(tags, tagsInverted);
        this.old = old;
        this.mesaLayers = mesaLayers;
    }

    @Override
    public PainterStepType<?> type() {
        return PainterStepType.REPLACE_MESA.get();
    }
}
