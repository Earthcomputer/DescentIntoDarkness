package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.RoomCarvingData;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class SimpleRoom extends Room<Object> {
    public static final MapCodec<SimpleRoom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(tagsCodec()).apply(instance, SimpleRoom::new));

    private SimpleRoom(List<String> tags) {
        super(tags);
    }

    @Override
    public RoomType<?> type() {
        return RoomType.SIMPLE.get();
    }

    @Override
    public void apply(RoomCarvingData carvingData, CaveGenContext ctx, RoomData roomData, Object userData) {
        applySphere(carvingData, roomData.location(), roomData.caveRadius(), roomData.roomIndex());
    }

    public static void applySphere(RoomCarvingData carvingData, Vec3 center, int radius, int roomIndex) {
        int centerX = Mth.floor(center.x);
        int centerY = Mth.floor(center.y);
        int centerZ = Mth.floor(center.z);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dy = -radius; dy <= radius; dy++) {
            double blockCenterY = Math.floor(center.y) + dy + 0.5;
            for (int dx = -radius; dx <= radius; dx++) {
                double blockCenterX = Math.floor(center.x) + dx + 0.5;
                for (int dz = -radius; dz <= radius; dz++) {
                    double blockCenterZ = Math.floor(center.z) + dz + 0.5;

                    if (center.distanceToSqr(blockCenterX, blockCenterY, blockCenterZ) <= radius * radius) {
                        if (!isOnAxisRing(dx, dy, dz) || Math.abs(dx + dy + dz) != radius) {
                            pos.set(centerX + dx, centerY + dy, centerZ + dz);
                            carvingData.setAir(roomIndex, pos);
                        }
                    }
                }
            }
        }
    }

    private static boolean isOnAxisRing(int dx, int dy, int dz) {
        return (dx == 0 && dy == 0) || (dx == 0 && dz == 0) || (dy == 0 && dz == 0);
    }
}
