package com.mrsasayo.legacycreaturescorey.core.config.domain;

import com.mrsasayo.legacycreaturescorey.core.config.dto.root_config_dto;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

/**
 * Configuración del dominio de Mutaciones.
 * Maneja: mutation_system, category_toggle, cost_system
 * 
 * Acceso estático para uso rápido en el runtime del sistema de mutaciones.
 */
public final class mutation_config {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("MutationConfig");
    private static final mutation_config INSTANCE = new mutation_config();
    
    // ============ MUTATION SYSTEM ============
    private int max_mutations_per_mob = 3;
    private int mutation_point_budget = 100;
    private boolean allow_incompatible_mutations = false;
    private boolean enable_debug_logging = false;
    
    // ============ CATEGORY TOGGLE ============
    private final Map<mutation_category, Boolean> category_toggle = new EnumMap<>(mutation_category.class);
    
    // ============ COST SYSTEM ============
    private double general_cost_reduction = 0.0;
    @Nullable private Integer standardized_cost_weak = null;
    @Nullable private Integer standardized_cost_intermediate = null;
    @Nullable private Integer standardized_cost_strong = null;
    
    private mutation_config() {
        // Inicializar todas las categorías activas por defecto
        for (mutation_category cat : mutation_category.values()) {
            category_toggle.put(cat, true);
        }
    }
    
    /**
     * Carga valores desde el DTO.
     */
    public static void loadFrom(root_config_dto dto) {
        if (dto == null) return;
        
        // Mutation System
        if (dto.mutation_system != null) {
            INSTANCE.max_mutations_per_mob = dto.mutation_system.max_mutations_per_mob;
            INSTANCE.mutation_point_budget = dto.mutation_system.mutation_point_budget;
            INSTANCE.allow_incompatible_mutations = dto.mutation_system.allow_incompatible_mutations;
            INSTANCE.enable_debug_logging = dto.mutation_system.enable_debug_logging;
        }
        
        // Category Toggle
        if (dto.category_toggle != null) {
            INSTANCE.category_toggle.put(mutation_category.PASSIVE, dto.category_toggle.passive);
            INSTANCE.category_toggle.put(mutation_category.ON_HIT, dto.category_toggle.on_hit);
            INSTANCE.category_toggle.put(mutation_category.MOB_EXCLUSIVE, dto.category_toggle.mob_exclusive);
            INSTANCE.category_toggle.put(mutation_category.AURAS, dto.category_toggle.auras);
            INSTANCE.category_toggle.put(mutation_category.ON_BEING_HIT, dto.category_toggle.on_being_hit);
            INSTANCE.category_toggle.put(mutation_category.ON_DEATH, dto.category_toggle.on_death);
            INSTANCE.category_toggle.put(mutation_category.SYNERGY, dto.category_toggle.synergy);
            INSTANCE.category_toggle.put(mutation_category.TERRAIN, dto.category_toggle.terrain);
        }
        
        // Cost System
        if (dto.cost_system != null) {
            INSTANCE.general_cost_reduction = dto.cost_system.general_cost_reduction;
            if (dto.cost_system.standardized_cost_difficulty != null) {
                INSTANCE.standardized_cost_weak = dto.cost_system.standardized_cost_difficulty.weak;
                INSTANCE.standardized_cost_intermediate = dto.cost_system.standardized_cost_difficulty.intermediate;
                INSTANCE.standardized_cost_strong = dto.cost_system.standardized_cost_difficulty.strong;
            }
        }
        
        INSTANCE.validate();
        
        if (INSTANCE.enable_debug_logging) {
            INSTANCE.logConfig();
        }
    }
    
    /**
     * Sincroniza los valores actuales de vuelta al DTO para guardado.
     */
    public static void saveTo(root_config_dto dto) {
        if (dto == null) return;
        
        // Mutation System
        if (dto.mutation_system == null) {
            dto.mutation_system = new root_config_dto.mutation_system_dto();
        }
        dto.mutation_system.max_mutations_per_mob = INSTANCE.max_mutations_per_mob;
        dto.mutation_system.mutation_point_budget = INSTANCE.mutation_point_budget;
        dto.mutation_system.allow_incompatible_mutations = INSTANCE.allow_incompatible_mutations;
        dto.mutation_system.enable_debug_logging = INSTANCE.enable_debug_logging;
        
        // Category Toggle
        if (dto.category_toggle == null) {
            dto.category_toggle = new root_config_dto.category_toggle_dto();
        }
        dto.category_toggle.passive = INSTANCE.category_toggle.getOrDefault(mutation_category.PASSIVE, true);
        dto.category_toggle.on_hit = INSTANCE.category_toggle.getOrDefault(mutation_category.ON_HIT, true);
        dto.category_toggle.mob_exclusive = INSTANCE.category_toggle.getOrDefault(mutation_category.MOB_EXCLUSIVE, true);
        dto.category_toggle.auras = INSTANCE.category_toggle.getOrDefault(mutation_category.AURAS, true);
        dto.category_toggle.on_being_hit = INSTANCE.category_toggle.getOrDefault(mutation_category.ON_BEING_HIT, true);
        dto.category_toggle.on_death = INSTANCE.category_toggle.getOrDefault(mutation_category.ON_DEATH, true);
        dto.category_toggle.synergy = INSTANCE.category_toggle.getOrDefault(mutation_category.SYNERGY, true);
        dto.category_toggle.terrain = INSTANCE.category_toggle.getOrDefault(mutation_category.TERRAIN, true);
        
        // Cost System
        if (dto.cost_system == null) {
            dto.cost_system = new root_config_dto.cost_system_dto();
        }
        dto.cost_system.general_cost_reduction = INSTANCE.general_cost_reduction;
        if (dto.cost_system.standardized_cost_difficulty == null) {
            dto.cost_system.standardized_cost_difficulty = new root_config_dto.cost_system_dto.standardized_cost_dto();
        }
        dto.cost_system.standardized_cost_difficulty.weak = INSTANCE.standardized_cost_weak;
        dto.cost_system.standardized_cost_difficulty.intermediate = INSTANCE.standardized_cost_intermediate;
        dto.cost_system.standardized_cost_difficulty.strong = INSTANCE.standardized_cost_strong;
    }
    
