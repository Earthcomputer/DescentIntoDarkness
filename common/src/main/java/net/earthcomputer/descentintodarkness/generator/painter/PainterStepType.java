package net.earthcomputer.descentintodarkness.generator.painter;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.registries.RegistrySupplier;
import net.earthcomputer.descentintodarkness.DIDRegistries;
import net.earthcomputer.descentintodarkness.DescentIntoDarkness;

@FunctionalInterface
public interface PainterStepType<PS extends PainterStep> {
    RegistrySupplier<PainterStepType<ReplaceAllStep>> REPLACE_ALL = register("replace_all", ReplaceAllStep.CODEC);
    RegistrySupplier<PainterStepType<ReplaceCeilingStep>> REPLACE_CEILING = register("replace_ceiling", ReplaceCeilingStep.CODEC);
    RegistrySupplier<PainterStepType<ReplaceFloorStep>> REPLACE_FLOOR = register("replace_floor", ReplaceFloorStep.CODEC);
    RegistrySupplier<PainterStepType<CeilingLayerStep>> CEILING_LAYER = register("ceiling_layer", CeilingLayerStep.CODEC);
    RegistrySupplier<PainterStepType<FloorLayerStep>> FLOOR_LAYER = register("floor_layer", FloorLayerStep.CODEC);
    RegistrySupplier<PainterStepType<ReplaceMesaStep>> REPLACE_MESA = register("replace_mesa", ReplaceMesaStep.CODEC);

    MapCodec<PS> codec();

    static void register() {
        // load class
    }

    static <PS extends PainterStep> RegistrySupplier<PainterStepType<PS>> register(String id, MapCodec<PS> codec) {
        return DIDRegistries.REGISTRAR_MANAGER.get(DIDRegistries.PAINTER_STEP_TYPE).register(DescentIntoDarkness.id(id), () -> () -> codec);
    }
}
