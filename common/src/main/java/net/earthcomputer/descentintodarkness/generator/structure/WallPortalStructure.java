package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.DIDConstants;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.PlacementEdge;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.List;
import java.util.Optional;

public final class WallPortalStructure extends Structure {
    public static final MapCodec<WallPortalStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        StructureProperties.CODEC.forGetter(StructureProperties::new),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("width", ConstantInt.of(2)).forGetter(structure -> structure.width),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("height", ConstantInt.of(3)).forGetter(structure -> structure.height),
        SnappingSide.CODEC.optionalFieldOf("snapping_side", SnappingSide.NONE).forGetter(structure -> structure.snappingSide),
        DIDCodecs.BLOCK_PREDICATE.optionalFieldOf("portal_clear_blocks").forGetter(structure -> structure.portalClearBlocks),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("portal_block", BlockStateProvider.simple(Blocks.NETHER_PORTAL)).forGetter(structure -> structure.portalBlock),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("frame_block").forGetter(structure -> structure.frameBlock)
    ).apply(instance, WallPortalStructure::new));

    private final IntProvider width;
    private final IntProvider height;
    private final SnappingSide snappingSide;
    private final Optional<BlockPredicate> portalClearBlocks;
    private final BlockStateProvider portalBlock;
    private final Optional<BlockStateProvider> frameBlock;

    private WallPortalStructure(StructureProperties props, IntProvider width, IntProvider height, SnappingSide snappingSide, Optional<BlockPredicate> portalClearBlocks, BlockStateProvider portalBlock, Optional<BlockStateProvider> frameBlock) {
        super(props);
        this.width = width;
        this.height = height;
        this.snappingSide = snappingSide;
        this.portalClearBlocks = portalClearBlocks;
        this.portalBlock = portalBlock;
        this.frameBlock = frameBlock;
    }

    @Override
    protected List<PlacementEdge> getDefaultEdges() {
        return List.of(PlacementEdge.WALL);
    }

    @Override
    protected boolean shouldTransformBlocksByDefault() {
        return true;
    }

    @Override
    protected Direction getDefaultOriginSide(List<PlacementEdge> edges) {
        return Direction.SOUTH;
    }

    @Override
    protected boolean shouldSnapToAxisByDefault() {
        return true;
    }

    @Override
    public Direction originPositionSide() {
        return Direction.SOUTH;
    }

    @Override
    protected boolean defaultCanReplace(CaveGenContext ctx, BlockPos pos) {
        return !ctx.style().isTransparentBlock(ctx, pos);
    }

    @Override
    public StructureType<?> type() {
        return StructureType.WALL_PORTAL.get();
    }

    @Override
    public boolean place(CaveGenContext ctx, BlockPos pos, int roomIndex, boolean force) {
        int width = this.width.sample(ctx.rand);
        int height = this.height.sample(ctx.rand);
        int x = pos.getX() - width / 2;
        if (width % 2 == 0 && ctx.rand.nextBoolean()) {
            x++;
        }
        int y = pos.getY() - height / 2;
        if (height % 2 == 0 && ctx.rand.nextBoolean()) {
            y++;
        }
        int z = pos.getZ();
        boolean canPlace = true;
        outer:
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (!canReplace(ctx, new BlockPos(x + i, y + j, z))) {
                    canPlace = false;
                    break outer;
                }
                if (!isPortalClearBlock(ctx, new BlockPos(x + i, y + j, z + 1))) {
                    canPlace = false;
                    break outer;
                }
            }
        }
        if (!force && !canPlace) {
            return false;
        }
        if (canPlace && snappingSide != SnappingSide.NONE) {
            if (snappingSide == SnappingSide.FLOOR) {
                outer:
                while (y >= DIDConstants.MIN_Y) {
                    y--;
                    for (int i = 0; i < width; i++) {
                        if (!canReplace(ctx, new BlockPos(x + i, y, z))) {
                            break outer;
                        }
                        if (!isPortalClearBlock(ctx, new BlockPos(x + i, y, z + 1))) {
                            break outer;
                        }
                    }
                }
                y++;
            } else {
                outer:
                while (y <= DIDConstants.MAX_Y) {
                    y++;
                    for (int i = 0; i < width; i++) {
                        if (!canReplace(ctx, new BlockPos(x + i, y + height - 1, z))) {
                            break outer;
                        }
                        if (!isPortalClearBlock(ctx, new BlockPos(x + i, y + height - 1, z + 1))) {
                            break outer;
                        }
                    }
                }
                y--;
            }
        }

        for (int i = -1; i <= width; i++) {
            for (int j = -1; j <= height; j++) {
                if (i == -1 || i == width || j == -1 || j == height) {
                    if (frameBlock.isPresent()) {
                        ctx.setBlock(new BlockPos(x + i, y + j, z), frameBlock.get(), roomIndex);
                    }
                } else {
                    ctx.setBlock(new BlockPos(x + i, y + j, z), portalBlock, roomIndex);
                }
            }
        }

        return true;
    }

    private boolean isPortalClearBlock(CaveGenContext ctx, BlockPos pos) {
        return portalClearBlocks.map(blockPredicate -> blockPredicate.test(ctx.asLevel(), pos)).orElseGet(() -> ctx.style().isTransparentBlock(ctx, pos));
    }

    private enum SnappingSide implements StringRepresentable {
        NONE("none"),
        FLOOR("floor"),
        CEILING("ceiling"),
        ;

        static final Codec<SnappingSide> CODEC = StringRepresentable.fromEnum(SnappingSide::values);

        private final String name;

        SnappingSide(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
