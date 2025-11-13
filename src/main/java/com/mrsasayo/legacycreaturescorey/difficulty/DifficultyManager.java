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

            var random = server.getOverworld().random;
            double globalChance = CoreyConfig.INSTANCE.dailyIncreaseChance;
            double roll = random.nextDouble();
            if (roll <= globalChance) {
                int oldDifficulty = state.getGlobalDifficulty();
                state.increaseGlobalDifficulty(CoreyConfig.INSTANCE.dailyIncreaseAmount);
                int newDifficulty = state.getGlobalDifficulty();

                Legacycreaturescorey.LOGGER.info(
                    "📈 Dificultad Global: {} → {} (Día {}, tirada={})",
                    oldDifficulty, newDifficulty, currentDay, String.format("%.2f", roll)
                );

                syncDifficultyToPlayers(server, CoreyConfig.INSTANCE.dailyIncreaseAmount, random);
            }
        }
    }
    
    private static void syncDifficultyToPlayers(MinecraftServer server, int amount, net.minecraft.util.math.random.Random random) {
        double playerChance = CoreyConfig.INSTANCE.playerDifficultyIncreaseChance;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (playerChance >= 1.0 || random.nextDouble() <= playerChance) {
                PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
                data.increasePlayerDifficulty(amount);
            }
        }
    }
    
    public static void applyDeathPenalty(ServerPlayerEntity player) {
        PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
    long currentTime = player.getEntityWorld().getTime();
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
            MinecraftServer server = player.getEntityWorld().getServer();
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