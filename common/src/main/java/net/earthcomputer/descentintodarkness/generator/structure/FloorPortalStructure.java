package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.DIDConstants;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.Centroid;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.List;
import java.util.Optional;

public final class FloorPortalStructure extends Structure {
    public static final MapCodec<FloorPortalStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        StructureProperties.CODEC.forGetter(StructureProperties::new),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("width", ConstantInt.of(2)).forGetter(structure -> structure.width),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("height", ConstantInt.of(3)).forGetter(structure -> structure.height),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("portal_block", BlockStateProvider.simple(Blocks.NETHER_PORTAL)).forGetter(structure -> structure.portalBlock),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("frame_block").forGetter(structure -> structure.frameBlock),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("floor_block").forGetter(structure -> structure.floorBlock)
    ).apply(instance, FloorPortalStructure::new));

    private final IntProvider width;
    private final IntProvider height;
    private final BlockStateProvider portalBlock;
    private final Optional<BlockStateProvider> frameBlock;
    private final Optional<BlockStateProvider> floorBlock;

    private FloorPortalStructure(StructureProperties props, IntProvider width, IntProvider height, BlockStateProvider portalBlock, Optional<BlockStateProvider> frameBlock, Optional<BlockStateProvider> floorBlock) {
        super(props);
        this.width = width;
        this.height = height;
        this.portalBlock = portalBlock;
        this.frameBlock = frameBlock;
        this.floorBlock = floorBlock;
    }

    @Override
    protected List<StructurePlacementEdge> getDefaultEdges() {
        return List.of(StructurePlacementEdge.FLOOR);
    }

    @Override
    protected boolean shouldTransformBlocksByDefault() {
        return true;
    }

    @Override
    protected boolean shouldSnapToAxisByDefault() {
        return true;
    }

    @Override
    public StructureType<?> type() {
        return StructureType.FLOOR_PORTAL.get();
    }

    @Override
    public boolean place(CaveGenContext ctx, BlockPos pos, Centroid centroid, boolean force) {
        int width = this.width.sample(ctx.rand);
        int height = this.height.sample(ctx.rand);
        int x = pos.getX() - width / 2;
        if (width % 2 == 0 && ctx.rand.nextBoolean()) {
            x++;
        }

        BlockPos.MutableBlockPos offsetPos = new BlockPos.MutableBlockPos();
        if (!force) {
            for (int i = -1; i <= width; i++) {
                if (!canPlaceOn(ctx, offsetPos.set(x + i, pos.getY(), pos.getZ()))) {
                    return false;
                }
                for (int j = 1; j <= height + 1; j++) {
                    if (!canReplace(ctx, offsetPos.set(x + i, pos.getY() + j, pos.getZ()))) {
                        return false;
                    }
                }
            }
        }

        for (int i = -1; i <= width; i++) {
            for (int j = 0; j <= height + 1; j++) {
                offsetPos.set(x + i, pos.getY() + j, pos.getZ());
                if (i == -1 || i == width || j == 0 || j == height + 1) {
                    BlockState frameBlock = this.frameBlock.map(blockStateProvider -> ctx.getState(blockStateProvider, offsetPos, centroid)).orElseGet(() -> ctx.style().baseBlock());
                    ctx.setBlock(offsetPos, frameBlock);
                } else {
                    ctx.setBlock(offsetPos, portalBlock, centroid);
                }
            }
            for (int j : new int[]{-1, 1}) {
                offsetPos.set(x + i, pos.getY(), pos.getZ() + j);
                if (ctx.style().isTransparentBlock(ctx, offsetPos)) {
                    BlockState floorBlock = this.floorBlock.map(blockStateProvider -> ctx.getState(blockStateProvider, offsetPos, centroid)).orElseGet(() -> ctx.style().baseBlock());
                    ctx.setBlock(offsetPos, floorBlock);
                }
            }
        }

        boolean escapeExists = false;
        escapeSearchLoop:
        for (int dx = 0; dx < width; dx++) {
            for (int dz : new int[]{-1, 1}) {
                offsetPos.set(x + dx, pos.getY() + 1, pos.getZ() + dz);
                BlockPos posAbove = offsetPos.above();
                if (ctx.style().isTransparentBlock(ctx, offsetPos) && ctx.style().isTransparentBlock(ctx, posAbove)) {
                    escapeExists = true;
                    break escapeSearchLoop;
                }
            }
        }

        if (!escapeExists) {
            for (int dx = 0; dx < width; dx++) {
                for (int dy = 1; dy <= height; dy++) {
                    for (int dz : new int[]{-1, 1}) {
                        offsetPos.setWithOffset(pos, dx, dy, dz);
                        ctx.setBlock(offsetPos, ctx.style().getAirBlock(offsetPos.getY(), centroid, DIDConstants.MIN_Y, DIDConstants.MAX_Y), centroid);
                    }
                }
            }
        }

        return true;
    }
}
