package com.mrsasayo.legacycreaturescorey.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class CoreyConfig {
    // Singleton
    public static final CoreyConfig INSTANCE = new CoreyConfig();
    private static final Logger LOGGER = LoggerFactory.getLogger("LegacyCreaturesCoreyConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // ============ CONFIGURACIÓN DE DIFICULTAD ============
    
    // Dificultad Global
    public int maxGlobalDifficulty = 1000;
    public double playerDifficultyIncreaseChance = 1.0; // Chance for players to gain personal difficulty once per día
    public boolean enableDifficultyHud = false; // Muestra HUD de dificultad en clientes si está activo
    public double biomeDifficultyMultiplier = 1.5; // Bonus aplicado en biomas especiales

    // Multiplicadores base por tier (solo salud por defecto)
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

    // Radio para cálculo de dificultad efectiva (0 = sin límite)
    public double effectiveDifficultyRadius = 48.0;

    // Herramientas de depuración
    public boolean debugForceHighestAllowedTier = false;
    public MobTier debugForceExactTier = MobTier.DEFINITIVE; // Si no es null y el tier es válido, se aplica directamente "MobTier.EPIC"
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
    public boolean antiFarmDetectionEnabled = true;
    public int antiFarmKillThreshold = 64;
    public long antiFarmWindowTicks = 6000L; // 5 minutos aproximados
    public int antiFarmBlockRadiusChunks = 4; // 4 chunks a cada lado = 9x9
    public boolean antiFarmRestrictTieredSpawns = true;
    public boolean antiFarmLogDetections = true;
    public int antiFarmDailyDecayAmount = 64;
    public boolean antiFarmHeatPenaltyEnabled = true;
    public double antiFarmHeatPenaltyMinMultiplier = 0.25;
    public double antiFarmHeatPenaltyExponent = 1.0;
    
    // Penalización por Muerte
    public int deathPenaltyAmount = 4;
    public long deathPenaltyCooldownTicks = 1200L; // Ventana para detectar muertes rápidas (~1 minuto)
    
    // Prevenir instanciación externa
    private CoreyConfig() {}

    public static Path getConfigPath() {
        return FabricLoader.getInstance()
            .getConfigDir()
            .resolve(Legacycreaturescorey.MOD_ID + ".json");
    }

    public synchronized void loadOrCreate() {
        Path configPath = getConfigPath();
        if (!loadFromDisk(configPath)) {
            saveInternal(configPath);
        }
    }

    public synchronized ReloadResult reloadFromDisk() {
        Path configPath = getConfigPath();
        if (!loadFromDisk(configPath)) {
            saveInternal(configPath);
            return ReloadResult.failure("No existía configuración. Se generó una nueva en " + configPath.getFileName());
        }
        validate();
        saveInternal(configPath);
        return ReloadResult.success("Configuración recargada desde " + configPath.getFileName());
    }

    public synchronized void save() {
        saveInternal(getConfigPath());
    }

    private void saveInternal(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException ex) {
            LOGGER.error("No se pudo escribir la configuración en {}", configPath, ex);
        }
    }

    private boolean loadFromDisk(Path configPath) {
        if (Files.notExists(configPath)) {
            return false;
        }
        try (Reader reader = Files.newBufferedReader(configPath)) {
            CoreyConfig loaded = GSON.fromJson(reader, CoreyConfig.class);
            if (loaded != null) {
                copyFrom(loaded);
                return true;
            }
            LOGGER.warn("El archivo de configuración está vacío: {}", configPath);
        } catch (IOException | JsonParseException ex) {
            LOGGER.error("No se pudo leer la configuración en {}", configPath, ex);
        }
        return false;
    }

    private void copyFrom(CoreyConfig other) {
        this.maxGlobalDifficulty = other.maxGlobalDifficulty;
        this.playerDifficultyIncreaseChance = other.playerDifficultyIncreaseChance;
    this.enableDifficultyHud = other.enableDifficultyHud;
    this.biomeDifficultyMultiplier = other.biomeDifficultyMultiplier;
        this.epicHealthMultiplier = other.epicHealthMultiplier;
        this.epicDamageMultiplier = other.epicDamageMultiplier;
        this.legendaryHealthMultiplier = other.legendaryHealthMultiplier;
        this.legendaryDamageMultiplier = other.legendaryDamageMultiplier;
        this.mythicHealthMultiplier = other.mythicHealthMultiplier;
        this.mythicDamageMultiplier = other.mythicDamageMultiplier;
        this.definitiveHealthMultiplier = other.definitiveHealthMultiplier;
        this.definitiveDamageMultiplier = other.definitiveDamageMultiplier;
        this.epicChanceMultiplier = other.epicChanceMultiplier;
        this.legendaryChanceMultiplier = other.legendaryChanceMultiplier;
        this.mythicChanceMultiplier = other.mythicChanceMultiplier;
        this.definitiveChanceMultiplier = other.definitiveChanceMultiplier;
        this.effectiveDifficultyRadius = other.effectiveDifficultyRadius;
        this.debugForceHighestAllowedTier = other.debugForceHighestAllowedTier;
        this.debugForceExactTier = other.debugForceExactTier;
        this.debugLogProbabilityDetails = other.debugLogProbabilityDetails;
        this.debugTraceBlockBreakDenials = other.debugTraceBlockBreakDenials;
    this.tieredLootEnabled = other.tieredLootEnabled;
    this.tieredLootStrictEntityTables = other.tieredLootStrictEntityTables;
    this.tieredLootEpicEnabled = other.tieredLootEpicEnabled;
    this.tieredLootLegendaryEnabled = other.tieredLootLegendaryEnabled;
    this.tieredLootMythicEnabled = other.tieredLootMythicEnabled;
    this.tieredLootDefinitiveEnabled = other.tieredLootDefinitiveEnabled;
    this.tieredLootTelemetryEnabled = other.tieredLootTelemetryEnabled;
        this.antiFarmDetectionEnabled = other.antiFarmDetectionEnabled;
        this.antiFarmKillThreshold = other.antiFarmKillThreshold;
        this.antiFarmWindowTicks = other.antiFarmWindowTicks;
        this.antiFarmBlockRadiusChunks = other.antiFarmBlockRadiusChunks;
        this.antiFarmRestrictTieredSpawns = other.antiFarmRestrictTieredSpawns;
        this.antiFarmLogDetections = other.antiFarmLogDetections;
        this.antiFarmDailyDecayAmount = other.antiFarmDailyDecayAmount;
        this.antiFarmHeatPenaltyEnabled = other.antiFarmHeatPenaltyEnabled;
        this.antiFarmHeatPenaltyMinMultiplier = other.antiFarmHeatPenaltyMinMultiplier;
        this.antiFarmHeatPenaltyExponent = other.antiFarmHeatPenaltyExponent;
        this.deathPenaltyAmount = other.deathPenaltyAmount;
        this.deathPenaltyCooldownTicks = other.deathPenaltyCooldownTicks;
    }
    
    // Método para validar valores
    public void validate() {
        maxGlobalDifficulty = clampInt(maxGlobalDifficulty, 1, 100_000);
        playerDifficultyIncreaseChance = clamp01(playerDifficultyIncreaseChance);
        deathPenaltyAmount = Math.max(deathPenaltyAmount, 0);
        deathPenaltyCooldownTicks = Math.max(deathPenaltyCooldownTicks, 0L);

        epicHealthMultiplier = clampMultiplier(epicHealthMultiplier);
        epicDamageMultiplier = clampMultiplier(epicDamageMultiplier);

        legendaryHealthMultiplier = ensureTierOrder(legendaryHealthMultiplier, epicHealthMultiplier);
        legendaryDamageMultiplier = ensureTierOrder(legendaryDamageMultiplier, epicDamageMultiplier);

        mythicHealthMultiplier = ensureTierOrder(mythicHealthMultiplier, legendaryHealthMultiplier);
        mythicDamageMultiplier = ensureTierOrder(mythicDamageMultiplier, legendaryDamageMultiplier);

        definitiveHealthMultiplier = ensureTierOrder(definitiveHealthMultiplier, mythicHealthMultiplier);
        definitiveDamageMultiplier = ensureTierOrder(definitiveDamageMultiplier, mythicDamageMultiplier);

        epicChanceMultiplier = clampChanceMultiplier(epicChanceMultiplier);
        legendaryChanceMultiplier = clampChanceMultiplier(legendaryChanceMultiplier);
        mythicChanceMultiplier = clampChanceMultiplier(mythicChanceMultiplier);
        definitiveChanceMultiplier = clampChanceMultiplier(definitiveChanceMultiplier);

        effectiveDifficultyRadius = Math.max(effectiveDifficultyRadius, 0.0);
        if (debugForceExactTier == MobTier.NORMAL) {
            debugForceExactTier = null; // Normal no aporta para depuración forzada
        }
        antiFarmKillThreshold = clampInt(antiFarmKillThreshold, 1, 100_000);
        antiFarmWindowTicks = Math.max(antiFarmWindowTicks, 0L);
        antiFarmBlockRadiusChunks = Math.max(antiFarmBlockRadiusChunks, 0);
        antiFarmDailyDecayAmount = Math.max(antiFarmDailyDecayAmount, 0);
        antiFarmDailyDecayAmount = Math.min(antiFarmDailyDecayAmount, antiFarmKillThreshold);
        antiFarmHeatPenaltyMinMultiplier = clamp01(antiFarmHeatPenaltyMinMultiplier);
        antiFarmHeatPenaltyExponent = clampPositive(antiFarmHeatPenaltyExponent, 0.1, 8.0);
    }

    public record ReloadResult(boolean success, String message) {
        public static ReloadResult success(String message) {
            return new ReloadResult(true, message);
        }

        public static ReloadResult failure(String message) {
            return new ReloadResult(false, message);
        }
    }

    private static double ensureTierOrder(double value, double previousTierMinimum) {
        return Math.max(clampMultiplier(value), previousTierMinimum);
    }

    private static double clamp01(double value) {
        if (Double.isNaN(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double clampMultiplier(double value) {
        if (!Double.isFinite(value)) {
            return 1.0;
        }
        return Math.max(0.1, Math.min(32.0, value));
    }

    private static double clampChanceMultiplier(double value) {
        if (!Double.isFinite(value)) {
            return 1.0;
        }
        return Math.max(0.01, Math.min(64.0, value));
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampPositive(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        double clampedMin = Math.max(0.0, min);
        double upper = max <= 0.0 ? Double.MAX_VALUE : max;
        return Math.max(clampedMin, Math.min(upper, value));
    }
}