package com.mrsasayo.legacycreaturescorey.difficulty;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.component.PlayerDifficultyData;
import com.mrsasayo.legacycreaturescorey.config.CoreyConfig;
import com.mrsasayo.legacycreaturescorey.network.DifficultySyncPayload;
import com.mrsasayo.legacycreaturescorey.network.ModNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class DifficultyManager {

    private static final int PLAYER_ROLL_INCREMENT = 1;
    
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
                broadcastDifficultySnapshot(server, state);
            }

            triggerPlayerDifficultyRolls(server, state, daysPassed);
        }
    }

    private static void triggerPlayerDifficultyRolls(MinecraftServer server, CoreyServerState state, long daysPassed) {
        if (daysPassed <= 0) {
            return;
        }

        double chance = CoreyConfig.INSTANCE.playerDifficultyIncreaseChance;
        if (chance <= 0.0) {
            return;
        }

        double roll = server.getOverworld().random.nextDouble();
        if (roll > chance) {
            return;
        }

        int affected = increaseDifficultyForOnlinePlayers(server, state, PLAYER_ROLL_INCREMENT);
        if (affected > 0) {
            Legacycreaturescorey.LOGGER.info(
                "🎲 Incremento diario único tras {} día(s) (tirada {} ≤ {}): +{} para {} jugadores conectados",
                daysPassed,
                String.format("%.2f", roll),
                String.format("%.2f", chance),
                PLAYER_ROLL_INCREMENT,
                affected
            );
        }
    }

    private static int increaseDifficultyForOnlinePlayers(MinecraftServer server, CoreyServerState state, int amount) {
        int affected = 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
            data.increasePlayerDifficulty(amount);
            affected++;
            sendDifficultySnapshot(player, state, data);
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

    MinecraftServer server = player.getEntityWorld().getServer();
        if (server != null) {
            CoreyServerState state = CoreyServerState.get(server);
            sendDifficultySnapshot(player, state, data);
        }
    }
    
    public static void initializePlayerDifficulty(ServerPlayerEntity player) {
        PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return;
        }

        CoreyServerState state = CoreyServerState.get(server);
        if (data.getPlayerDifficulty() == 0) {
            data.setPlayerDifficulty(state.getGlobalDifficulty());

            Legacycreaturescorey.LOGGER.info(
                "🎮 {} inicializado con dificultad: {}",
                player.getName().getString(),
                state.getGlobalDifficulty()
            );
        }

        sendDifficultySnapshot(player, state, data);
    }

    private static void broadcastDifficultySnapshot(MinecraftServer server, CoreyServerState state) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
            sendDifficultySnapshot(player, state, data);
        }
    }

    private static void sendDifficultySnapshot(ServerPlayerEntity player, CoreyServerState state, PlayerDifficultyData data) {
        DifficultySyncPayload payload = new DifficultySyncPayload(
            state.getGlobalDifficulty(),
            CoreyConfig.INSTANCE.maxGlobalDifficulty,
            data.getPlayerDifficulty(),
            CoreyConfig.INSTANCE.enableDifficultyHud && data.isDifficultyHudEnabled()
        );
        ModNetworking.sendDifficultyUpdate(player, payload);
    }

    public static void syncPlayer(ServerPlayerEntity player) {
    MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return;
        }
        CoreyServerState state = CoreyServerState.get(server);
        PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
        sendDifficultySnapshot(player, state, data);
    }
}