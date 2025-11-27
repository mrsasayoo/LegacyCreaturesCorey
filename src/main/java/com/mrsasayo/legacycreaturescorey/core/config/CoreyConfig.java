package com.mrsasayo.legacycreaturescorey.core.config;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.core.config.domain.antifarm_config;
import com.mrsasayo.legacycreaturescorey.core.config.domain.difficulty_config;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Fachada de compatibilidad hacia atrás para el sistema de configuración.
 * 
 * DEPRECACIÓN: Esta clase existe únicamente para mantener compatibilidad con código
 * existente. Todo código nuevo debe usar config_manager y las configs de dominio
 * (difficulty_config, antifarm_config, mutation_config, system_config).
 * 
 * Los campos públicos se sincronizan automáticamente desde las configs de dominio
 * cada vez que se recarga la configuración mediante config_manager.
 */
@Deprecated
public class CoreyConfig {
    // Singleton
    public static final CoreyConfig INSTANCE = new CoreyConfig();
    private static final Logger LOGGER = LoggerFactory.getLogger("LegacyCreaturesCoreyConfig");
    
    // ============ CONFIGURACIÓN DE DIFICULTAD ============
    // Estos campos son sincronizados desde difficulty_config
    
    public int maxGlobalDifficulty = 1000;
    public double playerDifficultyIncreaseChance = 1.0;
    public boolean enableDifficultyHud = false;
    public double biomeDifficultyMultiplier = 1.5;

    // Multiplicadores base por tier
    public double epicHealthMultiplier = 1.0;
    public double epicDamageMultiplier = 1.0;
    public double legendaryHealthMultiplier = 1.0;
    public double legendaryDamageMultiplier = 1.0;
    public double mythicHealthMultiplier = 1.0;
    public double mythicDamageMultiplier = 1.0;
    public double definitiveHealthMultiplier = 1.0;
    public double definitiveDamageMultiplier = 1.0;

    // Multiplicadores de probabilidad por tier
    public double epicChanceMultiplier = 6.0;
    public double legendaryChanceMultiplier = 12.0;
    public double mythicChanceMultiplier = 18.0;
    public double definitiveChanceMultiplier = 24.0;

    // Radio para cálculo de dificultad efectiva
    public double effectiveDifficultyRadius = 48.0;

    // Herramientas de depuración
    public boolean debugForceHighestAllowedTier = false;
    public MobTier debugForceExactTier = MobTier.DEFINITIVE;
    public boolean debugLogProbabilityDetails = false;
    public boolean debugTraceBlockBreakDenials = false;
    
    // ============ LOOT ESCALADO ==========
    public boolean tieredLootEnabled = true;
    public boolean tieredLootStrictEntityTables = true;
    public boolean tieredLootEpicEnabled = true;
    public boolean tieredLootLegendaryEnabled = true;
    public boolean tieredLootMythicEnabled = true;
    public boolean tieredLootDefinitiveEnabled = true;
    public boolean tieredLootTelemetryEnabled = false;

    // ============ SISTEMAS ANTI-ABUSO ============
    // Estos campos son sincronizados desde antifarm_config
    
    public boolean antiFarmDetectionEnabled = true;
    public int antiFarmKillThreshold = 64;
    public long antiFarmWindowTicks = 6000L;
    public int antiFarmBlockRadiusChunks = 4;
    public boolean antiFarmRestrictTieredSpawns = true;
    public boolean antiFarmLogDetections = true;
    public int antiFarmDailyDecayAmount = 64;
    public boolean antiFarmHeatPenaltyEnabled = true;
    public double antiFarmHeatPenaltyMinMultiplier = 0.25;
    public double antiFarmHeatPenaltyExponent = 1.0;
    
    // Penalización por Muerte
    public int deathPenaltyAmount = 4;
    public long deathPenaltyCooldownTicks = 1200L;
    
    // Prevenir instanciación externa
    private CoreyConfig() {}

    /**
     * Obtiene la ruta del archivo de configuración.
     * @return Path al archivo JSON de configuración
     */
    public static Path getConfigPath() {
        return FabricLoader.getInstance()
            .getConfigDir()
            .resolve(Legacycreaturescorey.MOD_ID + ".json");
    }

    /**
     * Inicializa o carga la configuración.
     * @deprecated Usar config_manager.initialize() en su lugar
     */
    @Deprecated
    public synchronized void loadOrCreate() {
        // Delegar a config_manager si ya está inicializado
        // Si no, sincronizar desde las configs de dominio
        syncFromDomainConfigs();
        LOGGER.info("[CoreyConfig] Sincronizado desde configs de dominio (fachada deprecada)");
    }

    /**
     * Recarga la configuración desde disco.
     * @deprecated Usar config_manager.reload() en su lugar
     * @return ReloadResult con el estado de la operación
     */
    @Deprecated
    public synchronized ReloadResult reloadFromDisk() {
        config_manager.reload_result result = config_manager.reload();
        syncFromDomainConfigs();
        return new ReloadResult(result.success(), result.message());
    }

    /**
     * Guarda la configuración actual.
     * @deprecated Usar config_manager.save() en su lugar
     */
    @Deprecated
    public synchronized void save() {
        config_manager.save();
    }

