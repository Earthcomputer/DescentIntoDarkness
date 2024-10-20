package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.RoomCarvingData;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class PitMineRoom extends Room<List<PitMineRoom.Step>> {
    public static final MapCodec<PitMineRoom> CODEC = RecordCodecBuilder.<PitMineRoom>mapCodec(instance -> instance.group(
        tagsCodec(),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("steps", UniformInt.of(3, 5)).forGetter(room -> room.steps),
        DIDCodecs.NON_NEGATIVE_INT_PROVIDER.optionalFieldOf("step_height", UniformInt.of(2, 5)).forGetter(room -> room.stepHeight),
        DIDCodecs.NON_NEGATIVE_INT_PROVIDER.optionalFieldOf("step_width", UniformInt.of(4, 7)).forGetter(room -> room.stepWidth),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("base_width", UniformInt.of(15, 45)).forGetter(room -> room.baseWidth),
        DIDCodecs.FLOAT_PROVIDER.optionalFieldOf("step_variance", UniformFloat.of(-2, 2)).forGetter(room -> room.stepVariance)
    ).apply(instance, PitMineRoom::new)).validate(room -> {
        if (-room.stepVariance.getMinValue() > room.stepWidth.getMinValue()) {
            return DataResult.error(() -> "Invalid step variance range");
        }
        return DataResult.success(room);
    });

    private final IntProvider steps;
    private final IntProvider stepHeight;
    private final IntProvider stepWidth;
    private final IntProvider baseWidth;
    private final FloatProvider stepVariance;

    private PitMineRoom(List<String> tags, IntProvider steps, IntProvider stepHeight, IntProvider stepWidth, IntProvider baseWidth, FloatProvider stepVariance) {
        super(tags);
        this.steps = steps;
        this.stepHeight = stepHeight;
        this.stepWidth = stepWidth;
        this.baseWidth = baseWidth;
        this.stepVariance = stepVariance;
    }

    @Override
    public RoomType<?> type() {
        return RoomType.PIT_MINE.get();
    }

    @Override
    public List<Step> createUserData(CaveGenContext ctx, RoomData roomData) {
        int numSteps = this.steps.sample(ctx.rand);
        List<Step> steps = new ArrayList<>(numSteps);
        int radius = this.baseWidth.sample(ctx.rand) / 2;
        int dy = 0;
        for (int i = 0; i < numSteps; i++) {
            int height = this.stepHeight.sample(ctx.rand);
            steps.add(new Step(
                roomData.location().add(0, dy, 0),
                radius + stepVariance.sample(ctx.rand),
                radius + stepVariance.sample(ctx.rand),
                2 * Math.PI * ctx.rand.nextDouble(),
                height
            ));
            dy += height;
            radius += stepWidth.sample(ctx.rand);
        }

        int entranceStep = ctx.rand.nextInt(numSteps);
        Vec3 entrancePos = steps.get(entranceStep).getEdge(Math.PI + Math.atan2(roomData.direction().z, roomData.direction().x));
        // entrancePos should actually be at location, shift everything by this vector
        Vec3 shift = roomData.location().subtract(entrancePos);
        for (Step step : steps) {
            step.center = step.center.add(shift);
        }

        return steps;
    }

    @Override
    public Vec3 adjustLocation(CaveGenContext ctx, RoomData roomData, List<Step> steps) {
        Step exitStep = Util.getRandom(steps, ctx.rand);
        double exitAngle = Math.atan2(roomData.direction().z, roomData.direction().x);
        exitAngle += -Math.PI / 2 + ctx.rand.nextDouble() * Math.PI; // -90 to 90 degrees
        return exitStep.getEdge(exitAngle);
    }

    @Override
    public void apply(RoomCarvingData carvingData, CaveGenContext ctx, RoomData roomData, List<Step> steps) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (Step step : steps) {
            BlockPos centerBlockPos = BlockPos.containing(step.center);

            double sinAngle = Math.sin(step.angle);
            double cosAngle = Math.cos(step.angle);
            int r = Mth.ceil(Math.max(step.rx, step.rz));

            for (int tx = -r; tx <= r; tx++) {
                double dx = step.center.x - (centerBlockPos.getX() + tx + 0.5);
                for (int tz = -r; tz <= r; tz++) {
                    double dz = step.center.z - (centerBlockPos.getZ() + tz + 0.5);

                    double unrotatedDx = dx * cosAngle + dz * sinAngle;
                    double unrotatedDz = -dx * sinAngle + dz * cosAngle;

                    if ((unrotatedDx * unrotatedDx) / (step.rx * step.rx) + (unrotatedDz * unrotatedDz) / (step.rz * step.rz) <= 1) {
                        for (int ty = 0; ty < step.height; ty++) {
                            carvingData.setAir(roomData.roomIndex(), pos.setWithOffset(centerBlockPos, tx, ty, tz));
                        }
                    }
                }
            }
        }
    }

    public static final class Step {
        private Vec3 center;
        private final double rx;
        private final double rz;
        private final double angle;
        private final int height;

        private Step(Vec3 center, double rx, double rz, double angle, int height) {
            this.center = center;
            this.rx = rx;
            this.rz = rz;
            this.angle = angle;
            this.height = height;
        }

        public Vec3 getEdge(double angle) {
            return center.add(new Vec3(rx * Math.cos(angle - this.angle), height * 0.5, rz * Math.sin(angle - this.angle)).yRot((float) this.angle));
        }
    }
}
