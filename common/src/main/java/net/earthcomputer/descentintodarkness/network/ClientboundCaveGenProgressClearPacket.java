package net.earthcomputer.descentintodarkness.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class ClientboundCaveGenProgressClearPacket implements CustomPacketPayload {
    public static final ClientboundCaveGenProgressClearPacket INSTANCE = new ClientboundCaveGenProgressClearPacket();
    public static final StreamCodec<ByteBuf, ClientboundCaveGenProgressClearPacket> CODEC = StreamCodec.unit(INSTANCE);

    private ClientboundCaveGenProgressClearPacket() {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return DIDNetwork.CAVE_GEN_PROGRESS_CLEAR;
    }
}
