package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;

import java.util.List;

public final class PitMineRoom extends Room {
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
}
