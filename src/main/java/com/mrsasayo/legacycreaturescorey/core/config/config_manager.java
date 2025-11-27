package com.mrsasayo.legacycreaturescorey.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.core.config.domain.antifarm_config;
import com.mrsasayo.legacycreaturescorey.core.config.domain.difficulty_config;
import com.mrsasayo.legacycreaturescorey.core.config.domain.mutation_config;
import com.mrsasayo.legacycreaturescorey.core.config.domain.system_config;
import com.mrsasayo.legacycreaturescorey.core.config.dto.root_config_dto;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Orquestador principal de configuración.
 * 
 * Responsabilidades:
 * - Leer/escribir el archivo físico legacycreaturescorey.json
 * - Delegar la carga de secciones a los módulos de dominio
 * - Sincronizar cambios de vuelta al archivo
 * 
 * Arquitectura DDD:
 * - DTO (root_config_dto): Mapeo directo del JSON
 * - Domain (mutation_config, system_config, etc.): Lógica de negocio y validación
 * - Manager (esta clase): Orquestación y persistencia
 */
public final class config_manager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("ConfigManager");
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create();
    
    // DTO en memoria - representa el estado actual del JSON
    private static root_config_dto currentDto = new root_config_dto();
    
    // Bandera de inicialización
    private static boolean initialized = false;
    
    private config_manager() {} // No instanciable
    
    /**
     * Ruta del archivo de configuración.
     */
    public static Path getConfigPath() {
        return FabricLoader.getInstance()
            .getConfigDir()
            .resolve(Legacycreaturescorey.MOD_ID + ".json");
    }
    
    /**
     * Inicializa el sistema de configuración.
     * Carga el archivo existente o crea uno nuevo con valores por defecto.
     */
    public static synchronized void initialize() {
        if (initialized) {
            LOGGER.warn("ConfigManager ya estaba inicializado, ignorando llamada duplicada.");
            return;
        }
        
        Path configPath = getConfigPath();
        LOGGER.info("Inicializando configuración desde: {}", configPath);
        
        // Cargar o crear DTO
        currentDto = loadDtoFromDisk(configPath);
        
        // Delegar a módulos de dominio
        loadDomainConfigs();
        
        // Guardar con posibles nuevos campos añadidos
        saveDtoToDisk(configPath);
        
        initialized = true;
        LOGGER.info("Sistema de configuración inicializado correctamente.");
    }
    
    /**
     * Recarga la configuración desde disco.
     * Útil para /reload o cambios en caliente.
     */
    public static synchronized reload_result reload() {
        if (!initialized) {
            initialize();
            return reload_result.success("Configuración inicializada por primera vez.");
        }
        
        Path configPath = getConfigPath();
        
        try {
            currentDto = loadDtoFromDisk(configPath);
            loadDomainConfigs();
            saveDtoToDisk(configPath); // Sincronizar posibles nuevos campos
            
            return reload_result.success("Configuración recargada desde " + configPath.getFileName());
        } catch (Exception e) {
            LOGGER.error("Error recargando configuración", e);
            return reload_result.failure("Error: " + e.getMessage());
        }
    }
    
    /**
     * Guarda la configuración actual a disco.
     */
    public static synchronized void save() {
        if (!initialized) {
            LOGGER.warn("Intentando guardar configuración antes de inicializar.");
            return;
        }
        
        // Sincronizar desde dominios al DTO
        saveDomainConfigs();
        
        // Guardar a disco
        saveDtoToDisk(getConfigPath());
    }
    
    /**
     * Obtiene el DTO actual (solo lectura, para acceso directo si es necesario).
     */
    public static root_config_dto getCurrentDto() {
        return currentDto;
    }
    
    // ============ INTERNAL: CARGA/GUARDADO ============
    
    private static root_config_dto loadDtoFromDisk(Path configPath) {
        if (Files.notExists(configPath)) {
            LOGGER.info("Archivo de configuración no existe, usando valores por defecto.");
            return new root_config_dto();
        }
        
        try (Reader reader = Files.newBufferedReader(configPath)) {
            root_config_dto loaded = GSON.fromJson(reader, root_config_dto.class);
            if (loaded != null) {
                LOGGER.info("Configuración cargada desde disco.");
                // Asegurar que las secciones anidadas no sean null
                ensureNestedDtosExist(loaded);
                return loaded;
            }
            LOGGER.warn("El archivo de configuración está vacío.");
        } catch (IOException | JsonParseException e) {
            LOGGER.error("Error leyendo configuración: {}", e.getMessage());
        }
        
        return new root_config_dto();
    }
    
    private static void ensureNestedDtosExist(root_config_dto dto) {
        if (dto.mutation_system == null) {
            dto.mutation_system = new root_config_dto.mutation_system_dto();
        }
        if (dto.category_toggle == null) {
            dto.category_toggle = new root_config_dto.category_toggle_dto();
        }
        if (dto.weighting_system == null) {
            dto.weighting_system = new root_config_dto.weighting_system_dto();
        }
        if (dto.cost_system == null) {
            dto.cost_system = new root_config_dto.cost_system_dto();
        }
        if (dto.cost_system.standardized_cost_difficulty == null) {
            dto.cost_system.standardized_cost_difficulty = new root_config_dto.cost_system_dto.standardized_cost_dto();
        }
        if (dto.performance == null) {
            dto.performance = new root_config_dto.performance_dto();
        }
    }
    
    private static void saveDtoToDisk(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {
                GSON.toJson(currentDto, writer);
            }
            LOGGER.debug("Configuración guardada en disco.");
        } catch (IOException e) {
            LOGGER.error("No se pudo escribir la configuración en {}", configPath, e);
        }
    }
    
    @SuppressWarnings("deprecation") // Sincronización intencional de fachada legacy
    private static void loadDomainConfigs() {
        difficulty_config.loadFrom(currentDto);
        antifarm_config.loadFrom(currentDto);
        mutation_config.loadFrom(currentDto);
        system_config.loadFrom(currentDto);
        
        // Sincronizar fachada legacy para compatibilidad hacia atrás
        CoreyConfig.INSTANCE.syncFromDomainConfigs();
    }
    
    private static void saveDomainConfigs() {
        difficulty_config.saveTo(currentDto);
        antifarm_config.saveTo(currentDto);
        mutation_config.saveTo(currentDto);
        system_config.saveTo(currentDto);
    }
    
    // ============ RESULTADO DE RECARGA ============
    
    public record reload_result(boolean success, String message) {
        public static reload_result success(String message) {
            return new reload_result(true, message);
        }
        public static reload_result failure(String message) {
            return new reload_result(false, message);
        }
    }
}
