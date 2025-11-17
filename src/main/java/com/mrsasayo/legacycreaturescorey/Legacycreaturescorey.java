package com.mrsasayo.legacycreaturescorey;

import com.mrsasayo.legacycreaturescorey.antifarm.AntiFarmManager;
import com.mrsasayo.legacycreaturescorey.command.CoreyHudCommand;
import com.mrsasayo.legacycreaturescorey.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.config.CoreyConfig;
import com.mrsasayo.legacycreaturescorey.difficulty.DifficultyManager;
import com.mrsasayo.legacycreaturescorey.difficulty.DifficultyTickHandler;
import com.mrsasayo.legacycreaturescorey.command.corey.CoreyCommand;
import com.mrsasayo.legacycreaturescorey.mob.MobSpawnHandler;
import com.mrsasayo.legacycreaturescorey.mob.TierParticleTicker;
import com.mrsasayo.legacycreaturescorey.mob.data.MobAttributeDataLoader;
import com.mrsasayo.legacycreaturescorey.mob.data.MobTierRuleDataLoader;
import com.mrsasayo.legacycreaturescorey.mob.data.BiomeTierWeightDataLoader;
import com.mrsasayo.legacycreaturescorey.mutation.MutationRegistry;
import com.mrsasayo.legacycreaturescorey.mutation.MutationRuntime;
import com.mrsasayo.legacycreaturescorey.mutation.data.MutationDataLoader;
import com.mrsasayo.legacycreaturescorey.health.CoreyHealthMonitor;
import com.mrsasayo.legacycreaturescorey.network.ModNetworking;
import com.mrsasayo.legacycreaturescorey.loot.CoreyLootModifiers;
import com.mrsasayo.legacycreaturescorey.loot.data.TieredLootDataLoader;
import com.mrsasayo.legacycreaturescorey.command.MutationCommand;
import com.mrsasayo.legacycreaturescorey.status.ModStatusEffects;
import com.mrsasayo.legacycreaturescorey.status.StatusEffectTicker;
import com.mrsasayo.legacycreaturescorey.synergy.SynergyManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
// CORRECCIÓN: usar net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Legacycreaturescorey implements ModInitializer {
    
    public static final String MOD_ID = "legacycreaturescorey";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("🚀 Inicializando Legacy Creatures - Corey");
        CoreyConfig.INSTANCE.loadOrCreate();
        CoreyConfig.INSTANCE.validate();
        CoreyConfig.INSTANCE.save();

        registerData();
        registerEvents();

        LOGGER.info("✅ Legacy Creatures - Corey cargado exitosamente");
    }

    private void registerData() {
        ModDataAttachments.initialize();
        MutationRegistry.initialize();
        MutationDataLoader.register();
        TieredLootDataLoader.register();
        MobAttributeDataLoader.register();
        MobTierRuleDataLoader.register();
        BiomeTierWeightDataLoader.register();
        MutationRuntime.register();
        ModStatusEffects.init();
        CoreyLootModifiers.register();
    }

    private void registerEvents() {
        DifficultyTickHandler.register();
        MobSpawnHandler.register();
        TierParticleTicker.register();
        StatusEffectTicker.register();
        ModNetworking.init();
        MutationCommand.register();
        CoreyCommand.register();
        CoreyHudCommand.register();
        AntiFarmManager.register();
        SynergyManager.bootstrap();
        CoreyHealthMonitor.register();
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (!success) {
                LOGGER.warn("Recarga de datapacks fallida; se conserva la configuración actual.");
                notifyAdmins(server, Text.literal("⚠️ /reload falló; la configuración no se tocó."));
                return;
            }
            CoreyConfig.ReloadResult result = CoreyConfig.INSTANCE.reloadFromDisk();
            if (result.success()) {
                LOGGER.info("[Config] {}", result.message());
                notifyAdmins(server, Text.literal("⚙️ Configuración recargada tras /reload: " + result.message()));
            } else {
                LOGGER.warn("[Config] {}", result.message());
                notifyAdmins(server, Text.literal("⚠️ /reload no pudo actualizar la configuración: " + result.message()));
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity player) {
                DifficultyManager.applyDeathPenalty(player);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            DifficultyManager.initializePlayerDifficulty(handler.getPlayer());
        });
    }

    private static void notifyAdmins(MinecraftServer server, Text message) {
        server.getPlayerManager().getPlayerList().forEach(player -> {
            if (player.hasPermissionLevel(2)) {
                player.sendMessage(message, false);
            }
        });
    }
}