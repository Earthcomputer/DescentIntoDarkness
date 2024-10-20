package net.earthcomputer.descentintodarkness.blockstateprovider;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.earthcomputer.descentintodarkness.generator.CaveGenContext;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;

public final class RoomWeightedStateProvider extends BlockStateProvider {
    public static final MapCodec<RoomWeightedStateProvider> CODEC = SimpleWeightedRandomList.wrappedCodec(BlockState.CODEC)
        .comapFlatMap(RoomWeightedStateProvider::create, weightedStateProvider -> weightedStateProvider.weightedList)
        .fieldOf("entries");
    private final SimpleWeightedRandomList<BlockState> weightedList;

    private static DataResult<RoomWeightedStateProvider> create(SimpleWeightedRandomList<BlockState> weightedList) {
        return weightedList.isEmpty()
            ? DataResult.error(() -> "RoomWeightedStateProvider with no states")
            : DataResult.success(new RoomWeightedStateProvider(weightedList));
    }

    private RoomWeightedStateProvider(SimpleWeightedRandomList<BlockState> weightedList) {
        this.weightedList = weightedList;
    }

    @Override
    protected BlockStateProviderType<?> type() {
        return DIDBlockStateProviderTypes.ROOM_WEIGHTED.get();
    }

    @Override
    public BlockState getState(RandomSource rand, BlockPos pos) {
        CaveGenContext ctx = CaveGenContext.current();
        Integer roomIndex = CaveGenContext.currentRoomIndex();
        if (ctx == null || roomIndex == null) {
            return weightedList.getRandomValue(rand).orElseThrow();
        }

        RandomSource centroidRand = RandomSource.create(ctx.caveSeed + 133742069L * roomIndex);
        return weightedList.getRandomValue(centroidRand).orElseThrow();
    }
}
