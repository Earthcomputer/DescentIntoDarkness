package net.earthcomputer.descentintodarkness.generator.painter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.Optional;

public final class ReplaceStep extends PainterStep {
    public static final MapCodec<ReplaceStep> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        PainterStepProperties.CODEC.forGetter(PainterStepProperties::new),
        DIDCodecs.BLOCK_PREDICATE.fieldOf("old").forGetter(step -> step.old),
        Codec.INT.optionalFieldOf("old_depth").forGetter(step -> step.oldDepth),
        DIDCodecs.BLOCK_STATE_PROVIDER.fieldOf("new").forGetter(step -> step._new)
    ).apply(instance, ReplaceStep::new));

    private final BlockPredicate old;
    private final Optional<Integer> oldDepth;
    private final BlockStateProvider _new;

    private ReplaceStep(PainterStepProperties props, BlockPredicate old, Optional<Integer> oldDepth, BlockStateProvider _new) {
        super(props);
        this.old = old;
        this.oldDepth = oldDepth;
        this._new = _new;
    }

    @Override
    public PainterStepType<?> type() {
        return PainterStepType.REPLACE.get();
    }

    @Override
    public void apply(CaveGenContext ctx, BlockPos pos, Direction side, int roomIndex) {
        int oldRelative = oldDepth.map(oldDepth -> oldDepth - depth()).orElse(0);
        if (old.test(ctx.asLevel(), pos.relative(side, oldRelative))) {
            ctx.setBlock(pos, _new, roomIndex);
        }
    }
}
