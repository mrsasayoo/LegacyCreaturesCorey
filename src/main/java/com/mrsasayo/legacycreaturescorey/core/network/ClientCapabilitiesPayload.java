package com.mrsasayo.legacycreaturescorey.core.network;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.EnumSet;

/**
 * Payload enviado por el cliente para anunciar si soporta HUD/efectos y su versión de protocolo.
 */
public record ClientCapabilitiesPayload(String modVersion,
                                        int protocolVersion,
                                        EnumSet<ClientFeature> features) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientCapabilitiesPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "client_capabilities"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientCapabilitiesPayload> CODEC = new StreamCodec<>() {
        @Override
        public ClientCapabilitiesPayload decode(RegistryFriendlyByteBuf buf) {
            String version = buf.readUtf(32);
            int protocol = buf.readVarInt();
            int flags = buf.readVarInt();
            EnumSet<ClientFeature> features = decodeFeatures(flags);
            return new ClientCapabilitiesPayload(version, protocol, features);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ClientCapabilitiesPayload value) {
            buf.writeUtf(value.modVersion(), 32);
            buf.writeVarInt(value.protocolVersion());
            buf.writeVarInt(encodeFeatures(value.features()));
        }
    };

    @Override
    public CustomPacketPayload.Type<ClientCapabilitiesPayload> type() {
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
