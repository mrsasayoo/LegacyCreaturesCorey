package com.mrsasayo.legacycreaturescorey.core.network;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload enviado al cliente con los valores de dificultad para mostrar en HUD.
 */
public record DifficultySyncPayload(int globalDifficulty,
                                    int maxGlobalDifficulty,
                                    int personalDifficulty,
                                    boolean hudEnabled) implements CustomPayload {

    public static final Id<DifficultySyncPayload> ID = new Id<>(Identifier.of(Legacycreaturescorey.MOD_ID, "difficulty_sync"));

    public static final PacketCodec<RegistryByteBuf, DifficultySyncPayload> CODEC = new PacketCodec<>() {
        @Override
        public DifficultySyncPayload decode(RegistryByteBuf buf) {
            int global = buf.readVarInt();
            int maxGlobal = buf.readVarInt();
            int personal = buf.readVarInt();
            boolean hudEnabled = buf.readBoolean();
            return new DifficultySyncPayload(global, maxGlobal, personal, hudEnabled);
        }

        @Override
        public void encode(RegistryByteBuf buf, DifficultySyncPayload value) {
            buf.writeVarInt(value.globalDifficulty());
            buf.writeVarInt(value.maxGlobalDifficulty());
            buf.writeVarInt(value.personalDifficulty());
            buf.writeBoolean(value.hudEnabled());
        }
    };

    @Override
    public Id<DifficultySyncPayload> getId() {
        return ID;
    }
}
