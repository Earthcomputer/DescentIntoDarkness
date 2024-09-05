package net.earthcomputer.descentintodarkness.neoforge.mixin;

import com.google.common.collect.Maps;
import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WorldGenSettings.class)
public class WorldGenSettingsMixin {
    @ModifyVariable(method = "encode(Lcom/mojang/serialization/DynamicOps;Lnet/minecraft/world/level/levelgen/WorldOptions;Lnet/minecraft/world/level/levelgen/WorldDimensions;)Lcom/mojang/serialization/DataResult;", at = @At("HEAD"), argsOnly = true)
    private static WorldDimensions modifyDimensions(WorldDimensions dimensions) {
        return new WorldDimensions(Maps.filterKeys(dimensions.dimensions(), key -> !key.location().getNamespace().equals(DescentIntoDarkness.MOD_ID)));
    }
}
