package com.mrsasayo.legacycreaturescorey.config;

public class CoreyConfig {
    // Singleton
    public static final CoreyConfig INSTANCE = new CoreyConfig();
    
    // ============ CONFIGURACIÓN DE DIFICULTAD ============
    
    // Dificultad Global
    public int maxGlobalDifficulty = 1000;
    public double dailyIncreaseChance = 0.5; // 50%
    public int dailyIncreaseAmount = 1;
    
    // Penalización por Muerte
    public int deathPenaltyAmount = 8;
    public long deathPenaltyCooldownTicks = 12000L; // 10 minutos (12000 ticks)
    
    // Prevenir instanciación externa
    private CoreyConfig() {}
    
    // Método para validar valores
    public void validate() {
        if (maxGlobalDifficulty < 0) maxGlobalDifficulty = 1000;
        if (dailyIncreaseChance < 0.0 || dailyIncreaseChance > 1.0) dailyIncreaseChance = 0.5;
        if (deathPenaltyAmount < 0) deathPenaltyAmount = 8;
        if (deathPenaltyCooldownTicks < 0) deathPenaltyCooldownTicks = 12000L;
    }
}