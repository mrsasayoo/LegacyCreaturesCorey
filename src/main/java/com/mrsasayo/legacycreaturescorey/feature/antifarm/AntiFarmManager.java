package com.mrsasayo.legacycreaturescorey.feature.antifarm;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.core.api.event.AntiFarmDashboardEvents;
import com.mrsasayo.legacycreaturescorey.core.api.event.AntiFarmEvents;
import com.mrsasayo.legacycreaturescorey.core.config.domain.antifarm_config;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.CoreyServerState;
import com.mrsasayo.legacycreaturescorey.feature.antifarm.data.AntiFarmExclusionDataLoader;
import com.mrsasayo.legacycreaturescorey.feature.mob.data.MobTierRuleDataLoader;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Gestiona la detección de granjas de mobs y aplica zonas seguras sin categorización.
 */
public final class AntiFarmManager {

    private static final String ANTI_FARM_EXEMPT_TAG = Legacycreaturescorey.MOD_ID + ":ignore_antifarm";

    private AntiFarmManager() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(AntiFarmManager::onMobDeath);
    }

    private static void onMobDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource damageSource) {
        if (!antifarm_config.isDetectionEnabled()) {
            return;
        }
        if (!(entity instanceof Mob mob)) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel world)) {
            return;
        }
        if (!isTieredMob(mob.getType())) {
            return;
        }
        ChunkPos chunkPos = ChunkPos.containing(mob.blockPosition());
        if (shouldIgnoreForAntiFarm(mob, chunkPos)) {
            return;
        }

        CoreyServerState state = CoreyServerState.get(world.getServer());
        long chunkKey = chunkPos.pack();

        if (state.isChunkSpawnBlocked(chunkKey)) {
            return;
        }

        ChunkActivityData data = state.getOrCreateChunkActivity(chunkKey);
        long window = antifarm_config.getWindowTicks();
        int killCount = data.registerKill(world.getGameTime(), window);
        state.setChunkActivity(chunkKey, data);

        int threshold = AntiFarmEvents.THRESHOLD_MODIFIER.invoker().modifyThreshold(mob, chunkPos, antifarm_config.getKillThreshold());
        AntiFarmDashboardEvents.CHUNK_ACTIVITY_UPDATED.invoker().onChunkActivityUpdated(
            world,
            chunkPos,
            data,
            AntiFarmDashboardEvents.UpdateReason.KILL_RECORDED,
            threshold,
            0
        );

        if (killCount >= threshold) {
            activateSafeZone(world, state, chunkPos, mob, threshold);
        }
    }

    public static boolean shouldBlockTieredSpawns(Mob mob) {
        if (!antifarm_config.isRestrictTieredSpawns()) {
            return false;
        }
        if (!(mob.level() instanceof ServerLevel world)) {
            return false;
        }
        if (!isTieredMob(mob.getType())) {
            return false;
        }
        CoreyServerState state = CoreyServerState.get(world.getServer());
        long chunkKey = ChunkPos.containing(mob.blockPosition()).pack();
        return state.isChunkSpawnBlocked(chunkKey);
    }

    private static void activateSafeZone(ServerLevel world, CoreyServerState state, ChunkPos center, Mob sample, int threshold) {
        int radius = Math.max(0, antifarm_config.getBlockRadiusChunks());
        long tick = world.getGameTime();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                long chunkKey = ChunkPos.pack(center.x() + dx, center.z() + dz);
                state.markChunkSpawnBlocked(chunkKey, tick);
            }
        }
        if (antifarm_config.isLogDetections()) {
            Legacycreaturescorey.LOGGER.warn(
                "🔥 Zona anti-granja activada ({}) alrededor del chunk {}|{} en {} para {}",
                (radius * 2) + 1,
                center.x(),
                center.z(),
                world.dimension().identifier(),
                sample.getType().getDescriptionId()
            );
        }
        ChunkActivityData centerData = state.getChunkActivity(center.pack());
        AntiFarmDashboardEvents.CHUNK_ACTIVITY_UPDATED.invoker().onChunkActivityUpdated(
            world,
            center,
            centerData,
            AntiFarmDashboardEvents.UpdateReason.SAFE_ZONE_ACTIVATED,
            threshold,
            radius
        );
        AntiFarmEvents.CHUNK_BLOCKED.invoker().onChunkBlocked(world, center, sample, radius);
    }

    private static boolean isTieredMob(EntityType<?> type) {
        return MobTierRuleDataLoader.getAllowedTiers(type) != null;
    }

    private static boolean shouldIgnoreForAntiFarm(Mob mob, ChunkPos chunkPos) {
        if (mob.entityTags().contains(ANTI_FARM_EXEMPT_TAG)) {
            return true;
        }
        if (AntiFarmExclusionDataLoader.isExcluded(mob.getType())) {
            return true;
        }
        return AntiFarmEvents.SHOULD_IGNORE.invoker().shouldIgnore(mob, chunkPos);
    }
}
