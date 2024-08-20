package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;

import java.util.List;

public final class ShelfRoom extends Room {
    public static final MapCodec<ShelfRoom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        tagsCodec(),
        DIDCodecs.INT_PROVIDER.optionalFieldOf("shelf_height", UniformInt.of(6, 10)).forGetter(room -> room.shelfHeight),
        DIDCodecs.NON_NEGATIVE_INT_PROVIDER.optionalFieldOf("shelf_size", ConstantInt.of(3)).forGetter(room -> room.shelfSize)
    ).apply(instance, ShelfRoom::new));

    private final IntProvider shelfHeight;
    private final IntProvider shelfSize;

    private ShelfRoom(List<String> tags, IntProvider shelfHeight, IntProvider shelfSize) {
        super(tags);
        this.shelfHeight = shelfHeight;
        this.shelfSize = shelfSize;
    }

    @Override
    public RoomType<?> type() {
        return RoomType.SHELF.get();
    }
}
