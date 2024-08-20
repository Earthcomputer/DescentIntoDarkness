package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.List;

public final class StalagmiteStructure extends Structure {
    public static final MapCodec<StalagmiteStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        StructureProperties.CODEC.forGetter(StructureProperties::new),
        DIDCodecs.BLOCK_STATE_PROVIDER.fieldOf("block").forGetter(structure -> structure.block),
        ExtraCodecs.intRange(1, 512).optionalFieldOf("floor_to_ceiling_search_range", 30).forGetter(structure -> structure.floorToCeilingSearchRange),
        Codec.floatRange(0.1f, 1).optionalFieldOf("max_column_radius_to_cave_height_ratio", 0.33f).forGetter(structure -> structure.maxColumnRadiusToCaveHeightRatio),
        ExtraCodecs.intRange(1, 30).optionalFieldOf("min_column_radius", 3).forGetter(structure -> structure.minColumnRadius),
        ExtraCodecs.intRange(1, 60).optionalFieldOf("max_column_radius", 3).forGetter(structure -> structure.maxColumnRadius),
        DIDCodecs.floatProviderRange(0.1f, 10)
            .validate(stalagmiteBluntness -> stalagmiteBluntness.getMinValue() > 5 ? DataResult.error(() -> "Minimum stalagmite bluntness cannot be greater than 5") : DataResult.success(stalagmiteBluntness))
            .optionalFieldOf("stalagmite_bluntness", UniformFloat.of(0.4f, 0.6f))
            .forGetter(structure -> structure.stalagmiteBluntness),
        DIDCodecs.floatProviderRange(0.1f, 10)
            .validate(stalactiteBluntness -> stalactiteBluntness.getMinValue() > 5 ? DataResult.error(() -> "Minimum stalactite bluntness cannot be greater than 5") : DataResult.success(stalactiteBluntness))
            .optionalFieldOf("stalactite_bluntness", UniformFloat.of(0.3f, 0.6f))
            .forGetter(structure -> structure.stalactiteBluntness),
        DIDCodecs.floatProviderRange(0, 20)
            .validate(heightScale -> heightScale.getMinValue() > 10 ? DataResult.error(() -> "Minimum height scale cannot be greater than 10") : DataResult.success(heightScale))
            .optionalFieldOf("height_scale", UniformFloat.of(0.4f, 1.6f))
            .forGetter(structure -> structure.heightScale),
        DIDCodecs.floatProviderRange(0, 1).optionalFieldOf("wind_speed", UniformFloat.of(0, 0.2f)).forGetter(structure -> structure.windSpeed),
        ExtraCodecs.intRange(0, 100).optionalFieldOf("min_radius_for_wind", 5).forGetter(structure -> structure.minRadiusForWind),
        Codec.floatRange(0, 5).optionalFieldOf("min_bluntness_for_wind", 0.7f).forGetter(structure -> structure.minBluntnessForWind),
        Codec.BOOL.optionalFieldOf("has_stalactite", false).forGetter(structure -> structure.hasStalactite)
    ).apply(instance, StalagmiteStructure::new));

    private final BlockStateProvider block;
    private final int floorToCeilingSearchRange;
    private final float maxColumnRadiusToCaveHeightRatio;
    private final int minColumnRadius;
    private final int maxColumnRadius;
    private final FloatProvider stalagmiteBluntness;
    private final FloatProvider stalactiteBluntness;
    private final FloatProvider heightScale;
    private final FloatProvider windSpeed;
    private final int minRadiusForWind;
    private final float minBluntnessForWind;
    private final boolean hasStalactite;

    private StalagmiteStructure(StructureProperties props, BlockStateProvider block, int floorToCeilingSearchRange, float maxColumnRadiusToCaveHeightRatio, int minColumnRadius, int maxColumnRadius, FloatProvider stalagmiteBluntness, FloatProvider stalactiteBluntness, FloatProvider heightScale, FloatProvider windSpeed, int minRadiusForWind, float minBluntnessForWind, boolean hasStalactite) {
        super(props);
        this.block = block;
        this.floorToCeilingSearchRange = floorToCeilingSearchRange;
        this.maxColumnRadiusToCaveHeightRatio = maxColumnRadiusToCaveHeightRatio;
        this.minColumnRadius = minColumnRadius;
        this.maxColumnRadius = maxColumnRadius;
        this.stalagmiteBluntness = stalagmiteBluntness;
        this.stalactiteBluntness = stalactiteBluntness;
        this.heightScale = heightScale;
        this.windSpeed = windSpeed;
        this.minRadiusForWind = minRadiusForWind;
        this.minBluntnessForWind = minBluntnessForWind;
        this.hasStalactite = hasStalactite;
    }

    @Override
    protected boolean shouldTransformBlocksByDefault() {
        return true;
    }

    @Override
    protected Direction getDefaultOriginSide(List<StructurePlacementEdge> edges) {
        return Direction.DOWN;
    }

    @Override
    public StructureType<?> type() {
        return StructureType.STALAGMITE.get();
    }
}
