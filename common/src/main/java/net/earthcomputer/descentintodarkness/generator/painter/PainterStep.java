package net.earthcomputer.descentintodarkness.generator.painter;

import com.mojang.serialization.Codec;
import net.earthcomputer.descentintodarkness.DIDRegistries;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.Centroid;
import net.earthcomputer.descentintodarkness.generator.TagList;
import net.minecraft.core.BlockPos;

import java.util.function.Predicate;

public abstract class PainterStep {
    public static final Codec<PainterStep> CODEC = Codec.lazyInitialized(() -> DIDRegistries.painterStepType().byNameCodec().dispatch(PainterStep::type, PainterStepType::codec));

    private final TagList tagList;

    protected PainterStep(TagList tagList) {
        this.tagList = tagList;
    }

    public final TagList tags() {
        return tagList;
    }

    public abstract PainterStepType<?> type();

    public abstract void apply(CaveGenContext ctx, Centroid centroid, Predicate<BlockPos> canTryToPaint);
}
