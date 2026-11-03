package com.mrsasayo.legacycreaturescorey.core.network;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Payload enviado al cliente con los valores de dificultad para mostrar en HUD.
 */
public record DifficultySyncPayload(int globalDifficulty,
                                    int maxGlobalDifficulty,
                                    int personalDifficulty,
                                    boolean hudEnabled) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DifficultySyncPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "difficulty_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DifficultySyncPayload> CODEC = new StreamCodec<>() {
        @Override
        public DifficultySyncPayload decode(RegistryFriendlyByteBuf buf) {
            int global = buf.readVarInt();
            int maxGlobal = buf.readVarInt();
            int personal = buf.readVarInt();
            boolean hudEnabled = buf.readBoolean();
            return new DifficultySyncPayload(global, maxGlobal, personal, hudEnabled);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, DifficultySyncPayload value) {
            buf.writeVarInt(value.globalDifficulty());
            buf.writeVarInt(value.maxGlobalDifficulty());
            buf.writeVarInt(value.personalDifficulty());
            buf.writeBoolean(value.hudEnabled());
        }
    };

    @Override
    public CustomPacketPayload.Type<DifficultySyncPayload> type() {
        return ID;
    }
}
