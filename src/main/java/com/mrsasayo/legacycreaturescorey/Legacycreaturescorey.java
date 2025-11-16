package com.mrsasayo.legacycreaturescorey;

import com.mrsasayo.legacycreaturescorey.antifarm.AntiFarmManager;
import com.mrsasayo.legacycreaturescorey.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.config.CoreyConfig;
import com.mrsasayo.legacycreaturescorey.difficulty.DifficultyManager;
import com.mrsasayo.legacycreaturescorey.difficulty.DifficultyTickHandler;
import com.mrsasayo.legacycreaturescorey.command.corey.CoreyCommand;
import com.mrsasayo.legacycreaturescorey.mob.MobSpawnHandler;
import com.mrsasayo.legacycreaturescorey.mob.TierParticleTicker;
import com.mrsasayo.legacycreaturescorey.mob.data.MobAttributeDataLoader;
import com.mrsasayo.legacycreaturescorey.mutation.MutationRegistry;
import com.mrsasayo.legacycreaturescorey.mutation.MutationRuntime;
import com.mrsasayo.legacycreaturescorey.mutation.data.MutationDataLoader;
import com.mrsasayo.legacycreaturescorey.network.ModNetworking;
import com.mrsasayo.legacycreaturescorey.loot.CoreyLootModifiers;
import com.mrsasayo.legacycreaturescorey.loot.data.TieredLootDataLoader;
import com.mrsasayo.legacycreaturescorey.command.MutationCommand;
import com.mrsasayo.legacycreaturescorey.status.ModStatusEffects;
import com.mrsasayo.legacycreaturescorey.status.StatusEffectTicker;
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
        MutationRegistry.initialize();
        MutationDataLoader.register();
        TieredLootDataLoader.register();
        MobAttributeDataLoader.register();
        DifficultyTickHandler.register();
        MobSpawnHandler.register();
        TierParticleTicker.register();
        MutationRuntime.register();
        ModStatusEffects.init();
        StatusEffectTicker.register();
        ModNetworking.init();
        MutationCommand.register();
        CoreyCommand.register();
        CoreyLootModifiers.register();
        AntiFarmManager.register();
        
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