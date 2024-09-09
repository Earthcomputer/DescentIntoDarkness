package net.earthcomputer.descentintodarkness.generator.painter;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.Centroid;
import net.earthcomputer.descentintodarkness.generator.PostProcessor;
import net.earthcomputer.descentintodarkness.generator.TagList;
import net.earthcomputer.descentintodarkness.style.BlockTypeRange;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.function.Predicate;

public final class ReplaceMesaStep extends PainterStep {
    public static final MapCodec<ReplaceMesaStep> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        TagList.CODEC.forGetter(PainterStep::tags),
        DIDCodecs.BLOCK_PREDICATE.fieldOf("old").forGetter(step -> step.old),
        BlockTypeRange.INCOMPLETE_INT_CODEC.fieldOf("layers").forGetter(step -> step.mesaLayers)
    ).apply(instance, ReplaceMesaStep::new));

    private final BlockPredicate old;
    private final BlockTypeRange<Integer> mesaLayers;

    private ReplaceMesaStep(TagList tagList, BlockPredicate old, BlockTypeRange<Integer> mesaLayers) {
        super(tagList);
        this.old = old;
        this.mesaLayers = mesaLayers;
    }

    @Override
    public PainterStepType<?> type() {
        return PainterStepType.REPLACE_MESA.get();
    }

    @Override
    public void apply(CaveGenContext ctx, Centroid centroid, Predicate<BlockPos> canTryToPaint) {
        BlockPos center = BlockPos.containing(centroid.pos);
        int radius = centroid.size + 4;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int ty = -radius; ty <= radius; ty++) {
            BlockStateProvider replacement = mesaLayers.get(ty + center.getY());
            if (replacement == null) {
                continue;
            }
            for (int tx = -radius; tx <= radius; tx++) {
                for (int tz = -radius; tz <= radius; tz++) {
                    if (tx * tx  +  ty * ty  +  tz * tz <= (radius - 2) * (radius - 2)) {
                        if(tx == 0 && tz == 0 && Math.abs(tx + ty + tz) == radius - 2) {
                            continue;
                        }

                        pos.setWithOffset(center, tx, ty, tz);
                        if (!ctx.style().isTransparentBlock(ctx, pos) && old.test(ctx.asLevel(), pos) && !PostProcessor.isFloor(ctx, pos) && canTryToPaint.test(pos)) {
                            ctx.setBlock(pos, replacement, centroid);
                        }
                    }
                }
            }
        }
    }
}
