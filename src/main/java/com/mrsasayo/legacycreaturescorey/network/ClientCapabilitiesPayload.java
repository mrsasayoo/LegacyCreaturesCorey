package com.mrsasayo.legacycreaturescorey.network;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.EnumSet;

/**
 * Payload enviado por el cliente para anunciar si soporta HUD/efectos y su versión de protocolo.
 */
public record ClientCapabilitiesPayload(String modVersion,
                                        int protocolVersion,
                                        EnumSet<ClientFeature> features) implements CustomPayload {

    public static final Id<ClientCapabilitiesPayload> ID = new Id<>(Identifier.of(Legacycreaturescorey.MOD_ID, "client_capabilities"));

    public static final PacketCodec<RegistryByteBuf, ClientCapabilitiesPayload> CODEC = new PacketCodec<>() {
        @Override
        public ClientCapabilitiesPayload decode(RegistryByteBuf buf) {
            String version = buf.readString(32);
            int protocol = buf.readVarInt();
            int flags = buf.readVarInt();
            EnumSet<ClientFeature> features = decodeFeatures(flags);
            return new ClientCapabilitiesPayload(version, protocol, features);
        }

        @Override
        public void encode(RegistryByteBuf buf, ClientCapabilitiesPayload value) {
            buf.writeString(value.modVersion(), 32);
            buf.writeVarInt(value.protocolVersion());
            buf.writeVarInt(encodeFeatures(value.features()));
        }
    };

    @Override
    public Id<ClientCapabilitiesPayload> getId() {
        return ID;
    }

    private static int encodeFeatures(EnumSet<ClientFeature> features) {
        int flags = 0;
        for (ClientFeature feature : features) {
            flags |= 1 << feature.ordinal();
        }
        return flags;
    }

    private static EnumSet<ClientFeature> decodeFeatures(int flags) {
        EnumSet<ClientFeature> features = EnumSet.noneOf(ClientFeature.class);
        for (ClientFeature feature : ClientFeature.values()) {
            if ((flags & (1 << feature.ordinal())) != 0) {
                features.add(feature);
            }
        }
        return features;
    }
}
