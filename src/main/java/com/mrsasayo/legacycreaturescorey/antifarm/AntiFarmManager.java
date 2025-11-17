package com.mrsasayo.legacycreaturescorey.antifarm;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.api.event.AntiFarmDashboardEvents;
import com.mrsasayo.legacycreaturescorey.api.event.AntiFarmEvents;
import com.mrsasayo.legacycreaturescorey.config.CoreyConfig;
import com.mrsasayo.legacycreaturescorey.difficulty.CoreyServerState;
import com.mrsasayo.legacycreaturescorey.mob.data.MobTierRuleDataLoader;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;

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

    private static void onMobDeath(LivingEntity entity, net.minecraft.entity.damage.DamageSource damageSource) {
        if (!CoreyConfig.INSTANCE.antiFarmDetectionEnabled) {
            return;
        }
        if (!(entity instanceof MobEntity mob)) {
            return;
        }
        if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        if (!isTieredMob(mob.getType())) {
            return;
        }
        ChunkPos chunkPos = new ChunkPos(mob.getBlockPos());
        if (shouldIgnoreForAntiFarm(mob, chunkPos)) {
            return;
        }

        CoreyServerState state = CoreyServerState.get(world.getServer());
        long chunkKey = chunkPos.toLong();

        if (state.isChunkSpawnBlocked(chunkKey)) {
            return;
        }

        ChunkActivityData data = state.getOrCreateChunkActivity(chunkKey);
        long window = CoreyConfig.INSTANCE.antiFarmWindowTicks;
        int killCount = data.registerKill(world.getTime(), window);
        state.setChunkActivity(chunkKey, data);

        int threshold = AntiFarmEvents.THRESHOLD_MODIFIER.invoker().modifyThreshold(mob, chunkPos, CoreyConfig.INSTANCE.antiFarmKillThreshold);
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

    public static boolean shouldBlockTieredSpawns(MobEntity mob) {
        if (!CoreyConfig.INSTANCE.antiFarmRestrictTieredSpawns) {
            return false;
        }
        if (!(mob.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }
        if (!isTieredMob(mob.getType())) {
            return false;
        }
        CoreyServerState state = CoreyServerState.get(world.getServer());
        long chunkKey = new ChunkPos(mob.getBlockPos()).toLong();
        return state.isChunkSpawnBlocked(chunkKey);
    }

    private static void activateSafeZone(ServerWorld world, CoreyServerState state, ChunkPos center, MobEntity sample, int threshold) {
        int radius = Math.max(0, CoreyConfig.INSTANCE.antiFarmBlockRadiusChunks);
        long tick = world.getTime();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                long chunkKey = ChunkPos.toLong(center.x + dx, center.z + dz);
                state.markChunkSpawnBlocked(chunkKey, tick);
            }
        }
        if (CoreyConfig.INSTANCE.antiFarmLogDetections) {
            Legacycreaturescorey.LOGGER.warn(
                "🔥 Zona anti-granja activada ({}) alrededor del chunk {}|{} en {} para {}",
                (radius * 2) + 1,
                center.x,
                center.z,
                world.getRegistryKey().getValue(),
                sample.getType().getTranslationKey()
            );
        }
        ChunkActivityData centerData = state.getChunkActivity(center.toLong());
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

    private static boolean shouldIgnoreForAntiFarm(MobEntity mob, ChunkPos chunkPos) {
        if (mob.getCommandTags().contains(ANTI_FARM_EXEMPT_TAG)) {
            return true;
        }
        return AntiFarmEvents.SHOULD_IGNORE.invoker().shouldIgnore(mob, chunkPos);
    }
}
