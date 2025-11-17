package com.mrsasayo.legacycreaturescorey.api.event;

import com.mrsasayo.legacycreaturescorey.antifarm.ChunkActivityData;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;

/**
 * Telemetry-style events meant for dashboards or analytics that need to observe anti-farm activity.
 */
public final class AntiFarmDashboardEvents {

    private AntiFarmDashboardEvents() {
    }

    public enum UpdateReason {
        KILL_RECORDED,
        SAFE_ZONE_ACTIVATED
    }

    public static final Event<ChunkActivityUpdated> CHUNK_ACTIVITY_UPDATED = EventFactory.createArrayBacked(
        ChunkActivityUpdated.class,
        callbacks -> (world, chunkPos, data, reason, threshold, radiusChunks) -> {
            for (ChunkActivityUpdated callback : callbacks) {
                callback.onChunkActivityUpdated(world, chunkPos, data, reason, threshold, radiusChunks);
            }
        }
    );

    @FunctionalInterface
    public interface ChunkActivityUpdated {
        void onChunkActivityUpdated(ServerWorld world, ChunkPos chunkPos, ChunkActivityData data, UpdateReason reason, int threshold, int radiusChunks);
    }
}
