package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.Centroid;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractPatchStructure extends Structure {
    private final int spreadX;
    private final int spreadY;
    private final int spreadZ;
    private final boolean spreadLocal;
    private final int tries;

    protected AbstractPatchStructure(StructureProperties props, PatchProperties patchProps) {
        super(props);
        this.spreadX = patchProps.spreadX;
        this.spreadY = patchProps.spreadY;
        this.spreadZ = patchProps.spreadZ;
        this.spreadLocal = patchProps.spreadLocal;
        this.tries = patchProps.tries;
    }

    @Override
    public boolean place(CaveGenContext ctx, BlockPos pos, Centroid centroid, boolean force) {
        BlockPos origin = pos.relative(originPositionSide().getOpposite());
        BlockPos spread = new BlockPos(spreadX, spreadY, spreadZ);
        if (!spreadLocal) {
            spread = BlockPos.containing(ctx.getLocationTransform().transformDirection(Vec3.atLowerCornerOf(spread)).add(0.5, 0.5, 0.5));
            spread = new BlockPos(Math.abs(spread.getX()), Math.abs(spread.getY()), Math.abs(spread.getZ()));
        }
        boolean placed = false;
        for (int i = 0; i < tries; i++) {
            BlockPos offsetPos = origin.offset(
                ctx.rand.nextInt(spread.getX() + 1) - ctx.rand.nextInt(spread.getX() + 1),
                ctx.rand.nextInt(spread.getY() + 1) - ctx.rand.nextInt(spread.getY() + 1),
                ctx.rand.nextInt(spread.getZ() + 1) - ctx.rand.nextInt(spread.getZ() + 1)
            );
            if (canReplace(ctx, offsetPos)) {
                if (canPlaceOn(ctx, offsetPos.relative(originPositionSide()))) {
                    placed |= doPlace(ctx, offsetPos, centroid);
                }
            }
        }

        return placed;
    }

    protected abstract boolean doPlace(CaveGenContext ctx, BlockPos pos, Centroid centroid);

    protected record PatchProperties(
        int spreadX,
        int spreadY,
        int spreadZ,
        boolean spreadLocal,
        int tries
    ) {
        public PatchProperties(AbstractPatchStructure structure) {
            this(
                structure.spreadX,
                structure.spreadY,
                structure.spreadZ,
                structure.spreadLocal,
                structure.tries
            );
        }

        public static final MapCodec<PatchProperties> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("spread_x", 8).forGetter(PatchProperties::spreadX),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("spread_y", 4).forGetter(PatchProperties::spreadY),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("spread_z", 8).forGetter(PatchProperties::spreadZ),
            Codec.BOOL.optionalFieldOf("spread_local", false).forGetter(PatchProperties::spreadLocal),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("tries", 64).forGetter(PatchProperties::tries)
        ).apply(instance, PatchProperties::new));
    }
}
