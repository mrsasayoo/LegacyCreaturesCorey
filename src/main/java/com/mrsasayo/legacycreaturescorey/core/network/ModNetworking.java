package com.mrsasayo.legacycreaturescorey.core.network;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.DifficultyManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

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
        PayloadTypeRegistry.clientboundPlay().register(ClientEffectPayload.ID, ClientEffectPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DifficultySyncPayload.ID, DifficultySyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ServerHelloPayload.ID, ServerHelloPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ClientCapabilitiesPayload.ID, ClientCapabilitiesPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ClientCapabilitiesPayload.ID, (payload, context) ->
            context.server().execute(() -> handleCapabilities(context.player(), payload))
        );

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            CAPABILITIES.remove(player.getUUID());
            sendServerHello(player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            CAPABILITIES.remove(handler.getPlayer().getUUID())
        );
    }

    public static void sendClientEffect(ServerPlayer player, ClientEffectPayload payload) {
        if (!supportsFeature(player, ClientFeature.CLIENT_EFFECTS)) {
            maybeRemindHandshake(player);
            return;
        }
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendDifficultyUpdate(ServerPlayer player, DifficultySyncPayload payload) {
        if (!supportsFeature(player, ClientFeature.DIFFICULTY_HUD)) {
            maybeRemindHandshake(player);
            return;
        }
        ServerPlayNetworking.send(player, payload);
    }

    public static boolean supportsFeature(ServerPlayer player, ClientFeature feature) {
        ClientCapabilities caps = CAPABILITIES.get(player.getUUID());
        return caps != null && caps.supports(feature);
    }

    private static void handleCapabilities(ServerPlayer player, ClientCapabilitiesPayload payload) {
        boolean compatible = payload.protocolVersion() == PROTOCOL_VERSION;
        EnumSet<ClientFeature> features = payload.features().isEmpty()
            ? EnumSet.noneOf(ClientFeature.class)
            : EnumSet.copyOf(payload.features());

        if (!compatible) {
            Legacycreaturescorey.LOGGER.warn("{} tiene protocolo {} pero el servidor usa {}. Se deshabilitan las características cliente.",
                player.getName().getString(), payload.protocolVersion(), PROTOCOL_VERSION);
            features = EnumSet.noneOf(ClientFeature.class);
        }

        CAPABILITIES.put(player.getUUID(), new ClientCapabilities(payload.protocolVersion(),
            features, payload.modVersion(), compatible, System.currentTimeMillis()));

        // En cuanto el cliente confirma handshake, volvemos a sincronizar HUD.
        DifficultyManager.syncPlayer(player);
    }

    private static void sendServerHello(ServerPlayer player) {
        ServerPlayNetworking.send(player, new ServerHelloPayload(PROTOCOL_VERSION));
    }

    private static void maybeRemindHandshake(ServerPlayer player) {
        ClientCapabilities caps = CAPABILITIES.get(player.getUUID());
        if (caps != null) {
            return;
        }
        if (player.level().getGameTime() % (HANDSHAKE_REMINDER_INTERVAL_MS / 50L) == 0) {
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
