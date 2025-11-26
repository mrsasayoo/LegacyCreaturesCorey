package com.mrsasayo.legacycreaturescorey.mob;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.config.CoreyConfig;
import com.mrsasayo.legacycreaturescorey.difficulty.EffectiveDifficultyCalculator;
import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;
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

        if (CoreyConfig.INSTANCE.debugLogProbabilityDetails) {
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
