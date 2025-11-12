package com.mrsasayo.legacycreaturescorey.difficulty;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.component.PlayerDifficultyData;
import com.mrsasayo.legacycreaturescorey.config.CoreyConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class DifficultyManager {
    
    public static long getCurrentDay(MinecraftServer server) {
        return server.getOverworld().getTime() / 24000L;
    }
    
    public static void checkDailyIncrease(MinecraftServer server) {
        CoreyServerState state = CoreyServerState.get(server);
        long currentDay = getCurrentDay(server);
        long lastDay = state.getLastDayChecked();
        
        if (currentDay > lastDay) {
            state.setLastDayChecked(currentDay);
            
            if (Math.random() < CoreyConfig.INSTANCE.dailyIncreaseChance) {
                int oldDifficulty = state.getGlobalDifficulty();
                state.increaseGlobalDifficulty(CoreyConfig.INSTANCE.dailyIncreaseAmount);
                int newDifficulty = state.getGlobalDifficulty();
                
                Legacycreaturescorey.LOGGER.info(
                    "📈 Dificultad Global: {} → {} (Día {})",
                    oldDifficulty, newDifficulty, currentDay
                );
                
                syncDifficultyToPlayers(server, CoreyConfig.INSTANCE.dailyIncreaseAmount);
            }
        }
    }
    
    private static void syncDifficultyToPlayers(MinecraftServer server, int amount) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
            data.increasePlayerDifficulty(amount);
        }
    }
    
    public static void applyDeathPenalty(ServerPlayerEntity player) {
        PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
        // CORRECCIÓN: usar getServerWorld() en lugar de getWorld()
        long currentTime = player.getServerWorld().getTime();
        long lastPenaltyTime = data.getLastDeathPenaltyTime();
        long cooldown = CoreyConfig.INSTANCE.deathPenaltyCooldownTicks;
        
        if (currentTime - lastPenaltyTime >= cooldown) {
            int currentDifficulty = data.getPlayerDifficulty();
            int penalty = CoreyConfig.INSTANCE.deathPenaltyAmount;
            int newDifficulty = Math.max(0, currentDifficulty - penalty);
            
            data.setPlayerDifficulty(newDifficulty);
            data.setLastDeathPenaltyTime(currentTime);
            
            Legacycreaturescorey.LOGGER.info(
                "💀 {} recibió penalización: {} → {}",
                player.getName().getString(), currentDifficulty, newDifficulty
            );
        }
    }
    
    public static void initializePlayerDifficulty(ServerPlayerEntity player) {
        PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
        
        if (data.getPlayerDifficulty() == 0) {
            // CORRECCIÓN: acceder directamente al campo server (es público en Yarn)
            MinecraftServer server = player.server;
            if (server != null) {
                CoreyServerState state = CoreyServerState.get(server);
                data.setPlayerDifficulty(state.getGlobalDifficulty());
                
                Legacycreaturescorey.LOGGER.info(
                    "🎮 {} inicializado con dificultad: {}",
                    player.getName().getString(),
                    state.getGlobalDifficulty()
                );
            }
        }
    }
}