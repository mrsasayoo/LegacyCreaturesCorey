package com.mrsasayo.legacycreaturescorey.feature.difficulty;

import com.mrsasayo.legacycreaturescorey.feature.antifarm.ChunkActivityData;
import com.mrsasayo.legacycreaturescorey.core.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.core.component.PlayerDifficultyData;
import com.mrsasayo.legacycreaturescorey.core.config.domain.antifarm_config;
import com.mrsasayo.legacycreaturescorey.core.config.domain.difficulty_config;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

public final class EffectiveDifficultyCalculator {
    private EffectiveDifficultyCalculator() {
    }

    public static int calculate(ServerLevel world, BlockPos origin) {
        MinecraftServer server = world.getServer();
        if (server == null) {
            return 0;
        }

        double centerX = origin.getX() + 0.5D;
        double centerY = origin.getY() + 0.5D;
        double centerZ = origin.getZ() + 0.5D;

        List<ServerPlayer> dimensionPlayers = world.getPlayers(player -> !player.isSpectator());

        CoreyServerState state = CoreyServerState.get(server);
        int globalDifficulty = state.getGlobalDifficulty();

        if (dimensionPlayers.isEmpty()) {
            return globalDifficulty;
        }

        double radius = difficulty_config.getEffectiveDifficultyRadius();


        List<ServerPlayer> relevantPlayers;
        if (radius > 0.0D) {
            double radiusSq = radius * radius;
            relevantPlayers = dimensionPlayers.stream()
                .filter(player -> player.distanceToSqr(centerX, centerY, centerZ) <= radiusSq)
                .toList();
        } else {
            relevantPlayers = dimensionPlayers;
        }

        if (relevantPlayers.isEmpty()) {
            return globalDifficulty;
        }

    double weightedDifficulty = 0.0D;
    double totalWeight = 0.0D;
    boolean boostedBiome = isBoostedBiome(world, origin);
    double biomeMultiplier = boostedBiome ? difficulty_config.getBiomeDifficultyMultiplier() : 1.0D;
    double antiFarmMultiplier = resolveAntiFarmMultiplier(state, origin);

        for (ServerPlayer player : relevantPlayers) {
            PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
            double contextualDifficulty = (globalDifficulty + data.getPlayerDifficulty()) * 0.5D;
            contextualDifficulty *= biomeMultiplier;
            contextualDifficulty *= antiFarmMultiplier;

            double distanceSq = player.distanceToSqr(centerX, centerY, centerZ);
            double weight;

            if (radius <= 0.0D) {
                weight = 1.0D;
            } else {
                double distance = Math.sqrt(distanceSq);
                double normalized = distance / Math.max(radius, 1.0D);
                weight = 1.0D / (1.0D + normalized);
            }

            weightedDifficulty += contextualDifficulty * weight;
            totalWeight += weight;
        }

        if (totalWeight <= 0.0D) {
            return globalDifficulty;
        }

        double average = weightedDifficulty / totalWeight;
        return (int) Math.ceil(average);
    }

    private static boolean isBoostedBiome(ServerLevel world, BlockPos origin) {
        Holder<Biome> biome = world.getBiome(origin);
        Identifier biomeId = biome.unwrapKey().map(key -> key.identifier()).orElse(null);
        if (biomeId == null) {
            return false;
        }

        return switch (biomeId.toString()) {
            case "minecraft:snowy_slopes",
                "minecraft:deep_dark",
                "minecraft:crimson_forest",
                "minecraft:the_end",
                "minecraft:dark_forest",
                "minecraft:end_highlands",
                "minecraft:jagged_peaks",
                "minecraft:swamp",
                "minecraft:nether_wastes",
                "minecraft:deep_ocean" -> true;
            default -> false;
        };
    }

    public static String format(int difficulty) {
        return Integer.toString(difficulty);
    }

    private static double resolveAntiFarmMultiplier(CoreyServerState state, BlockPos origin) {
        if (!antifarm_config.isDetectionEnabled() || !antifarm_config.isHeatPenaltyEnabled()) {
            return 1.0D;
        }

        ChunkPos chunkPos = ChunkPos.containing(origin);
        ChunkActivityData data = state.getChunkActivity(chunkPos.pack());
        if (data == null) {
            return 1.0D;
        }

        double minMultiplier = clamp01(antifarm_config.getHeatPenaltyMinMultiplier());
        if (data.isSpawnBlocked()) {
            return minMultiplier;
        }

        int killCount = data.getKillCount();
        if (killCount <= 0) {
            return 1.0D;
        }

        int threshold = Math.max(1, antifarm_config.getKillThreshold());
        double ratio = Math.min(1.0D, (double) killCount / threshold);
        double exponent = Math.max(0.1D, antifarm_config.getHeatPenaltyExponent());
        double scaled = Math.pow(ratio, exponent);
        double multiplier = 1.0D - scaled * (1.0D - minMultiplier);
        return Math.max(minMultiplier, Math.min(1.0D, multiplier));
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) {
            return 1.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
