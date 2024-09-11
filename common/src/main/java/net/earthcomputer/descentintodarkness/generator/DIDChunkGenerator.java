package net.earthcomputer.descentintodarkness.generator;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.DIDConstants;
import net.earthcomputer.descentintodarkness.DIDRegistries;
import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.earthcomputer.descentintodarkness.style.CaveStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

public final class DIDChunkGenerator extends ChunkGenerator {
    public static final TicketType<ChunkPos> DID_CAVE_GEN = TicketType.create("did_cave_gen", Comparator.comparingLong(ChunkPos::toLong));

    private static final MapCodec<DIDChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BiomeSource.CODEC.fieldOf("biome_source").forGetter(DIDChunkGenerator::getBiomeSource),
        RegistryFileCodec.create(DIDRegistries.CAVE_STYLE, CaveStyle.CODEC).fieldOf("style").forGetter(gen -> gen.style),
        Codec.INT.fieldOf("size").forGetter(gen -> gen.size),
        Codec.LONG.fieldOf("seed").forGetter(gen -> gen.seed),
        Codec.BOOL.fieldOf("debug").forGetter(gen -> gen.debug)
    ).apply(instance, (biomeSource1, style1, size1, seed1, debug1) -> new DIDChunkGenerator(biomeSource1, style1, size1, seed1, debug1, CaveGenProgressListener.EMPTY)));

    private final Holder<CaveStyle> style;
    private final PackedBlockStorage blockStorage;
    private final int size;
    private final long seed;
    private final boolean debug;
    private final CaveGenProgressListener listener;
    private volatile boolean generatedCave = false;

    private DIDChunkGenerator(BiomeSource biomeSource, Holder<CaveStyle> style, int size, long seed, boolean debug, CaveGenProgressListener listener) {
        super(biomeSource);
        this.style = style;
        this.blockStorage = new PackedBlockStorage(style.value().baseBlock());
        this.size = size;
        this.seed = seed;
        this.debug = debug;
        this.listener = listener;
    }

    public DIDChunkGenerator(RegistryAccess registryAccess, Holder<CaveStyle> style, int size, long seed, boolean debug, CaveGenProgressListener listener) {
        this(new FixedBiomeSource(style.value().getBiome(registryAccess)), style, size, seed, debug, listener);
    }

    public static void register() {
        DIDRegistries.REGISTRAR_MANAGER.get(Registries.CHUNK_GENERATOR).register(DescentIntoDarkness.id(DescentIntoDarkness.MOD_ID), () -> CODEC);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion worldGenRegion, long l, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunkAccess, GenerationStep.Carving carving) {
    }

    @Override
    public void buildSurface(WorldGenRegion worldGenRegion, StructureManager structureManager, RandomState randomState, ChunkAccess chunkAccess) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {
    }

    @Override
    public int getGenDepth() {
        return DIDConstants.WORLD_HEIGHT;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunkAccess) {
        Heightmap[] heightmaps = { chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG), chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG) };

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = DIDConstants.MIN_Y; y <= DIDConstants.MAX_Y; y++) {
            BlockState state = y == DIDConstants.MIN_Y || y == DIDConstants.MAX_Y ? Blocks.BEDROCK.defaultBlockState() : style.value().baseBlock();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    chunkAccess.setBlockState(pos.set(x, y, z), state, false);
                }
            }
        }

        for (Heightmap heightmap : heightmaps) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    heightmap.update(x, DIDConstants.MAX_Y, z, Blocks.BEDROCK.defaultBlockState());
                }
            }
        }

        return CompletableFuture.completedFuture(chunkAccess);
    }

    @Override
    public int getSeaLevel() {
        return style.value().startY();
    }

    @Override
    public int getMinY() {
        return DIDConstants.MIN_Y;
    }

    @Override
    public int getBaseHeight(int i, int j, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return DIDConstants.WORLD_HEIGHT;
    }

    @Override
    public NoiseColumn getBaseColumn(int i, int j, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return new NoiseColumn(
            DIDConstants.WORLD_HEIGHT,
            IntStream.rangeClosed(DIDConstants.MIN_Y, DIDConstants.MAX_Y)
                .mapToObj(y -> y == DIDConstants.MIN_Y || y == DIDConstants.MAX_Y ? Blocks.BEDROCK.defaultBlockState() : style.value().baseBlock())
                .toArray(BlockState[]::new)
        );
    }

    @Override
    public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos blockPos) {
        list.add("Cave Style: " + style.unwrapKey().orElseThrow().location());
    }

    @Override
    public @Nullable Pair<BlockPos, Holder<Structure>> findNearestMapStructure(ServerLevel serverLevel, HolderSet<Structure> holderSet, BlockPos blockPos, int i, boolean bl) {
        return null;
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel worldGenLevel, ChunkAccess chunkAccess, StructureManager structureManager) {
        if (!generatedCave) {
            synchronized (this) {
                if (!generatedCave) {
                    try (CaveGenContext ctx = new CaveGenContext(worldGenLevel, style, seed, blockStorage).setDebug(debug).setListener(listener)) {
                        CaveGenerator.generateCave(ctx, size);
                    }
                    generatedCave = true;
                }
            }
        }
        blockStorage.fillChunk(chunkAccess);
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor levelHeightAccessor) {
        return style.value().startY();
    }

    @Override
    public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(Holder<Biome> holder, StructureManager structureManager, MobCategory mobCategory, BlockPos blockPos) {
        return WeightedRandomList.create();
    }

    @Override
    public void createStructures(RegistryAccess registryAccess, ChunkGeneratorStructureState chunkGeneratorStructureState, StructureManager structureManager, ChunkAccess chunkAccess, StructureTemplateManager structureTemplateManager) {
    }

    @Override
    public void createReferences(WorldGenLevel worldGenLevel, StructureManager structureManager, ChunkAccess chunkAccess) {
    }
}
