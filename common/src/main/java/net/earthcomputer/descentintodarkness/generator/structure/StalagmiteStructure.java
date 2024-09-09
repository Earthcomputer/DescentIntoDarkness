package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.Centroid;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

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

    @Override
    public boolean place(CaveGenContext ctx, BlockPos pos, Centroid centroid, boolean force) {
        pos = pos.above();

        if (!force && !canReplace(ctx, pos)) {
            return false;
        }

        BlockPos ceilingPos = pos;
        for (int dy = 1; dy < floorToCeilingSearchRange * 2 && canReplace(ctx, ceilingPos); dy++) {
            ceilingPos = ceilingPos.above();
        }
        boolean hasCeiling = canPlaceOn(ctx, ceilingPos);
        BlockPos floorPos = pos;
        for (int dy = 1; dy < floorToCeilingSearchRange * 2 && canReplace(ctx, floorPos); dy++) {
            floorPos = floorPos.below();
        }
        boolean hasFloor = canPlaceOn(ctx, floorPos);

        if (!force && !hasFloor || !hasCeiling) {
            return false;
        }

        int height = ceilingPos.getY() - floorPos.getY() - 1;
        if (height < 4) {
            if (force) {
                height = 4;
            } else {
                return false;
            }
        }

        pos = new BlockPos(pos.getX(), floorPos.getY() + 1 + ctx.rand.nextInt(ceilingPos.getY() - floorPos.getY() + 1), pos.getZ());

        int r = (int)(height * maxColumnRadiusToCaveHeightRatio);
        int maxRadius;
        if (r < minColumnRadius) {
            maxRadius = minColumnRadius;
        } else if (r > maxColumnRadius) {
            maxRadius = maxColumnRadius;
        } else {
            maxRadius = r;
        }
        int radius = minColumnRadius + ctx.rand.nextInt(maxRadius - minColumnRadius + 1);

        DripstoneGenerator stalagmiteGenerator = createGenerator(ctx, pos.atY(ceilingPos.getY() - 1), false, radius);
        DripstoneGenerator stalactiteGenerator = createGenerator(ctx, pos.atY(floorPos.getY() + 1), true, radius);
        WindModifier windModifier;
        if (stalagmiteGenerator.shouldGenerateWind() && stalactiteGenerator.shouldGenerateWind()) {
            windModifier = new WindModifier(pos.getY(), ctx);
        } else {
            windModifier = new WindModifier();
        }
        boolean roomForStalagmite = stalagmiteGenerator.adjustScale(ctx, windModifier);
        boolean roomForStalactite = stalactiteGenerator.adjustScale(ctx, windModifier);

        boolean placed = false;
        if ((force || roomForStalagmite) && hasStalactite) {
            placed = true;
            stalagmiteGenerator.generate(ctx, centroid, windModifier);
        }
        if (force || roomForStalactite) {
            placed = true;
            stalactiteGenerator.generate(ctx, centroid, windModifier);
        }

        return placed;
    }

    private DripstoneGenerator createGenerator(CaveGenContext ctx, BlockPos pos, boolean isStalagmite, int scale) {
        float bluntness;
        if (isStalagmite) {
            bluntness = stalagmiteBluntness.sample(ctx.rand);
        } else {
            bluntness = stalactiteBluntness.sample(ctx.rand);
        }
        float heightScale = this.heightScale.sample(ctx.rand);
        return new DripstoneGenerator(pos, isStalagmite, scale, bluntness, heightScale);
    }

    private boolean canGenerateBase(CaveGenContext ctx, BlockPos pos, int radius) {
        if (canReplace(ctx, pos)) {
            return false;
        }
        final float arcLength = 6;
        float deltaAngle = arcLength / radius;
        for (float angle = 0; angle < Math.PI * 2; angle += deltaAngle) {
            int x = (int)(Math.cos(angle) * radius);
            int z = (int)(Math.sin(angle) * radius);
            if (canReplace(ctx, pos.offset(x, 0, z))) {
                return false;
            }
        }
        return true;
    }

    protected static double getHeightFromHDistance(double hDistance, double scale, double heightScale, double bluntness) {
        if (hDistance < bluntness) {
            hDistance = bluntness;
        }
        final double inputScale = 0.384;
        double scaledHDistance = hDistance / scale * inputScale;
        double temp1 = 0.75 * Math.pow(scaledHDistance, 4.0 / 3);
        double temp2 = Math.pow(scaledHDistance, 2.0 / 3);
        double temp3 = 1.0 / 3 * Math.log(scaledHDistance);
        double height = heightScale * (temp1 - temp2 - temp3);
        height = Math.max(height, 0);
        return height / inputScale * scale;
    }

    private final class DripstoneGenerator {
        private BlockPos pos;
        private final boolean isStalagmite;
        private int radius;
        private final double bluntness;
        private final double heightScale;

        private DripstoneGenerator(BlockPos pos, boolean isStalagmite, int radius, double bluntness, double heightScale) {
            this.pos = pos;
            this.isStalagmite = isStalagmite;
            this.radius = radius;
            this.bluntness = bluntness;
            this.heightScale = heightScale;
        }

        private int getTotalHeight() {
            return this.getHeight(0.0f);
        }

        private int getStalactiteBottomY() {
            if (this.isStalagmite) {
                return this.pos.getY();
            }
            return this.pos.getY() - this.getTotalHeight();
        }

        private int getStalagmiteTopY() {
            if (!this.isStalagmite) {
                return this.pos.getY();
            }
            return this.pos.getY() + this.getTotalHeight();
        }

        private boolean adjustScale(CaveGenContext ctx, WindModifier wind) {
            int originalScale = this.radius;
            while (this.radius > 1) {
                BlockPos pos = this.pos;
                int heightToCheck = Math.min(10, this.getTotalHeight());
                for (int i = 0; i < heightToCheck; i++) {
                    if (canGenerateBase(ctx, wind.modify(pos), this.radius)) {
                        this.pos = pos;
                        return true;
                    }
                    pos = this.isStalagmite ? pos.below() : pos.above();
                }
                this.radius /= 2;
            }
            this.radius = originalScale;
            return false;
        }

        private int getHeight(float hDistance) {
            return (int) getHeightFromHDistance(hDistance, this.radius, this.heightScale, this.bluntness);
        }

        private void generate(CaveGenContext ctx, Centroid centroid, WindModifier wind) {
            for (int x = -this.radius; x <= this.radius; x++) {
                for (int z = -this.radius; z <= this.radius; z++) {
                    float hDistance = (float)Math.sqrt(x * x + z * z);
                    if (hDistance <= this.radius) {
                        int localHeight = this.getHeight(hDistance);
                        if (localHeight > 0) {
                            if (ctx.rand.nextFloat() < 0.2) {
                                localHeight *= 0.8f + ctx.rand.nextFloat() * 0.2f;
                            }
                            BlockPos pos = this.pos.offset(x, 0, z);
                            boolean placedBlock = false;
                            for (int y = 0; y < localHeight; ++y) {
                                BlockPos windModifiedPos = wind.modify(pos);
                                if (canReplace(ctx, windModifiedPos)) {
                                    placedBlock = true;
                                    ctx.setBlock(windModifiedPos, block, centroid);
                                } else if (placedBlock && !canReplace(ctx, windModifiedPos)) {
                                    break;
                                }
                                pos = this.isStalagmite ? pos.above() : pos.below();
                            }
                        }
                    }
                }
            }
        }

        private boolean shouldGenerateWind() {
            return this.radius >= minRadiusForWind && this.bluntness >= minBluntnessForWind;
        }
    }

    private final class WindModifier {
        private final int y;
        @Nullable
        private final Vec3 wind;

        private WindModifier(int y, CaveGenContext ctx) {
            this.y = y;
            float speedX = windSpeed.sample(ctx.rand);
            float speedZ = windSpeed.sample(ctx.rand);
            this.wind = new Vec3(ctx.rand.nextBoolean() ? -speedX : speedX, 0.0, ctx.rand.nextBoolean() ? -speedZ : speedZ);
        }

        private WindModifier() {
            this.y = 0;
            this.wind = null;
        }

        private BlockPos modify(BlockPos pos) {
            if (this.wind == null) {
                return pos;
            }
            int dy = this.y - pos.getY();
            Vec3 delta = this.wind.scale(dy);
            return pos.offset((int)Math.round(delta.x), 0, (int)Math.round(delta.z));
        }
    }
}
