package net.earthcomputer.descentintodarkness.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.earthcomputer.descentintodarkness.DescentIntoDarkness;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class DIDNetwork {
    public static final CustomPacketPayload.Type<ClientboundCaveGenProgressPushPacket> CAVE_GEN_PROGRESS_PUSH = new CustomPacketPayload.Type<>(DescentIntoDarkness.id("cave_gen_progress_push"));
    public static final CustomPacketPayload.Type<ClientboundCaveGenProgressSetPacket> CAVE_GEN_PROGRESS_SET = new CustomPacketPayload.Type<>(DescentIntoDarkness.id("cave_gen_progress_set"));
    public static final CustomPacketPayload.Type<ClientboundCaveGenProgressPopPacket> CAVE_GEN_PROGRESS_POP = new CustomPacketPayload.Type<>(DescentIntoDarkness.id("cave_gen_progress_pop"));
    public static final CustomPacketPayload.Type<ClientboundCaveGenProgressClearPacket> CAVE_GEN_PROGRESS_CLEAR = new CustomPacketPayload.Type<>(DescentIntoDarkness.id("cave_gen_progress_clear"));

    private DIDNetwork() {
    }

    public static void register() {
        EnvExecutor.runInEnv(Env.SERVER, () -> DIDNetwork::registerServer);
    }

    private static void registerServer() {
        NetworkManager.registerS2CPayloadType(CAVE_GEN_PROGRESS_PUSH, ClientboundCaveGenProgressPushPacket.CODEC);
        NetworkManager.registerS2CPayloadType(CAVE_GEN_PROGRESS_SET, ClientboundCaveGenProgressSetPacket.CODEC);
        NetworkManager.registerS2CPayloadType(CAVE_GEN_PROGRESS_POP, ClientboundCaveGenProgressPopPacket.CODEC);
        NetworkManager.registerS2CPayloadType(CAVE_GEN_PROGRESS_CLEAR, ClientboundCaveGenProgressClearPacket.CODEC);
    }
}
