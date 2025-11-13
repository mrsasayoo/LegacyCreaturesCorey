package com.mrsasayo.legacycreaturescorey.difficulty;

import com.mrsasayo.legacycreaturescorey.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.component.PlayerDifficultyData;
import com.mrsasayo.legacycreaturescorey.config.CoreyConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public final class EffectiveDifficultyCalculator {
    private EffectiveDifficultyCalculator() {
    }

    public static int calculate(ServerWorld world, BlockPos origin) {
        MinecraftServer server = world.getServer();
        if (server == null) {
            return 0;
        }

        double centerX = origin.getX() + 0.5D;
        double centerY = origin.getY() + 0.5D;
        double centerZ = origin.getZ() + 0.5D;

        List<ServerPlayerEntity> dimensionPlayers = world.getPlayers(player -> !player.isSpectator());

        CoreyServerState state = CoreyServerState.get(server);
        int globalDifficulty = state.getGlobalDifficulty();

        if (dimensionPlayers.isEmpty()) {
            return globalDifficulty;
        }

        CoreyConfig config = CoreyConfig.INSTANCE;
        double radius = config.effectiveDifficultyRadius;

        double weightedDifficulty = 0.0D;
        double totalWeight = 0.0D;
        boolean anyWithinRadius = radius <= 0.0D;

        for (ServerPlayerEntity player : dimensionPlayers) {
            PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
            double contextualDifficulty = (globalDifficulty + data.getPlayerDifficulty()) * 0.5D;

            double distanceSq = player.squaredDistanceTo(centerX, centerY, centerZ);
            double weight;

            if (radius <= 0.0D) {
                weight = 1.0D;
            } else {
                double distance = Math.sqrt(distanceSq);
                if (distance <= radius) {
                    anyWithinRadius = true;
                }
                double normalized = distance / Math.max(radius, 1.0D);
                weight = 1.0D / (1.0D + normalized);
            }

            weightedDifficulty += contextualDifficulty * weight;
            totalWeight += weight;
        }

        if (radius > 0.0D && !anyWithinRadius) {
            return globalDifficulty;
        }

        if (totalWeight <= 0.0D) {
            return globalDifficulty;
        }

        double average = weightedDifficulty / totalWeight;
        return (int) Math.ceil(average);
    }

    public static String format(int difficulty) {
        return Integer.toString(difficulty);
    }
}
