package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.ModuleGenerator;
import net.earthcomputer.descentintodarkness.generator.RoomCarvingData;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class DropshaftRoom extends Room<DropshaftRoom.DropshaftData> {
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

    @Override
    public DropshaftData createUserData(CaveGenContext ctx, RoomData roomData) {
        return new DropshaftData(depth.sample(ctx.rand));
    }

    @Override
    public Vec3 adjustLocation(CaveGenContext ctx, RoomData roomData, DropshaftData userData) {
        int depth = userData.depth;
        if (roomData.caveRadius() <= 5) {
            return roomData.location().add(0, -(depth - 4), 0);
        } else {
            return roomData.location().add(0, -(depth - 2), 0);
        }
    }

    @Override
    public void apply(RoomCarvingData carvingData, CaveGenContext ctx, RoomData roomData, DropshaftData userData) {
        int depth = userData.depth;
        int i = 0;
        int radius = roomData.caveRadius() >= 4 ? roomData.caveRadius() - 1 : roomData.caveRadius();
        Vec3 loc = roomData.location();
        while (i < depth) {
            SimpleRoom.applySphere(carvingData, loc, radius, roomData.roomIndex());
            loc = ModuleGenerator.vary(ctx, loc);
            int step = this.step.sample(ctx.rand);
            loc = loc.add(0, -step, 0);
            i += step;
        }
    }

    public record DropshaftData(int depth) {
    }
}
