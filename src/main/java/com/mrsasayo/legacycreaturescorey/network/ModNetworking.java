package com.mrsasayo.legacycreaturescorey.network;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.difficulty.DifficultyManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralises the networking identifiers used by the mod and keeps track of client feature support.
 */
public final class ModNetworking {
    public static final int PROTOCOL_VERSION = 1;
    private static final Map<UUID, ClientCapabilities> CAPABILITIES = new ConcurrentHashMap<>();
    private static final long HANDSHAKE_REMINDER_INTERVAL_MS = Duration.ofMinutes(5).toMillis();

    private ModNetworking() {}

    public static void init() {
        PayloadTypeRegistry.playS2C().register(ClientEffectPayload.ID, ClientEffectPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DifficultySyncPayload.ID, DifficultySyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerHelloPayload.ID, ServerHelloPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ClientCapabilitiesPayload.ID, ClientCapabilitiesPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ClientCapabilitiesPayload.ID, (payload, context) ->
            context.server().execute(() -> handleCapabilities(context.player(), payload))
        );

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            CAPABILITIES.remove(player.getUuid());
            sendServerHello(player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            CAPABILITIES.remove(handler.getPlayer().getUuid())
        );
    }

    public static void sendClientEffect(ServerPlayerEntity player, ClientEffectPayload payload) {
        if (!supportsFeature(player, ClientFeature.CLIENT_EFFECTS)) {
            maybeRemindHandshake(player);
            return;
        }
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendDifficultyUpdate(ServerPlayerEntity player, DifficultySyncPayload payload) {
        if (!supportsFeature(player, ClientFeature.DIFFICULTY_HUD)) {
            maybeRemindHandshake(player);
            return;
        }
        ServerPlayNetworking.send(player, payload);
    }

    public static boolean supportsFeature(ServerPlayerEntity player, ClientFeature feature) {
        ClientCapabilities caps = CAPABILITIES.get(player.getUuid());
        return caps != null && caps.supports(feature);
    }

    private static void handleCapabilities(ServerPlayerEntity player, ClientCapabilitiesPayload payload) {
        boolean compatible = payload.protocolVersion() == PROTOCOL_VERSION;
        EnumSet<ClientFeature> features = payload.features().isEmpty()
            ? EnumSet.noneOf(ClientFeature.class)
            : EnumSet.copyOf(payload.features());

        if (!compatible) {
            Legacycreaturescorey.LOGGER.warn("{} tiene protocolo {} pero el servidor usa {}. Se deshabilitan las características cliente.",
                player.getName().getString(), payload.protocolVersion(), PROTOCOL_VERSION);
            features = EnumSet.noneOf(ClientFeature.class);
        }

        CAPABILITIES.put(player.getUuid(), new ClientCapabilities(payload.protocolVersion(),
            features, payload.modVersion(), compatible, System.currentTimeMillis()));

        // En cuanto el cliente confirma handshake, volvemos a sincronizar HUD.
        DifficultyManager.syncPlayer(player);
    }

    private static void sendServerHello(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new ServerHelloPayload(PROTOCOL_VERSION));
    }

    private static void maybeRemindHandshake(ServerPlayerEntity player) {
        ClientCapabilities caps = CAPABILITIES.get(player.getUuid());
        if (caps != null) {
            return;
        }
        if (player.getEntityWorld().getTime() % (HANDSHAKE_REMINDER_INTERVAL_MS / 50L) == 0) {
            Legacycreaturescorey.LOGGER.debug("Esperando handshake de red para {} antes de enviar payloads opcionales.",
                player.getName().getString());
        }
    }

    private record ClientCapabilities(int protocolVersion, EnumSet<ClientFeature> features,
                                      String modVersion, boolean compatible, long lastUpdated) {
        boolean supports(ClientFeature feature) {
            return compatible && features.contains(feature);
        }
    }
}
