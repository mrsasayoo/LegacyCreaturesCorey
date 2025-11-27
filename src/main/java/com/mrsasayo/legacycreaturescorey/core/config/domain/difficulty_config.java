package com.mrsasayo.legacycreaturescorey.core.config.domain;

import com.mrsasayo.legacycreaturescorey.core.config.dto.root_config_dto;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import org.jetbrains.annotations.Nullable;

/**
 * Configuración del dominio de Dificultad y Tiers.
 * Maneja los campos legacy de dificultad, multiplicadores de tier, loot, etc.
 */
public final class difficulty_config {
    
    private static final difficulty_config INSTANCE = new difficulty_config();
    
    // ============ DIFICULTAD GLOBAL ============
    private int max_global_difficulty = 1000;
    private double player_difficulty_increase_chance = 1.0;
    private boolean enable_difficulty_hud = false;
    private double biome_difficulty_multiplier = 1.5;
    
    // ============ MULTIPLICADORES POR TIER ============
    private double epic_health_multiplier = 1.0;
    private double epic_damage_multiplier = 1.0;
    private double legendary_health_multiplier = 1.0;
    private double legendary_damage_multiplier = 1.0;
    private double mythic_health_multiplier = 1.0;
    private double mythic_damage_multiplier = 1.0;
    private double definitive_health_multiplier = 1.0;
    private double definitive_damage_multiplier = 1.0;
    
    // ============ MULTIPLICADORES DE PROBABILIDAD ============
    private double epic_chance_multiplier = 6.0;
    private double legendary_chance_multiplier = 12.0;
    private double mythic_chance_multiplier = 18.0;
    private double definitive_chance_multiplier = 24.0;
    
    private double effective_difficulty_radius = 48.0;
    
    // ============ DEBUG ============
    private boolean debug_force_highest_allowed_tier = false;
    @Nullable private MobTier debug_force_exact_tier = null;
    private boolean debug_log_probability_details = false;
    private boolean debug_trace_block_break_denials = false;
    
    // ============ TIERED LOOT ============
    private boolean tiered_loot_enabled = true;
    private boolean tiered_loot_strict_entity_tables = true;
    private boolean tiered_loot_epic_enabled = true;
    private boolean tiered_loot_legendary_enabled = true;
    private boolean tiered_loot_mythic_enabled = true;
    private boolean tiered_loot_definitive_enabled = true;
    private boolean tiered_loot_telemetry_enabled = false;
    
    // ============ DEATH PENALTY ============
    private int death_penalty_amount = 4;
    private long death_penalty_cooldown_ticks = 1200L;
    
    private difficulty_config() {}
    
    /**
     * Carga valores desde el DTO.
     */
    public static void loadFrom(root_config_dto dto) {
        if (dto == null) return;
        
        // Dificultad Global
        INSTANCE.max_global_difficulty = dto.max_global_difficulty;
        INSTANCE.player_difficulty_increase_chance = dto.player_difficulty_increase_chance;
        INSTANCE.enable_difficulty_hud = dto.enable_difficulty_hud;
        INSTANCE.biome_difficulty_multiplier = dto.biome_difficulty_multiplier;
        
        // Multiplicadores de Tier
        INSTANCE.epic_health_multiplier = dto.epic_health_multiplier;
        INSTANCE.epic_damage_multiplier = dto.epic_damage_multiplier;
        INSTANCE.legendary_health_multiplier = dto.legendary_health_multiplier;
        INSTANCE.legendary_damage_multiplier = dto.legendary_damage_multiplier;
        INSTANCE.mythic_health_multiplier = dto.mythic_health_multiplier;
        INSTANCE.mythic_damage_multiplier = dto.mythic_damage_multiplier;
        INSTANCE.definitive_health_multiplier = dto.definitive_health_multiplier;
        INSTANCE.definitive_damage_multiplier = dto.definitive_damage_multiplier;
        
        // Multiplicadores de Probabilidad
        INSTANCE.epic_chance_multiplier = dto.epic_chance_multiplier;
        INSTANCE.legendary_chance_multiplier = dto.legendary_chance_multiplier;
        INSTANCE.mythic_chance_multiplier = dto.mythic_chance_multiplier;
        INSTANCE.definitive_chance_multiplier = dto.definitive_chance_multiplier;
        
        INSTANCE.effective_difficulty_radius = dto.effective_difficulty_radius;
        
        // Debug
        INSTANCE.debug_force_highest_allowed_tier = dto.debug_force_highest_allowed_tier;
        INSTANCE.debug_force_exact_tier = parseTier(dto.debug_force_exact_tier);
        INSTANCE.debug_log_probability_details = dto.debug_log_probability_details;
        INSTANCE.debug_trace_block_break_denials = dto.debug_trace_block_break_denials;
        
        // Tiered Loot
        INSTANCE.tiered_loot_enabled = dto.tiered_loot_enabled;
        INSTANCE.tiered_loot_strict_entity_tables = dto.tiered_loot_strict_entity_tables;
        INSTANCE.tiered_loot_epic_enabled = dto.tiered_loot_epic_enabled;
        INSTANCE.tiered_loot_legendary_enabled = dto.tiered_loot_legendary_enabled;
        INSTANCE.tiered_loot_mythic_enabled = dto.tiered_loot_mythic_enabled;
        INSTANCE.tiered_loot_definitive_enabled = dto.tiered_loot_definitive_enabled;
        INSTANCE.tiered_loot_telemetry_enabled = dto.tiered_loot_telemetry_enabled;
        
        // Death Penalty
        INSTANCE.death_penalty_amount = dto.death_penalty_amount;
        INSTANCE.death_penalty_cooldown_ticks = dto.death_penalty_cooldown_ticks;
        
        INSTANCE.validate();
    }
    
