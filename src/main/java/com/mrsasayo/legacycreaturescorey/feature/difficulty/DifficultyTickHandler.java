package com.mrsasayo.legacycreaturescorey.feature.difficulty;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;

public class DifficultyTickHandler {
    
    private static long lastTickTime = 0L;
    private static final long CHECK_INTERVAL = 20L;
    
    public static void register() {
        Legacycreaturescorey.LOGGER.info("Registrando manejador de ticks de dificultad");
        
        ServerTickEvents.END_SERVER_TICK.register(DifficultyTickHandler::onServerTick);
        
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            DifficultyManager.initializePlayerDifficulty(handler.getPlayer());
        });
    }
    
    private static void onServerTick(MinecraftServer server) {
        // CORRECCIÓN: usar getTicks() en lugar de getTickCount()
        long currentTick = server.getTicks();
        
        if (currentTick - lastTickTime >= CHECK_INTERVAL) {
            lastTickTime = currentTick;
            DifficultyManager.checkDailyIncrease(server);
        }
    }
}