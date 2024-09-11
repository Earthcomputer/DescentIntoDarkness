package net.earthcomputer.descentintodarkness.generator;

import dev.architectury.networking.NetworkManager;
import net.earthcomputer.descentintodarkness.network.ClientboundCaveGenProgressClearPacket;
import net.earthcomputer.descentintodarkness.network.ClientboundCaveGenProgressPopPacket;
import net.earthcomputer.descentintodarkness.network.ClientboundCaveGenProgressPushPacket;
import net.earthcomputer.descentintodarkness.network.ClientboundCaveGenProgressSetPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class PlayerProgressListener implements CaveGenProgressListener {
    private final List<CustomPacketPayload> packetsToSend = new ArrayList<>();
    private final ServerPlayer player;
    private boolean active = true;

    public PlayerProgressListener(ServerPlayer player) {
        this.player = player;
        PlayerProgressListener oldListener = ((PlayerProgressListenerAddon) player).did_swapPlayerProgressListener(this);
        if (oldListener != null) {
            oldListener.finish();
        }
    }

    @Override
    public void pushProgress(Component name) {
        if (active) {
            synchronized (packetsToSend) {
                packetsToSend.add(new ClientboundCaveGenProgressPushPacket(name));
            }
        }
    }

    @Override
    public void popProgress() {
        if (active) {
            synchronized (packetsToSend) {
                packetsToSend.add(ClientboundCaveGenProgressPopPacket.INSTANCE);
            }
        }
    }

    @Override
    public void setProgress(float progress) {
        if (active) {
            synchronized (packetsToSend) {
                if (!packetsToSend.isEmpty() && packetsToSend.getLast() instanceof ClientboundCaveGenProgressSetPacket) {
                    packetsToSend.set(packetsToSend.size() - 1, new ClientboundCaveGenProgressSetPacket(progress));
                } else {
                    packetsToSend.add(new ClientboundCaveGenProgressSetPacket(progress));
                }
            }
        }
    }

    @Override
    public void finish() {
        NetworkManager.sendToPlayer(player, ClientboundCaveGenProgressClearPacket.INSTANCE);
        active = false;
    }

    public void tick() {
        if (active) {
            synchronized (packetsToSend) {
                for (CustomPacketPayload packet : packetsToSend) {
                    NetworkManager.sendToPlayer(player, packet);
                }
                packetsToSend.clear();
            }
        }
    }

    public interface PlayerProgressListenerAddon {
        @Nullable
        PlayerProgressListener did_swapPlayerProgressListener(PlayerProgressListener listener);
    }
}