    /**
     * Sincroniza los campos públicos desde las configs de dominio.
     * Llamar después de cada reload de config_manager.
     */
    public void syncFromDomainConfigs() {
        // Sincronizar desde difficulty_config
        this.maxGlobalDifficulty = difficulty_config.getMaxGlobalDifficulty();
        this.playerDifficultyIncreaseChance = difficulty_config.getPlayerDifficultyIncreaseChance();
        this.enableDifficultyHud = difficulty_config.isEnableDifficultyHud();
        this.biomeDifficultyMultiplier = difficulty_config.getBiomeDifficultyMultiplier();
        this.effectiveDifficultyRadius = difficulty_config.getEffectiveDifficultyRadius();
        
        // Multiplicadores de tier
        this.epicHealthMultiplier = difficulty_config.getHealthMultiplier(MobTier.EPIC);
        this.epicDamageMultiplier = difficulty_config.getDamageMultiplier(MobTier.EPIC);
        this.legendaryHealthMultiplier = difficulty_config.getHealthMultiplier(MobTier.LEGENDARY);
        this.legendaryDamageMultiplier = difficulty_config.getDamageMultiplier(MobTier.LEGENDARY);
        this.mythicHealthMultiplier = difficulty_config.getHealthMultiplier(MobTier.MYTHIC);
        this.mythicDamageMultiplier = difficulty_config.getDamageMultiplier(MobTier.MYTHIC);
        this.definitiveHealthMultiplier = difficulty_config.getHealthMultiplier(MobTier.DEFINITIVE);
        this.definitiveDamageMultiplier = difficulty_config.getDamageMultiplier(MobTier.DEFINITIVE);
        
        // Multiplicadores de chance
        this.epicChanceMultiplier = difficulty_config.getChanceMultiplier(MobTier.EPIC);
        this.legendaryChanceMultiplier = difficulty_config.getChanceMultiplier(MobTier.LEGENDARY);
        this.mythicChanceMultiplier = difficulty_config.getChanceMultiplier(MobTier.MYTHIC);
        this.definitiveChanceMultiplier = difficulty_config.getChanceMultiplier(MobTier.DEFINITIVE);
        
        // Debug
        this.debugForceHighestAllowedTier = difficulty_config.isDebugForceHighestAllowedTier();
        this.debugForceExactTier = difficulty_config.getDebugForceExactTier();
        this.debugLogProbabilityDetails = difficulty_config.isDebugLogProbabilityDetails();
        this.debugTraceBlockBreakDenials = difficulty_config.isDebugTraceBlockBreakDenials();
        
        // Tiered Loot
        this.tieredLootEnabled = difficulty_config.isTieredLootEnabled();
        this.tieredLootStrictEntityTables = difficulty_config.isTieredLootStrictEntityTables();
        this.tieredLootEpicEnabled = difficulty_config.isTieredLootEpicEnabled();
        this.tieredLootLegendaryEnabled = difficulty_config.isTieredLootLegendaryEnabled();
        this.tieredLootMythicEnabled = difficulty_config.isTieredLootMythicEnabled();
        this.tieredLootDefinitiveEnabled = difficulty_config.isTieredLootDefinitiveEnabled();
        this.tieredLootTelemetryEnabled = difficulty_config.isTieredLootTelemetryEnabled();
        
        // Death penalty
        this.deathPenaltyAmount = difficulty_config.getDeathPenaltyAmount();
        this.deathPenaltyCooldownTicks = difficulty_config.getDeathPenaltyCooldownTicks();
        
        // Sincronizar desde antifarm_config
        this.antiFarmDetectionEnabled = antifarm_config.isDetectionEnabled();
        this.antiFarmKillThreshold = antifarm_config.getKillThreshold();
        this.antiFarmWindowTicks = antifarm_config.getWindowTicks();
        this.antiFarmBlockRadiusChunks = antifarm_config.getBlockRadiusChunks();
        this.antiFarmRestrictTieredSpawns = antifarm_config.isRestrictTieredSpawns();
        this.antiFarmLogDetections = antifarm_config.isLogDetections();
        this.antiFarmDailyDecayAmount = antifarm_config.getDailyDecayAmount();
        this.antiFarmHeatPenaltyEnabled = antifarm_config.isHeatPenaltyEnabled();
        this.antiFarmHeatPenaltyMinMultiplier = antifarm_config.getHeatPenaltyMinMultiplier();
        this.antiFarmHeatPenaltyExponent = antifarm_config.getHeatPenaltyExponent();
    }
    
    /**
     * Valida y ajusta los valores de configuración.
     * @deprecated La validación ahora se realiza en las configs de dominio
     */
    @Deprecated
    public void validate() {
        // Los valores ya vienen validados de las configs de dominio
        LOGGER.debug("[CoreyConfig] validate() llamado - delegando a configs de dominio");
    }

    /**
     * Resultado de una operación de recarga.
     */
    public record ReloadResult(boolean success, String message) {
        public static ReloadResult success(String message) {
            return new ReloadResult(true, message);
        }

        public static ReloadResult failure(String message) {
            return new ReloadResult(false, message);
        }
    }
}