    /**
     * Sincroniza los valores actuales de vuelta al DTO.
     */
    public static void saveTo(root_config_dto dto) {
        if (dto == null) return;
        
        // Dificultad Global
        dto.max_global_difficulty = INSTANCE.max_global_difficulty;
        dto.player_difficulty_increase_chance = INSTANCE.player_difficulty_increase_chance;
        dto.enable_difficulty_hud = INSTANCE.enable_difficulty_hud;
        dto.biome_difficulty_multiplier = INSTANCE.biome_difficulty_multiplier;
        
        // Multiplicadores de Tier
        dto.epic_health_multiplier = INSTANCE.epic_health_multiplier;
        dto.epic_damage_multiplier = INSTANCE.epic_damage_multiplier;
        dto.legendary_health_multiplier = INSTANCE.legendary_health_multiplier;
        dto.legendary_damage_multiplier = INSTANCE.legendary_damage_multiplier;
        dto.mythic_health_multiplier = INSTANCE.mythic_health_multiplier;
        dto.mythic_damage_multiplier = INSTANCE.mythic_damage_multiplier;
        dto.definitive_health_multiplier = INSTANCE.definitive_health_multiplier;
        dto.definitive_damage_multiplier = INSTANCE.definitive_damage_multiplier;
        
        // Multiplicadores de Probabilidad
        dto.epic_chance_multiplier = INSTANCE.epic_chance_multiplier;
        dto.legendary_chance_multiplier = INSTANCE.legendary_chance_multiplier;
        dto.mythic_chance_multiplier = INSTANCE.mythic_chance_multiplier;
        dto.definitive_chance_multiplier = INSTANCE.definitive_chance_multiplier;
        
        dto.effective_difficulty_radius = INSTANCE.effective_difficulty_radius;
        
        // Debug
        dto.debug_force_highest_allowed_tier = INSTANCE.debug_force_highest_allowed_tier;
        dto.debug_force_exact_tier = INSTANCE.debug_force_exact_tier != null ? 
            INSTANCE.debug_force_exact_tier.name() : null;
        dto.debug_log_probability_details = INSTANCE.debug_log_probability_details;
        dto.debug_trace_block_break_denials = INSTANCE.debug_trace_block_break_denials;
        
        // Tiered Loot
        dto.tiered_loot_enabled = INSTANCE.tiered_loot_enabled;
        dto.tiered_loot_strict_entity_tables = INSTANCE.tiered_loot_strict_entity_tables;
        dto.tiered_loot_epic_enabled = INSTANCE.tiered_loot_epic_enabled;
        dto.tiered_loot_legendary_enabled = INSTANCE.tiered_loot_legendary_enabled;
        dto.tiered_loot_mythic_enabled = INSTANCE.tiered_loot_mythic_enabled;
        dto.tiered_loot_definitive_enabled = INSTANCE.tiered_loot_definitive_enabled;
        dto.tiered_loot_telemetry_enabled = INSTANCE.tiered_loot_telemetry_enabled;
        
        // Death Penalty
        dto.death_penalty_amount = INSTANCE.death_penalty_amount;
        dto.death_penalty_cooldown_ticks = INSTANCE.death_penalty_cooldown_ticks;
    }
    
