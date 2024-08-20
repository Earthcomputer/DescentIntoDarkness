package net.earthcomputer.descentintodarkness.neoforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RegistryDataLoader.class)
public class RegistryDataLoaderMixin {
    @WrapOperation(method = {"loadContentsFromManager", "loadContentsFromNetwork"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/core/registries/Registries;elementsDirPath(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/String;"))
    private static String addDIDDirectory(ResourceKey<? extends Registry<?>> resourceKey, Operation<String> original) {
        String dir = original.call(resourceKey);
        if (resourceKey.location().getNamespace().equals(DescentIntoDarkness.MOD_ID)) {
            return DescentIntoDarkness.MOD_ID + "/" + dir;
        } else {
            return dir;
        }
    }
}
