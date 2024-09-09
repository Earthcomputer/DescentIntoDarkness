package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.Centroid;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.HashSet;
import java.util.Set;

public final class GlowstoneStructure extends Structure {
    public static final MapCodec<GlowstoneStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        StructureProperties.CODEC.forGetter(StructureProperties::new),
        ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("density", 1500).forGetter(structure -> structure.density),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("spread_x", 8).forGetter(structure -> structure.spreadX),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("height", 12).forGetter(structure -> structure.height),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("spread_z", 8).forGetter(structure -> structure.spreadZ),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("block", BlockStateProvider.simple(Blocks.GLOWSTONE)).forGetter(structure -> structure.block)
    ).apply(instance, GlowstoneStructure::new));

    private final int density;
    private final int spreadX;
    private final int height;
    private final int spreadZ;
    private final BlockStateProvider block;

    private GlowstoneStructure(StructureProperties props, int density, int spreadX, int height, int spreadZ, BlockStateProvider block) {
        super(props);
        this.density = density;
        this.spreadX = spreadX;
        this.height = height;
        this.spreadZ = spreadZ;
        this.block = block;
    }

    @Override
    public StructureType<?> type() {
        return StructureType.GLOWSTONE.get();
    }

    @Override
    public Direction originPositionSide() {
        return Direction.UP;
    }

    @Override
    public boolean place(CaveGenContext ctx, BlockPos pos, Centroid centroid, boolean force) {
        ctx.setBlock(pos, block, centroid);
        Set<BlockPos> placedGlowstone = new HashSet<>();
        placedGlowstone.add(pos);

        BlockPos.MutableBlockPos offsetPos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < density; i++) {
            offsetPos.setWithOffset(
                pos,
                ctx.rand.nextInt(spreadX) - ctx.rand.nextInt(spreadX),
                -ctx.rand.nextInt(height),
                ctx.rand.nextInt(spreadZ) - ctx.rand.nextInt(spreadZ)
            );

            int neighboringGlowstone = 0;
            for (Direction dir : Direction.values()) {
                if (placedGlowstone.contains(offsetPos.relative(dir))) {
                    neighboringGlowstone++;
                    if (neighboringGlowstone > 1) {
                        break;
                    }
                }
            }

            if (neighboringGlowstone == 1 && canReplace(ctx, offsetPos)) {
                ctx.setBlock(offsetPos, block, centroid);
                placedGlowstone.add(offsetPos.immutable());
            }
        }

        return !placedGlowstone.isEmpty();
    }
}
