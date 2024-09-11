package net.earthcomputer.descentintodarkness.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundCaveGenProgressPushPacket(
    Component name
) implements CustomPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundCaveGenProgressPushPacket> CODEC = ComponentSerialization.TRUSTED_STREAM_CODEC
        .map(ClientboundCaveGenProgressPushPacket::new, ClientboundCaveGenProgressPushPacket::name);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return DIDNetwork.CAVE_GEN_PROGRESS_PUSH;
    }
}
