package net.earthcomputer.descentintodarkness.generator.room;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.CaveGenerator;
import net.earthcomputer.descentintodarkness.generator.RoomCarvingData;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class BranchRoom extends Room<Object> {
    public static final MapCodec<BranchRoom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        tagsCodec(),
        DIDCodecs.floatProviderRange(-360, 360).optionalFieldOf("angle", ConstantFloat.of(90)).forGetter(room -> room.angle),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("size_reduction", ConstantInt.of(1)).forGetter(room -> room.sizeReduction),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("branch_length", UniformInt.of(20, 39)).forGetter(room -> room.branchLength),
        DIDCodecs.CHAR.optionalFieldOf("branch_symbol", 'C').forGetter(room -> room.branchSymbol)
    ).apply(instance, BranchRoom::new));

    private final FloatProvider angle;
    private final IntProvider sizeReduction;
    private final IntProvider branchLength;
    private final char branchSymbol;

    private BranchRoom(List<String> tags, FloatProvider angle, IntProvider sizeReduction, IntProvider branchLength, char branchSymbol) {
        super(tags);
        this.angle = angle;
        this.sizeReduction = sizeReduction;
        this.branchLength = branchLength;
        this.branchSymbol = branchSymbol;
    }

    @Override
    public RoomType<?> type() {
        return RoomType.BRANCH.get();
    }

    @Override
    public Vec3 adjustLocation(CaveGenContext ctx, RoomData roomData, Object userData) {
        return roomData.location();
    }

    @Override
    public void apply(RoomCarvingData carvingData, CaveGenContext ctx, RoomData roomData, Object userData) {
        int dir = ctx.rand.nextBoolean() ? 1 : -1;
        int newLength = branchLength.sample(ctx.rand);
        int sizeReduction = this.sizeReduction.sample(ctx.rand);
        Vec3 newDir = roomData.direction().yRot((float) Math.toRadians(angle.sample(ctx.rand) * dir));
        CaveGenerator.generateBranch(ctx, roomData.caveRadius() - sizeReduction, roomData.location(), newLength, branchSymbol, false, newDir);
    }

    @Override
    public boolean isBranch() {
        return true;
    }

    @Override
    public char getBranchSymbol() {
        return branchSymbol;
    }
}
