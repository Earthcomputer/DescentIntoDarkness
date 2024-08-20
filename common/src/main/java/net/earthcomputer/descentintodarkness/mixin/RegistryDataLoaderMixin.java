package net.earthcomputer.descentintodarkness.mixin;

import com.mojang.serialization.Decoder;
import net.earthcomputer.descentintodarkness.DIDRegistries;
import net.earthcomputer.descentintodarkness.style.CaveStyle;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RegistryDataLoader.class)
public class RegistryDataLoaderMixin {
    @Inject(method = "loadContentsFromManager", at = @At("HEAD"), cancellable = true)
    private static void handleCustomCaveStyleLoading(
        ResourceManager resourceManager,
        RegistryOps.RegistryInfoLookup infoLookup,
        WritableRegistry<CaveStyle> registry,
        Decoder<CaveStyle> decoder,
        Map<ResourceKey<?>, Exception> loadingErrors,
        CallbackInfo ci
    ) {
        if (registry.key() == DIDRegistries.CAVE_STYLE) {
            CaveStyle.loadCaveStyles(resourceManager, infoLookup, registry, decoder, loadingErrors);
            ci.cancel();
        }
    }
}
