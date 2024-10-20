package net.earthcomputer.descentintodarkness.generator;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.earthcomputer.descentintodarkness.DIDConstants;
import net.earthcomputer.descentintodarkness.generator.room.RoomData;
import net.earthcomputer.descentintodarkness.style.CaveStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickContainerAccess;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public final class CaveGenContext implements AutoCloseable {
    private static final ThreadLocal<CaveGenContext> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<Integer> CURRENT_ROOM = new ThreadLocal<>();

    private final WorldGenLevel asLevel;
    private final Holder<CaveStyle> style;
    public final RandomSource rand;
    public final long caveSeed;
    private boolean debug;
    private CaveGenProgressListener listener = CaveGenProgressListener.EMPTY;
    private final PackedBlockStorage blockStorage;
    private final Deque<Transform> blockTransformStack = new LinkedList<>(List.of(Transform.IDENTITY));
    private final Deque<Transform> inverseBlockTransformStack = new LinkedList<>(List.of(Transform.IDENTITY));
    private final Deque<Transform> locationTransformStack = new LinkedList<>(List.of(Transform.IDENTITY));
    private final Deque<Transform> inverseLocationTransformStack = new LinkedList<>(List.of(Transform.IDENTITY));
    @Nullable
    private BlockPos spawnPos;
    private final List<RoomData> rooms = new ArrayList<>();
    private final RoomCarvingData roomCarvingData = new RoomCarvingData();

    public CaveGenContext(WorldGenLevel level, Holder<CaveStyle> style, long caveSeed, PackedBlockStorage blockStorage) {
        this.asLevel = new AsWorldGenLevel(level);
        this.style = style;
        this.rand = RandomSource.create(caveSeed);
        this.caveSeed = caveSeed;
        this.blockStorage = blockStorage;
        CURRENT.set(this);
    }

    @Nullable
    public static CaveGenContext current() {
        return CURRENT.get();
    }

    public CaveStyle style() {
        return style.value();
    }

    public Holder<CaveStyle> styleHolder() {
        return style;
    }

    public CaveGenContext setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public boolean isDebug() {
        return debug;
    }

    public CaveGenContext setListener(CaveGenProgressListener listener) {
        this.listener = listener;
        return this;
    }

    public CaveGenProgressListener listener() {
        return listener;
    }

    public boolean setBlock(BlockPos pos, BlockState block) {
        pos = getInverseLocationTransform().transform(pos);
        block = getInverseBlockTransform().transform(block);
        if (pos.getY() <= DIDConstants.MIN_Y || pos.getY() >= DIDConstants.MAX_Y) {
            return false;
        }
        blockStorage.setBlock(pos, block);
        return true;
    }

    public boolean setBlock(BlockPos pos, BlockStateProvider provider, int roomIndex) {
        return setBlock(pos, getState(provider, pos, roomIndex));
    }

    public BlockState getBlock(BlockPos pos) {
        pos = getInverseLocationTransform().transform(pos);
        if (pos.getY() < DIDConstants.MIN_Y || pos.getY() > DIDConstants.MAX_Y) {
            return Blocks.AIR.defaultBlockState();
        }
        if (pos.getY() == DIDConstants.MIN_Y || pos.getY() == DIDConstants.MAX_Y) {
            return Blocks.BEDROCK.defaultBlockState();
        }
        BlockState block = blockStorage.getBlock(pos);
        block = getBlockTransform().transform(block);
        return block;
    }

    public void pushTransform(Transform blockTransform, Transform locationTransform) {
        blockTransformStack.push(getBlockTransform().combine(blockTransform));
        inverseBlockTransformStack.push(blockTransform.inverse().combine(getInverseBlockTransform()));
        locationTransformStack.push(getLocationTransform().combine(locationTransform));
        inverseLocationTransformStack.push(locationTransform.inverse().combine(getInverseLocationTransform()));
    }

    public void popTransform() {
        blockTransformStack.pop();
        inverseBlockTransformStack.pop();
        locationTransformStack.pop();
        inverseLocationTransformStack.pop();
    }

    /**
     * Gets the current world space -> local space block transform
     */
    public Transform getBlockTransform() {
        Transform transform = blockTransformStack.peek();
        assert transform != null;
        return transform;
    }

    /**
     * Gets the current local space -> world space block transform
     */
    public Transform getInverseBlockTransform() {
        Transform transform = inverseBlockTransformStack.peek();
        assert transform != null;
        return transform;
    }

    /**
     * Gets the current world space -> local space location transform
     */
    public Transform getLocationTransform() {
        Transform transform = locationTransformStack.peek();
        assert transform != null;
        return transform;
    }

    /**
     * Gets the current local space -> world space location transform
     */
    public Transform getInverseLocationTransform() {
        Transform transform = inverseLocationTransformStack.peek();
        assert transform != null;
        return transform;
    }

    @Nullable
    public BlockPos getSpawnPos() {
        return spawnPos;
    }

    public void setSpawnPos(@Nullable BlockPos spawnPos) {
        this.spawnPos = spawnPos;
    }

    public List<RoomData> rooms() {
        return rooms;
    }

    public RoomCarvingData roomCarvingData() {
        return roomCarvingData;
    }

    public WorldGenLevel asLevel() {
        return asLevel;
    }

    public BlockState getState(BlockStateProvider provider, BlockPos pos, int roomIndex) {
        Integer existingRoom = CURRENT_ROOM.get();
        CURRENT_ROOM.set(roomIndex);
        try {
            return provider.getState(rand, pos);
        } finally {
            CURRENT_ROOM.set(existingRoom);
        }
    }

    @Nullable
    public static Integer currentRoomIndex() {
        return CURRENT_ROOM.get();
    }

    @Override
    public void close() {
        CURRENT.remove();
    }

    private class AsWorldGenLevel extends WorldGenRegion {
        private final Long2ObjectMap<ChunkAccess> chunks = new Long2ObjectOpenHashMap<>();

        AsWorldGenLevel(WorldGenLevel level) {
            super(level.getLevel(), ((WorldGenRegion) level).cache, ((WorldGenRegion) level).generatingStep, ((WorldGenRegion) level).center);
        }

        @Override
        public boolean hasChunk(int i, int j) {
            return true;
        }

        @Override
        public boolean ensureCanWrite(BlockPos blockPos) {
            return true;
        }

        @Override
        public RandomSource getRandom() {
            return CaveGenContext.this.rand;
        }

        @Override
        public @Nullable ChunkAccess getChunk(int i, int j, ChunkStatus chunkStatus, boolean bl) {
            return chunks.computeIfAbsent(ChunkPos.asLong(i, j), k -> new ChunkAccess(new ChunkPos(i, j), UpgradeData.EMPTY, this, registryAccess().registryOrThrow(Registries.BIOME), 0, null, null) {
                private final TickContainerAccess<Block> blockTicks = new MyTickContainerAccess<>();
                private final TickContainerAccess<Fluid> fluidTicks = new MyTickContainerAccess<>();

                @Override
                public @Nullable BlockState setBlockState(BlockPos blockPos, BlockState blockState, boolean bl) {
                    blockPos = toGlobalPos(blockPos);
                    BlockState oldBlock = CaveGenContext.this.getBlock(blockPos);
                    CaveGenContext.this.setBlock(blockPos, blockState);
                    return oldBlock;
                }

                @Override
                public void setBlockEntity(BlockEntity blockEntity) {
                    // TODO
                }

                @Override
                public void addEntity(Entity entity) {
                    // TODO
                }

                @Override
                public ChunkStatus getPersistedStatus() {
                    return ChunkStatus.FEATURES;
                }

                @Override
                public void removeBlockEntity(BlockPos blockPos) {
                    // TODO
                }

                @Override
                public @Nullable CompoundTag getBlockEntityNbtForSaving(BlockPos blockPos, HolderLookup.Provider provider) {
                    throw new UnsupportedOperationException("Cannot serialize CaveGenContext chunk");
                }

                @Override
                public TickContainerAccess<Block> getBlockTicks() {
                    return blockTicks;
                }

                @Override
                public TickContainerAccess<Fluid> getFluidTicks() {
                    return fluidTicks;
                }

                @Override
                public TicksToSave getTicksForSerialization() {
                    throw new UnsupportedOperationException("Cannot serialize CaveGenContext chunk");
                }

                @Override
                public @Nullable BlockEntity getBlockEntity(BlockPos blockPos) {
                    return null; // TODO
                }

                @Override
                public BlockState getBlockState(BlockPos blockPos) {
                    return CaveGenContext.this.getBlock(toGlobalPos(blockPos));
                }

                @Override
                public FluidState getFluidState(BlockPos blockPos) {
                    return getBlockState(blockPos).getFluidState();
                }

                @Override
                public LevelChunkSection[] getSections() {
                    throw new UnsupportedOperationException("Cannot get sections for CaveGenContext chunk");
                }

                @Override
                public LevelChunkSection getSection(int i) {
                    throw new UnsupportedOperationException("Cannot get section for CaveGenContext chunk");
                }

                private BlockPos toGlobalPos(BlockPos pos) {
                    return new BlockPos(chunkPos.getMinBlockX() + (pos.getX() & 15), pos.getY(), chunkPos.getMinBlockZ() + (pos.getZ() & 15));
                }

                private class MyTickContainerAccess<T> implements TickContainerAccess<T> {
                    @Override
                    public void schedule(ScheduledTick<T> scheduledTick) {
                        // TODO
                    }

                    @Override
                    public boolean hasScheduledTick(BlockPos blockPos, T object) {
                        return false; // TODO
                    }

                    @Override
                    public int count() {
                        return 0; // TODO
                    }
                }
            });
        }
    }
}
