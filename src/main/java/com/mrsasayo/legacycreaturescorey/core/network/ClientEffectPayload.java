package com.mrsasayo.legacycreaturescorey.core.network;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload sent from the server to the client instructing it to play a custom feedback effect.
 */
public record ClientEffectPayload(ClientEffectType effect,
                                  boolean start,
                                  int durationTicks,
                                  float intensity) implements CustomPacketPayload {

    public static final Id<ClientEffectPayload> ID = new Id<>(ResourceLocation.ofMember(Legacycreaturescorey.MOD_ID, "client_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientEffectPayload> CODEC = new StreamCodec<>() {
        @Override
        public ClientEffectPayload decode(RegistryFriendlyByteBuf buf) {
            ClientEffectType effect = ClientEffectType.read(buf);
            boolean start = buf.readBoolean();
            int duration = buf.readVarInt();
            float intensity = buf.readFloat();
            return new ClientEffectPayload(effect, start, duration, intensity);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ClientEffectPayload value) {
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
