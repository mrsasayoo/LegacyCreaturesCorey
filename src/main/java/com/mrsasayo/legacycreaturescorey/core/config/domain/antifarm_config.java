package com.mrsasayo.legacycreaturescorey.core.config.domain;

import com.mrsasayo.legacycreaturescorey.core.config.dto.root_config_dto;

/**
 * Configuración del dominio Anti-Farm.
 * Maneja los campos legacy de antifarm desde el DTO.
 */
public final class antifarm_config {
    
    private static final antifarm_config INSTANCE = new antifarm_config();
    
    // ============ ANTIFARM SETTINGS ============
    private boolean detection_enabled = true;
    private int kill_threshold = 64;
    private long window_ticks = 6000L;
    private int block_radius_chunks = 4;
    private boolean restrict_tiered_spawns = true;
    private boolean log_detections = true;
    private int daily_decay_amount = 64;
    private boolean heat_penalty_enabled = true;
    private double heat_penalty_min_multiplier = 0.25;
    private double heat_penalty_exponent = 1.0;
    
    private antifarm_config() {}
    
    /**
     * Carga valores desde el DTO (campos legacy planos).
     */
    public static void loadFrom(root_config_dto dto) {
        if (dto == null) return;
        
        INSTANCE.detection_enabled = dto.antifarm_detection_enabled;
        INSTANCE.kill_threshold = dto.antifarm_kill_threshold;
        INSTANCE.window_ticks = dto.antifarm_window_ticks;
        INSTANCE.block_radius_chunks = dto.antifarm_block_radius_chunks;
        INSTANCE.restrict_tiered_spawns = dto.antifarm_restrict_tiered_spawns;
        INSTANCE.log_detections = dto.antifarm_log_detections;
        INSTANCE.daily_decay_amount = dto.antifarm_daily_decay_amount;
        INSTANCE.heat_penalty_enabled = dto.antifarm_heat_penalty_enabled;
        INSTANCE.heat_penalty_min_multiplier = dto.antifarm_heat_penalty_min_multiplier;
        INSTANCE.heat_penalty_exponent = dto.antifarm_heat_penalty_exponent;
        
        INSTANCE.validate();
    }
    
    /**
     * Sincroniza los valores actuales de vuelta al DTO.
     */
    public static void saveTo(root_config_dto dto) {
        if (dto == null) return;
        
        dto.antifarm_detection_enabled = INSTANCE.detection_enabled;
        dto.antifarm_kill_threshold = INSTANCE.kill_threshold;
        dto.antifarm_window_ticks = INSTANCE.window_ticks;
        dto.antifarm_block_radius_chunks = INSTANCE.block_radius_chunks;
        dto.antifarm_restrict_tiered_spawns = INSTANCE.restrict_tiered_spawns;
        dto.antifarm_log_detections = INSTANCE.log_detections;
        dto.antifarm_daily_decay_amount = INSTANCE.daily_decay_amount;
        dto.antifarm_heat_penalty_enabled = INSTANCE.heat_penalty_enabled;
        dto.antifarm_heat_penalty_min_multiplier = INSTANCE.heat_penalty_min_multiplier;
        dto.antifarm_heat_penalty_exponent = INSTANCE.heat_penalty_exponent;
    }
    
    // ============ GETTERS ESTÁTICOS ============
    
    public static boolean isDetectionEnabled() {
        return INSTANCE.detection_enabled;
    }
    
    public static int getKillThreshold() {
        return INSTANCE.kill_threshold;
    }
    
    public static long getWindowTicks() {
        return INSTANCE.window_ticks;
    }
    
    public static int getBlockRadiusChunks() {
        return INSTANCE.block_radius_chunks;
    }
    
    public static boolean restrictTieredSpawns() {
        return INSTANCE.restrict_tiered_spawns;
    }
    
    public static boolean isRestrictTieredSpawns() {
        return INSTANCE.restrict_tiered_spawns;
    }
    
    public static boolean logDetections() {
        return INSTANCE.log_detections;
    }
    
    public static boolean isLogDetections() {
        return INSTANCE.log_detections;
    }
    
    public static int getDailyDecayAmount() {
        return INSTANCE.daily_decay_amount;
    }
    
    public static boolean isHeatPenaltyEnabled() {
        return INSTANCE.heat_penalty_enabled;
    }
    
    public static double getHeatPenaltyMinMultiplier() {
        return INSTANCE.heat_penalty_min_multiplier;
    }
    
    public static double getHeatPenaltyExponent() {
        return INSTANCE.heat_penalty_exponent;
    }
    
    /**
     * Calcula el multiplicador de penalización por calor.
     * @param heatRatio Ratio de calor actual (0.0 = frío, 1.0 = máximo calor)
     * @return Multiplicador de spawn (1.0 = normal, heat_penalty_min_multiplier = máxima penalización)
     */
    public static double calculateHeatPenaltyMultiplier(double heatRatio) {
        if (!INSTANCE.heat_penalty_enabled || heatRatio <= 0.0) {
            return 1.0;
        }
        
        double ratio = Math.min(1.0, heatRatio);
        double penalty = Math.pow(ratio, INSTANCE.heat_penalty_exponent);
        double minMult = INSTANCE.heat_penalty_min_multiplier;
        
        // Interpolar de 1.0 a minMult basado en penalty
        return 1.0 - penalty * (1.0 - minMult);
    }
    
    // ============ VALIDACIÓN ============
    
    private void validate() {
        kill_threshold = Math.max(1, Math.min(100_000, kill_threshold));
        window_ticks = Math.max(0L, window_ticks);
        block_radius_chunks = Math.max(0, block_radius_chunks);
        daily_decay_amount = Math.max(0, Math.min(kill_threshold, daily_decay_amount));
        heat_penalty_min_multiplier = Math.max(0.0, Math.min(1.0, heat_penalty_min_multiplier));
        heat_penalty_exponent = Math.max(0.1, Math.min(8.0, heat_penalty_exponent));
    }
}
