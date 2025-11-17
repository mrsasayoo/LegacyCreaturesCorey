package com.mrsasayo.legacycreaturescorey;

import com.mrsasayo.legacycreaturescorey.client.ClientDifficultyHud;
import com.mrsasayo.legacycreaturescorey.client.ClientEffectHandler;
import net.fabricmc.api.ClientModInitializer;

public class LegacycreaturescoreyClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientEffectHandler.init();
		ClientDifficultyHud.init();
	}
}