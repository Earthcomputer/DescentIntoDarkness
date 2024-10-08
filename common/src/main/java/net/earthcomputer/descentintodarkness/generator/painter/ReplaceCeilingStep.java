package net.earthcomputer.descentintodarkness.generator.painter;

import com.mojang.serialization.Codec;
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

public final class ReplaceCeilingStep extends SimplePainterStep {
    public static final MapCodec<ReplaceCeilingStep> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        TagList.CODEC.forGetter(PainterStep::tags),
        DIDCodecs.BLOCK_PREDICATE.fieldOf("old").forGetter(step -> step.old),
        DIDCodecs.BLOCK_STATE_PROVIDER.fieldOf("new").forGetter(step -> step._new),
        Codec.doubleRange(0, 1).optionalFieldOf("chance", 1.0).forGetter(step -> step.chance)
    ).apply(instance, ReplaceCeilingStep::new));

    private final BlockPredicate old;
    private final BlockStateProvider _new;
    private final double chance;

    private ReplaceCeilingStep(TagList tagList, BlockPredicate old, BlockStateProvider _new, double chance) {
        super(tagList);
        this.old = old;
        this._new = _new;
        this.chance = chance;
    }

    @Override
    public PainterStepType<?> type() {
        return PainterStepType.REPLACE_CEILING.get();
    }

    @Override
    protected int getMinY(int radius) {
        return 3;
    }

    @Override
    protected boolean canEverApplyToPos(CaveGenContext ctx, BlockPos pos) {
        return PostProcessor.isRoof(ctx, pos);
    }

    @Override
    protected void applyToBlock(CaveGenContext ctx, BlockPos pos, Centroid centroid) {
        if (old.test(ctx.asLevel(), pos) && ctx.rand.nextDouble() < chance) {
            ctx.setBlock(pos, _new, centroid);
        }
    }
}
