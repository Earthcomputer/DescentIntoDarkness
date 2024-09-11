package net.earthcomputer.descentintodarkness.client;

import dev.architectury.networking.NetworkManager;
import net.earthcomputer.descentintodarkness.network.ClientboundCaveGenProgressClearPacket;
import net.earthcomputer.descentintodarkness.network.ClientboundCaveGenProgressPopPacket;
import net.earthcomputer.descentintodarkness.network.ClientboundCaveGenProgressPushPacket;
import net.earthcomputer.descentintodarkness.network.ClientboundCaveGenProgressSetPacket;
import net.earthcomputer.descentintodarkness.network.DIDNetwork;

public final class DIDNetworkClient {
    private DIDNetworkClient() {
    }

    public static void registerClient() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, DIDNetwork.CAVE_GEN_PROGRESS_PUSH, ClientboundCaveGenProgressPushPacket.CODEC, DIDNetworkClient::handleCaveGenProgressPush);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, DIDNetwork.CAVE_GEN_PROGRESS_SET, ClientboundCaveGenProgressSetPacket.CODEC, DIDNetworkClient::handleCaveGenProgressSet);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, DIDNetwork.CAVE_GEN_PROGRESS_POP, ClientboundCaveGenProgressPopPacket.CODEC, DIDNetworkClient::handleCaveGenProgressPop);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, DIDNetwork.CAVE_GEN_PROGRESS_CLEAR, ClientboundCaveGenProgressClearPacket.CODEC, DIDNetworkClient::handleCaveGenProgressClear);
    }

    public static void handleCaveGenProgressPush(ClientboundCaveGenProgressPushPacket packet, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> CaveGenProgress.pushEntry(packet.name()));
    }

    public static void handleCaveGenProgressSet(ClientboundCaveGenProgressSetPacket packet, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> CaveGenProgress.setProgress(packet.progress()));
    }

    public static void handleCaveGenProgressPop(ClientboundCaveGenProgressPopPacket packet, NetworkManager.PacketContext ctx) {
        ctx.queue(CaveGenProgress::popEntry);
    }

    public static void handleCaveGenProgressClear(ClientboundCaveGenProgressClearPacket packet, NetworkManager.PacketContext ctx) {
        ctx.queue(CaveGenProgress::clear);
    }
}
