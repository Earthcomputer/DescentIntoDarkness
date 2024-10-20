package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.RoomCarvingData;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class TurnRoom extends Room<Object> {
    public static final MapCodec<TurnRoom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        tagsCodec(),
        DIDCodecs.floatProviderRange(-360, 360).fieldOf("angle").forGetter(room -> room.angle)
    ).apply(instance, TurnRoom::new));

    private final FloatProvider angle;

    private TurnRoom(List<String> tags, FloatProvider angle) {
        super(tags);
        this.angle = angle;
    }

    @Override
    public RoomType<?> type() {
        return RoomType.TURN.get();
    }

    @Override
    public Vec3 adjustDirection(CaveGenContext ctx, RoomData roomData, Object userData) {
        return roomData.direction().yRot((float) Math.toRadians(angle.sample(ctx.rand)));
    }

    @Override
    public void apply(RoomCarvingData carvingData, CaveGenContext ctx, RoomData roomData, Object userData) {
        SimpleRoom.applySphere(carvingData, roomData.location(), roomData.caveRadius(), roomData.roomIndex());
    }
}