    @Nullable
    private static MobTier parseTier(@Nullable String tierName) {
        if (tierName == null || tierName.isEmpty()) return null;
        try {
            MobTier tier = MobTier.valueOf(tierName.toUpperCase());
            return tier == MobTier.NORMAL ? null : tier;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    // ============ GETTERS ESTÁTICOS - DIFICULTAD ============
    
    public static int getMaxGlobalDifficulty() {
        return INSTANCE.max_global_difficulty;
    }
    
    public static double getPlayerDifficultyIncreaseChance() {
        return INSTANCE.player_difficulty_increase_chance;
    }
    
    public static boolean isEnableDifficultyHud() {
        return INSTANCE.enable_difficulty_hud;
    }
    
    /** @deprecated Usar isEnableDifficultyHud() */
    @Deprecated
    public static boolean isDifficultyHudEnabled() {
        return INSTANCE.enable_difficulty_hud;
    }
    
    public static double getBiomeDifficultyMultiplier() {
        return INSTANCE.biome_difficulty_multiplier;
    }
    
    public static double getEffectiveDifficultyRadius() {
        return INSTANCE.effective_difficulty_radius;
    }
    
    // ============ GETTERS ESTÁTICOS - TIER MULTIPLIERS ============
    
    public static double getHealthMultiplier(MobTier tier) {
        return getHealthMultiplierByName(tier.name());
    }
    
    public static double getHealthMultiplierByName(String tierName) {
        return switch (tierName.toUpperCase()) {
            case "EPIC" -> INSTANCE.epic_health_multiplier;
            case "LEGENDARY" -> INSTANCE.legendary_health_multiplier;
            case "MYTHIC" -> INSTANCE.mythic_health_multiplier;
            case "DEFINITIVE" -> INSTANCE.definitive_health_multiplier;
            default -> 1.0;
        };
    }
    
    public static double getDamageMultiplier(MobTier tier) {
        return getDamageMultiplierByName(tier.name());
    }
    
    public static double getDamageMultiplierByName(String tierName) {
        return switch (tierName.toUpperCase()) {
            case "EPIC" -> INSTANCE.epic_damage_multiplier;
            case "LEGENDARY" -> INSTANCE.legendary_damage_multiplier;
            case "MYTHIC" -> INSTANCE.mythic_damage_multiplier;
            case "DEFINITIVE" -> INSTANCE.definitive_damage_multiplier;
            default -> 1.0;
        };
    }
    
    public static double getChanceMultiplier(MobTier tier) {
        return getChanceMultiplierByName(tier.name());
    }
    
    public static double getChanceMultiplierByName(String tierName) {
        return switch (tierName.toUpperCase()) {
            case "EPIC" -> INSTANCE.epic_chance_multiplier;
            case "LEGENDARY" -> INSTANCE.legendary_chance_multiplier;
            case "MYTHIC" -> INSTANCE.mythic_chance_multiplier;
            case "DEFINITIVE" -> INSTANCE.definitive_chance_multiplier;
            default -> 1.0;
        };
    }
    
    // ============ GETTERS ESTÁTICOS - DEBUG ============
    
    public static boolean isDebugForceHighestAllowedTier() {
        return INSTANCE.debug_force_highest_allowed_tier;
    }
    
    /** @deprecated Usar isDebugForceHighestAllowedTier() */
    @Deprecated
    public static boolean debugForceHighestAllowedTier() {
        return INSTANCE.debug_force_highest_allowed_tier;
    }
    
    @Nullable
    public static MobTier getDebugForceExactTier() {
        return INSTANCE.debug_force_exact_tier;
    }
    
    public static boolean isDebugLogProbabilityDetails() {
        return INSTANCE.debug_log_probability_details;
    }
    
    /** @deprecated Usar isDebugLogProbabilityDetails() */
    @Deprecated
    public static boolean debugLogProbabilityDetails() {
        return INSTANCE.debug_log_probability_details;
    }
    
    public static boolean isDebugTraceBlockBreakDenials() {
        return INSTANCE.debug_trace_block_break_denials;
    }
    
    /** @deprecated Usar isDebugTraceBlockBreakDenials() */
    @Deprecated
    public static boolean debugTraceBlockBreakDenials() {
        return INSTANCE.debug_trace_block_break_denials;
    }
    
    // ============ GETTERS ESTÁTICOS - TIERED LOOT ============
    
    public static boolean isTieredLootEnabled() {
        return INSTANCE.tiered_loot_enabled;
    }
    
    public static boolean isTieredLootStrictEntityTables() {
        return INSTANCE.tiered_loot_strict_entity_tables;
    }
    
    public static boolean isTieredLootEpicEnabled() {
        return INSTANCE.tiered_loot_epic_enabled;
    }
    
    public static boolean isTieredLootLegendaryEnabled() {
        return INSTANCE.tiered_loot_legendary_enabled;
    }
    
    public static boolean isTieredLootMythicEnabled() {
        return INSTANCE.tiered_loot_mythic_enabled;
    }
    
    public static boolean isTieredLootDefinitiveEnabled() {
        return INSTANCE.tiered_loot_definitive_enabled;
    }
    
    public static boolean isTierLootEnabled(MobTier tier) {
        return switch (tier) {
            case EPIC -> INSTANCE.tiered_loot_epic_enabled;
            case LEGENDARY -> INSTANCE.tiered_loot_legendary_enabled;
            case MYTHIC -> INSTANCE.tiered_loot_mythic_enabled;
            case DEFINITIVE -> INSTANCE.tiered_loot_definitive_enabled;
            default -> false;
        };
    }
    
    public static boolean isTieredLootTelemetryEnabled() {
        return INSTANCE.tiered_loot_telemetry_enabled;
    }
    
    // ============ GETTERS ESTÁTICOS - DEATH PENALTY ============
    
    public static int getDeathPenaltyAmount() {
        return INSTANCE.death_penalty_amount;
    }
    
    public static long getDeathPenaltyCooldownTicks() {
        return INSTANCE.death_penalty_cooldown_ticks;
    }
    
    // ============ VALIDACIÓN ============
    
    private void validate() {
        max_global_difficulty = Math.max(1, Math.min(100_000, max_global_difficulty));
        player_difficulty_increase_chance = Math.max(0.0, Math.min(1.0, player_difficulty_increase_chance));
        
        // Multiplicadores de tier (mínimo 0.1, máximo 32)
        epic_health_multiplier = clampMultiplier(epic_health_multiplier);
        epic_damage_multiplier = clampMultiplier(epic_damage_multiplier);
        legendary_health_multiplier = ensureTierOrder(legendary_health_multiplier, epic_health_multiplier);
        legendary_damage_multiplier = ensureTierOrder(legendary_damage_multiplier, epic_damage_multiplier);
        mythic_health_multiplier = ensureTierOrder(mythic_health_multiplier, legendary_health_multiplier);
        mythic_damage_multiplier = ensureTierOrder(mythic_damage_multiplier, legendary_damage_multiplier);
        definitive_health_multiplier = ensureTierOrder(definitive_health_multiplier, mythic_health_multiplier);
        definitive_damage_multiplier = ensureTierOrder(definitive_damage_multiplier, mythic_damage_multiplier);
        
        // Multiplicadores de chance
        epic_chance_multiplier = clampChanceMultiplier(epic_chance_multiplier);
        legendary_chance_multiplier = clampChanceMultiplier(legendary_chance_multiplier);
        mythic_chance_multiplier = clampChanceMultiplier(mythic_chance_multiplier);
        definitive_chance_multiplier = clampChanceMultiplier(definitive_chance_multiplier);
        
        effective_difficulty_radius = Math.max(0.0, effective_difficulty_radius);
        death_penalty_amount = Math.max(0, death_penalty_amount);
        death_penalty_cooldown_ticks = Math.max(0L, death_penalty_cooldown_ticks);
    }
    
    private static double clampMultiplier(double value) {
        if (!Double.isFinite(value)) return 1.0;
        return Math.max(0.1, Math.min(32.0, value));
    }
    
    private static double clampChanceMultiplier(double value) {
        if (!Double.isFinite(value)) return 1.0;
        return Math.max(0.01, Math.min(64.0, value));
    }
    
    private static double ensureTierOrder(double value, double previousMin) {
        return Math.max(clampMultiplier(value), previousMin);
    }
}
