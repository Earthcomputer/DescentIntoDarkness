package net.earthcomputer.descentintodarkness.style;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.DIDConstants;
import net.earthcomputer.descentintodarkness.DIDRegistries;
import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.earthcomputer.descentintodarkness.generator.Centroid;
import net.earthcomputer.descentintodarkness.generator.GrammarGraph;
import net.earthcomputer.descentintodarkness.generator.painter.PainterStep;
import net.earthcomputer.descentintodarkness.generator.room.Room;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record CaveStyle(
    MetaProperties meta,
    BlockProperties block,
    SpawningProperties spawning,
    GenProperties gen
) {
    public static final ResourceKey<CaveStyle> DEFAULT = ResourceKey.create(DIDRegistries.CAVE_STYLE, DescentIntoDarkness.id("default"));

    public static final Codec<CaveStyle> CODEC = RecordCodecBuilder.<CaveStyle>create(instance -> instance.group(
        MetaProperties.CODEC.forGetter(CaveStyle::meta),
        BlockProperties.CODEC.forGetter(CaveStyle::block),
        SpawningProperties.CODEC.forGetter(CaveStyle::spawning),
        GenProperties.CODEC.forGetter(CaveStyle::gen)
    ).apply(instance, CaveStyle::new)).validate(style -> {
        if (!style.meta.isAbstract) {
            if (style.gen.grammar.isEmpty()) {
                return DataResult.error(() -> "Cave style has no grammar");
            }
            Set<Character> startingSymbols = style.gen.rooms.values().stream().filter(Room::isBranch).map(Room::getBranchSymbol).collect(Collectors.toCollection(LinkedHashSet::new));
            startingSymbols.add('C');
            DataResult<GrammarGraph> result = style.gen.grammar.get().validate(startingSymbols, style.gen.rooms.keySet());
            if (result.isError()) {
                return result.map(grammar -> style);
            }
            if (style.gen.continuationSymbol.isPresent()) {
                if (!style.gen.grammar.get().hasRuleSet(style.gen.continuationSymbol.get()) && !style.gen.rooms.containsKey(style.gen.continuationSymbol.get())) {
                    return DataResult.error(() -> "Continuation symbol '" + style.gen.continuationSymbol.get() + "' is not a rule set in the grammar");
                }
            }
        }

        return DataResult.success(style);
    });

    public static final Codec<CaveStyle> NETWORK_CODEC = Codec.unit(new CaveStyle(
        new MetaProperties(Optional.empty(), List.of(), true, 0),
        new BlockProperties(BlockTypeRange.simpleInt(BlockStateProvider.simple(Blocks.AIR)), Map.of(), Map.of(), Blocks.STONE.defaultBlockState(), BlockPredicate.not(BlockPredicate.alwaysTrue()), HolderSet.empty()),
        new SpawningProperties(Map.of(), Map.of(), 0, 0, 0, 0, 0),
        new GenProperties(ConstantInt.of(1), ConstantInt.of(1), 0, true, ConstantInt.of(0), ConstantInt.of(0), Optional.empty(), false, Map.of(), Optional.empty(), Optional.empty(), true, List.of(), HolderSet.empty(), HolderSet.empty())
    ));

    public static void loadCaveStyles(
        ResourceManager resourceManager,
        RegistryOps.RegistryInfoLookup infoLookup,
        WritableRegistry<CaveStyle> registry,
        Decoder<CaveStyle> decoder,
        Map<ResourceKey<?>, Exception> loadingErrors
    ) {
        String dirPath = DescentIntoDarkness.MOD_ID + "/" + Registries.elementsDirPath(DIDRegistries.CAVE_STYLE);
        FileToIdConverter fileToIdConverter = FileToIdConverter.json(dirPath);
        RegistryOps<JsonElement> registryOps = RegistryOps.create(JsonOps.INSTANCE, infoLookup);

        Map<ResourceKey<CaveStyle>, JsonObject> styleJsons = new LinkedHashMap<>();
        Map<ResourceKey<CaveStyle>, RegistrationInfo> styleRegistrationInfos = new HashMap<>();

        // find resources and parse json
        for (var entry : fileToIdConverter.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation caveStyleFileLoc = entry.getKey();
            ResourceKey<CaveStyle> styleKey = ResourceKey.create(DIDRegistries.CAVE_STYLE, fileToIdConverter.fileToId(caveStyleFileLoc));
            Resource resource = entry.getValue();
            RegistrationInfo registrationInfo = RegistryDataLoader.REGISTRATION_INFO_CACHE.apply(resource.knownPackInfo());

            try (Reader reader = resource.openAsReader()) {
                styleJsons.put(styleKey, GsonHelper.convertToJsonObject(JsonParser.parseReader(reader), "<root>"));
                styleRegistrationInfos.put(styleKey, registrationInfo);
            } catch (Exception e) {
                loadingErrors.put(
                    styleKey, new IllegalStateException(String.format(Locale.ROOT, "Failed to parse %s from pack %s", caveStyleFileLoc, resource.sourcePackId()), e)
                );
            }
        }

        Set<ResourceKey<CaveStyle>> styleStack = new HashSet<>();
        Set<ResourceKey<CaveStyle>> processedStyles = new HashSet<>();

        for (var entry : styleJsons.entrySet()) {
            ResourceKey<CaveStyle> styleKey = entry.getKey();
            JsonObject styleJson = entry.getValue();
            inlineCaveStyleInheritance(styleJsons, styleKey, styleJson, styleStack, processedStyles);
        }

        for (JsonObject styleJson : styleJsons.values()) {
            inlineDefaultInheritance(styleJsons, styleJson);
        }

        // register the inlined cave styles
        for (var entry : styleJsons.entrySet()) {
            ResourceKey<CaveStyle> styleKey = entry.getKey();
            JsonObject json = entry.getValue();
            DataResult<CaveStyle> result = decoder.parse(registryOps, json);
            CaveStyle caveStyle;
            try {
                caveStyle = result.getOrThrow();
            } catch (Exception e) {
                loadingErrors.put(styleKey, e);
                continue;
            }
            registry.register(styleKey, caveStyle, styleRegistrationInfos.get(styleKey));
        }
    }

    private static void inlineCaveStyleInheritance(
        Map<ResourceKey<CaveStyle>, JsonObject> styleJsons,
        ResourceKey<CaveStyle> styleKey,
        JsonObject styleJson,
        Set<ResourceKey<CaveStyle>> styleStack,
        Set<ResourceKey<CaveStyle>> processedStyles
    ) {
        if (styleStack.contains(styleKey)) {
            throw new IllegalStateException("Detected cyclic cave style inheritance");
        }
        if (processedStyles.contains(styleKey)) {
            return;
        }
        styleStack.add(styleKey);
        processedStyles.add(styleKey);

        class InheritanceData {
            final ResourceKey<CaveStyle> key;
            final Set<String> mergeTop = new HashSet<>();
            final Set<String> mergeBottom = new HashSet<>();
            final Set<String> mergeSkip = new HashSet<>();

            InheritanceData(ResourceKey<CaveStyle> key) {
                this.key = key;
            }
        }
        List<InheritanceData> parents = new ArrayList<>();
        JsonElement inherit = styleJson.get("inherit");
        List<JsonElement> inheritList = inherit instanceof JsonArray list ? list.asList() : inherit != null ? List.of(inherit) : List.of();
        for (JsonElement parent : inheritList) {
            if (parent instanceof JsonObject map) {
                String name = GsonHelper.getAsString(map, "name");
                InheritanceData data = new InheritanceData(ResourceKey.create(DIDRegistries.CAVE_STYLE, ResourceLocation.parse(name)));
                if (map.get("merge") instanceof JsonObject mergeMap) {
                    for (var entry : mergeMap.entrySet()) {
                        String key = entry.getKey();
                        String val = GsonHelper.convertToString(entry.getValue(), key);
                        switch (val) {
                            case "top" -> data.mergeTop.add(key);
                            case "bottom" -> data.mergeBottom.add(key);
                            case "skip" -> data.mergeSkip.add(key);
                            default -> throw new IllegalStateException("Complex inherit merge must be either \"top\", \"bottom\" or \"skip\"");
                        }
                    }
                }
                parents.add(data);
            } else {
                String parentName = GsonHelper.convertToString(parent, "inherit");
                parents.add(new InheritanceData(ResourceKey.create(DIDRegistries.CAVE_STYLE, ResourceLocation.parse(parentName))));
            }
        }

        Map<String, ResourceKey<CaveStyle>> alreadyMerged = new HashMap<>();

        for (InheritanceData parent : parents) {
            JsonObject parentJson = styleJsons.get(parent.key);
            if (parentJson == null) {
                throw new IllegalStateException("Tried to inherit from cave style \"" + parent.key.location() + "\" which does not exist");
            }
            inlineCaveStyleInheritance(styleJsons, parent.key, parentJson, styleStack, processedStyles);

            for (var entry : parentJson.entrySet()) {
                String key = entry.getKey();
                JsonElement val = entry.getValue();
                if ("inherit".equals(key) || "abstract".equals(key) || "displayName".equals(key) || "__builtin_no_default_inherit".equals(key) || parent.mergeSkip.contains(key)) {
                    continue;
                }

                if (styleJson.has(key)) {
                    if (parent.mergeBottom.contains(key)) {
                        JsonElement ourVal = styleJson.get(key);
                        if (val instanceof JsonArray parentArray) {
                            if (!(ourVal instanceof JsonArray ourArray)) {
                                throw new IllegalStateException("Cannot merge mismatching types under key \"" + key + "\"");
                            }
                            ourArray.addAll(parentArray);
                        } else if (val instanceof JsonObject parentObj) {
                            if (!(ourVal instanceof JsonObject ourObj)) {
                                throw new IllegalStateException("Cannot merge mismatching types under key \"" + key + "\"");
                            }
                            ourObj.asMap().putAll(parentObj.asMap());
                        } else {
                            throw new IllegalStateException("Cannot merge type under key \"" + key + "\"");
                        }
                    } else if (parent.mergeTop.contains(key)) {
                        JsonElement ourVal = styleJson.get(key);
                        if (val instanceof JsonArray parentArray) {
                            if (!(ourVal instanceof JsonArray ourArray)) {
                                throw new IllegalStateException("Cannot merge mismatching types under key \"" + key + "\"");
                            }
                            ourArray.asList().addAll(0, parentArray.asList());
                        } else if (val instanceof JsonObject parentObj) {
                            if (!(ourVal instanceof JsonObject ourObj)) {
                                throw new IllegalStateException("Cannot merge mismatching types under key \"" + key + "\"");
                            }
                            JsonObject ourCopy = parentObj.deepCopy();
                            ourObj.asMap().clear();
                            ourObj.asMap().putAll(parentObj.asMap());
                            ourObj.asMap().putAll(ourCopy.asMap());
                        } else {
                            throw new IllegalStateException("Cannot merge type under key \"" + key + "\"");
                        }
                    } else if (alreadyMerged.containsKey(key)) {
                        throw new IllegalStateException(styleKey.location() + ": Cannot replace \"" + key + "\" from \"" + parent.key.location() + "\", already inherited that key from \"" + alreadyMerged.get(key).location() + "\".");
                    }
                } else {
                    styleJson.add(key, val.deepCopy());
                }
                alreadyMerged.putIfAbsent(key, parent.key);
            }
        }

        styleStack.remove(styleKey);
    }

    private static void inlineDefaultInheritance(Map<ResourceKey<CaveStyle>, JsonObject> styleJsons, JsonObject styleJson) {
        if (styleJson.has("__builtin_no_default_inherit")) {
            return;
        }

        JsonObject defaultStyle = styleJsons.get(DEFAULT);
        if (defaultStyle == null) {
            throw new IllegalStateException("Missing default cave style");
        }

        for (var entry : defaultStyle.entrySet()) {
            String key = entry.getKey();
            if (!styleJson.has(key)) {
                styleJson.add(key, entry.getValue().deepCopy());
            }
        }
    }

    public Optional<Component> displayName() {
        return meta.displayName;
    }

    public List<Component> lore() {
        return meta.lore;
    }

    public boolean isAbstract() {
        return meta.isAbstract;
    }

    public long lifetime() {
        return meta.lifetime;
    }

    public BlockStateProvider getAirBlock(int y, Centroid currentCentroid, int minRoomY, int maxRoomY) {
        double yInCentroid = (double) (y - Mth.floor(currentCentroid.pos.y) + currentCentroid.size) / (currentCentroid.size + currentCentroid.size);
        for (String tag : currentCentroid.tags) {
            BlockTypeRange<Double> range = block.tagAirBlocks.get(tag);
            if (range != null) {
                BlockStateProvider block = range.get(yInCentroid);
                if (block != null) {
                    return block;
                }
            }
        }

        double yInRoom = (double) (y - minRoomY) / (maxRoomY - minRoomY);
        for (String tag : currentCentroid.tags) {
            BlockTypeRange<Double> range = block.roomAirBlocks.get(tag);
            if (range != null) {
                BlockStateProvider block = range.get(yInRoom);
                if (block != null) {
                    return block;
                }
            }
        }

        BlockStateProvider block = this.block.airBlock.get(y);
        return block == null ? BlockStateProvider.simple(Blocks.AIR) : block;
    }

    public BlockState baseBlock() {
        return block.baseBlock;
    }

    public boolean isTransparentBlock(WorldGenLevel level, BlockPos pos) {
        return block.transparentBlocks.test(level, pos);
    }

    public HolderSet<Item> cannotPlace() {
        return block.cannotPlace;
    }

    public Map<String, Ore> ores() {
        return spawning.ores;
    }

    public Map<String, MobSpawnEntry> spawnEntries() {
        return spawning.spawnEntries;
    }

    public float naturalPollutionIncrease() {
        return spawning.naturalPollutionIncrease;
    }

    public int spawnAttemptsPerTick() {
        return spawning.spawnAttemptsPerTick;
    }

    public int sprintingPenalty() {
        return spawning.sprintingPenalty;
    }

    public int blockPlacePollution() {
        return spawning.blockPlacePollution;
    }

    public int blockBreakPollution() {
        return spawning.blockBreakPollution;
    }

    public IntProvider length() {
        return gen.length;
    }

    public IntProvider size() {
        return gen.size;
    }

    public int startY() {
        return gen.startY;
    }

    public boolean randomRotation() {
        return gen.randomRotation;
    }

    public IntProvider centroidVaryHorizontal() {
        return gen.centroidVaryHorizontal;
    }

    public IntProvider centroidVaryVertical() {
        return gen.centroidVaryVertical;
    }

    public Holder<Biome> getBiome(RegistryAccess registryAccess) {
        return gen.biome.orElseGet(() -> registryAccess.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.OCEAN));
    }

    public boolean nether() {
        return gen.nether;
    }

    public Map<Character, Room> rooms() {
        return gen.rooms;
    }

    public GrammarGraph grammar() {
        return gen.grammar.orElseThrow(() -> meta.isAbstract ? new IllegalStateException("Trying to get the grammar graph of an abstract cave") : new IllegalStateException("Cave has no grammar graph"));
    }

    public Optional<Character> continuationSymbol() {
        return gen.continuationSymbol;
    }

    public boolean truncateCaves() {
        return gen.truncateCaves;
    }

    public List<PainterStep> painterSteps() {
        return gen.painterSteps;
    }

    public HolderSet<PlacedFeature> structures() {
        return gen.structures;
    }

    public HolderSet<PlacedFeature> portals() {
        return gen.portals;
    }

    private record MetaProperties(
        Optional<Component> displayName,
        List<Component> lore,
        boolean isAbstract,
        long lifetime
    ) {
        static final MapCodec<MetaProperties> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ComponentSerialization.CODEC.optionalFieldOf("display_name").forGetter(MetaProperties::displayName),
            ComponentSerialization.CODEC.listOf().optionalFieldOf("lore", List.of()).forGetter(MetaProperties::lore),
            Codec.BOOL.optionalFieldOf("abstract", false).forGetter(MetaProperties::isAbstract),
            Codec.LONG.xmap(l -> l * (60 * 20), l -> l / (60 * 20)).optionalFieldOf("lifetime", 120L).forGetter(MetaProperties::lifetime)
        ).apply(instance, MetaProperties::new));
    }

    private record BlockProperties(
        BlockTypeRange<Integer> airBlock,
        Map<String, BlockTypeRange<Double>> roomAirBlocks,
        Map<String, BlockTypeRange<Double>> tagAirBlocks,
        BlockState baseBlock,
        BlockPredicate transparentBlocks,
        HolderSet<Item> cannotPlace
    ) {
        private static final Codec<Map<String, BlockTypeRange<Double>>> AIR_BLOCKS_BY_KEY_CODEC = Codec.unboundedMap(Codec.STRING, BlockTypeRange.DOUBLE_CODEC).xmap(
            combinedMap -> combinedMap.entrySet().stream().flatMap(entry -> Arrays.stream(entry.getKey().split(" ")).map(key -> Map.entry(key, entry.getValue()))).collect(Util.toMap()),
            uncombinedMap -> uncombinedMap.entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getValue))
                .entrySet().stream()
                .map(entry -> Map.entry(entry.getValue().stream().map(Map.Entry::getKey).collect(Collectors.joining(" ")), entry.getKey()))
                .collect(Util.toMap())
        );
        static final MapCodec<BlockProperties> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockTypeRange.INT_CODEC.optionalFieldOf("air_block", BlockTypeRange.simpleInt(BlockStateProvider.simple(Blocks.AIR))).forGetter(BlockProperties::airBlock),
            AIR_BLOCKS_BY_KEY_CODEC.optionalFieldOf("room_air_blocks", Map.of()).forGetter(BlockProperties::roomAirBlocks),
            AIR_BLOCKS_BY_KEY_CODEC.optionalFieldOf("tag_air_blocks", Map.of()).forGetter(BlockProperties::tagAirBlocks),
            DIDCodecs.BLOCK_STATE.optionalFieldOf("base_block", Blocks.STONE.defaultBlockState()).forGetter(BlockProperties::baseBlock),
            DIDCodecs.BLOCK_PREDICATE.optionalFieldOf("transparent_blocks", BlockPredicate.not(BlockPredicate.alwaysTrue())).forGetter(BlockProperties::transparentBlocks),
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("cannot_place", HolderSet.empty()).forGetter(BlockProperties::cannotPlace)
        ).apply(instance, BlockProperties::new));
    }

    private record SpawningProperties(
        Map<String, Ore> ores,
        Map<String, MobSpawnEntry> spawnEntries,
        float naturalPollutionIncrease,
        int spawnAttemptsPerTick,
        int sprintingPenalty,
        int blockPlacePollution,
        int blockBreakPollution
    ) {
        static final MapCodec<SpawningProperties> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Ore.CODEC).optionalFieldOf("ores", Map.of()).forGetter(SpawningProperties::ores),
            Codec.unboundedMap(Codec.STRING, MobSpawnEntry.CODEC).optionalFieldOf("spawn_entries", Map.of()).forGetter(SpawningProperties::spawnEntries),
            Codec.FLOAT.optionalFieldOf("natural_pollution_increase", 0.1f).forGetter(SpawningProperties::naturalPollutionIncrease),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("spawn_attempts_per_tick", 10).forGetter(SpawningProperties::spawnAttemptsPerTick),
            Codec.INT.optionalFieldOf("sprinting_penalty", 5).forGetter(SpawningProperties::sprintingPenalty),
            Codec.INT.optionalFieldOf("block_place_pollution", 10).forGetter(SpawningProperties::blockPlacePollution),
            Codec.INT.optionalFieldOf("block_break_pollution", 10).forGetter(SpawningProperties::blockBreakPollution)
        ).apply(instance, SpawningProperties::new));
    }

    private record GenProperties(
        IntProvider length,
        IntProvider size,
        int startY,
        boolean randomRotation,
        IntProvider centroidVaryHorizontal,
        IntProvider centroidVaryVertical,
        Optional<Holder<Biome>> biome,
        boolean nether,
        Map<Character, Room> rooms,
        Optional<GrammarGraph> grammar,
        Optional<Character> continuationSymbol,
        boolean truncateCaves,
        List<PainterStep> painterSteps,
        HolderSet<PlacedFeature> structures,
        HolderSet<PlacedFeature> portals
    ) {
        static final MapCodec<GenProperties> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("length", ConstantInt.of(90)).forGetter(GenProperties::length),
            DIDCodecs.POSITIVE_INT_PROVIDER.optionalFieldOf("size", UniformInt.of(7, 11)).forGetter(GenProperties::size),
            ExtraCodecs.intRange(DIDConstants.MIN_Y, DIDConstants.MAX_Y).optionalFieldOf("start_y", DIDConstants.DEFAULT_START_Y).forGetter(GenProperties::startY),
            Codec.BOOL.optionalFieldOf("random_rotation", true).forGetter(GenProperties::randomRotation),
            DIDCodecs.INT_PROVIDER.optionalFieldOf("centroid_vary_horizontal", UniformInt.of(-1, 1)).forGetter(GenProperties::centroidVaryHorizontal),
            DIDCodecs.INT_PROVIDER.optionalFieldOf("centroid_vary_vertical", ConstantInt.of(-1)).forGetter(GenProperties::centroidVaryVertical),
            Biome.CODEC.optionalFieldOf("biome").forGetter(GenProperties::biome),
            Codec.BOOL.optionalFieldOf("nether", false).forGetter(GenProperties::nether),
            Codec.unboundedMap(DIDCodecs.CHAR, Room.CODEC).optionalFieldOf("rooms", Map.of()).forGetter(GenProperties::rooms),
            GrammarGraph.CODEC.optionalFieldOf("grammar").forGetter(GenProperties::grammar),
            Codec.either(DIDCodecs.CHAR, ExtraCodecs.intRange(0, 0)).<Optional<Character>>xmap(
                either -> either.map(Optional::of, x -> Optional.empty()),
                opt -> opt.<Either<Character, Integer>>map(Either::left).orElseGet(() -> Either.right(0))
            ).optionalFieldOf("continuation_symbol", Optional.of('Y')).forGetter(GenProperties::continuationSymbol),
            Codec.BOOL.optionalFieldOf("truncate_caves", true).forGetter(GenProperties::truncateCaves),
            PainterStep.CODEC.listOf().optionalFieldOf("painter_steps", List.of()).forGetter(GenProperties::painterSteps),
            PlacedFeature.LIST_CODEC.optionalFieldOf("structures", HolderSet.empty()).forGetter(GenProperties::structures),
            PlacedFeature.LIST_CODEC.optionalFieldOf("portals", HolderSet.empty()).forGetter(GenProperties::portals)
        ).apply(instance, GenProperties::new));
    }
}
