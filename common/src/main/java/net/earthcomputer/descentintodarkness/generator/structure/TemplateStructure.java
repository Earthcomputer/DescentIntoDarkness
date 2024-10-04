package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.Centroid;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class TemplateStructure extends Structure {
    private static final Logger LOGGER = LogUtils.getLogger();

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

    @Override
    public Direction originPositionSide() {
        return originSide();
    }

    @Override
    public boolean place(CaveGenContext ctx, BlockPos pos, Centroid centroid, boolean force) {
        TemplateEntry templateEntry = Util.getRandom(templates, ctx.rand);
        Optional<StructureTemplate> template = Objects.requireNonNull(ctx.asLevel().getServer()).getStructureManager().get(templateEntry.template);
        if (template.isEmpty()) {
            LOGGER.error("Tried to place structure template {} which didn't exist", templateEntry.template);
            return false;
        }

        if (template.get().palettes.isEmpty()) {
            return false;
        }
        int paletteIndex = ctx.rand.nextInt(template.get().palettes.size());

        StructurePlaceSettings placeSettings = new StructurePlaceSettings() {
            @Override
            public StructureTemplate.Palette getRandomPalette(List<StructureTemplate.Palette> list, @Nullable BlockPos blockPos) {
                return list.get(paletteIndex);
            }
        };
        if (ignoreAir) {
            placeSettings.addProcessor(BlockIgnoreProcessor.AIR);
        }

        BlockPos structurePos = pos.subtract(templateEntry.origin);

        if (!force && !canPlace(ctx, template.get(), placeSettings, structurePos)) {
            return false;
        }
        boolean placed = template.get().placeInWorld(ctx.asLevel(), structurePos, structurePos, placeSettings, ctx.rand, Block.UPDATE_INVISIBLE);
        if (placed && ctx.isDebug()) {
            ctx.setBlock(pos, Blocks.DIAMOND_BLOCK.defaultBlockState());
        }
        return placed;
    }

    private boolean canPlace(CaveGenContext ctx, StructureTemplate template, StructurePlaceSettings placeSettings, BlockPos structurePos) {
        for (StructureTemplate.StructureBlockInfo block : placeSettings.getRandomPalette(template.palettes, null).blocks()) {
            if (ignoreAir && block.state().isAir()) {
                continue;
            }
            BlockPos posToCheck = StructureTemplate.calculateRelativePosition(placeSettings, block.pos()).offset(structurePos);
            if (!canReplace(ctx, posToCheck)) {
                return false;
            }
        }

        return true;
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
