package net.earthcomputer.descentintodarkness.resources;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.earthcomputer.descentintodarkness.DIDRegistries;
import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.earthcomputer.descentintodarkness.style.DIDCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public final class DIDBlocks {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Entry> entries = new LinkedHashMap<>();

    private DIDBlocks() {
    }

    public static void reload() {
        Map<String, Entry> newEntries;
        try {
            newEntries = DIDResourceLoader.loadResources("blocks", Entry.CODEC);
        } catch (Exception e) {
            LOGGER.error("Couldn't load blocks", e);
            return;
        }

        DIDResourceLoader.checkRequiresRestart(entries, newEntries);
        entries.clear();
        entries.putAll(newEntries);
    }

    public static void forEach(BiConsumer<String, Entry> func) {
        entries.forEach(func);
    }

    public static void register() {
        forEach((id, entry) -> {
            BlockBehaviour.Properties[] properties = { BlockBehaviour.Properties.of() };
            entry.inheritProperties.ifPresent(inheritProperties -> {
                if (DescentIntoDarkness.MOD_ID.equals(inheritProperties.getNamespace())) {
                    LOGGER.error("Block {} tried to inherit properties from DID block {} which is not allowed", id, inheritProperties);
                } else {
                    BuiltInRegistries.BLOCK.getOptional(inheritProperties).ifPresentOrElse(block -> {
                        if (block.defaultBlockState().isAir()) {
                            LOGGER.error("Block {} tried to inherit properties from air", id);
                        } else {
                            properties[0] = BlockBehaviour.Properties.ofFullCopy(block);
                        }
                    }, () -> LOGGER.error("Block {} tried to inherit properties from non-existent block {}", id, inheritProperties));
                }
            });
            entry.collision.ifPresent(collision -> {
                if (collision) {
                    LOGGER.error("Block {} tried to explicitly enable collision", id);
                } else {
                    properties[0].noCollission();
                }
            });
            entry.occlusion.ifPresent(occlusion -> {
                if (occlusion) {
                    LOGGER.error("Block {} tried to explicitly enable occlusion", id);
                } else {
                    properties[0].noOcclusion();
                }
            });
            entry.friction.ifPresent(friction -> properties[0].friction(friction));
            entry.speedFactor.ifPresent(speedFactor -> properties[0].speedFactor(speedFactor));
            entry.jumpFactor.ifPresent(jumpFactor -> properties[0].jumpFactor(jumpFactor));
            entry.soundType.flatMap(soundType -> BlockSoundType.toSoundType(id, soundType)).ifPresent(soundType -> properties[0].sound(soundType));
            entry.lightLevel.ifPresent(lightLevel -> properties[0].lightLevel(state -> lightLevel));
            entry.destroyTime.ifPresent(destroyTime -> properties[0].destroyTime(destroyTime));
            entry.explosionResistance.ifPresent(explosionResistance -> properties[0].explosionResistance(explosionResistance));
            VoxelShape collisionShape = entry.collisionShape.orElse(entry.shape);
            VoxelShape outlineShape = entry.outlineShape.orElse(entry.shape);
            DIDRegistries.REGISTRAR_MANAGER.get(Registries.BLOCK).register(DescentIntoDarkness.id(id), () -> new DIDBlock(properties[0], entry.shape, collisionShape, outlineShape));
        });
    }

    public static boolean itemHasBlock(String id) {
        Entry entry = entries.get(id);
        if (entry == null) {
            return false;
        }
        return entry.hasItem;
    }

    public record Entry(
        boolean hasItem,
        Optional<ResourceLocation> inheritProperties,
        Optional<Boolean> collision,
        Optional<Boolean> occlusion,
        Optional<Float> friction,
        Optional<Float> speedFactor,
        Optional<Float> jumpFactor,
        Optional<Either<ResourceLocation, BlockSoundType>> soundType,
        Optional<Integer> lightLevel,
        Optional<Float> destroyTime,
        Optional<Float> explosionResistance,
        BlockRenderType renderType,
        VoxelShape shape,
        Optional<VoxelShape> collisionShape,
        Optional<VoxelShape> outlineShape
    ) implements DIDResourceLoader.RestartRequiringEntry<Entry> {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("has_item", false).forGetter(Entry::hasItem),
            ResourceLocation.CODEC.optionalFieldOf("inherit_properties").forGetter(Entry::inheritProperties),
            Codec.BOOL.optionalFieldOf("collision").forGetter(Entry::collision),
            Codec.BOOL.optionalFieldOf("occlusion").forGetter(Entry::occlusion),
            Codec.FLOAT.optionalFieldOf("friction").forGetter(Entry::friction),
            Codec.FLOAT.optionalFieldOf("speed_factor").forGetter(Entry::speedFactor),
            Codec.FLOAT.optionalFieldOf("jump_factor").forGetter(Entry::jumpFactor),
            BlockSoundType.CODEC.optionalFieldOf("sound_type").forGetter(Entry::soundType),
            ExtraCodecs.intRange(0, 15).optionalFieldOf("light_level").forGetter(Entry::lightLevel),
            DIDCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("destroy_time").forGetter(Entry::destroyTime),
            DIDCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("explosion_resistance").forGetter(Entry::explosionResistance),
            BlockRenderType.CODEC.optionalFieldOf("render_type", BlockRenderType.SOLID).forGetter(Entry::renderType),
            DIDCodecs.VOXEL_SHAPE.optionalFieldOf("shape", Shapes.block()).forGetter(Entry::shape),
            DIDCodecs.VOXEL_SHAPE.optionalFieldOf("collision_shape").forGetter(Entry::collisionShape),
            DIDCodecs.VOXEL_SHAPE.optionalFieldOf("outline_shape").forGetter(Entry::outlineShape)
        ).apply(instance, Entry::new));
    }

    public enum BlockRenderType implements StringRepresentable {
        SOLID("solid"),
        CUTOUT("cutout"),
        TRANSLUCENT("translucent"),
        ;

        static final Codec<BlockRenderType> CODEC = StringRepresentable.fromEnum(BlockRenderType::values);

        private final String name;

        BlockRenderType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private record BlockSoundType(
        float volume,
        float pitch,
        ResourceLocation breakSound,
        ResourceLocation stepSound,
        ResourceLocation placeSound,
        ResourceLocation hitSound,
        ResourceLocation fallSound
    ) {
        private static final Codec<BlockSoundType> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("volume", 1f).forGetter(BlockSoundType::volume),
            Codec.FLOAT.optionalFieldOf("pitch", 1f).forGetter(BlockSoundType::pitch),
            ResourceLocation.CODEC.fieldOf("break_sound").forGetter(BlockSoundType::breakSound),
            ResourceLocation.CODEC.fieldOf("step_sound").forGetter(BlockSoundType::stepSound),
            ResourceLocation.CODEC.fieldOf("place_sound").forGetter(BlockSoundType::placeSound),
            ResourceLocation.CODEC.fieldOf("hit_sound").forGetter(BlockSoundType::hitSound),
            ResourceLocation.CODEC.fieldOf("fall_sound").forGetter(BlockSoundType::fallSound)
        ).apply(instance, BlockSoundType::new));

        static final Codec<Either<ResourceLocation, BlockSoundType>> CODEC = Codec.either(ResourceLocation.CODEC, DIRECT_CODEC);

        static Optional<SoundType> toSoundType(String thisBlock, Either<ResourceLocation, BlockSoundType> soundType) {
            return soundType.map(
                blockId -> {
                    if (DescentIntoDarkness.MOD_ID.equals(blockId.getNamespace())) {
                        LOGGER.error("Block {} tried to inherit sound type from DID block {}", thisBlock, blockId);
                        return Optional.empty();
                    }
                    Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(blockId);
                    if (block.isPresent()) {
                        return Optional.of(block.get().defaultBlockState().getSoundType());
                    } else {
                        LOGGER.error("Block {} tried to inherit sound type from non-existent block {}", thisBlock, blockId);
                        return Optional.empty();
                    }
                },
                blockSoundType -> {
                    Optional<SoundEvent> breakSound = BuiltInRegistries.SOUND_EVENT.getOptional(blockSoundType.breakSound);
                    if (breakSound.isEmpty()) {
                        LOGGER.error("Block {} specified non-existent break sound {}", thisBlock, blockSoundType.breakSound);
                        return Optional.empty();
                    }
                    Optional<SoundEvent> stepSound = BuiltInRegistries.SOUND_EVENT.getOptional(blockSoundType.stepSound);
                    if (stepSound.isEmpty()) {
                        LOGGER.error("Block {} specified non-existent step sound {}", thisBlock, blockSoundType.stepSound);
                        return Optional.empty();
                    }
                    Optional<SoundEvent> placeSound = BuiltInRegistries.SOUND_EVENT.getOptional(blockSoundType.placeSound);
                    if (placeSound.isEmpty()) {
                        LOGGER.error("Block {} specified non-existent place sound {}", thisBlock, blockSoundType.placeSound);
                        return Optional.empty();
                    }
                    Optional<SoundEvent> hitSound = BuiltInRegistries.SOUND_EVENT.getOptional(blockSoundType.hitSound);
                    if (hitSound.isEmpty()) {
                        LOGGER.error("Block {} specified non-existent hit sound {}", thisBlock, blockSoundType.hitSound);
                        return Optional.empty();
                    }
                    Optional<SoundEvent> fallSound = BuiltInRegistries.SOUND_EVENT.getOptional(blockSoundType.fallSound);
                    if (fallSound.isEmpty()) {
                        LOGGER.error("Block {} specified non-existent fall sound {}", thisBlock, blockSoundType.fallSound);
                        return Optional.empty();
                    }
                    return Optional.of(new SoundType(blockSoundType.volume, blockSoundType.pitch, breakSound.get(), stepSound.get(), placeSound.get(), hitSound.get(), fallSound.get()));
                }
            );
        }
    }
}
