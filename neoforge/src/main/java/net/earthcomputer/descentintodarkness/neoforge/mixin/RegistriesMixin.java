package net.earthcomputer.descentintodarkness.neoforge.mixin;

import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Registries.class)
public class RegistriesMixin {
    @Inject(method = "tagsDirPath", at = @At("HEAD"), cancellable = true)
    private static void addDIDDirectory(ResourceKey<? extends Registry<?>> key, CallbackInfoReturnable<String> cir) {
        if (key.location().getNamespace().equals(DescentIntoDarkness.MOD_ID)) {
            cir.setReturnValue("tags/" + DescentIntoDarkness.MOD_ID + "/" + key.location().getPath());
        }
    }
}
