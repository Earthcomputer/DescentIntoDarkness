package net.earthcomputer.descentintodarkness.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class ClientboundCaveGenProgressPopPacket implements CustomPacketPayload {
    public static final ClientboundCaveGenProgressPopPacket INSTANCE = new ClientboundCaveGenProgressPopPacket();
    public static final StreamCodec<ByteBuf, ClientboundCaveGenProgressPopPacket> CODEC = StreamCodec.unit(INSTANCE);

    private ClientboundCaveGenProgressPopPacket() {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return DIDNetwork.CAVE_GEN_PROGRESS_POP;
    }
}
