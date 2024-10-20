package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.ModuleGenerator;
import net.earthcomputer.descentintodarkness.generator.RoomCarvingData;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class RavineRoom extends Room<RavineRoom.RavineData> {
    public static final MapCodec<RavineRoom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        tagsCodec(),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("length", UniformInt.of(70, 100)).forGetter(room -> room.length),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("height", UniformInt.of(80, 120)).forGetter(room -> room.height),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("width", UniformInt.of(10, 20)).forGetter(room -> room.width),
        DIDCodecs.NON_NEGATIVE_FLOAT_PROVIDER.optionalFieldOf("turn", UniformFloat.of(0, 30)).forGetter(room -> room.turn),
        Codec.doubleRange(0, 1).optionalFieldOf("height_vary_chance", 0.2).forGetter(room -> room.heightVaryChance)
    ).apply(instance, RavineRoom::new));

    private final IntProvider length;
    private final IntProvider height;
    private final IntProvider width;
    private final FloatProvider turn;
    private final double heightVaryChance;

    private RavineRoom(List<String> tags, IntProvider length, IntProvider height, IntProvider width, FloatProvider turn, double heightVaryChance) {
        super(tags);
        this.length = length;
        this.height = height;
        this.width = width;
        this.turn = turn;
        this.heightVaryChance = heightVaryChance;
    }

    @Override
    public RoomType<?> type() {
        return RoomType.RAVINE.get();
    }

    @Override
    public RavineData createUserData(CaveGenContext ctx, RoomData roomData) {
        int length = this.length.sample(ctx.rand);
        int height = this.height.sample(ctx.rand);
        int width = this.width.sample(ctx.rand);
        double turn = this.turn.sample(ctx.rand);
        if (ctx.rand.nextBoolean()) {
            turn = -turn;
        }
        // move the origin to the center of the ravine
        Vec3 origin = roomData.location().add(roomData.direction().scale(width * 0.5));
        Vec3 entrance = getRandomEntranceLocation(ctx, origin, roomData.direction(), length, height, width, turn);
        // entrance should in fact be at location, move the origin of the ravine
        origin = origin.add(roomData.location().subtract(entrance));
        return new RavineData(length, height, width, turn, origin);
    }

    @Override
    public Vec3 adjustLocation(CaveGenContext ctx, RoomData roomData, RavineData userData) {
        // Get an exit location by getting an entrance location but inverting stuff
        return getRandomEntranceLocation(ctx, userData.origin, roomData.direction().scale(-1), userData.length, userData.height, userData.width, -userData.turn);
    }

    @Override
    public void apply(RoomCarvingData carvingData, CaveGenContext ctx, RoomData roomData, RavineData userData) {
        int length = userData.length;
        int height = userData.height;
        int width = userData.width;
        double turn = userData.turn;
        Vec3 origin = userData.origin;

        // get the length of the ravine on the long side (assuming it's the max width all the time and doesn't get thinner on each side)
        // circumference = length * 360/turn
        // radius = circumference/2pi
        // longRadius = radius + width/2
        // longCircumference = longRadius*2pi
        // longLength = longCircumference * turn/360
        // = longRadius*2pi * turn/360
        // = (radius + width/2)*2pi * turn/360
        // = (circumference/2pi + width/2)*2pi * turn/360
        // = ((length * 360/turn)/2pi + width/2)*2pi * turn/360
        // = length + (width/2)*2pi*turn/360
        // = length + width*pi*turn/360
        double longLength = length + width * (Math.PI / 360) * Math.abs(turn);

        float turnPerStep = (float) Math.toRadians(turn / longLength);
        double stepLength = length / longLength;
        for (int dir : new int[]{-1, 1}) {
            Vec3 localPosition = origin;
            Vec3 localDirection = roomData.direction().yRot(Mth.HALF_PI * dir);

            for (int distance = 0; distance < longLength / 2; distance++) {
                double localWidth = width * Math.cos((double) distance / longLength * Math.PI);
                Vec3 horizontalVector = localDirection.yRot(Mth.HALF_PI);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < localWidth; x++) {
                        Vec3 centroidPos = localPosition.add(horizontalVector.scale(-localWidth * 0.5 + x)).add(0, y, 0);
                        carvingData.setAir(roomData.roomIndex(), BlockPos.containing(centroidPos));
                    }
                }

                localPosition = localPosition.add(localDirection.scale(stepLength));
                if (ctx.rand.nextDouble() < heightVaryChance) {
                    localPosition = ModuleGenerator.vary(ctx, localPosition);
                }
                localDirection = localDirection.yRot(turnPerStep * dir);
            }
        }
    }

    private static Vec3 getRandomEntranceLocation(CaveGenContext ctx, Vec3 origin, Vec3 direction, int length,
                                              int height, int width, double turn) {
        Vec3 pos = origin;

        // follow the center of the ravine our chosen distance along it
        final int PROPORTION_OF_LENGTH = 5;
        float turnPerBlock = (float) Math.toRadians(turn / length);
        int distance =
            ctx.rand.nextInt((length + PROPORTION_OF_LENGTH - 1) / PROPORTION_OF_LENGTH) - ctx.rand.nextInt((length + PROPORTION_OF_LENGTH - 1) / PROPORTION_OF_LENGTH);
        Vec3 localDirection = direction.yRot(Math.copySign(Mth.HALF_PI, distance));
        for (int i = 0; i < Math.abs(distance); i++) {
            pos = pos.add(localDirection);
            localDirection = localDirection.yRot(turnPerBlock * Math.signum(distance));
        }

        // move to the edge of the ravine
        double localWidth = width * Math.cos((double) distance / length * Math.PI);
        pos = pos.add(localDirection.scale(localWidth * 0.5).yRot(Math.copySign(Mth.HALF_PI, distance)));

        // pick a random height
        final int PROPORTION_OF_HEIGHT = 5;
        int up =
            height / 2 + ctx.rand.nextInt((height + PROPORTION_OF_HEIGHT - 1) / PROPORTION_OF_HEIGHT) - ctx.rand.nextInt((height + PROPORTION_OF_HEIGHT - 1) / PROPORTION_OF_HEIGHT);
        pos = pos.add(0, up, 0);

        return pos;
    }

    public record RavineData(
        int length,
        int height,
        int width,
        double turn,
        Vec3 origin
    ) {
    }
}