    // ============ GETTERS ESTÁTICOS ============
    
    public static int getMaxMutationsPerMob() {
        return INSTANCE.max_mutations_per_mob;
    }
    
    public static int getMutationPointBudget() {
        return INSTANCE.mutation_point_budget;
    }
    
    public static boolean allowIncompatibleMutations() {
        return INSTANCE.allow_incompatible_mutations;
    }
    
    public static boolean isDebugLoggingEnabled() {
        return INSTANCE.enable_debug_logging;
    }
    
    public static boolean isCategoryEnabled(mutation_category category) {
        return INSTANCE.category_toggle.getOrDefault(category, true);
    }
    
    public static boolean isCategoryEnabled(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) return true;
        try {
            mutation_category cat = mutation_category.valueOf(categoryName.toUpperCase());
            return isCategoryEnabled(cat);
        } catch (IllegalArgumentException e) {
            return true; // Categoría desconocida = habilitada
        }
    }
    
    public static double getGeneralCostReduction() {
        return INSTANCE.general_cost_reduction;
    }
    
    @Nullable
    public static Integer getStandardizedCostWeak() {
        return INSTANCE.standardized_cost_weak;
    }
    
    @Nullable
    public static Integer getStandardizedCostIntermediate() {
        return INSTANCE.standardized_cost_intermediate;
    }
    
    @Nullable
    public static Integer getStandardizedCostStrong() {
        return INSTANCE.standardized_cost_strong;
    }
    
    /**
     * Obtiene el costo estandarizado para una dificultad específica.
     * @return null si se debe usar el costo original
     */
    @Nullable
    public static Integer getStandardizedCostForDifficulty(String difficulty) {
        if (difficulty == null) return null;
        return switch (difficulty.toLowerCase()) {
            case "weak", "débil" -> INSTANCE.standardized_cost_weak;
            case "intermediate", "intermedio" -> INSTANCE.standardized_cost_intermediate;
            case "strong", "fuerte" -> INSTANCE.standardized_cost_strong;
            default -> null;
        };
    }
    
    /**
     * Calcula el costo efectivo de una mutación aplicando reducciones.
     */
    public static int calculateEffectiveCost(int baseCost, @Nullable String difficulty) {
        // Primero verificar costo estandarizado
        Integer standardized = getStandardizedCostForDifficulty(difficulty);
        if (standardized != null) {
            return Math.max(0, standardized);
        }
        
        // Aplicar reducción general
        double reduction = INSTANCE.general_cost_reduction;
        if (reduction <= 0.0) {
            return baseCost;
        }
        
        double effectiveCost = baseCost * (1.0 - reduction);
        return Math.max(0, (int) Math.round(effectiveCost));
    }
    
    // ============ VALIDACIÓN ============
    
    private void validate() {
        max_mutations_per_mob = Math.max(1, Math.min(10, max_mutations_per_mob));
        mutation_point_budget = Math.max(1, Math.min(10000, mutation_point_budget));
        general_cost_reduction = Math.max(0.0, Math.min(1.0, general_cost_reduction));
        
        if (standardized_cost_weak != null) {
            standardized_cost_weak = Math.max(0, standardized_cost_weak);
        }
        if (standardized_cost_intermediate != null) {
            standardized_cost_intermediate = Math.max(0, standardized_cost_intermediate);
        }
        if (standardized_cost_strong != null) {
            standardized_cost_strong = Math.max(0, standardized_cost_strong);
        }
    }
    
    private void logConfig() {
        LOGGER.info("=== Mutation Config Loaded ===");
        LOGGER.info("  max_mutations_per_mob: {}", max_mutations_per_mob);
        LOGGER.info("  mutation_point_budget: {}", mutation_point_budget);
        LOGGER.info("  allow_incompatible: {}", allow_incompatible_mutations);
        LOGGER.info("  general_cost_reduction: {}", general_cost_reduction);
        
        StringBuilder cats = new StringBuilder("  Categories: ");
        for (mutation_category cat : mutation_category.values()) {
            cats.append(cat.name().toLowerCase())
                .append("=")
                .append(category_toggle.get(cat))
                .append(" ");
        }
        LOGGER.info(cats.toString());
    }
    
    // ============ ENUM DE CATEGORÍAS ============
    
    public enum mutation_category {
        PASSIVE,
        ON_HIT,
        MOB_EXCLUSIVE,
        AURAS,
        ON_BEING_HIT,
        ON_DEATH,
        SYNERGY,
        TERRAIN
    }
}
