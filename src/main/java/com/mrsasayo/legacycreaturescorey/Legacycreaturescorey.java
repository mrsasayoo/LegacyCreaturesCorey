package com.mrsasayo.legacycreaturescorey;

import com.mrsasayo.legacycreaturescorey.feature.antifarm.AntiFarmManager;
import com.mrsasayo.legacycreaturescorey.feature.antifarm.data.AntiFarmExclusionDataLoader;
import com.mrsasayo.legacycreaturescorey.core.command.CoreyHudCommand;
import com.mrsasayo.legacycreaturescorey.core.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.core.config.config_manager;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.DifficultyManager;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.DifficultyTickHandler;
import com.mrsasayo.legacycreaturescorey.core.command.corey.CoreyCommand;
import com.mrsasayo.legacycreaturescorey.feature.mob.MobSpawnHandler;
import com.mrsasayo.legacycreaturescorey.feature.mob.TierParticleTicker;
import com.mrsasayo.legacycreaturescorey.feature.mob.data.MobAttributeDataLoader;
import com.mrsasayo.legacycreaturescorey.feature.mob.data.MobTierRuleDataLoader;
import com.mrsasayo.legacycreaturescorey.feature.mob.data.BiomeTierWeightDataLoader;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation_registry;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation_runtime;
import com.mrsasayo.legacycreaturescorey.mutation.data.mutation_data_loader;
import com.mrsasayo.legacycreaturescorey.content.item.ModItems;
import com.mrsasayo.legacycreaturescorey.content.health.CoreyHealthMonitor;
import com.mrsasayo.legacycreaturescorey.core.network.ModNetworking;
import com.mrsasayo.legacycreaturescorey.feature.loot.CoreyLootModifiers;
import com.mrsasayo.legacycreaturescorey.feature.loot.data.TieredLootDataLoader;
import com.mrsasayo.legacycreaturescorey.content.status.ModStatusEffects;
import com.mrsasayo.legacycreaturescorey.content.status.StatusEffectTicker;
import com.mrsasayo.legacycreaturescorey.feature.synergy.SynergyManager;
import com.mrsasayo.legacycreaturescorey.util.mutation_logger_initializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
// CORRECCIÓN: usar net.minecraft.server.level.ServerPlayer
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Legacycreaturescorey implements ModInitializer {
    
    public static final String MOD_ID = "legacycreaturescorey";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String MOD_VERSION = FabricLoader.getInstance()
        .getModContainer(MOD_ID)
        .map(container -> container.getMetadata().getVersion().getFriendlyString())
        .orElse("unknown");

    @Override
    public void onInitialize() {
        LOGGER.info("🚀 Inicializando Legacy Creatures - Corey");
        
        // Inicializar sistema de configuración modular (DDD)
        config_manager.initialize();
        
        // Inicializar sistema de logging multi-nivel para mutaciones
        mutation_logger_initializer.initialize();

        registerData();
        registerEvents();

        LOGGER.info("✅ Legacy Creatures - Corey cargado exitosamente");
    }

    private void registerData() {
        ModDataAttachments.initialize();
        mutation_registry.initialize();
        mutation_data_loader.register();
        TieredLootDataLoader.register();
        MobAttributeDataLoader.register();
        MobTierRuleDataLoader.register();
        BiomeTierWeightDataLoader.register();
    AntiFarmExclusionDataLoader.register();
        mutation_runtime.register();
        ModStatusEffects.init();
        CoreyLootModifiers.register();
        ModItems.init();
    }

    private void registerEvents() {
        DifficultyTickHandler.register();
        MobSpawnHandler.register();
        TierParticleTicker.register();
        StatusEffectTicker.register();
        ModNetworking.init();
        CoreyCommand.register();
        CoreyHudCommand.register();
        AntiFarmManager.register();
        SynergyManager.bootstrap();
        CoreyHealthMonitor.register();
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (!success) {
                LOGGER.warn("Recarga de datapacks fallida; se conserva la configuración actual.");
                notifyAdmins(server, Component.literal("⚠️ /reload falló; la configuración no se tocó."));
                return;
            }
            config_manager.reload_result result = config_manager.reload();
            if (result.success()) {
                LOGGER.info("[Config] {}", result.message());
                notifyAdmins(server, Component.literal("⚙️ Configuración recargada tras /reload: " + result.message()));
            } else {
                LOGGER.warn("[Config] {}", result.message());
                notifyAdmins(server, Component.literal("⚠️ /reload no pudo actualizar la configuración: " + result.message()));
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player) {
                DifficultyManager.applyDeathPenalty(player);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            DifficultyManager.initializePlayerDifficulty(handler.getPlayer());
        });
    }

    private static void notifyAdmins(MinecraftServer server, Component message) {
        server.getPlayerList().getPlayers().forEach(player -> {
            if (player.permissions() instanceof net.minecraft.server.permissions.LevelBasedPermissionSet lbps && lbps.level().id() >= 2) {
                player.sendSystemMessage(message, false);
            }
        });
    }
}