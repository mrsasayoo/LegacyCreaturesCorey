package com.mrsasayo.legacycreaturescorey.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;

/**
 * Hooks around the anti-farm detection pipeline, allowing datapacks or other mods to tune thresholds
 * or suppress detections for specific mobs/chunks.
 */
public final class AntiFarmEvents {

    private AntiFarmEvents() {
    }

    /**
     * Gives other systems a chance to skip anti-farm processing for a mob death. If any callback returns
     * {@code true}, the detection is bypassed entirely.
     */
    public static final Event<ShouldIgnore> SHOULD_IGNORE = EventFactory.createArrayBacked(
        ShouldIgnore.class,
        callbacks -> (mob, chunkPos) -> {
            for (ShouldIgnore callback : callbacks) {
                if (callback.shouldIgnore(mob, chunkPos)) {
                    return true;
                }
            }
            return false;
        }
    );

    /**
     * Allows the kill threshold to be adjusted dynamically. Each callback receives the current value and
     * returns the new value to feed into subsequent callbacks.
     */
    public static final Event<ThresholdModifier> THRESHOLD_MODIFIER = EventFactory.createArrayBacked(
        ThresholdModifier.class,
        callbacks -> (mob, chunkPos, currentThreshold) -> {
            int result = Math.max(1, currentThreshold);
            for (ThresholdModifier callback : callbacks) {
                result = Math.max(1, callback.modifyThreshold(mob, chunkPos, result));
            }
            return result;
        }
    );

    /**
     * Fires after a safe zone has been activated so other systems can react or replicate the block elsewhere.
     */
    public static final Event<ChunkBlocked> CHUNK_BLOCKED = EventFactory.createArrayBacked(
        ChunkBlocked.class,
        callbacks -> (world, center, sample, radius) -> {
            for (ChunkBlocked callback : callbacks) {
                callback.onChunkBlocked(world, center, sample, radius);
            }
        }
    );

    @FunctionalInterface
    public interface ShouldIgnore {
        boolean shouldIgnore(MobEntity mob, ChunkPos chunkPos);
    }

    @FunctionalInterface
    public interface ThresholdModifier {
        int modifyThreshold(MobEntity mob, ChunkPos chunkPos, int currentThreshold);
    }

    @FunctionalInterface
    public interface ChunkBlocked {
        void onChunkBlocked(ServerWorld world, ChunkPos center, MobEntity sample, int radius);
    }
}
