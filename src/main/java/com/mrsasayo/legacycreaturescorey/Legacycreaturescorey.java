package com.mrsasayo.legacycreaturescorey;

import com.mrsasayo.legacycreaturescorey.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.config.CoreyConfig;
import com.mrsasayo.legacycreaturescorey.difficulty.DifficultyManager;
import com.mrsasayo.legacycreaturescorey.difficulty.DifficultyTickHandler;
import com.mrsasayo.legacycreaturescorey.item.ModItems;
import com.mrsasayo.legacycreaturescorey.mob.MobSpawnHandler;
import com.mrsasayo.legacycreaturescorey.mob.TierParticleTicker;
import com.mrsasayo.legacycreaturescorey.mutation.MutationRegistry;
import com.mrsasayo.legacycreaturescorey.mutation.MutationRuntime;
import com.mrsasayo.legacycreaturescorey.mutation.data.MutationDataLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
// CORRECCIÓN: usar net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Legacycreaturescorey implements ModInitializer {
    
    public static final String MOD_ID = "legacycreaturescorey";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("🚀 Inicializando Legacy Creatures - Corey");
        
        CoreyConfig.INSTANCE.validate();
        ModDataAttachments.initialize();
        ModItems.initialize();
            MutationRegistry.initialize();
            MutationDataLoader.register();
        DifficultyTickHandler.register();
        MobSpawnHandler.register();
        TierParticleTicker.register();
        MutationRuntime.register();
        
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity player) {
                DifficultyManager.applyDeathPenalty(player);
            }
        });
        
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            DifficultyManager.initializePlayerDifficulty(handler.getPlayer());
        });
        
        LOGGER.info("✅ Legacy Creatures - Corey cargado exitosamente");
    }
}