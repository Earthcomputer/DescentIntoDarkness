package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.earthcomputer.descentintodarkness.generator.Centroid;
import net.earthcomputer.descentintodarkness.generator.Transform;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.Optional;

public final class VinePatchStructure extends AbstractPatchStructure {
    public static final MapCodec<VinePatchStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        StructureProperties.CODEC.forGetter(StructureProperties::new),
        PatchProperties.CODEC.forGetter(PatchProperties::new),
        DIDCodecs.BLOCK_STATE_PROVIDER.fieldOf("vine").forGetter(structure -> structure.vine),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("first_block").forGetter(structure -> structure.firstBlock),
        DIDCodecs.BLOCK_STATE_PROVIDER.optionalFieldOf("last_block").forGetter(structure -> structure.lastBlock),
        DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("height", UniformInt.of(5, 10)).forGetter(structure -> structure.height),
        Codec.BOOL.optionalFieldOf("vine_random_rotation", true).forGetter(structure -> structure.vineRandomRotation)
    ).apply(instance, VinePatchStructure::new));

    private final BlockStateProvider vine;
    private final Optional<BlockStateProvider> firstBlock;
    private final Optional<BlockStateProvider> lastBlock;
    private final IntProvider height;
    private final boolean vineRandomRotation;

    private VinePatchStructure(StructureProperties props, PatchProperties patchProps, BlockStateProvider vine, Optional<BlockStateProvider> firstBlock, Optional<BlockStateProvider> lastBlock, IntProvider height, boolean vineRandomRotation) {
        super(props, patchProps);
        this.vine = vine;
        this.firstBlock = firstBlock;
        this.lastBlock = lastBlock;
        this.height = height;
        this.vineRandomRotation = vineRandomRotation;
    }

    @Override
    public StructureType<?> type() {
        return StructureType.VINE_PATCH.get();
    }

    @Override
    public Direction originPositionSide() {
        return Direction.UP;
    }

    @Override
    protected boolean doPlace(CaveGenContext ctx, BlockPos pos, Centroid centroid) {
        int height = this.height.sample(ctx.rand);
        int angle = vineRandomRotation ? ctx.rand.nextInt(4) * 90 : 0;
        Transform transform = Transform.rotateY(Math.toRadians(angle));

        BlockStateProvider firstBlock = this.firstBlock.orElse(this.vine);
        BlockStateProvider lastBlock = this.lastBlock.orElse(this.vine);

        BlockPos offsetPos = pos;
        boolean placed = false;
        for (int i = 0; i < height && canReplace(ctx, offsetPos); i++) {
            ctx.setBlock(offsetPos, transform.transform(i == 0 ? ctx.getState(firstBlock, offsetPos, centroid) : ctx.getState(vine, offsetPos, centroid)));
            placed = true;
            offsetPos = offsetPos.below();
        }
        offsetPos = offsetPos.above();
        if (placed) {
            ctx.setBlock(offsetPos, transform.transform(ctx.getState(lastBlock, offsetPos, centroid)));
        }

        return placed;
    }
}
