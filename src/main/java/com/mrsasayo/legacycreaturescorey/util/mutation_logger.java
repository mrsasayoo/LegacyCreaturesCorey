package com.mrsasayo.legacycreaturescorey.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.Nullable;

/**
 * Sistema de logging multi-nivel para mutaciones.
 * 
 * Separa los logs por categoría de mutación para facilitar debugging.
 * Cada categoría escribe a su propio archivo en run/logs/mutations_*.log
 * 
 * Arquitectura:
 * - Usa marcadores (Markers) de Log4j2 para filtrar por categoría
 * - Configuración en log4j2.xml para routear a archivos separados
 * - Idioma: Español
 * 
 * Uso:
 * ```java
 * mutation_logger.aura("Activando aura de fuego para mob {}", mobId);
 * mutation_logger.on_hit("Aplicando daño extra: {} -> {}", source, target);
 * mutation_logger.error("Error crítico en mutación", exception);
 * ```
 */
public final class mutation_logger {
    
    // Logger principal
    private static final Logger LOGGER = LogManager.getLogger("MutationLogger");
    
    // ============ MARCADORES POR CATEGORÍA ============
    // Cada marcador se usa para filtrar logs a archivos específicos
    
    /** Marcador para mutaciones de tipo Aura */
    public static final Marker MARKER_AURA = MarkerManager.getMarker("MUTATION_AURA");
    
    /** Marcador para mutaciones de tipo On-Hit (al golpear) */
    public static final Marker MARKER_ON_HIT = MarkerManager.getMarker("MUTATION_ON_HIT");
    
    /** Marcador para mutaciones exclusivas de mob */
    public static final Marker MARKER_MOB_EXCLUSIVE = MarkerManager.getMarker("MUTATION_MOB_EXCLUSIVE");
    
    /** Marcador para mutaciones pasivas */
    public static final Marker MARKER_PASSIVE = MarkerManager.getMarker("MUTATION_PASSIVE");
    
    /** Marcador para mutaciones On-Being-Hit (al recibir golpe) */
    public static final Marker MARKER_ON_BEING_HIT = MarkerManager.getMarker("MUTATION_ON_BEING_HIT");
    
    /** Marcador para mutaciones On-Death (al morir) */
    public static final Marker MARKER_ON_DEATH = MarkerManager.getMarker("MUTATION_ON_DEATH");
    
    /** Marcador para mutaciones de sinergia */
    public static final Marker MARKER_SYNERGY = MarkerManager.getMarker("MUTATION_SYNERGY");
    
    /** Marcador para mutaciones de terreno */
    public static final Marker MARKER_TERRAIN = MarkerManager.getMarker("MUTATION_TERRAIN");
    
    /** Marcador para debug técnico general */
    public static final Marker MARKER_DEBUG = MarkerManager.getMarker("MUTATION_DEBUG");
    
    private mutation_logger() {} // No instanciable
    
    // ============ MÉTODOS DE LOGGING: AURAS ============
    
    /**
     * Log de evento de aura (INFO).
     * @param mensaje Mensaje en español con placeholders {}
     * @param args Argumentos para los placeholders
     */
    public static void aura(String mensaje, Object... args) {
        LOGGER.info(MARKER_AURA, "[AURA] " + mensaje, args);
    }
    
    /**
     * Log de evento de aura (DEBUG).
     */
    public static void auraDebug(String mensaje, Object... args) {
        LOGGER.debug(MARKER_AURA, "[AURA-DEBUG] " + mensaje, args);
    }
    
    /**
     * Log de advertencia de aura.
     */
    public static void auraWarn(String mensaje, Object... args) {
        LOGGER.warn(MARKER_AURA, "[AURA-WARN] " + mensaje, args);
    }
    
    // ============ MÉTODOS DE LOGGING: ON-HIT ============
    
    /**
     * Log de evento on-hit (INFO).
     * @param mensaje Mensaje en español con placeholders {}
     * @param args Argumentos para los placeholders
     */
    public static void onHit(String mensaje, Object... args) {
        LOGGER.info(MARKER_ON_HIT, "[ON_HIT] " + mensaje, args);
    }
    
    /**
     * Log de evento on-hit (DEBUG).
     */
    public static void onHitDebug(String mensaje, Object... args) {
        LOGGER.debug(MARKER_ON_HIT, "[ON_HIT-DEBUG] " + mensaje, args);
    }
    
    /**
     * Log de advertencia on-hit.
     */
    public static void onHitWarn(String mensaje, Object... args) {
        LOGGER.warn(MARKER_ON_HIT, "[ON_HIT-WARN] " + mensaje, args);
    }
    
    // ============ MÉTODOS DE LOGGING: MOB EXCLUSIVE ============
    
    /**
     * Log de evento mob_exclusive (INFO).
     * @param mensaje Mensaje en español con placeholders {}
     * @param args Argumentos para los placeholders
     */
    public static void mobExclusive(String mensaje, Object... args) {
        LOGGER.info(MARKER_MOB_EXCLUSIVE, "[MOB_EXCLUSIVE] " + mensaje, args);
    }
    
