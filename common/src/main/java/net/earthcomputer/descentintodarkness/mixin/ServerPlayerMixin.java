package net.earthcomputer.descentintodarkness.mixin;

import net.earthcomputer.descentintodarkness.generator.PlayerProgressListener;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin implements PlayerProgressListener.PlayerProgressListenerAddon {
    @Unique
    @Nullable
    private PlayerProgressListener did_playerProgressListener;

    @Override
    @Nullable
    public PlayerProgressListener did_swapPlayerProgressListener(PlayerProgressListener listener) {
        PlayerProgressListener old = this.did_playerProgressListener;
        this.did_playerProgressListener = listener;
        return old;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (did_playerProgressListener != null) {
            did_playerProgressListener.tick();
        }
    }
}
