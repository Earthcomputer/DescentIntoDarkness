package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;

import java.util.List;

public final class DropshaftRoom extends Room {
    public static final MapCodec<DropshaftRoom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        tagsCodec(),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("depth", UniformInt.of(8, 11)).forGetter(room -> room.depth),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("step", UniformInt.of(2, 3)).forGetter(room -> room.step)
    ).apply(instance, DropshaftRoom::new));

    private final IntProvider depth;
    private final IntProvider step;

    private DropshaftRoom(List<String> tags, IntProvider depth, IntProvider step) {
        super(tags);
        this.depth = depth;
        this.step = step;
    }

    @Override
    public RoomType<?> type() {
        return RoomType.DROPSHAFT.get();
    }
}