    /**
     * Log de evento mob_exclusive (DEBUG).
     */
    public static void mobExclusiveDebug(String mensaje, Object... args) {
        LOGGER.debug(MARKER_MOB_EXCLUSIVE, "[MOB_EXCLUSIVE-DEBUG] " + mensaje, args);
    }
    
    /**
     * Log de advertencia mob_exclusive.
     */
    public static void mobExclusiveWarn(String mensaje, Object... args) {
        LOGGER.warn(MARKER_MOB_EXCLUSIVE, "[MOB_EXCLUSIVE-WARN] " + mensaje, args);
    }
    
    // ============ MÉTODOS DE LOGGING: PASSIVE ============
    
    /**
     * Log de evento passive (INFO).
     * @param mensaje Mensaje en español con placeholders {}
     * @param args Argumentos para los placeholders
     */
    public static void passive(String mensaje, Object... args) {
        LOGGER.info(MARKER_PASSIVE, "[PASSIVE] " + mensaje, args);
    }
    
    /**
     * Log de evento passive (DEBUG).
     */
    public static void passiveDebug(String mensaje, Object... args) {
        LOGGER.debug(MARKER_PASSIVE, "[PASSIVE-DEBUG] " + mensaje, args);
    }
    
    /**
     * Log de advertencia passive.
     */
    public static void passiveWarn(String mensaje, Object... args) {
        LOGGER.warn(MARKER_PASSIVE, "[PASSIVE-WARN] " + mensaje, args);
    }
    
    // ============ MÉTODOS DE LOGGING: ON-BEING-HIT ============
    
    /**
     * Log de evento on_being_hit (INFO).
     * @param mensaje Mensaje en español con placeholders {}
     * @param args Argumentos para los placeholders
     */
    public static void onBeingHit(String mensaje, Object... args) {
        LOGGER.info(MARKER_ON_BEING_HIT, "[ON_BEING_HIT] " + mensaje, args);
    }
    
    /**
     * Log de evento on_being_hit (DEBUG).
     */
    public static void onBeingHitDebug(String mensaje, Object... args) {
        LOGGER.debug(MARKER_ON_BEING_HIT, "[ON_BEING_HIT-DEBUG] " + mensaje, args);
    }
    
    /**
     * Log de advertencia on_being_hit.
     */
    public static void onBeingHitWarn(String mensaje, Object... args) {
        LOGGER.warn(MARKER_ON_BEING_HIT, "[ON_BEING_HIT-WARN] " + mensaje, args);
    }
    
    // ============ MÉTODOS DE LOGGING: ON-DEATH ============
    
    /**
     * Log de evento on_death (INFO).
     * @param mensaje Mensaje en español con placeholders {}
     * @param args Argumentos para los placeholders
     */
    public static void onDeath(String mensaje, Object... args) {
        LOGGER.info(MARKER_ON_DEATH, "[ON_DEATH] " + mensaje, args);
    }
    
    /**
     * Log de evento on_death (DEBUG).
     */
    public static void onDeathDebug(String mensaje, Object... args) {
        LOGGER.debug(MARKER_ON_DEATH, "[ON_DEATH-DEBUG] " + mensaje, args);
    }
    
    /**
     * Log de advertencia on_death.
     */
    public static void onDeathWarn(String mensaje, Object... args) {
        LOGGER.warn(MARKER_ON_DEATH, "[ON_DEATH-WARN] " + mensaje, args);
    }
    
    // ============ MÉTODOS DE LOGGING: SYNERGY ============
    
    /**
     * Log de evento synergy (INFO).
     * @param mensaje Mensaje en español con placeholders {}
     * @param args Argumentos para los placeholders
     */
    public static void synergy(String mensaje, Object... args) {
        LOGGER.info(MARKER_SYNERGY, "[SYNERGY] " + mensaje, args);
    }
    
    /**
     * Log de evento synergy (DEBUG).
     */
    public static void synergyDebug(String mensaje, Object... args) {
        LOGGER.debug(MARKER_SYNERGY, "[SYNERGY-DEBUG] " + mensaje, args);
    }
    
    /**
     * Log de advertencia synergy.
     */
    public static void synergyWarn(String mensaje, Object... args) {
        LOGGER.warn(MARKER_SYNERGY, "[SYNERGY-WARN] " + mensaje, args);
    }
    
    // ============ MÉTODOS DE LOGGING: TERRAIN ============
    
    /**
     * Log de evento terrain (INFO).
     * @param mensaje Mensaje en español con placeholders {}
     * @param args Argumentos para los placeholders
     */
    public static void terrain(String mensaje, Object... args) {
        LOGGER.info(MARKER_TERRAIN, "[TERRAIN] " + mensaje, args);
    }
    
    /**
     * Log de evento terrain (DEBUG).
     */
    public static void terrainDebug(String mensaje, Object... args) {
        LOGGER.debug(MARKER_TERRAIN, "[TERRAIN-DEBUG] " + mensaje, args);
    }
    
