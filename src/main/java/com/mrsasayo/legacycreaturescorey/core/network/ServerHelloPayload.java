package com.mrsasayo.legacycreaturescorey.core.network;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Payload enviado al cliente al conectar para indicar el protocolo de red del servidor.
 */
public record ServerHelloPayload(int protocolVersion) implements CustomPacketPayload {

    public static final Id<ServerHelloPayload> ID = new Id<>(Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "server_hello"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerHelloPayload> CODEC = new StreamCodec<>() {
        @Override
        public ServerHelloPayload decode(RegistryFriendlyByteBuf buf) {
            return new ServerHelloPayload(buf.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ServerHelloPayload value) {
            buf.writeVarInt(value.protocolVersion());
        }
    };

    @Override
    public Id<ServerHelloPayload> getId() {
        return ID;
    }
}
