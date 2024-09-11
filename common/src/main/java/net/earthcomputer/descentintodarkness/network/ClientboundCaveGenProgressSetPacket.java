package net.earthcomputer.descentintodarkness.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundCaveGenProgressSetPacket(
    float progress
) implements CustomPacketPayload {
    public static final StreamCodec<ByteBuf, ClientboundCaveGenProgressSetPacket> CODEC = ByteBufCodecs.FLOAT
        .map(ClientboundCaveGenProgressSetPacket::new, ClientboundCaveGenProgressSetPacket::progress);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return DIDNetwork.CAVE_GEN_PROGRESS_SET;
    }
}
