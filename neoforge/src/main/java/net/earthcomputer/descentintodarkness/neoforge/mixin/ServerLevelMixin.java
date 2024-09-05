package net.earthcomputer.descentintodarkness.neoforge.mixin;

import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level {
    @Shadow
    @NotNull
    public abstract MinecraftServer getServer();

    @Shadow
    @Final
    private static Logger LOGGER;

    protected ServerLevelMixin(WritableLevelData arg, ResourceKey<Level> arg2, RegistryAccess arg3, Holder<DimensionType> arg4, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l, int i) {
        super(arg, arg2, arg3, arg4, supplier, bl, bl2, l, i);
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void onClose(CallbackInfo ci) {
        if (dimension().location().getNamespace().equals(DescentIntoDarkness.MOD_ID)) {
            Path dimensionPath = getServer().storageSource.getDimensionPath(dimension());
            if (Files.exists(dimensionPath)) {
                try {
                    FileUtils.deleteDirectory(dimensionPath.toFile());
                } catch (IOException e) {
                    LOGGER.error("Failed to delete dimension folder", e);
                }
            }
        }
    }
}
