package com.mrsasayo.legacycreaturescorey;

import com.mrsasayo.legacycreaturescorey.client.ClientDifficultyHud;
import com.mrsasayo.legacycreaturescorey.client.ClientEffectHandler;
import com.mrsasayo.legacycreaturescorey.network.ClientCapabilitiesPayload;
import com.mrsasayo.legacycreaturescorey.network.ClientFeature;
import com.mrsasayo.legacycreaturescorey.network.ModNetworking;
import com.mrsasayo.legacycreaturescorey.network.ServerHelloPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.EnumSet;

public class LegacycreaturescoreyClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientEffectHandler.init();
		ClientDifficultyHud.init();

		ClientPlayNetworking.registerGlobalReceiver(ServerHelloPayload.ID, (payload, context) ->
			context.client().execute(() -> sendCapabilities())
		);
	ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> sendCapabilities());
	}

	private static void sendCapabilities() {
		EnumSet<ClientFeature> features = EnumSet.of(ClientFeature.CLIENT_EFFECTS, ClientFeature.DIFFICULTY_HUD);
		ClientPlayNetworking.send(new ClientCapabilitiesPayload(
			Legacycreaturescorey.MOD_VERSION,
			ModNetworking.PROTOCOL_VERSION,
			features
		));
	}
}