    /**
     * Log de advertencia terrain.
     */
    public static void terrainWarn(String mensaje, Object... args) {
        LOGGER.warn(MARKER_TERRAIN, "[TERRAIN-WARN] " + mensaje, args);
    }
    
    // ============ MÉTODOS DE LOGGING: DEBUG GENERAL ============
    
    /**
     * Log de debug técnico general.
     * @param mensaje Mensaje en español con placeholders {}
     * @param args Argumentos para los placeholders
     */
    public static void debug(String mensaje, Object... args) {
        LOGGER.debug(MARKER_DEBUG, "[DEBUG] " + mensaje, args);
    }
    
    /**
     * Log de información técnica.
     */
    public static void info(String mensaje, Object... args) {
        LOGGER.info(MARKER_DEBUG, "[INFO] " + mensaje, args);
    }
    
    /**
     * Log de advertencia general.
     */
    public static void warn(String mensaje, Object... args) {
        LOGGER.warn(MARKER_DEBUG, "[WARN] " + mensaje, args);
    }
    
    // ============ MÉTODOS DE LOGGING: ERRORES ============
    
    /**
     * Log de error general (sin excepción).
     * @param mensaje Mensaje en español con placeholders {}
     * @param args Argumentos para los placeholders
     */
    public static void error(String mensaje, Object... args) {
        LOGGER.error(MARKER_DEBUG, "[ERROR] " + mensaje, args);
    }
    
    /**
     * Log de error con excepción/throwable.
     * @param mensaje Mensaje en español
     * @param throwable Excepción a loggear
     */
    public static void error(String mensaje, Throwable throwable) {
        LOGGER.error(MARKER_DEBUG, "[ERROR] " + mensaje, throwable);
    }
    
    /**
     * Log de error con categoría específica.
     * @param marker Marcador de categoría
     * @param mensaje Mensaje en español
     * @param throwable Excepción a loggear (puede ser null)
     */
    public static void errorConCategoria(Marker marker, String mensaje, @Nullable Throwable throwable) {
        if (throwable != null) {
            LOGGER.error(marker, mensaje, throwable);
        } else {
            LOGGER.error(marker, mensaje);
        }
    }
    
    // ============ UTILIDADES ============
    
    /**
     * Obtiene el marcador apropiado para una categoría de mutación.
     * @param categoria Nombre de la categoría (aura, on_hit, passive, etc.)
     * @return Marker correspondiente o MARKER_DEBUG si no se reconoce
     */
    public static Marker getMarkerForCategory(String categoria) {
        if (categoria == null) return MARKER_DEBUG;
        
        return switch (categoria.toLowerCase()) {
            case "aura", "auras" -> MARKER_AURA;
            case "on_hit", "onhit" -> MARKER_ON_HIT;
            case "mob_exclusive", "mobexclusive" -> MARKER_MOB_EXCLUSIVE;
            case "passive", "passives" -> MARKER_PASSIVE;
            case "on_being_hit", "onbeinghit" -> MARKER_ON_BEING_HIT;
            case "on_death", "ondeath" -> MARKER_ON_DEATH;
            case "synergy", "synergies" -> MARKER_SYNERGY;
            case "terrain" -> MARKER_TERRAIN;
            default -> MARKER_DEBUG;
        };
    }
    
    /**
     * Log genérico por categoría.
     * @param categoria Nombre de la categoría
     * @param mensaje Mensaje en español
     * @param args Argumentos
     */
    public static void logByCategory(String categoria, String mensaje, Object... args) {
        LOGGER.info(getMarkerForCategory(categoria), "[" + categoria.toUpperCase() + "] " + mensaje, args);
    }
    
    /**
     * Log de carga de mutación.
     * @param mutationId ID de la mutación
     * @param categoria Categoría
     */
    public static void logMutacionCargada(String mutationId, String categoria) {
        LOGGER.debug(getMarkerForCategory(categoria), 
            "[CARGA] Mutación cargada: {} (categoría: {})", mutationId, categoria);
    }
    
    /**
     * Log de aplicación de mutación a mob.
     * @param mutationId ID de la mutación
     * @param mobType Tipo de mob
     * @param nivel Nivel de la mutación
     */
    public static void logMutacionAplicada(String mutationId, String mobType, int nivel) {
        LOGGER.info(MARKER_DEBUG, 
            "[APLICACIÓN] Mutación {} (nivel {}) aplicada a {}", mutationId, nivel, mobType);
    }
    
    /**
     * Log de ejecución de acción de mutación.
     * @param categoria Categoría de la mutación
     * @param actionType Tipo de acción
     * @param resultado Resultado (éxito/fallo)
     */
    public static void logAccionEjecutada(String categoria, String actionType, boolean resultado) {
        Marker marker = getMarkerForCategory(categoria);
        if (resultado) {
            LOGGER.debug(marker, "[ACCIÓN] {} ejecutada exitosamente", actionType);
        } else {
            LOGGER.warn(marker, "[ACCIÓN] {} falló o fue ignorada", actionType);
        }
    }
}
