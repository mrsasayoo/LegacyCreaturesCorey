package com.mrsasayo.legacycreaturescorey.core.config.domain;

import com.mrsasayo.legacycreaturescorey.core.config.dto.root_config_dto;

/**
 * Configuración del dominio de Sistema/Performance.
 * Maneja: performance, weighting_system
 */
public final class system_config {
    
    private static final system_config INSTANCE = new system_config();
    
    // ============ PERFORMANCE ============
    private int passive_tick_interval = 20;
    private int aura_check_interval = 20;
    
    // ============ WEIGHTING SYSTEM ============
    private double weighting_weight = 0.0; // 0 = pesos originales, 1 = uniforme
    
    private system_config() {}
    
    /**
     * Carga valores desde el DTO.
     */
    public static void loadFrom(root_config_dto dto) {
        if (dto == null) return;
        
        // Performance
        if (dto.performance != null) {
            INSTANCE.passive_tick_interval = dto.performance.passive_tick_interval;
            INSTANCE.aura_check_interval = dto.performance.aura_check_interval;
        }
        
        // Weighting System
        if (dto.weighting_system != null) {
            INSTANCE.weighting_weight = dto.weighting_system.weighting_weight;
        }
        
        INSTANCE.validate();
    }
    
    /**
     * Sincroniza los valores actuales de vuelta al DTO.
     */
    public static void saveTo(root_config_dto dto) {
        if (dto == null) return;
        
        // Performance
        if (dto.performance == null) {
            dto.performance = new root_config_dto.performance_dto();
        }
        dto.performance.passive_tick_interval = INSTANCE.passive_tick_interval;
        dto.performance.aura_check_interval = INSTANCE.aura_check_interval;
        
        // Weighting System
        if (dto.weighting_system == null) {
            dto.weighting_system = new root_config_dto.weighting_system_dto();
        }
        dto.weighting_system.weighting_weight = INSTANCE.weighting_weight;
    }
    
    // ============ GETTERS ESTÁTICOS ============
    
    /**
     * Intervalo en ticks entre ejecuciones de efectos pasivos.
     * Mínimo 1 tick.
     */
    public static int getPassiveTickInterval() {
        return Math.max(1, INSTANCE.passive_tick_interval);
    }
    
    /**
     * Intervalo en ticks entre verificaciones de auras.
     * Mínimo 1 tick.
     */
    public static int getAuraCheckInterval() {
        return Math.max(1, INSTANCE.aura_check_interval);
    }
    
    /**
     * Peso de uniformidad para el sistema de pesos.
     * 0.0 = Pesos originales del CSV
     * 1.0 = Probabilidad completamente uniforme
     */
    public static double getWeightingWeight() {
        return INSTANCE.weighting_weight;
    }
    
    /**
     * Calcula el peso efectivo interpolando entre el peso original y 1.0 (uniforme).
     * 
     * @param originalWeight Peso original de la mutación
     * @param totalMutations Total de mutaciones en el pool
     * @return Peso efectivo después de aplicar weighting_weight
     */
    public static double calculateEffectiveWeight(double originalWeight, int totalMutations) {
        if (totalMutations <= 0) return originalWeight;
        
        double uniformWeight = 1.0 / totalMutations;
        double w = INSTANCE.weighting_weight;
        
        // Interpolación lineal: (1 - w) * original + w * uniform
        return (1.0 - w) * originalWeight + w * uniformWeight;
    }
    
    // ============ VALIDACIÓN ============
    
    private void validate() {
        passive_tick_interval = Math.max(1, Math.min(1200, passive_tick_interval));
        aura_check_interval = Math.max(1, Math.min(1200, aura_check_interval));
        weighting_weight = Math.max(0.0, Math.min(1.0, weighting_weight));
    }
}
