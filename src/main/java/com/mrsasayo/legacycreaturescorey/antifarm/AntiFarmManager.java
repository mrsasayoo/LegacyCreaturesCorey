package com.mrsasayo.legacycreaturescorey.antifarm;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.config.CoreyConfig;
import com.mrsasayo.legacycreaturescorey.difficulty.CoreyServerState;
import com.mrsasayo.legacycreaturescorey.mob.ModEntityTypeTags;
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
        if (shouldIgnoreForAntiFarm(mob)) {
            return;
        }

        CoreyServerState state = CoreyServerState.get(world.getServer());
    ChunkPos chunkPos = new ChunkPos(mob.getBlockPos());
        long chunkKey = chunkPos.toLong();

        if (state.isChunkSpawnBlocked(chunkKey)) {
            return;
        }

        ChunkActivityData data = state.getOrCreateChunkActivity(chunkKey);
        long window = CoreyConfig.INSTANCE.antiFarmWindowTicks;
        int killCount = data.registerKill(world.getTime(), window);
        state.setChunkActivity(chunkKey, data);

        if (killCount >= CoreyConfig.INSTANCE.antiFarmKillThreshold) {
            activateSafeZone(world, state, chunkPos, mob);
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

    private static void activateSafeZone(ServerWorld world, CoreyServerState state, ChunkPos center, MobEntity sample) {
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
    }

    private static boolean isTieredMob(EntityType<?> type) {
        return type.isIn(ModEntityTypeTags.TIER_LEVE)
            || type.isIn(ModEntityTypeTags.TIER_BASIC)
            || type.isIn(ModEntityTypeTags.TIER_INTERMEDIATE)
            || type.isIn(ModEntityTypeTags.TIER_HARD);
    }

    private static boolean shouldIgnoreForAntiFarm(MobEntity mob) {
        return mob.getCommandTags().contains(ANTI_FARM_EXEMPT_TAG);
    }
}
