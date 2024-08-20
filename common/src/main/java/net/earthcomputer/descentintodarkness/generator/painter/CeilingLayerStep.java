package net.earthcomputer.descentintodarkness.generator.painter;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.List;

public final class CeilingLayerStep extends PainterStep {
    public static final MapCodec<CeilingLayerStep> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        tagsCodec(),
        tagsInvertedCodec(),
        DIDCodecs.BLOCK_PREDICATE.optionalFieldOf("can_place_on", BlockPredicate.alwaysTrue()).forGetter(step -> step.canPlaceOn),
        DIDCodecs.BLOCK_STATE_PROVIDER.fieldOf("block").forGetter(step -> step.block)
    ).apply(instance, CeilingLayerStep::new));

    private final BlockPredicate canPlaceOn;
    private final BlockStateProvider block;

    private CeilingLayerStep(List<String> tags, boolean tagsInverted, BlockPredicate canPlaceOn, BlockStateProvider block) {
        super(tags, tagsInverted);
        this.canPlaceOn = canPlaceOn;
        this.block = block;
    }

    @Override
    public PainterStepType<?> type() {
        return PainterStepType.CEILING_LAYER.get();
    }
}
