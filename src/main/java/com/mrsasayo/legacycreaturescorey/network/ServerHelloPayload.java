package com.mrsasayo.legacycreaturescorey.network;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload enviado al cliente al conectar para indicar el protocolo de red del servidor.
 */
public record ServerHelloPayload(int protocolVersion) implements CustomPayload {

    public static final Id<ServerHelloPayload> ID = new Id<>(Identifier.of(Legacycreaturescorey.MOD_ID, "server_hello"));

    public static final PacketCodec<RegistryByteBuf, ServerHelloPayload> CODEC = new PacketCodec<>() {
        @Override
        public ServerHelloPayload decode(RegistryByteBuf buf) {
            return new ServerHelloPayload(buf.readVarInt());
        }

        @Override
        public void encode(RegistryByteBuf buf, ServerHelloPayload value) {
            buf.writeVarInt(value.protocolVersion());
        }
    };

    @Override
    public Id<ServerHelloPayload> getId() {
        return ID;
    }
}
