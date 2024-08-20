package net.earthcomputer.descentintodarkness.generator.structure;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.registries.RegistrySupplier;
import net.earthcomputer.descentintodarkness.DIDRegistries;
import net.earthcomputer.descentintodarkness.DescentIntoDarkness;

@FunctionalInterface
public interface StructureType<S extends Structure> {
    RegistrySupplier<StructureType<TemplateStructure>> TEMPLATE = register("template", TemplateStructure.CODEC);
    RegistrySupplier<StructureType<VeinStructure>> VEIN = register("vein", VeinStructure.CODEC);
    RegistrySupplier<StructureType<PatchStructure>> PATCH = register("patch", PatchStructure.CODEC);
    RegistrySupplier<StructureType<VinePatchStructure>> VINE_PATCH = register("vine_patch", VinePatchStructure.CODEC);
    RegistrySupplier<StructureType<GlowstoneStructure>> GLOWSTONE = register("glowstone", GlowstoneStructure.CODEC);
    RegistrySupplier<StructureType<WaterfallStructure>> WATERFALL = register("waterfall", WaterfallStructure.CODEC);
    RegistrySupplier<StructureType<TreeStructure>> TREE = register("tree", TreeStructure.CODEC);
    RegistrySupplier<StructureType<StalagmiteStructure>> STALAGMITE = register("stalagmite", StalagmiteStructure.CODEC);
    RegistrySupplier<StructureType<ChorusPlantStructure>> CHORUS_PLANT = register("chorus_plant", ChorusPlantStructure.CODEC);
    RegistrySupplier<StructureType<WallPortalStructure>> WALL_PORTAL = register("wall_portal", WallPortalStructure.CODEC);
    RegistrySupplier<StructureType<FloorPortalStructure>> FLOOR_PORTAL = register("floor_portal", FloorPortalStructure.CODEC);

    MapCodec<S> codec();

    static void register() {
        // load class
    }

    static <S extends Structure> RegistrySupplier<StructureType<S>> register(String id, MapCodec<S> codec) {
        return DIDRegistries.REGISTRAR_MANAGER.get(DIDRegistries.STRUCTURE_TYPE).register(DescentIntoDarkness.id(id), () -> () -> codec);
    }
}
