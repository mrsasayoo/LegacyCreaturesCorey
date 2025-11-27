package com.mrsasayo.legacycreaturescorey.core.config.dto;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

/**
 * Data Transfer Object que representa la estructura completa del archivo JSON de configuración.
 * Esta clase mapea directamente el contenido de legacycreaturescorey.json.
 * 
 * Combina tanto la configuración legacy (campos planos) como la nueva estructura modular.
 */
public final class root_config_dto {
    
    // ============ DIFFICULTY SYSTEM (Legacy - campos planos) ============
    @SerializedName("maxGlobalDifficulty")
    public int max_global_difficulty = 1000;
    
    @SerializedName("playerDifficultyIncreaseChance")
    public double player_difficulty_increase_chance = 1.0;
    
    @SerializedName("enableDifficultyHud")
    public boolean enable_difficulty_hud = false;
    
    @SerializedName("biomeDifficultyMultiplier")
    public double biome_difficulty_multiplier = 1.5;
    
    // ============ TIER MULTIPLIERS (Legacy) ============
    @SerializedName("epicHealthMultiplier")
    public double epic_health_multiplier = 1.0;
    
    @SerializedName("epicDamageMultiplier")
    public double epic_damage_multiplier = 1.0;
    
    @SerializedName("legendaryHealthMultiplier")
    public double legendary_health_multiplier = 1.0;
    
    @SerializedName("legendaryDamageMultiplier")
    public double legendary_damage_multiplier = 1.0;
    
    @SerializedName("mythicHealthMultiplier")
    public double mythic_health_multiplier = 1.0;
    
    @SerializedName("mythicDamageMultiplier")
    public double mythic_damage_multiplier = 1.0;
    
    @SerializedName("definitiveHealthMultiplier")
    public double definitive_health_multiplier = 1.0;
    
    @SerializedName("definitiveDamageMultiplier")
    public double definitive_damage_multiplier = 1.0;
    
    // ============ TIER CHANCE MULTIPLIERS (Legacy) ============
    @SerializedName("epicChanceMultiplier")
    public double epic_chance_multiplier = 6.0;
    
    @SerializedName("legendaryChanceMultiplier")
    public double legendary_chance_multiplier = 12.0;
    
    @SerializedName("mythicChanceMultiplier")
    public double mythic_chance_multiplier = 18.0;
    
    @SerializedName("definitiveChanceMultiplier")
    public double definitive_chance_multiplier = 24.0;
    
    @SerializedName("effectiveDifficultyRadius")
    public double effective_difficulty_radius = 48.0;
    
    // ============ DEBUG (Legacy) ============
    @SerializedName("debugForceHighestAllowedTier")
    public boolean debug_force_highest_allowed_tier = false;
    
    @SerializedName("debugForceExactTier")
    @Nullable
    public String debug_force_exact_tier = null;
    
    @SerializedName("debugLogProbabilityDetails")
    public boolean debug_log_probability_details = false;
    
    @SerializedName("debugTraceBlockBreakDenials")
    public boolean debug_trace_block_break_denials = false;
    
    // ============ TIERED LOOT (Legacy) ============
    @SerializedName("tieredLootEnabled")
    public boolean tiered_loot_enabled = true;
    
    @SerializedName("tieredLootStrictEntityTables")
    public boolean tiered_loot_strict_entity_tables = true;
    
    @SerializedName("tieredLootEpicEnabled")
    public boolean tiered_loot_epic_enabled = true;
    
    @SerializedName("tieredLootLegendaryEnabled")
    public boolean tiered_loot_legendary_enabled = true;
    
    @SerializedName("tieredLootMythicEnabled")
    public boolean tiered_loot_mythic_enabled = true;
    
    @SerializedName("tieredLootDefinitiveEnabled")
    public boolean tiered_loot_definitive_enabled = true;
    
    @SerializedName("tieredLootTelemetryEnabled")
    public boolean tiered_loot_telemetry_enabled = false;
    
    // ============ ANTIFARM (Legacy - campos planos) ============
    @SerializedName("antiFarmDetectionEnabled")
    public boolean antifarm_detection_enabled = true;
    
    @SerializedName("antiFarmKillThreshold")
    public int antifarm_kill_threshold = 64;
    
    @SerializedName("antiFarmWindowTicks")
    public long antifarm_window_ticks = 6000L;
    
    @SerializedName("antiFarmBlockRadiusChunks")
    public int antifarm_block_radius_chunks = 4;
    
    @SerializedName("antiFarmRestrictTieredSpawns")
    public boolean antifarm_restrict_tiered_spawns = true;
    
    @SerializedName("antiFarmLogDetections")
    public boolean antifarm_log_detections = true;
    
    @SerializedName("antiFarmDailyDecayAmount")
    public int antifarm_daily_decay_amount = 64;
    
    @SerializedName("antiFarmHeatPenaltyEnabled")
    public boolean antifarm_heat_penalty_enabled = true;
    
    @SerializedName("antiFarmHeatPenaltyMinMultiplier")
    public double antifarm_heat_penalty_min_multiplier = 0.25;
    
    @SerializedName("antiFarmHeatPenaltyExponent")
    public double antifarm_heat_penalty_exponent = 1.0;
    
    // ============ DEATH PENALTY (Legacy) ============
    @SerializedName("deathPenaltyAmount")
    public int death_penalty_amount = 4;
    
    @SerializedName("deathPenaltyCooldownTicks")
    public long death_penalty_cooldown_ticks = 1200L;
    
    // ============ NUEVAS SECCIONES MODULARES ============
    
    @SerializedName("mutation_system")
    public mutation_system_dto mutation_system = new mutation_system_dto();
    
    @SerializedName("category_toggle")
    public category_toggle_dto category_toggle = new category_toggle_dto();
    
    @SerializedName("weighting_system")
    public weighting_system_dto weighting_system = new weighting_system_dto();
    
    @SerializedName("cost_system")
    public cost_system_dto cost_system = new cost_system_dto();
    
    @SerializedName("performance")
    public performance_dto performance = new performance_dto();
    
    // ============ NESTED DTOs ============
    
    public static final class mutation_system_dto {
        public int max_mutations_per_mob = 3;
        public int mutation_point_budget = 100;
        public boolean allow_incompatible_mutations = false;
        public boolean enable_debug_logging = false;
    }
    
    public static final class category_toggle_dto {
        public boolean passive = true;
        public boolean on_hit = true;
        public boolean mob_exclusive = true;
        public boolean auras = true;
        public boolean on_being_hit = true;
        public boolean on_death = true;
        public boolean synergy = true;
        public boolean terrain = true;
    }
    
    public static final class weighting_system_dto {
        public double weighting_weight = 0.0;
    }
    
    public static final class cost_system_dto {
        public double general_cost_reduction = 0.0;
        public standardized_cost_dto standardized_cost_difficulty = new standardized_cost_dto();
        
        public static final class standardized_cost_dto {
            @Nullable public Integer weak = null;
            @Nullable public Integer intermediate = null;
            @Nullable public Integer strong = null;
        }
    }
    
    public static final class performance_dto {
        public int passive_tick_interval = 20;
        public int aura_check_interval = 20;
    }
}
