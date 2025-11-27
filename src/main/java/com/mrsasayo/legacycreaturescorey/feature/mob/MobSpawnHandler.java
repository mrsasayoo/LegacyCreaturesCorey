package com.mrsasayo.legacycreaturescorey.feature.mob;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.core.config.domain.difficulty_config;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.EffectiveDifficultyCalculator;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.PhantasmalHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class MobSpawnHandler {

    private MobSpawnHandler() {}

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register(MobSpawnHandler::onEntityLoad);
    }

    private static void onEntityLoad(Entity entity, ServerWorld world) {
        if (!(entity instanceof MobEntity mob)) {
            return;
        }

        if (PhantasmalHandler.INSTANCE.isIllusion(mob)) {
            return;
        }

        BlockPos pos = mob.getBlockPos();
        int effectiveDifficulty = EffectiveDifficultyCalculator.calculate(world, pos);
        MobTier resultingTier = TierManager.tryCategorize(mob, effectiveDifficulty);

        if (difficulty_config.isDebugLogProbabilityDetails()) {
            Legacycreaturescorey.LOGGER.info(
                "📍 Hostil recién spawneado: {} en {},{},{} ({}) | dificultad efectiva={} | tier={}",
                entity.getType().getTranslationKey(),
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                world.getRegistryKey().getValue(),
                EffectiveDifficultyCalculator.format(effectiveDifficulty),
                resultingTier.getDisplayName()
            );
        }
    }
}
