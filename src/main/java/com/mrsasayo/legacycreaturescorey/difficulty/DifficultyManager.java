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
            long daysPassed = currentDay - lastDay;
            state.decayChunkHeat(daysPassed, CoreyConfig.INSTANCE.antiFarmDailyDecayAmount);

            int oldDifficulty = state.getGlobalDifficulty();
            int targetDifficulty = (int) Math.min(CoreyConfig.INSTANCE.maxGlobalDifficulty, currentDay);
            state.setGlobalDifficulty(targetDifficulty);
            state.setLastDayChecked(currentDay);

            if (oldDifficulty != targetDifficulty) {
                Legacycreaturescorey.LOGGER.info(
                    "📈 Dificultad Global sincronizada: {} → {} (Día {})",
                    oldDifficulty, targetDifficulty, currentDay
                );
            }

            triggerPlayerDifficultyRolls(server, daysPassed);
        }
    }

    private static void triggerPlayerDifficultyRolls(MinecraftServer server, long daysPassed) {
        if (daysPassed <= 0) {
            return;
        }

        double chance = CoreyConfig.INSTANCE.playerDifficultyIncreaseChance;
        int amount = CoreyConfig.INSTANCE.dailyIncreaseAmount;
        if (chance <= 0.0 || amount <= 0) {
            return;
        }

        var random = server.getOverworld().random;
        for (long dayOffset = 0; dayOffset < daysPassed; dayOffset++) {
            double roll = random.nextDouble();
            if (roll <= chance) {
                int affected = increaseDifficultyForOnlinePlayers(server, amount);
                if (affected > 0) {
                    Legacycreaturescorey.LOGGER.info(
                        "🎲 Incremento diario (tirada {} ≤ {}): +{} dificultad para {} jugadores conectados",
                        String.format("%.2f", roll),
                        String.format("%.2f", chance),
                        amount,
                        affected
                    );
                }
            }
        }
    }

    private static int increaseDifficultyForOnlinePlayers(MinecraftServer server, int amount) {
        int affected = 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
            data.increasePlayerDifficulty(amount);
            affected++;
        }
        return affected;
    }
    
    public static void applyDeathPenalty(ServerPlayerEntity player) {
        PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
        long currentTime = player.getEntityWorld().getTime();
        long streakWindow = Math.max(0L, CoreyConfig.INSTANCE.deathPenaltyCooldownTicks);

        data.recordDeath(currentTime);

        int deathsInWindow = data.countRecentDeaths(currentTime, streakWindow);
        if (deathsInWindow <= 0) {
            deathsInWindow = 1;
        }

        int basePenalty = Math.max(0, CoreyConfig.INSTANCE.deathPenaltyAmount);
        if (basePenalty <= 0) {
            data.setLastDeathPenaltyTime(currentTime);
            return;
        }

        double multiplier = 1.0 / deathsInWindow;
        int appliedPenalty = Math.max(1, (int) Math.round(basePenalty * multiplier));

        int currentDifficulty = data.getPlayerDifficulty();
        int newDifficulty = Math.max(0, currentDifficulty - appliedPenalty);

        data.setPlayerDifficulty(newDifficulty);
        data.setLastDeathPenaltyTime(currentTime);

        Legacycreaturescorey.LOGGER.info(
            "💀 {} penalización {} (x{} con {} muertes rápidas): {} → {}",
            player.getName().getString(),
            appliedPenalty,
            String.format("%.2f", multiplier),
            deathsInWindow,
            currentDifficulty,
            newDifficulty
        );
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