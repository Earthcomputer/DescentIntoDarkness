package net.earthcomputer.descentintodarkness.generator;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.earthcomputer.descentintodarkness.DIDConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.List;

public final class PackedBlockStorage {
    private final Reference2IntMap<BlockState> palette = new Reference2IntOpenHashMap<>();
    private final List<BlockState> inversePalette = new ArrayList<>();
    private int bitsPerBlock = 4;
    private int maxNumBlocks = 16;
    private int blocksPerWord = 16;
    private final BlockState defaultBlock;
    private final Long2ObjectMap<long[]> subchunks = new Long2ObjectOpenHashMap<>();

    public PackedBlockStorage(BlockState defaultBlock) {
        this.defaultBlock = defaultBlock;
    }

    private static long subchunkPos(BlockPos pos) {
        return new BlockPos(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4).asLong();
    }

    public BlockState getBlock(BlockPos pos) {
        long[] packedArray = subchunks.get(subchunkPos(pos));
        if (packedArray == null) {
            return defaultBlock;
        }

        int index = ((pos.getY() & 15) << 8) | ((pos.getZ() & 15) << 4) | (pos.getX() & 15);
        return getBlockByIndexInSubchunk(index, packedArray);
    }

    private BlockState getBlockByIndexInSubchunk(int index, long[] subchunk) {
        int indexInWord = index % blocksPerWord;
        long word = subchunk[index / blocksPerWord];
        int id = (int) (word >>> (indexInWord * bitsPerBlock)) & (maxNumBlocks - 1);
        if (id <= 0 || id > inversePalette.size()) {
            return defaultBlock;
        }
        return inversePalette.get(id - 1);
    }

    public void setBlock(BlockPos pos, BlockState block) {
        int prevSize = palette.size();
        int id = block.equals(defaultBlock) ? 0 : palette.computeIfAbsent(block, k -> prevSize + 1);
        if (palette.size() != prevSize) {
            inversePalette.add(block);
            if ((prevSize & (prevSize + 1)) == 0) {
                expandBitsPerBlock();
            }
        }

        long[] packedArray = subchunks.computeIfAbsent(subchunkPos(pos), k -> new long[(4096 + blocksPerWord - 1) / blocksPerWord]);
        int index = ((pos.getY() & 15) << 8) | ((pos.getZ() & 15) << 4) | (pos.getX() & 15);
        int indexInWord = index % blocksPerWord;
        int wordIndex = index / blocksPerWord;
        long word = packedArray[wordIndex];
        word &= ~((long) (maxNumBlocks - 1) << (indexInWord * bitsPerBlock));
        word |= (long) id << (indexInWord * bitsPerBlock);
        packedArray[wordIndex] = word;
    }

    private void expandBitsPerBlock() {
        int prevBitsPerBlock = bitsPerBlock;
        int prevMaxNumBlocks = maxNumBlocks;
        int prevBlocksPerWord = blocksPerWord;
        bitsPerBlock = prevBitsPerBlock + 1;
        maxNumBlocks = prevMaxNumBlocks << 1;
        blocksPerWord = 64 / bitsPerBlock;

        subchunks.replaceAll((pos, oldArray) -> {
            long[] newArray = new long[(4096 + blocksPerWord - 1) / blocksPerWord];
            for (int index = 0; index < 4096; index++) {
                int indexInOldWord = index % prevBlocksPerWord;
                int indexInNewWord = index % blocksPerWord;
                long oldWord = oldArray[index / prevBlocksPerWord];
                long id = (oldWord >>> (indexInOldWord * prevBitsPerBlock)) & (prevMaxNumBlocks - 1);
                newArray[index / blocksPerWord] |= id << (indexInNewWord * bitsPerBlock);
            }
            return newArray;
        });
    }

    public void fillChunk(ChunkAccess chunkAccess) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        ChunkPos chunkPos = chunkAccess.getPos();
        for (int chunkY = DIDConstants.MIN_Y >> 4; chunkY <= DIDConstants.MAX_Y >> 4; chunkY++) {
            long[] subchunk = subchunks.get(new BlockPos(chunkPos.x, chunkY, chunkPos.z).asLong());
            if (subchunk != null) {
                int index = 0;
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            chunkAccess.setBlockState(pos.set(x, (chunkY << 4) + y, z), getBlockByIndexInSubchunk(index++, subchunk), false);
                        }
                    }
                }
            }
        }
    }
}
