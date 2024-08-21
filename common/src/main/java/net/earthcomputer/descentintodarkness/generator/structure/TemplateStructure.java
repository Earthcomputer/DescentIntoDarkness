package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.util.List;
import java.util.function.Function;

public final class TemplateStructure extends Structure {
    public static final MapCodec<TemplateStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        StructureProperties.CODEC.forGetter(StructureProperties::new),
        ExtraCodecs.nonEmptyList(DIDCodecs.singleableList(TemplateEntry.CODEC)).fieldOf("templates").forGetter(structure -> structure.templates),
        Codec.BOOL.optionalFieldOf("ignore_air", true).forGetter(structure -> structure.ignoreAir)
    ).apply(instance, TemplateStructure::new));

    private final List<TemplateEntry> templates;
    private final boolean ignoreAir;

    private TemplateStructure(StructureProperties props, List<TemplateEntry> templates, boolean ignoreAir) {
        super(props);
        this.templates = templates;
        this.ignoreAir = ignoreAir;
    }

    @Override
    protected boolean shouldTransformBlocksByDefault() {
        return true;
    }

    @Override
    public StructureType<?> type() {
        return StructureType.TEMPLATE.get();
    }

    private record TemplateEntry(
        ResourceLocation template,
        BlockPos origin
    ) {
        static final Codec<TemplateEntry> CODEC = Codec.either(
            ResourceLocation.CODEC,
            RecordCodecBuilder.<TemplateEntry>create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("template").forGetter(TemplateEntry::template),
                BlockPos.CODEC.optionalFieldOf("origin", BlockPos.ZERO).forGetter(TemplateEntry::origin)
            ).apply(instance, TemplateEntry::new))
        ).xmap(
            either -> either.map(template -> new TemplateEntry(template, BlockPos.ZERO), Function.identity()),
            entry -> entry.origin.equals(BlockPos.ZERO) ? Either.left(entry.template) : Either.right(entry)
        );
    }
}
