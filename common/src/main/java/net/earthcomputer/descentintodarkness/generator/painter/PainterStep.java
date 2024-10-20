package net.earthcomputer.descentintodarkness.generator.painter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.DIDRegistries;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.PlacementEdge;
import net.earthcomputer.descentintodarkness.generator.TagList;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.function.Predicate;

public abstract class PainterStep {
    public static final Codec<PainterStep> CODEC = Codec.lazyInitialized(() -> DIDRegistries.painterStepType().byNameCodec().dispatch(PainterStep::type, PainterStepType::codec));

    private final TagList tagList;
    private final List<PlacementEdge> edges;
    private final List<Direction> sides;
    private final int depth;
    private final double chance;

    protected PainterStep(PainterStepProperties props) {
        this.tagList = props.tagList;
        this.edges = props.edges;
        this.sides = PlacementEdge.directions(props.edges);
        this.depth = props.depth;
        this.chance = props.chance;
    }

    public final TagList tags() {
        return tagList;
    }

    public final List<Direction> sides() {
        return sides;
    }

    public final int depth() {
        return depth;
    }

    public final double chance() {
        return chance;
    }

    public abstract PainterStepType<?> type();

    public abstract void apply(CaveGenContext ctx, BlockPos pos, Direction side, int roomIndex);

    protected record PainterStepProperties(
        TagList tagList,
        List<PlacementEdge> edges,
        int depth,
        double chance
    ) {
        public PainterStepProperties(PainterStep step) {
            this(
                step.tagList,
                step.edges,
                step.depth,
                step.chance
            );
        }

        public static final MapCodec<PainterStepProperties> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TagList.CODEC.forGetter(PainterStepProperties::tagList),
            DIDCodecs.singleableList(PlacementEdge.CODEC).optionalFieldOf("edges", List.of()).forGetter(PainterStepProperties::edges),
            Codec.INT.optionalFieldOf("depth", 0).forGetter(PainterStepProperties::depth),
            DIDCodecs.doubleRange(0, 1).optionalFieldOf("chance", 1.0).forGetter(PainterStepProperties::chance)
        ).apply(instance, PainterStepProperties::new));
    }
}
