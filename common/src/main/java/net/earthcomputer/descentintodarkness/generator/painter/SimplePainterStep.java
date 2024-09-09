package net.earthcomputer.descentintodarkness.generator.painter;

import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.Centroid;
import net.earthcomputer.descentintodarkness.generator.TagList;
import net.minecraft.core.BlockPos;

import java.util.function.Predicate;

public abstract class SimplePainterStep extends PainterStep {
    protected SimplePainterStep(TagList tagList) {
        super(tagList);
    }

    @Override
    public void apply(CaveGenContext ctx, Centroid centroid, Predicate<BlockPos> canTryToPaint) {
        BlockPos center = BlockPos.containing(centroid.pos);
        int radius = centroid.size + 4;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int ty = getMinY(radius); ty <= getMaxY(radius); ty++) {
            for (int tx = -radius; tx <= radius; tx++) {
                for (int tz = -radius; tz <= radius; tz++) {
                    if (tx * tx  +  ty * ty  +  tz * tz <= (radius - 2) * (radius - 2)) {
                        if(tx == 0 && tz == 0 && Math.abs(tx + ty + tz) == radius - 2) {
                            continue;
                        }

                        pos.setWithOffset(center, tx, ty, tz);
                        if (!ctx.style().isTransparentBlock(ctx, pos) && canEverApplyToPos(ctx, pos) && canTryToPaint.test(pos)) {
                            applyToBlock(ctx, pos, centroid);
                        }
                    }
                }
            }
        }
    }

    protected int getMinY(int radius) {
        return -radius;
    }

    protected int getMaxY(int radius) {
        return radius;
    }

    protected boolean canEverApplyToPos(CaveGenContext ctx, BlockPos pos) {
        return true;
    }

    protected abstract void applyToBlock(CaveGenContext ctx, BlockPos pos, Centroid centroid);
}
