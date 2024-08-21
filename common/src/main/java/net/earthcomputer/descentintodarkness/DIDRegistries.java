package net.earthcomputer.descentintodarkness;

import dev.architectury.registry.registries.RegistrarManager;
import net.earthcomputer.descentintodarkness.blockpredicate.DIDBlockPredicateTypes;
import net.earthcomputer.descentintodarkness.generator.painter.PainterStepType;
import net.earthcomputer.descentintodarkness.generator.room.RoomType;
import net.earthcomputer.descentintodarkness.generator.structure.StructureType;
import net.earthcomputer.descentintodarkness.style.CaveStyle;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;

public final class DIDRegistries {
    private DIDRegistries() {
    }

    public static final RegistrarManager REGISTRAR_MANAGER = RegistrarManager.get(DescentIntoDarkness.MOD_ID);

    public static final ResourceKey<Registry<CaveStyle>> CAVE_STYLE = ResourceKey.createRegistryKey(DescentIntoDarkness.id("cave_style"));
    public static final ResourceKey<Registry<RoomType<?>>> ROOM_TYPE = ResourceKey.createRegistryKey(DescentIntoDarkness.id("room_type"));
    public static final ResourceKey<Registry<PainterStepType<?>>> PAINTER_STEP_TYPE = ResourceKey.createRegistryKey(DescentIntoDarkness.id("painter_step_type"));
    public static final ResourceKey<Registry<StructureType<?>>> STRUCTURE_TYPE = ResourceKey.createRegistryKey(DescentIntoDarkness.id("structure_type"));

    public static Registry<RoomType<?>> roomType() {
        return getBuiltInRegistry(ROOM_TYPE);
    }

    public static Registry<PainterStepType<?>> painterStepType() {
        return getBuiltInRegistry(PAINTER_STEP_TYPE);
    }

    public static Registry<StructureType<?>> structureType() {
        return getBuiltInRegistry(STRUCTURE_TYPE);
    }

    @SuppressWarnings("unchecked")
    private static <T> Registry<T> getBuiltInRegistry(ResourceKey<Registry<T>> key) {
        return ((Registry<Registry<T>>) BuiltInRegistries.REGISTRY).getOrThrow(key);
    }

    public static void register() {
        DIDPlatform.registerDynamicRegistry(CAVE_STYLE, CaveStyle.CODEC, CaveStyle.NETWORK_CODEC);

        REGISTRAR_MANAGER.<RoomType<?>>builder(ROOM_TYPE.location()).build();
        RoomType.register();

        REGISTRAR_MANAGER.<PainterStepType<?>>builder(PAINTER_STEP_TYPE.location()).build();
        PainterStepType.register();

        REGISTRAR_MANAGER.<StructureType<?>>builder(STRUCTURE_TYPE.location()).build();
        StructureType.register();

        DIDBlockPredicateTypes.register();
    }
}
