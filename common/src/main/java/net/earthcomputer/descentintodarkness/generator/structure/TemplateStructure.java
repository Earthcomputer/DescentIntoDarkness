package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.util.List;

public final class TemplateStructure extends Structure {
    public static final MapCodec<TemplateStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        StructureProperties.CODEC.forGetter(StructureProperties::new),
        ExtraCodecs.nonEmptyList(DIDCodecs.singleableList(ResourceLocation.CODEC)).fieldOf("templates").forGetter(structure -> structure.templates),
        BlockPos.CODEC.optionalFieldOf("origin", BlockPos.ZERO).forGetter(structure -> structure.origin),
        Codec.BOOL.optionalFieldOf("ignore_air", true).forGetter(structure -> structure.ignoreAir)
    ).apply(instance, TemplateStructure::new));

    private final List<ResourceLocation> templates;
    private final BlockPos origin;
    private final boolean ignoreAir;

    private TemplateStructure(StructureProperties props, List<ResourceLocation> templates, BlockPos origin, boolean ignoreAir) {
        super(props);
        this.templates = templates;
        this.origin = origin;
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
}
