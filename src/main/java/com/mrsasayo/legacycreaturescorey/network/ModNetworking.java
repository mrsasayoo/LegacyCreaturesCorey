package com.mrsasayo.legacycreaturescorey.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Centralises the networking identifiers used by the mod.
 */
public final class ModNetworking {
    private ModNetworking() {}

    public static void init() {
        PayloadTypeRegistry.playS2C().register(ClientEffectPayload.ID, ClientEffectPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DifficultySyncPayload.ID, DifficultySyncPayload.CODEC);
    }

    public static void sendClientEffect(ServerPlayerEntity player, ClientEffectPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendDifficultyUpdate(ServerPlayerEntity player, DifficultySyncPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }
}
