package com.mrsasayo.legacycreaturescorey.api;

import com.mrsasayo.legacycreaturescorey.antifarm.ChunkActivityData;
import com.mrsasayo.legacycreaturescorey.difficulty.CoreyServerState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;

import java.util.Map;
import java.util.Optional;

/**
 * Snapshot-style helpers intended for dashboards that want to visualize anti-farm heatmaps.
 */
public final class AntiFarmDashboardApi {

    private AntiFarmDashboardApi() {
    }

    public static Map<Long, ChunkActivityData> getChunkActivitySnapshot(MinecraftServer server) {
        return CoreyServerState.get(server).getAllChunkActivity();
    }

    public static Optional<ChunkActivityData> getChunkActivity(MinecraftServer server, ChunkPos chunkPos) {
        if (server == null || chunkPos == null) {
            return Optional.empty();
        }
        CoreyServerState state = CoreyServerState.get(server);
        return Optional.ofNullable(state.getChunkActivity(chunkPos.toLong()));
    }

    public static boolean isChunkBlocked(MinecraftServer server, ChunkPos chunkPos) {
        if (server == null || chunkPos == null) {
            return false;
        }
        CoreyServerState state = CoreyServerState.get(server);
        return state.isChunkSpawnBlocked(chunkPos.toLong());
    }
}
