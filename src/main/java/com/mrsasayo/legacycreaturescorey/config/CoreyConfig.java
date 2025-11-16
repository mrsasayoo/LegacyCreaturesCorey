package com.mrsasayo.legacycreaturescorey.config;

import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;

public class CoreyConfig {
    // Singleton
    public static final CoreyConfig INSTANCE = new CoreyConfig();
    
    // ============ CONFIGURACIÓN DE DIFICULTAD ============
    
    // Dificultad Global
    public int maxGlobalDifficulty = 1000;
    public double dailyIncreaseChance = 1.0; // Global difficulty increase chance per day (100%)
    public double playerDifficultyIncreaseChance = 1.0; // Chance for each player to gain difficulty when global increases
    public int dailyIncreaseAmount = 1;

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
    public double effectiveDifficultyRadius = 0.0;

    // Herramientas de depuración
    public boolean debugForceHighestAllowedTier = false;
    public MobTier debugForceExactTier = MobTier.DEFINITIVE; // Si no es null y el tier es válido, se aplica directamente "MobTier.EPIC"
    public boolean debugLogProbabilityDetails = false;
    
    // ============ SISTEMAS ANTI-ABUSO ============
    public boolean antiFarmDetectionEnabled = true;
    public int antiFarmKillThreshold = 64;
    public long antiFarmWindowTicks = 6000L; // 5 minutos aproximados
    public int antiFarmBlockRadiusChunks = 4; // 4 chunks a cada lado = 9x9
    public boolean antiFarmRestrictTieredSpawns = true;
    public boolean antiFarmLogDetections = true;
    public int antiFarmDailyDecayAmount = 64;
    
    // Penalización por Muerte
    public int deathPenaltyAmount = 4;
    public long deathPenaltyCooldownTicks = 1200L; // Ventana para detectar muertes rápidas (~1 minuto)
    
    // Prevenir instanciación externa
    private CoreyConfig() {}
    
    // Método para validar valores
    public void validate() {
        if (maxGlobalDifficulty < 0) maxGlobalDifficulty = 1000;
    if (dailyIncreaseChance < 0.0 || dailyIncreaseChance > 1.0) dailyIncreaseChance = 1.0;
    if (playerDifficultyIncreaseChance < 0.0 || playerDifficultyIncreaseChance > 1.0) playerDifficultyIncreaseChance = 1.0;
    if (deathPenaltyAmount < 0) deathPenaltyAmount = 8;
    if (deathPenaltyCooldownTicks < 0) deathPenaltyCooldownTicks = 1200L;
    if (epicHealthMultiplier < 1.0) epicHealthMultiplier = 1.6;
    if (epicDamageMultiplier < 1.0) epicDamageMultiplier = 1.0;
    if (legendaryHealthMultiplier < epicHealthMultiplier) legendaryHealthMultiplier = 2.0;
    if (legendaryDamageMultiplier < epicDamageMultiplier) legendaryDamageMultiplier = 1.0;
    if (mythicHealthMultiplier < legendaryHealthMultiplier) mythicHealthMultiplier = 2.6;
    if (mythicDamageMultiplier < legendaryDamageMultiplier) mythicDamageMultiplier = 1.0;
    if (definitiveHealthMultiplier < mythicHealthMultiplier) definitiveHealthMultiplier = 3.2;
    if (definitiveDamageMultiplier < mythicDamageMultiplier) definitiveDamageMultiplier = 1.0;
        if (epicChanceMultiplier <= 0.0) epicChanceMultiplier = 1.0;
        if (legendaryChanceMultiplier <= 0.0) legendaryChanceMultiplier = 1.0;
        if (mythicChanceMultiplier <= 0.0) mythicChanceMultiplier = 1.0;
        if (definitiveChanceMultiplier <= 0.0) definitiveChanceMultiplier = 1.0;
        if (effectiveDifficultyRadius < 0.0) effectiveDifficultyRadius = 64.0;
        if (debugForceExactTier == MobTier.NORMAL) {
            debugForceExactTier = null; // Normal no aporta para depuración forzada
        }
        if (antiFarmKillThreshold < 1) antiFarmKillThreshold = 64;
        if (antiFarmWindowTicks < 0L) antiFarmWindowTicks = 6000L;
        if (antiFarmBlockRadiusChunks < 0) antiFarmBlockRadiusChunks = 0;
        if (antiFarmDailyDecayAmount < 0) antiFarmDailyDecayAmount = 64;
    }
}