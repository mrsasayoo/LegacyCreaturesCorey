package com.mrsasayo.legacycreaturescorey.feature.mob;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.core.config.domain.difficulty_config;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.EffectiveDifficultyCalculator;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.PhantasmalHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

public final class MobSpawnHandler {

    private MobSpawnHandler() {}

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register(MobSpawnHandler::onEntityLoad);
    }

    private static void onEntityLoad(Entity entity, ServerLevel world) {
        if (!(entity instanceof Mob mob)) {
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
