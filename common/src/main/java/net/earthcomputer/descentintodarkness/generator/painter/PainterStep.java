package net.earthcomputer.descentintodarkness.generator.painter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.DIDRegistries;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;

import java.util.List;

public abstract class PainterStep {
    private static final RecordCodecBuilder<PainterStep, List<String>> TAGS_CODEC = DIDCodecs.singleableList(Codec.STRING).optionalFieldOf("tags", List.of()).forGetter(PainterStep::tags);
    @SuppressWarnings("unchecked")
    protected static <PS extends PainterStep> RecordCodecBuilder<PS, List<String>> tagsCodec() {
        return (RecordCodecBuilder<PS, List<String>>) TAGS_CODEC;
    }

    private static final RecordCodecBuilder<PainterStep, Boolean> TAGS_INVERTED_CODEC = Codec.BOOL.optionalFieldOf("tags_inverted", false).forGetter(PainterStep::tagsInverted);
    @SuppressWarnings("unchecked")
    protected static <PS extends PainterStep> RecordCodecBuilder<PS, Boolean> tagsInvertedCodec() {
        return (RecordCodecBuilder<PS, Boolean>) TAGS_INVERTED_CODEC;
    }

    public static final Codec<PainterStep> CODEC = Codec.lazyInitialized(() -> DIDRegistries.painterStepType().byNameCodec().dispatch(PainterStep::type, PainterStepType::codec));

    private final List<String> tags;
    private final boolean tagsInverted;

    protected PainterStep(List<String> tags, boolean tagsInverted) {
        this.tags = tags;
        this.tagsInverted = tagsInverted;
    }

    public final List<String> tags() {
        return tags;
    }

    public final boolean tagsInverted() {
        return tagsInverted;
    }

    public abstract PainterStepType<?> type();
}
