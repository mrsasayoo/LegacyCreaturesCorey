package com.mrsasayo.legacycreaturescorey.core.network;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload sent from the server to the client instructing it to play a custom feedback effect.
 */
public record ClientEffectPayload(ClientEffectType effect,
                                  boolean start,
                                  int durationTicks,
                                  float intensity) implements CustomPayload {

    public static final Id<ClientEffectPayload> ID = new Id<>(Identifier.of(Legacycreaturescorey.MOD_ID, "client_effect"));

    public static final PacketCodec<RegistryByteBuf, ClientEffectPayload> CODEC = new PacketCodec<>() {
        @Override
        public ClientEffectPayload decode(RegistryByteBuf buf) {
            ClientEffectType effect = ClientEffectType.read(buf);
            boolean start = buf.readBoolean();
            int duration = buf.readVarInt();
            float intensity = buf.readFloat();
            return new ClientEffectPayload(effect, start, duration, intensity);
        }

        @Override
        public void encode(RegistryByteBuf buf, ClientEffectPayload value) {
            ClientEffectType.write(buf, value.effect());
            buf.writeBoolean(value.start());
            buf.writeVarInt(value.durationTicks());
            buf.writeFloat(value.intensity());
        }
    };

    @Override
    public Id<ClientEffectPayload> getId() {
        return ID;
    }
}
