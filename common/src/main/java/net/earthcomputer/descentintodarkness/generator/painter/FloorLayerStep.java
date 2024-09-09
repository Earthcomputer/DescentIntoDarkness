package net.earthcomputer.descentintodarkness.generator.painter;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.Centroid;
import net.earthcomputer.descentintodarkness.generator.PostProcessor;
import net.earthcomputer.descentintodarkness.generator.TagList;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public final class FloorLayerStep extends SimplePainterStep {
    public static final MapCodec<FloorLayerStep> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        TagList.CODEC.forGetter(PainterStep::tags),
        DIDCodecs.BLOCK_PREDICATE.optionalFieldOf("can_place_on", BlockPredicate.alwaysTrue()).forGetter(step -> step.canPlaceOn),
        DIDCodecs.BLOCK_STATE_PROVIDER.fieldOf("block").forGetter(step -> step.block)
    ).apply(instance, FloorLayerStep::new));

    private final BlockPredicate canPlaceOn;
    private final BlockStateProvider block;

    private FloorLayerStep(TagList tagList, BlockPredicate canPlaceOn, BlockStateProvider block) {
        super(tagList);
        this.canPlaceOn = canPlaceOn;
        this.block = block;
    }

    @Override
    public PainterStepType<?> type() {
        return PainterStepType.FLOOR_LAYER.get();
    }

    @Override
    protected boolean canEverApplyToPos(CaveGenContext ctx, BlockPos pos) {
        return PostProcessor.isFloor(ctx, pos) && canPlaceOn.test(ctx.asLevel(), pos);
    }

    @Override
    protected int getMaxY(int radius) {
        return -3;
    }

    @Override
    protected void applyToBlock(CaveGenContext ctx, BlockPos pos, Centroid centroid) {
        ctx.setBlock(pos.above(), block, centroid);
    }
}
