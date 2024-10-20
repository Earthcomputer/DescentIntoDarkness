package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.RoomCarvingData;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class NilRoom extends Room<Object> {
    public static final MapCodec<NilRoom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(tagsCodec()).apply(instance, NilRoom::new));

    private NilRoom(List<String> tags) {
        super(tags);
    }

    @Override
    public RoomType<?> type() {
        return RoomType.NIL.get();
    }

    @Override
    public Vec3 adjustLocation(CaveGenContext ctx, RoomData roomData, Object userData) {
        return roomData.location();
    }

    @Override
    public void apply(RoomCarvingData carvingData, CaveGenContext ctx, RoomData roomData, Object userData) {
    }
}
